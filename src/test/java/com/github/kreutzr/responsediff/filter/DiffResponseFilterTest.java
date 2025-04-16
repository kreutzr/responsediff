package com.github.kreutzr.responsediff.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;
import com.github.kreutzr.responsediff.filter.response.SortJsonBodyResponseFilter;

import com.github.kreutzr.responsediff.XmlHttpResponse;

public class DiffResponseFilterTest
{
  @Test
  public void testThatOriginalResponseIsNotStoredIfNotRequested()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( DiffResponseFilterImpl.PARAMETER_NAME__STORE_ORIGINAL, "false" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "{ \"b\" : 1, \"a\" : 1 }";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    try
    {
      // When
      assertTrue( DiffResponseFilterImpl.class.isAssignableFrom( SortJsonBodyResponseFilter.class ) );
      assertThat( xmlHttpResponse.getOriginalResponse() ).isNull();
      filter.apply( xmlHttpResponse );

      // Then
      assertThat( xmlHttpResponse.getOriginalResponse() ).isNull();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatOriginalResponseIsStoredIfRequested()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( DiffResponseFilterImpl.PARAMETER_NAME__STORE_ORIGINAL, "true" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "{ \"b\" : 1, \"a\" : 1 }";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    try
    {
      // When
      assertTrue( DiffResponseFilterImpl.class.isAssignableFrom( SortJsonBodyResponseFilter.class ) );
      assertThat( xmlHttpResponse.getOriginalResponse() ).isNull();
      filter.apply( xmlHttpResponse );

      // Then
      assertThat( xmlHttpResponse.getOriginalResponse() ).isNotNull();
      assertThat( xmlHttpResponse.getOriginalResponse().getBody() ).isEqualTo( json );
      assertThat( xmlHttpResponse.getBody() ).isNotEqualTo( json );
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
