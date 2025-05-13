package com.github.kreutzr.responsediff.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class ComparatorHelperTest
{
  @Test
  public void testThatEqualsWorksForLocalDate()
  {
    // Given
    LocalDate lhs     = LocalDate.parse( "2023-10-26" );
    LocalDate rhs     = null;
    Duration  epsilon = null;
    boolean   result  = false;

    // When / Then
    rhs = LocalDate.parse( "2023-10-26" );
    result = ComparatorHelper.equals( lhs, rhs );
    assertThat( result ).isTrue();

    // When / Then
    rhs = LocalDate.parse( "2023-10-25" );
    result = ComparatorHelper.equals( lhs, rhs );
    assertThat( result ).isFalse();

    // When / Then
    rhs = LocalDate.parse( "2023-10-25" );
    epsilon = Duration.parse( "P1D" );
    result = ComparatorHelper.equals( lhs, rhs, epsilon );
    assertThat( result ).isTrue();

    // When / Then
    rhs = LocalDate.parse( "2023-10-24" );
    epsilon = Duration.parse( "P1D" );
    result = ComparatorHelper.equals( lhs, rhs, epsilon );
    assertThat( result ).isFalse();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatEqualsWorksForLocalDateTime()
  {
    // Given
    LocalDateTime lhs     = LocalDateTime.parse( "2023-10-26T15:00:00" );
    LocalDateTime rhs     = null;
    Duration      epsilon = null;
    boolean       result  = false;

    // When / Then
    rhs = LocalDateTime.parse( "2023-10-26T15:00:00" );
    result = ComparatorHelper.equals( lhs, rhs );
    assertThat( result ).isTrue();

    // When / Then
    rhs = LocalDateTime.parse( "2023-10-26T14:30:00" );
    result = ComparatorHelper.equals( lhs, rhs );
    assertThat( result ).isFalse();

    // When / Then
    rhs = LocalDateTime.parse( "2023-10-26T14:30:00" );
    epsilon = Duration.parse( "PT30M" );
    result = ComparatorHelper.equals( lhs, rhs, epsilon );
    assertThat( result ).isTrue();

    // When / Then
    rhs = LocalDateTime.parse( "2023-10-26T14:30:00" );
    epsilon = Duration.parse( "PT29M59S" );
    result = ComparatorHelper.equals( lhs, rhs, epsilon );
    assertThat( result ).isFalse();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatEqualsWorksForLocalDuration()
  {
    // Given
    Duration lhs     = Duration.parse( "P1D" );
    Duration rhs     = null;
    Duration epsilon = null;
    boolean  result  = false;

    // When / Then
    rhs = Duration.parse( "P1D" );
    result = ComparatorHelper.equals( lhs, rhs );
    assertThat( result ).isTrue();

    // When / Then
    rhs = Duration.parse( "P1DT5S" );
    result = ComparatorHelper.equals( lhs, rhs );
    assertThat( result ).isFalse();

    // When / Then
    rhs = Duration.parse( "P1DT5S" );
    epsilon = Duration.parse( "PT5S" );
    result = ComparatorHelper.equals( lhs, rhs, epsilon );
    assertThat( result ).isTrue();

    // When / Then
    rhs = Duration.parse( "P1DT5S" );
    epsilon = Duration.parse( "PT4S" );
    result = ComparatorHelper.equals( lhs, rhs, epsilon );
    assertThat( result ).isFalse();
  }
}