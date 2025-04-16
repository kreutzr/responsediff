package com.github.kreutzr.responsediff.reporter;

import java.io.File;
import java.io.IOException;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper around AsciiDoctor API.
 */
public class AsciiDocConverter
{
  private static final Logger LOG = LoggerFactory.getLogger( AsciiDocConverter  .class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Converts a given AsciiDoc file to PDF.
   * @param sourceFilePath The source file to use for conversion. Must not be null.
   * @param targetFilePath The target file of the conversion. Must not be null.
   * @throws IOException
   */
  public static void toPdf(
    final String sourceFilePath,
    final String targetFilePath
  )
  throws IOException
  {
    final Attributes attributes = Attributes.builder()
      .sourceHighlighter( "rouge" )
      .icons( "font" )
      .build();

    final Options options = Options.builder()
      .inPlace( true )
      .backend( "pdf" )
      .attributes( attributes )
      .toFile( true )
      .toFile( new File( targetFilePath ) )
      .safe( SafeMode.UNSAFE )
      .build();

    LOG.debug( "Converting adoc to pdf" );
    convert( sourceFilePath, targetFilePath, options );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Converts a given AsciiDoc file to HTML.
   * @param sourceFilePath The source file to use for conversion. Must not be null.
   * @param targetFilePath The target file of the conversion. Must not be null.
   * @throws IOException
   */
  public static void toHtml(
    final String sourceFilePath,
    final String targetFilePath
  )
  throws IOException
  {
    final Attributes attributes = Attributes.builder()
      .sourceHighlighter( "coderay" )
      .tableOfContents( true )
      .tableOfContents( Placement.TOP )
      .icons( "font" )
//      .linkCss( true )
//      .copyCss( true )
//      .styleSheetName( "responseDiff.css" )
      .build();

    final Options options = Options.builder()
      .inPlace( true )
      .backend( "html5" )
      .attributes( attributes )
      .toFile( true )
      .toFile( new File( targetFilePath ) )
      .safe( SafeMode.UNSAFE )
      .build();

    LOG.debug( "Converting adoc to html" );
    convert( sourceFilePath, targetFilePath, options );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Converts a given AsciiDoc file according to the passed options.
   * @param sourceFilePath The source file to use for conversion. Must not be null.
   * @param targetFilePath The target file of the conversion. Must not be null.
   * @param options The Options to use. Must not be null.
   * @throws IOException
   */
  private static void convert(
    final String sourceFilePath,
    final String targetFilePath,
    final Options options
  )
  throws IOException
  {
    final Asciidoctor asciiDoctor = Factory.create();
    asciiDoctor.convertFile( new File( sourceFilePath ), options, String.class );
  }
}
