package com.github.kreutzr.responsediff.doc;

import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffResponseFilterImpl;

import com.github.kreutzr.responsediff.XmlHttpResponse;

public class MyResponseFilter extends DiffResponseFilterImpl
{
  private static final String MY_FILTER_PARAMETER = "...";

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();

    registerFilterParameterName( MY_FILTER_PARAMETER );
    // ...
  }

  @Override
  public void apply( final XmlHttpResponse xmlHttpResponse ) throws DiffFilterException
  {
    super.apply( xmlHttpResponse );
    // ...
  }
}