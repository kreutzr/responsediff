package com.github.kreutzr.responsediff;

import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.tools.CloneHelper;

import jakarta.xml.bind.JAXBException;

public class TestSetHandler
{
  public  static final String CANDIDATE = "candidate";
  private static final String REFERENCE = "reference";
  private static final String CONTROL   = "control  ";

  public  static final String ID_SEPARATOR = " / ";

  private static final Logger LOG = LoggerFactory.getLogger( TestSetHandler.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Traverses the given xmlTestSetup (depth first order), performs each test and stores the results within the analysis section of each incuded test and test set.
   * @param testIdPattern The pattern of the tests to execute. May be null. (default null means that all tests are executed)
   * @param xmlTestSetup The XmlResponseDiffSetup to traverse. Must not be null.
   * @param candidateServiceUrl The URL of the candidate server. Must not be null.
   * @param candidateHeaders A list of XmlHeader objects. May be null. This is required for e.g. passing server individual authentication headers.
   * @param referenceServiceUrl The URL of the reference server. Must not be null.
   * @param referenceHeaders A list of XmlHeader objects. May be null. This is required for e.g. passing server individual authentication headers.
   * @param controlServiceUrl The URL of the control server. Must be different to both - the reference and the candidate server. May be null.
   * @param controlHeaders   A list of XmlHeader object. May be null. This is required for e.g. passing server individual authentication headers.
   * @param filterRegistry The filter registry to use. Must not be null.
   * @param timeoutMs The timeout for the HTTP handling in milliseconds.
   * @param epsilon The epsilon for decimal comparison. Must not be null.
   * @param referenceFilePath Optional filename that points to a XML report that shall be used to simulate reference responses, if no reference service URL is configured. May be null.
   * @param storeReportPath The path where the report is stored to. May be null.
   * @param reportWhiteNoise Flag, if any different value shall be reported (true) or only those that were not discovered to be white noise (differences between reference and control, or expected differences) (false).
   * @param maskAuthorizationHeaderInCurl Flag, if authorization header shall be logged in the reported curl command (true) or not (false)
   * @throws SAXException
   * @throws JAXBException
   * @throws ParseException
   */
  static void processTestSetup(
      final Pattern                   testIdPattern,
      final XmlResponseDiffSetup      xmlTestSetup,
      final String                    candidateServiceUrl,
      final List< XmlHeader >         candidateHeaders,
      final String                    referenceServiceUrl,
      final List< XmlHeader >         referenceHeaders,
      final String                    controlServiceUrl,
      final List< XmlHeader >         controlHeaders,
      final Map< String, DiffFilter > filterRegistry,
      final long                      timeoutMs,
      final double                    epsilon,
      final String                    referenceFilePath,
      final String                    storeReportPath,
      final boolean                   reportWhiteNoise,
      final boolean                   maskAuthorizationHeaderInCurl
  )
  throws JAXBException, SAXException, ParseException
  {
    LOG.trace( "processTestSetup()" );

    final OuterContext outerContext = new OuterContext(
        candidateServiceUrl,
        candidateHeaders,
        referenceServiceUrl,
        referenceHeaders,
        controlServiceUrl,
        controlHeaders,
        filterRegistry,
        testIdPattern,
        timeoutMs,
        epsilon,
        storeReportPath,
        reportWhiteNoise,
        maskAuthorizationHeaderInCurl
      );

    final XmlResponseDiffSetup referenceXmlSetup = referenceFilePath != null
      ? XmlFileHandler.readSetup( referenceFilePath, false )
      : null;

    final int structureDepth = 1;
    xmlTestSetup.setStructureDepth( structureDepth );

    // Handle test sets
    final Iterator< XmlTestSet > it = xmlTestSetup.getTestSet().iterator();
    while( it.hasNext() ) {
      final XmlTestSet xmlTestSet = it.next();

      // Set default values if required. Do NOT define a default in the XSD because then we can not inherit since the default value is never null.
      if( xmlTestSet.getReport() == null ) { xmlTestSet.setReport( "fail,skip" ); }
      if( xmlTestSet.getOrder()  == null ) { xmlTestSet.setOrder( XmlTestOrder.RANDOM ); }

      boolean breakTestExecution = false;
      Exception breakException = null;

      try {
        handleTestSet(
          xmlTestSet,
          outerContext,
          referenceXmlSetup,
          structureDepth + 1
        );
      }
      catch( final BreakOnFailureException ex ) {
        ex.printStackTrace();
        breakTestExecution = true;
        breakException = ex;
      }

      xmlTestSetup.setAnalysis( handleXmlAnalysisDurations( null, xmlTestSetup.getTestSet() ) );

      if( breakTestExecution ) {
        LOG.info( "Breaking test set execution because " + breakException.getMessage() );
        break;
      }
    }

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Total analysis =" + ToJson.fromAnalysis( xmlTestSetup.getAnalysis() ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Traverses the test set structure (depth first) and gathers the test analysis of all tests of a XmlTestSet.
   * @param xmlTestSet The XmlTestSet to handle. Must not be null.
   * @param outerContext The outer context. Must not be null.
   * @param referenceXmlSetup An optional XmlSetup that shall be used to simulate reference responses, if no reference service URL is configured. May be null.
   * @param structureDepth The depth within the test structure. The root element (setup) has depth 1.
   * @throws ParseException
   * @throws BreakOnFailureException
   */
  private static void handleTestSet(
     final XmlTestSet           xmlTestSet,
     final OuterContext         outerContext,
     final XmlResponseDiffSetup referenceXmlSetup,
     final int                  structureDepth
  )
  throws BreakOnFailureException, ParseException
  {
    LOG.trace( "handleTestSet()" );

    // Update structure depth
    xmlTestSet.setStructureDepth( structureDepth );
    final int childStructureDepth = structureDepth + 1;

    Exception breakException = null;

    // Handle terminating tests
    final Collection< XmlTest > xmlTests = createTestCollection( xmlTestSet.getTest(), xmlTestSet.getOrder() );
    for( final XmlTest xmlTest : xmlTests ) {
      initializeTest( xmlTest, xmlTestSet );

      try {
        handleTest(
          xmlTestSet,
          xmlTest,
          outerContext,
          xmlTestSet.getFileName(),
          referenceXmlSetup,
          childStructureDepth
        );
      }
      catch( final BreakOnFailureException ex ) {
        breakException = ex;
      }

      if( breakException != null ) {
        // Adjust XmlAnalysis after XmlTest broke
        final int numberOfSkippedTests = getRemainingTests( xmlTestSet, xmlTest, childStructureDepth ).size();
        XmlAnalysis xmlAnalysis = xmlTestSet.getAnalysis();
        if( xmlAnalysis == null ) {
          xmlAnalysis = new XmlAnalysis();
          xmlTestSet.setAnalysis( xmlAnalysis );
        }
        xmlAnalysis.setTotalCount( xmlAnalysis.getTotalCount() + numberOfSkippedTests );
        xmlAnalysis.setSkipCount ( xmlAnalysis.getSkipCount () + numberOfSkippedTests );

        LOG.info( "Breaking test execution because " + breakException.getMessage() );
        break;
      }
    }

    if( breakException != null ) {
      // Handle duration statistic for handled tests
      xmlTestSet.setAnalysis( handleXmlAnalysisDurations( xmlTests, null ) );

      // NOTE: The result of getRemainingTests() includes all following TestSets, too.
      //       Therefore we have to make sure that no sub-TestSet is executed.
      if( xmlTestSet.isBreakOnFailure() ) {
        throw new BreakOnFailureException( "Test set \"" + xmlTestSet.getId() + "\" terminated unsuccessfully. Followup tests are skipped.", breakException );
      }
      return;
    }

    // Handle child TestSets by recursion
    final Collection< XmlTestSet > xmlTestSets = createTestSetCollection( xmlTestSet.getTestSet(), xmlTestSet.getOrder() );
    for( final XmlTestSet xmlTestSetChild : xmlTestSets ) {
      initializeTestSet( xmlTestSetChild, xmlTestSet );

      try {
        handleTestSet(
          xmlTestSetChild,
          outerContext,
          referenceXmlSetup,
          childStructureDepth
        );
      }
      catch( final BreakOnFailureException ex ) {
        if( xmlTestSet.isBreakOnFailure() ) {
          breakException = ex;
        }
      }

      if( breakException != null ) {
        // Adjust XmlAnalysis after XmlTestSet broke
        final int numberOfSkippedTests = getRemainingTests( xmlTestSet, xmlTestSetChild, childStructureDepth ).size();
        XmlAnalysis xmlAnalysis = xmlTestSet.getAnalysis();
        if( xmlAnalysis == null ) {
          xmlAnalysis = new XmlAnalysis();
          xmlTestSet.setAnalysis( xmlAnalysis );
        }
        xmlAnalysis.setTotalCount( xmlAnalysis.getTotalCount() + numberOfSkippedTests );
        xmlAnalysis.setSkipCount ( xmlAnalysis.getSkipCount () + numberOfSkippedTests );

        LOG.info( "Breaking test set execution because " + breakException.getMessage() );
        break;
      }
    }

    // Handle duration statistic for handled tests and test sets
    xmlTestSet.setAnalysis( handleXmlAnalysisDurations( xmlTests, xmlTestSets ) );

    // Check over all expected
    final List< JsonDiffEntry > jsonDiffEntries = validateOverAllExpected(
      xmlTestSet,
      null, // xmlTest
      xmlTestSet.getAnalysis()
    );

    // Check if an exception occurred or an overall expectation was violated
    if( breakException != null || !jsonDiffEntries.isEmpty() ) {
      if( xmlTestSet.isBreakOnFailure() ) {
        throw new BreakOnFailureException( "Test set \"" + xmlTestSet.getId() + "\" terminated unsuccessfully. Followup tests are skipped.", breakException );
      }
      return;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads variables defined as response variables(!) from the response headers and provides them as test set variables for following tests.
   * @param xmlTestSet The XmlTestSet to store the variables into. Must not be null.
   * @param xmlTest The XmlTest to read from. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL).
   * @param xmlHttpResponse The XmlHttpResponse. May be null.
   * @param foundDiffs The found JSON differences. Must not be null.
   */
  static void handleTestHeaders(
    final XmlTestSet      xmlTestSet,
    final XmlTest         xmlTest,
    final String          serviceId,
    final XmlHttpResponse xmlHttpResponse,
    final JsonDiff        foundDiffs
  )
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "handleTestHeaders( serviceId=" + serviceId + ", test id=\"" + xmlTest.getId() + "\" )" );
    }

    if( xmlTest.getResponse() == null
      || xmlTest.getResponse().getVariables() == null
      || xmlHttpResponse == null
      || xmlHttpResponse.getHeaders().getHeader().isEmpty()
     ) {
       return;
     }

    final String json = ToJson.fromHeaders( xmlHttpResponse.getHeaders(), true );

    JsonPathHelper jph = null;
    for( final XmlVariable xmlVariable : xmlTest.getResponse().getVariables().getVariable() ) {
      if( xmlVariable.getPath() == null
      || !xmlVariable.getPath().startsWith( "$." + ToJson.HEADERS_SUBPATH ) ) {
        // NOTE: We read variables from response body in method "handleTestResponse()".
        continue;
      }
      try {
        if( jph == null ) {
          // Performance: Lazy loading in case there are no variables with paths
          jph = new JsonPathHelper( json );
        }
        final XmlValueType valueType = xmlVariable.getType() != null ? xmlVariable.getType() : XmlValueType.STRING;
        // Read value from Body via JSON path
        final String value = (String) jph.getValue( xmlVariable.getPath() );

        // Search variable with matching id in XmlTestSet variables
        XmlVariable xmlSetVariable = null;
        for( final XmlVariable xmlVar : xmlTestSet.getVariables().getVariable() ) {
          if( xmlVar.getId().equals( xmlVariable.getId() ) ) {
            xmlSetVariable = xmlVar;
          }
        }
        if( xmlSetVariable == null ) {
          xmlSetVariable = new XmlVariable();
          xmlSetVariable.setId( serviceId.trim() + "." + xmlVariable.getId() );
          xmlSetVariable.setValue( value );
          xmlSetVariable.setType( valueType );
          if( xmlTestSet.getVariables() == null ) {
            xmlTestSet.setVariables( new XmlVariables() );
          }
          xmlTestSet.getVariables().getVariable().add( xmlSetVariable );
        }
      }
      catch( final Exception ex ) {
        foundDiffs.getChanges().add( new JsonDiffEntry(
          "Exception", "", "",
          "Error reading " + serviceId + " header variable " + xmlVariable.getId() + " from path " + xmlVariable.getPath() + " ."
        + " (Exception=" + ex.getClass().getName() + ", message=" + ex.getMessage() + ")"
        ) );
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads variables defined as response variables(!) from the response body and provides them as test set variables for following tests
   * @param xmlTestSet The XmlTestSet to store the variables into. Must not be null.
   * @param xmlTest The XmlTest to read from. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL).
   * @param xmlHttpResponse The XmlHttpResponse. May be null.
   * @param foundDiffs The found JSON differences. Must not be null.
   */
  static void handleTestResponse(
    final XmlTestSet      xmlTestSet,
    final XmlTest         xmlTest,
    final String          serviceId,
    final XmlHttpResponse xmlHttpResponse,
    final JsonDiff        foundDiffs
  )
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "handleTestResponse( serviceId=" + serviceId + ", test id=\"" + xmlTest.getId() + "\" )" );
    }

