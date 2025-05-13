package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class XmlToJsonTest
{
  private static final Logger LOG = LoggerFactory.getLogger( XmlToJsonTest.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  @Test
  public void testThatSimplificationWork()
  {
    // Given
    final String xml = "<a type=\"A\"><b name=\"B\">bbb</b><c>ccc</c></a>";

    try {
      // When
      final XmlToJson xmlToJson = new XmlToJson();
      xmlToJson.setPreserveOrder( true );
      xmlToJson.setSkipAttributes( false );
      String json = xmlToJson.toJson( xml );
      LOG.debug( "json (true/false)=" + json );

      // Then
      DocumentContext context = JsonPath.parse( json );
      assertThat( (String) context.read( "$.a.#value[ 0 ].b.#value" ) ).isEqualTo( "bbb" );
      assertThat( (String) context.read( "$.a.#value[ 1 ].c.#value" ) ).isEqualTo( "ccc" );

      // When
      xmlToJson.setPreserveOrder( false ); // Activate simplification
      xmlToJson.setSkipAttributes( false );
      json = xmlToJson.toJson( xml );
      LOG.debug( "json (false/false)=" + json );

      // Then
      context = JsonPath.parse( json );
      assertThat( (String) context.read( "$.a.#value.b.#value" ) ).isEqualTo( "bbb" );
      assertThat( (String) context.read( "$.a.#value.c.#value" ) ).isEqualTo( "ccc" );


      // When
      xmlToJson.setPreserveOrder( true );
      xmlToJson.setSkipAttributes( true ); // Deactivate attribute handling
      json = xmlToJson.toJson( xml );
      LOG.debug( "json (true/true)=" + json );

      // Then
      context = JsonPath.parse( json );
      assertThat( (String) context.read( "$.a[ 0 ].b" ) ).isEqualTo( "bbb" );
      assertThat( (String) context.read( "$.a[ 1 ].c" ) ).isEqualTo( "ccc" );

      // When
      xmlToJson.setPreserveOrder( false ); // Activate simplification
      xmlToJson.setSkipAttributes( true ); // Deactivate attribute handling
      json = xmlToJson.toJson( xml );
      LOG.debug( "json (false/true)=" + json );

      // Then
      context = JsonPath.parse( json );
      assertThat( (String) context.read( "$.a.b" ) ).isEqualTo( "bbb" );
      assertThat( (String) context.read( "$.a.c" ) ).isEqualTo( "ccc" );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable", ex );
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatXmlAttributesAreHandled()
  {
    // Given
    final String xml = "<a type=\"A\"><b name=\"B\">bbb</b><c>ccc</c></a>";

    try {
      // When
      final XmlToJson xmlToJson = new XmlToJson();
      xmlToJson.setPreserveOrder( true );
      xmlToJson.setSkipAttributes( false ); // This is required here
      final String json = xmlToJson.toJson( xml );
      LOG.debug( "json=" + json );
      // Then
      final DocumentContext context = JsonPath.parse( json );
      assertThat( (String) context.read( "$.a.@type" ) ).isEqualTo( "A" );
      assertThat( (String) context.read( "$.a.#value[ 0 ].b.@name" ) ).isEqualTo( "B" );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable", ex );
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatNativeDataTypesAreHandled()
  {
    // Given
    final String xml = "<data><int>5</int><double>2.3</double><bool>true</bool><string>text</string></data>";

    try {
      // When
      final XmlToJson xmlToJson = new XmlToJson();
      xmlToJson.setPreserveOrder( false );
      xmlToJson.setSkipAttributes( true );
      final String json = xmlToJson.toJson( xml );
      LOG.debug( "json=" + json );
      // Then
      final DocumentContext context = JsonPath.parse( json );
      assertThat( (int)     context.read( "$.data.int"    ) ).isEqualTo( 5 );
      assertThat( (double)  context.read( "$.data.double" ) ).isEqualTo( 2.3 );
      assertThat( (boolean) context.read( "$.data.bool"   ) ).isEqualTo( true );
      assertThat( (String)  context.read( "$.data.string" ) ).isEqualTo( "text" );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      fail( "Unreachable", ex );
    }
  }
}
