package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
public class JsonPathHelperTest
{
  @SuppressWarnings("unchecked")
  @Test
  public void testThatGetValueWorksForMapJson()
  {
    // Given
    final String json = "{ \"a\" : [ { \"b\" : \"B\", \"c\" : \"C\" }, { \"b\" : \"B\", \"c\" : \"X\" }, { \"b\" : \"B\", \"c\" : \"C\" } ] }";
    final JsonPathHelper jph = new JsonPathHelper( json );

    // When / Then
    try {
      assertThat( jph.getValue( "$.a[0].c" ) ).isEqualTo( "C" );
      assertThat( jph.getValue( "$.a[1].c" ) ).isEqualTo( "X" );
      assertThat( ( (List< Object >)jph.getValue( "$.a[*]" ) ) ).hasSize( 3 );

      // Know issue: Identical entries in "$.a" are identical objects (have the same hashCode).
//      assertThat( jph.getValue( "$.a[0]" ).hashCode() ).isNotEqualTo( jph.getValue( "$.a[2]" ).hashCode() );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @SuppressWarnings("unchecked")
  @Test
  public void testThatGetValueWorksForArrayJson()
  {
    // Given
    final String json = "[ { \"b\" : \"B\", \"c\" : \"C\" }, { \"b\" : \"B\", \"c\" : \"X\" }, { \"b\" : \"B\", \"c\" : \"C\" } ]";
    final JsonPathHelper jph = new JsonPathHelper( json );

    // When / Then
    try {
      assertThat( jph.getValue( "$[0].c" ) ).isEqualTo( "C" );
      assertThat( jph.getValue( "$[1].c" ) ).isEqualTo( "X" );
      assertThat( ( (List< Object >)jph.getValue( "$[*]" ) ) ).hasSize( 3 );

      // Know issue: Identical entries in "$[<index>]" are identical objects (have the same hashCode).
//      assertThat( jph.getValue( "$[0]" ).hashCode() ).isNotEqualTo( jph.getValue( "$[2]" ).hashCode() );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatGetValueWorksWithJsonPathFeatures()
  {
    // Given
    final String json = "{ \"a\" : [ { \"b\" : \"B\", \"c\" : \"C\" }, { \"b\" : \"B\", \"c\" : \"X\" }, { \"b\" : \"B\", \"c\" : \"C\" } ], \"numbers\" : [ 4, 3, 2, 1 ] }";
    final JsonPathHelper jph = new JsonPathHelper( json );

    // --------------------------------------------------------------------------------------------
    //
    // NOTE:
    // =====
    // JSONPath supports fetching values from a JSON structure, but does allow to check that two
    // paths point to the same data object.
    // This is the reason why ResponseDiff does not (yet) support the entire function set of
    // JSONPath.
    //
    // --------------------------------------------------------------------------------------------

    // When / Then
    try {
      assertThat( jph.getValue( "$..c" ) ).isEqualTo( List.of( "C", "X", "C" ) );
      assertThat( jph.getValue( "$.a.[?(@.b=='B')].c" ) ).isEqualTo( List.of( "C", "X", "C" ) );
      assertThat( jph.getValue( "$.a..[?(@.b=='B')].c" ) ).isEqualTo( List.of( "C", "X", "C" ) );
      assertThat( jph.getValue( "$..c" ) ).isEqualTo( List.of( "C", "X", "C" ) );
      assertThat( jph.getValue( "$.a.[:2].c" ) ).isEqualTo( List.of( "C", "X" ) );  // ":2" = indexes below 2 (exclusive)
      assertThat( jph.getValue( "$.a.[1:].c" ) ).isEqualTo( List.of( "X", "C" ) );  // "1:" = index 1 and following
      assertThat( jph.getValue( "$.a.[1:2].c" ) ).isEqualTo( List.of( "X" ) );      // "0:2" = exclusive 2

      // NOTE: Currently JSONPath does not allow access to an array that was read by filtering (see below)
      //       "$..c[0]" instead will return an empty array (as the filter examples ("... [?(@. ...)] ...") below.
      //       This is because [] for index access is not specified in the JSONPath spec by now (2023-11-15) .
      assertThat( jph.getValue( "$..c[0]"        ) ).isEqualTo( List.of() );
      assertThat( jph.getValue( "$.a.[1:2].c[0]" ) ).isEqualTo( List.of() );        // Unexpected behavior
      // => Test proprietary(!) index access
      assertThat( jph.getValue( "$..c#1"        ) ).isEqualTo( "X" );
      assertThat( jph.getValue( "$.a.[0:2].c#1" ) ).isEqualTo( "X" );



      assertThat( jph.getValue( "$.numbers.min()" ) ).isEqualTo( 1.0 );
      assertThat( jph.getValue( "$.numbers.max()" ) ).isEqualTo( 4.0 );
      assertThat( jph.getValue( "$.numbers.avg()" ) ).isEqualTo( 2.5 );
      assertThat( jph.getValue( "$.numbers.stddev()" ) ).isEqualTo( 1.118033988749895 );
      assertThat( jph.getValue( "$.numbers.length()" ) ).isEqualTo( 4 );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatHasPathWorks()
  {
    // Given
    final String json = "{ \"a\" : 1, \"b\": null, \"c\": \"null\" }";
    final JsonPathHelper jph = new JsonPathHelper( json );

    // When / Then
    assertThat( jph.hasPath( "$.a" ) ).isEqualTo( true );
    assertThat( jph.hasPath( "$.b" ) ).isEqualTo( true );
    assertThat( jph.hasPath( "$.c" ) ).isEqualTo( true );
    assertThat( jph.hasPath( "$.x" ) ).isEqualTo( false );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatIsNullWorks()
  {
    // Given
    final String json = "{ \"a\" : 1, \"b\": null, \"c\": \"null\" }";
    final JsonPathHelper jph = new JsonPathHelper( json );

    // When / Then
    assertThat( jph.isNull( "$.a" ) ).isEqualTo( false );
    assertThat( jph.isNull( "$.b" ) ).isEqualTo( true );
    assertThat( jph.isNull( "$.c" ) ).isEqualTo( false );
    assertThat( jph.isNull( "$.x" ) ).isEqualTo( true );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatContainsWorks()
  {
    // Given

    // When / Then
    try {
      assertThat( JsonPathHelper.contains( "$a",     "$.a"      ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$.a",    "$.a"      ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.a",    "$.az"     ) ).isFalse(); // Substrings must not be mixed (e.g. $.type must not test $.typeName)
      assertThat( JsonPathHelper.contains( "$.a",    "$.b"      ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$.a",    "$.a[0]"   ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.a",    "$.b[0]"   ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$.a",    "$.a.b"    ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.a",    "$.b.a"    ) ).isFalse();

      assertThat( JsonPathHelper.contains( "$*",     "$.a"      ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$*",     "$.a.b"    ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$*",     "$.a[0]"   ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$*",     "$.b"      ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$*",     "$[0].a"   ) ).isTrue();

      assertThat( JsonPathHelper.contains( "$.*",    "$.a"      ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.*",    "$.a[0]"   ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.*",    "$.b"      ) ).isTrue();

      assertThat( JsonPathHelper.contains( "$.*.a",  "$.a"      ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$.*.a",  "$.b.a"    ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.*.a",  "$.b.a.c"  ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.*.a",  "$.b[0].a" ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$.*.a",  "$.b"      ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$.*.a",  "$.b.c.a"  ) ).isFalse();

      assertThat( JsonPathHelper.contains( "$..a",   "$.a"      ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$..a",   "$.b.a"    ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$..a",   "$.b.a.c"  ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$..a",   "$.b.ab"   ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$..a",   "$.b[0].a" ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$..a",   "$.b"      ) ).isFalse();
      assertThat( JsonPathHelper.contains( "$..a",   "$.b.c.a"  ) ).isTrue();

      assertThat( JsonPathHelper.contains( "$[*].a", "$[5].a"       ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$[*].a", "$[5].az"      ) ).isFalse(); // Substrings must not be mixed (e.g. $.type must not test $.typeName)
      assertThat( JsonPathHelper.contains( "$[*].a", "$[5].a.b"     ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$[*].a", "$[5].a[4].b"  ) ).isTrue();
      assertThat( JsonPathHelper.contains( "$[*].a", "$[5].b[3].a"  ) ).isFalse();

      assertThat( JsonPathHelper.contains( "$[ * ].a", "$[ 5 ].a"   ) ).isTrue();
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable" );
    }
  }
}
