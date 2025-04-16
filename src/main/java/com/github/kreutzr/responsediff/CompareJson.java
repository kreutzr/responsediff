package com.github.kreutzr.responsediff;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.filter.response.SortJsonBodyResponseFilter;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * Small tool to compare two given JSON files.
 * It compiles the compare results into a short AsciiDoc report.
 *
 * CompareJson <reference> <candidate>
 */
public class CompareJson
{
  private static final String LINEBREAK = "\n\n";

  private static final Logger LOG = LoggerFactory.getLogger( CompareJson.class );

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private JsonDiff jsonDiff_;
  private SortJsonBodyResponseFilter filter_;
  private String referenceFilePath_;
  private String candidateFilePath_;
  private String referenceJson_;
  private String candidateJson_;

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param referenceFilePath The reference file path. Must not be null.
   * @param candidateFilePath The candidate file path. Must not be null.
   * @param trim Flag, if String comparison shall trim the Strings (true) or not (false). (Default is false)
   * @param ignoreCase Flag, if String comparison shall be case insensitive (true) or not (false). (Default is false)
   * @param epsilon The epsilon to use.
   * @param ignorePaths A set that holds all JsonPaths that shall be ignored. (Default is an empty set)
   * @param sortArrays Flag, if JSON arrays shall be sorted before comparison (true) or not (false).
   * @param sortArraysKeys A comma separated list of those array keys that shall be considered for sorting. An empty String means "all keys". All keys are considered by default if sortArrays is set true.
   */
  public CompareJson(
    final String        referenceFilePath,
    final String        candidateFilePath,
    final boolean       trim,
    final boolean       ignoreCase,
    final double        epsilon,
    final Set< String > ignorePaths,
    final boolean       sortArrays,
    final String        sortArraysKeys
  )
  {
    referenceFilePath_ = referenceFilePath;
    candidateFilePath_ = candidateFilePath;

    if( referenceFilePath_ == null || referenceFilePath_.trim().isEmpty() ) {
      throw new RuntimeException( "A reference JSON file is required." );
    }
    if( candidateFilePath_ == null || candidateFilePath_.trim().isEmpty() ) {
      throw new RuntimeException( "A candidate JSON file is required." );
    }

    try {
      // Create and initialize filter
      filter_ = new SortJsonBodyResponseFilter();
      filter_.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "" + sortArrays );
      filter_.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "" + sortArraysKeys );

      referenceJson_ = filter_.apply( Files.readString( Path.of( referenceFilePath_ ), StandardCharsets.UTF_8 ) );
      candidateJson_ = filter_.apply( Files.readString( Path.of( candidateFilePath_ ), StandardCharsets.UTF_8 ) );

      // Create and initialize JsonDiff
      jsonDiff_ = JsonDiff.createInstance();
      jsonDiff_.setReference( referenceJson_ );
      jsonDiff_.setCandidate( candidateJson_ );
      jsonDiff_.setTrim( trim );
      jsonDiff_.setIgnoreCase( ignoreCase );
      jsonDiff_.setEpsilon( epsilon );
      jsonDiff_.setIgnorePaths( ignorePaths );

