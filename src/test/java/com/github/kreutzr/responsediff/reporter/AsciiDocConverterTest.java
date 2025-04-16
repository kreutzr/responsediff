package com.github.kreutzr.responsediff.reporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.reporter.AsciiDocConverter;

public class AsciiDocConverterTest
{
  @Test
  public void testThatAdocCanBeConvertedToPdf()
  {
    try {
      // Given
      final URL url = AsciiDocConverterTest.class.getClassLoader().getResource( "com/github/kreutzr/responsediff/reporter/AsciiDocConverterTest.adoc" );
      final String sourceFilePath = url.getFile();
      final int pos = sourceFilePath.lastIndexOf( "." );
      final String targetFilePath = sourceFilePath.substring( 0, pos+1 ) + "pdf";

      // When
      AsciiDocConverter.toPdf( sourceFilePath, targetFilePath );

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
      final String sourceFilePath = url.getFile();
      final int pos = sourceFilePath.lastIndexOf( "." );
      final String targetFilePath = sourceFilePath.substring( 0, pos+1 ) + "html";

      // When
      AsciiDocConverter.toHtml( sourceFilePath, targetFilePath );

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
