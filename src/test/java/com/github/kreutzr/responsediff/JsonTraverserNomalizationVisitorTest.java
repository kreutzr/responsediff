package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class JsonTraverserNomalizationVisitorTest
{
  @Test
  public void testThatMapNormalizationWorks()
  {
    try {
      // Given
      final String json = "{ \"license.id\" : \"MIT\", \"project\" : { \"a\" : 2 }, \"project.id\": \"123\", \"project.name\": \"test\", \"some-key\" : 1, \"array\" : [ { \"not.affected\" : \"true\" } ] }";
      final boolean normalizeMaps   = true;
      final boolean normalizeArrays = false;
      final JsonTraverserNormalizationVisitor visitor = new JsonTraverserNormalizationVisitor( normalizeMaps, normalizeArrays );
      final JsonTraverser traverser = new JsonTraverser( json );

      // When
      final JsonNode root = traverser
        .addStructureVisitor( visitor )
        .traverse()
        .getRoot();

//      System.out.println( root.toString() );

      // Then
      // Substructure "license" does not exist at start
      assertThat( root.get( "license.id" ) ).isNull();
      assertThat( root.get( "license" ).get( "id" ).asText() ).isEqualTo( "MIT" );

      // Substructure "project" does exist at start
      assertThat( root.get( "project.id"   ) ).isNull();
      assertThat( root.get( "project.name" ) ).isNull();
      assertThat( root.get( "project" ).getNodeType() ).isEqualTo( JsonNodeType.OBJECT );
      assertThat( root.get( "project" ).get( "a"    ).asInt() ).isEqualTo( 2 );
      assertThat( root.get( "project" ).get( "id"   ).asText() ).isEqualTo( "123" );
      assertThat( root.get( "project" ).get( "name" ).asText() ).isEqualTo( "test" );

      // Substructure "some-key" must not be affected
      assertThat( root.get( "some-key" ).asInt() ).isEqualTo( 1 );

      // Array entries must not be affected
      assertThat( root.get( "array" ).get( 0 ).get( "not.affected" ).asText() ).isEqualTo( "true" );
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatMapNormalizationConflictsAreDetected()
  {
    try {
      // Given
      final String json = "{ \"project\" : { \"id\" : \"123\" }, \"project.id.suffix\": \"-SNAPSHOT\", \"array\": [ \"test\" ], \"array.length\" : 1 }";
      final boolean normalizeMaps   = true;
      final boolean normalizeArrays = false;
      final JsonTraverserNormalizationVisitor visitor = new JsonTraverserNormalizationVisitor( normalizeMaps, normalizeArrays );
      final JsonTraverser traverser = new JsonTraverser( json );

      // When
      final JsonNode root = traverser
        .addStructureVisitor( visitor )
        .traverse()
        .getRoot();

//      System.out.println( root.toString() );

      // Then
      // Substructure "project.id" does exist as text at start => id can not be converted to map
      assertThat( root.get( "project" ).getNodeType() ).isEqualTo( JsonNodeType.OBJECT );
      assertThat( root.get( "project" ).get( "id"   ).asText() ).isEqualTo( "123" );
      assertThat( root.get( "project" ).get( "id.suffix" ).asText() ).isEqualTo( "-SNAPSHOT" );

      // Substructure "array" does exist as array at start => array can not be converted to map
      assertThat( root.get( "array" ).getNodeType() ).isEqualTo( JsonNodeType.ARRAY );
      assertThat( root.get( "array.length" ).asInt() ).isEqualTo( 1 );
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatArrayNormalizationWorks()
  {
    try {
      // Given
      final String json = "{ \"array\" : [ { \"is.affected\" : \"true\" } ] }";
      final boolean normalizeMaps   = true;
      final boolean normalizeArrays = true;
      final JsonTraverserNormalizationVisitor visitor = new JsonTraverserNormalizationVisitor( normalizeMaps, normalizeArrays );
      final JsonTraverser traverser = new JsonTraverser( json );

      // When
      final JsonNode root = traverser
        .addStructureVisitor( visitor )
        .traverse()
        .getRoot();

//      System.out.println( root.toString() );

      // Then
      // Array entries are affected
      assertThat( root.get( "array" ).get( 0 ).get( "is" ).get( "affected" ).asText() ).isEqualTo( "true" );
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}
