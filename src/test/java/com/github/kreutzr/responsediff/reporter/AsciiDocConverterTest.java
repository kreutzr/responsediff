package com.github.kreutzr.responsediff.reporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class AsciiDocConverterTest
{
  @Test
  public void testThatAdocCanBeConvertedToPdf()
  {
    try {
      // Given
      final URL url = AsciiDocConverterTest.class.getClassLoader().getResource( "com/github/kreutzr/responsediff/reporter/AsciiDocConverterTest.adoc" );
      final String reportFilePath = url.getFile();
      final int pos = reportFilePath.lastIndexOf( "." );
      final String targetFilePath = reportFilePath.substring( 0, pos+1 ) + "pdf";
      final String transformerFilePath = targetFilePath; // NEEDS FIX C: Add test with custom style
      final boolean useLogo = false;

      // When
      AsciiDocConverter.toPdf( reportFilePath, targetFilePath, transformerFilePath, useLogo );

      // Then
      final File report = new File( targetFilePath );
      assertTrue( report.exists() );
      assertThat( report.length() ).isGreaterThan( 0L );

      // Cleanup
      report.delete();
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertTrue( false, "Unreachable" );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAdocCanBeConvertedToHtml()
  {
    try {
      // Given
      final URL url = AsciiDocConverterTest.class.getClassLoader().getResource( "com/github/kreutzr/responsediff/reporter/AsciiDocConverterTest.adoc" );
      final String reportFilePath = url.getFile();
      final int pos = reportFilePath.lastIndexOf( "." );
      final String targetFilePath = reportFilePath.substring( 0, pos+1 ) + "html";
      final String transformerFilePath = targetFilePath; // NEEDS FIX C: Add test with custom style
      final boolean useLogo = false;

      // When
      AsciiDocConverter.toHtml( reportFilePath, targetFilePath, transformerFilePath, useLogo );

      // Then
      final File report = new File( targetFilePath );
      assertTrue( report.exists() );
      assertThat( report.length() ).isGreaterThan( 0L );

      // Cleanup
      report.delete();
    }
    catch( final Throwable ex )
    {
      ex.printStackTrace();
      assertTrue( false, "Unreachable" );
    }
  }
}
