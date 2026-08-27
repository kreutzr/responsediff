package com.github.kreutzr.responsediff.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.base.TestRoot;

public class ConverterTest extends TestRoot
{
  private static final int[] OPTIONS_THROW_EXCEPION = new int[] { Converter.THROW_CONVERSION_EXCEPTION };

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatConstructorWorks()
  {
    try {
      testThatPublicConstructorWorks( Converter.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatIsOptionsSetWorks()
  {
    String result = null;

    result = Converter.asString( new BrokenObject(), "Fallback", null );
    assertThat( result ).isNotNull().isEqualTo( "Fallback" );

    result = Converter.asString( new BrokenObject(), "Fallback", new int[] { Integer.MIN_VALUE } );
    assertThat( result ).isNotNull().isEqualTo( "Fallback" );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsStringWorks()
  {
    String result = null;

    result = Converter.asString( "Test", null );
    assertThat( result ).isNotNull().isEqualTo( "Test" );

    result = Converter.asString( null, "Fallback" );
    assertThat( result ).isNotNull().isEqualTo( "Fallback" );

    result = Converter.asString( Long.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asString( new BrokenObject(), "Fallback" );
    assertThat( result ).isNotNull().isEqualTo( "Fallback" );

    Throwable ex = catchThrowable( () -> Converter.asString( new BrokenObject(), "Fallback", OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsDateWorks()
  {
    final Date fallback = new Date();

    Date result = null;

    result = Converter.asDate( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asDate( new Date(), null );
    assertThat( result ).isNotNull();

    result = Converter.asDate( "2024-05-02", null );
    assertThat( result ).isNotNull();

    Throwable ex = catchThrowable( () -> Converter.asDate( "2024-05", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( "Parameter \"2024-05\" has invalid length of 7." );

    result = Converter.asDate( "Unparsable", fallback );
    assertThat( result ).isNotNull();

    ex = catchThrowable( () -> Converter.asDate( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsLocalDateWorks()
  {
    final LocalDate fallback = LocalDate.now();

    LocalDate result = null;

    result = Converter.asLocalDate( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDate( LocalDate.now(), null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDate( "2024-05-02", null );
    assertThat( result ).isNotNull();

    Throwable ex = catchThrowable( () -> Converter.asLocalDate( "2024-05", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( "Parameter \"2024-05\" has invalid length of 7." );

    result = Converter.asLocalDate( "Unparsable", fallback );
    assertThat( result ).isNotNull();

    ex = catchThrowable( () -> Converter.asLocalDate( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );

    result = Converter.asLocalDate( new BrokenObject(), LocalDate.of( 2024, 5, 2 ) );
    assertThat( result ).isNotNull();

    ex = catchThrowable( () -> Converter.asLocalDate( new BrokenObject(), LocalDate.of( 2024, 5, 2 ), OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsLocalDateTimeWorks()
  {
    final LocalDateTime fallback = LocalDateTime.now();

    LocalDateTime result = null;

    result = Converter.asLocalDateTime( LocalDateTime.now(), null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000Z", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000+01", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000+01:00", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000+0100", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000-01", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000-01:00", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00.000-0100", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00Z", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00+01", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00+01:00", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00+0100", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00-01", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00-01:00", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02T19:20:00-0100", null );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( new BrokenObject(), fallback );
    assertThat( result ).isNotNull();

    result = Converter.asLocalDateTime( "2024-05-02Taaaaaaaa", null );
    assertThat( result ).isNull();

    Throwable ex = catchThrowable( () -> Converter.asLocalDateTime( new BrokenObject(), fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );

    ex = catchThrowable( () -> Converter.asLocalDateTime( "2024-05", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( "Parameter \"2024-05\" has invalid length of 7." );

    result = Converter.asLocalDateTime( "Unparsable", fallback );
    assertThat( result ).isNotNull();

    ex = catchThrowable( () -> Converter.asLocalDateTime( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsDurationWorks()
  {
    Duration fallback = Duration.parse( "PT1h" );

    Duration result = null;

    result = Converter.asDuration( Duration.parse( "PT2h" ), null );
    assertThat( result ).isNotNull();

    result = Converter.asDuration( "PT2h", null );
    assertThat( result ).isNotNull();

    result = Converter.asDuration( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asDuration( new BrokenObject(), fallback );
    assertThat( result ).isNotNull();

    Throwable ex = catchThrowable( () -> Converter.asDuration( new BrokenObject(), fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );

    ex = catchThrowable( () -> Converter.asDuration( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsLongWorks()
  {
    Long fallback = Long.MAX_VALUE;

    Long result = null;

    result = Converter.asLong( Long.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asLong( Integer.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asLong( "5", null );
    assertThat( result ).isNotNull();

    result = Converter.asLong( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asLong( new BrokenObject(), fallback );
    assertThat( result ).isNotNull();

    Throwable ex = catchThrowable( () -> Converter.asLong( new BrokenObject(), fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );

    ex = catchThrowable( () -> Converter.asLong( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsIntegerWorks()
  {
    Integer fallback = Integer.MAX_VALUE;

    Integer result = null;

    result = Converter.asInteger( Integer.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asInteger( Integer.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asInteger( "5", null );
    assertThat( result ).isNotNull();

    result = Converter.asInteger( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asInteger( new BrokenObject(), fallback );
    assertThat( result ).isNotNull();

    Throwable ex = catchThrowable( () -> Converter.asInteger( new BrokenObject(), fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );

    ex = catchThrowable( () -> Converter.asInteger( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsDoubleWorks()
  {
    Double fallback = Double.MAX_VALUE;

    Double result = null;

    result = Converter.asDouble( Double.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asDouble( Integer.MAX_VALUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asDouble( "5.5", null );
    assertThat( result ).isNotNull().isEqualTo( 5.5 );

    result = Converter.asDouble( null, fallback );
    assertThat( result ).isNotNull();

    result = Converter.asDouble( new BrokenObject(), fallback );
    assertThat( result ).isNotNull();

    Throwable ex = catchThrowable( () -> Converter.asDouble( new BrokenObject(), fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );

    ex = catchThrowable( () -> Converter.asDouble( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatAsBooleanWorks()
  {
    Boolean fallback = Boolean.FALSE;

    Boolean result = null;

    result = Converter.asBoolean( Boolean.TRUE, null );
    assertThat( result ).isNotNull();

    result = Converter.asBoolean( "true", null );
    assertThat( result ).isNotNull().isEqualTo( Boolean.TRUE );

    result = Converter.asBoolean( null, fallback );
    assertThat( result ).isNotNull().isEqualTo( fallback );

    result = Converter.asBoolean( new BrokenObject(), fallback );
    assertThat( result ).isNotNull().isEqualTo( fallback );

    Throwable ex = catchThrowable( () -> Converter.asBoolean( new BrokenObject(), fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat( ex )
      .isInstanceOf( IllegalArgumentException.class )
      .hasMessageContaining( BrokenObject.ERROR_MESSAGE );

    ex = catchThrowable( () -> Converter.asBoolean( "Unparsable", fallback, OPTIONS_THROW_EXCEPION ) );
    assertThat(ex)
      .isInstanceOf( IllegalArgumentException.class );
  }

}
