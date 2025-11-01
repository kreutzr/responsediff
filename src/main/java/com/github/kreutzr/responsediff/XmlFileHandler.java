package com.github.kreutzr.responsediff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.kreutzr.responsediff.tools.CloneHelper;
import com.github.kreutzr.responsediff.tools.ErrorHandlingHelper;
import com.github.kreutzr.responsediff.tools.FormatHelper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * Class to handle XML reading and writing.
 */
public class XmlFileHandler
{
  private static final String XSD_FILE_NAME        = "responseDiffSetup.xsd";
//  private static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  private static final Logger LOG = LoggerFactory.getLogger( XmlFileHandler.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public  static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
  private static JAXBContext            JAXB_CONTEXT_FOR_SETUP   = null;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads a XmlResponseDiffSetup with the the given fileName.
   * @param xmlFilePath The name (including the path) of the XML file to read. May be null.
   * @param initialize Flag, if the read XmlSetup shall be initialized (true) or not (false).
   * @return The deserialized XmlResponseDiffSetup. If xmlFilePath is null, an empty XmlResponseDiffSetup is returned.
   * @throws JAXBException
   * @throws SAXException
   */
  static XmlResponseDiffSetup readSetup(
    final String xmlFilePath,
    final boolean initialize
  )
  throws JAXBException, SAXException, ParseException
  {
    if( xmlFilePath == null ) {
      return new XmlResponseDiffSetup();
    }

    // Try to activate XSD schema validation
    Schema schema = null;
    try {
      final URL xsdUrl = ResponseDiff.class.getResource( XSD_FILE_NAME );
      schema = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI ).newSchema( xsdUrl );
    }
    catch( final Exception ex ) {
      final String message = ErrorHandlingHelper.createSingleLineMessage( "Error while trying to perform XSD validation. Validation is skipped.", ex );
      LOG.error( message );
    }

