package com.github.kreutzr.responsediff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.reporter.AsciiDocConverter;
import com.github.kreutzr.responsediff.reporter.XsltProcessor;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.ErrorHandlingHelper;
import com.github.kreutzr.responsediff.tools.JsonHelper;

import jakarta.xml.bind.JAXBException;

/**
 * Class to perform response diff testing.
 */
public class ResponseDiff
{
   private static final Logger LOG = LoggerFactory.getLogger( ResponseDiff.class );

   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss" );

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private final String                    reportTitle_;
   private final String                    testIdPattern_;
   private final String                    xsltFilePath_;
   private final String                    reportFileEnding_;
   private final String                    reportConversionFormats_;
   private final String                    storeReportPath_;
   private final boolean                   reportWhiteNoise_;
   private final boolean                   maskAuthorizationHeaderInCurl_;
   private final boolean                   reportControlResponse_;
   private final String                    executionContextAsString_;
   private final String                    ticketServiceUrl_;
   private final String                    candidateServiceUrl_;
   private final List< XmlHeader >         candidateHeaders_;
   private final String                    referenceServiceUrl_;
   private final List< XmlHeader >         referenceHeaders_;
   private final String                    controlServiceUrl_;
   private final List< XmlHeader >         controlHeaders_;
   private final long                      timeoutMs_;
   private final double                    epsilon_;
   private final String                    referenceFilePath_;
   private final boolean                   exitWithExitCode_;

