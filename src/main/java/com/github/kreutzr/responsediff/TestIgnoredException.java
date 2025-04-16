package com.github.kreutzr.responsediff;

/**
 * This Exception is thrown if a test id does not match the given test name pattern.
 */
public class TestIgnoredException extends RuntimeException
{
  private static final long serialVersionUID = 4333223029857615708L;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public TestIgnoredException( final String message )
  {
    super( message );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public TestIgnoredException( final String message, final Throwable ex )
  {
    super( message, ex );
  }
}
