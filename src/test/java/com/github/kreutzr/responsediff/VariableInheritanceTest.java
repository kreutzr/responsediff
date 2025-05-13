package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.github.kreutzr.responsediff.base.TestBase;

public class VariableInheritanceTest extends TestBase
{
  @Test
  public void testThatVariablesAreInherited() throws Exception
  {
    try( MockedStatic< HttpHandler > httpHandler = Mockito.mockStatic( HttpHandler.class, CALLS_REAL_METHODS ) ) {
      // ==========================
      // Given
      // ==========================
      initResponseMock( httpHandler, null, 200, "{}", xmlHeaders_ ); // null = any serviceURL

      // ==========================
      // When
      // ==========================
      responseDiff_.setXmlFilePath( rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_variableInheritance/setup.xml" );
      responseDiff_.runLocalTests();

      // ==========================
      // Then
      // ==========================
      final XmlResponseDiffSetup setUp = responseDiff_.getTestSetup();
      final XmlTestSet xmlTestSet_00    = setUp.getTestSet().get( 0 );
      final XmlTestSet xmlTestSet_01    = setUp.getTestSet().get( 1 );
      final XmlTestSet xmlTestSet_00_01 = xmlTestSet_00.getTestSet().get( 0 );
      final XmlTestSet xmlTestSet_00_02 = xmlTestSet_00.getTestSet().get( 1 );

      assertThat( xmlTestSet_00   .getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_01   .getTest() ).hasSize( 0 );
      assertThat( xmlTestSet_00_01.getTest() ).hasSize( 1 );
      assertThat( xmlTestSet_00_02.getTest() ).hasSize( 1 );

      // -----------------------------------------------------------------------------------------------------

      final XmlTest test_A = xmlTestSet_00   .getTest().get( 0 );
      final XmlTest test_B = xmlTestSet_00_01.getTest().get( 0 );
      final XmlTest test_C = xmlTestSet_00_02.getTest().get( 0 );

      assertThat( test_A.getId() ).isEqualTo( "TestSet 00 / Test A" );
      assertThat( test_B.getId() ).isEqualTo( "TestSet 00 / TestSet 00-01 / Test B" );
      assertThat( test_C.getId() ).isEqualTo( "TestSet 00 / TestSet 00-02 / Test C" );

      assertThat( test_A.getRequest().getEndpoint() ).isEqualTo( "http://candidate/my-endpoint?var-04=var-04+of+test+A+request&var-05-from-00=var-00+of+test+A&local=REQUEST_LOCAL" );

      // -----------------------------------------------------------------------------------------------------

      final XmlVariables setUp_Variables         = setUp           .getVariables();
      final XmlVariables testSet_00_Variables    = xmlTestSet_00   .getVariables();
      final XmlVariables testSet_01_Variables    = xmlTestSet_01   .getVariables();
      final XmlVariables testSet_00_01_Variables = xmlTestSet_00_01.getVariables();
      final XmlVariables testSet_00_02_Variables = xmlTestSet_00_02.getVariables();
      final XmlVariables test_A_Variables        = test_A          .getVariables();
      final XmlVariables test_B_Variables        = test_B          .getVariables();
      final XmlVariables test_C_Variables        = test_C          .getVariables();

      assertThat( setUp_Variables.getVariable() ).hasSize( 3 );
      assertThat( getVariableValue( setUp_Variables, "var-00" ) ).isEqualTo( "var-00 of root" );
      assertThat( getVariableValue( setUp_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( setUp_Variables, "var-globalUUID" ) ).isNotEqualTo( "${randomUUID()}" );
      final String globalUUID = getVariableValue( setUp_Variables, "var-globalUUID" );

      assertThat( testSet_00_Variables.getVariable() ).hasSize( 3 );
      assertThat( getVariableValue( testSet_00_Variables, "var-00" ) ).isEqualTo( "var-00 of test set 00" );
      assertThat( getVariableValue( testSet_00_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( testSet_00_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( testSet_01_Variables.getVariable() ).hasSize( 3 );
      assertThat( getVariableValue( testSet_01_Variables, "var-00" ) ).isEqualTo( "var-00 of root" );
      assertThat( getVariableValue( testSet_01_Variables, "var-01" ) ).isEqualTo( "var-01 of test set 01" );
      assertThat( getVariableValue( testSet_01_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( testSet_00_01_Variables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( testSet_00_01_Variables, "var-00" ) ).isEqualTo( "var-00 of child 01" );
      assertThat( getVariableValue( testSet_00_01_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( testSet_00_01_Variables, "var-02" ) ).isEqualTo( "var-02 of test set 00-01" );
      assertThat( getVariableValue( testSet_00_01_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( testSet_00_02_Variables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( testSet_00_02_Variables, "var-00" ) ).isEqualTo( "var-00 of child 02" );
      assertThat( getVariableValue( testSet_00_02_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( testSet_00_02_Variables, "var-03" ) ).isEqualTo( "var-03 of child 02" );
      assertThat( getVariableValue( testSet_00_02_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( test_A_Variables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( test_A_Variables, "var-00" ) ).isEqualTo( "var-00 of test A" );
      assertThat( getVariableValue( test_A_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( test_A_Variables, "var-03" ) ).isEqualTo( "var-03 of test A" );
      assertThat( getVariableValue( test_A_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( test_B_Variables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( test_B_Variables, "var-00" ) ).isEqualTo( "var-00 of test B" );
      assertThat( getVariableValue( test_B_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( test_B_Variables, "var-02" ) ).isEqualTo( "var-02 of test set 00-01" );
      assertThat( getVariableValue( test_B_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( test_C_Variables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( test_C_Variables, "var-00" ) ).isEqualTo( "var-00 of test C" );
      assertThat( getVariableValue( test_C_Variables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( test_C_Variables, "var-03" ) ).isEqualTo( "var-03 of child 02" );
      assertThat( getVariableValue( test_C_Variables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      // -----------------------------------------------------------------------------------------------------

      final XmlVariables testSet_00_RequestVariables    = xmlTestSet_00   .getRequest().getVariables();
      final XmlVariables testSet_01_RequestVariables    = xmlTestSet_01   .getRequest().getVariables();
      final XmlVariables testSet_00_01_RequestVariables = xmlTestSet_00_01.getRequest().getVariables();
      final XmlVariables testSet_00_02_RequestVariables = xmlTestSet_00_02.getRequest().getVariables();
      final XmlVariables test_A_RequestVariables        = test_A          .getRequest().getVariables();
      final XmlVariables test_B_RequestVariables        = test_B          .getRequest().getVariables();
      final XmlVariables test_C_RequestVariables        = test_C          .getRequest().getVariables();

      assertThat( testSet_00_RequestVariables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( testSet_00_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of test set 00" );
      assertThat( getVariableValue( testSet_00_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( testSet_00_RequestVariables, "var-02" ) ).isEqualTo( "var-02 of test set 00 request" );
      assertThat( getVariableValue( testSet_00_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      // An empty XmlRequest was created (for complete inheritance)
      assertThat( testSet_01_RequestVariables.getVariable() ).hasSize( 3 );
      assertThat( getVariableValue( testSet_01_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of root" );
      assertThat( getVariableValue( testSet_01_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of test set 01" );
      assertThat( getVariableValue( testSet_01_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      // An empty XmlRequest was created (for complete inheritance)
      assertThat( testSet_00_01_RequestVariables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( testSet_00_01_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of child 01" );
      assertThat( getVariableValue( testSet_00_01_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( testSet_00_01_RequestVariables, "var-02" ) ).isEqualTo( "var-02 of test set 00-01" );
      assertThat( getVariableValue( testSet_00_01_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      // An empty XmlRequest was created (for complete inheritance)
      assertThat( testSet_00_02_RequestVariables.getVariable() ).hasSize( 5 );
      assertThat( getVariableValue( testSet_00_02_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of child 02" );
      assertThat( getVariableValue( testSet_00_02_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( testSet_00_02_RequestVariables, "var-02" ) ).isEqualTo( "var-02 of test set 00 request" );
      assertThat( getVariableValue( testSet_00_02_RequestVariables, "var-03" ) ).isEqualTo( "var-03 of child 02" );
      assertThat( getVariableValue( testSet_00_02_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( test_A_RequestVariables.getVariable() ).hasSize( 16 );
      assertThat( getVariableValue( test_A_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of test A" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-02" ) ).isEqualTo( "var-02 of test set 00 request" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-03" ) ).isEqualTo( "var-03 of test A" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );
      assertThat( getVariableValue( test_A_RequestVariables, "var-request-local" ) ).isEqualTo( "REQUEST_LOCAL" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomUUID" ) ).matches( "[a-zA-Z0-9-]{36}" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomInteger" ) ).matches( "[-]?[0-9]+" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomLong" ) ).matches( "[-]?[0-9]+" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomDouble" ) ).matches( "[-]?[0-9]+\\.[0-9]+(E[0-9]+)?" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomDate" ) ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomDateTime" ) ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomBoolean" ) ).matches( "(true|false)" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-randomEnum" ) ).matches( "(AAA|BBB|CCC)" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-nowDate" ) ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( getVariableValue( test_A_RequestVariables, "var-nowDateTime" ) ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );

      assertThat( test_B_RequestVariables.getVariable() ).hasSize( 4 );
      assertThat( getVariableValue( test_B_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of test B request" );
      assertThat( getVariableValue( test_B_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( test_B_RequestVariables, "var-02" ) ).isEqualTo( "var-02 of test set 00-01" );
      assertThat( getVariableValue( test_B_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );

      assertThat( test_C_RequestVariables.getVariable() ).hasSize( 5 );
      assertThat( getVariableValue( test_C_RequestVariables, "var-00" ) ).isEqualTo( "var-00 of test C" );
      assertThat( getVariableValue( test_C_RequestVariables, "var-01" ) ).isEqualTo( "var-01 of root" );
      assertThat( getVariableValue( test_C_RequestVariables, "var-02" ) ).isEqualTo( "var-02 of test C request" );
      assertThat( getVariableValue( test_C_RequestVariables, "var-03" ) ).isEqualTo( "var-03 of child 02" );
      assertThat( getVariableValue( test_C_RequestVariables, "var-globalUUID" ) ).isEqualTo( globalUUID );
    }
  }
}
