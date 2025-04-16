package com.github.kreutzr.responsediff;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.tools.Converter;

/**
 * Handles calculation operations within variable values.
 */
public class VariablesCalculator
{
  public static final String VALUE__RANDOM_UUID_PREFIX     = "${randomUUID(";
  public static final String VALUE__RANDOM_INTEGER_PREFIX  = "${randomInteger(";
  public static final String VALUE__RANDOM_LONG_PREFIX     = "${randomLong(";
  public static final String VALUE__RANDOM_DOUBLE_PREFIX   = "${randomDouble(";
  public static final String VALUE__RANDOM_DATE_PREFIX     = "${randomDate(";
  public static final String VALUE__RANDOM_DATETIME_PREFIX = "${randomDateTime(";
  public static final String VALUE__RANDOM_BOOLEAN_PREFIX  = "${randomBoolean(";
  public static final String VALUE__RANDOM_ENUM_PREFIX     = "${randomEnum(";
  public static final String VALUE__NOW_DATE_PREFIX        = "${nowDate(";
  public static final String VALUE__NOW_DATETIME_PREFIX    = "${nowDateTime(";

  public static final String TOKEN_TODAY                   = "today";
  public static final String TOKEN_NOW                     = "now";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final String DOLLAR         = "__DOLLAR__";
  private static final String BRACKET        = "__BRACKET__";
  private static final String DOLLAR_BRACKET = DOLLAR + BRACKET;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( VariablesCalculator.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Calculates a variable value if required.
   * @param value The value. May be null.
   * @return A new calculated value (e.g. if value = "${randomUUID()}" (see VALUE__RANDOM_UUID). Otherwise the passed value is returned. If null is passed, null is returned.
   * @throws ParseException
   */
  public static String calculateIfRequired( final String value )
  throws ParseException
  {
    if( value == null ) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();
    final String[] parts = value.replace( "${", DOLLAR_BRACKET ).split( DOLLAR );

    for( final String part : parts ) {
      if( part.startsWith( BRACKET ) ) {
        String functionName = "${" + part.substring( BRACKET.length() );
        final int pos = functionName.indexOf( "}" );
        if( pos < 0 ) {
          throw new RuntimeException( "Variable \"" + functionName + "\" is missing terminating bracket \"}\"." );
        }
        final String tail = functionName.substring( pos+1 );
        functionName = functionName.substring( 0, pos+1 );

        final String calculatedValue = innerCalculateIfRequired( functionName );
        sb.append( calculatedValue );

        // Append tailing string
        sb.append( tail );
      }
      else {
        sb.append( part );
      }
    }

    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Calculates a variable value if required.
   * @param value The value. Must not be null.
   * @return A new calculated value (e.g. if value = "${randomUUID()}" (see VALUE__RANDOM_UUID). Otherwise the passed value is returned.
   * @throws ParseException
   */
  private static String innerCalculateIfRequired( final String value )
  throws ParseException
  {
    final String innerValue = value
      .trim()
      .replaceAll( "\\s", "" );

    if( !innerValue.startsWith( "${" ) ) {
      return value;
    }

    if( innerValue.startsWith( VALUE__RANDOM_UUID_PREFIX ) ) {
      String prefix = "";
      int maxLength = -1;
      List< String > replacements = new ArrayList<>();
      final String[] params = getParams( innerValue, VALUE__RANDOM_UUID_PREFIX );
      // Read prefix parameter
      if( params.length > 0 ) {
        prefix = Converter.asString( params[ 0 ], prefix );
      }
      // Read maxLength parameter
      if( params.length > 1 ) {
        maxLength = Converter.asInteger( params[ 1 ], maxLength );
      }
      // Read replacement parameter pairs
      if( params.length > 2 ) {
        String key = null;
        String replacement = null;
        try {
          for( int i=2; i < params.length; i+=2 ) {
            key         = params[ i ].trim();
            replacement = params[i+1].trim();
            replacements.add( key );         // to be replaced
            replacements.add( replacement ); // replacement
          }
        }
        catch( final IndexOutOfBoundsException ex ) {
          final String message = "Expression \"" + value + "\" is missing replacement parameter for \"" + key + "\".";
          LOG.error( message );
          throw new RuntimeException( message, ex );
        }
      }

      // Handle prefix
      String uuid = prefix + UUID.randomUUID().toString();
      // Handle replacements
      for( int i=0; i < replacements.size(); i+=2 ) {
        uuid = uuid.replaceAll( replacements.get( i ), replacements.get( i+1 ) );
      }
      // Handle maxLength
      if( maxLength > 0 ) {
        uuid = uuid.substring( 0, Math.min( maxLength, uuid.length() ) );
      }
      return uuid;
    }
    else if( innerValue.startsWith( VALUE__RANDOM_INTEGER_PREFIX ) ) {
      int min = Integer.MIN_VALUE;
      int max = Integer.MAX_VALUE;
      final String[] params = getParams( innerValue, VALUE__RANDOM_INTEGER_PREFIX );
      if( params.length > 0 ) {
        min = Converter.asInteger( params[ 0 ], min );
      }
      if( params.length > 1 ) {
        max = Converter.asInteger( params[ 1 ], max );
      }

      return "" + ThreadLocalRandom.current().nextInt( min, max );
    }
    else if( innerValue.startsWith( VALUE__RANDOM_LONG_PREFIX ) ) {
        long min = Long.MIN_VALUE;
        long max = Long.MAX_VALUE;
        final String[] params = getParams( innerValue, VALUE__RANDOM_LONG_PREFIX );
        if( params.length > 0 ) {
          min = Converter.asLong( params[ 0 ], min );
        }
        if( params.length > 1 ) {
          max = Converter.asLong( params[ 1 ], max );
        }

        return "" + ThreadLocalRandom.current().nextLong( min, max );
    }
    else if( innerValue.startsWith( VALUE__RANDOM_DOUBLE_PREFIX ) ) {
      double min = Double.MIN_VALUE;
      double max = Double.MAX_VALUE;
      final String[] params = getParams( innerValue, VALUE__RANDOM_DOUBLE_PREFIX );
      if( params.length > 0 ) {
        min = Converter.asDouble( params[ 0 ], min );
      }
      if( params.length > 1 ) {
        max = Converter.asDouble( params[ 1 ], max );
      }

      return "" + ThreadLocalRandom.current().nextDouble( min, max );
    }
    else if( innerValue.startsWith( VALUE__RANDOM_DATE_PREFIX ) ) {
      long min = parseDate( Constants.ISO_DATE, Constants.MIN_DATE, true ).getTime(); // 1970-01-01
      long max = parseDate( Constants.ISO_DATE, Constants.MAX_DATE, true ).getTime(); // 2999-12-31

      final DateTimeFormatter dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATE );
      String today = LocalDate.now().format( dtf );

      final String[] params = getParams( innerValue, VALUE__RANDOM_DATE_PREFIX );
      if( params.length > 0 ) {
        if( params[ 0 ].startsWith( TOKEN_TODAY ) ) {
          final Integer offset = getParamOffset( params[ 0 ] );
          if( offset != null ) {
            today = LocalDate.now().plusDays( offset ).format( dtf );
          }
          params[ 0 ] = today;
        }
        min = parseDate( Constants.ISO_DATE, params[ 0 ], true ).getTime();
      }
      if( params.length > 1 ) {
        if( params[ 1 ].startsWith( TOKEN_TODAY ) ) {
          final Integer offset = getParamOffset( params[ 1 ] );
          if( offset != null ) {
            today = LocalDate.now().plusDays( offset ).format( dtf );
          }
          params[ 1 ] = today;
        }
        max = parseDate( Constants.ISO_DATE, params[ 1 ], true ).getTime();
      }

      if( min == max ) {
        max = min + 1; // Avoid error in "ThreadLocalRandom.current().nextLong( min, max )" - see below
      }

      final Date date = new Date( ThreadLocalRandom.current().nextLong( min, max ) );
      final DateFormat df = createDateFormat( Constants.ISO_DATE );
      return df.format( date );
    }
    else if( innerValue.startsWith( VALUE__RANDOM_DATETIME_PREFIX ) ) {
      long min = parseDate( Constants.ISO_DATETIME, Constants.MIN_DATE + Constants.MIN_DATETIME, true ).getTime(); // 1970-01-01T00:00:00.000
      long max = parseDate( Constants.ISO_DATETIME, Constants.MAX_DATE + Constants.MAX_DATETIME, true ).getTime(); // 2999-12-31T23:59:59.999

      final DateTimeFormatter dtf = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );
      String now = LocalDateTime.now().format( dtf );

      final String[] params = getParams( innerValue, VALUE__RANDOM_DATETIME_PREFIX );
      if( params.length > 0 ) {
        if( params[ 0 ].startsWith( TOKEN_NOW ) ) {
          final Integer offset = getParamOffset( params[ 0 ] );
          if( offset != null ) {
            now = LocalDateTime.now().plus( offset, ChronoUnit.MILLIS ).format( dtf );
          }
          params[ 0 ] = now;
        }
        min = parseDate( Constants.ISO_DATETIME, params[ 0 ], true ).getTime();
      }
      if( params.length > 1 ) {
        if( params[ 1 ].startsWith( TOKEN_NOW ) ) {
          final Integer offset = getParamOffset( params[ 1 ] );
          if( offset != null ) {
            now = LocalDateTime.now().plus( offset, ChronoUnit.MILLIS ).format( dtf );
          }
          params[ 1 ] = now;
        }
        max = parseDate( Constants.ISO_DATETIME, params[ 1 ], true ).getTime();
      }

      if( min == max ) {
        max = min + 1; // Avoid error in "ThreadLocalRandom.current().nextLong( min, max )" - see below
      }

      final Date date = new Date( ThreadLocalRandom.current().nextLong( min, max ) );
      final DateFormat df = createDateFormat( Constants.ISO_DATETIME );
      return df.format( date );
    }
    else if( innerValue.startsWith( VALUE__RANDOM_BOOLEAN_PREFIX ) ) {
      return "" + ThreadLocalRandom.current().nextBoolean();
    }
    else if( innerValue.startsWith( VALUE__RANDOM_ENUM_PREFIX ) ) {
      final String[] params = getParams( innerValue, VALUE__RANDOM_ENUM_PREFIX );
      if( params.length > 0 ) {
        return params[ ThreadLocalRandom.current().nextInt( params.length ) ];
      }
      // The value remains unchanged.
      return value;
    }
    else if( innerValue.startsWith( VALUE__NOW_DATE_PREFIX ) ) {
      long offset = 0;

      final String[] params = getParams( innerValue, VALUE__NOW_DATE_PREFIX );
      if( params.length > 0 ) {
        offset = Converter.asLong( params[ 0 ], offset );
      }

      final LocalDate date = LocalDate.now().plusDays( offset );
      final DateTimeFormatter df = DateTimeFormatter.ofPattern( Constants.ISO_DATE );
      return date.format( df );
    }
    else if( innerValue.startsWith( VALUE__NOW_DATETIME_PREFIX ) ) {
      long offset = 0;

      final String[] params = getParams( innerValue, VALUE__NOW_DATETIME_PREFIX );
      if( params.length > 0 ) {
        offset = Converter.asLong( params[ 0 ], offset );
      }

      final LocalDateTime dateTime = LocalDateTime.now().plus( offset, ChronoUnit.MILLIS );
      final DateTimeFormatter df = DateTimeFormatter.ofPattern( Constants.ISO_DATETIME );
      return dateTime.format( df );
    }

    return value;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static DateFormat createDateFormat( final String pattern )
  {
     final SimpleDateFormat dateFormat = new SimpleDateFormat( pattern );
     return dateFormat;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Parses the given String by the given pattern.
   *
   * @param pattern      The parse pattern. Must not be null.
   * @param dateAsString The date representation to parse. Must not be null.
   * @param parseWeak    Flag if invalid dates shall be accepted (true) (e.g.
   *                     2016-12-00 => 2016-11-30) or rejected (false) .
   * @return The parsed Date. If an error occurs, an exception is thrown.
   * @throws ParseException
   */
  private static Date parseDate(
     final String pattern,
     final String dateAsString,
     final boolean parseWeak
  )
  throws ParseException
  {
    final DateFormat format = createDateFormat( pattern );
    format.setLenient( parseWeak );
    return format.parse( dateAsString );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Parses parameters that were optionally passed with the given value (e.g. "${randomLong( -100, 200 )}" => [ "-100", "200" ] )
   * @param value The value to inspect. Must not be null.
   * @param prefix The value prefix. Must not be null.
   * @return An array of all found (trimmed) parameters. May be empty but never null.
   */
  private static String[] getParams( final String value, final String prefix )
  {
    final String[] params = value.substring( prefix.length() )
      .replaceAll( "\\)", "" )
      .replaceAll( "\\}", "" )
      .split( "," );

    for( int i=0; i < params.length; i++ ) {
      params[ i ] = params[ i ].trim();
    }

    if( params.length == 1 && params[ 0 ].isEmpty() )
    {
      return new String[ 0 ];
    }

    return params;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Parses a parameter for an optional offset.
   * @param value The value to inspect. Must not be null.
   * @return The optional offset. May be null.
   */
  private static Integer getParamOffset( final String value )
  {
    String[] parts = value
      .replaceAll( "\\s", "" )
      .split( "\\+" );

    if( parts.length == 2 )
    {
      return Integer.valueOf( parts[ 1 ] );
    }

    parts = value
        .replaceAll( "\\s", "" )
        .split( "\\-" );

    if( parts.length == 2 ) {
      return Integer.valueOf( parts[ 1 ] ) * -1;
    }

    return null;
  }
}
