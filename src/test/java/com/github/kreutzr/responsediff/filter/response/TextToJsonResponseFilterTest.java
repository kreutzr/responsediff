package com.github.kreutzr.responsediff.filter.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.JsonPathHelper;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlHttpResponse;

public class TextToJsonResponseFilterTest
{
  @Test
  public void testThatTextIsConvertedToJsonAsCompleteBody()
  {
    try {
      // Given
      final String bodyText = "Some \"text\"\nin multiple lines.";
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );
      xmlHttpResponse.setBodyIsJson( false );

      // When
      final TextToJsonResponseFilter filter = new TextToJsonResponseFilter();
      filter.setFilterParameter( TextToJsonResponseFilter.PARAMETER_NAME__CONTENT_TYPE, HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API );
      filter.apply( xmlHttpResponse );

      // Then
      String contentType = "UNKNOWN";
      for( final XmlHeader xmlHeader : xmlHttpResponse.getHeaders().getHeader() ) {
        if( xmlHeader.getName().equalsIgnoreCase( HttpHandler.HEADER_NAME__CONTENT_TYPE ) ) {
          contentType = xmlHeader.getValue();
          break;
        }
      }
      assertThat( contentType ).isEqualTo( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API );
      final String json = xmlHttpResponse.getBody();
//      System.out.println( json );

      final JsonPathHelper jph = new JsonPathHelper( json );

      assertThat( jph.getValue( "$.body" ) ).isEqualTo( bodyText.replace( "\n", "" ) ); // NOTE: Line breaks are consumed by the TextToJsonResponseFilter.
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @SuppressWarnings("unchecked")
  @Test
  public void testThatTextIsConvertedToJsonAsBodyLines()
  {
    try {
      // Given
      final String bodyText = "Some \"text\"\nin multiple lines.";
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );
      xmlHttpResponse.setBodyIsJson( false );

      // When
      final TextToJsonResponseFilter filter = new TextToJsonResponseFilter();
      filter.setFilterParameter( TextToJsonResponseFilter.PARAMETER_NAME__CONTENT_TYPE, HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API );
      filter.setFilterParameter( TextToJsonResponseFilter.PARAMETER_NAME__SPLIT_INTO_LINES, "true" );
      filter.apply( xmlHttpResponse );

      // Then
      String contentType = "UNKNOWN";
      for( final XmlHeader xmlHeader : xmlHttpResponse.getHeaders().getHeader() ) {
        if( xmlHeader.getName().equalsIgnoreCase( HttpHandler.HEADER_NAME__CONTENT_TYPE ) ) {
          contentType = xmlHeader.getValue();
          break;
        }
      }
      assertThat( contentType ).isEqualTo( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API );
      final String json = xmlHttpResponse.getBody();
//      System.out.println( json );

      final JsonPathHelper jph = new JsonPathHelper( json );

      assertThat( ( (List< Object >)jph.getValue( "$.body.lines" ) ) ).hasSize( 2 );
      assertThat( jph.getValue( "$.body.lines[0]" ) ).isEqualTo( "Some \"text\"" );
      assertThat( jph.getValue( "$.body.lines[1]" ) ).isEqualTo( "in multiple lines." );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatNullIsConvertedToJsonAsCompleteBody()
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

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatNullIsConvertedToJsonAsBodyLines()
  {
    try {
      // Given
      final String bodyText = null;
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );

      // When
      final TextToJsonResponseFilter filter = new TextToJsonResponseFilter();
      filter.setFilterParameter( TextToJsonResponseFilter.PARAMETER_NAME__SPLIT_INTO_LINES, "true" );
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

      assertThat( json ).isEqualTo( "{\"body\":{\"lines\":null}}" );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }
}
