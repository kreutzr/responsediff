package com.github.kreutzr.responsediff.reporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper around AsciiDoctor API to allow theme customization.
 */
public class AsciiDocConverter
{
  private static final Logger LOG = LoggerFactory.getLogger( AsciiDocConverter.class );

  // Style file names for HTML and PDF customization
  public  static final String LOGO_FILE_NAME        = "logo.png";
  private static final String STYLE_FILE_NAME__HTML = "custom-html-overrides.html";
  private static final String STYLE_FILE_NAME__PDF  = "custom-pdf-theme.yml"; // Name must end with "-theme.yml" (yaml will not be found)

  // JavaScript for [.copy-code] support in XSLT for HTML
  private static final String DOCINFO_FILE_NAME        = "docinfo.html";        // Filename must not be changed (AsciiDoctor specific)
  private static final String DOCINFO_FOOTER_FILE_NAME = "docinfo-footer.html"; // Filename must not be changed (AsciiDoctor specific)
  private static final String DOCINFO_FOOTER_CONTENT =
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
   * @param sourceFilePath      The source      file (adoc) to use for conversion. Must not be null.
   * @param targetFilePath      The target      file (html) of the conversion. Must not be null.
   * @param transformerFilePath The transformer file (xslt) used to create the source file (adoc). Must not be null.
   * @param useLogo             Flag, if the report should have a logo (true) or not (false).
   * @throws IOException
   */
  public static void toPdf(
    final String sourceFilePath,
    final String targetFilePath,
    final String transformerFilePath,
    final boolean useLogo
  )
  throws IOException
  {
    final String transformerFolderPath = Paths.get( new File( transformerFilePath ).getParent() ).toString();
    final String targetFolderPath      = Paths.get( new File( targetFilePath      ).getParent() ).toString();

    final AttributesBuilder attributesBuilder = Attributes.builder()
      .sourceHighlighter( "rouge" )
      .icons( "font" )
      ;

    //Check if custom style for PDF exists
    if( Paths.get( transformerFolderPath, STYLE_FILE_NAME__PDF ).toFile().exists() ) {
      final int pos = STYLE_FILE_NAME__PDF.lastIndexOf( "-theme" );
      attributesBuilder
        .attribute( "pdf-themesdir", new File( transformerFolderPath ).getAbsolutePath() )
        .attribute( "pdf-theme", STYLE_FILE_NAME__PDF.substring( 0, pos ) ) // Cut off "-theme" plus file extension here!
        ;
    }

    final Attributes attributes = attributesBuilder
      .build();

    final Options options = Options.builder()
      .inPlace( true )
      .backend( "pdf" )
      .attributes( attributes )
      .toFile( true )
      .toFile( new File( targetFilePath ) )
      .safe( SafeMode.UNSAFE )
      .build();
      ;

    final boolean logoFileExisted = copyLogoIfRequired( useLogo, transformerFolderPath, targetFolderPath );

    LOG.debug( "Converting adoc to pdf" );
    convert( sourceFilePath, targetFilePath, options );

    // Clean up logo file if not required by other reports (e.g. HTML report)
    if( !logoFileExisted ) {
      deleteLogoFile( targetFolderPath );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Converts a given AsciiDoc file to HTML.
   * @param sourceFilePath      The source      file (adoc) to use for conversion. Must not be null.
   * @param targetFilePath      The target      file (html) of the conversion. Must not be null.
   * @param transformerFilePath The transformer file (xslt) used to create the source file (adoc). Must not be null.
   * @param useLogo             Flag, if the report should have a logo (true) or not (false).
   * @throws IOException
   */
  public static void toHtml(
    final String sourceFilePath,
    final String targetFilePath,
    final String transformerFilePath,
    final boolean useLogo
  )
  throws IOException
  {
    final String transformerFolderPath = Paths.get( new File( transformerFilePath ).getParent() ).toString();
    final String targetFolderPath      = Paths.get( new File( targetFilePath      ).getParent() ).toString();

    // Create temporary docinfo file to be included (only if a custom style override file exists)
    Path docinfoPath = null;
    final Path customStyleFilePath = Paths.get( transformerFolderPath, STYLE_FILE_NAME__HTML );
    if( customStyleFilePath.toFile().exists() ) {
      docinfoPath = Paths.get( targetFolderPath, DOCINFO_FILE_NAME );
      try {
        Files.copy( customStyleFilePath, docinfoPath, StandardCopyOption.REPLACE_EXISTING );
      }
      catch( final Throwable ex ) {
        ex.printStackTrace();
        docinfoPath = null;
      }
    }

    // Always create temporary docinfo footer file to be included for [.copy-code] support in XSLT
    final Path docinfoFooterPath = Paths.get( targetFolderPath, DOCINFO_FOOTER_FILE_NAME );
    Files.write( docinfoFooterPath, DOCINFO_FOOTER_CONTENT.getBytes( StandardCharsets.UTF_8 ) );

    try {
      final AttributesBuilder attributesBuilder = Attributes.builder()
        .sourceHighlighter( "coderay" )
        .tableOfContents( true )
        .tableOfContents( Placement.TOP )
        .icons( "font" )
        .attribute( "docinfo", docinfoPath != null ? "shared,footer" : "footer" )  // Required for "[.copy-code]" support in XSLT ("footer") and for custom style ("shared").
        .attribute( "docinfodir", new File( targetFolderPath ).getAbsolutePath() ) // Required for "[.copy-code]" support in XSLT.
        ;

      final Attributes attributes = attributesBuilder
        .build();

      final Options options = Options.builder()
        .inPlace( true )
        .backend( "html5" )
        .attributes( attributes )
        .toFile( true )
        .toFile( new File( targetFilePath ) )
        .safe( SafeMode.UNSAFE )
        .build();

      copyLogoIfRequired( useLogo, transformerFolderPath, targetFolderPath );

      LOG.debug( "Converting adoc to html" );
      convert( sourceFilePath, targetFilePath, options );
    }
    finally {
      // Clean up: Remove temporary docinfo footer file for [.copy-code] support in XSLT
      try{ Files.deleteIfExists( docinfoFooterPath ); } catch( final Exception ex ) { ex.printStackTrace(); }

      // Clean up: Remove temporary docinfo file for custom style
      if( docinfoPath != null ) {
        try{ Files.deleteIfExists( docinfoPath ); } catch( final Exception ex ) { ex.printStackTrace(); }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Checks if the logo source file exists.
   * @param transformerFolderPath The folder to look for the logo file.
   * @return True if the logo source file exists. Otherwise false is returned.
   */
  public static boolean getLogoSourceExists( final String transformerFolderPath )
  {
    return Paths.get( transformerFolderPath, LOGO_FILE_NAME ).toFile().exists();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Copies a logo file (see LOGO_FILE_NAME) to a given target folder.
   * @param useLogo Flag, if the logo shall be copied (true) or not (false).
   * @param transformerFolderPath The folder to look for the logo file (next to the XSL transformer). Must not be null.
   * @param targetFolderPath The target folder. Must not be null.
   * @return True if the logo file already existed. Otherwise false is returned.
   * @throws IOException
   */
  private static boolean copyLogoIfRequired(
    final boolean useLogo,
    final String transformerFolderPath,
    final String targetFolderPath
  )
  throws IOException
  {
    boolean logoFileExisted = false;

    if( useLogo ) {
      final Path logoPath = Paths.get( transformerFolderPath, LOGO_FILE_NAME );
      if( logoPath.toFile().exists() ) {
        logoFileExisted = Paths.get( targetFolderPath, LOGO_FILE_NAME ).toFile().exists();
        Files.copy( logoPath, Paths.get( targetFolderPath, LOGO_FILE_NAME ), StandardCopyOption.REPLACE_EXISTING );
      }
    }
    return logoFileExisted;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Delete the logo file from a given folder.
   * @param targetFolderPath The folder to remove the logo file from.
   * @throws IOException
   */
  private static void deleteLogoFile( final String targetFolderPath )
  throws IOException
  {
    try {
      Files.deleteIfExists( Paths.get( targetFolderPath, LOGO_FILE_NAME ) );
    }
    catch( final InvalidPathException ex ) {
      // Ignore this. This is thrown when a unit test tries to access a resource but the path is absolute and includes a ":" for windows.
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
