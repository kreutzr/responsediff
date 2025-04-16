package com.github.kreutzr.responsediff.base;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import javax.net.ssl.SSLSession;

import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlHeaders;

/**
 * HttpResponse instance used for testing
 */
public class HttpResponseInstance implements HttpResponse< byte[] > {

  private int         status_;
  private HttpRequest request_;
  private HttpHeaders headers_;
  private byte[]      body_;

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  public HttpResponseInstance(
    final int         status,
    final HttpRequest request,
    final byte[]      body,
    final XmlHeaders  xmlHeaders
  )
  {
    status_  = status;
    request_ = request;
    body_    = body;

    final Map< String, List< String > > map = new LinkedHashMap<>();

    if( xmlHeaders != null ) {
      for( final XmlHeader xmlHeader : xmlHeaders.getHeader() ) {
        final String key = xmlHeader.getName();
        final List< String > value = new ArrayList<>();
        value.add( xmlHeader.getValue() );
        map.put( key, value );
      }
    }

    headers_ = HttpHeaders.of( map, new BiPredicate< String, String >() {
      @Override
      public boolean test( final String t, final String u) {
        // The filter accepts everything
        return true;
      }
    });
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public int statusCode() {
    return status_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public HttpRequest request() {
    return request_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Optional< HttpResponse< byte[] > >  previousResponse() {
    return Optional.empty();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public HttpHeaders headers() {
    return headers_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public byte[] body() {
    return body_;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Optional< SSLSession > sslSession() {
    return Optional.empty();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public URI uri() {
    return null;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Version version() {
    return null;
  }
}