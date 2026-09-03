package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;
import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.filter.DummyRequestFilter;
import com.github.kreutzr.responsediff.filter.request.RemoveHeaderRequestFilter;

public class HttpHandlerTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( HttpHandler.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testReadFileNameFromContentDispositionHeaderWorks()
  {
    // Given

    // When / Then
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( null      ) ).isEqualTo( null );
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "inline;" ) ).isEqualTo( null );
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "inline; filename = test.txt"     ) ).isEqualTo( "test.txt" );
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = test.txt"             ) ).isEqualTo( "test.txt" );
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = \"test.txt\""         ) ).isEqualTo( "test.txt" );
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = \"\""                 ) ).isEqualTo( "" );
    assertThat( HttpHandler.readFileNameFromContentDispositionHeader( "filename = test.txt ; more-text" ) ).isEqualTo( "test.txt" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatCreateServiceUrlWorks()
  {
    // Given
    final XmlRequest xmlRequest = new XmlRequest();

    // When / Then
    xmlRequest.setEndpoint( "myEndpoint" ); // HINT: Missing leading "/"
    HttpHandler.createServiceUrl( xmlRequest, TestSetHandler.CANDIDATE, "TEST_ID", "TESTFILE_NAME", "http://my-server:1234" );
    assertThat( xmlRequest.getEndpoint() ).isEqualTo( "http://my-server:1234/myEndpoint" );

    xmlRequest.setEndpoint( "/myEndpoint" );
    HttpHandler.createServiceUrl( xmlRequest, TestSetHandler.CANDIDATE, "TEST_ID", "TESTFILE_NAME", "https://my-server:1234" );
    assertThat( xmlRequest.getEndpoint() ).isEqualTo( "https://my-server:1234/myEndpoint" );

    xmlRequest.setEndpoint( "https://my-server:1234/myEndpoint?a=1&b=2" );
    HttpHandler.createServiceUrl( xmlRequest, TestSetHandler.CANDIDATE, "TEST_ID", "TESTFILE_NAME", "https://my-server:1234" );
    assertThat( xmlRequest.getEndpoint() ).isEqualTo( "https://my-server:1234/myEndpoint?a=1&b=2" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatIsBodyJsonWorks()
  {
    // Given

    // When / Then
    assertThat( HttpHandler.isJsonResponse( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON     ) ).isTrue();
    assertThat( HttpHandler.isJsonResponse( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API ) ).isTrue();
    assertThat( HttpHandler.isJsonResponse( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON_API + ";ext=\"https://jsonapi.org/ext/atomic\"" ) ).isTrue();
    assertThat( HttpHandler.isJsonResponse( "application/text" ) ).isFalse();
    assertThat( HttpHandler.isJsonResponse( null ) ).isFalse();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatGetFileExtensionFromContentTypeWorks()
  {
    // Given

    // When / Then
    assertThat( HttpHandler.getFileExtensionFromContentType( null ) ).isEqualTo( "bin" );
    assertThat( HttpHandler.getFileExtensionFromContentType( "text/plain" ) ).isEqualTo( "txt" );
    assertThat( HttpHandler.getFileExtensionFromContentType( "application/hal+json" ) ).isEqualTo( "json" );
    assertThat( HttpHandler.getFileExtensionFromContentType( "application/hal+json; charset=UTF-8" ) ).isEqualTo( "json" );
    assertThat( HttpHandler.getFileExtensionFromContentType( "image/png" ) ).isEqualTo( "png" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatGetUploadFilepathWorks()
  {
    // Given
    final XmlFile xmlFile1 = new XmlFile();
    xmlFile1.setName( "THIS IS IGNORED" );
    xmlFile1.setValue( "c:/test-folder/regression/test_A/test-folder/test-image.png" );
    final String testFileName1 = null;

    // When / Then
    assertThat( HttpHandler.getUploadFilepath( xmlFile1, testFileName1 ) ).isEqualTo( "c:/test-folder/regression/test_A/test-folder/test-image.png" );

    // Given
    final XmlFile xmlFile2 = new XmlFile();
    xmlFile2.setName( "THIS IS IGNORED" );
    xmlFile2.setValue( "../test_B/test-folder/test-image.png" );
    final String testFileName2 = "c:/test-folder/regression/test_A/setup.xml";

    // When / Then
    assertThat( HttpHandler.getUploadFilepath( xmlFile2, testFileName2 ) ).isEqualTo( "c:/test-folder/regression/test_A/../test_B/test-folder/test-image.png" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatToCurlWorks()
  {
    // Given
    final XmlHeader xmlHeader1 = new XmlHeader();
    xmlHeader1.setName( "authorization" );
    xmlHeader1.setValue( "Basic <credentials>" );
    final XmlHeader xmlHeader2 = new XmlHeader();
    xmlHeader2.setName( "accept" );
    xmlHeader2.setValue( "application/json" );
    final String            candidateServiceUrl = "https://candidate/service/endpoint";
    final List< XmlHeader > candidateHeaders    = Arrays.asList( xmlHeader1, xmlHeader2 );
    final String            referenceServiceUrl = "https://reference/service/endpoint";
    final List< XmlHeader > referenceHeaders    = Arrays.asList( xmlHeader1, xmlHeader2 );
    final String            controlServiceUrl = null;
    final List< XmlHeader > controlHeaders    = null;
    final Map< String, DiffFilter > filterRegistry = null;
    final Pattern           testIdPattern = null;
    final long              timeoutMs = 5;
    final double            epsilon = Constants.EPSILON;
    final String            storeReportPath = null;
    final boolean           reportWhiteNoise = false;
    final boolean           maskAuthorizationHeaderInCurl = true;
    final boolean           reportControlResponse = false;
    final String            executionContextAsString = null;
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
      maskAuthorizationHeaderInCurl,
      reportControlResponse,
      executionContextAsString
    );
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().addAll( candidateHeaders );
    final XmlRequest xmlRequest = new XmlRequest();
    xmlRequest.setMethod( XmlHttpRequestMethod.GET );
    xmlRequest.setEndpoint( candidateServiceUrl );
    xmlRequest.setBody( "{\"a\":1}" );
    final String testFileName = "c:/test-folder/regression/test_A/setup.xml";

    // When / Then  (Test without headers)
    xmlRequest.setHeaders( null );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X GET https://candidate/service/endpoint -d '{\"a\":1}'" );

    // When / Then  (Test all HTTP methods)
    xmlRequest.setHeaders( xmlHeaders );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X GET https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.HEAD );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X HEAD https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.POST );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X POST https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.PUT );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X PUT https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.DELETE );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X DELETE https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.CONNECT );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X CONNECT https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.OPTIONS );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X OPTIONS https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.TRACE );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X TRACE https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
    xmlRequest.setMethod( XmlHttpRequestMethod.PATCH );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X PATCH https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -d '{\"a\":1}'" );
//    xmlRequest.setMethod( XmlHttpRequestMethod.fromValue( "UNDEFINED" ) );
//    Throwable ex = catchThrowable( () -> HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) );
//    assertThat( ex )
//      .isInstanceOf( RuntimeException.class )
//      .hasMessageContaining( "ERROR: Request method \"UNDEFINED\" is not yet supported." );

    // When / Then (test authorization masking)
    outerContext.setMaskAuthorizationHeaderInCurl_( false );  // Option under test
    xmlRequest.setMethod( XmlHttpRequestMethod.GET );
    xmlRequest.setBody( null );                               // Option under test
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X GET https://candidate/service/endpoint -H \"authorization: Basic <credentials>\" -H \"accept: application/json\"" );

    // When / Then (test upload files)
    outerContext.setMaskAuthorizationHeaderInCurl_( true );
    final XmlFile xmlFile1 = new XmlFile();
    xmlFile1.setName( "uploadFile1" );
    xmlFile1.setValue( "c:/test-folder/regression/test_A/test-folder/test-image.png" );
    xmlFile1.setContentType( "image/png" );
    final XmlFile xmlFile2 = new XmlFile();
    xmlFile2.setName( "uploadFile2" );
    xmlFile2.setValue( "../test_B/test-folder/test-document.pdf" );
    xmlFile2.setContentType( "application/pdf" );
    final List< XmlFile > uploadFiles = Arrays.asList( xmlFile1, xmlFile2);
    final XmlUploadParts xmlUploadParts = new XmlUploadParts();
    xmlUploadParts.getFile().addAll( uploadFiles );
    xmlRequest.setMethod( XmlHttpRequestMethod.POST );
    xmlRequest.setUploadParts( xmlUploadParts );
    assertThat( HttpHandler.toCurl( xmlRequest, testFileName, outerContext ) ).isEqualTo( "curl -X POST https://candidate/service/endpoint -H \"authorization: ...\" -H \"accept: application/json\" -F 'uploadFile1=@c:/test-folder/regression/test_A/test-folder/test-image.png;type=image/png' -F 'uploadFile2=@c:/test-folder/regression/test_A/../test_B/test-folder/test-document.pdf;type=application/pdf'" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatIsSuccessStatusCodeWorks()
  {
    assertThat( HttpHandler.isSuccessStatusCode( -299 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( -200 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( -1 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 0 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 20 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 100 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 199 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 200 ) ).isTrue();
    assertThat( HttpHandler.isSuccessStatusCode( 299 ) ).isTrue();
    assertThat( HttpHandler.isSuccessStatusCode( 300 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 400 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 500 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 600 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 700 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 800 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 900 ) ).isFalse();
    assertThat( HttpHandler.isSuccessStatusCode( 1000 ) ).isFalse();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatReadCharsetFromContentTypeHeaderWorks()
  {
    final Charset fallback = StandardCharsets.UTF_8;
    assertThat( HttpHandler.readCharsetFromContentTypeHeader( null,               fallback ) ).isEqualTo( fallback );
    assertThat( HttpHandler.readCharsetFromContentTypeHeader( "application/json", null     ) ).isNull();
    assertThat( HttpHandler.readCharsetFromContentTypeHeader( "application/json", fallback ) ).isEqualTo( fallback );
    assertThat( HttpHandler.readCharsetFromContentTypeHeader( "application/json; charset=iso8859-1", fallback ) ).isEqualTo( StandardCharsets.ISO_8859_1 );
    assertThat( HttpHandler.readCharsetFromContentTypeHeader( "application/json; charset=iso8859-1 something-else", fallback ) ).isEqualTo( StandardCharsets.ISO_8859_1 );
    assertThat( HttpHandler.readCharsetFromContentTypeHeader( "application/json; charset=iso8859-1;something-else", fallback ) ).isEqualTo( StandardCharsets.ISO_8859_1 );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatIsCompressedWorks()
  {
    assertThat( HttpHandler.isCompressed( null ) ).isFalse();

    final List< XmlHeader > xmlHeaders = new ArrayList<>();
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isFalse();

    XmlHeader xmlHeader1 = new XmlHeader();
    xmlHeader1.setName( HttpHandler.HEADER_NAME__CONTENT_TYPE );
    xmlHeader1.setValue( "application/json" );
    xmlHeaders.add(xmlHeader1 );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isFalse();

    XmlHeader xmlHeader2 = new XmlHeader();
    xmlHeader2.setName( HttpHandler.HEADER_NAME__CONTENT_ENCODING );
    xmlHeader2.setValue( "GZIP" );
    xmlHeaders.add(xmlHeader2 );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isTrue();
    xmlHeader2.setValue( "CompREss" );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isTrue();
    xmlHeader2.setValue( "deflate" );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isTrue();
    xmlHeader2.setValue( "deflate" );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isTrue();
    xmlHeader2.setValue( "br" );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isTrue();
    xmlHeader2.setValue( "zstd" );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isTrue();

    xmlHeader2.setValue( "UNDEFINED" );
    assertThat( HttpHandler.isCompressed( xmlHeaders ) ).isFalse();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatCreateXmlHttpResponseWorks()
  {
    CompletableFuture< HttpResponse< byte[] > > httpResponseFuture = null;
    final XmlResponse xmlResponse = new XmlResponse();
    final long        timeoutMs = 5;
    final Map< String, DiffFilter > filterRegistry = null;
    final String      serviceId = TestSetHandler.CANDIDATE;
    final String      testId = "TEST_A";
    final String      testFileName = "setup.xml";
    final XmlRequest  xmlRequest = new XmlRequest();
    final String      storeReportPath = "./test-results/reports/";
    final String      testSetPath = "./test-folder/regression/";
    final String      testSetWorkPath = "./downloads";

    try {
      // When / Then (future = null)
      assertThat( HttpHandler.createXmlHttpResponse(
        httpResponseFuture,
        xmlResponse,
        timeoutMs,
        filterRegistry,
        serviceId,
        testId,
        testFileName,
        xmlRequest,
        storeReportPath,
        testSetPath,
        testSetWorkPath
      ) ).isNull();

      // When / Then (future != null)
      httpResponseFuture = new CompletableFuture< HttpResponse< byte[] > >() {
        public HttpResponse< byte[] > get( final long timeOutMs, final TimeUnit timeUnit )
        {
          return new HttpResponse<>() {
            @Override
            public int statusCode() {
              return 200;
            }

            @Override
            public HttpRequest request() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Optional<HttpResponse<byte[]>> previousResponse() {
              // TODO Auto-generated method stub
              return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
              final Map< String, List< String > > headers = new HashMap<>();
              headers.put( HttpHandler.HEADER_NAME__ALLOW, Arrays.asList( "PUT, GET" ) );
              headers.put( HttpHandler.HEADER_NAME__CONTENT_LENGTH, Arrays.asList( "7" ) );
              headers.put( HttpHandler.HEADER_NAME__CONTENT_TYPE, Arrays.asList( "application/json" ));

              final BiPredicate<String,String> filter = new BiPredicate<>() {
                @Override
                public boolean test( final String t, final String u) {
                  return true;
                }
              };
              return HttpHeaders.of( headers, filter );
            }

            @Override
            public byte[] body() {
              return "{\"a\":1}".getBytes();
            }

            @Override
            public Optional<SSLSession> sslSession() {
              // TODO Auto-generated method stub
              return Optional.empty();
            }

            @Override
            public URI uri() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Version version() {
              // TODO Auto-generated method stub
              return null;
            }
          };
        }
      };
      XmlHttpResponse xmlHttpResponse = HttpHandler.createXmlHttpResponse(
        httpResponseFuture,
        xmlResponse,
        timeoutMs,
        filterRegistry,
        serviceId,
        testId,
        testFileName,
        xmlRequest,
        storeReportPath,
        testSetPath,
        testSetWorkPath
      );
      assertThat( xmlHttpResponse ).isNotNull();
      assertThat( xmlHttpResponse.getHttpStatus().getValue() ).isEqualTo( 200 );
      assertThat( xmlHttpResponse.getHeaders().getHeader().size() ).isEqualTo( 3 );
      assertThat( xmlHttpResponse.getBody() ).isEqualTo( "{\"a\":1}" );
      assertThat( xmlHttpResponse.isBodyIsJson() ).isEqualTo( true );

      // When / Then (future != null; hideBody)
      xmlResponse.setHideBody( true );
      xmlHttpResponse = HttpHandler.createXmlHttpResponse(
        httpResponseFuture,
        xmlResponse,
        timeoutMs,
        filterRegistry,
        serviceId,
        testId,
        testFileName,
        xmlRequest,
        storeReportPath,
        testSetPath,
        testSetWorkPath
      );
      assertThat( xmlHttpResponse.getBody() ).isEqualTo( "ResponseDiff: Body was hidden on demand (hideBody was set to true)." );

      // When / Then (future != null; as download)
      httpResponseFuture = new CompletableFuture< HttpResponse< byte[] > >() {
        public HttpResponse< byte[] > get( final long timeOutMs, final TimeUnit timeUnit )
        {
          return new HttpResponse<>() {
            @Override
            public int statusCode() {
              return 200;
            }

            @Override
            public HttpRequest request() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Optional<HttpResponse<byte[]>> previousResponse() {
              // TODO Auto-generated method stub
              return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
              final Map< String, List< String > > headers = new HashMap<>();
              headers.put( HttpHandler.HEADER_NAME__ALLOW, Arrays.asList( "PUT, GET" ) );
              headers.put( HttpHandler.HEADER_NAME__CONTENT_DISPOSITION, Arrays.asList( "filename=myFile.pdf" ) );
              headers.put( HttpHandler.HEADER_NAME__CONTENT_LENGTH, Arrays.asList( "10" ) );
              headers.put( HttpHandler.HEADER_NAME__CONTENT_TYPE, Arrays.asList( "application/pdf" ));

              final BiPredicate<String,String> filter = new BiPredicate<>() {
                @Override
                public boolean test( final String t, final String u) {
                  return true;
                }
              };
              return HttpHeaders.of( headers, filter );
            }

            @Override
            public byte[] body() {
              return "{\"a\":1}".getBytes();
            }

            @Override
            public Optional<SSLSession> sslSession() {
              // TODO Auto-generated method stub
              return Optional.empty();
            }

            @Override
            public URI uri() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Version version() {
              // TODO Auto-generated method stub
              return null;
            }
          };
        }
      };
      xmlResponse.setHideBody( false );
      xmlHttpResponse = HttpHandler.createXmlHttpResponse(
        httpResponseFuture,
        xmlResponse,
        timeoutMs,
        filterRegistry,
        serviceId,
        testId,
        testFileName,
        xmlRequest,
        storeReportPath,
        testSetPath,
        testSetWorkPath
      );
      assertThat( xmlHttpResponse ).isNotNull();
      assertThat( xmlHttpResponse.getHttpStatus().getValue() ).isEqualTo( 200 );
      assertThat( xmlHttpResponse.getHeaders().getHeader().size() ).isEqualTo( 4 );
      assertThat( xmlHttpResponse.getBody() ).isNull();
      assertThat( xmlHttpResponse.isBodyIsJson() ).isEqualTo( false );
      assertThat( xmlHttpResponse.getDownload() ).isNotNull();
      assertThat( xmlHttpResponse.getDownload().getFilename() ).isEqualTo( "./test-folder/regression/./downloads\\TEST_A_candidate_myFile.pdf" );
      assertThat( xmlHttpResponse.getDownload().getSize() ).isEqualTo( 7 );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatCreateXmlHttpResponseFromXmlRequestAndXmlResponseWorks()
  {
    final XmlRequest           xmlRequest = new XmlRequest();
    final XmlResponse          xmlResponse = new XmlResponse();
    final String               serviceId = TestSetHandler.CANDIDATE;
    final String               testId = "Test-A";
    final String               testFileName = "test-A.xml";
    final XmlResponseDiffSetup referenceXmlSetup = new XmlResponseDiffSetup();

    // When / Then ( no ResponseDiffXmlSetup)
    XmlHttpResponse xmlHttpResponse = HttpHandler.createXmlHttpResponse(
      xmlRequest,
      xmlResponse,
      serviceId,
      testId,
      testFileName,
      null // referenceXmlSetup
    );
    assertThat( xmlHttpResponse ).isNull();

    // When / Then (No matching test id)
    Throwable ex = catchThrowable( () -> HttpHandler.createXmlHttpResponse(
      xmlRequest,
      xmlResponse,
      serviceId,
      testId,
      testFileName,
      referenceXmlSetup
    ) );
    assertThat( ex )
      .isInstanceOf( RuntimeException.class )
      .hasMessageContaining( "No matching test \"" + testId + "\" found in reference XML report." );

    // When / Then (No matching test id)
    final XmlTestSet xmlTestSet = new XmlTestSet();
    final XmlTest xmlTest1 = new XmlTest();
    xmlTest1.setId( "SOME ID" );
    xmlTestSet.getTest().add( xmlTest1 );
    referenceXmlSetup.getTestSet().add( xmlTestSet );
    ex = catchThrowable( () -> HttpHandler.createXmlHttpResponse(
      xmlRequest,
      xmlResponse,
      serviceId,
      testId,
      testFileName,
      referenceXmlSetup
    ) );
    assertThat( ex )
      .isInstanceOf( RuntimeException.class )
      .hasMessageContaining( "No matching test \"" + testId + "\" found in reference XML report." );

    // When / Then (No XmlRespone)
    final XmlTest xmlTest2 = new XmlTest();
    xmlTest2.setId( testId );
    xmlTestSet.getTest().add( xmlTest2 );
    xmlHttpResponse = HttpHandler.createXmlHttpResponse(
      xmlRequest,
      xmlResponse,
      serviceId,
      testId,
      testFileName,
      referenceXmlSetup
    );
    assertThat( xmlHttpResponse ).isNull();

    // When / Then (No XmlHttpRespone)
    XmlResponse xmlResponseOfTest2 = new XmlResponse();
    xmlTest2.setResponse( xmlResponseOfTest2 );
    xmlHttpResponse = HttpHandler.createXmlHttpResponse(
      xmlRequest,
      xmlResponse,
      serviceId,
      testId,
      testFileName,
      referenceXmlSetup
    );
    assertThat( xmlHttpResponse ).isNull();

    // When / Then (No XmlHttpRespone)
    final String expectedBody = "ExpectedBody";
    final XmlHttpResponse xmlHttResponseOfTest2 = new XmlHttpResponse();
    xmlHttResponseOfTest2.setBody( expectedBody );
    xmlResponseOfTest2.setHttpResponse( xmlHttResponseOfTest2 );
    xmlHttpResponse = HttpHandler.createXmlHttpResponse(
      xmlRequest,
      xmlResponse,
      serviceId,
      testId,
      testFileName,
      referenceXmlSetup
    );
    assertThat( xmlHttpResponse ).isNotNull();
    assertThat( xmlHttpResponse.getBody() ).isEqualTo( expectedBody );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatCreateXmlDownloadWorks()
  {
    final byte[] bytes = "Test".getBytes();
    final String contentType = "application/pdf";
    final String serviceId = TestSetHandler.CANDIDATE;
    final String testId = "TestAll" + TestSetHandler.ID_SEPARATOR + "Test-A";
    final String contentDisposition = "filename=myFile.pdf";
    final String storeReportPath = "./test-results/reports/";
    final String testSetPath = "./test-folder/regression/";
    final String testSetWorkPath = "./downloads";

    try {
      // When /( Then
      XmlDownload xmlDownload = HttpHandler.createXmlDownload(
        bytes,
        contentType,
        serviceId,
        testId,
        contentDisposition,
        null, // storeReportPath,
        testSetPath,
        testSetWorkPath
      );
      assertThat( xmlDownload ).isNull();

      // When /( Then
      xmlDownload = HttpHandler.createXmlDownload(
        bytes,
        contentType,
        serviceId,
        testId,
        null, // contentDisposition,
        storeReportPath,
        testSetPath,
        testSetWorkPath
      );
      assertThat( xmlDownload ).isNotNull();
      assertThat( xmlDownload.getFilename() ).isEqualTo( "./test-folder/regression/./downloads\\Test-A_candidate_download.pdf" );
      assertThat( xmlDownload.getSize() ).isEqualTo( 4 );

    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatPrepareHttpRequestWorks()
  {
    final String      serviceUrl = "http://testserver/service/endpoint";
    final XmlRequest xmlRequest = new XmlRequest();
    final String     serviceId = TestSetHandler.CANDIDATE;
    final String     testId = "Test-A";
    final String     testFileName = "test-A.xml";

    xmlRequest.setMethod( XmlHttpRequestMethod.GET );
    final XmlFile xmlFile1 = new XmlFile();
    xmlFile1.setName( "uploadFile1" );
    xmlFile1.setValue( "./README.md" );
    xmlFile1.setContentType( "text/markdown" );
    final XmlUploadParts xmlUploadParts = new XmlUploadParts();
    xmlUploadParts.getFile().add( xmlFile1 );
    xmlRequest.setUploadParts( xmlUploadParts );

    try {
      // When / Then (xmlRequest is null)
      Builder builder = HttpHandler.prepareHttpRequest( null, serviceId, testId, testFileName );
      assertThat( builder ).isNull();

      // When / Then (with body)
      xmlRequest.setBody( "{\"a\":1}" );
      builder = HttpHandler.prepareHttpRequest( xmlRequest, serviceId, testId, testFileName );
      assertThat( builder ).isNotNull();
      builder.uri( URI.create( serviceUrl ) ); // NOTE: This is done outside of prepareHttpRequest() so we have to do it here
      HttpRequest httpRequest = builder.build();
      assertThat( httpRequest.method() ).isEqualTo( "GET" );

      // When / Then (without body, without headers)
      xmlRequest.setBody( null );
      builder = HttpHandler.prepareHttpRequest( xmlRequest, serviceId, testId, testFileName );
      assertThat( builder ).isNotNull();
      builder.uri( URI.create( serviceUrl ) ); // NOTE: This is done outside of prepareHttpRequest() so we have to do it here
      httpRequest = builder.build();
      assertThat( httpRequest.method() ).isEqualTo( "GET" );

      // When / Then (with empty headers)
      final XmlHeaders xmlHeaders = new XmlHeaders();
      xmlRequest.setHeaders( xmlHeaders );
      builder = HttpHandler.prepareHttpRequest( xmlRequest, serviceId, testId, testFileName );
      assertThat( builder ).isNotNull();
      builder.uri( URI.create( serviceUrl ) ); // NOTE: This is done outside of prepareHttpRequest() so we have to do it here
      httpRequest = builder.build();
      assertThat( httpRequest.method() ).isEqualTo( "GET" );

      // When / Then (with headers)
      for( final String method : Arrays.asList( "GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH" ) ) {
        xmlRequest.setMethod( XmlHttpRequestMethod.valueOf( method ) );
        final XmlHeader xmlHeader = new XmlHeader();
        xmlHeader.setName( HttpHandler.HEADER_NAME__CONTENT_TYPE );
        xmlHeader.setValue( "text/markdown" );
        try {
          builder = HttpHandler.prepareHttpRequest( xmlRequest, serviceId, testId, testFileName );
          assertThat( builder ).isNotNull();
          builder.uri( URI.create( serviceUrl ) ); // NOTE: This is done outside of prepareHttpRequest() so we have to do it here
          httpRequest = builder.build();
          assertThat( httpRequest.method() ).isEqualTo( method );
        }
        catch( final IllegalArgumentException ex )
        {
          assertThat( ex.getMessage() ).isEqualTo( "method CONNECT is not supported" ); // HttpRequestBuilderImpl currently does not support CONNECT
        }
      }
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatHandleUploadPartsWorks()
  {
    final XmlRequest xmlRequest = new XmlRequest();
    final String     serviceId = TestSetHandler.CANDIDATE;
    final String     testId = "Test-A";
    final String     testFileName = "test-A.xml";

    // When / Then (no uploadparts)
    HttpHandler.handleUploadParts( xmlRequest, serviceId, testId, testFileName );
    assertThat( xmlRequest.getUploadParts() ).isNull();

    // When / Then (empty uploadparts)
    XmlUploadParts xmlUploadParts = new XmlUploadParts();
    xmlRequest.setUploadParts( xmlUploadParts );
    HttpHandler.handleUploadParts( xmlRequest, serviceId, testId, testFileName );
    assertThat( xmlRequest.getUploadParts().getFile() ).isEmpty();

    // When / Then (with uploadparts, without variables)
    final XmlFile xmlFile1 = new XmlFile();
    xmlFile1.setName( "${VAR1}" );
    xmlFile1.setValue( "./README.md" );
    xmlFile1.setContentType( "text${VAR2}markdown" );
    xmlFile1.setCharSet( "${VAR3}" );
    xmlUploadParts.getFile().add( xmlFile1 );
    HttpHandler.handleUploadParts( xmlRequest, serviceId, testId, testFileName );
    assertThat( xmlRequest.getUploadParts().getFile().size() ).isEqualTo( 1 );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getName       () ).isEqualTo( "${VAR1}" );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getValue      () ).isEqualTo( "./README.md" );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getContentType() ).isEqualTo( "text${VAR2}markdown" );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getCharSet    () ).isEqualTo( "${VAR3}" );

    // When / Then (with uploadparts, with variables)
    final XmlVariables xmlVariables = new XmlVariables();
    final XmlVariable xmlVariable1 = new XmlVariable(); xmlVariable1.setId( "VAR1" ); xmlVariable1.setValue( "upload${VAR2}file1" );
    final XmlVariable xmlVariable2 = new XmlVariable(); xmlVariable2.setId( "VAR2" ); xmlVariable2.setValue( "/" );
    final XmlVariable xmlVariable3 = new XmlVariable(); xmlVariable3.setId( "VAR3" ); xmlVariable3.setValue( "UTF-8" );
    xmlVariables.getVariable().add( xmlVariable1 );
    xmlVariables.getVariable().add( xmlVariable2 );
    xmlVariables.getVariable().add( xmlVariable3 );
    xmlRequest.setVariables( xmlVariables );
    HttpHandler.handleUploadParts( xmlRequest, serviceId, testId, testFileName );
    assertThat( xmlRequest.getUploadParts().getFile().size() ).isEqualTo( 1 );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getName       () ).isEqualTo( "upload/file1" );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getValue      () ).isEqualTo( "./README.md" );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getContentType() ).isEqualTo( "text/markdown" );
    assertThat( xmlRequest.getUploadParts().getFile().get( 0 ).getCharSet    () ).isEqualTo( "UTF-8" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatApplyRequestFiltersWorks()
  {
    final XmlRequest xmlRequest = new XmlRequest();
    final String     serviceId = TestSetHandler.CANDIDATE;
    final XmlTest    xmlTest   = new XmlTest();
    final String     testFileName = "test-A.xml";
    final Map< String, DiffFilter > filterRegistry = new HashMap<>();
    final boolean    nextDiffTest = true;

    final DummyRequestFilter dummyFilter = new DummyRequestFilter();
    final RemoveHeaderRequestFilter removeHeaderFilter = new RemoveHeaderRequestFilter();
    removeHeaderFilter.setFilterParameter( RemoveHeaderRequestFilter.PARAMETER_NAME__NAMES, "Accept-Language,Accept" );
    filterRegistry.put( "dummy", dummyFilter );
    filterRegistry.put( "removeHeaders", removeHeaderFilter );

    // Set xmlRequest headers
    final XmlHeader xmlHeader1 = new XmlHeader();
    xmlHeader1.setName( "Accept-Language" );
    xmlHeader1.setValue( "de,de-DE;q=0.8");
    final XmlHeader xmlHeader2 = new XmlHeader();
    xmlHeader2.setName( "Accept-Encoding" );
    xmlHeader2.setValue( "gzip");
    final XmlHeader xmlHeader3 = new XmlHeader();
    xmlHeader3.setName( "Accept" );
    xmlHeader3.setValue( "application/json");
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( xmlHeader1 );
    xmlHeaders.getHeader().add( xmlHeader2 );
    xmlHeaders.getHeader().add( xmlHeader3 );
    xmlRequest.setHeaders( xmlHeaders );

    try {
      removeHeaderFilter.init();

      // When / Then (no XmlRequest)
      HttpHandler.applyRequestFilters( null, serviceId, xmlTest, testFileName, filterRegistry, nextDiffTest );
      // Nothing happens!

      // When / Then (without filters)
      HttpHandler.applyRequestFilters( xmlRequest, serviceId, xmlTest, testFileName, filterRegistry, nextDiffTest );
      assertThat( xmlRequest.getHeaders().getHeader().size() ).isEqualTo( 3 );

      // When / Then (with empty filters)
      final XmlFilters xmlFilters = new XmlFilters();
      xmlRequest.setFilters( xmlFilters );
      HttpHandler.applyRequestFilters( xmlRequest, serviceId, xmlTest, testFileName, filterRegistry, nextDiffTest );
      assertThat( xmlRequest.getHeaders().getHeader().size() ).isEqualTo( 3 );

      // When / Then (with dummy filter)
      final XmlFilter xmlFilter1 = new XmlFilter();
      xmlFilter1.setId( "dummy" );
      xmlFilters.getFilter().add( xmlFilter1 );
      HttpHandler.applyRequestFilters( xmlRequest, serviceId, xmlTest, testFileName, filterRegistry, nextDiffTest );
      assertThat( xmlRequest.getHeaders().getHeader().size() ).isEqualTo( 3 );

      // When / Then (with filters)
      final XmlFilter xmlFilter2 = new XmlFilter();
      xmlFilter2.setId( "removeHeaders" );
      xmlFilters.getFilter().add( xmlFilter2 );
      HttpHandler.applyRequestFilters( xmlRequest, serviceId, xmlTest, testFileName, filterRegistry, nextDiffTest );
      assertThat( xmlRequest.getHeaders().getHeader().size() ).isEqualTo( 1 );
      assertThat( xmlRequest.getHeaders().getHeader().get( 0 ).getName() ).isEqualTo( "Accept-Encoding" );
      assertThat( xmlRequest.getHeaders().getHeader().get( 0 ).getValue() ).isEqualTo( "gzip" );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /* TODOs
  @Test
  public void testThatSendRequestWorks() // Mock?
  {
    // TODO: Implement this!
  }
  */
}
