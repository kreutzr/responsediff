package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class VariablesHandlerTest
{
  @Test
  public void testThatUrlVariablesAreReplaced()
  {
    // Given
    final String       text         = "http://my-server:${PORT}/my-service/${ENDPOINT}/?id=${ID}&value=${UNKNOWN}";
    final XmlVariables xmlVariables = new XmlVariables();
    final String       source       = "serviceUrl";
    final String       serviceId    = TestSetHandler.CANDIDATE;
    final String       testId       = "someTestId";
    final String       fileName     = "someFileName";

    xmlVariables.getVariable().add( createXmlVariable(
      "PORT",
      XmlValueType.INT,
      "1234",
      null
    ) );
    xmlVariables.variable.add( createXmlVariable(
      "ENDPOINT",
      XmlValueType.STRING,
      "someEndpoint",
      null
    ) );
    xmlVariables.variable.add( createXmlVariable(
      serviceId + "." + "ID",
      XmlValueType.STRING,
      "someId",
      null
    ) );

    // When
    final String result = VariablesHandler.applyVariables(
      text,
      xmlVariables,
      source,
      serviceId,
      testId,
      fileName
    );

    // Then
    assertThat( result ).isEqualTo( "http://my-server:1234/my-service/someEndpoint/?id=someId&value=${UNKNOWN}" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatUnresolvedVariablesAreFound()
  {
    // Preparation
    final XmlHeader xmlHeader = new XmlHeader();
    xmlHeader.setName( "someHeader" );
    xmlHeader.setValue( "...${VAR_3}..." );
    final XmlHeaders xmlHeaders = new XmlHeaders();
    xmlHeaders.getHeader().add( xmlHeader );

    // Given
    final XmlRequest xmlRequest = new XmlRequest();
    xmlRequest.setEndpoint( "...${VAR_1}..." );
    xmlRequest.setBody( "...${VAR_2}..." );
    xmlRequest.setHeaders( xmlHeaders );

    // When
    final Set< String > unresolvedVariables = VariablesHandler.findUnresolvedVariables( xmlRequest );

    // Then
    assertThat( unresolvedVariables.size() ).isEqualTo( 3 );
    assertThat( unresolvedVariables ).contains( "${VAR_1}" );
    assertThat( unresolvedVariables ).contains( "${VAR_2}" );
    assertThat( unresolvedVariables ).contains( "${VAR_3}" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private XmlVariable createXmlVariable(
    final String id,
    final XmlValueType type,
    final String value,
    final String path
  )
  {
    final XmlVariable xmlVariable = new XmlVariable();

    xmlVariable.setId   ( id );
    xmlVariable.setType ( type );
    xmlVariable.setValue( value );
    xmlVariable.setPath ( path );

    return xmlVariable;
  }
}