      // Calculate differences
      jsonDiff_.calculate();
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      LOG.error( "An error occurred: " + ex.getMessage(), ex );
      System.exit( 2 );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getReport()
  {
    final StringBuilder sb = new StringBuilder( "= Json compare" )
      .append( "\n" )
      .append( ":doctype: book\n" )
      .append( ":encoding: utf-8\n" )
      .append( ":lang: de\n" )
      .append( ":toc: left\n" )
      .append( ":toclevels: 5\n" )
      .append( LINEBREAK )

      .append( "Reference: " ).append( referenceFilePath_ )
      .append( LINEBREAK )
      .append( "Candidate: " ).append( candidateFilePath_ )
      .append( LINEBREAK )

      .append( "== Changes" )
      .append( LINEBREAK )
      .append( writeDiffEntries( jsonDiff_.getChanges() ) )
      .append( LINEBREAK )

      .append( "== Additions" )
      .append( LINEBREAK )
      .append( writeDiffEntries( jsonDiff_.getAdditions() ) )
      .append( LINEBREAK )

      .append( "== Deletions" )
      .append( LINEBREAK )
      .append( writeDiffEntries( jsonDiff_.getDeletions() ) )
      .append( LINEBREAK )

      .append( "== Reference" )
      .append( LINEBREAK )
      .append( writeJson( referenceJson_ ) )
      .append( LINEBREAK )

      .append( "== Candidate" )
      .append( LINEBREAK )
      .append( writeJson( candidateJson_ ) )
      .append( LINEBREAK )
      ;

    return sb.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Indicates, if differences between the two JSON files were found.
   * @return true if any difference between the two JSON files was found. Otherwise false is returned.
   */
  private boolean hasDifference()
  {
    return jsonDiff_.hasDifference();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private String writeDiffEntries( final List< JsonDiffEntry > entries )
  {
    final StringBuilder sb = new StringBuilder()
      .append( "[cols=\"20,80\"]\n" )
      .append( "|===\n" );

    for( final JsonDiffEntry entry : entries ) {
      sb.append( "| " ).append( entry.getJsonPath() )
        .append( " | "  ).append( entry.getMessage() )
        .append( "\n" )
        ;
    }

    return sb
      .append( "|===\n" )
      .toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private String writeJson( final String json )
  {
    final StringBuilder sb = new StringBuilder()
      .append( "[source,json]\n" )
      .append( "----\n" )
      .append( json ).append( "\n" )
      .append( "----\n" );

    return sb.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final void main( final String[] args )
  {
    if( args == null || args.length != 1 ) {
      LOG.error( "The configuration JSON parameter is missing. Pass it as first (only) parameter." );
      System.exit( 2 );
    }

    CompareJsonConfiguration config = null;
    try
    {
      config = JsonHelper.provideObjectMapper().readValue( args[ 0 ], CompareJsonConfiguration.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      LOG.error( "An error occurred: " + ex.getMessage(), ex );
      System.exit( 2 );
    }

    // Pre-initialize
    String        referenceFilePath = null;
    String        candidateFilePath = null;
    String        storeResultPath   = null;
    boolean       trim              = true;
    boolean       ignoreCase        = true;
    double        epsilon           = 0.0000001;
    String        ignorePathsString = "";
    Set< String > ignorePaths       = new TreeSet<>();
    boolean       sortArrays        = true;
    String        sortArraysKeys    = "";

    // Initialize from parameter
    referenceFilePath = Converter.asString ( config.getReferenceFilePath(), referenceFilePath );
    candidateFilePath = Converter.asString ( config.getCandidateFilePath(), candidateFilePath );
    storeResultPath   = Converter.asString ( config.getStoreResultPath(),   storeResultPath );
    trim              = Converter.asBoolean( config.isTrim(),               trim );
    ignoreCase        = Converter.asBoolean( config.isIgnoreCase(),         ignoreCase );
    epsilon           = Converter.asDouble ( config.getEpsilon(),           epsilon );
    ignorePathsString = Converter.asString ( config.getIgnorePaths(),       ignorePathsString );
    ignoreCase        = Converter.asBoolean( config.isSortArrays(),         sortArrays );
    sortArraysKeys    = Converter.asString ( config.getSortArraysKeys(),    sortArraysKeys );

    // Create ignore paths set
    String[] paths = ignorePathsString.split( "," );
    for( final String path : paths ) {
      ignorePaths.add( path );
    }

    try {
      // Create worker
      final CompareJson compareJson = new CompareJson(
        referenceFilePath,
        candidateFilePath,
        trim,
        ignoreCase,
        epsilon,
        ignorePaths,
        sortArrays,
        sortArraysKeys
      );

      // Create and export report
      // NOTE: To be successful "storeResultPath" must point to a file NOT a folder.
      Files.write( Path.of( storeResultPath ), compareJson.getReport().getBytes() );

      System.exit( compareJson.hasDifference() ? 1 : 0  );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      LOG.error( "An error occurred: " + ex.getMessage(), ex );
      System.exit( 2 );
    }
  }
}
