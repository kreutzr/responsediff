package com.github.kreutzr.responsediff.filter.request.setvariables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.TestSetHandler;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlParameter;
import com.github.kreutzr.responsediff.XmlParameters;
import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlTest;

public class SetVariablesRequestFilterTest
{
  private XmlHeader createXmlHeader( final String name, final String value )
  {
    final XmlHeader xmlHeader = new XmlHeader();
    xmlHeader.setName ( name );
    xmlHeader.setValue( value );
    return xmlHeader;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private XmlParameter createXmlParameter( final String name, final String value )
  {
    final XmlParameter xmlParameter = new XmlParameter();
    xmlParameter.setId   ( name );
    xmlParameter.setValue( value );
    return xmlParameter;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testHeadersAreConvertedCorrectlyForVariables()
  {
    try {
      // Given
      final SetVariablesRequestFilter filter = new SetVariablesRequestFilter();
      final SetVariablesRequestFilterSource source = new SetVariablesRequestFilterSource();
      final Map< String, List< Object > > variables = new TreeMap<>();
      variables.put( "PORT", Arrays.asList( 4321 ) );
      variables.put( "ENDPOINT", Arrays.asList( "someEndpoint" ) );
      variables.put( "ID", Arrays.asList( "someId", "someOtherId" ) );
      variables.put( "TEXT_VALUE", Arrays.asList( "text-value" ) );
      variables.put( "NUMBER_VALUE", Arrays.asList( 123 ) );
      variables.put( "HEADER_VALUE", Arrays.asList( "header-value" ) );
      variables.put( "PARAMETER_VALUE", Arrays.asList( "parameter-value" ) );
      source.setVariables( variables );
      source.setVariableSets( null );
      filter.setSource( source );
      filter.setFilterParameter( SetVariablesRequestFilter.PARAMETER_NAME__USE_VARIABLES, "true" );

      XmlRequest xmlRequest = new XmlRequest();
      xmlRequest.setEndpoint("http://my-server:${PORT}/my-service/${ENDPOINT}/?id=${ID}&value=${UNKNOWN}" );
      xmlRequest.setBody( "{\"text\":\"${TEXT_VALUE}\",\"number\":${NUMBER_VALUE}}" );
      final XmlHeaders xmlHeaders = new XmlHeaders();
      xmlHeaders.getHeader().add( createXmlHeader( "my-header", "${HEADER_VALUE}" ) );
      xmlRequest.setHeaders( xmlHeaders );
      final XmlParameters xmlParameters = new XmlParameters();
      xmlParameters.getParameter().add( createXmlParameter( "my-parameter", "${PARAMETER_VALUE}") );
      xmlRequest.setParameters( xmlParameters );

      final String  serviceId = TestSetHandler.CANDIDATE;
      final XmlTest xmlTest   = new XmlTest();
      xmlTest.setId( "The id is ${ID}" );
      xmlTest.setDescription( "This is a description for test with id ${ID}" );

      // When
      filter.apply( xmlRequest, serviceId, xmlTest );
      filter.next();

      // Then
      assertThat( xmlRequest.getEndpoint() ).isEqualTo( "http://my-server:4321/my-service/someEndpoint/?id=someId&value=${UNKNOWN}" );
      assertThat( xmlRequest.getBody() ).isEqualTo( "{\"text\":\"text-value\",\"number\":123}" );

      final XmlParameter xmlParameter = xmlRequest.getParameters().getParameter().get( 0 );
      assertThat( xmlParameter.getId() ).isEqualTo( "my-parameter" );
      assertThat( xmlParameter.getValue() ).isEqualTo( "parameter-value" );

      final XmlHeader xmlHeader = xmlRequest.getHeaders().getHeader().get( 0 );
      assertThat( xmlHeader.getName() ).isEqualTo( "my-header" );
      assertThat( xmlHeader.getValue() ).isEqualTo( "header-value" );

      assertThat( xmlTest.getId() ).isEqualTo( "The id is someId" );
      assertThat( xmlTest.getDescription() ).isEqualTo( "This is a description for test with id someId" );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      assertTrue( false, "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testHeadersAreConvertedCorrectlyForVariableSets()
  {
    try {
      // Given
      final SetVariablesRequestFilter filter = new SetVariablesRequestFilter();
      final SetVariablesRequestFilterSource source = new SetVariablesRequestFilterSource();
      final List< Map< String, Object > > variableSets = new ArrayList< Map< String, Object > >();
      final Map< String, Object > set = new TreeMap< String, Object >();
      set.put( "PORT", 4321 );
      set.put( "ENDPOINT", "someEndpoint" );
      set.put( "ID", "someId" );
      set.put( "TEXT_VALUE", "text-value" );
      set.put( "NUMBER_VALUE", 123 );
      set.put( "HEADER_VALUE", "header-value" );
      set.put( "PARAMETER_VALUE", "parameter-value" );
      variableSets.add( set );
      source.setVariables( null );
      source.setVariableSets( variableSets );
      filter.setSource( source );
      filter.setFilterParameter( SetVariablesRequestFilter.PARAMETER_NAME__USE_VARIABLES, "false" );

      XmlRequest xmlRequest = new XmlRequest();
      xmlRequest.setEndpoint("http://my-server:${PORT}/my-service/${ENDPOINT}/?id=%24%7BID%7D&value=${UNKNOWN}" ); // Partially URL encoded for testing
      xmlRequest.setBody( "{\"text\":\"${TEXT_VALUE}\",\"number\":${NUMBER_VALUE}}" );
      final XmlHeaders xmlHeaders = new XmlHeaders();
      xmlHeaders.getHeader().add( createXmlHeader( "my-header", "${HEADER_VALUE}" ) );
      xmlRequest.setHeaders( xmlHeaders );
      final XmlParameters xmlParameters = new XmlParameters();
      xmlParameters.getParameter().add( createXmlParameter( "my-parameter", "${PARAMETER_VALUE}") );
      xmlRequest.setParameters( xmlParameters );

      final String  serviceId = TestSetHandler.CANDIDATE;
      final XmlTest xmlTest   = new XmlTest();
      xmlTest.setId( "The id is ${ID}" );
      xmlTest.setDescription( "This is a description for test with id ${ID}" );

      // When
      filter.apply( xmlRequest, serviceId, xmlTest );
      filter.next();

      // Then
      assertThat( xmlRequest.getEndpoint() ).isEqualTo( "http://my-server:4321/my-service/someEndpoint/?id=someId&value=${UNKNOWN}" );
      assertThat( xmlRequest.getBody() ).isEqualTo( "{\"text\":\"text-value\",\"number\":123}" );

      final XmlParameter xmlParameter = xmlRequest.getParameters().getParameter().get( 0 );
      assertThat( xmlParameter.getId() ).isEqualTo( "my-parameter" );
      assertThat( xmlParameter.getValue() ).isEqualTo( "parameter-value" );

      final XmlHeader xmlHeader = xmlRequest.getHeaders().getHeader().get( 0 );
      assertThat( xmlHeader.getName() ).isEqualTo( "my-header" );
      assertThat( xmlHeader.getValue() ).isEqualTo( "header-value" );

      assertThat( xmlTest.getId() ).isEqualTo( "The id is someId" );
      assertThat( xmlTest.getDescription() ).isEqualTo( "This is a description for test with id someId" );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      assertTrue( false, "Unreachable" );
    }
  }
}
