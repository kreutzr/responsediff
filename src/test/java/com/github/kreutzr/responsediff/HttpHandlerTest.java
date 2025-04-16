package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class HttpHandlerTest
{
   @Test
   public void testReadFileNameFromContentDispositionHeaderWorks()
   {
     assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "inline;" ) ).isEqualTo( null );
     assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "inline; filename = test.txt"     ) ).isEqualTo( "test.txt" );
     assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = test.txt"             ) ).isEqualTo( "test.txt" );
     assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = \"test.txt\""         ) ).isEqualTo( "test.txt" );
     assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = \"\""                 ) ).isEqualTo( "" );
     assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = test.txt ; more-text" ) ).isEqualTo( "test.txt" );
   }

   @Test
   public void testThatCreateServiceUrlWorks()
   {
     // Given
     final XmlRequest xmlRequest = new XmlRequest();
     xmlRequest.setEndpoint( "myEndpoint" ); // HINT: Missing leading "/"

     // When
     HttpHandler.createServiceUrl( xmlRequest, TestSetHandler.CANDIDATE, "TEST_ID", "TESTFILE_NAME", "http://my-server:1234" );

     // Then
     assertThat( xmlRequest.getEndpoint() ).isEqualTo( "http://my-server:1234/myEndpoint" );
   }

   @Test
   public void testThatCreateServiceUrlWorksForVariables()
   {
     // Given
     final XmlRequest xmlRequest = new XmlRequest();
     xmlRequest.setEndpoint( "${HEADER_LOCATION}" );

     // When
     final XmlVariables xmlVariables = new XmlVariables();
     final XmlVariable xmlVariable = new XmlVariable();
     xmlVariable.setId( "HEADER_LOCATION" );
     xmlVariable.setValue( "/asynchResults/SOME-UUID" ); // HINT: This brings its own leading "/"
     xmlVariables.getVariable().add( xmlVariable );
     xmlRequest.setVariables( xmlVariables );

     HttpHandler.createServiceUrl( xmlRequest, TestSetHandler.CANDIDATE, "TEST_ID", "TESTFILE_NAME", "http://my-server:1234" );

     // Then
     assertThat( xmlRequest.getEndpoint() ).isEqualTo( "http://my-server:1234/asynchResults/SOME-UUID" );
   }

   @Test
   public void testThatIsBodyJsonWorks()
   {
     // Given

     // When / Then
     assertThat( HttpHandler.isJsonResponse( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON     ) ).isTrue();
     assertThat( HttpHandler.isJsonResponse( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API ) ).isTrue();
     assertThat( HttpHandler.isJsonResponse( "application/text" ) ).isFalse();
   }}
