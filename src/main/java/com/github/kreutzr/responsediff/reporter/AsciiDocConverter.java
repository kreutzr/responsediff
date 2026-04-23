package com.github.kreutzr.responsediff.reporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
  private static final Logger LOG = LoggerFactory.getLogger( AsciiDocConverter.class );

  // JavaScript for [.copy-code] support in XSLT for HTML
  private static final String TEMP_DOCINFO_FILE_NAME = "docinfo-footer";
  private static final String DOCINFO_CONTENT =
    "<style>\n" +
    "  .copy-btn {\n" +
    "    position: absolute; top: 5px; right: 5px; opacity: 0.7; cursor: pointer;\n" +
    "    z-index: 10; background: #52677a; color: white; border: none;\n" +
    "    border-radius: 3px; padding: 2px 8px; font-size: 0.8em;\n" +
    "  }\n" +
    "  .copy-btn:hover { opacity: 1.0; }\n" +
    "</style>\n" +
    "<script>\n" +
    "document.addEventListener('DOMContentLoaded', function() {\n" +
    "  // Target .copy-code containers (works for blocks and 'a|' table cells)\n" +
    "  document.querySelectorAll('.copy-code').forEach(function(contentBlock) {\n" +
    "    if (contentBlock.querySelector('.copy-btn')) return;\n" +
    "\n" +
    "    let btn = document.createElement('button');\n" +
    "    btn.innerText = 'Copy';\n" +
    "    btn.className = 'copy-btn';\n" +
    "\n" +
    "    btn.onclick = function() {\n" +
    "      // Use <pre> if available (code blocks), otherwise use the block itself (table cells)\n" +
    "      let target = contentBlock.querySelector('pre') || contentBlock;\n" +
    "      let text = target.innerText;\n" +
    "\n" +
    "      // Strip the button label 'Copy' or 'Copied!' from the text if it was captured\n" +
    "      if (text.endsWith('Copied!')) text = text.slice(0, -7);\n" +
    "      else if (text.endsWith('Copy')) text = text.slice(0, -4);\n" +
    "\n" +
    "      navigator.clipboard.writeText(text.trim()).then(() => {\n" +
    "        btn.innerText = 'Copied!';\n" +
    "        setTimeout(() => btn.innerText = 'Copy', 2000);\n" +
    "      });\n" +
    "    };\n" +
    "\n" +
    "    contentBlock.style.position = 'relative';\n" +
    "    contentBlock.appendChild(btn);\n" +
    "  });\n" +
    "});\n" +
    "</script>";

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
    // Create temporary HTML file to be included for [.copy-code] support in XSLT
    final Path tempDocinfo = Paths.get( new File( sourceFilePath ).getParent(), TEMP_DOCINFO_FILE_NAME + ".html" );
    Files.write( tempDocinfo, DOCINFO_CONTENT.getBytes( StandardCharsets.UTF_8 ) );

    try {
      final Attributes attributes = Attributes.builder()
        .sourceHighlighter( "coderay" )
        .tableOfContents( true )
        .tableOfContents( Placement.TOP )
        .icons( "font" )
        .attribute( "docinfo", "shared-footer" )                                               // Required for "[.copy-code]" support in XSLT.
        .attribute( "docinfodir", new File(sourceFilePath).getParentFile().getAbsolutePath() ) // Required for "[.copy-code]" support in XSLT.
//        .linkCss( true )
//        .copyCss( true )
//        .styleSheetName( "responseDiff.css" )
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
    finally {
      // Clean up: Remove temporary HTML file for [.copy-code] support in XSLT
      Files.deleteIfExists( tempDocinfo );
    }
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
