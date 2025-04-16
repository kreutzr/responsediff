package com.github.kreutzr.responsediff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.Constants;
import com.github.kreutzr.responsediff.JsonDiff;
import com.github.kreutzr.responsediff.JsonDiffEntry;
import com.github.kreutzr.responsediff.JsonPathHelper;
import com.github.kreutzr.responsediff.TestSetHandler;
import com.github.kreutzr.responsediff.ToJson;
import com.github.kreutzr.responsediff.ValidationHandler;
import com.github.kreutzr.responsediff.tools.Converter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import com.github.kreutzr.responsediff.XmlExpected;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;
import com.github.kreutzr.responsediff.XmlHttpResponse;
import com.github.kreutzr.responsediff.XmlIgnore;
import com.github.kreutzr.responsediff.XmlResponse;
import com.github.kreutzr.responsediff.XmlTest;
import com.github.kreutzr.responsediff.XmlValue;
import com.github.kreutzr.responsediff.XmlValueType;
import com.github.kreutzr.responsediff.XmlValues;

public class ValidationHandlerTest
{
  private static final String rootPath_ = new File( "" ).getAbsolutePath() + File.separator;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( ValidationHandlerTest.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private XmlValue createXmlValue(
    final String       path,
    final XmlValueType type,
    final String       value,
    final Boolean      checkIsNull,
    final Boolean      checkPathExists,
    final Boolean      checkInverse
  )
  {
    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( path );
    xmlValue.setValue( value );

    if( type != null ) {
      xmlValue.setType( type );
    }
    if( checkIsNull != null ) {
      xmlValue.setCheckIsNull( checkIsNull );
    }
    if( checkPathExists != null ) {
      xmlValue.setCheckPathExists( checkPathExists );
    }
    if( checkInverse != null ) {
      xmlValue.setCheckInverse( checkInverse );
    }

    return xmlValue;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private XmlValue createXmlValue(
    final String       path,
    final XmlValueType type,
    final String       value,
    final Boolean      checkIsNull,
    final Boolean      checkPathExists,
    final Boolean      checkInverse,
    final Boolean      match
  )
  {
    final XmlValue xmlValue = createXmlValue( path, type, value, checkIsNull, checkPathExists, checkInverse );

    if( match != null ) {
      xmlValue.setMatch( match );
    }

    return xmlValue;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private XmlHeader createXmlHeader( final String name, final String value )
  {
    final XmlHeader xmlHeader = new XmlHeader();
    xmlHeader.setName( name );
    xmlHeader.setValue( value );

    return xmlHeader;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatValidateJsonWorks()
  {
    // Given
    final XmlHeader referenceHeader1 = new XmlHeader();
    referenceHeader1.setName( "date" );
    referenceHeader1.setValue( "2023-10-26T18:30:00" );
    final XmlHeader referenceHeader2 = new XmlHeader();
    referenceHeader2.setName( "equal-header" );
    referenceHeader2.setValue( "xxx" );
    final XmlHeader referenceHeader3 = new XmlHeader();
    referenceHeader3.setName( "different-header" );
    referenceHeader3.setValue( "aaa" );

    final XmlHeader candidateHeader1 = new XmlHeader();
    candidateHeader1.setName( "date" );
    candidateHeader1.setValue( "2023-10-26T18:30:01" ); // Different value
    final XmlHeader candidateHeader2 = new XmlHeader();
    candidateHeader2.setName( "equal-header" );
    candidateHeader2.setValue( "xxx" );
    final XmlHeader candidateHeader3 = new XmlHeader();
    candidateHeader3.setName( "different-header" );
    candidateHeader3.setValue( "bbb" );                 // Different value

    final XmlHeaders candidateHeaders = new XmlHeaders();
    candidateHeaders.getHeader().add( candidateHeader1 );
    candidateHeaders.getHeader().add( candidateHeader2 );
    candidateHeaders.getHeader().add( candidateHeader3 );

    final XmlHeaders referenceHeaders = new XmlHeaders();
    referenceHeaders.getHeader().add( referenceHeader2 ); // Different header order
    referenceHeaders.getHeader().add( referenceHeader3 );
    referenceHeaders.getHeader().add( referenceHeader1 );

    final Set< String > ignoreHeaders = new HashSet<>();
    ignoreHeaders.add( "date" );

    final String candidate = ToJson.fromHeaders( candidateHeaders, true );
    final String reference = ToJson.fromHeaders( referenceHeaders, true );
    final JsonDiff headerIgnore = ValidationHandler.createIgnoreJsonDiff( null, ignoreHeaders, ToJson.HEADERS_SUBPATH, ValidationHandler.IGNORE_HEADER_TOKEN );

    final String testId = "testThatValidateJsonWorks";

    try {
      final JsonDiff jsonDiff = ValidationHandler.validateJson(
        candidate,
        reference,
        headerIgnore,
        false, // reportWhiteNoise
        testId
      );

      LOG.trace( jsonDiff.toString() );

      assertThat( jsonDiff.hasDifference() ).isTrue();
      assertThat( jsonDiff.getChanges().size() ).isEqualTo( 1 );
      assertThat( jsonDiff.getChanges().get( 0 ).getJsonPath() ).isEqualTo( "$.headers.different-header" );
      assertThat( jsonDiff.getChanges().get( 0 ).getExpected() ).isEqualTo( "aaa" );
      assertThat( jsonDiff.getChanges().get( 0 ).getActual()   ).isEqualTo( "bbb" );
      assertThat( jsonDiff.getAdditions().size() ).isEqualTo( 0 );
      assertThat( jsonDiff.getDeletions().size() ).isEqualTo( 0 );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatValidateResponseWorks_extern0001()
  {
    try {
      // Given
      final String basePath  = rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/external/0001/";
      final String reference = Files.readString( Path.of( basePath + "reference.json" ) );
      final String candidate = Files.readString( Path.of( basePath + "candidate.json" ) );

      final XmlValues   xmlValues   = new XmlValues();
      final XmlExpected xmlExpected = new XmlExpected(); xmlExpected.setValues( xmlValues );

      // There was an issue that the expected value for "$[*].type" was also applied for all attributes "typeName".
      xmlValues.getValue().add( createXmlValue( "$[*].type", XmlValueType.STRING, "AS_DEV_TEST_SERIES", null, null, null ) );

      // There was an issue that complex path queries did not work.
      // NOTE: A single "=" will assign a value while "==" works for comparisons.
      xmlValues.getValue().add( createXmlValue( "$[*].attributes[?(@.attributeName=='SERIES_ID')].attributeValue", XmlValueType.STRING, "#2019.{5}", null, null, null, true ) ); // NOTE: Addition match parameter here

      final XmlResponse     xmlResponse       = new XmlResponse();      xmlResponse.setExpected( xmlExpected );
      final XmlHttpResponse candidateResponse = new XmlHttpResponse();  candidateResponse.setBody( candidate ); candidateResponse.setBodyIsJson( true );
      final XmlHttpResponse referenceResponse = new XmlHttpResponse();  referenceResponse.setBody( reference ); referenceResponse.setBodyIsJson( true );
      final JsonDiff        whiteNoise        = null;
      final Set< String >   ignorePaths       = null;
      final Set< String >   ignoreHeaders     = null;
      final double          epsilon           = Constants.EPSILON;
      final boolean         reportWhiteNoise  = false;
      final String          testId            = "testThatValidateResponseWorks_extern0001";

      // When
      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // xmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.hasDifference() ).isTrue();
      assertThat( differences.getAdditions() ).isEmpty();
      assertThat( differences.getDeletions() ).isEmpty();
      assertThat( differences.getChanges() ).hasSize( 12 ); // "begin" and "end" differ 2 times (each)
    }
    catch( final IOException ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatMultipleChecksWork_extern0002()
  {
    try {
      // Given
      final String basePath  = rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/external/0002/";
      final String candidate = Files.readString( Path.of( basePath + "response.json" ) );
      final String reference = Files.readString( Path.of( basePath + "response.json" ) ); // We do not look for unexpected differences!

      final XmlValues   xmlValues   = new XmlValues();
      final XmlExpected xmlExpected = new XmlExpected();      xmlExpected.setValues( xmlValues );

      // Simple path does not exits plus failing value check
      xmlValues.getValue().add( createXmlValue( "$.key0", XmlValueType.STRING, null, null, true, null ) ); // fail
      xmlValues.getValue().add( createXmlValue( "$.key0", XmlValueType.STRING, "X",  null, null, null ) ); // fail

      // Simple path does exist plus successful value check
      xmlValues.getValue().add( createXmlValue( "$.key2", XmlValueType.STRING, null, null, true, null ) );
      xmlValues.getValue().add( createXmlValue( "$.key2", XmlValueType.STRING, "Y",  null, null, null ) ); // fail ( 1 != "Y" )

      // Simple path does exist plus failing value checks
      xmlValues.getValue().add( createXmlValue( "$.key3", XmlValueType.STRING, null, null, true, null ) );
      xmlValues.getValue().add( createXmlValue( "$.key3", XmlValueType.STRING, "X",  null, null, null ) ); // fail
      xmlValues.getValue().add( createXmlValue( "$.key3", XmlValueType.STRING, "Y",  null, null, null ) ); // fail

      // Complex path does exist plus successful value check
      xmlValues.getValue().add( createXmlValue( "$.array[*].key4", XmlValueType.STRING, "A",  null, null, null ) );

      // Complex path does exist plus successful value check
      xmlValues.getValue().add( createXmlValue( "$.array[*].key5", XmlValueType.STRING, "B",  null, null, null ) ); // fail 2x

      final XmlResponse     xmlResponse       = new XmlResponse();      xmlResponse.setExpected( xmlExpected );
      final XmlHttpResponse candidateResponse = new XmlHttpResponse();  candidateResponse.setBody( candidate ); candidateResponse.setBodyIsJson( true );
      final XmlHttpResponse referenceResponse = new XmlHttpResponse();  referenceResponse.setBody( reference ); referenceResponse.setBodyIsJson( true );
      final JsonDiff        whiteNoise        = null;
      final Set< String >   ignorePaths       = null;
      final Set< String >   ignoreHeaders     = null;
      final double          epsilon           = Constants.EPSILON;
      final boolean         reportWhiteNoise  = false;
      final String          testId            = "testThatValidateResponseWorks_extern0001";

      // When
      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // xmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.hasDifference() ).isTrue();
      assertThat( differences.getAdditions() ).isEmpty();
      assertThat( differences.getDeletions() ).isEmpty();
      assertThat( differences.getChanges() ).hasSize( 7 );
      assertThat( differences.getChanges().get( 0 ).getActual() ).isEqualTo( "Not exists" );
      assertThat( differences.getChanges().get( 1 ).getActual() ).isNull();
      assertThat( differences.getChanges().get( 1 ).getMessage() ).isEqualTo( "Object expected: X but was: null" );
      assertThat( differences.getChanges().get( 2 ).getActual() ).isEqualTo( "TYPE_MISMATCH_IN_TEST_DEFINITION" );
      assertThat( differences.getChanges().get( 3 ).getActual() ).isEqualTo( "Z" );
      assertThat( differences.getChanges().get( 4 ).getActual() ).isEqualTo( "Z" );
      assertThat( differences.getChanges().get( 5 ).getActual() ).isEqualTo( "C" );
      assertThat( differences.getChanges().get( 6 ).getActual() ).isEqualTo( "D" );
    }
    catch( final IOException | RuntimeException ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatMultipleChecksWork_extern0003()
  {
    try {
      // Given
      final String basePath  = rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/external/0003/";
      // Setup: We have different timestamp entries in the JSON
      final String candidate = Files.readString( Path.of( basePath + "candidate.json" ) );
      final String reference = Files.readString( Path.of( basePath + "reference.json" ) );

      // Setup: We have different "date" headers
      final XmlHeaders xmlCandidateHeaders = new XmlHeaders(); xmlCandidateHeaders.getHeader().add( createXmlHeader( "date", UUID.randomUUID().toString() ) );
      final XmlHeaders xmlReferenceHeaders = new XmlHeaders(); xmlReferenceHeaders.getHeader().add( createXmlHeader( "date", UUID.randomUUID().toString() ) );

      // Test: We expect that the date differences are not reported
      final XmlIgnore   xmlIgnore   = new XmlIgnore();
      xmlIgnore.setHeader( "date" );
      xmlIgnore.setForEver( true ); // Tested: This does not have any effect here :)

      final XmlValues   xmlValues   = new XmlValues();
      final XmlExpected xmlExpected = new XmlExpected();
      xmlExpected.setValues( xmlValues );
      final XmlValue xmlValue = createXmlValue( "$.timestamp", XmlValueType.DATETIME, "2024-09-17T11:11:00", null, null, null );
      xmlValue.setEpsilon( "PT1S" ); // NOTE: This is not sufficient for values "...T11:11:02" and "...T11:11:05". => We expect some reporting
      xmlValues.getValue().add( xmlValue );


      final XmlTest         xmlTest           = new XmlTest();
      final XmlResponse     xmlResponse       = new XmlResponse();      xmlResponse.setExpected( xmlExpected ); xmlResponse.getIgnore().add( xmlIgnore );
      final XmlHttpResponse candidateResponse = new XmlHttpResponse();  candidateResponse.setBody( candidate ); candidateResponse.setBodyIsJson( true ); candidateResponse.setHeaders( xmlCandidateHeaders );
      final XmlHttpResponse referenceResponse = new XmlHttpResponse();  referenceResponse.setBody( reference ); referenceResponse.setBodyIsJson( true ); referenceResponse.setHeaders( xmlReferenceHeaders );
      final JsonDiff        whiteNoise        = null;
      final Set< String >   ignorePaths       = TestSetHandler.getIgnorePaths( xmlResponse, xmlTest );
      final Set< String >   ignoreHeaders     = TestSetHandler.getIgnoreHeaders( xmlResponse );
      final double          epsilon           = Constants.EPSILON;
      final boolean         reportWhiteNoise  = false;
      final String          testId            = "testThatValidateResponseWorks_extern0001";

      // When
      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // xmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise, // NOTE: This is important!
        testId
      );

      // Then
      assertThat( differences.getAdditions().size() ).isEqualTo( 0 );
      assertThat( differences.getDeletions().size() ).isEqualTo( 0 );
      assertThat( differences.getChanges().size() ).isEqualTo( 2 );
      // Errors from expected values (candidate differ from expected value)
      assertThat( differences.getChanges().get(0).getJsonPath() ).isEqualTo( "$.timestamp" );
      assertThat( differences.getChanges().get(0).getExpected() ).isEqualTo( "2024-09-17T11:11:00" );
      assertThat( differences.getChanges().get(0).getActual  () ).isEqualTo( "2024-09-17T11:11:05" );
      // Errors from body comparison (candidate differs from reference value)
      assertThat( differences.getChanges().get(1).getJsonPath() ).isEqualTo( "$.timestamp" );
      assertThat( differences.getChanges().get(1).getExpected() ).isEqualTo( "2024-09-17T11:11:02" );
      assertThat( differences.getChanges().get(1).getActual  () ).isEqualTo( "2024-09-17T11:11:05" );
    }
    catch( final IOException | RuntimeException ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatMultipleChecksWork_extern0003_a()
  {
    try {
      // Given
      final String basePath  = rootPath_ + "src/test/resources/com/github/kreutzr/responsediff/external/0003/";
      // Setup: We have different timestamp entries in the JSON
      final String candidate = Files.readString( Path.of( basePath + "candidate.json" ) );
      final String reference = Files.readString( Path.of( basePath + "reference.json" ) );

      // Setup: We have different "date" headers
      final XmlHeaders xmlCandidateHeaders = new XmlHeaders(); xmlCandidateHeaders.getHeader().add( createXmlHeader( "date", UUID.randomUUID().toString() ) );
      final XmlHeaders xmlReferenceHeaders = new XmlHeaders(); xmlReferenceHeaders.getHeader().add( createXmlHeader( "date", UUID.randomUUID().toString() ) );

      // Test: We expect that the timestamp differences are not reported
      final XmlIgnore   xmlIgnore   = new XmlIgnore();
      xmlIgnore.setPath( "$.timestamp" );
      xmlIgnore.setForEver( true ); // Tested: This does not have any effect here :)

      final XmlTest         xmlTest           = new XmlTest();
      final XmlResponse     xmlResponse       = new XmlResponse();      xmlResponse.getIgnore().add( xmlIgnore );
      final XmlHttpResponse candidateResponse = new XmlHttpResponse();  candidateResponse.setBody( candidate ); candidateResponse.setBodyIsJson( true ); candidateResponse.setHeaders( xmlCandidateHeaders );
      final XmlHttpResponse referenceResponse = new XmlHttpResponse();  referenceResponse.setBody( reference ); referenceResponse.setBodyIsJson( true ); referenceResponse.setHeaders( xmlReferenceHeaders );
      final JsonDiff        whiteNoise        = null;
      final Set< String >   ignorePaths       = TestSetHandler.getIgnorePaths( xmlResponse, xmlTest );
      final Set< String >   ignoreHeaders     = TestSetHandler.getIgnoreHeaders( xmlResponse );
      final double          epsilon           = Constants.EPSILON;
      final boolean         reportWhiteNoise  = false;
      final String          testId            = "testThatValidateResponseWorks_extern0001";

      // When
      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // xmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise, // NOTE: This is important!
        testId
      );

      // Then
      assertThat( differences.getAdditions().size() ).isEqualTo( 0 );
      assertThat( differences.getDeletions().size() ).isEqualTo( 0 );
      assertThat( differences.getChanges().size() ).isEqualTo( 1 );
      // Errors from headers (JSON) comparison
      assertThat( differences.getChanges().get(0).getJsonPath() ).isEqualTo( "$.headers.date" );
    }
    catch( final IOException | RuntimeException ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // NEEDS FIX A: Add test(s) with XmlTest and testFileName parameters (using variables in expected values)

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatCheckPathExistsWithCheckValueFails()
  {
    // Given
    final String candidate = "{ \"key1\" : \"A\" }";
    final String reference = "{ \"key1\" : \"A\" }";

    final XmlValues   xmlValues   = new XmlValues();
    final XmlExpected xmlExpected = new XmlExpected(); xmlExpected.setValues( xmlValues );

    final XmlResponse     xmlResponse       = new XmlResponse();      xmlResponse.setExpected( xmlExpected );
    final XmlHttpResponse candidateResponse = new XmlHttpResponse();  candidateResponse.setBody( candidate ); candidateResponse.setBodyIsJson( true );
    final XmlHttpResponse referenceResponse = new XmlHttpResponse();  referenceResponse.setBody( reference ); referenceResponse.setBodyIsJson( true );
    final JsonDiff        whiteNoise        = null;
    final Set< String >   ignorePaths       = null;
    final Set< String >   ignoreHeaders     = null;
    final double          epsilon           = Constants.EPSILON;
    final boolean         reportWhiteNoise  = false;
    final String          testId            = "testThatValidateResponseWorks_extern0001";

    // When
    try {
      xmlValues.getValue().clear();
      xmlValues.getValue().add( createXmlValue( "$.key1", XmlValueType.STRING, "Z", null, true, null ) ); // exception

      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // XmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.getChanges() ).hasSize( 1 );
      assertThat( differences.getChanges().get( 0 ).getMessage() ).startsWith( "A value check must not be combined with checkIsNull or checkPathExits." );
    }
    catch( final Exception ex ) {
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }

    // When
    try {
      xmlValues.getValue().clear();
      xmlValues.getValue().add( createXmlValue( "$.key1", XmlValueType.STRING, "Z", true, null, true ) ); // exception

      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // XmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.getChanges() ).hasSize( 1 );
      assertThat( differences.getChanges().get( 0 ).getMessage() ).startsWith( "A value check must not be combined with checkIsNull or checkPathExits." );
    }
    catch( final Exception ex ) {
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }

    // When
    try {
      xmlValues.getValue().clear();
      xmlValues.getValue().add( createXmlValue( "$.key1", XmlValueType.STRING, "Z", true, true, true ) ); // exception

      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // XmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.getChanges() ).hasSize( 1 );
      assertThat( differences.getChanges().get( 0 ).getMessage() ).startsWith( "A value check must not be combined with checkIsNull or checkPathExits." );
    }
    catch( final Exception ex ) {
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }

    // When
    try {
      xmlValues.getValue().clear();
      xmlValues.getValue().add( createXmlValue( "$.key1", XmlValueType.STRING, "Z", null, null, null ) ); // fail

      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // XmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.hasDifference() ).isTrue();
      assertThat( differences.getAdditions() ).isEmpty();
      assertThat( differences.getDeletions() ).isEmpty();
      assertThat( differences.getChanges() ).hasSize( 1 );
      assertThat( differences.getChanges().get( 0 ).getActual() ).isEqualTo( "A" );
    }
    catch( final Exception ex ) {
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }

    // When
    try {
      xmlValues.getValue().clear();
      xmlValues.getValue().add( createXmlValue( "$.key1", XmlValueType.STRING, "A", null, null, null ) ); // ok

      final JsonDiff differences = ValidationHandler.validateResponse(
        xmlResponse,
        null, // XmlTest
        null, // testFileName
        candidateResponse,
        referenceResponse,
        whiteNoise,
        ignorePaths,
        ignoreHeaders,
        false,
        epsilon,
        reportWhiteNoise,
        testId
      );

      // Then
      assertThat( differences.hasDifference() ).isFalse();
    }
    catch( final Exception ex ) {
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckValidateJsonWorksForNullEntries()
  {
    // Given
    final String referenceBody = "{}";
    final String candidateBody = "{ \"key\" : null }";

    final String testId = "testCheckValidateJsonWorksForNullEntries";

    try {
      // When
      final JsonDiff jsonDiff = ValidationHandler.validateJson(
          candidateBody,
          referenceBody,
          null,
          false, // reportWhiteNoise
          testId
        );

      // Then
      assertThat( jsonDiff.hasDifference() ).isTrue();
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
      assertThat( false ).isTrue().withFailMessage( "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForInt()
  {
    // Given
    final String json = "{ \"int\" : 5 }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.int" );
    xmlValue.setType( XmlValueType.INT );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "5" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "5" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setEpsilon( "1" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON ) );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setEpsilon( "1" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON ) );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // --------------------------------------------------------
    // Range check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,6]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,6]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[7,8]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[7,8]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,4]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,4]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]1,5[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]1,5[" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]5,8[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]5,8[" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForLong()
  {
    // Given
    final String json = "{ \"long\" : 5 }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.long" );
    xmlValue.setType( XmlValueType.LONG );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "5" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "5" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setEpsilon( "1" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON ) );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4" );
    xmlValue.setEpsilon( "1" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON ) );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // --------------------------------------------------------
    // Range check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,6]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,6]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[7,8]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[7,8]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,4]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1,4]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]1,5[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]1,5[" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]5,8[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]5,8[" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForDouble()
  {
    // Given
    final String json = "{ \"double\" : 5.0 }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.double" );
    xmlValue.setType( XmlValueType.DOUBLE );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "5.0" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "5.0" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4.9" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4.9" );
    xmlValue.setEpsilon( "0.1" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4.9" );
    xmlValue.setEpsilon( "0.1" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON ) );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "4.9" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON ) );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );


    // --------------------------------------------------------
    // Range check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1.0,5.1]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1.0,5.1]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[5.1,6.0]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[5.1,6.0]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1.0,4.9]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[1.0,4.9]" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]1.0,5.0[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]1.0,5.0[" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]5.0,8.0[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]5.0,8.0[" );
    xmlValue.setCheckInverse( true ); // Not supported for ranges
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForDate()
  {
    // Given
    final String json = "{ \"date\" : \"2023-10-26\" }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.date" );
    xmlValue.setType( XmlValueType.DATE );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-24" );
    xmlValue.setCheckInverse( false );
    xmlValue.setEpsilon( "P2D" );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-24" );
    xmlValue.setEpsilon( "P2D" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-24" );
    xmlValue.setCheckInverse( false );
    xmlValue.setEpsilon( "P1D" );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-24" );
    xmlValue.setEpsilon( "P1D" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();


    // --------------------------------------------------------
    // Range check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-25,2023-10-26]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-25,2023-10-26]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-26,2023-10-27]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-26,2023-10-27]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-20,2023-10-25]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-20,2023-10-25]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-27,2023-10-29]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-27,2023-10-29]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-21,2023-10-26[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-21,2023-10-26[" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-26,2023-10-29[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-26,2023-10-29[" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForDateTime()
  {
    // Given
    final String json = "{ \"datetime\" : \"2023-10-26T15:00:00\" }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.datetime" );
    xmlValue.setType( XmlValueType.DATETIME );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26T15:00:00" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26T15:00:00" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26T14:30:00" );
    xmlValue.setCheckInverse( false );
    xmlValue.setEpsilon( "PT30M" );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26T14:30:00" );
    xmlValue.setEpsilon( "PT30M" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26T14:30:00" );
    xmlValue.setCheckInverse( false );
    xmlValue.setEpsilon( "PT29M59S" );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "2023-10-26T14:30:00" );
    xmlValue.setEpsilon( "PT29M59S" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();


    // --------------------------------------------------------
    // Range check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-25T00:00:00,2023-10-26T15:00:00]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-25T00:00:00,2023-10-26T15:00:00]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-26T15:00:00,2023-10-27T00:00:00]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-26T15:00:00,2023-10-27T00:00:00]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-20T00:00:00,2023-10-26T14:59:59]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-20T00:00:00,2023-10-26T14:59:59]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-26T15:00:01,2023-10-29T00:00:00]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[2023-10-26T15:00:01,2023-10-29T00:00:00]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-21T00:00:00,2023-10-26T15:00:00[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-21T00:00:00,2023-10-26T15:00:00[" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-26T15:00:00,2023-10-29T00:00:00[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]2023-10-26T15:00:00,2023-10-29T00:00:00[" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForDuration()
  {
    // Given
    final String json = "{ \"duration\" : \"P1DT10H30M5S\" }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.duration" );
    xmlValue.setType( XmlValueType.DURATION );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "P1DT10H30M5S" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "P1DT10H30M5S" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "P1DT10H30M" );
    xmlValue.setCheckInverse( false );
    xmlValue.setEpsilon( "PT5S" );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "P1DT10H30M" );
    xmlValue.setEpsilon( "PT5S" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "P1DT10H30M" );
    xmlValue.setCheckInverse( false );
    xmlValue.setEpsilon( "PT4S" );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "P1DT10H30M" );
    xmlValue.setEpsilon( "PT4S" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();


    // --------------------------------------------------------
    // Range check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M,P1DT10H30M5S]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M,P1DT10H30M5S]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M5S,P1DT10H30M10S]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M5S,P1DT10H30M10S]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M,P1DT10H30M4S]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M,P1DT10H30M4S]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M6S,P1DT10H30M10S]" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "[P1DT10H30M6S,P1DT10H30M10S]" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]P1DT10H30M,P1DT10H30M5S[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]P1DT10H30M,P1DT10H30M5S[" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]P1DT10H30M5S,P1DT10H30M10S[" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "]P1DT10H30M5S,P1DT10H30M10S[" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForBoolean()
  {
    // Given
    final String json = "{ \"bool\" : true }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.bool" );
    xmlValue.setType( XmlValueType.BOOLEAN );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "true" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "true" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "false" );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "false" );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForString()
  {
    // Given
    final String json = "{ \"string\" : \"text\" }";
    final DocumentContext context = JsonPath.parse( json );
    final JsonPathHelper jph = new JsonPathHelper( context );

    final XmlValue xmlValue = new XmlValue();
    xmlValue.setPath( "$.string" );
    xmlValue.setType( XmlValueType.STRING );

    JsonDiff mismatches = null;
    List< JsonDiffEntry > entries = null;

    // --------------------------------------------------------
    // Value check
    // --------------------------------------------------------

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "text" );
    xmlValue.setIgnoreCase( false );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "text" );
    xmlValue.setIgnoreCase( false );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "TEXT" );
    xmlValue.setIgnoreCase( false );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "TEXT" );
    xmlValue.setIgnoreCase( false );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "TEXT" );
    xmlValue.setIgnoreCase( true );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "TEXT" );
    xmlValue.setIgnoreCase( true );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "  TEXT  " );
    xmlValue.setIgnoreCase( true );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "  TEXT  " );
    xmlValue.setIgnoreCase( true );
    xmlValue.setTrim( false );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "  TEXT  " );
    xmlValue.setIgnoreCase( true );
    xmlValue.setTrim( true );
    xmlValue.setCheckInverse( false );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isNotEmpty();
    assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
    assertThat( mismatches.hasDifference() ).isFalse();

