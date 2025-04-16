package com.github.kreutzr.responsediff.filter.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;

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
 */
public class TextToJsonResponseFilter extends DiffResponseFilterImpl
{
  private static final Logger LOG = LoggerFactory.getLogger( TextToJsonResponseFilter.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__CONTENT_TYPE );
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
      final StringBuilder sb = new StringBuilder( "{\"body\":" );
      if( xmlHttpResponse.getBody() == null ) {
        sb.append( "null}" );
      }
      else {
        sb.append( "\"" )
        .append( xmlHttpResponse.getBody().replace( "\"", "\\\"" ) ) // Mask quotes
        .append( "\"}" );
      }

      xmlHttpResponse.setBody( sb.toString() );
      setContentTypeHeader( xmlHttpResponse, HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON );
    }
    catch( final Exception ex ) {
      throw new DiffFilterException( ex );
    }
  }
}
