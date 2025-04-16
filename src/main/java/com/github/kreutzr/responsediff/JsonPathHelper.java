package com.github.kreutzr.responsediff;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.Defaults;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public class JsonPathHelper
{
  private static final Logger LOG = LoggerFactory.getLogger( JsonPathHelper.class );

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Defaults CONFIGURATION_DEFAULTS = new Configuration.Defaults() // By default Jackson is not used internally by JsonPath
  {
    private final JsonProvider    jsonProvider    = new JacksonJsonProvider();
    private final MappingProvider mappingProvider = new JacksonMappingProvider();

    @Override
    public JsonProvider jsonProvider()
    {
        return jsonProvider;
    }

    @Override
    public MappingProvider mappingProvider()
    {
        return mappingProvider;
    }

    @Override
    public Set<Option> options()
    {
        return EnumSet.noneOf(Option.class);
    }
  };

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final DocumentContext context_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param json The JSON to read. Must not be null.
   */
  public JsonPathHelper( final String json )
  {
    Configuration.setDefaults( CONFIGURATION_DEFAULTS );
    context_ = JsonPath.parse( json );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param context The DocumentContext to use. Must not be null.
   */
  public JsonPathHelper( final DocumentContext context )
  {
    Configuration.setDefaults( CONFIGURATION_DEFAULTS );
    context_ = context;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads a value object from the body of a JSON passed to the constructor.
   * <b>Please note</b> that, due to the used library. syntactically identical entries at different JsonPaths might be mapped to one single object.
   * @param path The JSON lookup path to use. Must not be null.
   * @return The requested object. If an error occurs, an exception is thrown.
   */
  public Object getValue( final String path )
  {
    // --------------------------------
    // NOTE: # is proprietary syntax
    // --------------------------------
    final int pos = path.lastIndexOf( "#" );
    if( pos >= 0 && path.indexOf( "#" ) != pos ) {
      throw new RuntimeException( "Illegal JSONPath syntax. Only one \"#\" allowed. JSONPath was \"" + path + "\"." );
    }
    final int index = ( pos > 0 )
      ? Integer.valueOf( path.substring( pos+1 ) )
      : -1;

    final String jsonPath = ( pos > 0 )
      ? path.substring( 0, pos )
      : path;


    // Read value from JSON
    Object obj = context_.read( jsonPath ); // NOTE: Do not catch a PathNotFoundException here!


    // --------------------------------
    // NOTE: # is proprietary syntax
    // --------------------------------
    if( index > -1 ) {
      if( obj != null && obj instanceof List<?> ) {
        @SuppressWarnings("unchecked")
        final List< Object > list = (List< Object >)obj;
        if( list.isEmpty() ) {
          obj = null;
        }
        else {
          obj = list.get( index );
        }
      }
      else {
        throw new RuntimeException(
          "Illegal JSONPath syntax. Array expected but was \""
          + (obj == null ? "null" : obj.getClass().getSimpleName() )
          + "\". JSONPath was \"" + path + "\"."
        );
      }
    }


    if( LOG.isTraceEnabled() ) {
      LOG.trace( "getValue( path=" + path + " ) result=" + getObjectInfo( obj ) );
    }

    return obj;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the given JSON path exists.
   * @param path The path to lookup. Must not be null.
   * @return true if the given path exists. Otherwise false is returned.
   */
  public boolean hasPath( final String path )
  {
    try {
      context_.read( path );
      return true;
    }
    catch( final PathNotFoundException ex ) {
      return false;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the value at the given JSON path is null.
   * @param path The path to lookup. Must not be null.
   * @return true if the value at the given path is null or the path does not exist. Otherwise false is returned.
   */
  public boolean isNull( final String path )
  {
    try {
      return getValue( path ) == null;
    }
    catch( final PathNotFoundException ex ) {
      return true;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if path1 "contains" path2. (e.g. path1="$.a[*]" contains path2="$.a[0]" and path2="$.a[1].b")
   * <br/>
   * <b>Please note</b> that, due to the used library, syntactically identical entries at different JsonPaths might be mapped to one single object.
   * @param path1 The path that might contain path2. Must not be null.
   * @param path2 The path that might be contained by path1. Must not be null.
   * @return true if path1 "contains" path2. Otherwise false is returned.
   */
  public static boolean contains(
    final String path1,
    final String path2
  )
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "contains( " + path1 + ", " + path2 + " )" );
    }

    final String regEx = path1
      .replaceAll( "\\$", "\\\\\\$" )                          // Preserve $ as not "end of line" in regEx
      .replaceAll( "\\[ *", "\\\\[" )                          // Mask brackets and ignore white spaces
      .replaceAll( " *\\]", "\\\\]" )                          // Mask brackets and ignore white spaces
      .replaceAll( "\\[\\*\\]", "<ANY_INDEX>" )                // Mask JsonPath array wild cards
      .replaceAll( "\\.\\.", "<ANY_PATH>" )                    // Mask JsonPath double-dots
      .replaceAll( "\\.", "\\\\." )                            // Mask JsonPath dots
      .replaceAll( "\\*", "[^\\\\.]*" )                        // Restrict JsonPath wild cards to one dot
      .replaceAll( "<ANY_INDEX>", "\\\\[[^\\\\[\\\\]]*\\\\]" ) // Restrict JsonPath array wild cards to one array
      .replaceAll( "<ANY_PATH>", ".*\\\\." )                   // Demask JsonPath double-dots to ".*\." ("any path that ends with a dot")
      + "((\\[|\\.).*)?"                                       // Avoid substring conflicts! Only structural elements ("[]" or ".") must follow.
      ;

    final Pattern pattern = Pattern.compile( regEx );
    final boolean result = pattern.matcher( path2 ).matches();

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "contains( path1=" + path1 + ", regEx=" + regEx + ", path2=" + path2 + " ) result=" + result );
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private String getObjectInfo( final Object obj )
  {
    return ( obj != null )
      ? "{ class=" + obj.getClass() + ", hashCode=" + obj.hashCode() + ", toString()=" + obj.toString() + " }"
      : "null";
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
  public static void main(String[] args)
  {
    String str = "$a.b[*].c"
      .replaceAll( "\\$", "\\\\\\$" )
      .replaceAll( "\\.", "\\\\." )
      .replaceAll( "\\[", "\\\\[" )
      .replaceAll( "\\]", "\\\\]" )
      .replaceAll( "\\*", ".*" )
      + ".*";
    System.out.println( str );
  }
*/
}
