package com.github.kreutzr.responsediff.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.File;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.ResponseDiff;
import com.github.kreutzr.responsediff.tools.FormatHelper;

import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlVariable;
import com.github.kreutzr.responsediff.XmlVariables;

public class TestBase
{
  protected static final String rootPath_     = new File( "" ).getAbsolutePath() + File.separator;
  protected static final String CANDIDATE_URL = "http://candidate/";
  protected static final String REFERENCE_URL = "http://reference/";
  protected static final String CONTROL_URL   = "http://control/";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected static ResponseDiff responseDiff_ = null;
  protected static XmlHeaders   xmlHeaders_   = null;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @BeforeAll
  public static void init() throws Exception
  {
    String  xmlFilePath             = null;
    String  testIdPattern           = null;
    String  xsltFilePath            = "src/main/resources/com/github/kreutzr/responsediff/reporter/report-to-adoc.xslt";
    String  reportFileEnding        = "adoc";
    String  reportConversionFormats = ""; // html, pdf (CAUTION: This will produce a lot of reports in the test-result folder)
    String  storeResultPath         = "./test-results/";
    boolean reportWhiteNoise        = false;
    String  ticketServiceUrl        = null;
    Long    responseTimeoutMs       = 1000L;
    Double  epsilon                 = 0.00000001;
    String  referenceFilePath       = null;
    Boolean exitWithExitCode        = false; // Disable for local IDE testing

    List< XmlHeader > candidateHeaders = new ArrayList<>();
    List< XmlHeader > referenceHeaders = new ArrayList<>();
    List< XmlHeader > controlHeaders   = new ArrayList<>();

    responseDiff_ = new ResponseDiff(
      rootPath_,
      xmlFilePath,
      testIdPattern,
      xsltFilePath,
      reportFileEnding,
      reportConversionFormats,
      storeResultPath,
      reportWhiteNoise,
      ticketServiceUrl,
      CANDIDATE_URL,
      candidateHeaders,
      REFERENCE_URL,
      referenceHeaders,
      CONTROL_URL,
      controlHeaders,
      responseTimeoutMs,
      epsilon,
      referenceFilePath,
      exitWithExitCode
    );

    // Define default headers (JSON)
    final XmlHeader xmlHeader1 = new XmlHeader();
    xmlHeader1.setName ( HttpHandler.HEADER_NAME__CONTENT_TYPE );
    xmlHeader1.setValue( HttpHandler.HEADER_VALUE__CONTENT_TYPE__JSON + "; charset=utf-8" );
    final XmlHeader xmlHeader2 = new XmlHeader();
    xmlHeader2.setName ( "receiveTime" );
    xmlHeader2.setValue( FormatHelper.formatIsoDateTime( new Date() ) + createTimeStampSuffix() );
    xmlHeaders_ = new XmlHeaders();
    xmlHeaders_.getHeader().add( xmlHeader1 );
    xmlHeaders_.getHeader().add( xmlHeader2 );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String createTimeStampSuffix()
  {
    double random = Math.random();

    // Add millis
    String suffix = ( random < 0.25 )
      ? ""
      : ".123";

    // Add time zone
    random = Math.random();
    if( random < 0.25 ) {
      // No time zone
      return suffix;
    }
    if( random < 0.5 ) {
      // Time zone Z
      return suffix + "Z";
    }
    // Time offset
    suffix += ( random < 0.75 )
        ? "+01"
        : "-01";

    // Prolong time zone offset
    random = Math.random();
    if( random < 0.333 ) {
      // No time zone offset prolongation
      return suffix;
    }
    suffix += ( random < 0.666 )
        ? "23"
        : ":23";

    return suffix;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @SuppressWarnings("unchecked")
  public void initResponseMock(
    final MockedStatic< HttpHandler > httpHandler,
    final String serviceUrl,
    final int status,
    final String body,
    final XmlHeaders xmlHeaders
  )
  {
    final HttpResponse< byte[] > httpResponse = new HttpResponseInstance(
      status,
      null, // request
      body.getBytes(),
      xmlHeaders
    );

    if( serviceUrl != null ) {
      httpHandler.when( () ->  HttpHandler.sendRequest(
        any( XmlRequest.class ), // xmlRequest
        any( List.class ),       // xmlHeaders
        any( Builder.class),     // builder
        eq( serviceUrl ),        // serviceUrl
        anyString(),             // serviceId
        anyString(),             // testId
        anyString()              // testFileName
      ) )
      .thenReturn( CompletableFuture.completedFuture( httpResponse ) );
    }
    else {
      httpHandler.when( () ->  HttpHandler.sendRequest(
        any( XmlRequest.class ), // xmlRequest
        any( List.class ),       // xmlHeaders
        any( Builder.class),     // builder
        anyString(),             // serviceUrl
        anyString(),             // serviceId
        anyString(),             // testId
        anyString()              // testFileName
      ) )
      .thenReturn( CompletableFuture.completedFuture( httpResponse ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the value of a XmlVariable with the given id from the given XmlVariables.
   * @param xmlVariables The XmlVariables to lookup. May be null.
   * @param id The variable id to lookup. Must not be null.
   * @return The variable value. May be null.
   */
  protected String getVariableValue( final XmlVariables xmlVariables, final String id )
  {
    if( xmlVariables == null ) {
      return null;
    }
    for( final XmlVariable xmlVariable : xmlVariables.getVariable() ) {
      if( xmlVariable.getId().equals( id ) ) {
        return xmlVariable.getValue();
      }
    }
    return null;
  }
}
