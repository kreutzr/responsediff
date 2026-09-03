package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class RangeParserTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( RangeParser.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatParseWorks()
  {
    // When / Then
    String test = null;
    Range range = RangeParser.parse( test );
    assertThat( range ).isNull();

    // When / Then
    test = " [ X    ,    y     ] ";
    range = RangeParser.parse( test );
    assertThat( range.getLowerBorder().getValue() ).isEqualTo( "X" );
    assertThat( range.getLowerBorder().getType() ).isEqualTo( RangeType.INCLUSIVE );
    assertThat( range.getUpperBorder().getValue() ).isEqualTo( "y" );
    assertThat( range.getUpperBorder().getType() ).isEqualTo( RangeType.INCLUSIVE );

    // When / Then
    test = "[ABC,XYZ[";
    range = RangeParser.parse( test );
    assertThat( range.getLowerBorder().getValue() ).isEqualTo( "ABC" );
    assertThat( range.getLowerBorder().getType() ).isEqualTo( RangeType.INCLUSIVE );
    assertThat( range.getUpperBorder().getValue() ).isEqualTo( "XYZ" );
    assertThat( range.getUpperBorder().getType() ).isEqualTo( RangeType.EXCLUSIVE );

    // When / Then
    test = "]1,5]";
    range = RangeParser.parse( test );
    assertThat( range.getLowerBorder().getValue() ).isEqualTo( "1" );
    assertThat( range.getLowerBorder().getType() ).isEqualTo( RangeType.EXCLUSIVE );
    assertThat( range.getUpperBorder().getValue() ).isEqualTo( "5" );
    assertThat( range.getUpperBorder().getType() ).isEqualTo( RangeType.INCLUSIVE );

    // When / Then
    test = "]3.14,5.67[";
    range = RangeParser.parse( test );
    assertThat( range.getLowerBorder().getValue() ).isEqualTo( "3.14" );
    assertThat( range.getLowerBorder().getType() ).isEqualTo( RangeType.EXCLUSIVE );
    assertThat( range.getUpperBorder().getValue() ).isEqualTo( "5.67" );
    assertThat( range.getUpperBorder().getType() ).isEqualTo( RangeType.EXCLUSIVE );

    // When / Then
    test = "[1/2]";
    range = RangeParser.parse( test );
    assertThat( range ).isNull();

    // When / Then
    test = "#1,2]";
    range = RangeParser.parse( test );
    assertThat( range ).isNull();

    // When / Then
    test = "]1,2ä";
    range = RangeParser.parse( test );
    assertThat( range ).isNull();

    // When / Then
    test = "1,2";
    range = RangeParser.parse( test );
    assertThat( range ).isNull();
  }
}
