package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class JsonTraverserTest
{
  private class _JsonTraverserTestListener implements JsonTraverserVisitor
  {
    private List< String > pathes_ = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void notify( final JsonNode node, final JsonNodeType parentNodeType, final String jsonPath )
    {
//      System.out.println( jsonPath );
      pathes_.add( jsonPath );
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List< String > getPathes()
    {
      return pathes_;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatTraverseWorksForMaps()
  {
    try {
      // Given
      final String json = "{ \"a\": [ { \"x\": 2 }, { \"x\": 2 } ], \"b\": { \"x\": 2 }, \"c\":3, \"x\": 2 }";
      final _JsonTraverserTestListener listener = new _JsonTraverserTestListener();
      final JsonTraverser traverser = new JsonTraverser( json );

      // When
      traverser
        .addValueVisitor( listener )
        .traverse();

      // Then
      assertThat( listener.getPathes().size() ).isEqualTo( 5 );
      assertThat( listener.getPathes().get( 0 ) ).isEqualTo( "$.a[0].x" );
      assertThat( listener.getPathes().get( 1 ) ).isEqualTo( "$.a[1].x" );
      assertThat( listener.getPathes().get( 2 ) ).isEqualTo( "$.b.x" );
      assertThat( listener.getPathes().get( 3 ) ).isEqualTo( "$.c" );
      assertThat( listener.getPathes().get( 4 ) ).isEqualTo( "$.x" );
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}
