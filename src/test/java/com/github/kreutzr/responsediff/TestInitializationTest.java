package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.github.kreutzr.responsediff.base.TestBase;

public class TestInitializationTest extends TestBase
{
  @Test
  public void testThatVariablesAreInherited() throws Exception
  {
    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_testinitialization/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      final XmlTestSet xmlTestSet_00    = setUp.getTestSet().get( 0 );
      final XmlTestSet xmlTestSet_00_01 = xmlTestSet_00.getTestSet().get( 0 );

      assertThat( xmlTestSet_00   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_00_01.getTest() ).hasSize( 1 );

      // -----------------------------------------------------------------------------------------------------

      final XmlTest test_A = xmlTestSet_00   .getTest().get( 0 );
      final XmlTest test_B = xmlTestSet_00_01.getTest().get( 0 );

      assertThat( test_A.getId() ).isEqualTo( "TestSet 00 / Test A" );
      assertThat( test_B.getId() ).isEqualTo( "TestSet 00 / TestSet 00-01 / Test B" );

      // -----------------------------------------------------------------------------------------------------

      {
        final XmlVariables setUp_Variables         = setUp           .getVariables();
        final XmlVariables testSet_00_Variables    = xmlTestSet_00   .getVariables();
        final XmlVariables testSet_00_01_Variables = xmlTestSet_00_01.getVariables();
        final XmlVariables test_A_Variables        = test_A          .getVariables();
        final XmlVariables test_B_Variables        = test_B          .getVariables();

        assertThat( setUp_Variables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( setUp_Variables, "var-00" ) ).isEqualTo( "AAA" );

        assertThat( testSet_00_Variables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( testSet_00_Variables, "var-00" ) ).isEqualTo( "AAA" );

        assertThat( testSet_00_01_Variables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( testSet_00_01_Variables, "var-00" ) ).isEqualTo( "DDD" );

        assertThat( test_A_Variables.getVariable() ).hasSize( 2 );
        assertThat( getVariableValue( test_A_Variables, "var-00" ) ).isEqualTo( "BBB" );
        assertThat( getVariableValue( test_A_Variables, "var-01" ) ).isEqualTo( "CCC" );

        assertThat( test_B_Variables.getVariable() ).hasSize( 2 );
        assertThat( getVariableValue( test_B_Variables, "var-00" ) ).isEqualTo( "DDD" );
        assertThat( getVariableValue( test_B_Variables, "var-01" ) ).isEqualTo( "EEE" );
      }

      // -----------------------------------------------------------------------------------------------------

      {
        final XmlVariables testSet_00_RequestVariables     = xmlTestSet_00   .getRequest().getVariables();
        final XmlVariables testSet_00_01_RequestVariables  = xmlTestSet_00_01.getRequest().getVariables();
        final XmlVariables test_A_RequestVariables         = test_A          .getRequest().getVariables();
        final XmlVariables test_B_RequestVariables         = test_B          .getRequest().getVariables();

        assertThat( testSet_00_RequestVariables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( testSet_00_RequestVariables, "var-00" ) ).isEqualTo( "AAA" );

        assertThat( testSet_00_01_RequestVariables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( testSet_00_01_RequestVariables, "var-00" ) ).isEqualTo( "DDD" );

        assertThat( test_A_RequestVariables.getVariable() ).hasSize( 2 );
        assertThat( getVariableValue( test_A_RequestVariables, "var-00" ) ).isEqualTo( "BBB" );
        assertThat( getVariableValue( test_A_RequestVariables, "var-01" ) ).isEqualTo( "CCC" );

        assertThat( test_B_RequestVariables.getVariable() ).hasSize( 2 );
        assertThat( getVariableValue( test_B_RequestVariables, "var-00" ) ).isEqualTo( "FFF" );
        assertThat( getVariableValue( test_B_RequestVariables, "var-01" ) ).isEqualTo( "EEE" );
      }

      // -----------------------------------------------------------------------------------------------------

      {
        final XmlVariables testSet_00_ResponseVariables    = xmlTestSet_00   .getResponse().getVariables();
        final XmlVariables testSet_00_01_ResponseVariables = xmlTestSet_00_01.getResponse().getVariables();
        final XmlVariables test_A_ResponseVariables        = test_A          .getResponse().getVariables();
        final XmlVariables test_B_ResponseVariables        = test_B          .getResponse().getVariables();

        assertThat( testSet_00_ResponseVariables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( testSet_00_ResponseVariables, "var-99" ) ).isEqualTo( "XXX" );

        assertThat( testSet_00_01_ResponseVariables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( testSet_00_01_ResponseVariables, "var-99" ) ).isEqualTo( "XXX" );

        assertThat( test_A_ResponseVariables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( test_A_ResponseVariables, "var-99" ) ).isEqualTo( "XXX" );

        assertThat( test_B_ResponseVariables.getVariable() ).hasSize( 1 );
        assertThat( getVariableValue( test_B_ResponseVariables, "var-99" ) ).isEqualTo( "XXX" );
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatResponseFiltersAreInherited() throws Exception
  {
    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_responseInheritance/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      assertThat( setUp.getTestSet() ).hasSize( 2 );
      final XmlTestSet xmlTestSet_00    = setUp.getTestSet().get( 0 );
      final XmlTestSet xmlTestSet_01    = setUp.getTestSet().get( 1 );
      assertThat( xmlTestSet_00.getTestSet() ).hasSize( 1 );
      assertThat( xmlTestSet_01.getTestSet() ).hasSize( 0 );
      final XmlTestSet xmlTestSet_00_01 = xmlTestSet_00.getTestSet().get( 0 );
      assertThat( xmlTestSet_00_01.getTestSet() ).hasSize( 0 );

      assertThat( xmlTestSet_00   .getId() ).isEqualTo( "TestSet 00" );
      assertThat( xmlTestSet_00   .getResponse().getFilters().getFilter() ).hasSize( 1 );
      assertThat( xmlTestSet_00   .getResponse().getFilters().getFilter().get( 0 ).getId() ).isEqualTo( "xxx" );
      assertThat( xmlTestSet_01   .getId() ).isEqualTo( "TestSet 01" );
      assertThat( xmlTestSet_01   .getResponse().getFilters() ).isNull();
      assertThat( xmlTestSet_00_01.getId() ).isEqualTo( "TestSet 00 / TestSet 00-01" );
      assertThat( xmlTestSet_00_01.getResponse().getFilters().getFilter() ).hasSize( 2 );
      assertThat( xmlTestSet_00_01.getResponse().getFilters().getFilter().get( 0 ).getId() ).isEqualTo( "xxx" );
      assertThat( xmlTestSet_00_01.getResponse().getFilters().getFilter().get( 1 ).getId() ).isEqualTo( "zzz" );

      assertThat( xmlTestSet_00   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_00_01.getTest() ).hasSize( 2 );

      assertThat( xmlTestSet_00   .getTest().get( 0 ).getId() ).isEqualTo( "TestSet 00 / Test A" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getFilters().getFilter() ).hasSize( 2 );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getFilters().getFilter().get( 0 ).getId() ).isEqualTo( "xxx" ); // NOTE: The order is NOT alphabetical but the filter definition order
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getFilters().getFilter().get( 1 ).getId() ).isEqualTo( "aaa" );

      assertThat( xmlTestSet_01   .getTest().get( 0 ).getId() ).isEqualTo( "TestSet 01 / Test B" );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getFilters().getFilter() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getFilters().getFilter().get( 0 ).getId() ).isEqualTo( "bbb" );

      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getId() ).isEqualTo( "TestSet 00 / TestSet 00-01 / Test C" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getFilters().getFilter() ).hasSize( 3 );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getFilters().getFilter().get( 0 ).getId() ).isEqualTo( "xxx" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getFilters().getFilter().get( 1 ).getId() ).isEqualTo( "zzz" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getFilters().getFilter().get( 2 ).getId() ).isEqualTo( "ccc" );
      assertThat( xmlTestSet_00_01.getTest().get( 1 ).getId() ).isEqualTo( "TestSet 00 / TestSet 00-01 / Test D" );
      assertThat( xmlTestSet_00_01.getTest().get( 1 ).getResponse().getFilters().getFilter() ).hasSize( 0 );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatResponseIgnoresAreInherited() throws Exception
  {
    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_responseInheritance/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      assertThat( setUp.getTestSet() ).hasSize( 2 );
      final XmlTestSet xmlTestSet_00    = setUp.getTestSet().get( 0 );
      final XmlTestSet xmlTestSet_01    = setUp.getTestSet().get( 1 );
      assertThat( xmlTestSet_00.getTestSet() ).hasSize( 1 );
      assertThat( xmlTestSet_01.getTestSet() ).hasSize( 0 );
      final XmlTestSet xmlTestSet_00_01 = xmlTestSet_00.getTestSet().get( 0 );
      assertThat( xmlTestSet_00_01.getTestSet() ).hasSize( 0 );

      assertThat( xmlTestSet_00   .getId() ).isEqualTo( "TestSet 00" );
      assertThat( xmlTestSet_00   .getResponse().getIgnore() ).hasSize( 1 );
      assertThat( xmlTestSet_00   .getResponse().getIgnore().get( 0 ).getHeader() ).isEqualTo( "XXX" );
      assertThat( xmlTestSet_01   .getId() ).isEqualTo( "TestSet 01" );
      assertThat( xmlTestSet_01   .getResponse().getIgnore() ).hasSize( 0 );
      assertThat( xmlTestSet_00_01.getId() ).isEqualTo( "TestSet 00 / TestSet 00-01" );
      assertThat( xmlTestSet_00_01.getResponse().getIgnore() ).hasSize( 2 );
      assertThat( xmlTestSet_00_01.getResponse().getIgnore().get( 0 ).getHeader() ).isEqualTo( "XXX" );
      assertThat( xmlTestSet_00_01.getResponse().getIgnore().get( 1 ).getHeader() ).isEqualTo( "ZZZ" );

      assertThat( xmlTestSet_00   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_00_01.getTest() ).hasSize( 2 );

      assertThat( xmlTestSet_00   .getTest().get( 0 ).getId() ).isEqualTo( "TestSet 00 / Test A" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getIgnore() ).hasSize( 2 );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getIgnore().get( 0 ).getHeader() ).isEqualTo( "AAA" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getIgnore().get( 1 ).getHeader() ).isEqualTo( "XXX" );

      assertThat( xmlTestSet_01   .getTest().get( 0 ).getId() ).isEqualTo( "TestSet 01 / Test B" );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getIgnore() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getIgnore().get( 0 ).getHeader() ).isEqualTo( "BBB" );

      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getId() ).isEqualTo( "TestSet 00 / TestSet 00-01 / Test C" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getIgnore() ).hasSize( 4 );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getIgnore().get( 0 ).getHeader() ).isEqualTo( "CCC" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getIgnore().get( 1 ).getHeader() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getIgnore().get( 2 ).getHeader() ).isEqualTo( "XXX" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getIgnore().get( 3 ).getHeader() ).isEqualTo( "ZZZ" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatResponseExpectedAreInherited() throws Exception
  {
    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );
//      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ );

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_responseInheritance/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      assertThat( setUp.getTestSet() ).hasSize( 2 );
      final XmlTestSet xmlTestSet_00    = setUp.getTestSet().get( 0 );
      final XmlTestSet xmlTestSet_01    = setUp.getTestSet().get( 1 );
      assertThat( xmlTestSet_00.getTestSet() ).hasSize( 1 );
      assertThat( xmlTestSet_01.getTestSet() ).hasSize( 0 );
      final XmlTestSet xmlTestSet_00_01 = xmlTestSet_00.getTestSet().get( 0 );
      assertThat( xmlTestSet_00_01.getTestSet() ).hasSize( 0 );

      assertThat( xmlTestSet_00   .getId() ).isEqualTo( "TestSet 00" );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getHttpStatus().getValue() ).isEqualTo( 200 );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getHeaders().getHeader() ).hasSize( 1 );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getHeaders().getHeader().get( 0 ).getName()  ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getValues().getValue() ).hasSize( 1 );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getValues().getValue().get( 0 ).getPath() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00   .getResponse().getExpected().getValues().getValue().get( 0 ).getValue() ).isEqualTo( "MMM" );

      assertThat( xmlTestSet_01   .getId() ).isEqualTo( "TestSet 01" );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getHttpStatus().getValue() ).isEqualTo( 202 );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getHeaders().getHeader() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getHeaders().getHeader().get( 0 ).getName()  ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getValues().getValue() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getValues().getValue().get( 0 ).getPath() ).isEqualTo( "NNN" );
      assertThat( xmlTestSet_01   .getResponse().getExpected().getValues().getValue().get( 0 ).getValue() ).isEqualTo( "NNN" );

      assertThat( xmlTestSet_00_01.getId() ).isEqualTo( "TestSet 00 / TestSet 00-01" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHttpStatus().getValue() ).isEqualTo( 203 );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader() ).hasSize( 3 );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader().get( 0 ).getName()  ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader().get( 1 ).getName()  ).isEqualTo( "FFF" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader().get( 1 ).getValue() ).isEqualTo( "FFFFFF" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader().get( 2 ).getName()  ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getHeaders().getHeader().get( 2 ).getValue() ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getValues().getValue() ).hasSize( 2 );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getValues().getValue().get( 0 ).getPath() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getValues().getValue().get( 0 ).getValue() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getValues().getValue().get( 1 ).getPath() ).isEqualTo( "NNN" );
      assertThat( xmlTestSet_00_01.getResponse().getExpected().getValues().getValue().get( 1 ).getValue() ).isEqualTo( "NNN" );

      assertThat( xmlTestSet_00   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_00_01.getTest() ).hasSize( 2 );

      assertThat( xmlTestSet_00   .getTest().get( 0 ).getId() ).isEqualTo( "TestSet 00 / Test A" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getHttpStatus().getValue() ).isEqualTo( 201 );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader() ).hasSize( 2 );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 0 ).getName()  ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 1 ).getName()  ).isEqualTo( "FFF" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 1 ).getValue() ).isEqualTo( "FFF" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue() ).hasSize( 2 );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 0 ).getPath()  ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 0 ).getValue() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 1 ).getPath()  ).isEqualTo( "MMM" );   // We allow checking more than one criteria per path (e.g. x > 1 and x < 2)
      assertThat( xmlTestSet_00   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 1 ).getValue() ).isEqualTo( "MMMMMM" );

      assertThat( xmlTestSet_01   .getTest().get( 0 ).getId() ).isEqualTo( "TestSet 01 / Test B" );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getHttpStatus().getValue() ).isEqualTo( 202 );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 0 ).getName()  ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 0 ).getPath() ).isEqualTo( "NNN" );
      assertThat( xmlTestSet_01   .getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 0 ).getValue() ).isEqualTo( "NNN" );

      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getId() ).isEqualTo( "TestSet 00 / TestSet 00-01 / Test C" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHttpStatus().getValue() ).isEqualTo( 204 );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader() ).hasSize( 4 );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 0 ).getName()  ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "EEE" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 1 ).getName()  ).isEqualTo( "FFF" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 1 ).getValue() ).isEqualTo( "FFFFFF" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 2 ).getName()  ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 2 ).getValue() ).isEqualTo( "GGG" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 3 ).getName()  ).isEqualTo( "HHH" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getHeaders().getHeader().get( 3 ).getValue() ).isEqualTo( "HHH" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue() ).hasSize( 3 );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 0 ).getPath() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 0 ).getValue() ).isEqualTo( "MMM" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 1 ).getPath()  ).isEqualTo( "NNN" );   // We allow checking more than one criteria per path (e.g. x > 1 and x < 2)
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 1 ).getValue() ).isEqualTo( "NNN" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 2 ).getPath() ).isEqualTo( "NNN" );
      assertThat( xmlTestSet_00_01.getTest().get( 0 ).getResponse().getExpected().getValues().getValue().get( 2 ).getValue() ).isEqualTo( "NNNNNN" );
    }
  }
}
