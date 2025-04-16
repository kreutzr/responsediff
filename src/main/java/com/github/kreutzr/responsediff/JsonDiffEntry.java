package com.github.kreutzr.responsediff;

/**
 * This class holds the information of on found difference of a Json comparison.
 */
public class JsonDiffEntry implements Comparable< JsonDiffEntry >
{
  private final String      jsonPath_;
  private final String      actual_;
  private final String      expected_;
  private       String      message_;
  private       XmlLogLevel logLevel_;

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public JsonDiffEntry(
      final String jsonPath,
      final String actual,
      final String expected,
      final String message
  )
  {
    jsonPath_ = jsonPath;
    actual_   = actual;
    expected_ = expected;
    message_  = message;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getJsonPath()
  {
    return jsonPath_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getActual()
  {
    return actual_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getExpected()
  {
    return expected_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getMessage()
  {
    return message_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setMessage( final String message )
  {
    message_ = message;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public XmlLogLevel getLogLevel()
  {
    return logLevel_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setLogLevel( final XmlLogLevel logLevel )
  {
    logLevel_ = logLevel;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public int compareTo( final JsonDiffEntry other )
  {
    if( other == null ) {
      return 1;
    }
    return jsonPath_.compareTo( other.jsonPath_ );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder( "{" );

    sb.append( "\"jsonPath\":\""   ).append( jsonPath_ )
      .append( "\", \"expected\":\"" ).append( expected_ )
      .append( "\", \"actual\":\""   ).append( actual_ )
      .append( "\"}" );

    return sb.toString();
  }
}
