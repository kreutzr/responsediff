package com.github.kreutzr.responsediff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * Helper class to compute differences between two given JSON data sets.
 */
public class JsonDiff
{
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger( JsonDiff.class );

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private JsonNode       reference_      = null;
  private JsonNode       candidate_      = null;
  private double         epsilon_        = 0.00001;
  private boolean        trim_           = false;
  private boolean        ignoreCase_     = false;
  private boolean        ready_          = false;
  private Set< String >  ignorePaths_    = new TreeSet<>();
//  private JsonPathHelper jsonPathHelper_ = null;
  private long           expectedCount_  = 0;

  private List< JsonDiffEntry > changes_   = new ArrayList<>();
  private List< JsonDiffEntry > deletions_ = new ArrayList<>();
  private List< JsonDiffEntry > additions_ = new ArrayList<>();

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private JsonDiff()
  {
    // Nothing to do here...
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates an new instance of a JsonDiff object.
   * <b>NOTE:</b> Use createDataInstance() if using the instance as data container but NOT for differnece calculation.
   * @return A new instance of a JsonDiff object.
   */
  public static JsonDiff createInstance()
  {
    return new JsonDiff();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates an new instance of a JsonDiff object which is used as data container but NOT for difference calculation.
   * @return A new instance of a JsonDiff object.
   */
  public static JsonDiff createDataInstance()
  {
    final JsonDiff jsonDiff = new JsonDiff();
    jsonDiff.setReady();
    return jsonDiff;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void setReady()
  {
    ready_ = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the reference Json data set, the candidate is compared against.
   * @param reference The reference Json data set. Must not be null.
   * @return this.
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  public JsonDiff setReference( final String reference ) throws JsonMappingException, JsonProcessingException
  {
    reference_ = JsonHelper.provideObjectMapper().readTree( reference );
    ready_ = false;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the candidate Json data set, to compared against the reference.
   * @param candidate The candidate Json data set. Must not be null.
   * @return this.
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  public JsonDiff setCandidate( final String candidate ) throws JsonMappingException, JsonProcessingException
  {
    candidate_ = JsonHelper.provideObjectMapper().readTree( candidate );
    ready_ = false;
//
//    jsonPathHelper_ = new JsonPathHelper( candidate );
//
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * The epsilon to use for number comparison. (Default is 0.00001)
   * @param epsilon The epsilon to use.
   * @return this.
   */
  public JsonDiff setEpsilon( final double epsilon )
  {
    epsilon_ = epsilon;
    ready_ = false;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Allows String trimming for comparison.
   * @param trim Flag, if String comparison shall trim the Strings (true) or not (false). (Default is false)
   * @return this.
   */
  public JsonDiff setTrim( final boolean trim )
  {
    trim_ = trim;
    ready_ = false;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Allows case insensitive String comparison.
   * @param ignoreCase Flag, if String comparison shall be case insensitive (true) or not (false). (Default is false)
   * @return this.
   */
  public JsonDiff setIgnoreCase( final boolean ignoreCase )
  {
    ignoreCase_ = ignoreCase;
    ready_ = false;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Allows define all JsonPaths that shall be ignored. <br/><b>NOTE:</b> All formerly set JsonPaths are replaced.
   * @param ignorePaths A set that holds all JsonPaths that shall be ignored. (Default is an empty set)
   * @return this.
   */
  public JsonDiff setIgnorePaths( final Set< String > ignorePaths )
  {
    ignorePaths_ = ignorePaths;
    ready_ = false;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Allows to add a JsonPath that shall be ignored.
   * @param ignorePath A JsonPaths that shall be ignored.
   * @return this.
   */
  public JsonDiff addIgnorePath( final String ignorePath )
  {
    ignorePaths_.add( ignorePath );
    ready_ = false;
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public long getExpectedCount()
  {
    return expectedCount_;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setExpectedCount( final long expectedCount )
  {
    expectedCount_ = expectedCount;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void incrementExpectedCount()
  {
    expectedCount_ += 1;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs the difference calculation. This is automatically invoked when accessing the getters.
   * @return this.
   */
  public JsonDiff calculate()
  {
    calculateIfRequired();
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return A list
   */
  public List< JsonDiffEntry > getChanges()
  {
    calculateIfRequired();
    return changes_;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public List< JsonDiffEntry > getDeletions()
  {
    calculateIfRequired();
    return deletions_;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public List< JsonDiffEntry > getAdditions()
  {
    calculateIfRequired();
    return additions_;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the given list holds any entry with LogLevel ERROR or more severe (FATAL).
   * @param jsonDiffEntries The list to check. Must not be null.
   * @return true if any entry with LogLevel ERROR or more severe was found. Otherwise false is returned.
   */
  public static boolean hasAnyError( final List< JsonDiffEntry > jsonDiffEntries )
  {
    for( final JsonDiffEntry jsonDiffEntry : jsonDiffEntries ) {
      if( jsonDiffEntry.getLogLevel() == XmlLogLevel.ERROR
       || jsonDiffEntry.getLogLevel() == XmlLogLevel.FATAL
      ) {
        return true;
      }
    }
    return false;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the changes list holds any entry with LogLevel ERROR or more severe (FATAL) or any addition or deletion was detected.
   * @return true if any entry with LogLevel ERROR or more severe was found or any addition or deletion was detected. Otherwise false is returned.
   */
  public boolean hasAnyError()
  {
    return hasAnyError( getChanges() ) || !getAdditions().isEmpty() || !getDeletions().isEmpty();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the given list holds any entry with LogLevel WARN.
   * @return true if any entry with LogLevel WARN was found. Otherwise false is returned.
   */
  public boolean hasAnyWarning()
  {
    for( final JsonDiffEntry jsonDiffEntry : getChanges() ) {
      if( jsonDiffEntry.getLogLevel() == XmlLogLevel.WARN ) {
        return true;
      }
    }
    return false;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if any change, any addition or deletion was detected.
   * @return true if any change, any addition or deletion was detected. Otherwise false is returned.
   */
  public boolean hasDifference()
  {
    return !getChanges().isEmpty() || !getAdditions().isEmpty() || !getDeletions().isEmpty();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Joins all entries of a given JsonDiff into this.
   * @param other The JsonDiff to join. May be null.
   */
  public void join( final JsonDiff other )
  {
    join( null, other );
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Joins all entries of a given JsonDiff into this.
   * @param pos The position where the other values shall be inserted. May be null. If null is passed, the entries are appended.
   * @param other The JsonDiff to join. May be null.
   */
  public void join( final Integer pos, final JsonDiff other )
  {
    if( other == null || !other.hasDifference() ) {
      return;
    }

    if( pos != null ) {
      changes_  .addAll( pos, other.getChanges  () );
      additions_.addAll( pos, other.getAdditions() );
      deletions_.addAll( pos, other.getDeletions() );
    }
    else {
      changes_  .addAll( other.getChanges  () );
      additions_.addAll( other.getAdditions() );
      deletions_.addAll( other.getDeletions() );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void calculateIfRequired()
  {
    if( ready_ ) {
      return;
    }

    // Reset all results
    changes_.clear();
    deletions_.clear();
    additions_.clear();

    if( candidate_ != null && reference_ != null ) {
      iterate( candidate_, reference_, "$" );
    }

    Collections.sort( changes_ );
    Collections.sort( deletions_ );
    Collections.sort( additions_ );

    ready_ = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterate( final JsonNode candidate, final JsonNode reference, final String path )
  {
    // NEEDS FIX B: Allow wildcards here.
    if( ignorePaths_.contains( path ) ) {
      return;
    }
// NEEDS FIX B: Allow wildcards here. But this approach is too naiv. The performance is horrible!
//    for( final String ignorePath : ignorePaths_ ) {
//      if( jsonPathHelper_.contains( ignorePath, path ) ) {
//        return;
//      }
//    }

    final JsonNodeType canType = candidate.getNodeType();
    final JsonNodeType refType = reference.getNodeType();

    if( canType != refType ) {
      final StringBuilder sb = new StringBuilder()
         .append( "Expected type: " ).append( refType.name() )
         .append( " but was: " ).append( canType.name() );

      changes_.add( new JsonDiffEntry( path, canType.name(), refType.name(), null, sb.toString() ) );
      return;
    }

    switch( canType ) {
    case ARRAY   : iterateArray( candidate, reference, path );
      break;
    case OBJECT  : iterateMap( candidate, reference, path );
      break;
    case BOOLEAN : compareBoolean( candidate, reference, path );
      break;
    case NUMBER  : compareNumber( candidate, reference, path );
      break;
    case STRING  : compareString( candidate, reference, path );
      break;
    case NULL    : // Do nothing because we checked canType != refType already, so here both types are NULL.
      break;
    case BINARY  : // fall through
    case POJO    : // fall through
    case MISSING : // fall through
    default :
      throw new RuntimeException( "Node type " + canType.name() + " is not supported yet." );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterateArray( final JsonNode candidate, final JsonNode reference, final String path )
  {
    final int canLength = candidate.size();
    final int refLength = reference.size();
    final int length    = Math.min( canLength, refLength );

    // NEEDS FIX A: refactor this naive approach. Find matching array entries!
    // => IDEA: We are able to sort the array with tthe reposnseSortFilter with identifying the relevant sort criteria
    //          => We could try to use these criteria syntax to identify common entries (modified or unmodified)
    int i=0;
    while( i < length) {
       iterate( candidate.get( i ), reference.get( i ), getArrayPath( path, i ) );
       i++;
    }
    if( canLength < refLength ) {
      while( i < refLength ) {
        deletions_.add( new JsonDiffEntry( getArrayPath( path, i ), ""+canLength, ""+refLength, null, "Array entry was deleted." ) );
        i++;
      }
    }
    else if( canLength > refLength ) {
      while( i < canLength ) {
        additions_.add( new JsonDiffEntry( getArrayPath( path, i ), ""+canLength, ""+refLength, null, "Array entry was added." ) );
        i++;
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param path The path. Must not be null.
   * @param i The index.
   * @return "path[ i ]"
   */
  static String getArrayPath( final String path, final int i )
  {
    return new StringBuilder( path ).append( "[" ).append( i ).append( "]" ).toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterateMap( final JsonNode candidate, final JsonNode reference, final String path )
  {
    final Set< String > canKeys = getKeySet( candidate );
    final Set< String > refKeys = getKeySet( reference );
    final Set< String > common  = new TreeSet< String >( canKeys ); common.retainAll( refKeys );
    final Set< String > canOnly = new TreeSet< String >( canKeys ); canOnly.removeAll( common );
    final Set< String > refOnly = new TreeSet< String >( refKeys ); refOnly.removeAll( common );

    for( final String child : canOnly ) {
      additions_.add( new JsonDiffEntry( getMapPath( path, child ), child, null, null, "Map entry was added." ) );
    }
    for( final String child : refOnly ) {
      deletions_.add( new JsonDiffEntry( getMapPath( path, child ), null, child, null, "Map entry was deleted."  ) );
    }
    for( final String child : common ) {
      iterate( candidate.get( child ), reference.get( child ), getMapPath( path, child ) );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param path  The path. Must not be null.
   * @param child The child. Must not be null.
   * @return "path.child"
   */
  static String getMapPath( final String path, final String child )
  {
    return new StringBuilder( path ).append( "." ).append( child ).toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Set< String > getKeySet( final JsonNode jsonNode )
  {
    final Set< String > keys = new TreeSet<>();

    final Iterator< Map.Entry< String, JsonNode > > fields = jsonNode.properties().iterator();
    while(fields.hasNext()) {
      keys.add( fields.next().getKey() );
    }

    return keys;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void compareBoolean( final JsonNode candidate, final JsonNode reference, final String path )
  {
    final boolean can = candidate.asBoolean();
    final boolean ref = reference.asBoolean();

    if( can != ref ) {
      final StringBuilder sb = new StringBuilder()
       .append( "Boolean value expected: " ).append( ref )
       .append( " but was: " ).append( can );

      changes_.add( new JsonDiffEntry( path, ""+can, ""+ref, null, sb.toString() ) );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void compareNumber( final JsonNode candidate, final JsonNode reference, final String path )
  {
    final Number can = candidate.numberValue();
    final Number ref = reference.numberValue();

    if( Math.abs( can.doubleValue() - ref.doubleValue() ) >= epsilon_ ) {
      final StringBuilder sb = new StringBuilder()
       .append( "Number value expected: " ).append( ref )
       .append( " but was: " ).append( can );

      changes_.add( new JsonDiffEntry( path, ""+can, ""+ref, null, sb.toString() ) );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void compareString( final JsonNode candidate, final JsonNode reference, final String path )
  {
    final String can = trim_ ? candidate.asText().trim() : candidate.asText();
    final String ref = trim_ ? reference.asText().trim() : reference.asText();

    if( ignoreCase_
      ? !can.equalsIgnoreCase( ref )
      : !can.equals( ref )
    ) {
      final StringBuilder sb = new StringBuilder()
         .append( "String value expected: \"" ).append( ref )
         .append( "\" but was \"" ).append( can )
         .append( "\"" );

      changes_.add( new JsonDiffEntry( path, can, ref, null, sb.toString() ) );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder( "{" );

    sb.append("\"changes\":").append( changes_ )
      .append(", \"deletions\":" ).append( deletions_ )
      .append(", \"additions\":" ).append( additions_ )
      .append( "}" );

    return sb.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /*
  protected void logDifferences( final List< JsonDiffEntry > differences )
  {
    for( final JsonDiffEntry diff : differences ) {
      final String message = diff.getJsonPath() + " ( " + diff.getMessage() + " )";
      LOG.info( message );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void main( final String[] args )
  {
    final String candidate = "{\"a\":{\"d\":3.1415,\"e\":[\"z\",\"y\",\"x\"]},\"b\":\" TEXT \",\"c\":1,\"z\":\"new\"}";
    final String reference = "{\"a\":{\"d\":3.1416,\"e\":[\"a\",\"z\",\"y\",\"x\"]},\"b\":\"text\",\"c\":1,\"d\":2}";

    JsonDiff diff = null;
    try {
       diff = JsonDiff.createInstance()
          .setCandidate(candidate)
          .setReference(reference)
          .setEpsilon( 0.000001 )
          .setTrim( true )
          .setIgnoreCase( true )
          ;
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      System.exit( 1 );
    }

    final List< JsonDiffEntry > changes   = diff.getChanges();
    final List< JsonDiffEntry > deletions = diff.getDeletions();
    final List< JsonDiffEntry > additions = diff.getAdditions();

    diff.logDifferences( changes );
    diff.logDifferences( deletions );
    diff.logDifferences( additions );
  }
*/
}
