package com.github.kreutzr.responsediff.filter.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.base.TestRoot;

public class RemoveHeaderRequestFilterTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( RemoveHeaderRequestFilter.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatHeadersAreConvertedCorrectlyForVariables()
  {
    try {
      final String headerValue = "Some value";

      // Given
      RemoveHeaderRequestFilter filter = new RemoveHeaderRequestFilter();
      filter.setFilterParameter( RemoveHeaderRequestFilter.PARAMETER_NAME__NAMES, " HeaderA, 	HeaderB  , HeaderC" );
      filter.init();

      final XmlHeader xmlHeaderA = new XmlHeader(); xmlHeaderA.setName( "HeaderA" ); xmlHeaderA.setValue( headerValue );
      final XmlHeader xmlHeaderB = new XmlHeader(); xmlHeaderB.setName( "HeaderB" ); xmlHeaderB.setValue( headerValue );
      final XmlHeader xmlHeaderC = new XmlHeader(); xmlHeaderC.setName( "HeaderC" ); xmlHeaderC.setValue( headerValue );
      final XmlHeader xmlHeaderX = new XmlHeader(); xmlHeaderX.setName( "HeaderX" ); xmlHeaderX.setValue( headerValue );

      final XmlHeaders xmlHeaders = new XmlHeaders();
      xmlHeaders.getHeader().add( xmlHeaderA );
      xmlHeaders.getHeader().add( xmlHeaderX );
      xmlHeaders.getHeader().add( xmlHeaderB );
      xmlHeaders.getHeader().add( xmlHeaderC );

      final XmlRequest xmlRequest = new XmlRequest();
      xmlRequest.setHeaders( xmlHeaders );

      //When
      filter.apply( xmlRequest, null, null );
      filter.next(); // Check that this has no bad effect

      // Then
      assertThat( xmlRequest.getHeaders().getHeader() ).hasSize( 1 ).contains( xmlHeaderX );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      assertTrue( false, "Unreachable" );
    }
  }
}
