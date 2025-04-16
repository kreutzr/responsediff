package com.github.kreutzr.responsediff;

/**
 * This Exception is thrown if an unregistered parameter name is used.
 */
public class UnregisteredParameterException extends RuntimeException
{
  private static final long serialVersionUID = 4333223029857615707L;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public UnregisteredParameterException()
   {
     this( "Unregistered parameter must not be used." );
   }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public UnregisteredParameterException( final String message )
   {
     super( message );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   public UnregisteredParameterException( final String message, final Throwable ex )
   {
     super( message, ex );
   }
}
