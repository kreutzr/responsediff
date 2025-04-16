package com.github.kreutzr.responsediff.filter.request;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffFilterImpl;
import com.github.kreutzr.responsediff.filter.DiffRequestFilter;
import com.github.kreutzr.responsediff.tools.Converter;

import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlTest;

/**
 * A filter that removes request headers.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>id="names", values=content-type, content-length</li>
 * </ul>
 */
public class RemoveHeaderRequestFilter extends DiffFilterImpl implements DiffRequestFilter
{
  public  static final String PARAMETER_NAME__NAMES = "names";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger( RemoveHeaderRequestFilter.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Set< String > names_ = new HashSet<>();

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__NAMES );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void next()
  {
    // Do nothing
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply(
    final XmlRequest xmlRequest,
    final String serviceId,
    final XmlTest xmlTest
  )
  throws DiffFilterException
  {
    final Iterator< XmlHeader > it = xmlRequest.getHeaders().getHeader().iterator();
    while( it.hasNext() ) {
      final XmlHeader xmlHeader = it.next();
      if( names_.contains( xmlHeader.getName().toLowerCase() ) ) {
        it.remove();
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void init() throws DiffFilterException
  {
    String[] names = Converter.asString( getFilterParameter( PARAMETER_NAME__NAMES ), "" ).split( "," );
    for( final String name : names ) {
      names_.add( name.toLowerCase().trim() ); // HTTP spec says that header names are case-insensitive ( see "https://datatracker.ietf.org/doc/html/rfc2616#section-4.2")
    }
  }
}
