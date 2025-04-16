package com.github.kreutzr.responsediff.filter;

import com.github.kreutzr.responsediff.XmlHttpResponse;

/**
 * A filter interface for ResponseDiff responses. 
 */
public interface DiffResponseFilter extends DiffFilter
{
  /**
   * Applies the filters functionality to the given data.
   * @param xmlHttpResponse The XmlHttpResponse to apply the filters functionality to. May be null.
   * @throws If an error occurs, an Exception is thrown.
   */
  public void apply( final XmlHttpResponse xmlHttpResponse ) throws DiffFilterException;
}
