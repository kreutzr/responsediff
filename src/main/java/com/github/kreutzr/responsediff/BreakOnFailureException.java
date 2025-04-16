package com.github.kreutzr.responsediff;

/**
 * This Exception is thrown if a test failed and the following test execution shall be skipped.
 */
public class BreakOnFailureException extends RuntimeException
{
  private static final long serialVersionUID = 6969574502499156772L;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public BreakOnFailureException( final String message )
  {
    super( message );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public BreakOnFailureException( final String message, final Throwable ex )
  {
    super( message, ex );
  }
}
