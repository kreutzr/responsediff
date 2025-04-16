package com.github.kreutzr.responsediff;

/**
 * Wrapper around a standard Throwable to carry the following details:
 * - instanceId (Either "candidate", "reference" or "control" (see TestSetHandler constants))
 */
public class HttpHandlerException extends Throwable
{
  private static final long serialVersionUID = -235534544976858779L;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final String instanceId_;

  public HttpHandlerException(
    final String message,
    final String instanceId,
    final Throwable ex
  )
  {
    super( message, ex );
    instanceId_ = instanceId;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return The instance id. May be null.
   */
  public String getInstanceId()
  {
    return instanceId_;
  }
}
