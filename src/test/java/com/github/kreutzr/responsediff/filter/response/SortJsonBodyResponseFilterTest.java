package com.github.kreutzr.responsediff.filter.response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.XmlHttpResponse;

public class SortJsonBodyResponseFilterTest
{
  @Test
  public void testThatJsonMapIsSorted()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "{ \"c\" : 1, \"b\" : \"text\", \"a\" : { \"e\" : [ \"z\", \"y\", \"x\" ], \"d\" : 3.1415 }, \"f\" : [ { \"w\" : 2, \"v\" : 1 } ] }";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "{\"a\":{\"d\":3.1415,\"e\":[\"z\",\"y\",\"x\"]},\"b\":\"text\",\"c\":1,\"f\":[{\"v\":1,\"w\":2}]}", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsSortedIfRequested()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "[ { \"b\" : 4, \"a\" : { \"d\" : [6,5,4] } }, { \"b\" : 2, \"a\" : { \"c\" : [3,2,1] } } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":{\"c\":[1,2,3]},\"b\":2},{\"a\":{\"d\":[4,5,6]},\"b\":4}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsNotSortedIfNotRequested()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "false" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "[ { \"b\" : 4, \"a\" : { \"d\" : [6,5,4] } }, { \"b\" : 2, \"a\" : { \"c\" : [3,2,1] } } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":{\"d\":[6,5,4]},\"b\":4},{\"a\":{\"c\":[3,2,1]},\"b\":2}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsSortedSelectiveIfRequested()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "c" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "[ { \"b\" : 4, \"a\" : { \"d\" : [6,5,4] } }, { \"b\" : 2, \"a\" : { \"c\" : [3,2,1] } } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":{\"d\":[6,5,4]},\"b\":4},{\"a\":{\"c\":[1,2,3]},\"b\":2}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsSortedSelectiveForRootIfRequested()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "$" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "[ { \"b\" : 4, \"a\" : { \"d\" : [6,5,4] } }, { \"b\" : 2, \"a\" : { \"c\" : [3,2,1] } } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":{\"c\":[3,2,1]},\"b\":2},{\"a\":{\"d\":[6,5,4]},\"b\":4}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsNotSortedIfWhitelistHasNoMatchingEntries()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, " " ); // No (matching) whitelist entries are defined here

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "[ { \"b\" : 4, \"a\" : { \"d\" : [6,5,4] } }, { \"b\" : 2, \"a\" : { \"c\" : [3,2,1] } } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":{\"d\":[6,5,4]},\"b\":4},{\"a\":{\"c\":[3,2,1]},\"b\":2}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsSortedSelectiveIfRequestedWithMapJsonPaths()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, " c( $.x ; $.y ) , d " );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json = "[ { \"b\" : 4, \"a\" : { \"d\" : [6,5,4] } }, { \"b\" : 2, \"a\" : { \"c\" : [ { \"x\" : 2, \"y\" : 2, \"z\" : \"A\" }, { \"x\" : 2, \"y\" : 1, \"z\" : \"B\" }, { \"x\" : 1, \"y\" : 1, \"z\" : \"C\" } ] } } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":{\"d\":[4,5,6]},\"b\":4},{\"a\":{\"c\":[{\"x\":1,\"y\":1,\"z\":\"C\"},{\"x\":2,\"y\":1,\"z\":\"B\"},{\"x\":2,\"y\":2,\"z\":\"A\"}]},\"b\":2}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsSortedSelectiveIfRequestedWithArrayJsonPaths()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "$($.id),a($.x;$.y),b" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json =  "[ { \"id\" : \"002\", \"a\" : [ { \"x\" : 3, \"y\" : 1 }, { \"x\" : 2, \"y\" : 1 } ], \"b\" : [ 6,5,4 ], \"c\": [ 9,8,7 ] }, { \"id\" : \"001\", \"a\" : [ { \"x\" : 1, \"y\" : 2.1 }, { \"x\" : 1, \"y\" : 11.2 } ], \"b\" : [ 3,2,1 ], \"c\": [ 8,4,2 ] } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":[{\"x\":1,\"y\":2.1},{\"x\":1,\"y\":11.2}],\"b\":[1,2,3],\"c\":[8,4,2],\"id\":\"001\"},{\"a\":[{\"x\":2,\"y\":1},{\"x\":3,\"y\":1}],\"b\":[4,5,6],\"c\":[9,8,7],\"id\":\"002\"}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testPerformanceOfSortArraysMethod()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "$($.id),a($.x;$.y),b" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json =  "[ { \"id\" : \"002\", \"a\" : [ { \"x\" : 3, \"y\" : 1 }, { \"x\" : 2, \"y\" : 1 } ], \"b\" : [ 6,5,4 ], \"c\": [ 9,8,7 ] }, { \"id\" : \"001\", \"a\" : [ { \"x\" : 1, \"y\" : 2.1 }, { \"x\" : 1, \"y\" : 11.2 } ], \"b\" : [ 3,2,1 ], \"c\": [ 8,4,2 ] } ]";
    xmlHttpResponse.setBodyIsJson( true );

    String sortedJson = "NOT SORTED";

    final int loops = 1000;
    final long start = System.currentTimeMillis();
    for( int i=0; i < loops; i++ ) {
      xmlHttpResponse.setBody( json );

      // When
      try
      {
        filter.apply( xmlHttpResponse );
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    final long duration = (System.currentTimeMillis() - start);
    System.out.print( "Sorting " + loops + " times took " + duration + " ms." );

    // Then
    sortedJson = xmlHttpResponse.getBody();
    Assertions.assertEquals( "[{\"a\":[{\"x\":1,\"y\":2.1},{\"x\":1,\"y\":11.2}],\"b\":[1,2,3],\"c\":[8,4,2],\"id\":\"001\"},{\"a\":[{\"x\":2,\"y\":1},{\"x\":3,\"y\":1}],\"b\":[4,5,6],\"c\":[9,8,7],\"id\":\"002\"}]", sortedJson );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatSelectiveArraySortingDoes_NOT_WorkForNegativeNumbers()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "a($.x ; $.THIS_DOES_NOT_EXIST)" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json =  "[ { \"a\" : [ { \"x\" : -3, \"y\" : 3 }, { \"x\" : -2, \"y\" : 2 }, { \"x\" : 1, \"y\" : 1 }, { \"x\" : 0, \"y\" : 0 } ] } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":[{\"x\":-2,\"y\":2},{\"x\":-3,\"y\":3},{\"x\":0,\"y\":0},{\"x\":1,\"y\":1}]}]", sortedJson ); // -3 is less than -2 but we compare alphabetically
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatJsonArrayIsSortedSelectiveIfRequestedWithArrayJsonPathsForBoolean()
  {
    // Given
    final SortJsonBodyResponseFilter filter = new SortJsonBodyResponseFilter();
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS, "true" );
    filter.setFilterParameter( SortJsonBodyResponseFilter.PARAMETER_NAME__SORT_ARRAYS__KEYS, "a($.x)" );

    final XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
    final String json =  "[ { \"a\" : [ { \"x\" : true, \"y\" : 2 }, { \"x\" : false, \"y\" : 3 } ] } ]";
    xmlHttpResponse.setBody( json );
    xmlHttpResponse.setBodyIsJson( true );

    // When
    String sortedJson = "NOT SORTED";
    try
    {
      filter.apply( xmlHttpResponse );
      sortedJson = xmlHttpResponse.getBody();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Then
    Assertions.assertEquals( "[{\"a\":[{\"x\":false,\"y\":3},{\"x\":true,\"y\":2}]}]", sortedJson );
  }
}
