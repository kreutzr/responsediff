package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class XmlFileHandlerTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( XmlFileHandler.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatExpandSetupByIterationsWorks()
  {
    // Given
    final XmlResponseDiffSetup xmlSetup = new XmlResponseDiffSetup();
    xmlSetup.setId( "Setup" );

    final XmlTestSet xmlTestSetOuter = new XmlTestSet();
    xmlTestSetOuter.setId( "OuterTS" );
    xmlSetup.getTestSet().add( xmlTestSetOuter );

    final XmlTestSet xmlTestSetInner = new XmlTestSet();
    xmlTestSetInner.setId( "InnerTS" );
    xmlTestSetInner.setIterations( 2 );
    xmlTestSetOuter.getTestSet().add( xmlTestSetInner );

    final XmlTest xmlTestInner1 = new XmlTest();
    xmlTestInner1.setId( "InnerTest1" );
    xmlTestSetInner.getTest().add( xmlTestInner1 );
    final XmlTest xmlTestInner2 = new XmlTest();
    xmlTestInner2.setId( "InnerTest2" );
    xmlTestInner2.setIterations( 3 );
    xmlTestSetInner.getTest().add( xmlTestInner2 );

    final XmlTest xmlTestOuter1 = new XmlTest();
    xmlTestOuter1.setId( "OuterTest1" );
    xmlTestOuter1.setIterations( 4 );
    xmlTestSetOuter.getTest().add( xmlTestOuter1 );
    final XmlTest xmlTestOuter2 = new XmlTest();
    xmlTestOuter2.setId( "OuterTest2" );
    xmlTestSetOuter.getTest().add( xmlTestOuter2 );

    // When
    XmlFileHandler.expandSetupByIterations( xmlSetup );

    // Then
    assertThat( xmlSetup.getTestSet() ).hasSize( 1 );
    final XmlTestSet outerTS = xmlSetup.getTestSet().get( 0 );
    assertThat( outerTS.getId() ).isEqualTo( "OuterTS" );
    assertThat( outerTS.getTestSet() ).hasSize( 2 );
    assertThat( outerTS.getTest()    ).hasSize( 1 );
    final XmlTestSet innerTS_wrapper_2 = outerTS.getTestSet().get( 0 );
    final XmlTestSet innerTS_wrapper_4 = outerTS.getTestSet().get( 1 );

    assertThat( innerTS_wrapper_2.getId() ).isEqualTo( "InnerTS" );
    assertThat( innerTS_wrapper_2.getDescription() ).isEqualTo( "IterationWrapper( 2 )" );
    assertThat( innerTS_wrapper_2.getTestSet() ).hasSize( 2 );
    assertThat( innerTS_wrapper_2.getTest() ).isEmpty();

    assertThat( innerTS_wrapper_4.getId() ).isEqualTo( "OuterTS" );
    assertThat( innerTS_wrapper_4.getDescription() ).isEqualTo( "IterationWrapper( 4 )" );
    assertThat( innerTS_wrapper_4.getTestSet() ).isEmpty();
    assertThat( innerTS_wrapper_4.getTest() ).hasSize( 4 );
    assertThat( innerTS_wrapper_4.getTest().get( 0 ).getId() ).isEqualTo( "OuterTest1" );
    assertThat( innerTS_wrapper_4.getTest().get( 1 ).getId() ).isEqualTo( "OuterTest1" );
    assertThat( innerTS_wrapper_4.getTest().get( 2 ).getId() ).isEqualTo( "OuterTest1" );
    assertThat( innerTS_wrapper_4.getTest().get( 3 ).getId() ).isEqualTo( "OuterTest1" );

    assertThat( outerTS.getTest().get( 0 ).getId() ).isEqualTo( "OuterTest2" );

    {
      final XmlTestSet innerTS_A = innerTS_wrapper_2.getTestSet().get( 0 );

      assertThat( innerTS_A.getId() ).isEqualTo( "InnerTS" );
      assertThat( innerTS_A.getDescription() ).isNull();
      assertThat( innerTS_A.getTestSet() ).hasSize( 1 );
      assertThat( innerTS_A.getTest() ).hasSize( 1 );

      final XmlTestSet innerTest2_wrapper_3_A = innerTS_A.getTestSet().get( 0 );
      final XmlTest    innerTest1_A           = innerTS_A.getTest().get( 0 );

      assertThat( innerTest2_wrapper_3_A.getId() ).isEqualTo( "InnerTS" );
      assertThat( innerTest2_wrapper_3_A.getDescription() ).isEqualTo( "IterationWrapper( 3 )" );
      assertThat( innerTest2_wrapper_3_A.getTestSet() ).isEmpty();
      assertThat( innerTest2_wrapper_3_A.getTest() ).hasSize( 3 );
      assertThat( innerTest2_wrapper_3_A.getTest().get( 0 ).getId() ).isEqualTo( "InnerTest2" );
      assertThat( innerTest2_wrapper_3_A.getTest().get( 1 ).getId() ).isEqualTo( "InnerTest2" );
      assertThat( innerTest2_wrapper_3_A.getTest().get( 2 ).getId() ).isEqualTo( "InnerTest2" );

      assertThat( innerTest1_A.getId() ).isEqualTo( "InnerTest1" );
    }
    {
      final XmlTestSet innerTS_B = innerTS_wrapper_2.getTestSet().get( 1 );

      assertThat( innerTS_B.getId() ).isEqualTo( "InnerTS" );
      assertThat( innerTS_B.getDescription() ).isNull();
      assertThat( innerTS_B.getTestSet() ).hasSize( 1 );
      assertThat( innerTS_B.getTest() ).hasSize( 1 );

      final XmlTestSet innerTest2_wrapper_3_B = innerTS_B.getTestSet().get( 0 );
      final XmlTest innerTest1_B = innerTS_B.getTest().get( 0 );

      assertThat( innerTest2_wrapper_3_B.getId() ).isEqualTo( "InnerTS" );
      assertThat( innerTest2_wrapper_3_B.getDescription() ).isEqualTo( "IterationWrapper( 3 )" );
      assertThat( innerTest2_wrapper_3_B.getTestSet() ).isEmpty();
      assertThat( innerTest2_wrapper_3_B.getTest() ).hasSize( 3 );
      assertThat( innerTest2_wrapper_3_B.getTest().get( 0 ).getId() ).isEqualTo( "InnerTest2" );
      assertThat( innerTest2_wrapper_3_B.getTest().get( 1 ).getId() ).isEqualTo( "InnerTest2" );
      assertThat( innerTest2_wrapper_3_B.getTest().get( 2 ).getId() ).isEqualTo( "InnerTest2" );

      assertThat( innerTest1_B.getId() ).isEqualTo( "InnerTest1" );
    }
  }
}