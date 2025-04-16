package com.github.kreutzr.responsediff.filter.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.filter.response.XmlToJsonResponseFilter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlHttpResponse;

public class XmlToJsonResponseFilterTest
{
  @Test
  public void testThatXmlHttpResponseIsConvertedToJson()
  {
    try {
      // Given
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( "<xml><a>A</a><b>B</b>C</xml>" );
      xmlHttpResponse.setBodyIsJson( false );

      // When
      final XmlToJsonResponseFilter filter = new XmlToJsonResponseFilter();
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
      final DocumentContext context = JsonPath.parse( json );
//      LOG.debug( json );
      assertThat( (String) context.read( "$.xml.#value[0].a.#value" ) ).isEqualTo( "A" );
      assertThat( (String) context.read( "$.xml.#value[1].b.#value" ) ).isEqualTo( "B" );
      assertThat( (String) context.read( "$.xml.#value[2].#text"    ) ).isEqualTo( "C" );

    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
  @Test
  public void testThatSoapRequestsCanBeHandled()
  {
    try {
      // Given
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header /><SOAP-ENV:Body><SOAP-ENV:Fault><faultcode>SOAP-ENV:Server</faultcode><faultstring xml:lang=\"en\">400 {\"warningMessage\":\"&lt;html&gt;Input value for 'Fahrzeug-Ordernummer' does not match the reqired text-pattern&lt;br&gt;Details: [0-9]{8}&lt;/html&gt;\"}</faultstring><detail><message>400 {\"warningMessage\":\"&lt;html&gt;Input value for 'Fahrzeug-Ordernummer' does not match the reqired text-pattern&lt;br&gt;Details: [0-9]{8}&lt;/html&gt;\"}</message></detail></SOAP-ENV:Fault></SOAP-ENV:Body></SOAP-ENV:Envelope>" );
      xmlHttpResponse.setBodyIsJson( false );

      // When
      final XmlToJsonResponseFilter filter = new XmlToJsonResponseFilter();
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

      final JsonNode node = JsonHelper.provideObjectMapper().readTree( json );

      System.out.println( node.toPrettyString() );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }
*/
}