    return readSetup( xmlFilePath, schema, null, initialize, "" );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads a XmlResponseDiffSetup with the the given fileName.
   * @param xmlFilePath The name (including the path) of the XML file to read. Must not be null.
   * @param schema An optional Schema to use for XSD validation. May be null.
   * @param xmlTestSetParent The XmlTestSetParent. May be null.
   * @param initialize Flag, if the read XmlSetup shall be initialized (true) or not (false).
   * @param testSetPath The path of the current TestSet. Must not be null.
   * @return The deserialized XmlResponseDiffSetup.
   * @throws JAXBException
   * @throws SAXException
   * @throws ParseException
   */
  static XmlResponseDiffSetup readSetup(
    final String xmlFilePath,
    final Schema schema,
    final XmlTestSet xmlTestSetParent,
    final boolean initialize,
    final String testSetPath
  )
  throws JAXBException, SAXException, ParseException
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "readSetup( " + xmlFilePath + " )" );
    }

     if( JAXB_CONTEXT_FOR_SETUP == null ) {
       JAXB_CONTEXT_FOR_SETUP = JAXBContext.newInstance( XmlResponseDiffSetup.class );
     }
     final File xmlFile = new File( xmlFilePath );

     final Unmarshaller jaxbUnmarshaller = JAXB_CONTEXT_FOR_SETUP.createUnmarshaller();

     // Try to activate XSD schema validation
     if( schema != null ) {
       jaxbUnmarshaller.setSchema( schema ); // Activate validation
       LOG.debug( "XML validation is activated for XML file " + xmlFilePath + " ." );
     }

     XmlResponseDiffSetup setup = null;
     try {
       setup = (XmlResponseDiffSetup) jaxbUnmarshaller.unmarshal( xmlFile );
     }
     catch( final Throwable ex ) {
       LOG.error( "Error reading setup file \"" + xmlFile.getAbsolutePath() + "\". ", ex );
       throw new RuntimeException( "Error reading setup file. Test run aborted.");
     }

     if( !initialize ) {
       return setup;
     }

     // Initialize setup variables with outer test set variables
     if( xmlTestSetParent != null ) {
       // Surrounding XmlTestSet -> local XmlSetup
       setup.setVariables(
         TestSetHandler.joinVariables(
           setup.getVariables(),
           xmlTestSetParent.getVariables(),
           null, // testId
           "Joining variables of test set \"" + xmlTestSetParent.getId() + "\" into setup \"" + setup.getId() + "\""
         )
       );
     }

     // Prepare XmlTestSet to initialize inner XmlTestSets with setup variables
     final XmlTestSet dummyTestSet = new XmlTestSet(); // We use this as vehicle to join XmlSetup variables.
     dummyTestSet.setVariables( setup.getVariables() );
     dummyTestSet.setId( setup.getId() );

     for( final XmlTestSet xmlTestSet : setup.getTestSet() ) {
       // Insert information for debugging and logging
       xmlTestSet.setFileName( xmlFilePath );
       xmlTestSet.setFilePath( testSetPath );

       readInnerTestSets( xmlTestSet, xmlFile, dummyTestSet, schema, initialize, testSetPath );
     }

     expandSetupByIterations( setup );

     return setup;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the inner test sets (which may contain test sets themselves) and initializes it with parent variables.
   * @param xmlTestSet The XmlTestSet to use. Must not be null.
   * @param xmlFile The XML file of the outer setup. Must not be null.
   * @param xmlTestSetParent The outer XmlTestSet (use to inherit variables from). Must not be null.
   * @param schema An optional Schema to use for XSD validation. May be null.
   * @param initialize Flag, if the read XmlSetup shall be initialized (true) or not (false).
   * @param testSetPath The path of the current TestSet. Must not be null.
   * @throws JAXBException
   * @throws SAXException
   * @throws ParseException
   */
  private static void readInnerTestSets(
    final XmlTestSet xmlTestSet,
    final File       xmlFile,
    final XmlTestSet xmlTestSetParent,
    final Schema     schema,
    final boolean    initialize,
    final String     testSetPath
  )
  throws JAXBException, SAXException, ParseException
  {
    // Initialize with parent variables
    TestSetHandler.joinVariablesForXmlTestSet( xmlTestSet, xmlTestSetParent, xmlTestSet.getId() );

    // Read inner test sets
    for( XmlTestSet innerXmlTestSet : xmlTestSet.getTestSet() ) {
      readInnerTestSets( innerXmlTestSet, xmlFile, xmlTestSet, schema, initialize, testSetPath );
    }

    // Read included test sets
    readIncludedTestSets( xmlTestSet, xmlFile, schema, initialize, testSetPath );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the included test sets (which may contain test sets themselves).
   * @param xmlTestSet The XmlTestSet to use. Must not be null.
   * @param xmlFile The XML file of the outer setup. Must not be null.
   * @param schema An optional Schema to use for XSD validation. May be null.
   * @param initialize Flag, if the read XmlSetup shall be initialized (true) or not (false).
   * @param testSetPath The path of the current TestSet. Must not be null.
   * @throws JAXBException
   * @throws SAXException
   * @throws ParseException
   */  private static void readIncludedTestSets(
    final XmlTestSet xmlTestSet,
    final File       xmlFile,
    final Schema     schema,
    final boolean    initialize,
    final String     testSetPath
  )
  throws JAXBException, SAXException, ParseException
  {
    // Read test set inclusions by recursion
    final List< XmlTestSet > includedTestSets = new ArrayList<>();
    if( xmlTestSet.getTestSetInclude() != null ) {
      for( final XmlTestSetInclude xmlTestSetInclude : xmlTestSet.getTestSetInclude() ) {
        final String absPath         = xmlFile.getAbsolutePath();
        final String name            = xmlFile.getName();
        final String includeFileName = absPath.substring( 0, absPath.length() - name.length() ) + xmlTestSetInclude.getFile();
        final Path   parentPath      = Path.of( xmlTestSetInclude.getFile() ).getParent();
        final String includeFilePath = testSetPath + ( parentPath != null ? parentPath.toString() + File.separator : "" );

        final XmlResponseDiffSetup includeSetup = readSetup(
          includeFileName,
          schema,
          xmlTestSet,
          initialize,
          includeFilePath
        );

        includedTestSets.addAll( includeSetup.getTestSet() );
      }
      xmlTestSet.getTestSet().addAll( includedTestSets );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Expands XmlTestSets with an "iterations=n" attribute (with n > 1) by a XmlTestSet that holds n copies of the original XmlTestSets each with iterations=1.
   * @param setup The XmlResponseDiffSetup to expand. Must not be null.
   */
  private static void expandSetupByIterations( final XmlResponseDiffSetup setup )
  {
    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Expanding test setup with id \"" + setup.getId() + "\"." );
    }

    final List< XmlTestSet > expandedTestSets = new ArrayList<>();
    for( final XmlTestSet xmlTestSet : setup.getTestSet() ) {
      expandedTestSets.add( expandTestSetByIterations( xmlTestSet ) );
    }

    // Replace original XmlTestSets by expanded ones
    setup.getTestSet().clear();
    setup.getTestSet().addAll( expandedTestSets );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static XmlTestSet expandTestSetByIterations( final XmlTestSet xmlTestSet )
  {
    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Expanding test set with id \"" + xmlTestSet.getId() + "\"." );
    }

    // --------------------------------------------------
    // Handle recursion
    // --------------------------------------------------
    final List< XmlTestSet > expandedTestSets = new ArrayList<>();
    for( final XmlTestSet xmlTestChildSet : xmlTestSet.getTestSet() ) {
      expandedTestSets.add( expandTestSetByIterations( xmlTestChildSet ) );
    }

    // Replace original XmlTestSets by expanded ones
    xmlTestSet.getTestSet().clear();
    xmlTestSet.getTestSet().addAll( expandedTestSets );

    // Terminate recursion with test (leaves)
    expandTestByIterations( xmlTestSet );

    // --------------------------------------------------
    // Handle iterations
    // --------------------------------------------------
    final int iterations = xmlTestSet.getIterations() != null
    ? xmlTestSet.getIterations()
    : 1;

    XmlTestSet result = xmlTestSet;
    if( iterations > 1 ) {
      result = createWrapperTestSet( xmlTestSet, iterations );

      xmlTestSet.setIterations( null );
      result.getTestSet().add( xmlTestSet );

      for( int i=0; i < iterations - 1; i++ ) {
        final XmlTestSet clonedXmlTestSet = CloneHelper.deepCopyJAXB( xmlTestSet, XmlTestSet.class );
        result.getTestSet().add( clonedXmlTestSet );
      }
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void expandTestByIterations( final XmlTestSet xmlTestSet )
  {
    final List< XmlTest > xmlTests = new ArrayList<>();

    for( final XmlTest xmlTest : xmlTestSet.getTest() ) {
      final int iterations = xmlTest.getIterations() != null
          ? xmlTest.getIterations()
          : 1;
      if( iterations > 1 ) {
        if( LOG.isDebugEnabled() ) {
          LOG.debug( "Expanding test with id \"" + xmlTest.getId() + "\"." );
        }

        xmlTest.setIterations( null ); // NOTE: Do NOT copy the iterations attribute here!

        // Add wrapper XmlTestSet for XmlTest with iterations.
        final XmlTestSet wrapperTestSet = createWrapperTestSet( xmlTestSet, iterations );

        wrapperTestSet.getTest().add( xmlTest );
        for( int i=0; i < iterations - 1; i++ ) {
          final XmlTest clonedXmlTest = CloneHelper.deepCopyJAXB( xmlTest, XmlTest.class );
          wrapperTestSet.getTest().add( clonedXmlTest );
        }

        // Move XmlTest(s) with iterations to XmlTestSets.
        // NOTE: This will change the execution order of the tests since XmlTestSets are treated separately from XmlTests.
        xmlTestSet.getTestSet().add( wrapperTestSet );
      }
      else {
        // Keep XmlTest without iterations as it is.
        xmlTests.add( xmlTest );
      }
    }

    // Replace original XmlTests.
    xmlTestSet.getTest().clear();
    xmlTestSet.getTest().addAll( xmlTests );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static XmlTestSet createWrapperTestSet( final XmlTestSet xmlTestSet, final int iterations )
  {
    final XmlTestSet result = new XmlTestSet();

    result.setIterations( null ); // NOTE: Do NOT copy the iterations attribute here!
    result.setDescription( "IterationWrapper( " + iterations + " )" );
    result.setFileName( xmlTestSet.getFileName() );
    result.setOrder( xmlTestSet.getOrder() );
    result.setId( xmlTestSet.getId() );

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static String getFileName( final String filePath )
  {
    if( filePath == null ) {
      return null;
    }

    return Path.of( filePath ).getFileName().toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Writes the given XmlResponseDiffSetup object to a XML file. The path is given, the fileName is taken from the given setup object.
   * @param xmlTestSetup The XmlResponseDiffSetup object to write. Must not be null.
   * @param storeReportPath The path to write the XML file to. Must not be null.
   * @param suffix An optional report file name suffix. May be null.
   * @return The name (entire path) of the XML file.
   * @throws JAXBException
   * @throws IOException
   */
  static String storeXmlReport(
      final XmlResponseDiffSetup xmlTestSetup,
      final String storeReportPath,
      final String suffix
  )
  throws JAXBException, IOException
  {
    // Prepare report file name
    String fileName = xmlTestSetup.getTestSet().get( 0 ).getFileName();
    int pos = Math.max( fileName.lastIndexOf( "\\" ), fileName.lastIndexOf( "/" ) );
    int dot = fileName.lastIndexOf( "." );
    String ending = "";

    // Mask filenames "setup.xml" with folder name.
    String candidate = fileName.substring( pos + 1 );
    if( candidate.toLowerCase().startsWith( "setup." ) ) {
      ending = fileName.substring( dot );
      fileName = fileName.substring( 0, pos );
      pos = Math.max( fileName.lastIndexOf( "\\" ), fileName.lastIndexOf( "/" ) );
    }
    fileName = fileName.substring( pos + 1 ) + ending;

    // Create path if required
    Files.createDirectories( Path.of( storeReportPath ) );
    fileName = storeReportPath
        + "report_" + fileName.substring( 0, fileName.lastIndexOf( "." ) )
        + "_" + FormatHelper.formatIsoDateTime( new Date() ).replaceAll( "T", "_" ).replaceAll( ":", "-" )
        + ( suffix != null ? suffix : "" )
        + ".xml";

    // Store XML report
    final Marshaller jaxbMarshaller = JAXB_CONTEXT_FOR_SETUP.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    OutputStream os = null;
    try {
      os = new FileOutputStream( fileName );
      jaxbMarshaller.marshal( xmlTestSetup, os );
      os.flush();
    }
    finally {
      if( os != null ) {
        try { os.close(); } catch( final Exception ex ) {}
      }
    }

    return fileName;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Converts a given XmlResponseDiffSetup into a XML Document
   * @param xmlTestSetup The XmlResponseDiffSetup to convert.
   * @return The converted XmlResponseDiffSetup as XML Document.
   * @throws ParserConfigurationException
   * @throws JAXBException
   */
  public static Document toDocument( final XmlResponseDiffSetup xmlTestSetup )
  throws ParserConfigurationException, JAXBException
  {
    final Marshaller marshaller = JAXB_CONTEXT_FOR_SETUP.createMarshaller();

    DOCUMENT_BUILDER_FACTORY.setNamespaceAware( true );
    final DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
    final Document doc = db.newDocument();

    marshaller.marshal( xmlTestSetup, doc );

    return doc;
  }
}
