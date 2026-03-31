package com.github.kreutzr.responsediff.proxy;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.JsonHelper;
import com.sun.net.httpserver.HttpServer;

public class ResponseDiffProxy 
{
  private static final Logger LOG = LoggerFactory.getLogger( ResponseDiffProxy.class );
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void main( final String[] args )
  {
    // ====================================
    // Check configuration
    // ====================================
    if( args == null || args.length != 1 ) {
      LOG.error( "The configuration JSON parameter is missing. Pass it as first (only) parameter." );
      System.exit( 1 );
    }

    ProxyConfiguration config = null;
    try
    {
      config = JsonHelper.provideObjectMapper().readValue( args[ 0 ], ProxyConfiguration.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      System.exit( 1 );
    }
    
    // Check mandatory configuration parameters
    if( config.getTargetBaseUrl() == null || config.getTargetBaseUrl().isBlank() ) {
      LOG.error( "The configuration parameter \"targetBaseUrl\" is missing." );
      System.exit( 1 );
    }
    
    // Set fallback configuration
    config.setPort       ( Converter.asInteger( config.getPort(),         8080 ) );
    config.setStoragePath( Converter.asString ( config.getStoragePath(),  "." ) );
    config.setMaxThreads ( Converter.asInteger( config.getMaxThreads(),   10 ) );
    if( config.getBasePathsToConsider() == null ) {
      config.setBasePathsToConsider( new ArrayList< String >() ); // Avoid null value
    }
    if( config.getBasePathsToIgnore() == null ) {
      config.setBasePathsToIgnore( new ArrayList< String >() ); // Avoid null value
    }
  
    try {
      // ====================================
      // Initialize directories
      // ====================================
      final Path storage = Path.of( config.getStoragePath() );
      if (!Files.exists(storage)) {
        Files.createDirectories( storage );
        System.out.println( "Created storage folder: " + storage.toAbsolutePath() );
      }
    
      // ====================================
      // Start proxy
      // ====================================
      final HttpServer server = HttpServer.create( new InetSocketAddress( config.getPort() ), 0 );

      // Register handler with shutdown command
      final ShutdownCommand shutDownCommand = () -> {
    	  System.out.println( "Shutdown command received." );
        new Thread(() -> {
          try {
            Thread.sleep( 500 ); // Time to respond to browser
            server.stop( 0 );    // Stop server hard
            System.exit( 0 );    // Quit JVM 
          }
          catch (InterruptedException e) {
            System.exit(1);
          }
        }).start();
      };
      server.createContext( "/", new ProxyHandler( config, shutDownCommand ) );
      
      final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( config.getMaxThreads() );
      server.setExecutor( executor );

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    	  System.out.println( "Shutdown signal received. Stopping proxy." );
        server.stop( 2 ); // Give a bit of time for running requests
        executor.shutdown();
        try {
          if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        } catch (InterruptedException e) {
          executor.shutdownNow();
        }
        System.out.println( "Proxy terminated gracefully." );
      }));

      // Start server
      server.start();
        
      System.out.println( "===============================================" );
      System.out.println( "Proxy started" );
      System.out.println( "Local URL:  http://localhost:" + config.getPort() );
      System.out.println( "Target-URL: " + config.getTargetBaseUrl() );
      System.out.println( "Storage:    " + storage.toAbsolutePath() );
      System.out.println( "Press Strg+C to terminate");
      System.out.println("===============================================");
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      LOG.error( "An error occurred: " + ex.getMessage(), ex );
      System.exit( 2 );
    }    
  }
}
