package com.github.kreutzr.responsediff.filter.response;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.kreutzr.responsediff.JsonTraverserNomalizationVisitor;
import com.github.kreutzr.responsediff.XmlHttpResponse;
import com.github.kreutzr.responsediff.JsonTraverser;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * A filter that normalizes JSON.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>name="replacements", value={ "&lt;replace_1&gt;" : "&lt;...&gt;", "&lt;replace_2&gt;" : "&lt;...&gt;", ... }</li>
 * <li>name="normalizeArrays", values=[ "true", "false" ] // Not supported yet</li>
 * <li>name="normalizeMaps", values=[ "true", "false" ]</li>
 * </ul>
 * <p>
 * <b>Supported inherited parameters:</b>
 * <ul>
 * <li>name="storeOriginalResponse", values=[ "true", "false" ] (default is false)</li>
 * </ul>
 */
public class NormalizeJsonBodyResponseFilter extends DiffResponseFilterImpl
{
  public static final String PARAMETER_NAME__NORMALIZE_MAPS   = "normalizeMaps";
  public static final String PARAMETER_NAME__NORMALIZE_ARRAYS = "normalizeArrays";
  public static final String PARAMETER_NAME__REPLACEMENTS     = "replacements";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( NormalizeJsonBodyResponseFilter.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__NORMALIZE_MAPS );
    registerFilterParameterName( PARAMETER_NAME__NORMALIZE_ARRAYS );
    registerFilterParameterName( PARAMETER_NAME__REPLACEMENTS );
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
    String result = json;

    try {
      final String replacementsJson = getFilterParameter( PARAMETER_NAME__REPLACEMENTS );
      if( replacementsJson != null ) {
        @SuppressWarnings("unchecked")
        final Map< String, String > replacements = (Map< String, String >)
          JsonHelper.provideObjectMapper().readValue( replacementsJson, Map.class );
        result = applyReplacements( json, replacements );
      }

      final boolean normalizeMaps   = Converter.asBoolean( getFilterParameter( PARAMETER_NAME__NORMALIZE_MAPS   ), false );
      final boolean normalizeArrays = Converter.asBoolean( getFilterParameter( PARAMETER_NAME__NORMALIZE_ARRAYS ), false );
      if( normalizeMaps ) {
    	  applyNormalization( result, normalizeMaps, normalizeArrays );
      }
      else {
        if( normalizeArrays ) {
          // Filter configuration check
          throw new DiffFilterException( "The configration parameter \"" + PARAMETER_NAME__NORMALIZE_ARRAYS + "\" must only be set true, if \"" + PARAMETER_NAME__NORMALIZE_MAPS + "\" is set true.");
        }
      }
    }
    catch( final Throwable ex ) {
      throw new DiffFilterException( ex );
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Replace all configured replacements within the given JSON Sring.
   * @param json The JSON string. May be null.
   * @param replacements The configured replacements. Must not be null.
   * @return The passed JSON String with all replacements. If json is null, null is returned.
   */
  private String applyReplacements( final String json, final Map< String, String > replacements )
  {
    if( json == null ) {
      return null;
    }

    String result = json;

    // NOTE:
    // - Currently no regular expressions supported in keys. (Otherwise regex-groups must be supported, too.)
    // - Currently '"<key>"' substrings (including the quotes (") are replaced.
    //   => Not particularly restricted to attribute names but effects matching attribute values, too!
    for( final String key: replacements.keySet() ) {
      result = result.replace( "\"" + key + "\"", "\"" + replacements.get( key ) + "\"" );
    }
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Normalizes the given JSON String.
   * @param json The JSON String to normalize. May be null.
   * @param normalizeArray Flag, if JSON array entries shall be normalized (true) or not (false).
   * @param normalizeMap   Flag, if JSON map entries shall be normalized (true) or not (false).
   * @return The normalized JSON String. If json is null, null is returned.
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  private String applyNormalization(
    final String json,
    final boolean normalizeArray,
    final boolean normalizeMap
  )
  throws JsonMappingException, JsonProcessingException
  {
    if( json == null ) {
      return null;
    }

    final JsonTraverserNomalizationVisitor listener = new JsonTraverserNomalizationVisitor(
      normalizeArray,
      normalizeMap
    );
    final JsonTraverser traverser = new JsonTraverser( json );
    final JsonNode root = traverser
      .addStructureVisitor( listener )
      .traverse()
      .getRoot();
	
    final String result = JsonHelper.provideObjectMapper().writeValueAsString( root );
    return result;
  }
}
