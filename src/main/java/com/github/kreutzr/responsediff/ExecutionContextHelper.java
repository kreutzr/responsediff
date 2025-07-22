package com.github.kreutzr.responsediff;

import java.util.Set;

import org.slf4j.Logger;

/**
 * Description of the context in which a test is performed.
 */
public class ExecutionContextHelper
{
  public static final String CHECK_CONTEXT__TEST_EXPECTATION = "test expectation";
  public static final String CHECK_CONTEXT__TEST             = "test";
  public static final String CHECK_CONTEXT__TESTSET          = "testset";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the execution context applies to the expectation condition. Matches by default if no expectation condition is defined.
   * @param executionContextKeys The contexts for which an expectation must be considered. May be null.
   * @param executionContext The execution context. Must not be null.
   * @param checkContext The current check context. Must not be null.
   * @param LOG The Logger to use. Must not be null.
   * @return true no executionContextKeys are defined (empty or null) or at least one of the given context keys can be found within the given execution context. Otherwise false is returned.
   */
  public static boolean matchesExecutionContext(
    final String executionContextKeys,
    final Set< String > executionContext,
    final String checkContext,
    final Logger LOG
  )
  {
    if( executionContextKeys == null || executionContextKeys.isBlank() ) {
      // No execution context restriction set => Matches by default
      return true;
    }

    final String[] parts = executionContextKeys.split( "," );
    for( final String part : parts ) {
      if( executionContext.contains( part.trim().toLowerCase() ) ) {
        // Required context key matches given context
        return true;
      }
    }

    // No required context key matches given context
    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Required execution context (\"" + executionContextKeys + "\") is not met (" + executionContext.toString() + "). Skipping " + checkContext + "." );
    }
    return false;
  }
}
