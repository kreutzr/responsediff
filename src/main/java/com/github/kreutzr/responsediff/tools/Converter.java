package com.github.kreutzr.responsediff.tools;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Helper class to convert a given object to a target data type. By default a
 * fallback is returned if the passed object is null or could not be converted.
 */
public class Converter
{
  public static final int THROW_CONVERSION_EXCEPTION = 1;

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if a given option is set.
   * @param option  The requested option.
   * @param options The options set.
   * @return Either true, if the given option is set or false.
   */
  private static boolean isOptionSet(
     final int option, final int[] options
  ){
     for( int i = 0; i < options.length; i++ ) {
        if( options[ i ] == option ) {
           return true;
        }
     }
     return false;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static String asString(
      final Object value, final String fallback, final int... options
   ){
      if( value == null ) {
         return fallback;
      }
      if( value instanceof String ) {
         return (String) value;
      }
      try {
         return value.toString();
      }
      catch( final Exception ex ) {
         if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
            throw new IllegalArgumentException( ex );
         }
         return fallback;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static Date asDate(
      final Object value, final Date fallback, final int... options
   ){
      if( value == null ) {
         return fallback;
      }
      if( value instanceof Date ) {
         return (Date) value;
      }
      try {
         final String valueAsString = value.toString().trim();
         final int    length        = valueAsString.length();
         if( length != 10 ) {
            throw new IllegalArgumentException(
               "Parameter \"" + valueAsString + "\" has invalid length of " + length + "." );
         }
         return FormatHelper.parseIsoDate( valueAsString );
      }
      catch( final Exception ex ) {
         if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
            throw new IllegalArgumentException( ex );
         }
         return fallback;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static LocalDate asLocalDate(
      final Object value, final LocalDate fallback, final int... options
   ){
     if( value == null ) {
       return fallback;
     }
     if( value instanceof LocalDate ) {
        return (LocalDate) value;
     }
     try {
       final String valueAsString = value.toString().trim();
       final int    length        = valueAsString.length();
        if( length != 10 ) {
           throw new IllegalArgumentException(
              "Parameter \"" + valueAsString + "\" has invalid length of " + length + "." );
        }
        return LocalDate.parse( valueAsString );
     }
     catch( final Exception ex ) {
        if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
           throw new IllegalArgumentException( ex );
        }
        return fallback;
     }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static LocalDateTime asLocalDateTime(
      final Object value, final LocalDateTime fallback, final int... options
   ){
     if( value == null ) {
       return fallback;
     }
     if( value instanceof LocalDateTime ) {
        return (LocalDateTime) value;
     }
     try {
       String valueAsString = value.toString().trim();

       // Cut off tailing milliseconds and time zone
       {
         int pos = Integer.MAX_VALUE;
         int posEnd = -1;
         posEnd = valueAsString.indexOf( "." ); // Millis
         if( posEnd > 0 ) {
           pos = Math.min( posEnd, pos );
         }
         posEnd = valueAsString.indexOf( "+" ); // time zone
         if( posEnd > 0 ) {
           pos = Math.min( posEnd, pos );
         }
         posEnd = valueAsString.lastIndexOf( "-" ); // time zone
         if( posEnd >= 19 ) { // 19 = length of ("yyyy-mm-ddThh:mm:ss")
           pos = Math.min( posEnd, pos );
         }
         posEnd = valueAsString.indexOf( "Z" ); // time zone
         if( posEnd > 0 ) {
           pos = Math.min( posEnd, pos );
         }

         if( pos > 0 && pos != Integer.MAX_VALUE ) {
           valueAsString = valueAsString.substring(0, pos );
         }
       }

       final int length = valueAsString.length();
        if( length != 19 ) {
           throw new IllegalArgumentException(
              "Parameter \"" + valueAsString + "\" has invalid length of " + length + "." );
        }
        return LocalDateTime.parse( valueAsString );
     }
     catch( final Exception ex ) {
        if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
           throw new IllegalArgumentException( ex );
        }
        return fallback;
     }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static Duration asDuration(
      final Object value, final Duration fallback, final int... options
   ){
     if( value == null ) {
       return fallback;
     }
     if( value instanceof Duration ) {
        return (Duration) value;
     }
     try {
       final String valueAsString = value.toString().trim();
       return Duration.parse( valueAsString );
     }
     catch( final Exception ex ) {
        if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
           throw new IllegalArgumentException( ex );
        }
        return fallback;
     }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static Long asLong(
      final Object value, final Long fallback, final int... options
   ){
      if( value == null ) {
         return fallback;
      }
      if( value instanceof Number ) {
         return ( (Number) value ).longValue();
      }
      try {
         return Long.parseLong( value.toString().trim() );
      }
      catch( final NumberFormatException ex ) {
         if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
            throw new IllegalArgumentException( ex );
         }
         return fallback;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static Integer asInteger(
      final Object value, final Integer fallback, final int... options
   ){
      if( value == null ) {
         return fallback;
      }
      if( value instanceof Number ) {
         return ( (Number) value ).intValue();
      }
      try {
         return Integer.parseInt( value.toString().trim() );
      }
      catch( final NumberFormatException ex ) {
         if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
            throw new IllegalArgumentException( ex );
         }
         return fallback;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static Double asDouble(
      final Object value, final Double fallback, final int... options
   ){
      if( value == null ) {
         return fallback;
      }
      if( value instanceof Number ) {
         return ( (Number) value ).doubleValue();
      }
      try {
         return Double.parseDouble( value.toString().trim() );
      }
      catch( final Exception ex ) {
         if( isOptionSet( THROW_CONVERSION_EXCEPTION, options ) ) {
            throw new IllegalArgumentException( ex );
         }
         return fallback;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param value    The value to convert. "true", "false" are accepted case insensitive.
    * @param fallback A fallback value.
    * @param options Some options to define the methods behavior.
    * @return Either the converted value or the fallback value.
    */
   public static Boolean asBoolean(
      final Object value, final Boolean fallback, final int... options
   ){
      if( value == null ) {
         return fallback;
      }
      if( value instanceof Boolean ) {
         return (Boolean) value;
      }
      Object obj = value;
      try {
         // Try as String representation.
         final Boolean bool = booleanFromString( obj.toString() );
         if( bool != null ) {
            return bool;
         }
         throw new IllegalArgumentException(
            "Parameter \"" + obj.toString() + "\" can not be parsed as Boolean." );
      }
      catch( final Exception ex ) {
         return fallback;
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Converts a given String into a Boolean.
    *
    * @param value The value. Must not be null.
    * @return Either TRUE or FALSE if successful. Otherwise null.
    */
   private static Boolean booleanFromString(
      final String value
   ){
      final String v = value.trim().toUpperCase();
      if( v.equals( "TRUE" ) ) {
         return true;
      }
      else if( v.equals( "FALSE" ) ) {
         return false;
      }
      return null;
   }
}
