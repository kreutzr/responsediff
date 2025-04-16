package com.github.kreutzr.responsediff.filter;

import com.github.kreutzr.responsediff.UnregisteredParameterException;

/**
 * A filter interface for ResponseDiff.
 */
public interface DiffFilter
{
  /**
   * Set a filter parameter.
   * @param name The name of the filter parameter. Must not be null.
   * @param value The value of the filter parameter. May be null.
   * @return this.
   * @throws UnregisteredParameterException If the filter parameter name is not supported, an Exception is thrown.
   */
  public DiffFilter setFilterParameter( final String name, final String value ) throws UnregisteredParameterException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Looks up the value of a filter parameter.
   * @param name The name of the filter parameter to lookup. Must not be null.
   * @return The value of the filter parameter. May be null.
   */
  public String getFilterParameter( final String name );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Initializes the filter according to the parameters.
   * @throws DiffFilterException If an error occurs, an Exception is thrown.
   */
  public void init() throws DiffFilterException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the path to the central test setup XML file. This may be used for filters which read their configuration from the file system.
   * @param path The path to the central test setup XML file. May be null.
   */
  public void setTestSetupPath( final String path );
}
