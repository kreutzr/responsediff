package com.github.kreutzr.responsediff.filter;

import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlTest;

/**
 * A filter interface for ResponseDiff requests.
 */
public interface DiffRequestFilter extends DiffFilter
{
  /**
   * Indicates the filter, that a next variable permutation is requested.
   * <br/><b>NOTE:</b> Candidate, reference and control need to be invoked with the same variables.
   */
  public void next();

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Applies the filters functionality to the given data.
   * @param xmlRequest The XmlRequest to apply the filters functionality to. May be null.
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param xmlTest The current XmlTest. Must not be null.
   * @throws If an error occurs, an Exception is thrown.
  */
  public void apply(
    final XmlRequest xmlRequest,
    final String serviceId,
    final XmlTest xmlTest
  )
  throws DiffFilterException;
}
