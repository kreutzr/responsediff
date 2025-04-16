package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.Constants;
import com.github.kreutzr.responsediff.VariablesCalculator;

public class VariablesCalculatorTest
{
  @Test
  public void testCalculateIfRequieredWorks()
  {
    try {
      // Given

      // ----------------------------------------------------
      // String value
      // ----------------------------------------------------

      // When / Then
      String rawValue = "test";
      String value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );

      // ----------------------------------------------------
      // Null value
      // ----------------------------------------------------

      // When / Then
      rawValue = null;
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNull();


      // ----------------------------------------------------
      // Variable
      // ----------------------------------------------------

      // When / Then
      rawValue = "${  MY_VARIABLE_1  }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );

      // ----------------------------------------------------
      // Keep surrounding text
      // ----------------------------------------------------

      // When / Then
      rawValue = "aaa ${ MY_VARIABLE_1 } bbb ${ MY_VARIABLE_2 } ccc";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );

      // When / Then
      rawValue = "aaa ${ MY_VARIABLE_1 } bbb ${ MY_VARIABLE_2 }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );

      // When / Then
      rawValue = "${ MY_VARIABLE_1 } bbb ${ MY_VARIABLE_2 }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );

      // When / Then
      rawValue = "${ MY_VARIABLE_1 } bbb ${ randomInteger( ) } ccc";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "\\$\\{ MY_VARIABLE_1 \\} bbb [\\-]?[0-9]+ ccc" );


      // ----------------------------------------------------
      // UUID value
      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomUUID( ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[a-zA-Z0-9-]{36}" );
      try {
        @SuppressWarnings("unused")
        final double number = Double.valueOf( value );
        assertThat( false ).as( "UUID must not deliver numbers." ).isEqualTo( true );
      }
      catch( final Exception ex ) {
        // UUID is not a number
      }

      // ----------------------------------------------------

      // When / Then
      final String prefix = "TEST_";
      rawValue = "${randomUUID( " + prefix + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).startsWith( prefix );
      try {
        @SuppressWarnings("unused")
        final double number = Double.valueOf( value );
        assertThat( false ).as( "UUID must not deliver numbers." ).isEqualTo( true );
      }
      catch( final Exception ex ) {
        // UUID is not a number
      }

      // ----------------------------------------------------

      // When / Then
      int maxLength = 15;
      rawValue = "${randomUUID( " + prefix + ", " + maxLength + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).startsWith( prefix );
      assertThat( value.length() ).isLessThanOrEqualTo( maxLength );
      try {
        @SuppressWarnings("unused")
        final double number = Double.valueOf( value );
        assertThat( false ).as( "UUID must not deliver numbers." ).isEqualTo( true );
      }
      catch( final Exception ex ) {
        // UUID is not a number
      }

      // ----------------------------------------------------

      // When / Then
      maxLength = 100;
      rawValue = "${randomUUID( " + prefix + ", " + maxLength + ",-,_,TEST_,PREFIX_ )}";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).startsWith( "PREFIX_" );
      assertThat( value ).doesNotContain( "-" );
      assertThat( value.length() ).isEqualTo( 7 + 36 );
