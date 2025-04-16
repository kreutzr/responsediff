package com.github.kreutzr.responsediff.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.tools.Converter;

public class ConverterTest
{
  @Test
  public void testThatAsLocalDateTimeWorks()
  {
    LocalDateTime ldt = null;

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000Z", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000+01", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000+01:00", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000+0100", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000-01", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000-01:00", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00.000-0100", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00Z", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00+01", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00+01:00", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00+0100", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00-01", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00-01:00", null );
    assertThat( ldt ).isNotNull();

    ldt = Converter.asLocalDateTime( "2024-05-02T19:20:00-0100", null );
    assertThat( ldt ).isNotNull();
  }
}
