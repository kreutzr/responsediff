package com.github.kreutzr.responsediff.tools;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComparatorHelper
{
  public static final Duration ZERO_DURATION = Duration.parse( "PT0S" );

  private static final Logger LOG = LoggerFactory.getLogger( ComparatorHelper.class );

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs an epsilon equals comparison.
   * @param lhs The value to compare. Must not be null.
   * @param rhs The value to compare. Must not be null.
   * @return true if both values are equal. Otherwise false is returned.
   */
  public static boolean equals(
    final LocalDate lhs,
    final LocalDate rhs
  ) {
    return equals( lhs, rhs, ZERO_DURATION );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs an epsilon equals comparison.
   * @param lhs The value to compare. Must not be null.
   * @param rhs The value to compare. Must not be null.
   * @param epsilon The epsilon to use. Must not be null.
   * @return true if both values are epsilon equal. Otherwise false is returned.
   */
  public static boolean equals(
      final LocalDate lhs,
      final LocalDate rhs,
      final Duration epsilon
    ) {
    final long lhsMillis = Date.from( lhs.atStartOfDay().atZone( ZoneId.systemDefault() ).toInstant() ).getTime();
    final long rhsMillis = Date.from( rhs.atStartOfDay().atZone( ZoneId.systemDefault() ).toInstant() ).getTime();
    final long epsMillis = epsilon.toMillis();
    final long min = lhsMillis - epsMillis;
    final long max = lhsMillis + epsMillis;

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "LocalDate equals(): lhs=" + lhs + " (" + lhsMillis + "), rhs=" + rhs + " (" + rhsMillis + "), epsilon=" + epsilon + " (" + epsMillis + "), min=" + min + ", max=" + max );
    }
    return min <= rhsMillis && rhsMillis <= max;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs an epsilon equals comparison.
   * @param lhs The value to compare. Must not be null.
   * @param rhs The value to compare. Must not be null.
   * @return true if both values are equal. Otherwise false is returned.
   */
  public static boolean equals(
    final LocalDateTime lhs,
    final LocalDateTime rhs
  ) {
    return equals( lhs, rhs, ZERO_DURATION );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs an epsilon equals comparison.
   * @param lhs The value to compare. Must not be null.
   * @param rhs The value to compare. Must not be null.
   * @param epsilon The epsilon to use. Must not be null.
   * @return true if both values are epsilon equal. Otherwise false is returned.
   */
  public static boolean equals(
      final LocalDateTime lhs,
      final LocalDateTime rhs,
      final Duration epsilon
    ) {
    final long lhsMillis = Date.from( lhs.atZone( ZoneId.systemDefault() ).toInstant() ).getTime();
    final long rhsMillis = Date.from( rhs.atZone( ZoneId.systemDefault() ).toInstant() ).getTime();
    final long epsMillis = epsilon.toMillis();
    final long min = lhsMillis - epsMillis;
    final long max = lhsMillis + epsMillis;

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "LocalDateTime equals(): lhs=" + lhs + " (" + lhsMillis + "), rhs=" + rhs + " (" + rhsMillis + "), epsilon=" + epsilon + " (" + epsMillis + "), min=" + min + ", max=" + max );
    }
    return min <= rhsMillis && rhsMillis <= max;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs an epsilon equals comparison.
   * @param lhs The value to compare. Must not be null.
   * @param rhs The value to compare. Must not be null.
   * @return true if both values are equal. Otherwise false is returned.
   */
  public static boolean equals(
    final Duration lhs,
    final Duration rhs
  ) {
    return equals( lhs, rhs, ZERO_DURATION );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs an epsilon equals comparison.
   * @param lhs The value to compare. Must not be null.
   * @param rhs The value to compare. Must not be null.
   * @param epsilon The epsilon to use. Must not be null.
   * @return true if both values are epsilon equal. Otherwise false is returned.
   */
  public static boolean equals(
      final Duration lhs,
      final Duration rhs,
      final Duration epsilon
    ) {
    final long lhsMillis = lhs.toMillis();
    final long rhsMillis = rhs.toMillis();
    final long epsMillis = epsilon.toMillis();
    final long min = lhsMillis - epsMillis;
    final long max = lhsMillis + epsMillis;

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "Duration equals(): lhs=" + lhs + " (" + lhsMillis + "), rhs=" + rhs + " (" + rhsMillis + "), epsilon=" + epsilon + " (" + epsMillis + "), min=" + min + ", max=" + max );
    }
    return min <= rhsMillis && rhsMillis <= max;
  }
}
