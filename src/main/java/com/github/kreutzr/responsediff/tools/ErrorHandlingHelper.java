package com.github.kreutzr.responsediff.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Provides some support for error handling.
 */
public class ErrorHandlingHelper
{
   public final static String DEFAULT_MESSAGE = "An exception occurred:";
   public final static String LINE_BREAK_MASK = " | ";

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Creates a log message (<b>with a default message text</b>) without line
    * breaks.
    * 
    * @param ex      The Throwable to handle. May be null.
    * @return The message and the stacktrace as one single line.
    */
   public static final String createSingleMessageLine(
      final Throwable ex
   ){
      return createSingleLineMessage( DEFAULT_MESSAGE, ex, LINE_BREAK_MASK );
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Creates a log message as one single line.
    * 
    * @param message The main message.
    * @param ex      The Throwable to handle. May be null.
    * @return The message and the stacktrace as one single line.
    */
   public static final String createSingleLineMessage(
      final String message,
      final Throwable ex
   ){
      return createSingleLineMessage( message, ex, LINE_BREAK_MASK );
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Creates a log message as one single line.
    * 
    * @param message       The main message.
    * @param ex            The Throwable to handle. May be null.
    * @param lineBreakMask The line break mask to use. May be null.
    * @return The message and the stacktrace as one single line.
    */
   public static final String createSingleLineMessage(
      final String message,
      final Throwable ex,
      final String lineBreakMask
   ){
      return createMessage( message, ex )
         .replaceAll( "\n", lineBreakMask != null ? lineBreakMask : LINE_BREAK_MASK )
         .replaceAll( "\t", " " );
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Creates a log message
    * 
    * @param message The main message.
    * @param ex      The Throwable to handle. May be null.
    * @return The message and the stacktrace.
    */
   public static final String createMessage(
      final String message, final Throwable ex
   ){
      final StringBuilder sb = new StringBuilder( message != null ? message : DEFAULT_MESSAGE );
      if( ex != null ) {
         sb.append( " Exception: " ).append( ex.getClass().getSimpleName() )
           .append( " Message: " ).append( ex.getMessage() )
           .append( " StackTrace: " ).append( getStackTraceFromThrowable( ex ) );
      }
      return sb.toString();
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Gets the stacktrace of a given Throwable.
    * 
    * @param ex The throwable to read the stacktrace from. Must not be null.
    * @return The stacktrace of the given Throwable.
    */
   public static final String getStackTraceFromThrowable(
      final Throwable ex
   ){
      final Writer      writer      = new StringWriter();
      final PrintWriter printWriter = new PrintWriter( writer );
      ex.printStackTrace( printWriter );
      return writer.toString();
   }
}
