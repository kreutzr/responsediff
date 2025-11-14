package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.github.kreutzr.responsediff.base.TestBase;

public class ResponseVariablesTest extends TestBase
{
  @Test
  public void testThatResponseVariablesAreHandledServerInstanceSpecific() throws Exception
  {
    final String endpoint = "my-endpoint";

    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      // Step 1
      initResponseMock( httpHandler, CANDIDATE_URL + endpoint, 200, "{ \"key\": \"CANDIDATE_VALUE\" }", xmlHeaders_ );
      initResponseMock( httpHandler, REFERENCE_URL + endpoint, 200, "{ \"key\": \"REFERENCE_VALUE\" }", xmlHeaders_ );
      initResponseMock( httpHandler, CONTROL_URL   + endpoint, 200, "{ \"key\": \"CONTROL_VALUE\" }",   xmlHeaders_ );
      // Step 2
      initResponseMock( httpHandler, CANDIDATE_URL + endpoint + "?step1_key=CANDIDATE_VALUE", 200, "{}", xmlHeaders_ );
      initResponseMock( httpHandler, REFERENCE_URL + endpoint + "?step1_key=REFERENCE_VALUE", 200, "{}", xmlHeaders_ );
      initResponseMock( httpHandler, CONTROL_URL   + endpoint + "?step1_key=CONTROL_VALUE",   200, "{}", xmlHeaders_ );
      // Step 4
      final String step4Response = "{ \"key00\" : \"aaa ZZZ bbb\", \"key01\" : \"false\", \"key02\" : false, \"array\" : [ { \"key03\" : \"a\", \"key04\" : \"false\", \"key05\" : false, \"key06\" : null } ] }";
      initResponseMock( httpHandler, CANDIDATE_URL + endpoint + "?step4_key=CANDIDATE_VALUE", 200, step4Response, xmlHeaders_ );
      initResponseMock( httpHandler, REFERENCE_URL + endpoint + "?step4_key=REFERENCE_VALUE", 200, step4Response, xmlHeaders_ );
      initResponseMock( httpHandler, CONTROL_URL   + endpoint + "?step4_key=CONTROL_VALUE",   200, step4Response, xmlHeaders_ );
      // Step 5
      final String step5Response = "";
      initResponseMock( httpHandler, CANDIDATE_URL + endpoint + "?step5_key=CANDIDATE_VALUE", 200, step5Response, xmlHeaders_ );
      initResponseMock( httpHandler, REFERENCE_URL + endpoint + "?step5_key=REFERENCE_VALUE", 200, step5Response, xmlHeaders_ );
      initResponseMock( httpHandler, CONTROL_URL   + endpoint + "?step5_key=CONTROL_VALUE",   200, step5Response, xmlHeaders_ );

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_responseVariables/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      final XmlTestSet xmlTestSet = setUp.getTestSet().get( 0 );
      assertThat( xmlTestSet.getTest() ).hasSize( 5 );

      final XmlTest step1 = xmlTestSet.getTest().get( 0 );
      final XmlTest step2 = xmlTestSet.getTest().get( 1 );
      final XmlTest step3 = xmlTestSet.getTest().get( 2 );
      final XmlTest step4 = xmlTestSet.getTest().get( 3 );
      final XmlTest step5 = xmlTestSet.getTest().get( 4 );

      final List< XmlVariable > testSetVariables = xmlTestSet.getVariables().getVariable();
      final List< XmlVariable > step1Variables   = step1.getVariables().getVariable();
      final List< XmlVariable > step2Variables   = step2.getVariables().getVariable();
      final List< XmlVariable > step3Variables   = step3.getVariables().getVariable();

      // TestSet holds server instance specific variables
      assertThat( testSetVariables ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "candidate.STEP1_KEY" ) && xmlVariable.getValue().equals( "CANDIDATE_VALUE" ) );
      assertThat( testSetVariables ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "reference.STEP1_KEY" ) && xmlVariable.getValue().equals( "REFERENCE_VALUE" ) );
      assertThat( testSetVariables ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "control.STEP1_KEY"   ) && xmlVariable.getValue().equals( "CONTROL_VALUE" ) );

      // Test from which a response variable was read does NOT have the response variable set
      assertThat( step1Variables   ).noneMatch( xmlVariable -> xmlVariable.getId().equals( "STEP1_KEY" ) );
      assertThat( step1Variables   ).noneMatch( xmlVariable -> xmlVariable.getId().equals( "candidate.STEP1_KEY" ) );
      assertThat( step1Variables   ).noneMatch( xmlVariable -> xmlVariable.getId().equals( "reference.STEP1_KEY" ) );
      assertThat( step1Variables   ).noneMatch( xmlVariable -> xmlVariable.getId().equals( "control.STEP1_KEY"   ) );

      // Following tests have the response variable set
      assertThat( step2Variables   ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "candidate.STEP1_KEY" ) && xmlVariable.getValue().equals( "CANDIDATE_VALUE" ) );
      assertThat( step2Variables   ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "reference.STEP1_KEY" ) && xmlVariable.getValue().equals( "REFERENCE_VALUE" ) );
      assertThat( step2Variables   ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "control.STEP1_KEY"   ) && xmlVariable.getValue().equals( "CONTROL_VALUE"   ) );
      assertThat( step3Variables   ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "candidate.STEP1_KEY" ) && xmlVariable.getValue().equals( "CANDIDATE_VALUE" ) );
      assertThat( step3Variables   ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "reference.STEP1_KEY" ) && xmlVariable.getValue().equals( "REFERENCE_VALUE" ) );
      assertThat( step3Variables   ).anyMatch ( xmlVariable -> xmlVariable.getId().equals( "control.STEP1_KEY"   ) && xmlVariable.getValue().equals( "CONTROL_VALUE"   ) );

      assertThat( step3.getAnalysis().getMessages().getMessage() ).hasSize( 1 );
      assertThat( step3.getAnalysis().getMessages().getMessage().get( 0 ).getLevel() ).isEqualTo( XmlLogLevel.ERROR );
      assertThat( step3.getAnalysis().getMessages().getMessage().get( 0 ).getPath()  ).isEqualTo( "$.DOES_NOT_EXIST" );
      assertThat( step3.getAnalysis().getMessages().getMessage().get( 0 ).getValue() ).isEqualTo( "Path expected: exists but was: Not exists" );

      // Check feature evaluation
      assertThat( step4.getAnalysis().getMessages().getMessage() ).hasSize( 3 );
      // Note: Messages are sorted by path (see TestSetHandler.handleAnalysis)
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 0 ).getLevel() ).isEqualTo( XmlLogLevel.FATAL );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 0 ).getPath()  ).isEqualTo( "$.array[?(@.key03=='a')].key04#0" );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 0 ).getValue() ).startsWith( "class java.lang.String cannot be cast to class java.lang.Boolean" );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 1 ).getLevel() ).isEqualTo( XmlLogLevel.FATAL );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 1 ).getPath()  ).isEqualTo( "$.key01" );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 1 ).getValue() ).startsWith( "class java.lang.String cannot be cast to class java.lang.Boolean" );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 2 ).getLevel() ).isEqualTo( XmlLogLevel.WARN );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 2 ).getPath()  ).isEqualTo( "$.doesNotExist" );
      assertThat( step4.getAnalysis().getMessages().getMessage().get( 2 ).getValue() ).isEqualTo( "Object expected: (trim=false, ignoreCase=false) true but was: null" );

      // Check feature evaluation with empty body
      assertThat( step5.getAnalysis().getMessages().getMessage() ).hasSize( 1 );
      assertThat( step5.getAnalysis().getMessages().getMessage().get( 0 ).getLevel() ).isEqualTo( XmlLogLevel.ERROR );
      assertThat( step5.getAnalysis().getMessages().getMessage().get( 0 ).getPath()  ).isEqualTo( "$" );
      assertThat( step5.getAnalysis().getMessages().getMessage().get( 0 ).getValue() ).isEqualTo( "Values expected but body is empty or not JSON." );
    }
  }
}
