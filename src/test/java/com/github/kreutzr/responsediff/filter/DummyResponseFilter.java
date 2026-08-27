package com.github.kreutzr.responsediff.filter;

import com.github.kreutzr.responsediff.XmlHttpResponse;

public class DummyResponseFilter extends DiffResponseFilterImpl implements DiffResponseFilter
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
  public void apply( final XmlHttpResponse xmlHttpResponse )
  throws DiffFilterException
  {
	super.apply( xmlHttpResponse );

	setContentTypeHeader( xmlHttpResponse, "application/pdf" );
	setContentTypeHeader( xmlHttpResponse, "application/json" ); // We test more than one path for JaCoCo (test that overriding works)
  }
}
