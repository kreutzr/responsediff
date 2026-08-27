package com.github.kreutzr.responsediff.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.UnregisteredParameterException;
import com.github.kreutzr.responsediff.base.TestRoot;
import com.github.kreutzr.responsediff.filter.request.RemoveHeaderRequestFilter;
import com.github.kreutzr.responsediff.filter.response.SortJsonBodyResponseFilter;

public class DiffFilterImplTest extends TestRoot
{
  @Test
  public void testThatConstructorWorks()
  {
    try {
        testThatPublicConstructorWorks( DummyRequestFilter.class );
        testThatPublicConstructorWorks( DummyResponseFilter.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      assertThat( false ).isEqualTo( true ).withFailMessage( "Unreachable" );
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatParameterRegistrationWorks()
  {
    final String parameterName     = "Test";
    final String parameterValue    = "Some value";
    final String newParameterValue = "New value";

    final DummyRequestFilter requestFilter = new DummyRequestFilter();
    requestFilter.registerFilterParameterNames( 0, new String[] { null, parameterName } );
    assertThat( requestFilter.getRegisteredFilterParameterNames() ).hasSize( 1 ).contains( parameterName );
    
    requestFilter.setFilterParameter( parameterName, parameterValue );
    assertThat( requestFilter.getRegisteredFilterParameterNames() ).hasSize( 1 );
    assertThat( requestFilter.getFilterParameter( parameterName ) ).isEqualTo( parameterValue );
    
    // Removed from filter parameter
    requestFilter.setFilterParameter( parameterName, null );
    assertThat( requestFilter.getRegisteredFilterParameterNames() ).hasSize( 1 );
    assertThat( requestFilter.getFilterParameter( parameterName ) ).isNull();

    // Re-Added to filter parameters
    requestFilter.setFilterParameter( parameterName, newParameterValue );
    assertThat( requestFilter.getRegisteredFilterParameterNames() ).hasSize( 1 );
    assertThat( requestFilter.getFilterParameter( parameterName ) ).isEqualTo( newParameterValue );
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    final DummyResponseFilter responseFilter = new DummyResponseFilter();
    responseFilter.registerFilterParameterNames( 0, new String[] { null, parameterName } );
    assertThat( responseFilter.getRegisteredFilterParameterNames() ).hasSize( 2 ).contains( parameterName ); // 2 = DiffresponseFilterImpl.PARAMETER_NAME__STORE_ORIGINAL is automatically registered

    responseFilter.setFilterParameter( parameterName, parameterValue );
    assertThat( responseFilter.getRegisteredFilterParameterNames() ).hasSize( 2 );
    assertThat( responseFilter.getFilterParameter( parameterName ) ).isEqualTo( parameterValue );
    
    // Removed from filter parameter
    responseFilter.setFilterParameter( parameterName, null );
    assertThat( responseFilter.getRegisteredFilterParameterNames() ).hasSize( 2 );
    assertThat( responseFilter.getFilterParameter( parameterName ) ).isNull();

    // Re-Added to filter parameters
    responseFilter.setFilterParameter( parameterName, newParameterValue );
    assertThat( responseFilter.getRegisteredFilterParameterNames() ).hasSize( 2 );
    assertThat( responseFilter.getFilterParameter( parameterName ) ).isEqualTo( newParameterValue );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatSettingAnUnregisteredParameterThrowsAnException()
  {
    final String unregisteredParameterName = "UNREGISTED";
    final DiffRequestFilter requestFilter = new RemoveHeaderRequestFilter();
    
    Throwable ex = catchThrowable( () -> requestFilter.setFilterParameter( unregisteredParameterName, "Some value" ) );
    assertThat( ex )
      .isInstanceOf( UnregisteredParameterException.class )
      .hasMessageContaining( "Unregistered filter parameter \"" + unregisteredParameterName + "\" must not be used." );
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    final DiffResponseFilter responseFilter = new SortJsonBodyResponseFilter();
    
    ex = catchThrowable( () -> responseFilter.setFilterParameter( unregisteredParameterName, "Some value" ) );
    assertThat( ex )
      .isInstanceOf( UnregisteredParameterException.class )
      .hasMessageContaining( "Unregistered filter parameter \"" + unregisteredParameterName + "\" must not be used." );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testThatTestSetupPathIsAccessable()
  {
    final String path = "Some path value";
    final DiffRequestFilter requestFilter = new RemoveHeaderRequestFilter();
    requestFilter.setTestSetupPath( path );
    assertThat( requestFilter.getTestSetupPath() ).isEqualTo( path );
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    final DiffResponseFilter responseFilter = new SortJsonBodyResponseFilter();
    responseFilter.setTestSetupPath( path );
    assertThat( responseFilter.getTestSetupPath() ).isEqualTo( path );
  }
}