    // When / Then
    mismatches = JsonDiff.createInstance();
    xmlValue.setValue( "  TEXT  " );
    xmlValue.setIgnoreCase( true );
    xmlValue.setTrim( true );
    xmlValue.setCheckInverse( true );
    entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
    assertThat( entries ).isEmpty();
    assertThat( mismatches.hasDifference() ).isTrue();
    assertThat( mismatches.getChanges().get( 0 ).getMessage() ).isNotEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForNullValue()
  {
    {
      // Given
      final String json = "{ \"key\" : null }";
      final DocumentContext context = JsonPath.parse( json );
      final JsonPathHelper jph = new JsonPathHelper( context );

      final XmlValue xmlValue = new XmlValue();

      JsonDiff mismatches = null;
      List< JsonDiffEntry > entries = null;

      // --------------------------------------------------------
      // Value check
      // --------------------------------------------------------

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckIsNull( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isNotEmpty();
      assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
      assertThat( mismatches.hasDifference() ).isFalse();

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckIsNull( true );
      xmlValue.setCheckInverse( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isEmpty();
      assertThat( mismatches.hasDifference() ).isTrue();
    }

    // ----------------------------------------------------------

    {
      // Given
      final String json = "{}";
      final DocumentContext context = JsonPath.parse( json );
      final JsonPathHelper jph = new JsonPathHelper( context );

      final XmlValue xmlValue = new XmlValue();

      JsonDiff mismatches = null;
      List< JsonDiffEntry > entries = null;

      // --------------------------------------------------------
      // Value check
      // --------------------------------------------------------

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckIsNull( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isNotEmpty();
      assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
      assertThat( mismatches.hasDifference() ).isFalse();

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckIsNull( true );
      xmlValue.setCheckInverse( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isEmpty();
      assertThat( mismatches.hasDifference() ).isTrue();
    }

    // ----------------------------------------------------------

    {
      // Given
      final String json = "{ \"key\" : \"Not null\" }";
      final DocumentContext context = JsonPath.parse( json );
      final JsonPathHelper jph = new JsonPathHelper( context );

      final XmlValue xmlValue = new XmlValue();

      JsonDiff mismatches = null;
      List< JsonDiffEntry > entries = null;

      // --------------------------------------------------------
      // Value check
      // --------------------------------------------------------

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckIsNull( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isEmpty();
      assertThat( mismatches.hasDifference() ).isTrue();

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckIsNull( true );
      xmlValue.setCheckInverse( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isNotEmpty();
      assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
      assertThat( mismatches.hasDifference() ).isFalse();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testCheckExpectedWorksForNotExistingPaths()
  {
    {
      // Given
      final String json = "{ \"key\" : null }";
      final DocumentContext context = JsonPath.parse( json );
      final JsonPathHelper jph = new JsonPathHelper( context );

      final XmlValue xmlValue = new XmlValue();

      JsonDiff mismatches = null;
      List< JsonDiffEntry > entries = null;

      // --------------------------------------------------------
      // Value check
      // --------------------------------------------------------

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckPathExists( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isNotEmpty();
      assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
      assertThat( mismatches.hasDifference() ).isFalse();

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckPathExists( true );
      xmlValue.setCheckInverse( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isEmpty();
      assertThat( mismatches.hasDifference() ).isTrue();
    }

    // ----------------------------------------------------

    {
      // Given
      final String json = "{}";
      final DocumentContext context = JsonPath.parse( json );
      final JsonPathHelper jph = new JsonPathHelper( context );

      final XmlValue xmlValue = new XmlValue();

      JsonDiff mismatches = null;
      List< JsonDiffEntry > entries = null;

      // --------------------------------------------------------
      // Value check
      // --------------------------------------------------------

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckPathExists( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isEmpty();
      assertThat( mismatches.hasDifference() ).isTrue();

      // When / Then
      mismatches = JsonDiff.createInstance();
      xmlValue.setPath( "$.key" );
      xmlValue.setCheckPathExists( true );
      xmlValue.setCheckInverse( true );
      entries = ValidationHandler.checkExpected( jph, xmlValue, null, null, mismatches, Constants.EPSILON );
      assertThat( entries ).isNotEmpty();
      assertThat( entries.get( 0 ).getMessage() ).isEqualTo( ValidationHandler.EXPECTED_VALUE_TOKEN );
      assertThat( mismatches.hasDifference() ).isFalse();
    }
  }
}
