package com.github.kreutzr.responsediff.filter.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlHttpResponse;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class TextToJsonResponseFilterTest
{
  @Test
  public void testThatTextIsConvertedToJson()
  {
    try {
      // Given
      final String bodyText = "Some \"text\"";
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );
      xmlHttpResponse.setBodyIsJson( false );

      // When
      final TextToJsonResponseFilter filter = new TextToJsonResponseFilter();
      filter.setFilterParameter( TextToJsonResponseFilter.PARAMETER_NAME__CONTENT_TYPE, HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API );
      filter.apply( xmlHttpResponse );

      // Then
      String contentType = "UNKNWON";
      for( final XmlHeader xmlHeader : xmlHttpResponse.getHeaders().getHeader() ) {
        if( xmlHeader.getName().equalsIgnoreCase( HttpHandler.HEADER_NAME__CONTENT_TYPE ) ) {
          contentType = xmlHeader.getValue();
          break;
        }
      }
      assertThat( contentType ).isEqualTo( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API );
      final String json = xmlHttpResponse.getBody();
      final DocumentContext context = JsonPath.parse( json );
//      System.out.println( json );
      assertThat( (String) context.read( "$.body" ) ).isEqualTo( bodyText );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatNullIsConvertedToJson()
  {
    try {
      // Given
      final String bodyText = null;
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );

      // When
      final TextToJsonResponseFilter filter = new TextToJsonResponseFilter();
      filter.apply( xmlHttpResponse );

      // Then
      String contentType = "UNKNWON";
      for( final XmlHeader xmlHeader : xmlHttpResponse.getHeaders().getHeader() ) {
        if( xmlHeader.getName().equalsIgnoreCase( HttpHandler.HEADER_NAME__CONTENT_TYPE ) ) {
          contentType = xmlHeader.getValue();
          break;
        }
      }
      assertThat( contentType ).isEqualTo( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON );
      final String json = xmlHttpResponse.getBody();
//      System.out.println( json );
      assertThat( json ).isEqualTo( "{\"body\":null}" );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }
}
