package com.github.kreutzr.responsediff;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffRequestFilter;
import com.github.kreutzr.responsediff.filter.DiffResponseFilter;
import com.github.kreutzr.responsediff.tools.Converter;

/**
 * Class to handle HTTP related operations.
 */
public class HttpHandler
{
  public static final String HEADER_NAME__ALLOW                       = "allow";
  public static final String HEADER_NAME__CONTENT_DISPOSITION         = "content-disposition";
  public static final String HEADER_NAME__CONTENT_LENGTH              = "content-length";
  public static final String HEADER_NAME__CONTENT_TYPE                = "content-type";
  public static final String HEADER_NAME__CONTENT_ENCODING            = "content-encoding";

  public static final String HEADER_VALUE__CONTENT_TYPE__JSON         = "application/json";
  public static final String HEADER_VALUE__CONTENT_TYPE__JSON_PATTERN = "application/.*json.*"; // application/json, .../problem+json, .../vnd.api+json, .../vnd.uber+json, .../vnd.siren+json, .../vnd.hal+json NOTE: May be followed by e.g. "; charset=utf-8"
  public static final String HEADER_VALUE__CONTENT_TYPE__JSON_API     = "application/vnd.api+json";

  private static final Logger LOG = LoggerFactory.getLogger( HttpHandler.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Charset URL_ENCODING = StandardCharsets.UTF_8;
  private static final String  CRLF         = "\r\n";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Map< String, HttpClient > httpClientByServiceId_ = new TreeMap<>();

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Prepares a HttpRequest
   * @param xmlRequest   The XmlRequest object to read from. May be null.
   * @param serviceId    A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId       The current test id. Must not be null.
   * @param testFileName The current test file name. Must not be null.
   * @return A Builder object to create a HttpRequest. If xmlRequest is null, null is returned.
   * @throws DiffFilterException
   * @throws IOException
   */
  static Builder prepareHttpRequest(
    final XmlRequest xmlRequest,
    final String     serviceId,
    final String     testId,
    final String     testFileName
  )
  throws DiffFilterException, IOException
  {
    if( xmlRequest == null ) {
      return null;
    }

    // Prepare body
    BodyPublisher bodyPublisher = null;
    if( xmlRequest.getBody() != null ) {
      bodyPublisher = HttpRequest.BodyPublishers.ofString( xmlRequest.getBody() );

      if( xmlRequest.getUploadParts() != null ) {
        LOG.warn( "Upload parts of " + serviceId + " test \"" + testId + "\" are ignored because the request has a body." );
      }
    }
    else {
      if( xmlRequest.getUploadParts() != null && !xmlRequest.getUploadParts().getFile().isEmpty() ) {
        final ByteArrayInputStream is = getUploadPartsAsStream( xmlRequest, testFileName );
        bodyPublisher = HttpRequest.BodyPublishers.ofInputStream( () -> is );
      }
      else {
        bodyPublisher = HttpRequest.BodyPublishers.noBody();
      }
    }

    // Prepare request builder and method
    Builder requestBuilder = null;
    switch( xmlRequest.getMethod() ) {
      case GET     : requestBuilder = HttpRequest.newBuilder().GET();
        break;
      case HEAD    : requestBuilder = HttpRequest.newBuilder().method( XmlHttpRequestMethod.HEAD   .name(), bodyPublisher );
        break;
      case POST    : requestBuilder = HttpRequest.newBuilder().POST( bodyPublisher );
        break;
      case PUT     : requestBuilder = HttpRequest.newBuilder().PUT ( bodyPublisher );
        break;
      case DELETE  : requestBuilder = HttpRequest.newBuilder().method( XmlHttpRequestMethod.DELETE .name(), bodyPublisher );
        break;
      case CONNECT : requestBuilder = HttpRequest.newBuilder().method( XmlHttpRequestMethod.CONNECT.name(), bodyPublisher );
        break;
      case OPTIONS : requestBuilder = HttpRequest.newBuilder().method( XmlHttpRequestMethod.OPTIONS.name(), bodyPublisher );
         break;
      case TRACE   : requestBuilder = HttpRequest.newBuilder().method( XmlHttpRequestMethod.TRACE  .name(), bodyPublisher );
        break;
      case PATCH   : requestBuilder = HttpRequest.newBuilder().method( XmlHttpRequestMethod.PATCH  .name(), bodyPublisher );
        break;
      default :
        throw new RuntimeException( "ERROR: Request method \"" + xmlRequest.getMethod().name() + "\" is not yet supported." );
    }

    // Add headers (if any)
    if( xmlRequest.getHeaders() != null && xmlRequest.getHeaders().getHeader() != null ) {
      for( final XmlHeader xmlHeader : xmlRequest.getHeaders().getHeader() ) {
        requestBuilder.header( xmlHeader.getName(), xmlHeader.getValue() );
      }
    }

    return requestBuilder;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static String getUploadFilepath( final XmlFile xmlFile, final String testFileName )
  {
    // Handle relative paths
    String uploadFilePath = xmlFile.getValue().trim();
    if( uploadFilePath.startsWith( "." ) ) {
      final int pos = Math.max( testFileName.lastIndexOf( "\\" ), testFileName.lastIndexOf( "/" ) );
      uploadFilePath = testFileName.substring( 0, pos+1 ) + uploadFilePath;
    }
    return uploadFilePath;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a ByteArrayInputStream that contains all uploadParts. <b>NOTE:</b> The "Content-Type" header will be adjusted as a side effect.
   * @param xmlRequest The XmlRequest to use. Must not be null.
   * @param testFileName The complete file name of the test. Must not null.
   * @return A ByteArrayInputStream. May be empty but never null.
   * @throws IOException
   */
  static ByteArrayInputStream getUploadPartsAsStream(
    final XmlRequest xmlRequest,
    final String testFileName
  )
  throws IOException
  {
    final String boundary = UUID.randomUUID().toString(); // Boundary must not be longer than 66 characters since "--" is set to front and (additionally) at the end (end of transmission) and total size must not be longer than 70 characters,
    final List< byte[] > byteArrayList = new ArrayList<>();

    // Read all files to upload and create multipart content
    final List< XmlFile > xmlFiles = xmlRequest.getUploadParts().getFile();
    for( int i=0; i < xmlFiles.size(); i++ ) {
      final XmlFile xmlFile = xmlFiles.get( i );

      final String fileName = ( xmlFile.getName() != null && !xmlFile.getName().trim().isEmpty() ) // This is an optional attribute
        ? xmlFile.getName().trim()
        : ( "" + XmlFileHandler.getFileName( xmlFile.getValue() ) ).trim(); // Avoid NullPointerException

      // Handle relative paths
      final String uploadFilePath = getUploadFilepath(xmlFile, testFileName );

      final StringBuilder sb = new StringBuilder()
        .append( CRLF )
        .append( "--" ).append( boundary )
        .append( CRLF )
        .append( "Content-Disposition: form-data; name=\"" + fileName + "\"; filename=\"" + uploadFilePath + "\"" )
        .append( CRLF )
        .append( "Content-Type: " + xmlFile.getContentType().trim() )                              // This is a  required attribute
        .append( xmlFile.getCharSet() != null ? "; charset=" + xmlFile.getCharSet().trim() : "" )  // This is an optional attribute
        .append( CRLF )
        .append( CRLF );
      byteArrayList.add( sb.toString().getBytes() );

      final byte[] fileBytes = Files.readAllBytes( Path.of( uploadFilePath ) );
      byteArrayList.add( fileBytes );
    } // for

    if( !xmlFiles.isEmpty() ) {
      final StringBuilder sb = new StringBuilder()
        .append( CRLF )
        .append( "--" ).append( boundary ).append( "--" )
        .append( CRLF );
      byteArrayList.add( sb.toString().getBytes() );
    }

    // Gather all bytes
    int size = 0;
    for( final byte[] byteArray : byteArrayList ) {
      size += byteArray.length;
    }
    final byte[] bytes = new byte[ size ];
    int targetPos = 0;
    for( final byte[] byteArray : byteArrayList ) {
      System.arraycopy( byteArray, 0, bytes, targetPos, byteArray.length );
      targetPos += byteArray.length;
    }

    // Set content-type header to "multipart"
    if( size > 0 && xmlRequest.getHeaders() != null ) {
      XmlHeader foundHeader = getHeader( HEADER_NAME__CONTENT_TYPE, xmlRequest.getHeaders().getHeader() );

      if( foundHeader == null ) {
        foundHeader = new XmlHeader();
        foundHeader.setName( HEADER_NAME__CONTENT_TYPE );
        xmlRequest.getHeaders().getHeader().add( foundHeader );
      }
      foundHeader.setValue( "multipart/form-data; boundary=" + boundary );
    }

    return new ByteArrayInputStream( bytes );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Searches a header of a given name within the given XmlHeader list.
   * @param headerName The header name. Must not be null.
   * @param xmlHeaders The list of headers to use. Must not be null.
   * @return The requested XmlHeader. If no matching header could be found, null is returned.
   */
  public static XmlHeader getHeader( final String headerName, final List< XmlHeader > xmlHeaders )
  {
    for( final XmlHeader xmlHeader : xmlHeaders ) {
      if( xmlHeader.getName().trim().equalsIgnoreCase( headerName ) ) {
        return xmlHeader;
      }
    }
    return null;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a service URL with all variables applied and sets it to the passed XmlRequest's endpoint.
   * @param xmlRequest The XmlRequest. Must not be null.
   * @param serviceId  A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param serviceUrl The base service URL to use. Must not be null. Must not end with "/".
   */
  static void createServiceUrl(
      final XmlRequest xmlRequest,
      final String serviceId,
      final String testId,
      final String testFileName,
      final String serviceUrl
  ) {
    // NOTE: A variable (e.g. from location header) may have a leading "/".
    String endPoint = VariablesHandler.applyVariables( xmlRequest.getEndpoint(), xmlRequest.getVariables(), "serviceUrl", serviceId, testId, testFileName );
    if( !endPoint.startsWith( "/" ) ) {
      endPoint = "/" + endPoint;
    }

    final StringBuilder sb = new StringBuilder()
      .append( serviceUrl )
      .append( endPoint );

    // Add parameters to serviceUrl (if any)
    if( xmlRequest.getParameters() != null ) {
      final boolean urlHasParameters = ( endPoint.lastIndexOf( "?" ) >= 0 );
      for( int i=0; i < xmlRequest.getParameters().getParameter().size(); i++ ) {
        final XmlParameter xmlParameter = xmlRequest.getParameters().getParameter().get( i );

        sb.append( ( i == 0 && !urlHasParameters ) ? "?" : "&" )
          .append( xmlParameter.getId() ).append( "=" )
          .append( URLEncoder.encode( xmlParameter.getValue(), URL_ENCODING ) );
      }
    }

    // Apply variable to entire URL (serviceUrl + endpoint + parameters)
    xmlRequest.setEndpoint( VariablesHandler.applyVariables( sb.toString(), xmlRequest.getVariables(), "serviceUrl", serviceId, testId, testFileName ) );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Adds a CURL entry to the given XmlRequest.
   * @param xmlRequest   The XmlRequest to use. May be null.
   * @param serviceId    A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testFileName The current test file name. Must not be null.
   * @param outerContext The outer context. Must not be null.
   */
  static void addCurl(
    final XmlRequest xmlRequest,
    final String serviceId,
    final String testFileName,
    final OuterContext outerContext
  )
  {
    if( xmlRequest != null ) {
      final String curl = toCurl( xmlRequest, testFileName, outerContext );
      if( LOG.isTraceEnabled() ) {
        LOG.trace( "Sending " + serviceId + " request. " + curl );
      }
      xmlRequest.setCurl( curl );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Send the given HttpRequest.
   * @param xmlRequest The XmlRequest to store the final URL to. May be null.
   * @param xmlHeaders A list of XmlHeader objects. May be null.
   * @param builder The Builder to build the HttpRequest with. May be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @return A HttpResonse future. If builder or xmlRequest is null, null is returned.
   * @throws Exception
   */
  static CompletableFuture< HttpResponse< byte[] > > sendRequest(
    final XmlRequest        xmlRequest,
    final List< XmlHeader > xmlHeaders,
    final Builder           builder,
    final String            serviceId,
    final String            testId,
    final String            testFileName
  )
  throws Exception
  {
    return sendRequest(
      xmlRequest,
      xmlHeaders,
      builder,
      xmlRequest != null ? xmlRequest.getEndpoint() : null,
      serviceId,
      testId,
      testFileName
    );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Send the given HttpRequest.
   * @param xmlRequest The XmlRequest to store the final URL to. May be null.
   * @param xmlHeaders A list of XmlHeader objects. May be null.
   * @param builder The Builder to build the HttpRequest with. May be null.
   * @param serviceUrl The request serviceUrl. May be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @return A HttpResonse future. If builder or serviceUrl is null, null is returned.
   * @throws Exception
   */
  public static CompletableFuture< HttpResponse< byte[]> > sendRequest(
    final XmlRequest        xmlRequest,
    final List< XmlHeader > xmlHeaders,
    final Builder           builder,
    final String            serviceUrl,
    final String            serviceId,
    final String            testId,
    final String            testFileName
  )
  throws Exception
  {
    if( builder == null || serviceUrl == null ) {
      return null;
    }

    // Set or overwrite server specific headers (e.g. for authentication or authorization)
    if( xmlHeaders != null ) {
      for( final XmlHeader xmlHeader : xmlHeaders ) {
        builder.setHeader( xmlHeader.getName(), xmlHeader.getValue() );
      }
    }

    // Add URL to request builder
    builder.uri( URI.create( serviceUrl ) );

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Sending " + serviceId + " request for test \"" + testId + "\": " + serviceUrl );
    }

    final HttpRequest httpRequest = builder.build();

    // Send request
    HttpClient client = httpClientByServiceId_.get( serviceId );
    if( client == null ) {
      client = HttpClient.newHttpClient();
      httpClientByServiceId_.put( ( serviceId != null ? serviceId : "UNKNOWN" ), client );
    }
    final CompletableFuture< HttpResponse< byte[] > > response = client.sendAsync(
       httpRequest,
       HttpResponse.BodyHandlers.ofByteArray()
    );

    return response;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a CURL String for the given XmlRequest.
   * @param xmlRequest   The XmlRequest to use. Must not be null.
   * @param testFileName The current test file name. Must not be null.
   * @param outerContext The outer context. Must not be null.
   */
  public static String toCurl(
    final XmlRequest xmlRequest,
    final String testFileName,
    final OuterContext outerContext
  )
  {
    final StringBuilder sb = new StringBuilder( "curl" );
    switch( xmlRequest.getMethod() ) {
      case GET     : sb.append(" -X GET" );
        break;
      case HEAD    : sb.append(" -X HEAD" );
        break;
      case POST    : sb.append(" -X POST" );
        break;
      case PUT     : sb.append(" -X PUT" );
        break;
      case DELETE  : sb.append(" -X DELETE" );
        break;
      case CONNECT : sb.append(" -X CONNECT" );
        break;
      case OPTIONS : sb.append(" -X OPTIONS" );
        break;
      case TRACE   : sb.append(" -X TRACE" );
        break;
      case PATCH   : sb.append(" -X PATCH" );
        break;
      default :
        throw new RuntimeException( "ERROR: Request method \"" + xmlRequest.getMethod().name() + "\" is not yet supported." );
    }

    // NOTE: Here the parameters are already included to the endpoint
    sb.append( " " ).append( xmlRequest.getEndpoint() );

    // Handle headers
    if( xmlRequest.getHeaders() != null ) {
      for( final XmlHeader xmlHeader : xmlRequest.getHeaders().getHeader() ) {
        final String xmlHeaderValue = ( xmlHeader.getName().equalsIgnoreCase( "Authorization" ) && outerContext.getMaskAuthorizationHeaderInCurl() )
          ? "..."
          : xmlHeader.getValue();
        sb.append( " -H \"" ).append( xmlHeader.getName() ).append( ": " ).append( xmlHeaderValue ).append( "\"" );
      }
    }

    // Handle body
    if( xmlRequest.getBody() != null ) {
      sb.append( " -d '" ).append( xmlRequest.getBody() ).append( "'" );
    }

    // Handle upload parts
    if( xmlRequest.getUploadParts() != null ) {
      for( final XmlFile xmlFile : xmlRequest.getUploadParts().getFile() ) {
        final String fileName = ( "" + XmlFileHandler.getFileName( xmlFile.getName() ) ).trim();

        final String uploadFilePath = getUploadFilepath( xmlFile, testFileName );

        sb.append( " -F '" ).append( fileName ).append( "=@" ).append( uploadFilePath )
          .append( ";type=" ).append( xmlFile.getContentType() ) // This is a required attribute
          .append( "'" );
      }
    }

    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a XmlHttpResponse from the given response future.
   * @param httpResponseFuture The Future to use. May be null.
   * @param xmlResponse The XmlResponse. Must not be null.
   * @param timeoutMs The timeout in milliseconds.
   * @param filterRegistry The registry that holds all registered filters. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param xmlRequest The XmlRequest. May be null.
   * @param storeReportPath The path where the report is stored to. May be null.
   * @param testSetPath The path of the current TestSet. Must not be null.
   * @param testSetWorkPath An optional folder (relative to the testSetPath) that should be used (e.g., for downloads)
   * @return The initialized XmlHttpResponse object. If httpResponseFuture is null, null is returned.
   * @throws TimeoutException
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws DiffFilterException
   * @throws IOException
   */
  static XmlHttpResponse createXmlHttpResponse(
    final CompletableFuture< HttpResponse< byte[] > > httpResponseFuture,
    final XmlResponse xmlResponse,
    final long        timeoutMs,
    final Map< String, DiffFilter > filterRegistry,
    final String      serviceId,
    final String      testId,
    final String      testFileName,
    final XmlRequest  xmlRequest,
    final String      storeReportPath,
    final String      testSetPath,
    final String      testSetWorkPath
  )
  throws DiffFilterException, HttpHandlerException, IOException
  {
    if( httpResponseFuture == null ) {
      return null;
    }

    if( LOG.isTraceEnabled() && xmlRequest != null ) {
      LOG.trace( "createXmlHttpResponse( testId=" + testId + ", serviceId="+ serviceId + ", timeout=" + timeoutMs + ", endpoint=" + xmlRequest.getEndpoint() + ")" );
    }

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    xmlHttpResponse.setHeaders( new XmlHeaders() );

    HttpResponse< byte[] > httpResponse = null;
    try {
      httpResponse = httpResponseFuture.get( timeoutMs, TimeUnit.MILLISECONDS );
//    ###  final HttpResponse< String > httpResponse = httpResponseFuture.join();
    }
    catch( final InterruptedException | CancellationException | ExecutionException | TimeoutException ex ) {
      throw new HttpHandlerException( ex.getMessage(), serviceId, ex );
    }

    // NOTE: Calculate request duration as soon as possible
    String requestDuration = null;
    if( xmlResponse.getRequestTime() != null ) {
      final LocalDateTime now = LocalDateTime.now();
      final LocalDateTime start = LocalDateTime.parse( xmlResponse.getRequestTime() );
      final Duration duration = Duration.between( start, now );
      requestDuration = duration.toString();
    }

    // Copy all relevant response data
    final XmlHttpStatus xmlHttpStatus = new XmlHttpStatus();
    xmlHttpStatus.setValue( httpResponse.statusCode() );
    xmlHttpResponse.setHttpStatus( xmlHttpStatus );
    xmlHttpResponse.setRequestDuration( requestDuration );
    String contentDisposition = null;
    int    contentLength      = 0;
    String contentType        = null;
    final Map< String, List< String > > headers = new TreeMap<>( httpResponse.headers().map() );
    boolean hasContentLengthHeader = false;
    for( final String key : headers.keySet() ) {
      // Having the allowed HTTP methods ("PUT", "GET", ...) sorted, allows comparison in tests.
      if( key.equalsIgnoreCase( HEADER_NAME__ALLOW ) ) {
        final List< String > mutableList = new ArrayList<>();
        for( final String headerValues : headers.get( key ) ) { // NOTE: headers.get(key) is immutable and can not be sorted.
          final String[] values = headerValues.split( "," ); // Sometimes "PUT, GET" is returned as one list entry.
          for( final String value : values ) {
            mutableList.add( value.trim() );
          }
        }
        Collections.sort( mutableList );
        headers.put( key, mutableList );
      }

      // Convert HttpHeader to XmlHeader
      final XmlHeader xmlHeader = new XmlHeader();
      xmlHeader.setName( key );
      // Avoid brackets in the header value (which is induced because httpResponse.headers() is a List). Otherwise the resulting JSON is corrupted.
      final StringBuilder sb = new StringBuilder();
      final Iterator< String > it = headers.get( key ).iterator();
      while( it.hasNext() ) {
        sb.append( it.next() );
        if( it.hasNext() ) {
          sb.append( "," ); // See HTTP spec "RFC 2616"
        }
      }
      xmlHeader.setValue( sb.toString() );

      xmlHttpResponse.getHeaders().getHeader().add( xmlHeader );

      if( key.equalsIgnoreCase( HEADER_NAME__CONTENT_DISPOSITION ) ) {
        contentDisposition = xmlHeader.getValue();
      }
      else if( key.equalsIgnoreCase( HEADER_NAME__CONTENT_LENGTH ) ) {
        contentLength = Converter.asInteger( xmlHeader.getValue(), 0 );
        hasContentLengthHeader = true;
      }
      else if( key.equalsIgnoreCase( HEADER_NAME__CONTENT_TYPE ) ) {
        contentType = xmlHeader.getValue();
      }
    }

    // Set bodyIsJson flag
    xmlHttpResponse.setBodyIsJson( isJsonResponse( contentType ) );

    // Set body
    final byte[] rawBody = httpResponse.body(); // NEEDS FIX C: Check if this is required (e.g., when downloading a PNG)
    final Charset charSet = readCharsetFromContentTypeHeader( contentType, StandardCharsets.UTF_8 );
    xmlHttpResponse.setBody( new String( httpResponse.body(), charSet ) );

    // Fix missing content-length header
    // ( e.g., Jetty 12 does not return this header for performance reasons.
    //   They just stream the data, without knowing how long the body will be when sending the headers.
    // )
    if( !hasContentLengthHeader ) {
      contentLength = rawBody.length; // The length of the e.g. compressed body)
      final XmlHeader xmlHeader = new XmlHeader();
      xmlHeader.setName( HEADER_NAME__CONTENT_LENGTH );
      xmlHeader.setValue( Integer.toString( contentLength ) );
      xmlHttpResponse.getHeaders().getHeader().add( xmlHeader );
    }

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Received " + serviceId + " response body( " + ( xmlHttpResponse.isBodyIsJson() ? "json" : "non-json" )
               + " )" + ( LOG.isTraceEnabled() ? ": " + xmlHttpResponse.getBody() : "" ) );
    }

    // Apply response filters
    applyResponseFilters( xmlHttpResponse, xmlResponse, filterRegistry, serviceId, testId, testFileName );
    // NEEDS FIX C: Apply response variables if we have a use case for that

    // (Re-)set bodyIsJson flag (if changed by a filter)
    final XmlHeader contentTypeHeader = getHeader( HEADER_NAME__CONTENT_TYPE, xmlHttpResponse.getHeaders().getHeader() );
    final String newContentType = contentTypeHeader != null
      ? contentTypeHeader.getValue()
      : null;
    xmlHttpResponse.setBodyIsJson( isJsonResponse( newContentType ) && !isCompressed( xmlHttpResponse.getHeaders().getHeader() ) );

    // Set body
    if( !xmlResponse.isHideBody() ) {
      if( !xmlHttpResponse.isBodyIsJson()
       && isSuccessStatusCode( httpResponse.statusCode() )
       && contentLength > 0
      ) {
        // Download successful non JSON response body
        xmlHttpResponse.setDownload( createXmlDownload(
          rawBody,
          newContentType,
          serviceId,
          testId,
          contentDisposition,
          storeReportPath,
          testSetPath,
          testSetWorkPath
        ) );

        xmlHttpResponse.setBody( null ); // Either download or body
      }
    }
    else {
      xmlHttpResponse.setBody( "ResponseDiff: Body was hidden on demand (hideBody was set to true)." );
    }

    xmlResponse.setHttpResponse( xmlHttpResponse );

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "createXmlHttpResponse( serviceId="+ serviceId + " ) finished" );
    }
    return xmlHttpResponse;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the passed status code is a success status code (2xx)
   * @param statusCode The status code.
   * @return true if statusCode is in range [200 - 299]. Otherwise false is returned.
   */
  static boolean isSuccessStatusCode( final int statusCode )
  {
    return ( statusCode > 199 && statusCode < 300 );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Stores the body content to a file and creates a XmlDownload object that holds all information associated with to that file.
   * @param bytes The bytes to store. May be null.
   * @param contentType The content type. May be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param contentDisposition The content disposition header. May be null.
   * @param storeReportPath The path to store the download file. May be null.
   * @param testSetPath The path of the current TestSet. Must not be null.
   * @param testSetWorkPath An optional folder (relative to the testSetPath) that should be used (e.g., for downloads)
   * @return The link to the downloaded file as JSON.
   * @throws IOException
   */
  static XmlDownload createXmlDownload(
    final byte[] bytes,
    final String contentType,
    final String serviceId,
    final String testId,
    final String contentDisposition,
    final String storeReportPath,
    final String testSetPath,
    final String testSetWorkPath
  ) throws IOException {
    if( storeReportPath == null ) {
      return null;
    }

    String testName = testId;
    int pos = testName.lastIndexOf( TestSetHandler.ID_SEPARATOR );
    if( pos > 0 ) {
      testName = testName.substring( pos + TestSetHandler.ID_SEPARATOR.length() ).replaceAll( "\\s+", "-");
    }

    // Read filename from content disposition header.
    String fileName = readFileNameFromContentDispositionHeader( contentDisposition );

    if( fileName == null ) {
      final String extension = contentType != null
        ? getFileExtensionFromContentType( contentType )
        : "bin";

      fileName = new StringBuilder( "download." ).append( extension ).toString();
    }
    // NOTE: Using "__" around the serviceId will trigger the AsciiDoc renderer and ruin the resulting file URL
    fileName = testName + "_" + ( serviceId != null ? serviceId.trim() : "UNKNOWN" ) + "_" + fileName;

//LOG.error( "### RKR ###: filename="        + fileName );
//LOG.error( "### RKR ###: storeReportPath=" + storeReportPath );
//LOG.error( "### RKR ###: testSetPath="     + testSetPath );
//LOG.error( "### RKR ###: testSetWorkPath=" + testSetWorkPath );

    // Read body and store to file
    String relativeTestSetPath = testSetPath + testSetWorkPath;
    relativeTestSetPath = relativeTestSetPath + ( ( relativeTestSetPath.endsWith( "\\" ) || relativeTestSetPath.endsWith( "/" )) ? "" : File.separator ); // Assure there is a tailing file separator
    final String folderPath = storeReportPath + relativeTestSetPath; // This is an absolute path

    Files.createDirectories( Path.of( folderPath ) ); // Make sure path structure exists
    final String filePath = folderPath + fileName;
//LOG.error( "### RKR ###: filePath="        + filePath );
    try {
      Files.write( Path.of( filePath ), bytes, StandardOpenOption.CREATE );  // Error with "file=/export/home/rkreutz/work/develop/test/aixigo-responsediff/src/test/resources/poc/goals-and-constraints/../info.xml"
    }
    catch( final Throwable ex ) {
      final String message = "Error creating download file \"" + filePath + "\".";
      LOG.error( message );
      throw new RuntimeException( message, ex );
    }
    // Create XmlDownload
    final XmlDownload xmlDownload= new XmlDownload();
    xmlDownload.setFilename( relativeTestSetPath + fileName ); // We add a relative path here
    xmlDownload.setSize( bytes.length );

    return xmlDownload;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Computes a file extension for a given content type.
   * @param contentType The content type. May be null.
   * @return The requested content type. If content type is null, "bin" is returned.
   */
  static String getFileExtensionFromContentType( final String contentType )
  {
	  String extension = "bin";

	  if( contentType != null ) {
        // E.g., "application/hal+json; charset=UTF-8"
        int pos = contentType.lastIndexOf( "/" );
        if( pos > 0 ) {
          extension = contentType.substring( pos + 1 ); // => "hal+json; charset=UTF-8"
        }
        pos = extension.lastIndexOf( "+" );
        if( pos > 0 ) {
          extension = extension.substring( pos + 1 ); // => "json; charset=UTF-8"
        }
        pos = extension.lastIndexOf( ";" );
        if( pos > 0 ) {
          extension = extension.substring( 0, pos );  // => "json"
        }

        // Special mapping
        if( extension.equalsIgnoreCase( "plain" ) ) { // E.g.; "text/plain; charset=UTF-8"
          extension = "txt";
        }
	  }

      return extension;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the character set from a given content type.
   * @param contentType The content type. May be null.
   * @param fallback The fallback to use. May be null.
   * @return The requested character set. May be null.
   */
  static Charset readCharsetFromContentTypeHeader( final String contentType, final Charset fallback )
  {
    if( contentType == null ) {
      return fallback;
    }

    // Find entry
    int pos = contentType.toLowerCase().indexOf( "charset" );
    if( pos > 0 ) {
      String charset = contentType.substring( pos+7 ); // 7 = length of "charset"

      // Find start
      pos = charset.indexOf( "=" );
      charset = charset.substring( pos+1 ).trim();

      // Find end
      pos = charset.indexOf( " " );
      if( pos > 0 ) {
        charset = charset.substring( 0, pos );
      }
      pos = charset.indexOf( ";" );
      if( pos > 0 ) {
        charset = charset.substring( 0, pos );
      }

      return Charset.forName( charset.trim() );
    }

    return fallback;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the filename information (if any) from a given content disposition header.
   * @param contentDisposition The header to read from.
   * @return The filename if it could be found. Otherwise null is returned.
   */
  static String readFileNameFromContentDispositionHeader( final String contentDisposition )
  {
    if( contentDisposition == null ) {
      return null;
    }

    int pos = contentDisposition.toLowerCase().indexOf( "filename" );
    if( pos >= 0 ) {
      String fileName = contentDisposition.substring( pos+8 ) // 8 = length of "filename"
        .replaceAll( "\\\"", "" ) // Remove quotes
        .replaceAll( "=", "" );   // Remove "="

      pos = fileName.indexOf( ";" );
      if( pos > 0 ) {
        fileName = fileName.substring( 0, pos );
      }
      return Path.of( fileName.trim() ).getFileName().toString(); // Cut of path information (if any) (e.g. complete path that was added when the file was uploaded)
    }

    return null;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a XmlHttpResponse from the given "old" XmlSetup.
   * @param xmlRequest The XmlRequest. May be null.
   * @param xmlResponse The XmlResponse. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param referenceXmlSetup An optional XmlSetup that shall be used to simulate reference responses, if no reference service URL is configured. May be null.
   * @return The initialized XmlHttpResponse object. ay be null. If referenceXmlSetup is null, null is returned.
   */
  public static XmlHttpResponse createXmlHttpResponse(
    final XmlRequest           xmlRequest,
    final XmlResponse          xmlResponse,
    final String               serviceId,
    final String               testId,
    final String               testFileName,
    final XmlResponseDiffSetup referenceXmlSetup
  )
  {
    if( referenceXmlSetup == null ) {
      return null;
    }

    final XmlTest xmlTest = XmlResponseDiffSetupHelper.findXmlTest( referenceXmlSetup, testId, xmlRequest );
    if( xmlTest == null ) {
      throw new RuntimeException( "No matching test \"" + testId + "\" found in reference XML report." );
    }

    final XmlHttpResponse xmlHttpResponse = ( xmlTest.getResponse() != null )
      ? xmlTest.getResponse().getHttpResponse()
      : null;

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Found test with id \"" + testId + "\". httpResponse=" + ( xmlHttpResponse != null ? xmlHttpResponse.getBody() : "null" ) );
    }

    return xmlHttpResponse;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Decides if the response content is JSON according to the content type.
   * @param contentType The content type to check. May be null.
   * @return true, if the passed content type is not null and indicates a JSON content. Otherwise false is returned.
   */
  static boolean isJsonResponse( final String contentType )
  {
    if( contentType == null ) {
      return false;
    }

    // -----------------------------------------
    // Mime types of HATEOAS JSON formats:
    // -----------------------------------------
    // application/hal+json
    // application/prs.hal-forms+json
    // application/vnd.siren+json
    // application/vnd.collection+json
    // application/vnd.api+json
    // application/vnd.amundsen-uber+json
    //
    // May be followed e.g. by "; charset=utf-8"
    // -----------------------------------------

    final String contentTypeLower = contentType.toLowerCase();
    return contentTypeLower.matches( HEADER_VALUE__CONTENT_TYPE__JSON_PATTERN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the given headers indicate that the response is compressed (instead of being plain JSON text).
   * @param xmlHeaders The List of headers to use. Must not be null.
   * @return true if an compression indication could be found within the given headers. Otherwise false is returned.
   */
  static boolean isCompressed( final List< XmlHeader > xmlHeaders )
  {
    final XmlHeader contentEncodingHeader = getHeader( HEADER_NAME__CONTENT_ENCODING, xmlHeaders );

    if( contentEncodingHeader == null ) {
      return false;
    }

    final String encoding = contentEncodingHeader.getValue().toLowerCase();

    return encoding.contains( "gzip" )
        || encoding.contains( "compress" )
        || encoding.contains( "deflate" )
        || encoding.contains( "br" )
        || encoding.contains( "zstd" )
        ;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Replaces all found variable names within the request body by the defined variable values.
   * @param xmlRequest The XmlRequest to process. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   */
  static void handleRequestBody(
    final XmlRequest xmlRequest,
    final String     serviceId,
    final String     testId,
    final String     testFileName
  )
  {
    LOG.trace( "handleRequestBody()" );

    // Apply variables
    xmlRequest.setBody( VariablesHandler.applyVariables( xmlRequest.getBody(), xmlRequest.getVariables(), "body of test \"" + testId + "\"", serviceId, testId, testFileName ) );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Replaces all found variable names within the request parameters by the defined variable values.
   * @param xmlRequest The XmlRequest to process. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   */
  static void handleRequestParameters(
    final XmlRequest xmlRequest,
    final String     serviceId,
    final String     testId,
    final String     testFileName
  )
  {
    LOG.trace( "handleRequestParameters()" );

    final XmlVariables xmlVariables = xmlRequest.getVariables();
    if( xmlRequest.getParameters() != null && xmlVariables != null ) {
      for( final XmlParameter xmlParameter : xmlRequest.getParameters().getParameter() ) {
        xmlParameter.setValue( VariablesHandler.applyVariables( xmlParameter.getValue(), xmlVariables, "request parameter", serviceId, testId, testFileName ) );
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Adds the headers passed by the ResponseDiff configuration to the current XmlRequest (if not set in Test / TestSet).
   * @param xmlRequest The XmlRequest to process. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param xmlExternalHeaders The external Headers to use. May be null.
   */
  static void handleExternalHeaders(
    final XmlRequest xmlRequest,
    final String     serviceId,
    final String     testId,
    final String     testFileName,
    final List< XmlHeader > xmlExternalHeaders
  )
  {
    LOG.trace( "handleExternalHeaders()" );

    if( xmlExternalHeaders == null ) {
      return;
    }

    if( xmlRequest.getHeaders() == null ) {
      xmlRequest.setHeaders( new XmlHeaders() );
    }

    for( final XmlHeader xmlExternalHeader : xmlExternalHeaders ) {
      final String name = xmlExternalHeader.getName();

      boolean found = false;
      for( final XmlHeader xmlHeader : xmlRequest.getHeaders().getHeader() ) {
        // Check if header is already present
        if( xmlHeader.getName().trim().equalsIgnoreCase( name ) ) {
          found = true;
          break;
        }
      }
      if( !found ) {
        final XmlHeader copy = new XmlHeader();
        copy.setName( name );
        copy.setValue( xmlExternalHeader.getValue() );
        xmlRequest.getHeaders().getHeader().add( copy );
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Replaces all found variable names within the request headers by the defined variable values.
   * @param xmlRequest The XmlRequest to process. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   */
   static void handleRequestHeaders(
    final XmlRequest xmlRequest,
    final String     serviceId,
    final String     testId,
    final String     testFileName
  )
  {
     LOG.trace( "handleRequestHeaders()" );

    if( xmlRequest.getHeaders() == null || xmlRequest.getHeaders().getHeader() == null ) {
      return;
    }

    final List< XmlHeader > list = new ArrayList<>();
    for( final XmlHeader xmlHeader : xmlRequest.getHeaders().getHeader() ) {
      final XmlHeader copy = new XmlHeader();
      final String name = xmlHeader.getName();
      copy.setName( name );
      copy.setValue( VariablesHandler.applyVariables( xmlHeader.getValue(), xmlRequest.getVariables(), "header \"" + name + "\" for test \"" + testId + "\"", serviceId, testId, testFileName ) );
      list.add( copy );
    }

    xmlRequest.getHeaders().getHeader().clear();
    xmlRequest.getHeaders().getHeader().addAll( list );
  }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Replaces all found variable names within the request upload parts by the defined variable values.
    * @param xmlRequest The XmlRequest to process. Must not be null.
    * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
    * @param testId The current test id. Must not be null.
    * @param testFileName The file name the current test is configured in. Must not be null.
    */
    static void handleUploadParts(
     final XmlRequest xmlRequest,
     final String     serviceId,
     final String     testId,
     final String     testFileName
   )
   {
      LOG.trace( "handleUploadParts()" );

      if( xmlRequest.getUploadParts() == null ) {
       return;
     }

     final List< XmlFile > list = new ArrayList<>();
     for( final XmlFile xmlFile : xmlRequest.getUploadParts().getFile() ) {
       final XmlFile copy = new XmlFile();
       final String path = xmlFile.getValue();
       copy.setValue( path );
       copy.setName       ( VariablesHandler.applyVariables( xmlFile.getName(),        xmlRequest.getVariables(), "upload part name of file \""        + path + "\" for test \"" + testId + "\"", serviceId, testId, testFileName ) );
       copy.setCharSet    ( VariablesHandler.applyVariables( xmlFile.getCharSet(),     xmlRequest.getVariables(), "upload part charSet of file \""     + path + "\" for test \"" + testId + "\"", serviceId, testId, testFileName ) );
       copy.setContentType( VariablesHandler.applyVariables( xmlFile.getContentType(), xmlRequest.getVariables(), "upload part contentType of file \"" + path + "\" for test \"" + testId + "\"", serviceId, testId, testFileName ) );
       list.add( copy );
     }

     xmlRequest.getUploadParts().getFile().clear();
     xmlRequest.getUploadParts().getFile().addAll( list );
   }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Applies all configured request filters to the given XmlRequest
   * @param xmlRequest The XmlRequest. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param xmlTest The current XmlTest. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @param filterRegistry The registry that holds all registered filters. Must not be null.
   * @param nextDiffTest Flag, if a new diff test will be started (true) or not (false).
   * @throws DiffFilterException
   */
  static void applyRequestFilters(
    final XmlRequest xmlRequest,
    final String     serviceId,
    final XmlTest    xmlTest,
    final String     testFileName,
    final Map< String, DiffFilter > filterRegistry,
    final boolean    nextDiffTest
  )
  throws DiffFilterException
  {
    LOG.trace( "applyRequestFilters()" );

    if( xmlRequest == null || xmlRequest.getFilters() == null ) {
      return;
    }

    final String testId = xmlTest.getId();

    // Apply filters
    for( final XmlFilter xmlFilter : xmlRequest.getFilters().getFilter() ) {
      final String filterId = xmlFilter.getId();
      final DiffFilter filter = filterRegistry.get( filterId );
      if( filter == null ) {
        LOG.warn( "Filter \"" + filterId + "\" in " + serviceId + " test \"" + testId + "\" was configured for request but it was not registered. It is ignore. (file="  + testFileName + ")" );
      }
      else if( !( filter instanceof DiffRequestFilter ) ) {
        LOG.warn( "Filter \"" + filterId + "\" in " + serviceId + " test \"" + testId + "\" was configured for request but is not a request filter. It is ignore. (file=" + testFileName + ")" );
      }
      else {
        ( (DiffRequestFilter)filter ).apply( xmlRequest, serviceId, xmlTest );
        if( LOG.isTraceEnabled() ) {
          LOG.trace( "Request of " + serviceId + " test \"" + testId + "\" after applying filter \"" + filter.getClass().getSimpleName() + "\" : " + ToJson.fromXmlRequest( xmlRequest ) );
        }
        if( nextDiffTest ) {
          ( (DiffRequestFilter)filter ).next();
        }
      }
    }

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "Request of " + serviceId + " test \"" + testId + "\" after applying all request filters : " + ToJson.fromXmlRequest( xmlRequest ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Applies all configured response filters to the given XmlHttpResponse
   * @param xmlHttpResponse The response filter to apply the filters to. Must not be null.
   * @param xmlResponse The XmlResponse. Must not be null.
   * @param filterRegistry The registry that holds all registered filters. Must not be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @throws DiffFilterException
   */
  private static void applyResponseFilters(
    final XmlHttpResponse xmlHttpResponse,
    final XmlResponse     xmlResponse,
    final Map< String, DiffFilter > filterRegistry,
    final String          serviceId,
    final String          testId,
    final String          testFileName
  )
  throws DiffFilterException
  {
    LOG.trace( "applyResponseFilters()" );

    if( xmlHttpResponse == null || xmlResponse == null || xmlResponse.getFilters() == null ) {
      return;
    }

    String formerContentType = "" + getHeaderValue( xmlHttpResponse, HEADER_NAME__CONTENT_TYPE ); // Avoid handling of null values

    // Apply filters
    for( final XmlFilter xmlFilter : xmlResponse.getFilters().getFilter() ) {
      final String filterId = xmlFilter.getId();
      final DiffFilter filter = filterRegistry.get( filterId );
      if( filter == null ) {
        LOG.warn( "Filter \"" + filterId + "\" in test \"" + testId + "\" was configured for response of test case \"" + testId + "\" but it was not registered. It is ignore. (file=" + testFileName + ")" );
      }
      else if( !( filter instanceof DiffResponseFilter ) ) {
        LOG.warn( "Filter \"" + filterId + "\" in test \"" + testId + "\" was configured for response of test case \"" + testId + "\" but is not a response filter. It is ignore. (file=" +testFileName + ")" );
      }
      else {
        ( (DiffResponseFilter)filter ).apply( xmlHttpResponse );
        if( LOG.isTraceEnabled() ) {
          LOG.trace( "Response for " + serviceId + " of test \"" + testId + "\" after applying filter \"" + filter.getClass().getSimpleName() + "\" : " + ToJson.fromXmlResponse( xmlHttpResponse ) );
        }
      }

      if( LOG.isDebugEnabled() ) {
        final String newContentType = "" + getHeaderValue( xmlHttpResponse, HEADER_NAME__CONTENT_TYPE ); // Avoid handling of null values
        if( !formerContentType.equalsIgnoreCase( newContentType ) ) {
          LOG.debug( "Filter \"" + filterId + "\" changed content-type header from \"" + formerContentType + "\" to \"" + newContentType + "\"." );
          formerContentType = newContentType;
        }
      }
    } // for xmlFilter

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "Response for " + serviceId + " of test \"" + testId + "\" after applying all response filters : " + ToJson.fromXmlResponse( xmlHttpResponse ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Looks up the header value of a given header name.
   * @param xmlHttpResponse The XmlHttpResponse to use. Must not be null.
   * @param headerName The header name. Must not be null.
   * @return The requested header value. If no matching header could be found, nuzll is returned.
   */
  private static String getHeaderValue( final XmlHttpResponse xmlHttpResponse, final String headerName )
  {
    for( final XmlHeader xmlHeader : xmlHttpResponse.getHeaders().getHeader() ) {
      if( xmlHeader.getName().equalsIgnoreCase( headerName ) ) {
        return xmlHeader.getValue();
      }
    }
    return null;
  }
}
