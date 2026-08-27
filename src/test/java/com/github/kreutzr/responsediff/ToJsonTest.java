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
    String result = ToJson.fromXmlVariable( xmlVariable );
    assertThat( result ).isEqualTo( "{\"id\":\"id\",\"path\":\"path\",\"value\":\"value\"}" );

    result = ToJson.fromXmlVariable( null );
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
    String result = ToJson.fromXmlAnalysis( xmlAnalysis );
    assertThat( result ).isEqualTo( "{\"begin\":\"2026-08-27T20:28:00\",\"end\":\"2026-08-27T20:29:00\",\"duration\":\"PT1M\",\"successCount\":1,\"failCount\":0,\"skipCount\":2,\"totalCount\":3,\"messages\":[{\"level\":\"TRACE\",\"path\":\"some path\",\"value\":\"some value\",\"executionConstraint\":\"some constraint\"},{\"level\":\"UNKNOWN\",\"path\":\"other path\",\"value\":\"other value\",\"executionConstraint\":\"other constraint\"},null]}");

    result = ToJson.fromXmlAnalysis( null );
    assertThat( result ).isEqualTo( "null" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromHeadersWorks()
  {
    // Given
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( createXmlHeader( "header-name", "header-value" ) );
    xmlHeaders.getHeader().add( createXmlHeader( "etag", null ) );

    // When / Then
    String result = ToJson.fromXmlHeaders( xmlHeaders, false, null );
    assertThat( result ).isEqualTo( "\"headers\":{\"header-name\":\"header-value\",\"etag\":null}" );

    result = ToJson.fromXmlHeaders( null, true, null );
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

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatFromXmlExpectedWorks()
  {
    // Given
    final XmlHttpStatus xmlHttpStatus = new XmlHttpStatus();
    xmlHttpStatus.setCheckInverse( Boolean.TRUE );
    xmlHttpStatus.setLogLevel( XmlLogLevel.WARN );
    xmlHttpStatus.setValue( 200 );
    xmlHttpStatus.setTicketReference( "some ticket reference" );

    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( createXmlHeader( "header-with-quotes", "header-\"value\"" ) );
    xmlHeaders.getHeader().add( createXmlHeader( "etag", "my-etag" ) );

    final XmlMaxDuration xmlMaxDuration = new XmlMaxDuration();
    xmlMaxDuration.setLogLevel( XmlLogLevel.WARN );
    xmlMaxDuration.setValue( "PT5S" );

    final XmlValue xmlValue1 = new XmlValue();
    xmlValue1.setCheckInverse( Boolean.TRUE );
    xmlValue1.setPath( "$.a" );
    xmlValue1.setValue( "2" );
    xmlValue1.setType( XmlValueType.INT);
    final XmlValue xmlValue2 = new XmlValue();
    xmlValue2.setCheckInverse( Boolean.FALSE );
    xmlValue2.setPath( "$.a" );
    xmlValue2.setValue( "1" );
    xmlValue2.setType( XmlValueType.INT);
    final XmlValues xmlValues = new XmlValues();
    xmlValues.getValue().add( xmlValue1 );
    xmlValues.getValue().add( xmlValue2 );

    final XmlBody xmlBody = new XmlBody();
    xmlBody.setLogLevel( XmlLogLevel.ERROR );
    xmlBody.setNoBody( Boolean.FALSE );
    xmlBody.setTicketReference( "other ticket reference" );
    xmlBody.setValue( "{\"a\":1}" );

    final XmlExpected xmlExpected = new XmlExpected();
    xmlExpected.setHttpStatus( xmlHttpStatus );
    xmlExpected.setHeaders( xmlHeaders );
    xmlExpected.setMaxDuration( xmlMaxDuration );
    xmlExpected.setValues( xmlValues );
    xmlExpected.setBody( xmlBody );

    // When / Then
    String result = ToJson.fromXmlExpected( xmlExpected );
    assertThat( result ).isEqualTo( "{\"headers\":{\"header-with-quotes\":\"header-\\\"value\\\"\",\"etag\":\"my-etag\"},\"httpStatus\":{\"value\":\"200\",\"checkInverse\":true,\"logLevel\":\"WARN\",\"ticketReference\":\"some ticket reference\"},\"maxDuration\":{\"value\":\"PT5S\",\"logLevel\":\"WARN\"},\"body\":{\"value\":\"{\"a\":1}\",\"noBody\":false,\"logLevel\":\"ERROR\",\"ticketReference\":\"other ticket reference\"},\"values\":[{\"epsilon\":\"null\",\"path\":\"$.a\",\"type\":\"INT\",\"value\":\"2\"},{\"epsilon\":\"null\",\"path\":\"$.a\",\"type\":\"INT\",\"value\":\"1\"}]}" );

    result = ToJson.fromXmlExpected( null );
    assertThat( result ).isEqualTo( "null" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatVariousNullValuesWork()
  {
    String result = ToJson.fromXmlHttpStatus( null );
    assertThat( result ).isEqualTo( "null" );

    result = ToJson.fromXmlHeaders( null, false, null );
    assertThat( result ).isEqualTo( "\"headers\":null" );

    result = ToJson.fromXmlMaxDuration( null );
    assertThat( result ).isEqualTo( "null" );

    result = ToJson.fromXmlValues( null );
    assertThat( result ).isEqualTo( "null" );

    result = ToJson.fromXmlBody( null );
    assertThat( result ).isEqualTo( "null" );
  }
}
