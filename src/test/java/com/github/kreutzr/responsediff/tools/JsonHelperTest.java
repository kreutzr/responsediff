package com.github.kreutzr.responsediff.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class JsonHelperTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( JsonHelper.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}