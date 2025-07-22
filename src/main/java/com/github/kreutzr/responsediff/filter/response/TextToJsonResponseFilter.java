package com.github.kreutzr.responsediff.filter.response;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.XmlHttpResponse;

/**
 * A filter that converts Text to JSON.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>name="contentType" (default is "application/json")</li>
 * </ul>
 * <p>
 * <b>Supported inherited parameters:</b>
 * <ul>
 * <li>name="storeOriginalResponse", values=[ "true", "false" ] (default is false)</li>
 * </ul>
 * <br/>
 * The result body will look like this:
 * <pre>
 * { "body" : "<the text>" }
 * </pre>
 * or
 * <pre>
 * { "body" : null }
 * </pre>
 * <p/>
 * With the parameter "splitIntoLines" set to "true the result body will look like this:
 * <pre>
 * { "body" : { lines : [ "<1st line>", "2nd line", ... ] } }
 * </pre>
 * or
 * <pre>
 * { "body" : { lines : null } }
 * </pre>
 */
public class TextToJsonResponseFilter extends DiffResponseFilterImpl
{
  public static final String PARAMETER_NAME__SPLIT_INTO_LINES = "splitIntoLines";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( TextToJsonResponseFilter.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__CONTENT_TYPE );
    registerFilterParameterName( PARAMETER_NAME__SPLIT_INTO_LINES );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply( final XmlHttpResponse xmlHttpResponse )
  throws DiffFilterException
  {
    if( xmlHttpResponse.isBodyIsJson() ) {
      // Not intended for JSON
      LOG.debug( "Skipped because content type is JSON." );
      return;
    }

    super.apply( xmlHttpResponse );

    try {
      final boolean splitIntoLines = Converter.asBoolean(
        getFilterParameter( PARAMETER_NAME__SPLIT_INTO_LINES ),
        false
      );

      String body = splitIntoLines
        ? getBodyAsLines ( xmlHttpResponse.getBody() )
        : getCompleteBody( xmlHttpResponse.getBody() );

      xmlHttpResponse.setBody( body );
      setContentTypeHeader( xmlHttpResponse, HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON );
    }
    catch( final Exception ex ) {
      throw new DiffFilterException( ex );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String getBodyAsLines( final String body )
  {
    final StringBuilder sb = new StringBuilder( "{\"body\":{\"lines\":" );

    if( body == null ) {
      sb.append( "null}}" );
    }
    else {
      sb.append( "[" );
      try (Scanner scanner = new Scanner( body )) {
        while( scanner.hasNextLine() ) {
          sb.append( "\"" )
          .append( scanner.nextLine()
            .replace( "\"", "\\\"" ) // Mask quotes
          )
          .append( "\"" );

          if( scanner.hasNextLine() ) {
            sb.append( ", " );
          }
        }
      }
      sb.append( "]}}" );
    }

    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String getCompleteBody( final String body )
  {
    final StringBuilder sb = new StringBuilder( "{\"body\":" );
    if( body == null ) {
      sb.append( "null}" );
    }
    else {
      sb.append( "\"" )
      .append( body
        .replace( "\"", "\\\"" ) // Mask quotes
        .replace( "\n", ""     ) // Mask line breaks (required for JSON)
      )
      .append( "\"}" );
    }

    return sb.toString();
  }
}
