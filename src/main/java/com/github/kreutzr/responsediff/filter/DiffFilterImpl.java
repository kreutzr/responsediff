package com.github.kreutzr.responsediff.filter;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.kreutzr.responsediff.UnregisteredParameterException;

/**
 * Base class for DiffFilter implementations.
 */
public abstract class DiffFilterImpl implements DiffFilter
{
  private String                testSetupPath_        = null;
  private Set< String >         filterParameterNames_ = new TreeSet<>();
  private Map< String, String > filterParameters_     = new TreeMap<>();

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public DiffFilterImpl()
  {
    registerFilterParameterNames();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This method is invoked initially. Register your filter parameter names here.
   */
  protected void registerFilterParameterNames()
  {
    // Nothing to register here...
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Registers a filter parameter name.
   * <br/><b>NOTE:</b> It is not checked, if the filter parameter name is already registered.
   * @param name The filter parameter name to register. Must not be null.
   */
  protected void registerFilterParameterName( final String name )
  {
    if( name == null ) {
      return;
    }
    filterParameterNames_.add( name );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return A Set that holds all registered filter parameter names. May be empty but never null.
   */
  protected Set< String > getRegisteredFilterParameterNames()
  {
    return filterParameterNames_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public DiffFilter setFilterParameter( final String name, final String value )
  {
    if( !filterParameterNames_.contains(name) ) {
      throw new UnregisteredParameterException( "Unregistered filter parameter \"" + name + "\" must not be used." );
    }

    if( value == null ) {
      filterParameters_.remove( name );
    }
    else {
      filterParameters_.put( name, value );
    }
    return this;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String getFilterParameter( final String name )
  {
    return filterParameters_.get( name );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void init() throws DiffFilterException
  {
    // Override this if required
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void setTestSetupPath( final String testSetupPath )
  {
    testSetupPath_ = testSetupPath;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getTestSetupPath()
  {
    return testSetupPath_;
  }
}
