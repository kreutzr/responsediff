package com.github.kreutzr.responsediff.filter.response;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.kreutzr.responsediff.JsonPathHelper;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.JsonHelper;
import com.jayway.jsonpath.PathNotFoundException;
import com.github.kreutzr.responsediff.XmlHttpResponse;

/**
 * A filter that sorts the map entries of a JSON data set.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>name="sortArrays",      values=[ "true", "false" ]</li>
 * <li>name="sortArrays.keys", values=[ &lt;id&gt;, &lt;id&gt; ]</li>
 * </ul>
 * <p>
 * <b>Supported inherited parameters:</b>
 * <ul>
 * <li>name="storeOriginalResponse",  values=[ "true", "false" ] (default is false)</li>
 * </ul>
 */
public class SortJsonBodyResponseFilter extends DiffResponseFilterImpl
{
  public static final String PARAMETER_NAME__SORT_ARRAYS       = "sortArrays";
  public static final String PARAMETER_NAME__SORT_ARRAYS__KEYS = "sortArrays.keys";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( SortJsonBodyResponseFilter.class );

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat( "p0000000000000000000.000000000000000;m0000000000000000000.000000000000000" ); // NOTE: "m" < "p" in ASCII (while "-" > " " in ASCII)

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__SORT_ARRAYS );
    registerFilterParameterName( PARAMETER_NAME__SORT_ARRAYS__KEYS );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply( final XmlHttpResponse xmlHttpResponse )
  throws DiffFilterException
  {
    if( !xmlHttpResponse.isBodyIsJson() ) {
      // Only intended for JSON
      LOG.debug( "Skipped because content type is not JSON." );
      return;
    }

    super.apply( xmlHttpResponse );

    xmlHttpResponse.setBody( apply( xmlHttpResponse.getBody() ) );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String apply( final String json )
  throws DiffFilterException
  {
    try {
      // Sort JSON maps
      final JsonNode root = JsonHelper.provideObjectMapper().readTree( json );

      // Sort JSON arrays
       if( Converter.asBoolean( getFilterParameter( PARAMETER_NAME__SORT_ARRAYS ), false ) ) {
        // Initialize keys to Sort
        Map< String, List< String > > keysToSort = null;
        final String keysToSortAsString = getFilterParameter( PARAMETER_NAME__SORT_ARRAYS__KEYS );
        if( keysToSortAsString != null ) {
          final String[] keysToSortEntries = keysToSortAsString.split( "," );
          keysToSort = new TreeMap<>();
          for( final String keysToSortEntry : keysToSortEntries ) {
            String key = keysToSortEntry.trim();
            List< String > sortPaths = null;

            // Parse lookup paths (if any)
            final int pos1 = key.indexOf    ( "(" );
            final int pos2 = key.lastIndexOf( ")" );
            if( pos1 > 0 && pos2 > pos1 ) {
              final String jsonPathsString = key.substring( pos1+1, pos2 );
              final String[] jsonPaths = jsonPathsString.split( ";" );
              if( jsonPaths.length > 0 ) {
                sortPaths = new ArrayList< String >();
                for( final String jsonPath : jsonPaths ) {
                  sortPaths.add( jsonPath.trim() );
                }
              }

              key = key.substring( 0, pos1 );
            }

            keysToSort.put( key, sortPaths );
          }
        }

        // Traverse JSON tree to sort arrays
        traverse( root, "$", keysToSort );
      }

      // Convert sorted JSON into String and update response body
      final String result = JsonHelper.provideObjectMapper().writeValueAsString( root );
      return result;
    }
    catch( final JsonProcessingException ex ) {
      throw new DiffFilterException( ex );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void traverse(
    final JsonNode node,
    final String name,
    final Map< String, List< String > > keysToSort
  )
  {
    JsonNodeType type = node.getNodeType();
    if( type == JsonNodeType.ARRAY ) {
      if( LOG.isTraceEnabled() ) {
        LOG.trace( "Found array entry " + name );
      }

      // ---------------------
      // Depth first sorting
      // ---------------------
      for( int i=0; i < node.size(); i++ ) {
        traverse( node.get( i ), name + "[" + i + "]", keysToSort );
      }

      // ---------------------
      // Sort if required
      // ---------------------
      if(  keysToSort == null                   // Sort all if not specified further
        || keysToSort.keySet().contains( name ) // Sort specified arrays
      ) {
        final List< String > sortPaths = ( keysToSort == null )
          ? null
          : keysToSort.get( name );

        if( LOG.isTraceEnabled() ) {
          LOG.trace( "Sorting array entry " + name );
        }

        sortArrayByTreeMap( (ArrayNode)node, sortPaths );
      }
    }
    else if( type == JsonNodeType.OBJECT ) {
      if( LOG.isTraceEnabled() ) {
        LOG.trace( "Found map entry " + name );
      }

      final Iterator< Entry< String, JsonNode > > it = node.properties().iterator();
      while( it.hasNext() ) {
        final Entry< String, JsonNode > entry = it.next();
        traverse( entry.getValue(), entry.getKey(), keysToSort );
      }
    }

    return;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sorts the entries of a given array node.
   * @param arrayNode The array node to sort. Must not be null.
   * @param sortPaths The paths that define the sort order. May be null.
   *
   * NOTE This is 26 ms slower than sortArrayByTreeMap() when test method
   *      "testPerformanceOfSortArraysMethod()" is being run 5 times, each
   *      looping 1000 times over the tested sortArray methods.
   */
  @SuppressWarnings("unused")
  private void sortArrayByListSynchronization(
    final ArrayNode      arrayNode,
    final List< String > sortPaths
  )
  {
    // -----------------------------------------------------------
    // We compare two String representations of the array entries
    //
    // ref1 = [ A, E, D, B, C ]
    // ref2 = [ A, E, D, B, C ]
    //        [ 0, 1 ,2 ,3 ,4 ] // Reference index
    //
    // ref1 = [ A, B, C, D, E ] // After sort()
    //
    //     => [ 0, 3, 4, 2, 1 ] // Sort index
    // -----------------------------------------------------------
    final List< String > ref1 = new ArrayList<>();
    final List< String > ref2 = new ArrayList<>();

    for( int i=0; i < arrayNode.size(); i++ ) {
      final JsonNode entry = arrayNode.get( i );

      String entryAsString = entry.toString(); // NOTE: This is expensive => Therefore we convert only once
      if( sortPaths != null ) {
        final JsonPathHelper jph = new JsonPathHelper( entryAsString ); // NOTE: performance! Why first convert to String and re-parse here?
        final StringBuilder sb = new StringBuilder();
        for( int j=0; j < sortPaths.size(); j++ ) {
          final String jsonPath = sortPaths.get( j );
          try {
            final Object obj = jph.getValue( jsonPath );
            String comparePart = ""; // null values are sorted to the beginning
            if( obj != null ) {
              if( obj instanceof Boolean ) {
                comparePart = ((Boolean)obj) == false ? "0" : "1";
              }
              else if( obj instanceof Number ) {
                comparePart = DECIMAL_FORMAT.format( obj );
              }
              else {
                comparePart = obj.toString(); // NOTE: This is also expensive
              }
            }
            sb.append( comparePart );
            if( j < sortPaths.size() - 1 ) {
              sb.append( " | " );
            }
          }
          catch( final PathNotFoundException ex ) {
            LOG.warn( "Unable to sort by \"" + jsonPath + "\". The sortArray key is ignored.", ex );
          }
        }
        entryAsString = sb.toString();
      }

      ref1.add( entryAsString );
      ref2.add( entryAsString );
    }

    // Sort one of the two reference arrays
    Collections.sort( ref1 );

    // Find index of entries in sorted reference array
    final List< JsonNode > entryList = new ArrayList<>( arrayNode.size() );
    for( int i=0; i < arrayNode.size(); i++ ) {
      final String entryAsString = ref1.get( i );
      for( int index=0; index < arrayNode.size(); index++ ) {
        if( ref2.get( index ).equals( entryAsString ) ) {
          final JsonNode entry = arrayNode.get( index );
          entryList.add( entry );
          break;
        }
      }
    }

    // Replace array with sorted list
    arrayNode.removeAll();
    arrayNode.addAll( entryList );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sorts the entries of a given array node.
   * @param arrayNode The array node to sort. Must not be null.
   * @param sortPaths The paths that define the sort order. May be null.
   */
  private void sortArrayByTreeMap(
    final ArrayNode      arrayNode,
    final List< String > sortPaths
  )
  {
    // -----------------------------------------------------------
    // We put the string representations of each array entry
    // together with the entry itself into TreeMap (which sorts
    // the keys automatically) and (re-)arrange the array entries
    // according to the maps values.
    //
    // map = [ A : entry0, E : entry1, D : entry2, B : entry3, C : entry4 ]
    //
    // map.values() = [ entry0, entry3, entry4, entry2, entry1 ]
    // -----------------------------------------------------------
    final TreeMap< String, List< JsonNode > > ref = new TreeMap<>();

    for( int i=0; i < arrayNode.size(); i++ ) {
      final JsonNode entry = arrayNode.get( i );

      String entryAsString = entry.toString(); // NOTE: This is expensive => Therefore we convert only once
      if( sortPaths != null ) {
        final JsonPathHelper jph = new JsonPathHelper( entryAsString ); // NOTE: performance! Why first convert to String and re-parse here?
        final StringBuilder sb = new StringBuilder();
        for( int j=0; j < sortPaths.size(); j++ ) {
          final String jsonPath = sortPaths.get( j );
          try {
            final Object obj = jph.getValue( jsonPath );
            String comparePart = ""; // null values are sorted to the beginning
            if( obj != null ) {
              if( obj instanceof Boolean ) {
                comparePart = ((Boolean)obj) == false ? "0" : "1";
              }
              else if( obj instanceof Number ) {
                comparePart = DECIMAL_FORMAT.format( obj );
              }
              else {
                comparePart = obj.toString(); // NOTE: This is also expensive
              }
            }
            sb.append( comparePart );
            if( j < sortPaths.size() - 1 ) {
              sb.append( " | " );
            }
          }
          catch( final PathNotFoundException ex ) {
            LOG.warn( "Unable to sort by \"" + jsonPath + "\". The sortArray key is ignored.", ex );
          }
        }
        entryAsString = sb.toString();
      }

      List< JsonNode > entryList = ref.get( entryAsString );
      if( entryList == null ) {
        entryList = new ArrayList<>();
        ref.put( entryAsString, entryList );
      }
      entryList.add( entry );
    }

    // Replace array with sorted list
    arrayNode.removeAll();
    for( final List< JsonNode > entryList : ref.values() ) {
      arrayNode.addAll( entryList );
    }
  }
}