   private       XmlResponseDiffSetup      xmlTestSetup_;
   private       Map< String, DiffFilter > filterRegistry_; // NOTE: Since filters are identified by id not by class, multiple instances of the same filter class are supported.

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Constructor
    * @param rootPath The root path from where all relative file paths start. Must not be null.
    * @param xmlFilePath The configuration file (XML) that describes the test setup.
    * @param reportTitle The report title. May be null.
    * @param testIdPattern The id pattern of the tests to execute. May be null. (default null means that all tests are executed)
    * @param xsltFilePath The path to the XSLT file to use. May be null.
    * @param reportFileEnding The file ending of the report that may be created by XSLT at the the given xsltFilePath.
    * @param reportConversionFormats The target formats (as comma separated string, e.g. "pdf,html") an AscciiDoc report shall be converted to. Only considered if reportFileEnding is "adoc".
    * @param storeReportPath The path to store the report in. Must not be null.
    * @param reportWhiteNoise Flag, if any different value shall be reported (true) or only those that were not discovered to be white noise (differences between reference and control, or expected differences) (false).
    * @param ticketServiceUrl The URL of the ticket server. This is used for links within the generated documentation. May be null.
    * @param candidateServiceUrl The URL of the candidate server. Must not be null.
    * @param candidateHeaders A XmlHeaders object. May be null. This is required for e.g. passing server individual authentication headers.
    * @param referenceServiceUrl The URL of the reference server. May only be null if referenceFilePath is not null.
    * @param referenceHeaders A XmlHeaders object. May be null. This is required for e.g. passing server individual authentication headers.
    * @param controlServiceUrl The URL of the control server. Must be different to both - the reference and the candidate server. May be null.
    * @param controlHeaders   A XmlHeaders object. May be null. This is required for e.g. passing server individual authentication headers.
    * @param timeoutMs The timeout in milliseconds to receive a HTTP response.
    * @param epsilon The epsilon for decimal comparison. Must not be null.
    * @param referenceFilePath Optional filename that points to a XML report that shall be used to simulate reference responses, if no reference service URL is configured. May be null.
    * @param exitWithExitCode Flag, if an exit code shall be returned or not. (Pass false for local development.)
    * @throws Exception If an error occurs during the initialization, an Exception is thrown
    */
   public ResponseDiff(
     final String            rootPath,
     final String            xmlFilePath,
     final String            reportTitle,
     final String            testIdPattern,
     final String            xsltFilePath,
     final String            reportFileEnding,
     final String            reportConversionFormats,
     final String            storeReportPath,
     final boolean           reportWhiteNoise,
     final boolean           maskAuthorizationHeaderInCurl,
     final boolean           reportControlResponse,
     final String            executionContextAsString,
     final String            ticketServiceUrl,
     final String            candidateServiceUrl,
     final List< XmlHeader > candidateHeaders,
     final String            referenceServiceUrl,
     final List< XmlHeader > referenceHeaders,
     final String            controlServiceUrl,
     final List< XmlHeader > controlHeaders,
     final long              timeoutMs,
     final double            epsilon,
     final String            referenceFilePath,
     final boolean           exitWithExitCode
   )
   throws Exception
   {
     reportTitle_                   = reportTitle;
     testIdPattern_                 = testIdPattern;
     xsltFilePath_                  = ( xsltFilePath == null ) ? null : rootPath + xsltFilePath;
     reportFileEnding_              = reportFileEnding;
     reportConversionFormats_       = reportConversionFormats;
     storeReportPath_               = rootPath + storeReportPath
                                    + ( ( storeReportPath.endsWith( "\\" ) || storeReportPath.endsWith( "/" )) ? "" : File.separator ) // Assure there is a tailing file separator
                                    + getTimeStampFolder();
     reportWhiteNoise_              = reportWhiteNoise;
     maskAuthorizationHeaderInCurl_ = maskAuthorizationHeaderInCurl;
     reportControlResponse_         = reportControlResponse;
     executionContextAsString_      = executionContextAsString;
     ticketServiceUrl_              = ticketServiceUrl;
     candidateServiceUrl_           = unifyUrl( candidateServiceUrl );
     candidateHeaders_              = candidateHeaders;
     referenceServiceUrl_           = unifyUrl( referenceServiceUrl );
     referenceHeaders_              = referenceHeaders;
     controlServiceUrl_             = unifyUrl( controlServiceUrl );
     controlHeaders_                = controlHeaders;
     timeoutMs_                     = timeoutMs;
     epsilon_                       = epsilon;
     referenceFilePath_             = referenceFilePath;
     exitWithExitCode_              = exitWithExitCode;

     xmlTestSetup_                  = null;
     filterRegistry_                = null;
     setXmlFilePath( xmlFilePath );

     if( candidateServiceUrl_ == null ) {
       throw new RuntimeException( "A candidate server url is required." );
     }
     if( referenceServiceUrl_ == null ) {
       if( referenceFilePath_ == null ) {
         throw new RuntimeException( "Either a reference server url or a reference file path is required ." );
       }
     }
     else if( referenceServiceUrl_.equalsIgnoreCase( candidateServiceUrl_ ) ) {
       LOG.warn( "The candidate and reference server are identical. It is most probably a misconfiguration." );
     }
     if( controlServiceUrl_ != null
         && ( candidateServiceUrl_.equalsIgnoreCase( controlServiceUrl_ ) || referenceServiceUrl_.equalsIgnoreCase( controlServiceUrl_ ) ) ) {
       throw new RuntimeException( "The control server url must differ from the reference and candidate server urls." );
     }
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private static String getTimeStampFolder()
   {
     return DATE_FORMAT.format( new Date() ) + File.separator;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Cuts off tailing "/" characters of the given URL.
    * @param url The URL to unify. May be null.
    * @return The unified URL. If null was passed, null is returned.
    */
   private static String unifyUrl( final String url )
   {
     if( url == null ) {
       return null;
     }

     return url.endsWith( "/" ) // The service URL must not end with a "/" (which is supposed to be spart of the endpoint path)
       ? url.substring( 0, url.length()-1 )
       : url;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Allows to change the XML test setup.
    * <br/><b>NOTE:</b> This (re-)sets xmlTestSetup_ as a <b>side effect</b>!
    * @param xmlFilePath The configuration file (XML) that describes the test setup.
    * @throws Exception
    */
   public void setXmlFilePath( final String xmlFilePath ) throws Exception
   {
     String testSetupPath = null;
     if( xmlFilePath != null ) {                                    // xmlFilePath = "C:\develop\responseDiff\myTest\setup.xml"
       final Path parentPath = Path.of( xmlFilePath ).getParent();  // parentPath  = "C:\develop\responseDiff\myTest"
       if( parentPath != null ) {
         final String rootPath = new File( "" ).getAbsolutePath() + File.separator; // Current working directory  "C:\develop\responseDiff\"
         testSetupPath = parentPath.toString() + File.separator;    // testSetupPath = "C:\develop\responseDiff\myTest/"

         // Cut off leading root path (independent of case and file separators)
         if( testSetupPath        .toLowerCase().replaceAll("\\\\", "#" ).replaceAll( "/", "#" )
             .startsWith( rootPath.toLowerCase().replaceAll("\\\\", "#" ).replaceAll( "/", "#" ) )
         ) {
           testSetupPath = testSetupPath.substring( rootPath.length() ); // testSetupPath = "myTest/"
         }
       }
     }

     xmlTestSetup_   = XmlFileHandler.readSetup( xmlFilePath, true );
     filterRegistry_ = FilterRegistryHelper.getFilterRegistry( xmlTestSetup_, testSetupPath );

     setRuntimeInformation( xmlTestSetup_ );

//if( LOG.isTraceEnabled() ) {
//   LOG.trace( "### setup=" + JsonHelper.provideObjectMapper().writeValueAsString( xmlTestSetup_ ) );
//}

     xmlTestSetup_.setTicketServiceUrl( ticketServiceUrl_ );
     if( reportTitle_ != null ) {
       xmlTestSetup_.setReportTitle( reportTitle_ );
     }
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private void setRuntimeInformation( final XmlResponseDiffSetup xmlTestSetup )
   throws IOException
   {
     XmlRuntime xmlRuntime = xmlTestSetup.getRuntime();
     if( xmlRuntime == null ) {
       xmlRuntime = new XmlRuntime();
       xmlTestSetup.setRuntime( xmlRuntime );
     }
     PropertyReader propertiesReader = new PropertyReader( "responsediff.properties" );
     xmlRuntime.setBuildVersion( Converter.asString( propertiesReader.getProperty( "build-version"   ), "" ) );
     xmlRuntime.setBuildTime   ( Converter.asString( propertiesReader.getProperty( "build-timestamp" ), "" ) );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @return The test setup. May be null.
    */
   public XmlResponseDiffSetup getTestSetup()
   {
     return xmlTestSetup_;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Runs the tests configured within the setup file.
    * @throws IOException
    * @throws JAXBException
   * @throws SAXException
   * @throws ParseException
    */
   public void runLocalTests() throws JAXBException, IOException, SAXException, ParseException
   {
     // Leave this for debugging
     //XmlFileHandler.storeXmlReport( xmlTestSetup_, storeReportPath_, "-raw" );

     final Pattern testIdPattern = testIdPattern_ != null
       ? Pattern.compile( testIdPattern_ )
       : null;

     LOG.info( "Starting test processing." );

     TestSetHandler.processTestSetup(
       testIdPattern,
       xmlTestSetup_,
       candidateServiceUrl_,
       candidateHeaders_,
       referenceServiceUrl_,
       referenceHeaders_,
       controlServiceUrl_,
       controlHeaders_,
       filterRegistry_,
       timeoutMs_,
       epsilon_,
       referenceFilePath_,
       storeReportPath_,
       reportWhiteNoise_,
       maskAuthorizationHeaderInCurl_,
       reportControlResponse_,
       executionContextAsString_
     );

     LOG.info( "Storing XML report." );

     // Store test setup with all analysis results
     final String xmlReportFileName = XmlFileHandler.storeXmlReport( xmlTestSetup_, storeReportPath_, null );

     // Convert XML report to e.g. HTML or ADOC
     if( xsltFilePath_ != null ) {
       Document doc = null;
       try {
         doc = XmlFileHandler.toDocument( xmlTestSetup_ );
       }
       catch( final Exception ex ) {
         final String message = ErrorHandlingHelper.createSingleLineMessage( "Error while XML Document creation.", ex );
         LOG.error( message );
       }

       if( doc != null ) {
         LOG.info( "Transforming XML report to \"" + reportFileEnding_ + "\"." );

         int pos = xmlReportFileName.lastIndexOf( '.' );
         final String reportFilePath = xmlReportFileName.substring( 0, pos+1 ) + reportFileEnding_;
         XsltProcessor.process( doc, xsltFilePath_, reportFilePath );

         // Convert AsciiDoc report to configured formats
         if( reportFileEnding_.equalsIgnoreCase( "adoc" ) && reportConversionFormats_ != null ) {
           final String[] targetFormats = reportConversionFormats_.split( "," );
           int conversionCount = 0;
           for( int i=0; i < targetFormats.length; i++ ) {
             final String targetFormat = targetFormats[ i ].trim().toLowerCase();
             if( targetFormat.isEmpty() ) {
               continue;
             }

             LOG.info( "Converting \"adoc\" report to \"" + targetFormat + "\"." );

             pos = reportFilePath.lastIndexOf( "." );
             final String targetFilePath = reportFilePath.substring(0, pos+1 ) + targetFormat;

             try {
               switch ( targetFormat ) {
                 case "pdf":
                   AsciiDocConverter.toPdf( reportFilePath, targetFilePath );
                   conversionCount += 1;
                   break;
                 case "html":
                   AsciiDocConverter.toHtml( reportFilePath, targetFilePath );
                   conversionCount += 1;
                   break;
                 default:
                   LOG.warn( "Don't know how to convert adoc to \"" + targetFormat + "\"." );
                   break;
                }
             }
             catch( final Exception ex ) {
               ex.printStackTrace();
               LOG.error( "Error while converting adoc to \"" + targetFormat + "\"." );
             }
           } // for
           if( LOG.isDebugEnabled() ) {
             LOG.debug( "Converted adoc " + conversionCount + " times." );
           }
         }
       }
     }

     LOG.info( "Finished." );

     if( exitWithExitCode_ ) {
       // Terminate with exit code.
       final XmlAnalysis xmlAnalysis = xmlTestSetup_.getAnalysis();
       final int exitCode = ( xmlAnalysis.getFailCount() > 0 || xmlAnalysis.getSkipCount() > 0 ) ? 1 : 0;

       if( LOG.isDebugEnabled() ) {
         LOG.debug( "Exiting with code " + exitCode );
       }
       System.exit( exitCode );
     }
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Runs tests that were sent from a remote client.
    */
   public void runRemoteTests()
   {
     // NEEDS FIX B: Implement this!
     LOG.warn( "Running remote tests (http forwarding) is not supported yet." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public static void main( final String[] args )
   {
     if( args == null || args.length != 1 ) {
       LOG.error( "The configuration JSON parameter is missing. Pass it as first (only) parameter." );
       System.exit( 1 );
     }

     ResponseDiffConfiguration config = null;
     try
     {
       config = JsonHelper.provideObjectMapper().readValue( args[ 0 ], ResponseDiffConfiguration.class );
     }
     catch( final Exception ex )
     {
       ex.printStackTrace();
       System.exit( 1 );
     }

     String   rootPath                      = new File( "" ).getAbsolutePath() + File.separator;
     String   xmlFilePath                   = null;
     String   reportTitle                   = null;
     String   testIdPattern                 = null;
     String   xsltFilePath                  = "src/main/resources/com/github/kreutzr/responsediff/reporter/report-to-adoc.xslt";
     String   reportFileEnding              = "adoc";
     String   reportConversionFormats       = "pdf";
     String   storeResultPath               = "../test-results/";
     Boolean  reportWhiteNoise              = false;
     Boolean  maskAuthorizationHeaderInCurl = true;
     Boolean  reportControlResponse         = false;
     String   executionContextAsString      = null;
     String   ticketServiceUrl              = null;
     String   candidateServiceUrl           = null;
     String   referenceServiceUrl           = null;
     String   controlServiceUrl             = null;
     Long     responseTimeoutMs             = 1000L;
     Double   epsilon                       = Constants.EPSILON;
     String   referenceFilePath             = null;
     Boolean  exitWithExitCode              = true; // Disable for local IDE testing
     long     startupSleepMs                = -1;

     // Read parameters from configuration
     rootPath                      = Converter.asString ( config.getRootPath(),                      rootPath );
     xmlFilePath                   = Converter.asString ( config.getXmlFilePath(),                   xmlFilePath );
     reportTitle                   = Converter.asString ( config.getReportTitle(),                   reportTitle );
     testIdPattern                 = Converter.asString ( config.getTestIdPattern(),                 testIdPattern );
     xsltFilePath                  = Converter.asString ( config.getXsltFilePath(),                  xsltFilePath );
     reportFileEnding              = Converter.asString ( config.getReportFileEnding(),              reportFileEnding );
     reportConversionFormats       = Converter.asString ( config.getReportConversionFormats(),       reportConversionFormats );
     storeResultPath               = Converter.asString ( config.getStoreResultPath(),               storeResultPath );
     reportWhiteNoise              = Converter.asBoolean( config.getReportWhiteNoise(),              reportWhiteNoise );
     maskAuthorizationHeaderInCurl = Converter.asBoolean( config.getMaskAuthorizationHeaderInCurl(), maskAuthorizationHeaderInCurl );
     reportControlResponse         = Converter.asBoolean( config.getReportControlResponse(),         reportControlResponse );
     executionContextAsString      = Converter.asString ( config.getExecutionContext(),              executionContextAsString );
     ticketServiceUrl              = Converter.asString ( config.getTicketServiceUrl(),              ticketServiceUrl );
     candidateServiceUrl           = Converter.asString ( config.getCandidateServiceUrl(),           candidateServiceUrl );
     referenceServiceUrl           = Converter.asString ( config.getReferenceServiceUrl(),           referenceServiceUrl );
     controlServiceUrl             = Converter.asString ( config.getControlServiceUrl(),             controlServiceUrl );
     responseTimeoutMs             = Converter.asLong   ( config.getResponseTimeoutMs(),             responseTimeoutMs );
     epsilon                       = Converter.asDouble ( config.getEpsilon(),                       epsilon );
     referenceFilePath             = Converter.asString ( config.getReferenceFilePath(),             referenceFilePath );
     exitWithExitCode              = Converter.asBoolean( config.isExitWithExitCode(),               exitWithExitCode );
     startupSleepMs                = Converter.asLong   ( config.getStartupSleepMs(),                startupSleepMs );

     if( testIdPattern != null
       && ( testIdPattern.trim().isEmpty() || testIdPattern.trim().equals( "null" ) )
     ) {
       testIdPattern = null;
     }
     if( referenceServiceUrl != null
       && ( referenceServiceUrl.trim().isEmpty() || referenceServiceUrl.trim().equals( "null" ) )
     ) {
         referenceServiceUrl = null;
     }
     if( controlServiceUrl != null
       && ( controlServiceUrl.trim().isEmpty() || controlServiceUrl.trim().equals( "null" ) )
     ) {
       controlServiceUrl = null;
     }

     List< XmlHeader > candidateHeaders = new ArrayList<>();
     if( config.getCandidateHeaders() != null ) {
       candidateHeaders = config.getCandidateHeaders();
     }
     List< XmlHeader > referenceHeaders = new ArrayList<>();
     if( config.getReferenceHeaders() != null ) {
       referenceHeaders = config.getReferenceHeaders();
     }
     List< XmlHeader > controlHeaders = new ArrayList<>();
     if( config.getControlHeaders() != null ) {
       controlHeaders = config.getControlHeaders();
     }

     // Idle delay (e.g. to hook on with profiler)
     if( startupSleepMs > 0 )
     try {
       Thread.sleep( startupSleepMs );
     }
     catch( final Exception ex )
     {
       // Ignore this.
     }

     try {
       final ResponseDiff responseDiff = new ResponseDiff(
         rootPath,
         xmlFilePath,
         reportTitle,
         testIdPattern,
         xsltFilePath,
         reportFileEnding,
         reportConversionFormats,
         storeResultPath,
         reportWhiteNoise,
         maskAuthorizationHeaderInCurl,
         reportControlResponse,
         executionContextAsString,
         ticketServiceUrl,
         candidateServiceUrl,
         candidateHeaders,
         referenceServiceUrl,
         referenceHeaders,
         controlServiceUrl,
         controlHeaders,
         responseTimeoutMs,
         epsilon,
         referenceFilePath != null && !referenceFilePath.isEmpty() ? ( rootPath + referenceFilePath ) : null,
         exitWithExitCode
      );

      responseDiff.runLocalTests();
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      LOG.error( "An error occurred: " + ex.getMessage(), ex );
      System.exit( 2 );
    }
  }
}
