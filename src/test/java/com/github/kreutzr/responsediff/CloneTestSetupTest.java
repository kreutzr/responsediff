package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CloneTestSetupTest
{
  @Test
  public void testThatIgnoreTagsAreCommentedOut()
  {
    // Given
    final String c1 = "<!-- ### ";
    final String c2 = " ### -->";
    final String comment1  = "<!-- ccc -->";
    final String comment2a = "<!-- fff -->";
    final String comment2b = "<!-x- fff -x->";
    final String part1  = "<ignore a=\"a\" forEver=\"true\" b=\"b\">" + comment1 + "abc</ignore>";
    final String part2  = "<ignore >ddd</ignore >";
    final String part3  = "<ignore >eee</ignore >";
    final String part4a = "<ignore forEver=\"false\">" + comment2a + "</ignore>";
    final String part4b = "<ignore forEver=\"false\">" + comment2b + "</ignore>";
    final String xml = part1 + "111" + part2 + "222" + part3 + "333" + part4a;

    // When / Then
    String result = CloneTestSetup.handleXml( xml, true );
//    LOG.debug( result );
    assertThat( result ).isEqualTo( part1 + "111" + c1 + part2 + c2 + "222" + c1 + part3 + c2 + "333" + c1+ part4b + c2 );

    // When / Then
    result = CloneTestSetup.handleXml( xml, false );
//    LOG.debug( result );
     assertThat( result ).isEqualTo( xml );
  }
}
