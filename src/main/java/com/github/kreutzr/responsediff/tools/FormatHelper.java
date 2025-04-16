package com.github.kreutzr.responsediff.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * handles date parsing and formatting
 *
 * CAUTION: (Simple)DateFormat is NOT thread safe and therefore must NOT be used static!
 */
public class FormatHelper
{
   public static final String ISO_DATE_FORMAT      = "yyyy-MM-dd";
   public static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Parses a given String by ISO_DATE_FORMAT.
    * @param dateAsString The date representation to parse. Must not be null.
    * @return The parsed Date.
    * @throws ParseException
    */
   public static Date parseIsoDate(
      final String dateAsString
   )
   throws ParseException
   {
     final SimpleDateFormat dateFormat = new SimpleDateFormat( ISO_DATE_FORMAT );
     dateFormat.setLenient( false );
     return dateFormat.parse( dateAsString );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Formats the given date to ISO.
    * @param date The date to format. Must not be null.
    * @return The formatted date.
    */
   public static String formatIsoDateTime(
     final Date date
   ){
     final SimpleDateFormat dateFormat = new SimpleDateFormat( ISO_DATE_TIME_FORMAT  );
     return dateFormat.format( date );
   }
}
