package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutionContextHelperTest
{
  private static final Logger LOG = LoggerFactory.getLogger( ExecutionContextHelperTest.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testMatchesExecutionContextWorks()
  {
    // Given
    final Set< String > executionContext = new TreeSet<>();
    executionContext.add( "releasing, some-TOKEN" );

    // When / Then
    try {
      // Positive check: list
      assertThat( ExecutionContextHelper.matchesExecutionContext( "AAA, releasing, BBB", executionContext, "test", LOG ) ).isTrue();
      // Positive check: case insensitive
      assertThat( ExecutionContextHelper.matchesExecutionContext( "some-token",          executionContext, "test", LOG ) ).isTrue();
      // Negative check
      assertThat( ExecutionContextHelper.matchesExecutionContext( "HELLO",               executionContext, "test", LOG ) ).isFalse();
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable" );
    }
  }
}
