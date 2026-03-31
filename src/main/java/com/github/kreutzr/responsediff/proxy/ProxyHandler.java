package com.github.kreutzr.responsediff.proxy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.XmlHeader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ProxyHandler implements HttpHandler 
{
  private static final Logger LOG = LoggerFactory.getLogger( ProxyHandler.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
  private final ProxyConfiguration config_;
  private final ShutdownCommand    shutDownCommand_;
  private final HttpClient         httpClient_;
  private List<String>             restrictedRequestHeaders_;
  private String                   htmlFileName_ = "index.html";  
  private String                   htmlContent_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param config The ProxyConfiguration. Must not be null.
   */
  public ProxyHandler( final ProxyConfiguration config, final ShutdownCommand shutDownCommand ) 
  {
    config_                   = config;
    shutDownCommand_          = shutDownCommand;
    httpClient_               = HttpClient.newBuilder().build();
    restrictedRequestHeaders_ =  List.of( 
      "host", 
      "content-length", 
      "connection",
      "upgrade", 
      "accept-encoding"  // We do not want to decode the response data for recording. 
    );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Handles the given exchange.
   * @param exchange The exchange to handle. Must not be null.
   * @throws IOException
   */
  @Override
  public void handle( final HttpExchange exchange ) 
  throws IOException 
  {
    try {
      final URI requestUri = exchange.getRequestURI(); 
      final String path  = requestUri.getPath();
      final String query = requestUri.getQuery();
        
      // ====================================
      // Handle proxy "commands" 
      // ====================================
      if( "/START".equalsIgnoreCase( path ) ) {
        // Read title information
        if( query != null && !query.isBlank() ) {
          final String[] parts = query.split( "&" );
          for( final String part : parts ) {
            final int pos = part.indexOf( "=" );
            if( pos > 0 ) {
              final String key   = part.substring( 0 , pos );
              final String value = part.substring( pos + 1 );
              if( key.equalsIgnoreCase( "TITLE" ) ) {
                LOG.error( "### RKR ### START TITLE=" + value );
                // ### TODO: Set title !
              }
            }
          }
        }
        else {
          LOG.error( "### RKR ### START" );
        }
        // ### TODO: Change internal state !
        
        // Send response to browser
        sendDefaultResponse( exchange );

        return;
      }
      else if( "/STOP".equalsIgnoreCase( path ) ) {
        createTestSetup();
        
        // Send response to browser
        sendDefaultResponse( exchange );
        
        return;
      }
      else if( "/QUIT".equalsIgnoreCase( path ) ) {
        // Send response to browser
        sendDefaultResponse( exchange );
        
        // ### TODO: Check that state is STOPPED!

        LOG.error( "### RKR ### QUIT" );
        shutDownCommand_.execute();
        return;
      }


      // ====================================
      // Forward request to server
      // ====================================
      final String targetUrl = config_.getTargetBaseUrl() + path + (query != null ? "?" + query : "");

      // Read request data
      final byte[] requestBody = exchange.getRequestBody().readAllBytes();

      // Build forward request
      final HttpRequest.Builder rb = HttpRequest.newBuilder()
        .uri( URI.create( targetUrl ) )
        .method( exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray( requestBody ) );

      // Copy request headers
      exchange.getRequestHeaders().forEach( (key, values) -> {
        if ( !isRestrictedHeader( key ) ) {
          values.forEach( v -> rb.header( key, v ) );
        }
      } );
      // Add default headers
      for( final XmlHeader xmlHeader : config_.getDefaultHeaders() ) {
        rb.header( xmlHeader.getName(), xmlHeader.getValue() );
      }

      // Send request
      final HttpResponse<byte[]> response = httpClient_.send( rb.build(), HttpResponse.BodyHandlers.ofByteArray() );

      // ====================================
      // Check if response should be recorded
      // ====================================
      if( shouldRecord( path ) ) {
        performRecording( path, exchange.getRequestMethod(), requestBody, response );
      }
      else {
        LOG.info( "Ignoring path " + path );
      }

      // ====================================
      // Forward response to calling browser
      // ====================================
      
      // Copy response headers
      response.headers().map().forEach( (key, values) -> {
        if( !key.equalsIgnoreCase( "Transfer-Encoding" ) ) { // Chunked encoding are handled by the HttpServer
          values.forEach( v -> exchange.getResponseHeaders().add( key, v ) );
        }
      });

      exchange.sendResponseHeaders( response.statusCode(), response.body().length == 0 ? -1 : response.body().length );
      try( OutputStream os = exchange.getResponseBody() ) {
        os.write( response.body() );
      }
    }
    catch ( final Exception ex ) {
      ex.printStackTrace();
    } 
    finally {
      exchange.close();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Check, if the given path should be recorded or not.
   * @param path The path to check. Must not be null.
   * @return True, if the path should be recorded. Otherwise false is returned.
   */
  public boolean shouldRecord( final String path ) 
  {
    // Whitelist Check: Allow all when list is empty. Otherwise path must match.
    final List< String > basePathsToConsider = config_.getBasePathsToConsider();
    final boolean whitelisted = basePathsToConsider.isEmpty() || basePathsToConsider.stream().anyMatch( path::startsWith );
        
    if (!whitelisted) return false;

    // Blacklist Check: None of the listed entries is allowed. 
    final List< String > basePathsToIgnore = config_.getBasePathsToIgnore();
    final boolean blacklisted = basePathsToIgnore.stream().anyMatch( path::startsWith );
    
    return !blacklisted;      
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the given header name is restricted.
   * @param headerName The header name to check. Must not be null.
   * @return True, if  the given header name is restricted. Otherwise false is returned.
   */
  private boolean isRestrictedHeader( final String headerName )
  {
    return restrictedRequestHeaders_.stream().anyMatch( h -> h.equalsIgnoreCase( headerName ) );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Send a default response after the proxy has received a control command.
   * @param exchange The HttpExchange to use. Must not be null.
   * @throws IOException
   */
  private void sendDefaultResponse( final HttpExchange exchange )
  throws IOException
  {
	  if( htmlContent_ == null ) {
		  htmlContent_ = readIndexHtmlFile( htmlFileName_ );
	  }

	  exchange.sendResponseHeaders( 200, htmlContent_.length() );
      try( OutputStream os = exchange.getResponseBody() ) {
        os.write( htmlContent_.getBytes() );
      }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the control HTML file.
   * @param The name of the file to read. Must not be null.  
   * @return The HTML content of the controll HTML file.
   * @throws IOException
   */
  private String readIndexHtmlFile( final String fileName ) throws IOException
  {
    final StringBuilder sb = new StringBuilder();
	String line;
	
	BufferedReader in = null;
	try {
      in = new BufferedReader( new FileReader( fileName ) );
      while( (line = in.readLine()) !=null ) {
        sb.append( line );
      }
	}
	finally {
      in.close();
	}

    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * 
   * @param path
   * @param method
   * @param reqBody
   * @param response
   */
  private void performRecording(
    final String path, 
    final String method, 
    final byte[] reqBody, 
    final HttpResponse<byte[]> response 
  ) {
    System.out.println("RECORDING: " + method + " " + path + " -> Status: " + response.statusCode());
    // ### TODO Store everything in memory for later analysation
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private void createTestSetup()
  {
    LOG.info( "Starting test setup creation." );
    // ### TODO: Implement this!
  }
}