package com.github.kreutzr.responsediff;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.kreutzr.responsediff.tools.ComparatorHelper;
import com.github.kreutzr.responsediff.tools.Converter;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Handles the Response validation.
 */
public class ValidationHandler
{
   private   static final String WHITE_NOISE_TOKEN    = "White Noise";
   protected static final String EXPECTED_VALUE_TOKEN = "Expected value";
             static final String IGNORE_HEADER_TOKEN  = "Ignore header";
   private   static final String IGNORE_PATH_TOKEN    = "Ignore path";

   private static final Logger LOG = LoggerFactory.getLogger( ValidationHandler.class );

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Computes white noise differences between the reference and the control response.
    * @param referenceResponse The reference response. May be null.
    * @param controlResponse The control response. May be null.
    * @param epsilon The epsilon for decimal comparison. Must not be null.
    * @param testId The current test id. Must not be null.
    * @return A JsonDiff that holds all white noise differences. Never null.
    * @throws JsonProcessingException
    * @throws JsonMappingException
    */
   static JsonDiff getWhiteNoise(
       final XmlHttpResponse referenceResponse,
       final XmlHttpResponse controlResponse,
       final double epsilon,
       final String testId
   )
   throws JsonMappingException, JsonProcessingException
   {
     if( ( referenceResponse == null || referenceResponse.getBody() == null || referenceResponse.getBody().trim().isEmpty() )
      || ( controlResponse   == null || controlResponse  .getBody() == null || controlResponse  .getBody().trim().isEmpty() )
     ) {
       return JsonDiff.createDataInstance();
     }

     final JsonDiff whiteNoise = validateResponse(
       null,              // xmlResponse
       null,              // xmlTest
       null,              // testFileName
       referenceResponse,
       controlResponse,
       null,              // whiteNoise
       null,              // ignoreHeaders
       null,              // ignorePaths
       true,              // checkOnlyUnexpected (Do not put mismatches in expected values into whiteNoise!)
       epsilon,
       false,             // reportWhiteNoise
       testId
     );

     if( whiteNoise.hasDifference() ) {
       for( final JsonDiffEntry entry : whiteNoise.getChanges() ) {
         entry.setMessage( WHITE_NOISE_TOKEN );
       }
       for( final JsonDiffEntry entry : whiteNoise.getAdditions() ) {
         entry.setMessage( WHITE_NOISE_TOKEN );
       }
       for( final JsonDiffEntry entry : whiteNoise.getDeletions() ) {
         entry.setMessage( WHITE_NOISE_TOKEN );
       }
     }

     return whiteNoise;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Compares a candidate against a reference response and finds all relevant differences.
    * All found differences are checked against the optional white Noise differences
    * and only non white noise (irrelevant) differences are returned.
    * @param xmlResponse The XmlResponse that shall be used for validation. May be null (e.g. for white noise computation).
    * @param xmlTest The current XmlTest. May be null.
    * @param testFileName The file name the current test is configured in. May be null.
    * @param candidateResponse The candidate response. May be null.
    * @param referenceResponse The reference response. May be null.
    * @param whiteNoise A JsonDiff object that holds all irrelevant JsonPaths. May be null.
    * @param ignorePaths A Set of paths to ignore additionally. May be null.
    * @param ignoreHeaders A Set of "header paths" to ignore additionally. May be null.
    * @param checkOnlyUnexpected Flag, if only unexpected changes shall be identified (true) (for whiteNoise computation) or expected values shall be checked, too (false).
    * @param epsilon The epsilon for decimal comparison. Must not be null.
    * @param reportWhiteNoise Flag, if any different value shall be reported (true) or only those that were not discovered to be white noise (differences between reference and control, or expected differences) (false).
    * @param testId The current test id. Must not be null.
    * @return A JsonDiff object that holds all relevant differences. Never null.
    * @throws JsonProcessingException
    * @throws JsonMappingException
    */
   protected static JsonDiff validateResponse(
     final XmlResponse     xmlResponse,
     final XmlTest         xmlTest,
     final String          testFileName,
     final XmlHttpResponse candidateResponse,
     final XmlHttpResponse referenceResponse,
     final JsonDiff        whiteNoise,
     final Set< String >   ignorePaths,
     final Set< String >   ignoreHeaders,
     final boolean         checkOnlyUnexpected,
     final double          epsilon,
     final boolean         reportWhiteNoise,
     final String          testId
   )
   throws JsonMappingException, JsonProcessingException
   {
     if( LOG.isTraceEnabled() ) {
       LOG.trace("validateResponse() whiteNoise=" + whiteNoise + ", ignorePaths=" + ignorePaths + ", ignoreHeaders=" + ignoreHeaders + ", testId=" + testId );
     }

     final JsonDiff relevantDiffs = JsonDiff.createDataInstance();

     if( candidateResponse == null
      || referenceResponse == null
     ) {
       return relevantDiffs;
     }

     final JsonDiff innerWhiteNoise = JsonDiff.createDataInstance();
     if( whiteNoise != null ) {
       innerWhiteNoise.join( whiteNoise );
     }

     // -------------------------------------------------------------------
     // Handle "<expected>"
     // -------------------------------------------------------------------
     if( xmlResponse != null && xmlResponse.getExpected() != null && !checkOnlyUnexpected ) {
       final XmlExpected xmlExpected = xmlResponse.getExpected();

       // Validate expected HTTP status
       if( xmlExpected.getHttpStatus() != null ) {
         relevantDiffs.incrementExpectedCount();

         final XmlHttpStatus xmlHttpStatus = xmlExpected.getHttpStatus();
         TestSetHandler.joinTicketReferences( xmlTest, xmlHttpStatus.getTicketReference() );

         final int actual = candidateResponse.getHttpStatus().getValue();
         final int expect = xmlHttpStatus.getValue();
         final String jsonPath = "$." +ToJson.HEADERS_SUBPATH + "." + ToJson.HEADER_HTTPSTATUS;

         final String errorMessage = "Http status expected: ### " + formatHttpStatus( expect ) + " but was: " + formatHttpStatus( actual );
         String  message = EXPECTED_VALUE_TOKEN;
         boolean expectationMismatch = false;

         if( actual != expect ) {
           expectationMismatch = true;
         }

         message             = handleInverse( expectationMismatch, xmlHttpStatus.isCheckInverse(), errorMessage );
         expectationMismatch = handleInverse( expectationMismatch, xmlHttpStatus.isCheckInverse() );

         if( expectationMismatch ) {
           final JsonDiffEntry jsonDiffEntry = new JsonDiffEntry(
               jsonPath,
               ""+actual, // actual
               ""+expect, // expected
               message    // message
           );
           relevantDiffs  .getChanges().add( jsonDiffEntry );
           innerWhiteNoise.getChanges().add( jsonDiffEntry ); // Avoid double entries due to unexpected header differences
         }
         else {
           innerWhiteNoise.getChanges().add( new JsonDiffEntry(
               jsonPath,
               ""+actual, // actual
               ""+expect, // expected
               EXPECTED_VALUE_TOKEN // message
           ) );
         }
       }

       // Validate expected response headers
       if( xmlExpected.getHeaders() != null ) {
         // Create JSON from candidate headers
         final String headersAsJson = ToJson.fromHeaders( candidateResponse.getHeaders(), true );
         final JsonPathHelper jph = new JsonPathHelper( headersAsJson );

         // Lookup expected headers
         for( final XmlHeader xmlHeader : xmlExpected.getHeaders().getHeader() ) {
           // We allow all checking options for headers as for values => We create a temporary XmlValue from the header and check the XmlValue.
           final XmlValue xmlValue = ToXmlValue.fromHeader( xmlHeader );
           final double localEpsilon = Converter.asDouble( xmlValue.getEpsilon(), epsilon );

           // Mark JsonPaths of expected values as handled if matching
           final List< JsonDiffEntry > whiteNoiseEntries = checkExpected( jph, xmlValue, xmlTest, testFileName, relevantDiffs, localEpsilon );
           if( !whiteNoiseEntries.isEmpty() ) {
             innerWhiteNoise.getChanges().addAll( whiteNoiseEntries );
           }
         }
       }

       // Validate expected maximum request duration
       if( xmlExpected.getMaxDuration() != null ) {
         relevantDiffs.incrementExpectedCount();

         final Duration maximumDuration = Duration.parse( xmlExpected.getMaxDuration() );
         final Duration requestDuration = Duration.parse( candidateResponse.getRequestDuration() );
         if( ( requestDuration.getSeconds() >  maximumDuration.getSeconds() )
          || ( requestDuration.getSeconds() == maximumDuration.getSeconds()
               && requestDuration.getNano() >  maximumDuration.getNano() )
         ) {
           final String requestDurationString = requestDuration.toString();
           final String maximumDurationString = maximumDuration.toString();
           relevantDiffs.getChanges().add( new JsonDiffEntry(
               "", // jsonPath
               requestDurationString,
               maximumDurationString,
               "Maximum request duration expected: " + maximumDurationString + " but was: " + requestDurationString
           ) );
         }
       }

       // Validate expected response body values
       if( !xmlResponse.isHideBody() ) {
         if( xmlExpected.getValues() != null ) {
           if( candidateResponse.isBodyIsJson() && candidateResponse.getBody() != null && !candidateResponse.getBody().trim().isEmpty() ) {
             final JsonPathHelper jph = new JsonPathHelper( candidateResponse.getBody() ); // This is expensive!

             for( final XmlValue xmlValue : xmlExpected.getValues().getValue() ) {
               relevantDiffs.incrementExpectedCount();

               // Avoid extensive debugging due to tailing spaces!
               xmlValue.setPath( xmlValue.getPath().trim() );

               final double localEpsilon = Converter.asDouble( xmlValue.getEpsilon(), epsilon );

               // Mark JsonPaths of expected values as handled if matching
               final List< JsonDiffEntry > whiteNoiseEntries = checkExpected( jph, xmlValue, xmlTest, testFileName, relevantDiffs, localEpsilon );
               if( !whiteNoiseEntries.isEmpty() ) {
                 innerWhiteNoise.getChanges().addAll( whiteNoiseEntries );
               }
             }
           }
           else {
             final String errorMessage = "Values expected but body is empty or not JSON.";
             relevantDiffs.getChanges().add( new JsonDiffEntry( "$", "", "", errorMessage ) );
           }
         }

         // Validate expected response body (entirely)
         if( xmlExpected.getBody() != null ) {
           relevantDiffs.incrementExpectedCount();

           final XmlBody xmlBody = xmlExpected.getBody();
           TestSetHandler.joinTicketReferences( xmlTest, xmlBody.getTicketReference() );

           if( xmlBody.isNoBody() ) {
             if( candidateResponse.getBody() != null && !candidateResponse.getBody().trim().isEmpty() ) {
               final String errorMessage = "Body expected: null or empty but was: not empty";
               relevantDiffs.getAdditions().add( new JsonDiffEntry( "$", "", "", errorMessage ) );
             }
           }
           else {
             if( candidateResponse.getBody() == null || candidateResponse.getBody().trim().isEmpty() ) {
               final String errorMessage = "Body expected: not null nor empty but was: null or empty";
               relevantDiffs.getDeletions().add( new JsonDiffEntry( "$", "", "", errorMessage ) );
             }
             else {
               // NEEDS FIX B: Be more tolerant here (ignore any white spaces and line breaks)
               if( !xmlBody.getValue().equals( candidateResponse.getBody() ) ) {
                 final String errorMessage = "Body expected: " + xmlBody.getValue() + " + but was: " + candidateResponse.getBody();
                 relevantDiffs.getChanges().add( new JsonDiffEntry( "$", "", "", errorMessage ) );
               }
             }
           }
         }
       }
       else {
         logConfigurationConflicts( xmlResponse, testId, "hidden" );
       }
     }

     // -------------------------------------------------------------------
     // Handle unexpected differences
     // -------------------------------------------------------------------

     if( LOG.isTraceEnabled() ) {
       LOG.trace( "validateResponse(): After expected checks: relevantDiffs=" + relevantDiffs.toString() );
     }

     // Calculate header differences
     final JsonDiff headerIgnore = createIgnoreJsonDiff( innerWhiteNoise, ignoreHeaders, ToJson.HEADERS_SUBPATH, IGNORE_HEADER_TOKEN );
     relevantDiffs.join( validateJson(
       ToJson.fromHeaders( candidateResponse.getHeaders(), true ),
       ToJson.fromHeaders( referenceResponse.getHeaders(), true ),
       headerIgnore,
       reportWhiteNoise,
       testId
     ) );

     if( LOG.isTraceEnabled() ) {
       LOG.trace( "validateResponse(): After header checks: relevantDiffs=" + relevantDiffs.toString() );
     }

     // Calculate body differences (if bodies are both JSON and not empty)
     if( ( candidateResponse.isBodyIsJson() && candidateResponse.getBody() != null && !candidateResponse.getBody().trim().isEmpty() )
      && ( referenceResponse.isBodyIsJson() && referenceResponse.getBody() != null && !referenceResponse.getBody().trim().isEmpty() )
      && ( xmlResponse == null || !xmlResponse.isHideBody() )
     ) {
       final JsonDiff pathIgnore = createIgnoreJsonDiff( innerWhiteNoise, ignorePaths, null, IGNORE_PATH_TOKEN );
       relevantDiffs.join( validateJson(
          candidateResponse.getBody(),
          referenceResponse.getBody(),
          pathIgnore,
          reportWhiteNoise,
          testId
       ) );
     }

     if( LOG.isTraceEnabled() ) {
       LOG.trace( "validateResponse(): After body check: relevantDiffs=" + relevantDiffs.toString() );
     }

     return relevantDiffs;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private static String formatHttpStatus( final int httpStatus )
   {
     return new StringBuilder()
       .append( httpStatus )
       .append( " (" )
       .append( HttpStatus.valueOf( httpStatus ).getMessage() )
       .append( ")" )
       .toString();
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Logs for configuration conflicts.
    * @param xmlResponse The XmlReponse to use. Must not be null.
    * @param testId The current test id. Must not be null.
    * @param reason A conflict reason to log. Must not be null.
    */
   private static void logConfigurationConflicts(
     final XmlResponse xmlResponse,
     final String testId,
     final String reason
   )
   {
     if( !LOG.isWarnEnabled() ) {
       return;
     }

     if(  xmlResponse.getExpected() != null
      &&  xmlResponse.getExpected().getValues() != null
      && !xmlResponse.getExpected().getValues().getValue().isEmpty()
     ) {
       LOG.warn( "Test \"" + testId + "\": Expected values for " + reason + " response body are skipped. You probably want to remove this configuration." );
     }

     if(  xmlResponse.getIgnore() != null
      && !xmlResponse.getIgnore().isEmpty()
     ) {
      LOG.warn( "Test \"" + testId + "\": Ignore paths for " + reason + " response body are skipped. You probably want to remove this configuration." );
     }
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * IMPORTANT: Invoke this BEFORE handleInverse for expectationMismatch was called.
    * @param expectationMismatch Flag, if an expectation mismatch has occurred.
    * @param checkInverse Flag, if the value of expectationMismatch shall be negated (true) or not (false).
    * @param message The message that shall be used if a success message needs to be changed to an error message.
    * @return The message to use depending on the checkInverse value.
    */
   private static String handleInverse(
     final boolean expectationMismatch,
     final boolean checkInverse,
     final String message
   )
   {
     if(  (  checkInverse &&  expectationMismatch )     // Here we need to reset the passed message.
       || ( !checkInverse && !expectationMismatch ) ) { // Here message has already the same value.
       return EXPECTED_VALUE_TOKEN;
     }

     final String replacement = checkInverse ? "Not " : "";
     return message.replaceAll( "### ", replacement );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * IMPORTANT: Invoke this AFTER handleInverse for message was called.
    * @param expectationMismatch Flag, if an expectation mismatch has occurred.
    * @param checkInverse Flag, if the value of expectationMismatch shall be negated (true) or not (false).
    * @return If checkInverse is true, the negation of expectationMismatch is returned. Otherwise expectationMismatch is returned.
    */
   private static boolean handleInverse(
     final boolean expectationMismatch,
     final boolean checkInverse
   )
   {
     return !checkInverse
       ? expectationMismatch
       : !expectationMismatch;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Checks if the actual value equals to the expected value.
    * @param jph The JsonPathHelper to use. Must not be null.
    * @param xmlValue The XmlValue object that holds the JsonPath and the expected value (including type information). Must not be null.
    * @param xmlTest The current XmlTest. May be null.
    * @param testFileName The file name the current test is configured in. May be null.
    * @param jsonDiff The JsonDiff into which mismatches from the expected values are written. Must not be null.
    * @param epsilon The epsilon to use.
    * @return A list that holds all JsonDiffEntries where the actual value matches the expected value. May be empty but never null. Mismatches are added to the passed jsonDiff parameter. Since the xmlValue's path may apply to multiple JSON nodes, a List is required here.
    */
   public static List< JsonDiffEntry > checkExpected(
     final JsonPathHelper  jph,
     final XmlValue        xmlValue,
     final XmlTest         xmlTest,
     final String          testFileName,
     final JsonDiff        jsonDiff,
     final double          epsilon
   )
   {
     if( LOG.isTraceEnabled() ) {
       LOG.trace( "checkExpected() xmlValue=" + xmlValue.getValue() );
     }

     final List< JsonDiffEntry > list = new ArrayList<>();

     TestSetHandler.joinTicketReferences( xmlTest, xmlValue.getTicketReference() );

     try {
       final Object object = jph.getValue( xmlValue.getPath() ); // This is expensive. Therefore we read it only once.

       if( object instanceof List && object != null ) { // null is handled as single value
         @SuppressWarnings("unchecked")
         final List< Object > actualObjects = (List< Object >) object;

         for( int i=0; i < actualObjects.size(); i++ ) {
           final Object actualObject = actualObjects.get( i );
           final String jsonPath     = xmlValue.getPath() + "[" + i + "]";

           final JsonDiffEntry listEntry = innerCheckExpected( jph, actualObject, jsonPath, xmlValue, xmlTest, testFileName, jsonDiff, epsilon );
           if( listEntry != null ) {
             list.add( listEntry );
           }
         }
       }
       else {
         final Object actualObject = object;
         final String jsonPath     = xmlValue.getPath();

         final JsonDiffEntry listEntry = innerCheckExpected( jph, actualObject, jsonPath, xmlValue, xmlTest, testFileName, jsonDiff, epsilon );
         if( listEntry != null ) {
           list.add( listEntry );
         }
       }
     }
     catch( final PathNotFoundException ex ) {
       final Object actualObject = null;
       final String jsonPath     = xmlValue.getPath();

       final JsonDiffEntry listEntry = innerCheckExpected( jph, actualObject, jsonPath, xmlValue, xmlTest, testFileName, jsonDiff, epsilon );
       if( listEntry != null ) {
         list.add( listEntry );
       }
     }

     return list;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Checks if the actual value equals to the expected value.
    * @param jph The JsonPathHelper to read the actual value from. Must not be null.
    * @param actualObject The actual object to check. May be null.
    * @param jsonPath The JsonPath of the given actual object. Must not be null.
    * @param xmlValue The XmlValue object that holds the JsonPath and the expected value (including type information). Must not be null.
    * @param xmlTest The current XmlTest. May be null.
    * @param testFileName The file name the current test is configured in. May be null.
    * @param jsonDiff The JsonDiff into which mismatches from the expected values are written. Must not be null.
    * @param epsilon The epsilon to use.
    * @return A JsonDiffEntry if the actual value matches the expected value. Otherwise null is returned. Mismatches are added to the passed jsonDiff parameter.
    */
   private static JsonDiffEntry innerCheckExpected(
     final JsonPathHelper jph,
     final Object         actualObject,
     final String         jsonPath,
     final XmlValue       xmlValue,
     final XmlTest        xmlTest,
     final String         testFileName,
     final JsonDiff       jsonDiff,
     final double         epsilon
   )
   {
     String actualValue = "TYPE_MISMATCH_IN_TEST_DEFINITION";
     String expectValue = "";

     final boolean checkInverse = xmlValue.isCheckInverse();

     String  errorMessage        = null;
     String  message             = EXPECTED_VALUE_TOKEN;
     boolean expectationMismatch = false;
     boolean checkValue          = true;

     // Plausibility check
     if( ( xmlValue.isCheckPathExists() || xmlValue.isCheckIsNull() ) && xmlValue.getValue() != null && !xmlValue.getValue().isEmpty() ) {
       message = "A value check must not be combined with checkIsNull or checkPathExits."
         + " Use an additional value XML element to check the value.";
       expectationMismatch = true;
       checkValue = false;
     }

     // NOTE: Check if path exists before a null check, because this check is stronger!
     if( xmlValue.isCheckPathExists() && !expectationMismatch ) {
       final String actual = jph.hasPath( xmlValue.getPath() ) ? "exists" : "Not exists";
       final String expect = "exists";
       errorMessage = "Path expected: ### " + expect + " but was: " + actual;
       if( !actual.equals( expect ) ) {
         expectationMismatch = true;
       }
       message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
       expectationMismatch = handleInverse( expectationMismatch, checkInverse );
       actualValue = "" + actual;
       expectValue = "" + expect;
       checkValue = false;
     }

     if( xmlValue.isCheckIsNull() && !expectationMismatch ) {
       final String actual = jph.isNull( xmlValue.getPath() ) ? "null" : "Not null";
       final String expect = "null";
       errorMessage = "Object expected: ### null but was: " + actual;
       if( !actual.equals( expect ) ) {
         expectationMismatch = true;
       }
       message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
       expectationMismatch = handleInverse( expectationMismatch, checkInverse );
       actualValue = "" + actual;
       expectValue = "" + expect;
       checkValue = false;
     }

     // Check if either actual of expect is null to avoid NullpointerException in the following code.
     if( checkValue
        && ( ( actualObject == null && xmlValue.getValue() != null )
          || ( actualObject != null && xmlValue.getValue() == null ) )
     ) {
       message = "Object expected: " + xmlValue.getValue() + " but was: " + actualObject;
       expectationMismatch = true;
       actualValue = actualObject == null ? null : "" + actualObject;
       expectValue = xmlValue.getValue();
       checkValue = false;
     }

     if( checkValue ) {
       // Apply variables (if any)
       if( xmlTest != null ) {
         xmlValue.setValue( VariablesHandler.applyVariables(
           xmlValue.getValue(),
           xmlTest.getVariables(),
           "body or header", // source
           TestSetHandler.CANDIDATE,
           xmlTest.getId(),
           testFileName
         ) );
       }

       try {
         // Apply functions
         xmlValue.setValue( VariablesCalculator.calculateIfRequired( xmlValue.getValue() ) );

         switch( xmlValue.getType() ) {
           case INT : {
             final Integer actual = (Integer) actualObject;
             final Range   range = RangeParser.parse( xmlValue.getValue() );
             if( range == null ) {
               final Integer expect = Converter.asInteger( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );

               errorMessage = "Integer expected: ### " + expect + " but was: " + actual;
               if( Math.abs( actual.doubleValue() - expect.doubleValue() ) > epsilon ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               final RangeType lowerType = range.getLowerBorder().getType();
               final int lowerValue = Converter.asInteger( range.getLowerBorder().getValue(), null );
               final boolean lowerBorderViolated = ( lowerType == RangeType.EXCLUSIVE )
                 ? actual <= lowerValue
                 : actual <  lowerValue;
               final RangeType upperType = range.getUpperBorder().getType();
               final int upperValue = Converter.asInteger( range.getUpperBorder().getValue(), null );
               final boolean upperBorderViolated = ( upperType == RangeType.EXCLUSIVE )
                 ? actual >= upperValue
                 : actual >  upperValue;

               if( lowerBorderViolated ) {
                 message = "Integer expected: " + ( lowerType == RangeType.EXCLUSIVE ? "> " : ">= " ) + lowerValue + " but was: " + actual;
                 expectationMismatch = true;
               }
               else if( upperBorderViolated ) {
                 message = "Integer expected: " + ( upperType == RangeType.EXCLUSIVE ? "< " : "<= " ) + upperValue + " but was: " + actual;
                 expectationMismatch = true;
               }

               if( checkInverse ) {
                 // NOTE: We do not support inversion handling within ranges.
                 throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" check inverse is not implemented yet for ranges." );
               }
             }
             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case LONG : {
             final Number number = (Number) actualObject;
             final Long  actual = number.longValue();
             final Range range  = RangeParser.parse( xmlValue.getValue() );
             if( range == null ) {
               final Long expect = Converter.asLong( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );

               errorMessage = "Long expected: ### " + expect + " but was: " + actual;
               if( Math.abs( actual.doubleValue() - expect.doubleValue() ) > epsilon ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               final RangeType lowerType = range.getLowerBorder().getType();
               final long lowerValue = Converter.asLong( range.getLowerBorder().getValue(), null );
               final boolean lowerBorderViolated = ( lowerType == RangeType.EXCLUSIVE )
                 ? actual <= lowerValue
                 : actual <  lowerValue;
               final RangeType upperType = range.getUpperBorder().getType();
               final long upperValue = Converter.asLong( range.getUpperBorder().getValue(), null );
               final boolean upperBorderViolated = ( upperType == RangeType.EXCLUSIVE )
                 ? actual >= upperValue
                 : actual >  upperValue;

               if( lowerBorderViolated ) {
                 message = "Long expected: " + ( lowerType == RangeType.EXCLUSIVE ? "> " : ">= " ) + lowerValue + " but was: " + actual;
                 expectationMismatch = true;
               }
               else if( upperBorderViolated ) {
                 message = "Long expected: " + ( upperType == RangeType.EXCLUSIVE ? "< " : "<= " ) + upperValue + " but was: " + actual;
                 expectationMismatch = true;
               }

               if( checkInverse ) {
                 // NOTE: We do not support inversion handling within ranges.
                 throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" check inverse is not implemented yet for ranges." );
               }
             }
             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case DOUBLE : {
             final Double actual = (Double) actualObject;
             final Range  range  = RangeParser.parse( xmlValue.getValue() );
             if( range == null ) {
               final Double expect = Converter.asDouble( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );

               errorMessage = "Double expected: ### " + expect + " but was: " + actual + " epsilon was: " + epsilon;
               if( Math.abs( actual.doubleValue() - expect.doubleValue() ) > epsilon ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               final RangeType lowerType = range.getLowerBorder().getType();
               final double lowerValue = Converter.asDouble( range.getLowerBorder().getValue(), null );
               final boolean lowerBorderViolated = ( lowerType == RangeType.EXCLUSIVE )
                 ? actual <= lowerValue
                 : actual <  lowerValue;
               final RangeType upperType = range.getUpperBorder().getType();
               final double upperValue = Converter.asDouble( range.getUpperBorder().getValue(), null );
               final boolean upperBorderViolated = ( upperType == RangeType.EXCLUSIVE )
                 ? actual >= upperValue
                 : actual >  upperValue;

               if( lowerBorderViolated ) {
                 message = "Double expected: " + ( lowerType == RangeType.EXCLUSIVE ? "> " : ">= " ) + lowerValue + " but was: " + actual;
                 expectationMismatch = true;
               }
               else if( upperBorderViolated ) {
                 message = "Double expected: " + ( upperType == RangeType.EXCLUSIVE ? "< " : "<= " ) + upperValue + " but was: " + actual;
                 expectationMismatch = true;
               }

               if( checkInverse ) {
                 // NOTE: We do not support inversion handling within ranges.
                 throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" check inverse is not implemented yet for ranges." );
               }
             }
             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case DATE : {
             final LocalDate actual = LocalDate.parse( (String) actualObject );
             final Range     range  = RangeParser.parse( xmlValue.getValue() );
             if( range == null ) {
               final LocalDate expect = Converter.asLocalDate( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );

               final Duration durationEpsilon = ( xmlValue.getEpsilon() != null )
                 ? Duration.parse( xmlValue.getEpsilon() )
                 : ComparatorHelper.ZERO_DURATION;

               errorMessage = "Date expected: ### " + expect + " but was: " + actual + " epsilon was: " + durationEpsilon;
               if( !ComparatorHelper.equals( actual, expect, durationEpsilon ) ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               final RangeType lowerType = range.getLowerBorder().getType();
               final LocalDate lowerValue = Converter.asLocalDate( range.getLowerBorder().getValue(), null );
               final boolean lowerBorderViolated = ( lowerType == RangeType.EXCLUSIVE )
                 ? actual.isBefore( lowerValue ) || actual.isEqual( lowerValue )
                 : actual.isBefore( lowerValue );
               final RangeType upperType = range.getUpperBorder().getType();
               final LocalDate upperValue = Converter.asLocalDate( range.getUpperBorder().getValue(), null );
               final boolean upperBorderViolated = ( upperType == RangeType.EXCLUSIVE )
                 ? actual.isAfter( upperValue ) || actual.isEqual( upperValue )
                 : actual.isAfter( upperValue );

               if( lowerBorderViolated ) {
                 message = "Date expected: " + ( lowerType == RangeType.EXCLUSIVE ? "> " : ">= " ) + lowerValue + " but was: " + actual;
                 expectationMismatch = true;
               }
               else if( upperBorderViolated ) {
                 message = "Date expected: " + ( upperType == RangeType.EXCLUSIVE ? "< " : "<= " ) + upperValue + " but was: " + actual;
                 expectationMismatch = true;
               }

               if( checkInverse ) {
                 // NOTE: We do not support inversion handling within ranges.
                 throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" check inverse is not implemented yet for ranges." );
               }
             }
             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case DATETIME : {
             final LocalDateTime actual = Converter.asLocalDateTime( (String) actualObject, null, Converter.THROW_CONVERSION_EXCEPTION );
             final Range         range  = RangeParser.parse( xmlValue.getValue() );
             if( range == null ) {
               final LocalDateTime expect = Converter.asLocalDateTime( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );

               final Duration durationEpsilon = ( xmlValue.getEpsilon() != null )
                 ? Duration.parse( xmlValue.getEpsilon() )
                 : ComparatorHelper.ZERO_DURATION;

               errorMessage = "DateTime expected: ### " + expect + " but was: " + actual + " epsilon was: " + durationEpsilon;
               if( !ComparatorHelper.equals( actual, expect, durationEpsilon ) ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               final RangeType lowerType = range.getLowerBorder().getType();
               final LocalDateTime lowerValue = Converter.asLocalDateTime( range.getLowerBorder().getValue(), null );
               final boolean lowerBorderViolated = ( lowerType == RangeType.EXCLUSIVE )
                 ? actual.isBefore( lowerValue ) || actual.isEqual( lowerValue )
                 : actual.isBefore( lowerValue );
               final RangeType upperType = range.getUpperBorder().getType();
               final LocalDateTime upperValue = Converter.asLocalDateTime( range.getUpperBorder().getValue(), null );
               final boolean upperBorderViolated = ( upperType == RangeType.EXCLUSIVE )
                 ? actual.isAfter( upperValue ) || actual.isEqual( upperValue )
                 : actual.isAfter( upperValue );

               if( lowerBorderViolated ) {
                 message = "DateTime expected: " + ( lowerType == RangeType.EXCLUSIVE ? "> " : ">= " ) + lowerValue + " but was: " + actual;
                 expectationMismatch = true;
               }
               else if( upperBorderViolated ) {
                 message = "DateTime expected: " + ( upperType == RangeType.EXCLUSIVE ? "< " : "<= " ) + upperValue + " but was: " + actual;
                 expectationMismatch = true;
               }

               if( checkInverse ) {
                 // NOTE: We do not support inversion handling within ranges.
                 throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" check inverse is not implemented yet for ranges." );
               }
             }
             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case DURATION : {
             final Duration actual = Duration.parse( (String) actualObject );
             final Range    range  = RangeParser.parse( xmlValue.getValue() );
             if( range == null ) {
               final Duration expect = Converter.asDuration( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );

               final Duration durationEpsilon = ( xmlValue.getEpsilon() != null )
                   ? Duration.parse( xmlValue.getEpsilon() )
                   : ComparatorHelper.ZERO_DURATION;

               errorMessage = "Duration expected: ### " + expect + " but was: " + actual + " epsilon was: " + durationEpsilon;
               if( !ComparatorHelper.equals( actual, expect, durationEpsilon ) ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               final RangeType lowerType = range.getLowerBorder().getType();
               final Duration lowerValue = Converter.asDuration( range.getLowerBorder().getValue(), null );
               final boolean lowerBorderViolated = ( lowerType == RangeType.EXCLUSIVE )
                 ? actual.getSeconds() <= lowerValue.getSeconds()
                 : actual.getSeconds() <  lowerValue.getSeconds();
               final RangeType upperType = range.getUpperBorder().getType();
               final Duration upperValue = Converter.asDuration( range.getUpperBorder().getValue(), null );
               final boolean upperBorderViolated = ( upperType == RangeType.EXCLUSIVE )
                 ? actual.getSeconds() >= upperValue.getSeconds()
                 : actual.getSeconds() >  upperValue.getSeconds();

               if( lowerBorderViolated ) {
                 message = "Duration expected: " + ( lowerType == RangeType.EXCLUSIVE ? "> " : ">= " ) + lowerValue + " but was: " + actual;
                 expectationMismatch = true;
               }
               else if( upperBorderViolated ) {
                 message = "Duration expected: " + ( upperType == RangeType.EXCLUSIVE ? "< " : "<= " ) + upperValue + " but was: " + actual;
                 expectationMismatch = true;
               }

               if( checkInverse ) {
                 // NOTE: We do not support inversion handling within ranges.
                 throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" check inverse is not implemented yet for ranges." );
               }
             }
             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case BOOLEAN : {
             final Boolean actual = (Boolean) actualObject;
             final Boolean expect = Converter.asBoolean( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );
             final Range   range  = RangeParser.parse( xmlValue.getValue() );
             if( range != null ) {
               throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" range compare is not supported." );
             }

             errorMessage = "Boolean expected: ### " + expect + " but was: " + actual;
             if( !actual.equals( expect ) ) {
               expectationMismatch = true;
             }

             message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
             expectationMismatch = handleInverse( expectationMismatch, checkInverse );

             actualValue = "" + actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           case STRING : {
             String actual = (String) actualObject;
             String expect = Converter.asString( xmlValue.getValue(), null, Converter.THROW_CONVERSION_EXCEPTION );
             if( xmlValue.isTrim() ) {
               actual = actual.trim();
               expect = expect.trim();
             }

             if( !xmlValue.isMatch() ) {
               errorMessage = "String expected: (trim=" + xmlValue.isTrim() + ", ignoreCase=" + xmlValue.isIgnoreCase() + ") ### " + expect + " but was: " + actual;
               if( !((xmlValue.isIgnoreCase() ) ? actual.equalsIgnoreCase( expect ) : actual.equals( expect ) ) ) {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }
             else {
               errorMessage = "String expected to match: (trim=\" + xmlValue.isTrim() + \", ignoreCase=\" + xmlValue.isIgnoreCase() + \")  ### " + expect + " but was: " + actual;

               if( actual != null ) { // Avoid Pattern error because passed value is null
                 final Pattern pattern = xmlValue.isIgnoreCase()
                   ? Pattern.compile( expect, Pattern.CASE_INSENSITIVE )
                   : Pattern.compile( expect );
                 final Matcher matcher = pattern.matcher( actual );

                 if( !matcher.matches() ) {
                   expectationMismatch = true;
                 }
               }
               else {
                 expectationMismatch = true;
               }

               message             = handleInverse( expectationMismatch, checkInverse, errorMessage );
               expectationMismatch = handleInverse( expectationMismatch, checkInverse );
             }

             actualValue = actual;
             expectValue = "" + xmlValue.getValue();
             break;
           }
           default :
             // This will result in a skipped test.
             throw new RuntimeException( "XmlValueType \"" + xmlValue.getType().name() + "\" is not supported." );
         }
       }
       catch( final Exception ex ) {
         LOG.error( "ERROR: An error occurred when processing JsonPath \"" + jsonPath + "\".", ex );
         expectationMismatch = true;
         message = (ex instanceof NullPointerException) ? "NullPointerException" : ex.getMessage();
       }
     } // if( checkValue )

     final JsonDiffEntry jsonDiffEntry = new JsonDiffEntry(
         jsonPath,    // jsonPath
         actualValue, // actual
         expectValue, // expected
         message      // message
     );

     if( expectationMismatch ) {
       // Store expectation matches
       jsonDiff.getChanges().add( jsonDiffEntry );
       return null;
     }

     return jsonDiffEntry;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public static String toString( final XmlExpected xmlExpected )
   {
     final StringBuilder sb = new StringBuilder( "{" );

     // values
     sb.append( "values : [" );
     if( xmlExpected.getValues() != null && xmlExpected.getValues().value != null ) {
       for( final XmlValue xmlValue : xmlExpected.getValues().value ) {
         sb.append( "{ path : "    ).append( xmlValue.getPath() )
           .append( ", type : "    ).append( xmlValue.getType() )
           .append( ", value : "   ).append( xmlValue.getValue() )
           .append( ", epsilon : " ).append( xmlValue.getEpsilon() )
           .append( " }" );
       }
     }
     sb.append( "]" );

     sb.append( "}" );

     return sb.toString();
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Creates a JsonDiff that included differences to ignore.
    * @param source      The original ignore JsonDiff (e.g. white noise diff). May be null.
    * @param ignorePaths A Set of paths to ignore additionally. May be null.
    * @param subPath A JSON subpath to deal with non-body artificial JSON content (used for HttpStatus- and Headers comparison).
    * @param reason The reason, why a JSON path may be ignored.
    * @return A JsonDiff that included differences to ignore. Never null.
    */
   static JsonDiff createIgnoreJsonDiff(
     final JsonDiff source,
     final Set< String > ignorePaths,
     final String subPath,
     final String reason
   )
   {
     if( LOG.isTraceEnabled() ) {
       LOG.trace( "createIgnoreJsonDiff() source=" + source.toString()
         + ", ignorePaths=" + ignorePaths
         + ", subPath=" + subPath
         + ", reason=" + reason
       );
     }

     final JsonDiff diff = JsonDiff.createDataInstance();
     if( source != null ) {
       diff.getChanges  ().addAll( source.getChanges() );
       diff.getAdditions().addAll( source.getAdditions() );
       diff.getDeletions().addAll( source.getDeletions() );
     }

     if( ignorePaths != null ) {
       final List< JsonDiffEntry > list = new ArrayList<>();
       for( final String ignorePath : ignorePaths ) {
         list.add( new JsonDiffEntry(
           ( subPath != null ) ? ( "$." + subPath + "." + ignorePath ) : ignorePath, // jsonPath
           null,    // actual
           null,    // expected
           reason ) // message
         );
       }

       diff.getChanges  ().addAll( list );
       diff.getAdditions().addAll( list );
       diff.getDeletions().addAll( list );
     }

     return diff;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Computes the differences (held within a JsonDiff object) between two JSON string representations.
    * @param candidate  The candidate. Must not be null.
    * @param reference  The reference. Must not be null.
    * @param whiteNoise The white noise to ignore. May be null.
    * @param reportWhiteNoise Flag, if any different value shall be reported (true) or only those that were not discovered to be white noise (differences between reference and control, or expected differences) (false).
    * @param testId The current test id. Must not be null.
    * @return A JsonDiff object that holds all relevant differences.
    * @throws JsonMappingException
    * @throws JsonProcessingException
    */
   static JsonDiff validateJson(
     final String   candidate,
     final String   reference,
     final JsonDiff whiteNoise,
     final boolean  reportWhiteNoise,
     final String   testId
   )
   throws JsonMappingException, JsonProcessingException
   {
     if( LOG.isTraceEnabled() ) {
       LOG.trace( "validateJson( candidate=\"" + candidate
         + "\", reference=\"" + reference
         + "\", whiteNoise=" + ( whiteNoise != null ? whiteNoise.toString() : "null" )
         + ", reportWhiteNoise=" + reportWhiteNoise
         + ", testId=" + testId
         + " )"
       );
     }

     // Calculate differences
     final JsonDiff jsonDiff = JsonDiff.createInstance()
       .setCandidate( candidate )
       .setReference( reference )
       .calculate();

     if( LOG.isTraceEnabled() ) {
       LOG.trace( "validateJson() jsonDiff=" + jsonDiff.toString() );
     }

     // Remove white noise differences
     if( whiteNoise != null && !reportWhiteNoise ) {
       removeDifference( jsonDiff.getChanges  (), whiteNoise.getChanges  (), testId );
       removeDifference( jsonDiff.getAdditions(), whiteNoise.getAdditions(), testId );
       removeDifference( jsonDiff.getDeletions(), whiteNoise.getDeletions(), testId );
     }

     if( LOG.isTraceEnabled() ) {
       LOG.trace( "validateJson() result  =" + jsonDiff.toString() );
     }
     return jsonDiff;
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Removes entries from a given List of JsonDiffEntry objects.
    * @param list The list to delete entries from. Must not be null.
    * @param entriesToRemove The entries to remove. Must not be null.
    * @param testId The current test id. Must not be null.
    */
   private static void removeDifference(
     final List< JsonDiffEntry > list,
     final List< JsonDiffEntry > entriesToRemove,
     final String testId
   )
   {
     final Iterator< JsonDiffEntry > it = list.iterator();
     while( it.hasNext() ) {
       final JsonDiffEntry entry = it.next();
       for( final JsonDiffEntry entryToRemove : entriesToRemove ) {
         boolean removeEntry = entryToRemove.getJsonPath().equals( entry.getJsonPath() ); // Check simple case first (e.g. "$.key1")
         if( !removeEntry && entryToRemove.getJsonPath().startsWith( "$" ) ) {
           removeEntry = JsonPathHelper.contains( entryToRemove.getJsonPath(), entry.getJsonPath() ); // This is expensive
         }

         if( removeEntry ) {
           if( LOG.isDebugEnabled() ) {
             LOG.debug( "Removing difference for path \"" + entry.getJsonPath() + "\" in test \"" + testId + "\" (message=" + entry.getMessage() + ")" + " Reason: " + entryToRemove.getMessage() );
           }
           it.remove();
           break;
         }
       }
     }
   }
}
