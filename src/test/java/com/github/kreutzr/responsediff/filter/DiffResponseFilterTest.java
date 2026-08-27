package com.github.kreutzr.responsediff.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.HttpHandler;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHttpResponse;
import com.github.kreutzr.responsediff.base.TestRoot;
import com.github.kreutzr.responsediff.filter.response.SortJsonBodyResponseFilter;

public class DiffResponseFilterTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( DummyResponseFilter.class ); // Abstract class DiffResponseFilterImpl can not be instantiated.  
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

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
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
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
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatNullIsHandledAsHttpResonse()
  {
    // Given
    final DummyResponseFilter filter = new DummyResponseFilter();
    filter.setFilterParameter( DiffResponseFilterImpl.PARAMETER_NAME__STORE_ORIGINAL, "false" );

    final XmlHttpResponse xmlHttpResponse = null;

    try
    {
      // When
      filter.apply( xmlHttpResponse );

      // Then
      // We expect no exception
    }
    catch (Exception e)
    {
      e.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatContentTypeIsProperlySet()
  {
    // Given
    final DummyResponseFilter filter = new DummyResponseFilter();
    filter.setFilterParameter( DiffResponseFilterImpl.PARAMETER_NAME__STORE_ORIGINAL, "false" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "{ \"b\" : 1, \"a\" : 1 }";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    try
    {
      // When
      filter.apply( xmlHttpResponse );

      // Then
      String contentType = null;
      for( final XmlHeader xmlHeader : xmlHttpResponse.getHeaders().getHeader() ) {
        if( xmlHeader.getName().equals( HttpHandler.HEADER_NAME__CONTENT_TYPE ) ) {
          contentType = xmlHeader.getValue();
          break;
        }
      }
      assertThat( contentType ).isNotNull().isEqualTo( "application/json" );
    }
    catch (Exception e)
    {
      e.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}
