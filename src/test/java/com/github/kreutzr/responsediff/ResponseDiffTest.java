package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestBase;

public class ResponseDiffTest extends TestBase
{
  @Test
  public void testThatTicketServiceUrlsParsingWorks() throws Exception
  {
    try {
	    Map< String, String > map = null;
	
	    // Given
	    String ticketServiceUrlsAsString = null;
	    // When
	    map = ResponseDiff.parseTicketServiceUrls( ticketServiceUrlsAsString );
	    // Then
        assertThat( map ).isEmpty();

        // --------------------------------------------------------------------------------------------

	    // Given
	    ticketServiceUrlsAsString = "some-service-url";
	    // When
	    map = ResponseDiff.parseTicketServiceUrls( ticketServiceUrlsAsString );
	    // Then
        assertThat( map.get( ResponseDiff.DEFAULT_TICKET_SERVICE_ID ) ).isEqualTo( "some-service-url" );
        assertThat( map.size() ).isEqualTo( 1 );

        // --------------------------------------------------------------------------------------------

	    // Given
	    ticketServiceUrlsAsString = "ssu=some-service-url";
	    // When
	    map = ResponseDiff.parseTicketServiceUrls( ticketServiceUrlsAsString );
	    // Then
        assertThat( map.get( ResponseDiff.DEFAULT_TICKET_SERVICE_ID ) ).isNull();
        assertThat( map.get( "ssu" ) ).isEqualTo( "some-service-url" );
        assertThat( map.size() ).isEqualTo( 1 );

        // --------------------------------------------------------------------------------------------

	    // Given
	    ticketServiceUrlsAsString = "ssu=some-service-url,some-other-service-url";
	    // When
	    map = ResponseDiff.parseTicketServiceUrls( ticketServiceUrlsAsString );
	    // Then
        assertThat( map.get( ResponseDiff.DEFAULT_TICKET_SERVICE_ID ) ).isEqualTo( "some-other-service-url" );
        assertThat( map.get( "ssu" ) ).isEqualTo( "some-service-url" );
        assertThat( map.size() ).isEqualTo( 2 );

        // --------------------------------------------------------------------------------------------

	    // Given
	    ticketServiceUrlsAsString = "ssu=some-service-url,sosu=some-other-service-url";
	    // When
	    map = ResponseDiff.parseTicketServiceUrls( ticketServiceUrlsAsString );
	    // Then
        assertThat( map.get( ResponseDiff.DEFAULT_TICKET_SERVICE_ID ) ).isNull();
        assertThat( map.get( "ssu" ) ).isEqualTo( "some-service-url" );
        assertThat( map.get( "sosu" ) ).isEqualTo( "some-other-service-url" );
        assertThat( map.size() ).isEqualTo( 2 );

        // --------------------------------------------------------------------------------------------

	    // Given
	    ticketServiceUrlsAsString = "ssu=some-service-url,sosu=some-other-service-url,sosu=key-clash,some-default-url";
	    // When
	    map = ResponseDiff.parseTicketServiceUrls( ticketServiceUrlsAsString );
	    // Then
        assertThat( map.get( ResponseDiff.DEFAULT_TICKET_SERVICE_ID ) ).isEqualTo( "some-default-url" );
        assertThat( map.get( "ssu"  ) ).isEqualTo( "some-service-url" );
        assertThat( map.get( "sosu" ) ).isEqualTo( "some-other-service-url" );
        assertThat( map.size() ).isEqualTo( 3 );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }
}
