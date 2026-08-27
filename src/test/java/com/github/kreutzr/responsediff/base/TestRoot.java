package com.github.kreutzr.responsediff.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.github.kreutzr.responsediff.tools.ComparatorHelper;

/**
 * Root class for tests that introduces LOG-Level support and a generic constructor test for better code coverage.
 */
public class TestRoot 
{
  @BeforeEach
  void setUp() {
    Configurator.setLevel( ComparatorHelper.class.getName(), Level.TRACE );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @AfterEach
  void tearDown() {
    Configurator.setLevel( ComparatorHelper.class.getName(), Level.INFO );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public <T> void testThatPublicConstructorWorks( final Class< T > objectUndertestClass )
  throws Exception
  {
    final Constructor< T > constructor = objectUndertestClass.getDeclaredConstructor();
    assertThat( Modifier.isPrivate( constructor.getModifiers() ) ).isEqualTo( false );
    constructor.setAccessible( true );
    constructor.newInstance();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public <T> void testThatPrivateConstructorWorks( final Class< T > objectUndertestClass )
  throws Exception
  {
    final Constructor< T > constructor = objectUndertestClass.getDeclaredConstructor();
    assertThat( Modifier.isPrivate( constructor.getModifiers() ) ).isEqualTo( true );
    constructor.setAccessible( true );
    constructor.newInstance();
  }
}
