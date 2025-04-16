package com.github.kreutzr.responsediff.filter.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.XmlToJson;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;
import com.github.kreutzr.responsediff.tools.Converter;

import com.github.kreutzr.responsediff.XmlHttpResponse;

/**
 * A filter that converts XML to JSON. It handles attributes and multi content by default. See the following parameters to adjust the filter behavior.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>name="contentType" (default is "application/json")</li>
 * <li>name="preserveOrder",  values=[ "true", "false" ] (default is true)</li>
 * <li>name="skipAttributes", values=[ "true", "false" ] (default is false)</li>
 * </ul>
 * <p>
 * <b>Supported inherited parameters:</b>
 * <ul>
 * <li>name="storeOriginalResponse",  values=[ "true", "false" ] (default is false)</li>
 * </ul>
 *
 * <b>Examples</b>:
 * The given XML is transformed as follows.
 * <pre>
 * &lt;a type="A"&gt;&lt;b name="B"&gt;bbb&lt;/b&gt;&lt;c&gt;ccc&lt;/c&gt;&lt;/a&gt;
 * </pre>
 * <ul>
 * <li>(preserverOrder=true,  skipAttributes=false) :
 *   <pre>{"a":{"@type":"A","#value":[{"b":{"@name":"B","#value":"bbb"}},{"c":{"#value":"ccc"}}]}}</pre>
 * </li>
 * <li>(preserverOrder=false, skipAttributes=false) :
 *   <pre>{"a":{"@type":"A","#value":{"b":{"@name":"B","#value":"bbb"},"c":{"#value":"ccc"}}}}</pre>
 * </li>
 * <li>(preserverOrder=true,  skipAttributes=true ) :
 *   <pre>{"a":[{"b":"bbb"},{"c":"ccc"}]}</pre>
 * </li>
 * <li>(preserverOrder=false, skipAttributes=true ) :
 *   <pre>{"a":{"b":"bbb","c":"ccc"}}</pre>
 * </li>
 * </ul>
 */
public class XmlToJsonResponseFilter extends DiffResponseFilterImpl
{
  public static final String PARAMETER_NAME__PRESERVE_ORDER  = "preserveOrder";
  public static final String PARAMETER_NAME__SKIP_ATTRIBUTES = "skipAttributes";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( XmlToJsonResponseFilter.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__CONTENT_TYPE );
    registerFilterParameterName( PARAMETER_NAME__PRESERVE_ORDER );
    registerFilterParameterName( PARAMETER_NAME__SKIP_ATTRIBUTES );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply( final XmlHttpResponse xmlHttpResponse ) throws DiffFilterException
  {
    if( xmlHttpResponse.isBodyIsJson() ) {
      // Not intended for JSON
      LOG.debug( "Skipped because content type is JSON." );
      return;
    }

    super.apply( xmlHttpResponse );

    try {
      final XmlToJson xmlToJson = new XmlToJson();
      xmlToJson.setPreserveOrder ( Converter.asBoolean( getFilterParameter( PARAMETER_NAME__PRESERVE_ORDER  ), true  ) );
      xmlToJson.setSkipAttributes( Converter.asBoolean( getFilterParameter( PARAMETER_NAME__SKIP_ATTRIBUTES ), false ) );

      xmlHttpResponse.setBody( xmlToJson.toJson( xmlHttpResponse.getBody() ) );
      setContentTypeHeader( xmlHttpResponse, HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON );
    }
    catch( final Exception ex ) {
      throw new DiffFilterException( ex );
    }
  }
}