    if( xmlTest.getResponse() == null
     || xmlTest.getResponse().getVariables() == null
     || xmlHttpResponse == null
     || xmlHttpResponse.getBody() == null
     || xmlHttpResponse.getBody().trim().isEmpty()
    ) {
      return;
    }

    JsonPathHelper jph = null;
    for( final XmlVariable xmlVariable : xmlTest.getResponse().getVariables().getVariable() ) {
      if( xmlVariable.getPath() == null
       || xmlVariable.getPath().startsWith( "$." + ToJson.HEADERS_SUBPATH ) ) {
        // NOTE: We read variables from headers in method "handleTestHeaders()".
        continue;
      }
      try {
        if( jph == null ) {
          // Performance: Lazy loading in case there are no variables with paths
          jph = new JsonPathHelper( xmlHttpResponse.getBody() );
        }
        final XmlValueType valueType = xmlVariable.getType() != null ? xmlVariable.getType() : XmlValueType.STRING;
        // Read value from Body via JSON path
        final Object obj = jph.getValue( xmlVariable.getPath() );

        // Search variable with matching id in XmlTestSet variables
        XmlVariable xmlSetVariable = null;
        for( final XmlVariable xmlVar : xmlTestSet.getVariables().getVariable() ) {
          if( xmlVar.getId().equals( xmlVariable.getId() ) ) {
            xmlSetVariable = xmlVar;
            break;
          }
        }
        // Provide a variable (already defined variables MUST NOT be updated at runtime (otherwise the XML file is not useful for debugging).
        if( xmlSetVariable == null ) {
          xmlSetVariable = new XmlVariable();
          xmlSetVariable.setId( serviceId.trim() + "." + xmlVariable.getId() );
          xmlSetVariable.setValue( obj == null ? null : obj.toString() );
          xmlSetVariable.setType( valueType );
          if( xmlTestSet.getVariables() == null ) {
            xmlTestSet.setVariables( new XmlVariables() );
          }
          xmlTestSet.getVariables().getVariable().add( xmlSetVariable );
        }
        else {
          LOG.warn( "Found existing variable " + xmlVariable.getId() + " as follows \"" + ToJson.fromVariable( xmlSetVariable ) + "\". Reading variable value from response is skipped." );
        }
      }
      catch( final Exception ex ) {
        foundDiffs.getChanges().add( new JsonDiffEntry(
          "Exception", "", "",
          "Error reading " + serviceId + " response variable " + xmlVariable.getId() + " from path " + xmlVariable.getPath() + " ."
        + " (Exception=" + ex.getClass().getName() + ", message=" + ex.getMessage() + ")"
        ) );
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates either an ordered or unordered Collection of XmlTest objects.
   * @param xmlTests A Collection of XmlTest objects that shall be handled.
   * @param testOrder The intended test order. Must not be null.
   * @return A Collection that holds all given XmlTest objects. May be empty but never null.
   */
  private static Collection< XmlTest > createTestCollection(
     final Collection< XmlTest > xmlTests,
     final XmlTestOrder testOrder
  )
  {
    final Collection< XmlTest > result = ( testOrder == XmlTestOrder.RANDOM )
      ? new HashSet< XmlTest >()
      : new ArrayList< XmlTest >();

    if( xmlTests != null ) {
      result.addAll( xmlTests );
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates either an ordered or unordered Collection of XmlTestSet objects.
   * @param xmlTestSets A Collection of XmlTestSet objects that shall be handled.
   * @param testOrder The intended test order. Must not be null.
   * @return A Collection that holds all given XmlTestSet objects. May be empty but never null.
   */
  private static Collection< XmlTestSet > createTestSetCollection(
     final Collection< XmlTestSet > xmlTestSets,
     final XmlTestOrder testOrder
  )
  {
    final Collection< XmlTestSet > result = ( testOrder == XmlTestOrder.RANDOM )
      ? new HashSet< XmlTestSet >()
      : new ArrayList< XmlTestSet >();

    if( xmlTestSets != null ) {
      result.addAll( xmlTestSets );
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Handles a test.
   * @param xmlTestSet The XmlTest's parent XmlTestSet. Must not be null.
   * @param xmlTest The XmlTest to handle. Must not be null.
   * @param outerContext The outer context. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param referenceXmlSetup An optional XmlSetup that shall be used to simulate reference responses, if no reference service URL is configured. May be null.
   * @param structureDepth The depth within the test structure. The root element (setup) has depth 1.
   * @throws BreakOnFailureException
   */
  private static void handleTest(
     final XmlTestSet           xmlTestSet,
     final XmlTest              xmlTest,
     final OuterContext         outerContext,
     final String               testFileName,
     final XmlResponseDiffSetup referenceXmlSetup,
     final int                  structureDepth
  )
  throws BreakOnFailureException
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "handleTest( " + xmlTest.getId() + " )" );
    }

    // Update structure depth
    xmlTest.setStructureDepth( structureDepth );

    final LocalDateTime begin = LocalDateTime.now();
          LocalDateTime end   = null;

    final String            testSetPath            = xmlTestSet.getFilePath();
    final XmlRequest        xmlRequest             = xmlTest.getRequest();
    final XmlResponse       xmlResponse            = xmlTest.getResponse();
    final String            testId                 = xmlTest.getId();
    final long              timeoutMs              = outerContext.getTimeoutMs();
    final Map< String, DiffFilter > filterRegistry = outerContext.getFilterRegistry();
    final String            candidateServiceUrl    = outerContext.getCandidateServiceUrl();
    final List< XmlHeader > candidateHeaders       = outerContext.getCandidateHeaders();
    final String            referenceServiceUrl    = outerContext.getReferenceServiceUrl();
    final List< XmlHeader > referenceHeaders       = outerContext.getReferenceHeaders();
    final String            controlServiceUrl      = outerContext.getControlServiceUrl();
    final List< XmlHeader > controlHeaders         = outerContext.getControlHeaders();
    final String            storeReportPath        = outerContext.getStroreReportPath();
    final Set< String >     ignorePaths            = getIgnorePaths  ( xmlResponse, xmlTest );
    final Set< String >     ignoreHeaders          = getIgnoreHeaders( xmlResponse );


    boolean  skipped    = false;
    boolean  hasError   = false;
    JsonDiff foundDiffs = null;

    try {
      final Pattern pattern = outerContext.getTestIdPattern();
      if( pattern != null ) {
        if( !pattern.matcher( testId ).matches() ) {
          throw new TestIgnoredException( "Test id \"" + testId + "\" does not match the pattern \"" + pattern.pattern() + "\". It is skipped." );
        }
      }

      // Inherit description from XmlTest if not defined locally.
      // NOTE: This has to be done before prepareXmlRequest is invoked!
      xmlRequest.setDescription( joinString( xmlRequest.getDescription(), xmlTest.getDescription(), testId ) );

      final XmlRequest referenceXmlRequest = prepareXmlRequest( xmlRequest, REFERENCE, xmlTest, testFileName, referenceHeaders, referenceServiceUrl, filterRegistry );
      final XmlRequest controlXmlRequest   = prepareXmlRequest( xmlRequest, CONTROL,   xmlTest, testFileName, controlHeaders,   controlServiceUrl,   filterRegistry );
      final XmlRequest candidateXmlRequest = prepareXmlRequest( xmlRequest, CANDIDATE, xmlTest, testFileName, candidateHeaders, candidateServiceUrl, filterRegistry );

      final Builder    referenceBuilder    = HttpHandler.prepareHttpRequest( referenceXmlRequest, REFERENCE, testId, testFileName );
      final Builder    controlBuilder      = HttpHandler.prepareHttpRequest( controlXmlRequest,   CONTROL,   testId, testFileName );
      final Builder    candidateBuilder    = HttpHandler.prepareHttpRequest( candidateXmlRequest, CANDIDATE, testId, testFileName );

      // Copy variable replacements (applied by request filter(s) - see above)
      xmlRequest.setEndpoint   ( candidateXmlRequest.getEndpoint() );
      xmlRequest.setBody       ( candidateXmlRequest.getBody() );
      xmlRequest.setHeaders    ( candidateXmlRequest.getHeaders() );
      xmlRequest.setParameters ( candidateXmlRequest.getParameters() );
      xmlRequest.setDescription( candidateXmlRequest.getDescription() );

      // Create candidate CURL for reporting
      HttpHandler.addCurl( candidateXmlRequest, CANDIDATE, testFileName, outerContext );
      xmlRequest.setCurl( candidateXmlRequest.getCurl() );

      // NOTE: We invoke the reference and the control services first, because we need their responses first
      final CompletableFuture< HttpResponse< byte[] > > referenceResponseFuture = HttpHandler.sendRequest( referenceXmlRequest, referenceHeaders, referenceBuilder, REFERENCE, testId, testFileName );
      final CompletableFuture< HttpResponse< byte[] > > controlResponseFuture   = HttpHandler.sendRequest( controlXmlRequest,   controlHeaders,   controlBuilder,   CONTROL,   testId, testFileName );

            XmlHttpResponse referenceResponse = HttpHandler.createXmlHttpResponse( referenceResponseFuture, xmlResponse, timeoutMs, filterRegistry, REFERENCE, testId, testFileName, referenceXmlRequest, storeReportPath, testSetPath );
      final XmlHttpResponse controlResponse   = HttpHandler.createXmlHttpResponse( controlResponseFuture,   xmlResponse, timeoutMs, filterRegistry, CONTROL,   testId, testFileName, controlXmlRequest,   storeReportPath, testSetPath );

      // If we do not have a reference service, we try to read from an old XML report.
      if( referenceResponse == null && referenceXmlSetup != null) {
        referenceResponse = HttpHandler.createXmlHttpResponse( xmlRequest, xmlResponse, REFERENCE, testId, testFileName, referenceXmlSetup );
      }

      // Add reference response for comparison
      xmlTest.getResponse().setReferenceResponse( referenceResponse );

      // Calculate white noise
      final JsonDiff whiteNoise = ValidationHandler.getWhiteNoise( referenceResponse, controlResponse, outerContext.getEpsilon(), testId );

      // Invoke candidate service as late as possible because we measure the time
      xmlResponse.setRequestTime( LocalDateTime.now().toString() );
      final CompletableFuture< HttpResponse< byte[] > > candidateResponseFuture = HttpHandler.sendRequest( candidateXmlRequest, candidateHeaders, candidateBuilder, CANDIDATE, testId, testFileName );
      // Compare candidate and reference considering optional white noise differences
      final XmlHttpResponse candidateResponse = HttpHandler.createXmlHttpResponse( candidateResponseFuture, xmlResponse, timeoutMs, filterRegistry, CANDIDATE, testId, testFileName, candidateXmlRequest, storeReportPath, testSetPath );
      final boolean bodyIsJson = candidateResponse.isBodyIsJson();

      end = LocalDateTime.now();

      // Check expected values and unexpected changes
      foundDiffs = ValidationHandler.validateResponse(
        xmlResponse,
        xmlTest,
        testFileName,
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false, // Not only unexpected changes!
        outerContext.getEpsilon(),
        outerContext.getReportWhiteNoise(),
        testId
      );

      // Handle headers (read variables and store them in outer variables)
      handleTestHeaders( xmlTestSet, xmlTest, REFERENCE, referenceResponse, foundDiffs );
      handleTestHeaders( xmlTestSet, xmlTest, CONTROL,   controlResponse, foundDiffs );
      handleTestHeaders( xmlTestSet, xmlTest, CANDIDATE, candidateResponse, foundDiffs );

      if( !xmlResponse.isHideBody() ) {
        if( bodyIsJson ) {
          // Handle responses (read variables and store them in outer variables)
          handleTestResponse( xmlTestSet, xmlTest, REFERENCE, referenceResponse, foundDiffs );
          handleTestResponse( xmlTestSet, xmlTest, CONTROL,   controlResponse,   foundDiffs );
          handleTestResponse( xmlTestSet, xmlTest, CANDIDATE, candidateResponse, foundDiffs );
        }
        else {
          logConfigurationConflicts( xmlResponse, testId, "non JSON" );
        }
      }
      else {
        logConfigurationConflicts( xmlResponse, testId, "hidden" );
      }
    }
    catch( final Throwable ex ) {
      skipped  = true;
      end      = LocalDateTime.now();
      hasError = true;

      if( foundDiffs == null ) {
        foundDiffs = JsonDiff.createDataInstance();
      }

      final JsonDiffEntry jsonDiffEntry = new JsonDiffEntry( "Exception", "", "", ex.getMessage() );

      if( ex instanceof TestIgnoredException ) {
        LOG.debug( ex.getMessage() );
        jsonDiffEntry.setLogLevel( XmlLogLevel.INFO );
        hasError = false;
      }
      else if( ex instanceof HttpHandlerException ) {
        final String instance = " instance=" + ((HttpHandlerException)ex).getInstanceId();
        final Throwable innerEx = ex.getCause(); // "Remove" wrapping exception
        final String cause = " cause=" + ( innerEx.getCause() == null
          ? "null"
          : "{ class:\"" + innerEx.getCause().getClass().getSimpleName() + "\", message:\"" + innerEx.getCause().getMessage() + "\" }"
        );
        LOG.error( "Exception while handling test \"" + testId + "\"."
                 + " Probably an URL is mistyped, variables are applied incompletly or an exception occured on server side."
                 + " (file="+ testFileName + ")"
                 + instance
                 + cause, innerEx );
        jsonDiffEntry.setMessage( jsonDiffEntry.getMessage() + instance + cause ); // Expose some exception details to report
        jsonDiffEntry.setLogLevel( XmlLogLevel.ERROR );
      }
      else {
        LOG.error( "Exception while handling test \"" + testId + "\". (file="+ testFileName + ")", ex );
        jsonDiffEntry.setLogLevel( XmlLogLevel.ERROR );
      }

      foundDiffs.getChanges().add( jsonDiffEntry );
    }

    xmlTest.setAnalysis( handleAnalysis(
      foundDiffs,
      skipped,
      begin,
      end,
      testId
     ) );

    // Check over all expected
    {
      final List< JsonDiffEntry > jsonDiffEntries = validateOverAllExpected(
        null, // xmlTestSet
        xmlTest,
        xmlTest.getAnalysis()
      );
      if( !jsonDiffEntries.isEmpty() ) {
        hasError = true;
      }
    }

    // Check if an exception occurred or an expectation was violated
    if( hasError || foundDiffs.hasDifference() ) {
      if( xmlTest.isBreakOnFailure() ) {
        throw new BreakOnFailureException( "Test \"" + testId + "\" terminated unsuccessfully." );
      }
      return;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Gathers all XmlTests of the current XmlTestSet that have not been executed yet in any recursion level. The structureDepth of xmlTestSet and all XmlTests is set.
   * @param xmlTestSet The XmlTestSet to inspect. Must not be null.
   * @param xmlTest The XmlTest that broke. Must not be null.
   * @param structureDepth The depth within the test structure. The root element (setup) has depth 1.
   * @return A List that holds all XmlTests that have not been executed yet in any recursion level. May be empty but never null.
   * @throws ParseException
   */
  static List< XmlTest > getRemainingTests(
    final XmlTestSet xmlTestSet,
    final XmlTest xmlTest,
    final int structureDepth
  )
  throws ParseException
  {
    final List< XmlTest > list = new ArrayList<>();

    // Add remaining XmlTests
    boolean found = false;
    for( final XmlTest candidate : xmlTestSet.getTest() ) {
      if( !found ) {
        if( candidate == xmlTest ) {
          found = true;
        }
        continue;
      }
      else {
        initializeTest( candidate, xmlTestSet );
        candidate.setStructureDepth( structureDepth );
        list.add( candidate );
      }
    }

    // Add all encapsulated XmlTests
    for( final XmlTestSet xmlChildTestSet : xmlTestSet.getTestSet() ) {
      initializeTestSet( xmlChildTestSet, xmlTestSet );
      list.addAll( getAllTests( xmlChildTestSet, structureDepth ) );
    }

    return list;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Gathers all XmlTests of the current XmlTestSet that have not been executed yet in any recursion level. The structureDepth of all XmlTestSets and all XmlTests is set.
   * @param xmlTestSet The XmlTestSet to inspect. Must not be null.
   * @param xmlTestSetChild The XmlTestSet that broke. Must not be null.
   * @param structureDepth The depth within the test structure. The root element (setup) has depth 1.
   * @return A List that holds all XmlTests that have not been executed yet in any recursion level. May be empty but never null.
   * @throws ParseException
   */
  static List< XmlTest > getRemainingTests(
    final XmlTestSet xmlTestSet,
    final XmlTestSet xmlTestSetChild,
    final int structureDepth
  )
  throws ParseException
  {
    final List< XmlTest > list = new ArrayList<>();

    // Get remaining XmlTestSets
    final List< XmlTestSet > xmlTestSets = new ArrayList<>();
    boolean found = false;
    for( final XmlTestSet candidate : xmlTestSet.getTestSet() ) {
      if( !found ) {
        if( candidate == xmlTestSetChild ) {
          found = true;
        }
        continue;
      }
      else {
        candidate.setStructureDepth( structureDepth );
        xmlTestSets.add( candidate );
      }
    }

    // Add all encapsulated XmlTests
    for( final XmlTestSet xmlChildTestSet : xmlTestSets ) {
      initializeTestSet( xmlChildTestSet, xmlTestSet );
      list.addAll( getAllTests( xmlChildTestSet, structureDepth + 1 ) );
    }

    return list;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Gathers all XmlTests of the current XmlTestSet in any recursion level. The structureDepth of xmlTestSet and all XmlTests is set.
   * @param xmlTestSet The XmlTestSet to inspect. Must not be null.
   * @param structureDepth The depth within the test structure. The root element (setup) has depth 1.
   * @return A List that holds all XmlTests of the given XmlTestSet. May be empty but never null.
   * @throws ParseException
   */
  static List< XmlTest > getAllTests(
    final XmlTestSet xmlTestSet,
    final int structureDepth
  )
  throws ParseException
  {
    xmlTestSet.setStructureDepth( structureDepth );
    final int childStructureDepth = structureDepth + 1;

    final List< XmlTest > list = new ArrayList<>();

    // Add all XmlTests
    for( final XmlTest xmlTest : xmlTestSet.getTest() ) {
      initializeTest( xmlTest, xmlTestSet );
      xmlTest.setStructureDepth( childStructureDepth );
      list.add( xmlTest );
    }

    // Add all encapsulated XmlTests
    for( final XmlTestSet xmlChildTestSet : xmlTestSet.getTestSet() ) {
      initializeTestSet( xmlChildTestSet, xmlTestSet );
      list.addAll( getAllTests( xmlChildTestSet, childStructureDepth ) );
    }

    return list;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Logs for configuration conflicts.
   * @param xmlResponse The XmlReponse to use. Must not be null.
   * @param testId The current test id. Must not be null.
   * @param reason A conflict reason to log. Must not be null.
   */
  private static void logConfigurationConflicts(
    final XmlResponse xmlResponse,
    final String testId,
    final String reason
  )
  {
    if( !LOG.isWarnEnabled() ) {
      return;
    }

    if( xmlResponse != null
     && xmlResponse.getVariables() != null
    ) {
      LOG.warn( "Test \"" + testId + "\": Reading values from " + reason + " response body is skipped. You probably want to remove this configuration." );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a deep copy of the given XmlRequest and prepares it for usage on the service with the given id.
   * @param xmlRequest The XmlRequest object to copy.
   * @param serviceId  A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL).
   * @param xmlTest The current XmlTest. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param xmlExternalHeaders The external Headers to use. May be null.
   * @param serviceUrl The service URL. May be null. Must not end with "/".
   * @param filterRegistry The registry that holds all registered filters. Must not be null.
   * @return A XmlRequest object cloned from the passed xmlRequest. If serviceUrl is null, null is returned.
   * @throws DiffFilterException
   */
  static XmlRequest prepareXmlRequest(
    final XmlRequest xmlRequest,
    final String serviceId,
    final XmlTest xmlTest,
    final String testFileName,
    final List< XmlHeader > xmlExternalHeaders,
    final String serviceUrl,
    final Map< String, DiffFilter > filterRegistry
  )
  throws DiffFilterException
  {
    if( serviceUrl == null ) {
      return null;
    }

    final XmlRequest clonedXmlRequest = CloneHelper.deepCopyJAXB( xmlRequest, XmlRequest.class );

    final String testId = xmlTest.getId();

    // Apply variables and filters
    HttpHandler.handleRequestParameters( clonedXmlRequest, serviceId, testId,  testFileName );
    HttpHandler.createServiceUrl       ( clonedXmlRequest, serviceId, testId,  testFileName, serviceUrl );
    HttpHandler.handleRequestBody      ( clonedXmlRequest, serviceId, testId,  testFileName );
    HttpHandler.handleExternalHeaders  ( clonedXmlRequest, serviceId, testId,  testFileName, xmlExternalHeaders );
    HttpHandler.handleRequestHeaders   ( clonedXmlRequest, serviceId, testId,  testFileName );
    HttpHandler.handleUploadParts      ( clonedXmlRequest, serviceId, testId,  testFileName );
    HttpHandler.applyRequestFilters    ( clonedXmlRequest, serviceId, xmlTest, testFileName, filterRegistry, serviceId.equals( CANDIDATE ) );

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "Request of " + serviceId + " test \"" + testId + "\" after variable replacement and filter application: " + ToJson.fromXmlRequest( clonedXmlRequest ) );
    }

    // Abort hard if any variable was not resolved
    final Set< String > unresolvedVariables = VariablesHandler.findUnresolvedVariables( clonedXmlRequest );
    if( !unresolvedVariables.isEmpty() ) {
      final String message = "The " + serviceId + " test " + testId
        + " has unresolved variables: " + unresolvedVariables.toString();
      throw( new RuntimeException( message ) );
    }

    return clonedXmlRequest;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Handles the local test analysis of a XmlTest and gathers the test analysis of all tests of a XmlTestSet.
   * @param foundDiffs The found differences. May be null.
   * @param skipped Flag, if the test was skipped (true) or performed (false). If the analysis object does not refer to a test (but a test set), null may be passed.
   * @param begin The time of the test begin. May be null (if referring to a test set).
   * @param end   The time of the test end. May be null (if referring to a test set).
   * @param testId The current test id. Must not be null.
   * @return A new XmlAnalysis object that holds the combination of the given XmlAnalysis objects (xmlAnalysis and child).
   */
  private static XmlAnalysis handleAnalysis(
    final JsonDiff foundDiffs,
    final Boolean skipped,
    final LocalDateTime begin,
    final LocalDateTime end,
    final String testId
  )
  {
    XmlAnalysis result = new XmlAnalysis();

    Duration duration = ( begin != null && end != null )
      ? Duration.between( begin, end )
      : null;
    result.setFailCount    ( foundDiffs != null && foundDiffs.hasDifference() && ( skipped == null || !skipped ) ? 1 : 0 ); // NOTE: Skipped tests log the reason for skipping into the foundDiffs changes.
    result.setSkipCount    ( skipped    != null && skipped   ? 1 : 0 );
    result.setSuccessCount ( ( result.getFailCount() + result.getSkipCount() == 0 ) ? 1 : 0 );
    result.setTotalCount   ( 1 );
    result.setBegin        ( begin != null ? begin.toString() : null );
    result.setEnd          ( end   != null ? end  .toString() : null );
    result.setDuration     ( duration != null ? duration.toString() : null );
    result.setExpectedCount( foundDiffs != null ? foundDiffs.getExpectedCount() : 0 );

    if( foundDiffs != null && foundDiffs.hasDifference() ) {
      if( result.getMessages() == null ) {
        result.setMessages( new XmlMessages() );
      }
      for( final JsonDiffEntry diffEntry : foundDiffs.getChanges() ) {
        result.getMessages().getMessage().add( toXmlMessage( diffEntry ) );
      }
      for( final JsonDiffEntry diffEntry : foundDiffs.getAdditions() ) {
        result.getMessages().getMessage().add( toXmlMessage( diffEntry ) );
      }
      for( final JsonDiffEntry diffEntry : foundDiffs.getDeletions() ) {
        result.getMessages().getMessage().add( toXmlMessage( diffEntry ) );
      }

      // Sort messages by path
      Collections.sort( result.getMessages().getMessage(), new Comparator< XmlMessage >(){
        @Override
        public int compare( final XmlMessage o1, final XmlMessage o2 ) {
          int diff = o1.getLevel().compareTo( o2.getLevel() );
          if( diff != 0 ) {
            return diff;
          }
          diff = o1.getPath().compareTo( o2.getPath() );
          if( diff != 0 ) {
            return diff;
          }
          diff = o1.getValue().compareTo( o2.getValue() );
          return diff;
        }
      } );
    }

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Analysis for test/set \"" + testId + "\"=" + ToJson.fromAnalysis( result ) );
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the XmlAnalysis attributes minDuration, maxDuration and avgDuration according to the given XmlTests and XmlTestSets.
   * @param xmlTests The XmlTests. Must not be null.
   * @param xmlTestSets The XmlTestSets. Must not be null.
   * @return A new XmlAnalysis object. Never null.
   */
  private static XmlAnalysis handleXmlAnalysisDurations(
    final Collection< XmlTest > xmlTests,
    final Collection< XmlTestSet > xmlTestSets
  )
  {
    final XmlAnalysis xmlAnalysis = new XmlAnalysis();

    Duration minDuration = null;
    Duration maxDuration = null;
    Duration avgDuration = null;
    Duration durationSum = Duration.parse( "PT0S" );
    long divisor = 0;


    if( xmlTests != null ) {
      for( final XmlTest xmlTest : xmlTests ) {
        if( xmlTest.getAnalysis() == null
         || xmlTest.getAnalysis().getDuration() == null
        ) {
          // This may happen if tests are skipped due to a breakOnFailure configuration.
          continue;
        }

        divisor += 1;

        handleXmlAnalysisCounters( xmlAnalysis, xmlTest.getAnalysis() );

        final Duration testDuration = Duration.parse( xmlTest.getAnalysis().getDuration() );
        durationSum = durationSum.plus( testDuration );
        if( minDuration == null ) {
          minDuration = testDuration;
        }
        else {
          minDuration = minDuration.compareTo( testDuration ) <= 0 ? minDuration : testDuration;
        }
        if( maxDuration == null ) {
          maxDuration = testDuration;
        }
        else {
          maxDuration = maxDuration.compareTo( testDuration ) >= 0 ? maxDuration : testDuration;
        }
      }
    }

    if( xmlTestSets != null ) {
      for( final XmlTestSet xmlTestSet : xmlTestSets ) {
        if( xmlTestSet.getAnalysis() == null
         || xmlTestSet.getAnalysis().getDuration() == null
        ) {
          // This may happen if tests are skipped due to a breakOnFailure configuration.
          continue;
        }

        divisor += 1;

        handleXmlAnalysisCounters( xmlAnalysis, xmlTestSet.getAnalysis() );

        final Duration testDuration = Duration.parse( xmlTestSet.getAnalysis().getDuration() );
        durationSum = durationSum.plus( testDuration );
        if( minDuration == null ) {
          minDuration = testDuration;
        }
        else {
          minDuration = minDuration.compareTo( testDuration ) <= 0 ? minDuration : testDuration;
        }
        if( maxDuration == null ) {
          maxDuration = testDuration;
        }
        else {
          maxDuration = maxDuration.compareTo( testDuration ) >= 0 ? maxDuration : testDuration;
        }
      }
    }

    avgDuration = divisor > 0
      ? durationSum.dividedBy( divisor )
      : durationSum; // PT0S by initialization

    // minDuraion, maxDuration and avgDuration might be null, if there is an empty XmlTestSet (by accident)
    xmlAnalysis.setMinDuration( minDuration != null ? minDuration.toString() : null );
    xmlAnalysis.setMaxDuration( maxDuration != null ? maxDuration.toString() : null );
    xmlAnalysis.setAvgDuration( avgDuration != null ? avgDuration.toString() : null );

    return xmlAnalysis;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Adjusts the counters, begin, end, duration and expectedCount of the given XmlAnalysis object.
   * @param xmlAnalysis The XmlAnalysis object to adjust. NOTE: This will change as a side effect.
   * @param child The child XmlAnaylis to consider.
   */
  private static void handleXmlAnalysisCounters( final XmlAnalysis xmlAnalysis, final XmlAnalysis child )
  {
    // Handle counting
    final LocalDateTime minimumBegin = getDateTime( xmlAnalysis.getBegin() != null ? LocalDateTime.parse( xmlAnalysis.getBegin() ) : null,
                                                    child.getBegin()       != null ? LocalDateTime.parse( child.getBegin()       ) : null, true );
    final LocalDateTime maximumEnd   = getDateTime( xmlAnalysis.getEnd()   != null ? LocalDateTime.parse( xmlAnalysis.getEnd()   ) : null,
                                                    child.getEnd()         != null ? LocalDateTime.parse( child.getEnd()         ) : null, false );

    xmlAnalysis.setSuccessCount ( xmlAnalysis.getSuccessCount()   + child.getSuccessCount() );
    xmlAnalysis.setFailCount    ( xmlAnalysis.getFailCount()      + child.getFailCount() );
    xmlAnalysis.setSkipCount    ( xmlAnalysis.getSkipCount()      + child.getSkipCount() );
    xmlAnalysis.setTotalCount   ( xmlAnalysis.getTotalCount()     + child.getTotalCount() );
    xmlAnalysis.setBegin        ( minimumBegin.toString() );
    xmlAnalysis.setEnd          ( maximumEnd.toString() );
    xmlAnalysis.setDuration     ( Duration.between( minimumBegin, maximumEnd ).toString() );
    xmlAnalysis.setExpectedCount( xmlAnalysis.getExpectedCount()  + child.getExpectedCount() );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Validates all expectations defined within the overAllExpected tag (if any).
   * For each found conflict an entry for the resulting list is created and the XmlAnalysis is adjusted as a side effect.
   * @param xmlTestSet The current XmlTestSet. May be null (e.g. when invoked for a XmlTest).
   * @param xmlTest    The current XmlTest. May be null (e.g. when invoked for a XmlTestSet).
   * @param xmlAnalysis The current XmlAnalysis. Must not be null. <b>CAUTION:</b> This may be adjusted as a side effect.
   * @return A list of JsonDiffEntry objects that describe all found conflicts. May be empty but never null.
   *         If the passed non null XmlTest has no iterations set or the iterations value is one, the validation is skipped and an empty list is returned.
   */
  public static List< JsonDiffEntry > validateOverAllExpected(
    final XmlTestSet  xmlTestSet,
    final XmlTest     xmlTest,
    final XmlAnalysis xmlAnalysis
  ) {
    final List< JsonDiffEntry > result = new ArrayList<>();

    // Check if overAllExpected is defined
    final XmlOverAllExpected xmlOverAllExpected = xmlTestSet != null
      ? xmlTestSet.getOverAllExpected()
      : xmlTest.getOverAllExpected();
    if( xmlOverAllExpected == null
    ) {
      return result;
    }

    // Handle maxDuration
    if( xmlOverAllExpected.getMaxDuration() != null )
    {
      boolean skip = false;
      // Check if we ignore the maxDuration
      if( xmlTest != null && ( xmlTest.getIterations() == null || xmlTest.getIterations() == 1 ) )
      {
        LOG.warn( "Test \"" + xmlTest.getId() + "\" has no iterations set. The maxDuration entry in the overAllExpected tag is ignored." );
        skip = true;
      }
      if( xmlTestSet != null ) {
        final int childrenCount =
            xmlTestSet.getTest().size()
          + xmlTestSet.getTestSet().size()
          + xmlTestSet.getTestSetInclude().size();

        if( ( xmlTestSet.getIterations() == null || xmlTestSet.getIterations() == 1 ) && childrenCount <= 1 ) {
          LOG.warn( "TestSet \"" + xmlTestSet.getId() + "\" has no iterations set and only " + childrenCount + " children. The maxDuration entry in the overAllExpected tag is ignored." );
          skip = true;
        }
      }

      if( !skip ) {
        final String   maxDurationString   = xmlOverAllExpected.getMaxDuration();
        final String   totalDurationString = xmlAnalysis.getTotalDuration() != null
                                           ? xmlAnalysis.getTotalDuration()
                                           : xmlAnalysis.getDuration();
        final Duration maxDuration         = Duration.parse( maxDurationString );
        final Duration totalDuration       = Duration.parse( totalDurationString );

        // Check if maxDuration was exceeded
        if( totalDuration.compareTo( maxDuration ) > 0 ) {
          final String prefix = xmlTestSet != null
              ? "TestSet \"" + xmlTestSet.getId() + "\": "
              : "Test \""    + xmlTest   .getId() + "\": ";

          final JsonDiffEntry jsonDiffEntry = new JsonDiffEntry(
            "",
            totalDurationString,
            maxDurationString,
            prefix + "Maximum over all duration expected: " + maxDurationString + " but was: " + totalDurationString
          );
          jsonDiffEntry.setLogLevel( XmlLogLevel.ERROR );

          if( xmlAnalysis.getMessages() == null ) {
            xmlAnalysis.setMessages( new XmlMessages() );
          }
          xmlAnalysis.getMessages().getMessage().add( toXmlMessage( jsonDiffEntry ) );
          xmlAnalysis.setTotalCount( xmlAnalysis.getTotalCount() + 1 );
          xmlAnalysis.setFailCount ( xmlAnalysis.getFailCount () + 1 );

          result.add( jsonDiffEntry );
        }
      }
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the defined variables (id, value).
   * @param xmlVariables The XmlVariables object to read from. May be null.
   * @param base A map that holds variables that shall be used, too. Those variables may be overridden by the xmlVariables, May be null.
   * @return A Map that holds all defined variables. May be empty but never null.
   */
  static Map< String, XmlVariable > getVariablesMap(
     final XmlVariables xmlVariables,
     final Map< String, XmlVariable > base
  )
  {
    final Map< String, XmlVariable > map = new TreeMap<>();
    if( base != null ) {
      map.putAll( base );
    }

    if( xmlVariables != null ) {
      for( final XmlVariable xmlVariable : xmlVariables.getVariable() ) {
        map.put( xmlVariable.getId(), xmlVariable );
      }
    }

    return map;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Gets either the minimum or the maximum of two given LocalDateTime objects. If one of both is null, the other one is returned.
   * @param datetime1 One LocalDateTime to compare. May be null.
   * @param datetime2 Other LocalDateTime to compare. May be null.
   * @param getMinimum Flag, if the minimum datetime shall be returned (true) or the maximum (false).
   * @return Either the minimum or the maximum of two given LocalDateTime objects.
   */
  private static LocalDateTime getDateTime(
      final LocalDateTime datetime1,
      final LocalDateTime datetime2,
      final boolean getMinimum
  )
  {
    if( datetime1 == null ) {
      return datetime2;
    }
    if( datetime2 == null ) {
      return datetime1;
    }
    return (datetime1.isBefore( datetime2 ) )
      ? getMinimum
          ? datetime1
          : datetime2
      : getMinimum
          ? datetime2
          : datetime1;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the ignore paths defined in the given XmlResponse.
   * If a ignore element holds a ticket reference, this ticket reference is added to the passed XmlTest.
   * @param xmlResponse The XmlResponse to read from. May be null.
   * @param xmlTest The current XmlTest. Must not be null.
   * @return The ignore paths defined in the XmlResponse.
   */
  public static Set< String > getIgnorePaths(
    final XmlResponse xmlResponse,
    final XmlTest     xmlTest
  )
  {
    final Set< String > set = new TreeSet<>();

    if( xmlResponse != null ) {
      for( final XmlIgnore xmlIgnore : xmlResponse.getIgnore() ) {
        if( xmlIgnore.getPath() != null && !xmlIgnore.isJustExplain()) {
          final String ignorePath = xmlIgnore.getPath();
          if( ignorePath.toLowerCase().indexOf( "$." + ToJson.HEADERS_SUBPATH ) >= 0 ) {
            throw new RuntimeException( "An ignore path must not be used for headers. Use <ignore><header>...</header></ignore> instead." );
          }
          set.add( ignorePath );
        }

        // Handle ticket reference (if any)
        if( xmlIgnore.getTicketReference() != null ) {
          joinTicketReferences( xmlTest, xmlIgnore.getTicketReference() );
        }
      }
    }

    return set;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the ignore headers defined in the given XmlResponse.
   * @param xmlResponse The XmlResponse to read from. May be null.
   * @return The ignore headers defined in the XmlResponse.
   */
  public static Set< String > getIgnoreHeaders(
    final XmlResponse xmlResponse
  )
  {
    final Set< String > set = new TreeSet<>();

    if( xmlResponse != null ) {
      for( final XmlIgnore xmlIgnore : xmlResponse.getIgnore() ) {
        if( xmlIgnore.getHeader() != null ) {
           set.add( xmlIgnore.getHeader().toLowerCase() ); // HTTP spec says that header names are case-insensitive ( see "https://datatracker.ietf.org/doc/html/rfc2616#section-4.2")
        }
      }
    }

    return set;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Joins the given ticket reference(s) to those of the given XmlTest.
   * @param xmlTest The XmlTest. Must not be null.
   * @param ticketReferences The ticket reference(s) to join. May be null.
   */
  static void joinTicketReferences(
    final XmlTest xmlTest,
    final String ticketReferences
  )
  {
    if( ticketReferences == null ) {
      return;
    }
//System.out.println( "### " + xmlTest.getTicketReference() + " + " + ticketReferences );
    if( xmlTest.getTicketReference() == null ) {
      xmlTest.setTicketReference( "" );
    }

    final String[] ticketReferenceArray = ticketReferences.split( "," );
    for( int i=0; i < ticketReferenceArray.length; i++ ) {
      final String ticketReference = ticketReferenceArray[ i ].trim();
      if( !xmlTest.getTicketReference().contains( ticketReference ) ) {
        // Append ticket reference to existing (if any)
        xmlTest.setTicketReference( xmlTest.getTicketReference().trim().isEmpty()
          ? ticketReference
          : ( xmlTest.getTicketReference().trim() + "," + ticketReference )
        );
      }
    }

    if( xmlTest.getTicketReference().isEmpty() ) {
      xmlTest.setTicketReference( null );
    }
    else {
      xmlTest.setTicketReference( sortTicketReferences( xmlTest.getTicketReference() ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sorts the given comma separated String in alphabetical order.
   * @param ticketReferences The references to sort. May be null.
   * return The sorted references. If null is passed, null is returned.
   */
  static String sortTicketReferences( final String ticketReferences )
  {
    if( ticketReferences == null || ticketReferences.trim().isEmpty() ) {
      return ticketReferences;
    }

    final String[] ticketReferenceArray = ticketReferences.split( "," );
    Arrays.sort( ticketReferenceArray );
    final StringBuilder sb = new StringBuilder( ticketReferenceArray[ 0 ]);
    for( int i=1; i < ticketReferenceArray.length; i++ ) {
      sb.append( "," ).append( ticketReferenceArray[ i ] );
    }
    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a XmlMessage object from a given JsonDiffEntry.
   * @param diffEntry The JsonDiffEntry object to use. Must not be null.
   * @return A new XmlMessage object.
   */
  private static XmlMessage toXmlMessage( final JsonDiffEntry diffEntry )
  {
    final XmlMessage xmlMessage = new XmlMessage();
    xmlMessage.setLevel( diffEntry.getLogLevel() != null ? diffEntry.getLogLevel() : XmlLogLevel.ERROR );
    xmlMessage.setPath( diffEntry.getJsonPath() );
    xmlMessage.setValue( diffEntry.getMessage() );
    return xmlMessage;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Initializes the given child XmlTestSet.
   * @param xmlTestSetChild The XmlTestSet to initialize. Must not be null.
   * @param xmlTestSet The parent XmlTestSet. Must not be null.
   * @throws ParseException
   */
  static void initializeTestSet(
    final XmlTestSet xmlTestSetChild,
    final XmlTestSet xmlTestSet
  )
  throws ParseException
  {
    LOG.trace( "initializeTestSet()" );

    final String testId = xmlTestSet.getId();

    // ---------------------------------------------------------------
    // Init attributes
    // ---------------------------------------------------------------
    if( xmlTestSetChild.getReport()  == null ) { xmlTestSetChild.setReport ( xmlTestSet.getReport()  ); }
    if( xmlTestSetChild.getProject() == null ) { xmlTestSetChild.setProject( xmlTestSet.getProject() ); }
    if( xmlTestSetChild.getOrder()   == null ) { xmlTestSetChild.setOrder  ( xmlTestSet.getOrder()   ); }

    // ---------------------------------------------------------------
    // Init variables
    // ---------------------------------------------------------------
    joinVariablesForXmlTestSet( xmlTestSetChild, xmlTestSet, testId );

    // ---------------------------------------------------------------
    // Init common values
    // ---------------------------------------------------------------
    xmlTestSetChild.setFileName( joinString( xmlTestSetChild.getFileName(),       xmlTestSet.getFileName(),    testId ) );
    xmlTestSetChild.setDescription( joinString( xmlTestSetChild.getDescription(), xmlTestSet.getDescription(), testId ) );
    if( !xmlTestSet.getId().equals( xmlTestSetChild.getId() ) ) {
      xmlTestSetChild.setId( xmlTestSet.getId() + ID_SEPARATOR + xmlTestSetChild.getId() );
    }

    xmlTestSet.setDescription( VariablesHandler.applyVariables( xmlTestSet.getDescription(), xmlTestSet.getVariables(), "description of testset \"" + testId + "\"", null, testId, xmlTestSet.getFileName() ) );

    // ---------------------------------------------------------------
    // Init request
    // ---------------------------------------------------------------
    XmlRequest xmlRequest = xmlTestSetChild.getRequest();
    if( xmlRequest == null ) {
      xmlRequest = new XmlRequest();
      xmlTestSetChild.setRequest( xmlRequest );
    }
    final XmlRequest xmlTestSetRequest = xmlTestSet.getRequest();
    if( xmlTestSetRequest != null ) {
      xmlRequest.setDescription( joinString     ( xmlRequest.getDescription(), xmlTestSetRequest.getDescription(), testId ) );
      xmlRequest.setVariables  ( joinVariables  ( xmlRequest.getVariables(),   xmlTestSetRequest.getVariables(),   testId, "Joining request variables of test set \"" + xmlTestSet.getId() + "\" into request variables of test set child  \"" + xmlTestSetChild.getId() + "\"" ) );
      xmlRequest.setFilters    ( joinFilters    ( xmlRequest.getFilters(),     xmlTestSetRequest.getFilters(),     testId ) );
      xmlRequest.setParameters ( joinParameters ( xmlRequest.getParameters(),  xmlTestSetRequest.getParameters(),  testId ) );
      xmlRequest.setHeaders    ( joinHeaders    ( xmlRequest.getHeaders(),     xmlTestSetRequest.getHeaders(),     testId ) );
      xmlRequest.setBody       ( joinString     ( xmlRequest.getBody(),        xmlTestSetRequest.getBody(),        testId ) );
      xmlRequest.setUploadParts( joinUploadParts( xmlRequest.getUploadParts(), xmlTestSetRequest.getUploadParts(), testId ) );
      xmlRequest.setMethod     ( joinMethod     ( xmlRequest.getMethod(),      xmlTestSetRequest.getMethod(),      testId ) );
      xmlRequest.setEndpoint   ( joinString     ( xmlRequest.getEndpoint(),    xmlTestSetRequest.getEndpoint(),    testId ) );

      xmlRequest.setDescription( VariablesHandler.applyVariables( xmlRequest.getDescription(), xmlRequest.getVariables(), "request description of testset \"" + testId + "\"", null, testId, xmlTestSet.getFileName() ) );
    }

    // ---------------------------------------------------------------
    // Init response
    // ---------------------------------------------------------------
    XmlResponse xmlResponse = xmlTestSetChild.getResponse();
    if( xmlResponse == null ) {
      xmlResponse = new XmlResponse();
      xmlTestSetChild.setResponse( xmlResponse );
    }
    final XmlResponse xmlTestSetResponse = xmlTestSet.getResponse();
    if( xmlTestSetResponse != null ) {
      List< XmlIgnore > joinedIgnores = null;
      xmlResponse.setDescription( joinString   ( xmlResponse.getDescription(), xmlTestSetResponse.getDescription(), testId ) );
      xmlResponse.setVariables  ( joinVariables( xmlResponse.getVariables(),   xmlTestSetResponse.getVariables(),   testId, "Joining response variables of test set \"" + xmlTestSet.getId() + "\" into response variables of test set child \"" + xmlTestSetChild.getId()+ "\""  ) );
      xmlResponse.setFilters    ( joinFilters  ( xmlResponse.getFilters(),     xmlTestSetResponse.getFilters(),     testId ) );
      xmlResponse.setExpected   ( joinExpected ( xmlResponse.getExpected(),    xmlTestSetResponse.getExpected(),    testId ) );
      joinedIgnores =             joinIgnores  ( xmlResponse.getIgnore(),      xmlTestSetResponse.getIgnore(),      testId );
      xmlResponse.getIgnore().clear();
      xmlResponse.getIgnore().addAll( joinedIgnores );

      xmlResponse.setDescription( VariablesHandler.applyVariables( xmlResponse.getDescription(), xmlResponse.getVariables(), "response description of testset \"" + testId + "\"", null, testId, xmlTestSet.getFileName() ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Initializes the given XmlTest.
   * @param xmlTest The XmlTest to initialize. Must not be null.
   * @param xmlTestSet The parent XmlTestSet. Must not be null.
   * @throws ParseException
   */
  static void initializeTest(
    final XmlTest xmlTest,
    final XmlTestSet xmlTestSet
  )
  throws ParseException
  {
    LOG.trace( "initializeTest()" );

    final String testId   = xmlTest.getId();
    final String fileName = xmlTestSet.getFileName();

    // ---------------------------------------------------------------
    // Init attributes
    // ---------------------------------------------------------------
    if( xmlTest.getReport() == null ) { xmlTest.setReport ( xmlTestSet.getReport()  ); }

    // ---------------------------------------------------------------
    // Init variables
    // ---------------------------------------------------------------
    joinVariablesForXmlTest( xmlTest, xmlTestSet, testId );

    // ---------------------------------------------------------------
    // Init common values
    // ---------------------------------------------------------------
    xmlTest.setDescription( joinString( xmlTest.getDescription(), xmlTestSet.getDescription(), testId ) );
    xmlTest.setId( xmlTestSet.getId() + ID_SEPARATOR + xmlTest.getId() );

    xmlTest.setDescription( VariablesHandler.applyVariables( xmlTest.getDescription(), xmlTest.getVariables(), "description of test \"" + testId + "\"", null, testId, fileName ) );

    // ---------------------------------------------------------------
    // Init request
    // ---------------------------------------------------------------
    XmlRequest xmlRequest = xmlTest.getRequest();
    if( xmlRequest == null ) {
      xmlRequest = new XmlRequest();
      xmlTest.setRequest( xmlRequest );
    }
    final XmlRequest xmlTestSetRequest = xmlTestSet.getRequest();
    if( xmlTestSetRequest != null ) {
      xmlRequest.setDescription( joinString     ( xmlRequest.getDescription(), xmlTestSetRequest.getDescription(), testId ) );
      xmlRequest.setVariables  ( joinVariables  ( xmlRequest.getVariables(),   xmlTestSetRequest.getVariables(),   testId, "Joining request variables of test set \"" + xmlTestSet.getId() + "\" into request variables of test \"" + xmlTest.getId()+ "\""  ) );
      xmlRequest.setFilters    ( joinFilters    ( xmlRequest.getFilters(),     xmlTestSetRequest.getFilters(),     testId ) );
      xmlRequest.setParameters ( joinParameters ( xmlRequest.getParameters(),  xmlTestSetRequest.getParameters(),  testId ) );
      xmlRequest.setHeaders    ( joinHeaders    ( xmlRequest.getHeaders(),     xmlTestSetRequest.getHeaders(),     testId ) );
      xmlRequest.setBody       ( joinString     ( xmlRequest.getBody(),        xmlTestSetRequest.getBody(),        testId ) );
      xmlRequest.setUploadParts( joinUploadParts( xmlRequest.getUploadParts(), xmlTestSetRequest.getUploadParts(), testId ) );
      xmlRequest.setMethod     ( joinMethod     ( xmlRequest.getMethod(),      xmlTestSetRequest.getMethod(),      testId ) );
      xmlRequest.setEndpoint   ( joinString     ( xmlRequest.getEndpoint(),    xmlTestSetRequest.getEndpoint(),    testId ) );

      xmlRequest.setDescription( VariablesHandler.applyVariables( xmlRequest.getDescription(), xmlRequest.getVariables(), "request description of test \"" + testId + "\"", null, testId, fileName ) );
    }

    // ---------------------------------------------------------------
    // Init response
    // ---------------------------------------------------------------
    XmlResponse xmlResponse = xmlTest.getResponse();
    if( xmlResponse == null ) {
      xmlResponse = new XmlResponse();
      xmlTest.setResponse( xmlResponse );
    }

    final XmlResponse xmlTestSetResponse = xmlTestSet.getResponse();
    if( xmlTestSetResponse != null ) {
      List< XmlIgnore > joinedIgnores = null;
      xmlResponse.setDescription( joinString   ( xmlResponse.getDescription(), xmlTestSetResponse.getDescription(), testId ) );
// NOTE: We only read variables from the response but NEVER overwrite variables
//      xmlResponse.setVariables  ( joinVariables( xmlResponse.getVariables(),   xmlTestSetResponse.getVariables(),   testId ) );
      xmlResponse.setFilters    ( joinFilters  ( xmlResponse.getFilters(),     xmlTestSetResponse.getFilters(),     testId ) );
      xmlResponse.setExpected   ( joinExpected ( xmlResponse.getExpected(),    xmlTestSetResponse.getExpected(),    testId ) );
      joinedIgnores =             joinIgnores  ( xmlResponse.getIgnore(),      xmlTestSetResponse.getIgnore(),      testId );
      xmlResponse.getIgnore().clear();
      xmlResponse.getIgnore().addAll( joinedIgnores );

      xmlResponse.setDescription( VariablesHandler.applyVariables( xmlResponse.getDescription(), xmlResponse.getVariables(), "response description of test \"" + testId + "\"", null, testId, fileName ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** Joins copies of all parent XmlTestSet variable to the child XmlTestSet variables.
   * @param xmlTestSetChild The XmlTestSet child to store the parent setting into. Must not be null.
   * @param xmlTestSet The XmlTestSet to read from. Must not be null.
   * @param testId The test id. May be null.
   * @throws ParseException
   */
  static void joinVariablesForXmlTestSet(
    final XmlTestSet xmlTestSetChild,
    final XmlTestSet xmlTestSet,
    final String testId
  )
  throws ParseException
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "joinVariablesForXmlTestSet( \"" + xmlTestSet.getId() + "\"-> \"" + xmlTestSetChild.getId() + "\" )" );
    }

    // ---------------------------------------------------------------
    // Consistency check
    // ---------------------------------------------------------------
    if( xmlTestSetChild.isBreakOnFailure() && xmlTestSet.getOrder() == XmlTestOrder.RANDOM ) {
      throw new RuntimeException( "Test set \"" + xmlTestSetChild.getId() + "\" has flag breakOnFailure set true but surrounding test set \"" + xmlTestSet.getId() + "\" has order RANDOM. BreakOnFailure is valid for STRICT order only." );
    }

    // ---------------------------------------------------------------
    // Join variables
    // ---------------------------------------------------------------
    xmlTestSetChild.setVariables( joinVariables(
      xmlTestSetChild.getVariables(),
      xmlTestSet.getVariables(),
      testId,
      "Joining variables of test set \"" + xmlTestSet.getId() + "\" into test set \"" + xmlTestSetChild.getId()+ "\""
    ) );

    // ---------------------------------------------------------------
    // Join request variables
    // ---------------------------------------------------------------
    XmlRequest xmlRequest = xmlTestSetChild.getRequest();
    if( xmlRequest == null ) {
      xmlRequest = new XmlRequest();
      if( xmlTestSet.getRequest() != null ) {
        xmlRequest.setVariables( CloneHelper.deepCopyJAXB( xmlTestSet.getRequest().getVariables(), XmlVariables.class ) );
      }
      xmlTestSetChild.setRequest( xmlRequest );
    }
    // NOTE: We do NOT copy request variables due to this conflict
    // Setup               Setup
    //   var a=1             var a=1
    //   request             request
    //     var a=2             => a=1
    //   testSet             testSet
    //     => a=1              var a=3
    //     request             request
    //       => a=2              => a=1 <- Outer request variable a is not set but automatically added!
    xmlRequest.setVariables( joinVariables(
      xmlRequest.getVariables(),
      xmlTestSetChild.getVariables(),
      testId,
      "Joining variables of test set \"" + xmlTestSetChild.getId() + "\" into its request"
    ) );

    // ---------------------------------------------------------------
    // NOTE: We do NOT join response variables
    // ---------------------------------------------------------------
    XmlResponse xmlResponse = xmlTestSetChild.getResponse();
    if( xmlResponse == null ) {
      xmlResponse = new XmlResponse();
      if( xmlTestSet.getResponse() != null ) {
        xmlResponse.setVariables( CloneHelper.deepCopyJAXB( xmlTestSet.getResponse().getVariables(), XmlVariables.class ) );
      }
      xmlTestSetChild.setResponse( xmlResponse );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void joinVariablesForXmlTest(
    final XmlTest xmlTest,
    final XmlTestSet xmlTestSet,
    final String testId
  )
  throws ParseException
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "joinVariablesForXmlTest( \"" + xmlTestSet.getId() + "\"-> \"" + xmlTest.getId() + "\" )" );
    }

    // ---------------------------------------------------------------
    // Consistency check
    // ---------------------------------------------------------------
    if( xmlTest.isBreakOnFailure() && xmlTestSet.getOrder() == XmlTestOrder.RANDOM ) {
      throw new RuntimeException( "Test \"" + xmlTest.getId() + "\" has flag breakOnFailure set true but surrounding test set \"" + xmlTestSet.getId() + "\" has order RANDOM. BreakOnFailure is valid for STRICT order only." );
    }

    // ---------------------------------------------------------------
    // Join variables
    // ---------------------------------------------------------------
    xmlTest.setVariables( joinVariables(
      xmlTest.getVariables(),
      xmlTestSet.getVariables(),
      testId,
      "Joining variables of test set \"" + xmlTestSet.getId() + "\" into test \"" + xmlTest.getId()+ "\""
    ) );

    // ---------------------------------------------------------------
    // Join request variables
    // ---------------------------------------------------------------
    XmlRequest xmlRequest = xmlTest.getRequest();
    if( xmlRequest == null ) {
      xmlRequest = new XmlRequest();
      if( xmlTestSet.getRequest() != null ) {
        xmlRequest.setVariables( CloneHelper.deepCopyJAXB( xmlTestSet.getRequest().getVariables(), XmlVariables.class ) );
      }
      xmlTest.setRequest( xmlRequest );
    }
    // NOTE: We do NOT copy request variables due to this conflict
    // Setup               Setup
    //   var a=1             var a=1
    //   request             request
    //     var a=2             => a=1
    //   testSet             testSet
    //     => a=1              var a=3
    //     request             request
    //       => a=2              => a=1 <- Outer request variable a is not set but automatically added!
    xmlRequest.setVariables( joinVariables(
      xmlRequest.getVariables(),
      xmlTest.getVariables(),
      testId,
      "Joining variables of test  \"" + xmlTest.getId() + "\" into its request"
    ) );

    // ---------------------------------------------------------------
    // NOTE: We do NOT join response variables
    // ---------------------------------------------------------------
    XmlResponse xmlResponse = xmlTest.getResponse();
    if( xmlResponse == null ) {
      xmlResponse = new XmlResponse();
      if( xmlTestSet.getResponse() != null ) {
        xmlResponse.setVariables( CloneHelper.deepCopyJAXB( xmlTestSet.getResponse().getVariables(), XmlVariables.class ) );
      }
      xmlTest.setResponse( xmlResponse );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Joins the base variables into the target variables. Each variable is cloned.
   * @param target The target XmlVariables. May be null. Existing XmlVariables are not overwritten.
   * @param base   The base XmlVariables. May be null.
   * @param testId The testId. May be null.
   * @param logMessage A logMessage.
   * @return A XmlVariables object with all joined and cloned XmlVariables. May be empty but never null.
   * @throws ParseException
   */
  static XmlVariables joinVariables(
    final XmlVariables target,
    final XmlVariables base,
    final String testId,
    final String logMessage
  )
  throws ParseException
  {
    final Map< String, XmlVariable > xmlVariableById = new TreeMap<>();

    // Join XmlParameters
    if( base != null ) {
      for( final XmlVariable xmlVariable : base.getVariable() ) {
        xmlVariableById.put( xmlVariable.getId(), cloneXmlVariable( xmlVariable ) );
      }
    }
    if( target != null ) {
      for( final XmlVariable xmlVariable : target.getVariable() ) {
        xmlVariableById.put( xmlVariable.getId(), cloneXmlVariable( xmlVariable ) );
      }
    }

    // Convert to XmlValues
    final XmlVariables xmlVariables = new XmlVariables();
    for( final String id : xmlVariableById.keySet() ) {
      xmlVariables.getVariable().add( xmlVariableById.get( id ) );
    }

    if( LOG.isTraceEnabled() ) {
      LOG.trace( logMessage );
      LOG.trace( "---------------------------------------" );
      for( final XmlVariable xmlVariable : xmlVariables.getVariable() ) {
        final String value = xmlVariable.getValue() != null
          ? xmlVariable.getValue()
          : "null";
        LOG.trace( "variable id=" + xmlVariable.getId() + ", value=" + value );
      }
      LOG.trace( "---------------------------------------" );
    }

    return xmlVariables;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Resolves a function within the current XmlVariable's value (if any) and returns a clone.
   * @param xmlVariable The XmlVariable to clone Must not be null.
   * @return The cloned XmlVariable.
   * @throws ParseException
   */
  private static XmlVariable cloneXmlVariable( final XmlVariable xmlVariable )
  throws ParseException
  {
    // Resolve function in variable value (if any)
    xmlVariable.setValue( VariablesCalculator.calculateIfRequired( xmlVariable.getValue() ) );

    return CloneHelper.deepCopyJAXB( xmlVariable, XmlVariable.class );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlFilters joinFilters(
    final XmlFilters target,
    final XmlFilters base,
    final String testId
  )
  {
    if( target == null ) {
      return base;
    }
    if( base == null || base.getFilter().isEmpty() ) {
      return target;
    }

    final XmlFilters xmlFilters = new XmlFilters();
    if( target.isInherit() ) {
      // NOTE: We must preserve the filter order!
      xmlFilters.getFilter().addAll( base.getFilter() );
    }
    xmlFilters.getFilter().addAll( target.getFilter() ); // We allow duplicate filters

    return xmlFilters;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlParameters joinParameters(
    final XmlParameters target,
    final XmlParameters base,
    final String testId
  )
  {
    if( target == null || target.getParameter().isEmpty() ) {
      return base;
    }
    if( base == null || base.getParameter().isEmpty() ) {
      return target;
    }

    final Map< String, XmlParameter > xmlParameterById = new TreeMap<>();

    // Join XmlParameters
    if( base != null ) {
      for( final XmlParameter xmlParameter : base.getParameter() ) {
        xmlParameterById.put( xmlParameter.getId(), xmlParameter );
      }
    }
    if( target != null ) {
      for( final XmlParameter xmlParameter : target.getParameter() ) {
        xmlParameterById.put( xmlParameter.getId(), xmlParameter );
      }
    }

    // Convert to XmlValues
    final XmlParameters xmlParameters = new XmlParameters();
    for( final String id : xmlParameterById.keySet() ) {
      xmlParameters.getParameter().add( xmlParameterById.get( id ) );
    }

    return xmlParameters;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlHeaders joinHeaders(
    final XmlHeaders target,
    final XmlHeaders base,
    final String testId
  )
  {
    if( target == null || target.getHeader().isEmpty() ) {
      return base;
    }
    if( base == null || base.getHeader().isEmpty() ) {
      return target;
    }

    final Map< String, XmlHeader > xmlHeadersByName = new TreeMap<>();

    // Join XmlHeaders
    if( base != null ) {
      for( final XmlHeader xmlHeader : base.getHeader() ) {
        xmlHeadersByName.put( xmlHeader.getName(), xmlHeader );
      }
    }
    if( target != null ) {
      for( final XmlHeader xmlHeader : target.getHeader() ) {
        xmlHeadersByName.put( xmlHeader.getName(), xmlHeader );
      }
    }

    // Convert to XmlHeaders
    final XmlHeaders xmlHeaders = new XmlHeaders();
    for( final String name : xmlHeadersByName.keySet() ) {
      xmlHeaders.getHeader().add( xmlHeadersByName.get( name ) );
    }

    return xmlHeaders;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlUploadParts joinUploadParts(
    final XmlUploadParts target,
    final XmlUploadParts base,
    final String testId
  )
  {
    {
      if( target == null || target.getFile().isEmpty() ) {
        return base;
      }
      if( base == null || base.getFile().isEmpty() ) {
        return target;
      }

      final Map< String, XmlFile > xmlFileByFileName = new TreeMap<>();

      // Join XmlFiles
      if( base != null ) {
        for( final XmlFile xmlFile : base.getFile() ) {
          xmlFileByFileName.put( xmlFile.getValue(), xmlFile );
        }
      }
      if( target != null ) {
        for( final XmlFile xmlFile : target.getFile() ) {
          xmlFileByFileName.put( xmlFile.getValue(), xmlFile );
        }
      }

      // Convert to XmlUploadParts
      final XmlUploadParts xmlUploadParts = new XmlUploadParts();
      for( final String name : xmlFileByFileName.keySet() ) {
        xmlUploadParts.getFile().add( xmlFileByFileName.get( name ) );
      }

      return xmlUploadParts;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String joinString(
    final String target,
    final String base,
    final String testId
  )
  {
    return target != null
      ? target
      : base;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlHttpRequestMethod joinMethod(
    final XmlHttpRequestMethod target,
    final XmlHttpRequestMethod base,
    final String testId
  )
  {
    return target != null
      ? target
      : base;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlExpected joinExpected(
    final XmlExpected target,
    final XmlExpected base,
    final String testId
  )
  {
    if( target == null ) {
      return base;
    }
    if( base == null ) {
      return target;
    }

    if( target.getHttpStatus() == null ) {
      target.setHttpStatus( base.getHttpStatus() );
    }
    target.setHeaders( joinHeaders( target.getHeaders(), base.getHeaders(), testId ) );
    target.setValues ( joinValues ( target.getValues(),  base.getValues(),  testId ) );
    target.setBody   ( joinBody   ( target.getBody(),    base.getBody(),    testId ) );

    return target;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlValues joinValues(
    final XmlValues target,
    final XmlValues base,
    final String testId
  )
  {
    if( target == null || target.getValue().isEmpty() ) {
      return base;
    }
    if( base == null || base.getValue().isEmpty() ) {
      return target;
    }

    final Map< String, XmlValue > xmlValueByPath = new TreeMap<>();

    // Join XmlValues
    if( base != null ) {
      for( final XmlValue xmlValue : base.getValue() ) {
        xmlValueByPath.put( xmlValue.getPath(), xmlValue );
      }
    }
    if( target != null ) {
      for( final XmlValue xmlValue : target.getValue() ) {
        xmlValueByPath.put( xmlValue.getPath(), xmlValue );
      }
    }

    // Convert to XmlValues
    final XmlValues xmlValues = new XmlValues();
    for( final String path : xmlValueByPath.keySet() ) {
      xmlValues.getValue().add( xmlValueByPath.get( path ) );
    }

    return xmlValues;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlBody joinBody(
    final XmlBody target,
    final XmlBody base,
    final String testId
  )
  {
    if( target == null || target.getValue().isEmpty() ) {
      return base;
    }
    return target;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static List< XmlIgnore > joinIgnores(
    final List< XmlIgnore > target,
    final List< XmlIgnore > base,
    final String testId
  )
  {
    // NOTE: Since we return a List<>, no short cut for return values as in joinExpected and joinValues is allowed here.
    //       Otherwise the outer clear method would delete the result of this method.

    final Map< String, XmlIgnore > xmlIgnoreByHeader = new TreeMap<>();
    final Map< String, XmlIgnore > xmlIgnoreByPath   = new TreeMap<>();

    // Join XmlIgnore
    if( base != null ) {
      for( final XmlIgnore xmlIgnore : base ) {
        if( xmlIgnore.getHeader() != null ) {
          xmlIgnoreByHeader.put( xmlIgnore.getHeader(), xmlIgnore );
        }
        else if( xmlIgnore.getPath() != null ) {
          xmlIgnoreByPath.put( xmlIgnore.getPath(), xmlIgnore );
        }
      }
    }
    if( target != null ) {
      for( final XmlIgnore xmlIgnore : target ) {
        if( xmlIgnore.getHeader() != null ) {
          xmlIgnoreByHeader.put( xmlIgnore.getHeader(), xmlIgnore );
        }
        else if( xmlIgnore.getPath() != null ) {
          xmlIgnoreByPath.put( xmlIgnore.getPath(), xmlIgnore );
        }
      }
    }

    // Convert to List
    final List< XmlIgnore > xmlIgnores = new ArrayList<>();
    for( final String key : xmlIgnoreByHeader.keySet() ) {
      xmlIgnores.add( xmlIgnoreByHeader.get( key ) );
    }
    for( final String key : xmlIgnoreByPath.keySet() ) {
      xmlIgnores.add( xmlIgnoreByPath.get( key ) );
    }

    return xmlIgnores;
  }
}
