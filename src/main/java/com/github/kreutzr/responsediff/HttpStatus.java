package com.github.kreutzr.responsediff;

import java.util.HashMap;
import java.util.Map;

public enum HttpStatus
{
  STATUS_100( 100, "Continue" ),
  STATUS_101( 101, "Switching Protocols" ),
  STATUS_102( 102, "Processing" ),
  STATUS_103( 103, "Early Hints" ),

  STATUS_200( 200, "OK" ),
  STATUS_201( 201, "Created" ),
  STATUS_202( 202, "Accepted" ),
  STATUS_203( 203, "Non-Authoritative Information" ),
  STATUS_204( 204, "No Content" ),
  STATUS_205( 205, "Reset Content" ),
  STATUS_206( 206, "Partial Content" ),
  STATUS_207( 207, "Multi-Status" ),
  STATUS_208( 208, "Already Reported" ),
  STATUS_226( 226, "IM Used" ),

  STATUS_300( 300, "Multiple Choices" ),
  STATUS_301( 301, "Moved Permanently" ),
  STATUS_302( 302, "Found (Moved Temporarily)" ),
  STATUS_303( 303, "See Other" ),
  STATUS_304( 304, "Not Modified" ),
  STATUS_305( 305, "Use Proxy" ),
  STATUS_306( 306, "(reserviert)" ),
  STATUS_307( 307, "Temporary Redirect" ),
  STATUS_308( 308, "Permanent Redirect" ),

  STATUS_400( 400, "Bad Request" ),
  STATUS_401( 401, "Unauthorized" ),
  STATUS_402( 402, "Payment Required" ),
  STATUS_403( 403, "Forbidden" ),
  STATUS_404( 404, "Not Found" ),
  STATUS_405( 405, "Method Not Allowed" ),
  STATUS_406( 406, "Not Acceptable" ),
  STATUS_407( 407, "Proxy Authentication Required" ),
  STATUS_408( 408, "Request Timeout" ),
  STATUS_409( 409, "Conflict" ),
  STATUS_410( 410, "Gone" ),
  STATUS_411( 411, "Length Required" ),
  STATUS_412( 412, "Precondition Failed" ),
  STATUS_413( 413, "Payload Too Large" ),
  STATUS_414( 414, "URI Too Long[16]" ),
  STATUS_415( 415, "Unsupported Media Type" ),
  STATUS_416( 416, "Range Not Satisfiable" ),
  STATUS_417( 417, "Expectation Failed" ),
  STATUS_418( 418, "I’m a teapot" ),
  STATUS_420( 420, "Policy Not Fulfilled" ),
  STATUS_421( 421, "Misdirected Request" ),
  STATUS_422( 422, "Unprocessable Entity" ),
  STATUS_423( 423, "Locked" ),
  STATUS_424( 424, "Failed Dependency" ),
  STATUS_425( 425, "Too Early" ),
  STATUS_426( 426, "Upgrade Required" ),
  STATUS_428( 428, "Precondition Required" ),
  STATUS_429( 429, "Too Many Requests" ),
  STATUS_431( 431, "Request Header Fields Too Large" ),
  STATUS_444( 444, "No Response" ),
  STATUS_449( 449, "The request should be retried after doing the appropriate action" ),
  STATUS_451( 451, "Unavailable For Legal Reasons" ),
  STATUS_499( 499, "Client Closed Request" ),

  STATUS_500( 500, "Internal Server Error" ),
  STATUS_501( 501, "Not Implemented" ),
  STATUS_502( 502, "Bad Gateway" ),
  STATUS_503( 503, "Service Unavailable" ),
  STATUS_504( 504, "Gateway Timeout" ),
  STATUS_505( 505, "HTTP Version not supported" ),
  STATUS_506( 506, "Variant Also Negotiates" ),
  STATUS_507( 507, "Insufficient Storage" ),
  STATUS_508( 508, "Loop Detected" ),
  STATUS_509( 509, "Bandwidth Limit Exceeded" ),
  STATUS_510( 510, "Not Extended" ),
  STATUS_511( 511, "Network Authentication Required" );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Map< Integer, HttpStatus > httpStatusByStatus_ = new HashMap<>();

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private final int    status_;
  private final String message_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param status The status.
   * @param message The associated message. Must not be null.
   */
  HttpStatus( final int status, final String message )
  {
    if( message == null ) {
      throw new IllegalArgumentException( "The message parameter must not be null." );
    }

    status_  = status;
    message_ = message;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return The HttpStatus status code.
   */
  public int getStatus()
  {
    return status_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return The HttpStatus message. Never null.
   */
  public String getMessage()
  {
    return message_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param status The status to lookup.
   * @return The HttpStatus object with the given status code. May be null.
   */
  public static HttpStatus valueOf( final int status )
  {
    if( httpStatusByStatus_.isEmpty() ) {
      for( final HttpStatus httpStatus : HttpStatus.values() ) {
        httpStatusByStatus_.put( httpStatus.getStatus(), httpStatus );
      }
    }

    return httpStatusByStatus_.get( status );
  }
}
