package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class ConstantsTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( Constants.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}
