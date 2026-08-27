package com.github.kreutzr.responsediff.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class ErrorHandlingHelperTest extends TestRoot
{
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( ErrorHandlingHelper.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatCreateSingleLineMessageWorks()
  {
    final String exceptionMessage = "Test exception";
    final Throwable ex = new RuntimeException( exceptionMessage );
    final String message ="Test";
    final String linebreak = "###";

    String result = ErrorHandlingHelper.createSingleLineMessage( ex );
    assertThat( result ).startsWith( ErrorHandlingHelper.DEFAULT_MESSAGE ).contains( ": " + exceptionMessage );

    result = ErrorHandlingHelper.createSingleLineMessage( null );
    assertThat( result ).startsWith( ErrorHandlingHelper.DEFAULT_MESSAGE ).doesNotContain( ": " + exceptionMessage );

    result = ErrorHandlingHelper.createSingleLineMessage( null, ex );
    assertThat( result ).startsWith( ErrorHandlingHelper.DEFAULT_MESSAGE ).contains( ": " + exceptionMessage );

    result = ErrorHandlingHelper.createSingleLineMessage( message, ex );
    assertThat( result ).startsWith( message ).contains( ": " + exceptionMessage );

    result = ErrorHandlingHelper.createSingleLineMessage( message, ex, linebreak );
    assertThat( result ).startsWith( message ).contains( ": " + exceptionMessage ).contains( linebreak );

    result = ErrorHandlingHelper.createSingleLineMessage( message, ex, null );
    assertThat( result ).startsWith( message ).contains( ": " + exceptionMessage ).contains( ErrorHandlingHelper.LINE_BREAK_MASK );
  }
}