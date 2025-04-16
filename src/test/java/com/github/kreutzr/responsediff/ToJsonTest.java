package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.ToJson;

import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;

public class ToJsonTest
{
  private XmlHeader createXmlHeader( final String name, final String value )
  {
    final XmlHeader xmlHeader = new XmlHeader();
    xmlHeader.setName ( name );
    xmlHeader.setValue( value );
    return xmlHeader;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatHeadersAreConvertedCorrectly()
  {
    // Given
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( createXmlHeader( "header-name", "header-value" ) );
    xmlHeaders.getHeader().add( createXmlHeader( "etag", "my-etag" ) );

    // When
    final String result = ToJson.fromHeaders( xmlHeaders, false );

    // Then
    assertThat( result ).isEqualTo( "\"headers\":{\"header-name\":\"header-value\",\"etag\":\"my-etag\"}" );
  }
}
