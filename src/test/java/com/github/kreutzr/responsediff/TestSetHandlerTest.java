package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.filter.DiffFilterException;

import jakarta.xml.bind.JAXBException;

public class TestSetHandlerTest
{
  private static final String rootPath_ = new File( "" ).getAbsolutePath() + File.separator;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatPrepareXmlRequestWorks()
  {
    try {
      // Given
      final String               xmlFilePath  = rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/external/0000/setup.xml";
      final XmlResponseDiffSetup xmlTestSetup = XmlFileHandler.readSetup( xmlFilePath, true );

      final XmlTestSet xmlTestSet = xmlTestSetup.getTestSet().get( 0 );
      final XmlTest    xmlTest    = xmlTestSet.getTest().get( 0 );


      // This is essential for initialization
      TestSetHandler.initializeTest( xmlTest, xmlTestSet );

      final XmlRequest                xmlRequest         = xmlTest.getRequest();
      final String                    serviceId          = TestSetHandler.CANDIDATE;
      final String                    testFileName       = xmlFilePath;
      final String                    serviceUrl         = "http://my-service";
      final List< XmlHeader >         xmlExternalHeaders = null;
      final Map< String, DiffFilter > filterRegistry     = new HashMap<>();

      // When
      final XmlRequest clonedXmlRequest = TestSetHandler.prepareXmlRequest(
        xmlRequest,
        serviceId,
        xmlTest,
        testFileName,
        xmlExternalHeaders,
        serviceUrl,
        filterRegistry
      );

      // Then
      assertThat( clonedXmlRequest.getParameters().getParameter().size() ).isEqualTo( 3 );
      assertThat( clonedXmlRequest.getParameters().getParameter().get( 2 ).getValue() ).isEqualTo( "100000000" );
      assertThat( clonedXmlRequest.getEndpoint() ).isEqualTo( "http://my-service/solution/portfolio/calculate-growth?begin=2010-01-01&end=2023-08-01&contract=100000000" );
    }
    catch( final JAXBException | SAXException | DiffFilterException | ParseException ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatReadingVariablesFromResponseHeadersWorks()
  {
    try {
      // Given
      final String               xmlFilePath  = rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/test_responseVariables/setup.xml";
      final XmlResponseDiffSetup setUp        = XmlFileHandler.readSetup( xmlFilePath, true );
      final XmlTestSet           xmlTestSet   = setUp.getTestSet().get( 0 );
      final XmlTest              xmlTest      = xmlTestSet.getTest().get( 0 );
      final String               serviceId    = TestSetHandler.CANDIDATE;
      final JsonDiff             foundDiffs   = JsonDiff.createDataInstance();

      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      final List< XmlHeader > xmlHeaders = xmlHttpResponse.getHeaders().getHeader();

      final String headerValue = "application/vnd.some-application";
      final XmlHeader xmlHeader = new XmlHeader();
      xmlHeader.setName( HttpHandler.HEADER_NAME__CONTENT_TYPE );
      xmlHeader.setValue( headerValue );
      xmlHeaders.add( xmlHeader );

      // When
      TestSetHandler.handleTestHeaders( xmlTestSet, xmlTest, serviceId, xmlHttpResponse, foundDiffs );

      // Then
      assertThat( foundDiffs.hasDifference() ).isFalse();
      assertThat( xmlTestSet.getVariables() ).isNotNull();
      assertThat( xmlTestSet.getVariables().getVariable().size() ).isEqualTo( 1 );
      assertThat( xmlTestSet.getVariables().getVariable().get( 0 ).getId() ).isEqualTo( TestSetHandler.CANDIDATE + ".HEADER__CONTENT_TYPE" );
      assertThat( xmlTestSet.getVariables().getVariable().get( 0 ).getValue() ).isEqualTo( headerValue );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      // Unreachable
      assertThat( false ).isTrue();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatReadingVariablesFromResponseBodyWorks()
  {
    // -------------------------------------------------------------------
    //
    // NOTE: More tests are covered by test class "ResponseVariablesTest"
    //
    // -------------------------------------------------------------------

    // Given
    XmlTestSet      xmlTestSetRoot  = new XmlTestSet();
    XmlTestSet      xmlTestSet      = new XmlTestSet();
    XmlTest         xmlTest         = new XmlTest();
    XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    JsonDiff        foundDiffs      = JsonDiff.createDataInstance();
    XmlResponse     xmlResponse     = new XmlResponse();
    XmlVariable     xmlVariable1     = new XmlVariable();
    XmlVariable     xmlVariable2     = new XmlVariable();
    XmlVariable     xmlVariable3     = new XmlVariable();

    xmlTestSetRoot.getTestSet().add( xmlTestSet );
    xmlTestSetRoot.setVariables( new XmlVariables() );
    xmlTestSet.getTest().add( xmlTest );
    xmlTestSet.setVariables( new XmlVariables() );
    xmlTest.setResponse( xmlResponse );
    xmlTest.setVariables( new XmlVariables() );

    xmlResponse.setVariables( new XmlVariables() );
    xmlResponse.getVariables().getVariable().add( xmlVariable1 );
    xmlResponse.getVariables().getVariable().add( xmlVariable2 );
    xmlResponse.getVariables().getVariable().add( xmlVariable3 );

    xmlHttpResponse.setBody( "{ \"array\" : [ { \"key1\" : \"A\", \"key2\" : \"B\" }, { \"key1\" : \"A\", \"key2\" : \"C\" }, { \"key1\" : \"X\", \"key2\" : \"Y\" } ], \"key0\" : \"Z\" }" );
    xmlVariable1.setId( "var1");
    xmlVariable1.setPath( "$.key0");
    xmlVariable2.setId( "var2");
    xmlVariable2.setPath( "$.array[?(@.key1=='A')].key2#1");
    xmlVariable3.setId( "var3");
    xmlVariable3.setPath( "$..key2#2");

    // When
    TestSetHandler.handleTestResponse( xmlTestSet, xmlTest, TestSetHandler.CANDIDATE, xmlHttpResponse, foundDiffs );

    // Then
    // Read response variables are propagated to surrounding TestSet to be available for following sibling tests.
    assertThat( xmlTestSet.getVariables().getVariable() ).hasSize( 3 );
    assertThat( xmlTestSet.getVariables().getVariable().get( 0 ).getId()    ).isEqualTo( TestSetHandler.CANDIDATE + ".var1" );
    assertThat( xmlTestSet.getVariables().getVariable().get( 0 ).getValue() ).isEqualTo( "Z" );
    assertThat( xmlTestSet.getVariables().getVariable().get( 1 ).getId()    ).isEqualTo( TestSetHandler.CANDIDATE + ".var2" );
    assertThat( xmlTestSet.getVariables().getVariable().get( 1 ).getValue() ).isEqualTo( "C" );
    assertThat( xmlTestSet.getVariables().getVariable().get( 2 ).getId()    ).isEqualTo( TestSetHandler.CANDIDATE + ".var3" );
    assertThat( xmlTestSet.getVariables().getVariable().get( 2 ).getValue() ).isEqualTo( "Y" );

    // Read response variables are NOT propagated to Test itself (because the response was already received).
    assertThat( xmlTest.getVariables().getVariable() ).hasSize( 0 );

    // Read response variables are NOT propagated to outer TestSet.
    assertThat( xmlTestSetRoot.getVariables().getVariable() ).hasSize( 0 );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatOverAllExpectedForTestsWorks()
  {
    // Given
    final XmlOverAllExpected xmlOverAllExpected = new XmlOverAllExpected();
    xmlOverAllExpected.setMaxDuration( "PT2S" );
    final XmlTest xmlTest = new XmlTest();
    xmlTest.setId( "MyTest" );
    xmlTest.setOverAllExpected( xmlOverAllExpected );

    final XmlAnalysis xmlAnalysis = new XmlAnalysis();
    xmlAnalysis.setTotalCount( 10 );

    List< JsonDiffEntry> jsonDiffEntries = null;

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2S" ); // This is fine
    xmlTest.setIterations( 10 );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( null, xmlTest, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long
    xmlTest.setIterations( 10 );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( null, xmlTest, xmlAnalysis );
    assertThat( jsonDiffEntries.size() ).isEqualTo( 1 );

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTest.setIterations( 1 ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( null, xmlTest, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTest.setIterations( null ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( null, xmlTest, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatOverAllExpectedForTestSetsWorks()
  {
    // Given
    final XmlOverAllExpected xmlOverAllExpected = new XmlOverAllExpected();
    xmlOverAllExpected.setMaxDuration( "PT2S" );
    final XmlTestSet xmlTestSet = new XmlTestSet();
    xmlTestSet.setId( "MyTestSet" );
    xmlTestSet.setOverAllExpected( xmlOverAllExpected );

    final XmlAnalysis xmlAnalysis = new XmlAnalysis();
    xmlAnalysis.setTotalCount( 10 );

    List< JsonDiffEntry> jsonDiffEntries = null;

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2S" ); // This is fine
    xmlTestSet.setIterations( 10 );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long
    xmlTestSet.setIterations( 10 );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.size() ).isEqualTo( 1 );

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTestSet.setIterations( 1 ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTestSet.setIterations( null ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTestSet.setIterations( null ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    xmlTestSet.getTest().clear();
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSetInclude().clear();
    xmlTestSet.getTest().add( new XmlTest() );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTestSet.setIterations( null ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    xmlTestSet.getTest().clear();
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSetInclude().clear();
    xmlTestSet.getTestSet().add( new XmlTestSet() );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long...
    xmlTestSet.setIterations( null ); // ... but we ignore trivial overAllExpected entries => Use "response/expected/masDuration" instead.
    xmlTestSet.getTest().clear();
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSetInclude().clear();
    xmlTestSet.getTestSetInclude().add( new XmlTestSetInclude() );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.isEmpty() ).isTrue();

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long
    xmlTestSet.setIterations( null ); // We do not ignore this because there is more than one child
    xmlTestSet.getTest().clear();
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSetInclude().clear();
    xmlTestSet.getTest().add( new XmlTest() );
    xmlTestSet.getTestSet().add( new XmlTestSet() );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.size() ).isEqualTo( 1 );

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long
    xmlTestSet.setIterations( null ); // We do not ignore this because there is more than one child
    xmlTestSet.getTest().clear();
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSetInclude().clear();
    xmlTestSet.getTest().add( new XmlTest() );
    xmlTestSet.getTestSetInclude().add( new XmlTestSetInclude() );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.size() ).isEqualTo( 1 );

    // When / Then
    xmlAnalysis.setTotalDuration( "PT2.001S" ); // This is too long
    xmlTestSet.setIterations( null ); // We do not ignore this because there is more than one child
    xmlTestSet.getTest().clear();
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSetInclude().clear();
    xmlTestSet.getTestSet().add( new XmlTestSet() );
    xmlTestSet.getTestSetInclude().add( new XmlTestSetInclude() );
    jsonDiffEntries = TestSetHandler.validateOverAllExpected( xmlTestSet, null, xmlAnalysis );
    assertThat( jsonDiffEntries.size() ).isEqualTo( 1 );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJoinTicketReferencesWorks()
  {
    // Given
    final XmlTest xmlTest = new XmlTest();

    // When / Then
    TestSetHandler.joinTicketReferences( xmlTest, null );
    assertThat( xmlTest.getTicketReference() ).isNull();

    // When / Then
    TestSetHandler.joinTicketReferences( xmlTest, "    " );
    assertThat( xmlTest.getTicketReference() ).isNull();

    // When / Then
    TestSetHandler.joinTicketReferences( xmlTest, " BBB " );
    assertThat( xmlTest.getTicketReference() ).isEqualTo( "BBB" );

    // When / Then
    TestSetHandler.joinTicketReferences( xmlTest, null );
    assertThat( xmlTest.getTicketReference() ).isEqualTo( "BBB" );

    // When / Then
    TestSetHandler.joinTicketReferences( xmlTest, "    " );
    assertThat( xmlTest.getTicketReference() ).isEqualTo( "BBB" );

    // When / Then
    TestSetHandler.joinTicketReferences( xmlTest, " CCC, AAA " );
    assertThat( xmlTest.getTicketReference() ).isEqualTo( "AAA,BBB,CCC" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String toString( final XmlTestSet xmlTestSet, final String indent )
  {
    final StringBuilder sb = new StringBuilder();
    sb.append( indent + "TestSet: \"" + xmlTestSet.getId() + "\"" ).append( "("+xmlTestSet.getStructureDepth()+")").append( "\n" );

    final String childIndent = indent + "  ";
    for( final XmlTest xmlTest : xmlTestSet.getTest() ) {
      sb.append( toString( xmlTest, childIndent ) );
    }
    for( final XmlTestSet xmlChildTestSet : xmlTestSet.getTestSet() ) {
      sb.append( toString( xmlChildTestSet, childIndent ) );
    }

    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String toString( final XmlTest xmlTest, final String indent )
  {
    return indent + "Test: \"" + xmlTest.getId() + "\" ("+xmlTest.getStructureDepth()+")\n";
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // NEEDS FIX A: Refactor for proper unit test!
  // These methods MUST NOT have any effect on the XmlTestSet structure
  public static void main(String[] args)
  {
    try {
      XmlTestSet root = new XmlTestSet();
      root.setId( "root" );
        XmlTest t1 = new XmlTest();
        XmlTest t2 = new XmlTest();
        t1.setId( "T1" );
        t2.setId( "T2" );
        root.getTest().add( t1 );
        root.getTest().add( t2 );

      XmlTestSet ts1 = new XmlTestSet();
      ts1.setId( "TS1" );
      root.getTestSet().add( ts1 );
        XmlTest t3 = new XmlTest();
        XmlTest t4 = new XmlTest();
        t3.setId( "T3" );
        t4.setId( "T4" );
        ts1.getTest().add( t3 );
        ts1.getTest().add( t4 );

      XmlTestSet ts2 = new XmlTestSet();
      ts2.setId( "TS2" );
      root.getTestSet().add( ts2 );
        XmlTest t5 = new XmlTest();
        XmlTest t6 = new XmlTest();
        t5.setId( "T5" );
        t6.setId( "T6" );
        ts2.getTest().add( t5 );
        ts2.getTest().add( t6 );

      XmlTestSet ts3 = new XmlTestSet();
      ts3.setId( "TS3" );
      ts1.getTestSet().add( ts3 );
        XmlTest t7 = new XmlTest();
        XmlTest t8 = new XmlTest();
        t7.setId( "T7" );
        t8.setId( "T8" );
        ts3.getTest().add( t7 );
        ts3.getTest().add( t8 );

      XmlTestSet ts4 = new XmlTestSet();
      ts4.setId( "TS4" );
      ts2.getTestSet().add( ts4 );
        XmlTest t9  = new XmlTest();
        XmlTest t10 = new XmlTest();
        t9.setId( "T9" );
        t10.setId( "T10" );
        ts4.getTest().add( t9 );
        ts4.getTest().add( t10 );

      List< XmlTest > allTests =  null;
      allTests = TestSetHandler.getAllTests( root, 1 );
      System.out.println( allTests.size() );
      System.out.println( toString( root, "" ) );

//      allTests = TestSetHandler.getRemainingTests( root, ts1, 1 );
//      System.out.println( allTests.size() );
//      System.out.println( toString( root, "" ) );

//      allTests = TestSetHandler.getRemainingTests( root, ts2, 1 );
//      System.out.println( allTests.size() );
//      System.out.println( toString( root, "" ) );

//      allTests = TestSetHandler.getRemainingTests( ts1, t3, 2 );
//      System.out.println( allTests.size() );
//      System.out.println( toString( root, "" ) );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatWaitBeforeWorks()
  {
    // Given

    // When / Then
    String durationAsString = null;
    long now = System.currentTimeMillis();
    TestSetHandler.waitBefore( durationAsString );
    assertThat( System.currentTimeMillis() - now ).isLessThan( 5 ); // 5 = almost no execution time

    // When / Then
    durationAsString = "UNPARSABLE";
    now = System.currentTimeMillis();
    TestSetHandler.waitBefore( durationAsString );
    assertThat( System.currentTimeMillis() - now ).isLessThan( 5 ); // 5 = almost no execution time

    // When / Then
    durationAsString = "PT3s"; // 3 seconds
    now = System.currentTimeMillis();
    TestSetHandler.waitBefore( durationAsString );
    assertThat( System.currentTimeMillis() - now ).isGreaterThanOrEqualTo( 3000 );
  }
}
