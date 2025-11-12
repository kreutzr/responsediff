package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.github.kreutzr.responsediff.base.TestBase;

public class ResponseWithJsonHeaderTest extends TestBase
{
  @Test
  public void testThatTestingResponsesWithJsonHeaderWorks() throws Exception
  {
    final String endpoint = "my-endpoint";

    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      final XmlHeader headerWithJsonValue = new XmlHeader();
      headerWithJsonValue.setName( "x-duration" );
      headerWithJsonValue.setValue( "{\"duration\":\"PT0.05S\"}" );

      xmlHeaders_.getHeader().add( headerWithJsonValue );
      initResponseMock( httpHandler, CANDIDATE_URL + endpoint, 200, "{}", xmlHeaders_ );
      initResponseMock( httpHandler, REFERENCE_URL + endpoint, 200, "{}", xmlHeaders_ );
      initResponseMock( httpHandler, CONTROL_URL   + endpoint, 200, "{}", xmlHeaders_ );

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_responseWithJsonHeader/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      final XmlTestSet xmlTestSet = setUp.getTestSet().get( 0 );
      final XmlTest xmlTest = xmlTestSet.getTest().get( 0 );
      final XmlAnalysis xmlAnalysis = xmlTest.getAnalysis();

      // TestSet holds server instance specific variables
      assertThat( xmlAnalysis.getMessages().getMessage() ).hasSize( 1 );
      assertThat( xmlAnalysis.getMessages().getMessage().get( 0 ).getLevel() ).isEqualTo( XmlLogLevel.ERROR );
      assertThat( xmlAnalysis.getMessages().getMessage().get( 0 ).getPath()  ).isEqualTo( "$.headers.x-duration.duration" );
      assertThat( xmlAnalysis.getMessages().getMessage().get( 0 ).getValue() ).isEqualTo( "Duration expected: PT0.02S but was: PT0.05S epsilon was: PT0.001S" );
    }
  }
}
