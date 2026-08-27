package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class JsonDiffEntryTest extends TestRoot
{
  public class JsonDiffTest extends TestRoot
  {
    @Test
    public void testThatConstructorWorks()
    {
      try {
        testThatPrivateConstructorWorks( JsonDiffEntry.class );
      }
      catch( final Exception ex )
      {
        ex.printStackTrace();
        assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatComparisonWithNullWorks()
   {
     // Given
	 final JsonDiffEntry jsonDiffEntry = new JsonDiffEntry("a", "b", "c", "d", "e" );
	
     // When / Then
     assertThat( jsonDiffEntry.compareTo( null ) ).isEqualTo( 1 );
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   // NOTE: The other methods are already covered by other tests (see JaCoCo report)
}
