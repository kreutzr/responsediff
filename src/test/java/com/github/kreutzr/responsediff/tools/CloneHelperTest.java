package com.github.kreutzr.responsediff.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class CloneHelperTest extends TestRoot
{
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( CloneHelper.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatDeepCopyJAXBWorks()
  {
    final BrokenObject object    = new BrokenObject();
    final BrokenObject parent = new BrokenObject();
    object.setParent( parent );
    parent.setValue( "parent" );
    object.setValue( "object" );

    final BrokenObject result = CloneHelper.deepCopyJAXB( object, BrokenObject.class );
    assertThat( result.getValue() ).isEqualTo( "object" );
    assertThat( result.getParent().getValue() ).isEqualTo( "parent" );
    assertThat( result.hashCode() ).isNotEqualTo( object.hashCode() );
  }
}