//      System.out.println( value );
      try {
        @SuppressWarnings("unused")
        final double number = Double.valueOf( value );
        assertThat( false ).as( "UUID must not deliver numbers." ).isEqualTo( true );
      }
      catch( final Exception ex ) {
        // UUID is not a number
      }

      // ----------------------------------------------------
      // Integer value
      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomInteger  (  ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        @SuppressWarnings("unused")
        final int number = Integer.valueOf( value );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomInteger must deliver an integer value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${ randomInteger ( -5 ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final int number = Integer.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomInteger must deliver an integer value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomInteger( -5, 8 ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final int number = Integer.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5 );
        assertThat( number ).isLessThanOrEqualTo( 8 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomInteger must deliver an integer value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomInteger( -5, 8, 6 ) }"; // Last parameter is ignored
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final int number = Integer.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5 );
        assertThat( number ).isLessThanOrEqualTo( 8 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomInteger must deliver an integer value." ).isEqualTo( true );
      }

      // ----------------------------------------------------
      // Long value
      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomLong(  ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        @SuppressWarnings("unused")
        final long number = Long.valueOf( value );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomLong must deliver a long value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomLong( -5 ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final long number = Long.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomLong must deliver a long value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomLong( -5, 8 ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final long number = Long.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5 );
        assertThat( number ).isLessThanOrEqualTo( 8 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomLong must deliver a long value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomLong( -5, 8, 6 ) }"; // Last parameter is ignored
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final long number = Long.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5 );
        assertThat( number ).isLessThanOrEqualTo( 8 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomLong must deliver a long value." ).isEqualTo( true );
      }

      // ----------------------------------------------------
      // Double value
      // ----------------------------------------------------

      // When / Then
      rawValue = " ${ randomDouble  (  ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        @SuppressWarnings("unused")
        final double number = Double.valueOf( value );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomLong must deliver a long value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomDouble( -5.5 ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final double number = Double.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5.5 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomDouble must deliver a double value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomDouble( -5.5, 8.3 ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final double number = Double.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5.5 );
        assertThat( number ).isLessThanOrEqualTo( 8.3 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomDouble must deliver a double value." ).isEqualTo( true );
      }

      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomDouble( -5.5, 8.3, 6.0 ) }"; // Third parameter is ignored
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      try {
        final double number = Double.valueOf( value );
        assertThat( number ).isGreaterThanOrEqualTo( -5.5 );
        assertThat( number ).isLessThanOrEqualTo( 8.3 );
      }
      catch( final Exception ex ) {
        assertThat( false ).as( "randomDouble must deliver a double value." ).isEqualTo( true );
      }

      // ----------------------------------------------------
      // Random date
      // ----------------------------------------------------

      DateTimeFormatter dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATE );

      // When / Then
      rawValue = "${randomDate() }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );

      // When / Then
      rawValue = "${randomDate()}T12:00:00"; // See example in manual
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T12:00:00" );

      // When / Then
      String start = "2024-02-28";
      String end   = "2999-12-31";
      rawValue = "${randomDate( " + start + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThanOrEqualTo   ( end ); // exclusive

      // When / Then
      start = "2024-02-28";
      end   = "2024-02-29"; // Corner case (end = start + 1) (exclusive)
      rawValue = "${randomDate( " + start + "," + end + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isEqualTo( start );

      // When / Then
      start = "2024-02-28";
      end   = "2024-03-01";
      rawValue = "${randomDate( " + start + "," + end + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end ); // exclusive

      // When / Then
      start = LocalDate.now().format( dtf ); // today
      end   = "2999-12-31T23:59:59.999";
      rawValue = "${randomDate( today ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // When / Then
      int offsetDays = 3;
      start = LocalDate.now().plusDays( offsetDays ).format( dtf );
      end   = "2999-12-31";
      rawValue = "${randomDate( today    +   " + offsetDays + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // When / Then
      offsetDays = 1;
      int offsetDaysEnd = 1;
      start = LocalDate.now().minusDays( offsetDays ).format( dtf );
      end   = LocalDate.now().plusDays( offsetDaysEnd ).format( dtf );
      rawValue = "${randomDate( today - " + offsetDays + ", today+" + offsetDaysEnd + ") }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // ----------------------------------------------------
      // Random datetime
      // ----------------------------------------------------

      dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );

      // When / Then
      rawValue = "${randomDateTime() }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );

      // When / Then
      start = "2024-02-28T12:00:00.000";
      end   = "2999-12-31T23:59:59.999";
      rawValue = "${randomDateTime( " + start + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // When / Then
      start = "2024-02-28T12:00:00.000";
      end   = "2024-04-01T00:00:00.000";
      rawValue = "${randomDateTime( " + start + " , " + end + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // When / Then
      start = LocalDateTime.now().format( dtf ); // today
      end   = "2999-12-31T23:59:59.999";
      rawValue = "${randomDateTime( now ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // When / Then
      long offsetMillis    = 3000000;
      long offsetMillisEnd = 0;
      start = LocalDateTime.now().plus( offsetMillis, ChronoUnit.MILLIS ).format( dtf );
      end   = "2999-12-31T23:59:59.999";
      rawValue = "${randomDateTime( now + " + offsetMillis + " ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // When / Then
      offsetMillis    = 3000000;
      offsetMillisEnd = 6000000;
      start = LocalDateTime.now().minus( offsetMillis   , ChronoUnit.MILLIS ).format( dtf );
      end   = LocalDateTime.now().plus ( offsetMillisEnd, ChronoUnit.MILLIS ).format( dtf );
      rawValue = "${randomDateTime( now - " + offsetMillis + ", now+" + offsetMillisEnd + ") }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( start );
      assertThat( value ).isLessThan            ( end );

      // ----------------------------------------------------
      // Random boolean
      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomBoolean() }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "(true|false)" );

      // When / Then
      rawValue = "${randomBoolean( NONSENSE ) }"; // Parameter is ignored
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "(true|false)" );

      // ----------------------------------------------------
      // Random enum
      // ----------------------------------------------------

      // When / Then
      rawValue = "${randomEnum() }"; // No parameter => value remains unchanged for later replacement
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );

      // When / Then
      rawValue = "${randomEnum(AAA, BBB  , CCC    ) }";
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "(AAA|BBB|CCC)" );

      // ----------------------------------------------------
      // Now date
      // ----------------------------------------------------

      // When / Then
      rawValue = "${nowDate() }";
      dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATE );
      String today = LocalDate.now().format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isEqualTo( today );

      // When / Then
      offsetDays = 1;
      rawValue = "${nowDate( " + offsetDays + ", 500 )}"; // Additional parameters are ignored
      String todayPlusOffset = LocalDate.now().plusDays( offsetDays ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isEqualTo( todayPlusOffset );

      // When / Then
      offsetDays = 5;
      rawValue = "${nowDate(" + offsetDays + ") }";
      todayPlusOffset = LocalDate.now().plusDays( offsetDays ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isEqualTo( todayPlusOffset );

      // When / Then
      offsetDays = -5;
      rawValue = "${nowDate(" + offsetDays + ") }";
      todayPlusOffset = LocalDate.now().plusDays( offsetDays ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}" );
      assertThat( value ).isEqualTo( todayPlusOffset );

      // ----------------------------------------------------
      // Now datetime
      // ----------------------------------------------------

      // When / Then
      rawValue = "${nowDateTime() }";
      dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );
      String now = LocalDateTime.now().format( dtf );
      String nowPlusExecution = LocalDateTime.now().plus( 200, ChronoUnit.MILLIS ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( now );
      assertThat( value ).isLessThan( nowPlusExecution );

      // When / Then
      offsetMillis = 600000; // Offset 10 minutes as millis
      rawValue = "${nowDateTime( " + offsetMillis + " ) }";
      dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );
      now = LocalDateTime.now().plus( offsetMillis, ChronoUnit.MILLIS ).format( dtf );
      nowPlusExecution = LocalDateTime.now().plus( offsetMillis + 200, ChronoUnit.MILLIS ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( now );
      assertThat( value ).isLessThan( nowPlusExecution );

      // When / Then
      offsetMillis = -600000; // Offset -10 minutes as millis
      rawValue = "${nowDateTime( " + offsetMillis + " ) }";
      dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );
      now = LocalDateTime.now().plus( offsetMillis, ChronoUnit.MILLIS ).format( dtf );
      nowPlusExecution = LocalDateTime.now().plus( offsetMillis + 200, ChronoUnit.MILLIS ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( now );
      assertThat( value ).isLessThan( nowPlusExecution );

      // When / Then
      offsetMillis = 0;
      rawValue = "${nowDateTime( " + offsetMillis + ", 100000000 ) }"; // Additional parameters are ignores
      dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );
      now = LocalDateTime.now().plus( offsetMillis, ChronoUnit.MILLIS ).format( dtf );
      nowPlusExecution = LocalDateTime.now().plus( offsetMillis + 200, ChronoUnit.MILLIS ).format( dtf );
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isNotEqualTo( rawValue );
      assertThat( value ).matches( "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}" );
      assertThat( value ).isGreaterThanOrEqualTo( now );
      assertThat( value ).isLessThan( nowPlusExecution );

      // ----------------------------------------------------
      // Undefined
      // ----------------------------------------------------

      // When / Then
      rawValue = "${nonsense( a, b, c ) }"; // Unhandled operations remain unchanged
      value = VariablesCalculator.calculateIfRequired( rawValue );
      assertThat( value ).isEqualTo( rawValue );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      assertTrue( false, "Unreachable" );
    }
  }
}
