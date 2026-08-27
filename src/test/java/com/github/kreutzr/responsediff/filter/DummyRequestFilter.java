package com.github.kreutzr.responsediff.filter;

import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlTest;

public class DummyRequestFilter extends DiffFilterImpl implements DiffRequestFilter 
{
  /**
   * @param dummy Used to allow public access to protected super method.
   * @param parameterNames The parameters to register.
   */
  public void registerFilterParameterNames( final int dummy, final String[] parameterNames )
  {
    super.registerFilterParameterNames();
    for( final String parameterName : parameterNames ) {
      registerFilterParameterName( parameterName );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void next() 
  {
    // Do nothing
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply( final XmlRequest xmlRequest, final String serviceId, final XmlTest xmlTest )
  throws DiffFilterException 
  {
    // Do nothing
  }
}
