package com.github.kreutzr.responsediff.reporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.ErrorHandlingHelper;

/**
 * Processes XSLT transformations.
 */
public class XsltProcessor
{
  private static final TransformerFactory     TRANSFORMER_FACTORY      = TransformerFactory.newInstance();
  private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

  private static final Logger LOG = LoggerFactory.getLogger( XsltProcessor.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Writes the output of a given source file to a given target file.
   * @param sourceFilePath The source file to use for transformation. Must not be null.
   * @param xsltFilePath   The transformation description. Must not be null.
   * @param targetFilePath The target file of the transformation. Must not be null.
   * @throws IOException
   * @throws FileNotFoundException
   * @throws Exception
   */
  public static void process(
    final String sourceFilePath,
    final String xsltFilePath,
    final String targetFilePath
  )
  {
    try( InputStream is = new FileInputStream( sourceFilePath ) ) {
      final DocumentBuilder db  = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
      final Document        doc = db.parse( is );

      process( doc, xsltFilePath, targetFilePath );
    }
    catch( IOException | ParserConfigurationException | SAXException ex ) {
      final String message = ErrorHandlingHelper.createSingleLineMessage( "Error while reading XML.", ex );
      LOG.error( message );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Writes the output of a given source file to a given target file.
   * @param doc            The Document to transform. Must not be null.
   * @param xsltFilePath   The transformation description. Must not be null.
   * @param targetFilePath The target file of the transformation. Must not be null.
   * @throws Exception
   */
  public static void process(
    final Document doc,
    final String xsltFilePath,
    final String targetFilePath
  )
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "Processing XSLT: xsltFile=" + xsltFilePath + " , targetFile="  + targetFilePath );
    }
    // Transform source (e.g. XML) to target (e.g. HTML)  via a XSLT file
    try ( FileOutputStream output = new FileOutputStream( targetFilePath ) ) {
      final StreamSource xsltStreamSource = new StreamSource( new File( xsltFilePath ) );

      transform( doc, xsltStreamSource, output );
    }
    catch( IOException | TransformerException ex ) {
      final String message = ErrorHandlingHelper.createSingleLineMessage( "Error while XSLT processing.", ex );
      LOG.error( message );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void transform(
    final Document doc,
    final StreamSource xsltStreamSource,
    final OutputStream output
  )
  throws TransformerException
  {
    final Transformer transformer = TRANSFORMER_FACTORY.newTransformer( xsltStreamSource );

    // Do the transformation
    transformer.transform( new DOMSource( doc ), new StreamResult( output ) );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void main( final String[] args )
  {
    try {
      String xmlFilePath    = "C:/home/rkreutz/work/develop/test/responsediff/xslt/report.xml";
      String xsltFilePath   = "C:/home/rkreutz/work/develop/test/responsediff/src/main/resources/com/github/kreutzr/responsediff/reporter/report-to-adoc.xslt";
      String targetFilePath = xmlFilePath.substring(0, xmlFilePath.lastIndexOf( "." ) ) + ".adoc";

      if( args.length >= 1 ) { xmlFilePath    = Converter.asString( args[ 0 ], xmlFilePath ); }
      if( args.length >= 2 ) { xsltFilePath   = Converter.asString( args[ 1 ], xsltFilePath ); }
      if( args.length >= 3 ) { targetFilePath = Converter.asString( args[ 2 ], targetFilePath ); }

      System.setProperty( "classpath", "C:/");
      XsltProcessor.process(
        xmlFilePath,
        xsltFilePath,
        targetFilePath
      );
    }
    catch( final Exception ex ) {
      ex.printStackTrace();
    }
  }
}
