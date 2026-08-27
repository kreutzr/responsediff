package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class ToJsonTest extends TestRoot
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
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( ToJson.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromVariableWorks()
  {
    // Given
    final XmlVariable xmlVariable = new XmlVariable();
    xmlVariable.setConfigured( true );
    xmlVariable.setId( "id" );
    xmlVariable.setPath( "path" );
    xmlVariable.setType( XmlValueType.STRING );
    xmlVariable.setValue( "value" );

    // When / Then
    String result = ToJson.fromVariable( xmlVariable );
    assertThat( result ).isEqualTo( "{\"id\":\"id\",\"path\":\"path\",\"value\":\"value\"}" );

    result = ToJson.fromVariable( null );
    assertThat( result ).isEqualTo( "null" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromAnalysisWorks()
  {
    // Given
    final XmlMessage xmlMessage1 = new XmlMessage();
    xmlMessage1.setExecutionContextConstraint( "some constraint" );
    xmlMessage1.setPath( "some path" );
    xmlMessage1.setValue( "some value" );
    xmlMessage1.setLevel( XmlLogLevel.TRACE );
    final XmlMessage xmlMessage2 = new XmlMessage();
    xmlMessage2.setExecutionContextConstraint( "other constraint" );
    xmlMessage2.setPath( "other path" );
    xmlMessage2.setValue( "other value" );
    final XmlMessages xmlMessages = new XmlMessages();
    xmlMessages.getMessage().add( xmlMessage1 );
    xmlMessages.getMessage().add( xmlMessage2 );
    xmlMessages.getMessage().add( null );
    final XmlAnalysis xmlAnalysis = new XmlAnalysis();
    xmlAnalysis.setBegin( "2026-08-27T20:28:00");
    xmlAnalysis.setEnd( "2026-08-27T20:29:00");
    xmlAnalysis.setDuration( "PT1M" );
    xmlAnalysis.setSuccessCount(1);
    xmlAnalysis.setFailCount( 0 );
    xmlAnalysis.setSkipCount( 2 );
    xmlAnalysis.setTotalCount( 3 );
    xmlAnalysis.setMessages( xmlMessages );

    // When / Then
    String result = ToJson.fromAnalysis( xmlAnalysis );
    assertThat( result ).isEqualTo( "{\"begin\":\"2026-08-27T20:28:00\",\"end\":\"2026-08-27T20:29:00\",\"duration\":\"PT1M\",\"successCount\":1,\"failCount\":0,\"skipCount\":2,\"totalCount\":3,\"messages\":[{\"level\":\"TRACE\",\"path\":\"some path\",\"value\":\"some value\",\"executionConstraint\":\"some constraint\"},{\"level\":\"UNKNOWN\",\"path\":\"other path\",\"value\":\"other value\",\"executionConstraint\":\"other constraint\"},null]}");

    result = ToJson.fromAnalysis( null );
    assertThat( result ).isEqualTo( "null" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromHeadersWorks()
  {
    // Given
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( createXmlHeader( "header-name", "header-value" ) );
    xmlHeaders.getHeader().add( createXmlHeader( "etag", "my-etag" ) );

    // When / Then
    String result = ToJson.fromHeaders( xmlHeaders, false, null );
    assertThat( result ).isEqualTo( "\"headers\":{\"header-name\":\"header-value\",\"etag\":\"my-etag\"}" );

    result = ToJson.fromHeaders( null, true, null );
    assertThat( result ).isEqualTo( "{\"headers\":null}" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromXmlValueWorks()
  {
    // Given
    final XmlValue xmlValue = new XmlValue();
    xmlValue.setEpsilon( "some epsilon");
    xmlValue.setPath( "some path");
    xmlValue.setType( XmlValueType.INT );
    xmlValue.setValue( "1" );
    xmlValue.setTicketReference( "some ticket reference" );
    xmlValue.setIfExecutionContextContains( "some if constraint");

    // When / Then
    String result = ToJson.fromXmlValue( xmlValue );
    assertThat( result ).isEqualTo( "{\"epsilon\":\"some epsilon\",\"path\":\"some path\",\"type\":\"INT\",\"value\":\"1\"}" );
//    assertThat( result ).isEqualTo( "{\"epsilon\":\"some epsilon\",\"path\":\"some path\",\"type\":\"INT\",\"value\":\"1\",\"ticketReference\":\"some ticket reference\",\"ifExecutionContextContains\":\"some if constraint\"}" );

    result = ToJson.fromXmlValue( null );
    assertThat( result ).isEqualTo( "null" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromXmlRequestWorks()
  {
    // Given
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( createXmlHeader( "header-name", "header-value" ) );
    xmlHeaders.getHeader().add( createXmlHeader( "etag", "my-etag" ) );
    final XmlRequest xmlRequest = new XmlRequest();
    xmlRequest.setHeaders( xmlHeaders );
    xmlRequest.setBody( "{\"a\":1}" );

    // When / Then
    String result = ToJson.fromXmlRequest( xmlRequest );
    assertThat( result ).isEqualTo( "{\"headers\":{\"header-name\":\"header-value\",\"etag\":\"my-etag\"},\"body\":\"{\"a\":1}\"}" );

    result = ToJson.fromXmlRequest( null );
    assertThat( result ).isEqualTo( "null" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromXmlResponseWorks()
  {
    // Given
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( createXmlHeader( "header-name", "header-value" ) );
    xmlHeaders.getHeader().add( createXmlHeader( "etag", "my-etag" ) );
    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    xmlHttpResponse.setHeaders( xmlHeaders );
    xmlHttpResponse.setBody( "{\"a\":1}" );
    xmlHttpResponse.setBodyIsJson( true );

    // When / Then
    String result = ToJson.fromXmlResponse( xmlHttpResponse );
    assertThat( result ).isEqualTo( "{\"headers\":{\"header-name\":\"header-value\",\"etag\":\"my-etag\"},\"body\":\"{\"a\":1}\"}" );

    result = ToJson.fromXmlResponse( null );
    assertThat( result ).isEqualTo( "null" );
  }
}
