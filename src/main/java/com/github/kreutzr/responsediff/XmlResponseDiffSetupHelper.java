package com.github.kreutzr.responsediff;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to work upon a given XmlSetup
 */
public class XmlResponseDiffSetupHelper
{
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger( XmlResponseDiffSetupHelper.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the XmlTest from the given XmlSetup, that matches the given testId and xmlRequest.
   * @param xmlSetup The XmlSetup to use. Must not be null.
   * @param testId The testId to lookup. Must not be null.
   * @param xmlRequest The XmlRequest to use for lookup the requested test. Must not be null.
   * @return The matching XmlTest. May be null.
   */
  public static XmlTest findXmlTest(
    final XmlResponseDiffSetup xmlSetup,
    final String               testId,
    final XmlRequest           xmlRequest
  ) {
    final List< XmlTest > xmlTests = new ArrayList<>();

    for( final XmlTestSet xmlTestSet : xmlSetup.getTestSet() ) {
      final List< XmlTest > foundXmlTests = findXmlTests( xmlTestSet, testId, xmlRequest );
      if( foundXmlTests != null ) {
        xmlTests.addAll( foundXmlTests );
      }
    }

    if( xmlTests.size() > 1 ) {
      throw new RuntimeException( "Found " + xmlTests.size() + " test results with test id \"" + testId + "\"." );
    }

    return xmlTests.isEmpty()
      ? null
      : xmlTests.get( 0 );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the XmlTests from the given XmlTestSet, that matches the given testId and xmlRequest.
   * @param xmlTestSet The XmlTestSep to use. Must not be null.
   * @param testId The testId to lookup. Must not be null.
   * @param xmlRequest The XmlRequest to use for lookup the requested test. Must not be null.
   * @return A list of all matching XmlTests. May be empty but never null.
   */
  public static List< XmlTest > findXmlTests(
    final XmlTestSet xmlTestSet,
    final String     testId,
    final XmlRequest xmlRequest
  ) {
    final List< XmlTest > xmlTests = new ArrayList<>();

    for( final XmlTest xmlTest : xmlTestSet.getTest() ) {
      if( xmlTest.getId().equals( testId ) ) {
        xmlTests.add( xmlTest );
      }
    }

    for( final XmlTestSet xmlTestSetChild : xmlTestSet.getTestSet() ) {
      xmlTests.addAll( findXmlTests( xmlTestSetChild, testId, xmlRequest ) );
    }

    return xmlTests;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
  public static void main( final String[] args )
  {
    try {
      final String referenceFilePath = "C:/home/rkreutz/work/develop/test/responsediff/old_report.xml";
      final XmlResponseDiffSetup setup = XmlFileHandler.readSetup( referenceFilePath, false, LOG );

      final String testId = "Testset Resource / Resource query / 0002";

      XmlTest xmlTest = findXmlTest( setup, testId, null );

      LOG.info( "xmlTest=" + xmlTest );
    }
    catch( final Throwable ex ) {
      ex.printStackTrace();
    }
  }
*/
}
