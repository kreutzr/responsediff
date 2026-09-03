package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;
import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.filter.DummyResponseFilter;
import com.github.kreutzr.responsediff.filter.request.RemoveHeaderRequestFilter;

public class FilterRegistryHelperTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( FilterRegistryHelper.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatGetFilterRegistryWorks()
  {
    final XmlResponseDiffSetup xmlTestSetup = new XmlResponseDiffSetup();
    final String testSetupPath = null;

    final XmlFilterRegistry setupRegistry = new XmlFilterRegistry();
    xmlTestSetup.setFilterRegistry( setupRegistry );

    final XmlTestSet xmlTestSetOuter = new XmlTestSet(); // No filter registry here
    xmlTestSetOuter.setFilePath( "setup/outer/" );
    xmlTestSetOuter.setFileName( "outer.xml" );
    xmlTestSetup.getTestSet().add( xmlTestSetOuter );

    final XmlTestSet xmlTestSetInner = new XmlTestSet();
    xmlTestSetInner.setFilePath( "setup/outer/inner/" );
    xmlTestSetInner.setFileName( "inner.xml" );
    final XmlFilterRegistry innerRegistry = new XmlFilterRegistry();
    xmlTestSetInner.setFilterRegistry( innerRegistry );
    xmlTestSetOuter.getTestSet().add( xmlTestSetInner );

    final XmlFilterRegistryEntry entry1 = new XmlFilterRegistryEntry();
    entry1.setId( "dummyResponse" );
    entry1.setClazz( DummyResponseFilter.class.getName() );
    setupRegistry.getFilter().add( entry1 );

    final XmlFilterRegistryEntry entry2 = new XmlFilterRegistryEntry();
    entry2.setId( "removeHeaders" );
    entry2.setClazz( RemoveHeaderRequestFilter.class.getName() );
    innerRegistry.getFilter().add( entry2 );

    try {
      final Map< String, DiffFilter > registryMap = FilterRegistryHelper.getFilterRegistry( xmlTestSetup, testSetupPath );
      assertThat( registryMap ).isNotNull();
      assertThat( registryMap.size() ).isEqualTo( 2 );
      assertThat( registryMap.get( "dummyResponse"  ).getClass() ).isEqualTo( DummyResponseFilter.class );
      assertThat( registryMap.get( "removeHeaders" ).getClass() ).isEqualTo( RemoveHeaderRequestFilter.class );
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}