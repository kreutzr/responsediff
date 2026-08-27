package com.github.kreutzr.responsediff;

import com.github.kreutzr.responsediff.tools.Generated;

/**
 * This Exception is thrown if a test failed and the following test execution shall be skipped.
 */
@Generated
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
