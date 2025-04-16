package com.github.kreutzr.responsediff.doc;

import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffFilterImpl;
import com.github.kreutzr.responsediff.filter.DiffRequestFilter;

import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlTest;

public class MyRequestFilter extends DiffFilterImpl implements DiffRequestFilter
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
  public void apply( final XmlRequest xmlRequest, final String serviceId, final XmlTest xmlTest ) throws DiffFilterException
  {
    // ...
  }

  @Override
  public void next()
  {
    // ...
  }
}