package com.github.kreutzr.responsediff.filter;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.tools.CloneHelper;
import com.github.kreutzr.responsediff.tools.Converter;

import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHttpResponse;

/**
 * A filter base implementation. It allows to store the original HTTP response before it is manipulated by the inheriting filter class.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>name="storeOriginalResponse",  values=[ "true", "false" ] (default is false)</li>
 * </ul>
 */
public abstract class DiffResponseFilterImpl extends DiffFilterImpl implements DiffResponseFilter
{
  public static final String PARAMETER_NAME__CONTENT_TYPE   = "contentType"; // This must be implemented by inheriting filter classes.
  public static final String PARAMETER_NAME__STORE_ORIGINAL = "storeOriginalResponse";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__STORE_ORIGINAL );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply( final XmlHttpResponse xmlHttpResponse )
  throws DiffFilterException
  {
    if( Converter.asBoolean( getFilterParameter( PARAMETER_NAME__STORE_ORIGINAL ), false ) ) {
      XmlHttpResponse original = CloneHelper.deepCopyJAXB( xmlHttpResponse, XmlHttpResponse.class );
      xmlHttpResponse.setOriginalResponse( original );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the content type header of the given XmlHttpResponse.
   * @param xmlHttpResponse The XmlHttpResponse to use. May be null.
   * @param contentTypeFallback The content type fallback. May be null.
   */
  public void setContentTypeHeader( final XmlHttpResponse xmlHttpResponse, final String contentTypeFallback )
  {
    if( xmlHttpResponse == null ) {
      return;
    }

    final String contentType = Converter.asString(
      getFilterParameter( PARAMETER_NAME__CONTENT_TYPE ),
      contentTypeFallback
    );

    // Update header
    XmlHeader xmlHeader = HttpHandler.getHeader( HttpHandler.HEADER_NAME__CONTENT_TYPE, xmlHttpResponse.getHeaders().getHeader() );
    if( xmlHeader == null ) {
      xmlHeader = new XmlHeader();
      xmlHeader.setName( HttpHandler.HEADER_NAME__CONTENT_TYPE );

      xmlHttpResponse.getHeaders().getHeader().add( xmlHeader );
    }
    xmlHeader.setValue( contentType );
  }
}