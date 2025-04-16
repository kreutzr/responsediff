package com.github.kreutzr.responsediff.filter;

/**
 * Exception class that might be thrown when using a DiffFilter.
 */
public class DiffFilterException extends Exception
{
  private static final long serialVersionUID = -4314875184382368034L;
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public DiffFilterException( final String message )
  {
    super( message ); 
  }
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public DiffFilterException( final Throwable ex )
  {
    super( ex ); 
  }
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public DiffFilterException( final String message, final Throwable ex )
  {
    super( message, ex ); 
  }
}
