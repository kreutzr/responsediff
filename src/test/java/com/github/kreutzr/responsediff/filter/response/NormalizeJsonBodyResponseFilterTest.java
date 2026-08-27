package com.github.kreutzr.responsediff.filter.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlHttpResponse;
import com.github.kreutzr.responsediff.base.TestRoot;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class NormalizeJsonBodyResponseFilterTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( NormalizeJsonBodyResponseFilter.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatReplacementsWorks()
  {
    try {
      // Given
      final String bodyText = "{ \"project.id\" : \"123\", \"some-key\" : \"_project.id_\" }";
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );
      xmlHttpResponse.setBodyIsJson( true );

      // When
      final NormalizeJsonBodyResponseFilter filter = new NormalizeJsonBodyResponseFilter();
      filter.setFilterParameter(
        NormalizeJsonBodyResponseFilter.PARAMETER_NAME__REPLACEMENTS,
        "{ \"project.id\" : \"project-id\", \"123\" : \"321\" }"
      );
      filter.setFilterParameter( NormalizeJsonBodyResponseFilter.PARAMETER_NAME__NORMALIZE_MAPS,   "true" );
      filter.setFilterParameter( NormalizeJsonBodyResponseFilter.PARAMETER_NAME__NORMALIZE_ARRAYS, "true" );
      filter.apply( xmlHttpResponse );

      // Then
      final String json = xmlHttpResponse.getBody();
      final DocumentContext context = JsonPath.parse( json );
//      System.out.println( json );
      assertThat( (String) context.read( "$.project-id" ) ).isEqualTo( "321" );          // Key and value have changed
      assertThat( (String) context.read( "$.some-key"   ) ).isEqualTo( "_project.id_" ); // Value remains unchanged
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatPassingNullDoesNotHarm()
  {
    try {
      // Given
      final String bodyText = null;
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );
      xmlHttpResponse.setBodyIsJson( true );

      // When
      final NormalizeJsonBodyResponseFilter filter = new NormalizeJsonBodyResponseFilter();
      filter.setFilterParameter(
        NormalizeJsonBodyResponseFilter.PARAMETER_NAME__REPLACEMENTS,
        "{ \"project.id\" : \"project-id\", \"123\" : \"321\" }"
      );
      filter.setFilterParameter( NormalizeJsonBodyResponseFilter.PARAMETER_NAME__NORMALIZE_MAPS,   "true" );
      filter.setFilterParameter( NormalizeJsonBodyResponseFilter.PARAMETER_NAME__NORMALIZE_ARRAYS, "true" );
      filter.apply( xmlHttpResponse );

      // Then
      // We expect no exception here
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatIllegalParameterCombinationsThrowException()
  {
    try {
      // Given
        final String bodyText = "{ \"project.id\" : \"123\", \"some-key\" : \"_project.id_\" }";
      final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
      xmlHttpResponse.setHeaders( new XmlHeaders() );
      xmlHttpResponse.setBody( bodyText );
      xmlHttpResponse.setBodyIsJson( true );

      // When
      final NormalizeJsonBodyResponseFilter filter = new NormalizeJsonBodyResponseFilter();
      filter.setFilterParameter(
        NormalizeJsonBodyResponseFilter.PARAMETER_NAME__REPLACEMENTS,
        "{ \"project.id\" : \"project-id\", \"123\" : \"321\" }"
      );
      filter.setFilterParameter( NormalizeJsonBodyResponseFilter.PARAMETER_NAME__NORMALIZE_MAPS,   "false" );
      filter.setFilterParameter( NormalizeJsonBodyResponseFilter.PARAMETER_NAME__NORMALIZE_ARRAYS, "true" );

      // Then
      Throwable ex = catchThrowable( () -> filter.apply( xmlHttpResponse ) );
      assertThat( ex )
        .isInstanceOf( DiffFilterException.class )
        .hasMessageContaining( NormalizeJsonBodyResponseFilter.ERROR_MESSAGE__ILLEGAL_PARAMETER_COMBINATION );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      fail( "unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // NOTE: Tests for map and array normalization are covered by the JsonTraverserNormalizationVisitorTest.
}
