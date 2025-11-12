package com.github.kreutzr.responsediff;

import java.util.Set;

/**
 * Class to provide XML to JSON transformation.
 */
public class ToJson
{
  public static final String  HEADERS_SUBPATH   = "headers";
  public static final String  HEADER_HTTPSTATUS = ":status";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlHeaders.
   * @param xmlHeaders The headers object to transform. May be null.
   * @param withOuterBrackets Flag, if the outer brackets shall be added (true) or not (false).
   * @param lowerCaseHeaderNamesWithPaths An optional Set of lower case header names which have a path definition. => Therefore the value must be treated as JSON. May be null.
   * @return The formatted XmlHeaders.
   */
  static String fromHeaders(
    final XmlHeaders xmlHeaders,
    final boolean withOuterBrackets,
    final Set< String > lowerCaseHeaderNamesWithPaths
  )
  {
    final StringBuilder sb = new StringBuilder();

    if( withOuterBrackets ) {
      sb.append( "{" );
    }

    sb.append( "\"" ).append( HEADERS_SUBPATH ).append( "\":" );

    if( xmlHeaders == null || xmlHeaders.getHeader() == null ) {
      sb.append( "null" );
    }
    else {
      sb.append( "{" );
      for( int i=0; i< xmlHeaders.getHeader().size(); i++ ) {
        if( i > 0 ) {
          sb.append( "," );
        }
        final XmlHeader xmlHeader = xmlHeaders.getHeader().get( i );
        final String lowerCaseHeaderName = xmlHeader.getName().toLowerCase(); // HTTP spec says that header names are case-insensitive ( see "https://datatracker.ietf.org/doc/html/rfc2616#section-4.2")

        final boolean hasPath = lowerCaseHeaderNamesWithPaths != null && lowerCaseHeaderNamesWithPaths.contains( lowerCaseHeaderName );
        sb.append( "\"" ).append( lowerCaseHeaderName ).append( "\":" );
        if( hasPath ) {
          sb.append( xmlHeader.getValue() ); // Use plain value => It becomes part of the JSON structure instead of a String value.
        }
        else {
          sb.append( "\"" ).append( maskQuotes( xmlHeader.getValue() ) ).append( "\"" );
        }
      }
      sb.append( "}" );
    }

    if( withOuterBrackets ) {
      sb.append( "}" );
    }

    return sb.toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Masks all quotes within the given text.
   * @param text The text to mask quotes within. May be null.
   * @return TRhe passed text with masked quotes. If null was passed, null is returned.
   */
  private static String maskQuotes( final String text )
  {
    return text != null ? text.replaceAll( "\"", "\\\\\"" ) : text;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlVariable.
   * @param xmlVariable The variable to transform. May be null.
   * @return The formatted XmlVariable.
   */
  static String fromVariable( final XmlVariable xmlVariable )
  {
    if( xmlVariable == null ) {
      return "null";
    }

    final StringBuilder sb = new StringBuilder( "{" );

    sb.append( "\"id\":\""     ).append( xmlVariable.getId()    ).append( "\"" )
      .append( "\",path\":\""  ).append( xmlVariable.getPath()  ).append( "\"" )
      .append( "\",value\":\"" ).append( xmlVariable.getValue() ).append( "\"" )
      ;

    return sb.append( "}" ).toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlAnalysis.
   * @param xmlAnalysis The analysis object to transform. May be null.
   * @return The formatted XmlAnalysis.
   */
  static String fromAnalysis( final XmlAnalysis xmlAnalysis )
  {
    if( xmlAnalysis == null ) {
      return "null";
    }

    final StringBuilder sb = new StringBuilder( "{" );

    sb.append( "\"begin\":"           ).append( xmlAnalysis.getBegin() )
      .append( "\",end\":"            ).append( xmlAnalysis.getEnd() )
      .append( "\",duration\":"       ).append( xmlAnalysis.getDuration() )
      .append( "\",successCount\":"   ).append( xmlAnalysis.getSuccessCount() )
      .append( "\",failCount\":"      ).append( xmlAnalysis.getFailCount() )
      .append( "\",skipCount\":"      ).append( xmlAnalysis.getSkipCount() )
      .append( "\",totalCount\":"     ).append( xmlAnalysis.getTotalCount() );

    sb.append( "\",messages\":[" );
    if( xmlAnalysis.getMessages() != null && xmlAnalysis.getMessages().getMessage() != null ) {
      for( int i=0; i < xmlAnalysis.getMessages().getMessage().size(); i++ ) {
        final XmlMessage xmlMessage = xmlAnalysis.getMessages().getMessage().get( i );
        if( i > 0 ) {
          sb.append( "," );
        }
        sb.append( fromXmlMessage( xmlMessage ) )
        ;
      }
    }
    sb.append( "]" );

    return sb.append( "}" ).toString();
  }

 /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlMessage.
   * @param xmlMessage The message to transform. May be null.
   * @return The formatted XmlMessage.
   */
  static String fromXmlMessage( final XmlMessage xmlMessage )
  {
    if( xmlMessage == null ) {
      return "null";
    }

    final StringBuilder sb = new StringBuilder()
      .append("{\"level\":\"").append( ( xmlMessage.getLevel() != null )
        ? xmlMessage.getLevel().name()
        : XmlLogLevel.UNKNOWN.name() ).append( "\"" )
      .append( ",\"path\":\""  ).append(xmlMessage.getPath()  ).append( "\"" )
      .append( ",\"value\":\"" ).append(xmlMessage.getValue() ).append( "\"" );

    return sb.append( "}" ).toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlRequest.
   * @param xmlRequest The request to transform. May be null.
   * @return The formatted XmlRequest.
   */
  static String fromXmlRequest( final XmlRequest xmlRequest )
  {
    if( xmlRequest == null ) {
      return "null";
    }

    return formatHttpMessage( xmlRequest.getEndpoint(), xmlRequest.getHeaders(), /*null,*/ xmlRequest.getBody() );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlResponse.
   * @param xmlHttpResponse The response to transform. May be null.
   * @return The formatted XmlHttpResponse.
   */
  static String fromXmlResponse( final XmlHttpResponse xmlHttpResponse )
  {
    if( xmlHttpResponse == null ) {
      return "null";
    }

    return formatHttpMessage(
      null,
      xmlHttpResponse.getHeaders(),
      xmlHttpResponse.getBody()
    );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed XmlValue.
   * @param xmlvalue The XmlValue to transform. May be null.
   * @return The formatted XmValue.
   */
  static String fromXmlValue( final XmlValue xmlValue )
  {
    if( xmlValue == null ) {
      return "null";
    }

    final StringBuilder sb = new StringBuilder()
      .append("{\"epsilon\":\"").append( xmlValue.getEpsilon() ).append( "\"" )
      .append( ",\"path\":\""  ).append( xmlValue.getPath()    ).append( "\"" )
      .append( ",\"type\":\""  ).append( xmlValue.getType()    ).append( "\"" )
      .append( ",\"value\":\"" ).append( xmlValue.getValue()   ).append( "\"" )
//      .append( ",\"ticketReference\":\""            ).append( xmlValue.getTicketReference()            ).append( "\"" )
//      .append( ",\"ifExecutionContextContains\":\"" ).append( xmlValue.getIfExecutionContextContains() ).append( "\"" )
    ;

    return sb.append( "}" ).toString();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a JSON representation of the passed Xml objects.
   * @param endpoint   The HTTP endpoint. May be null.
   * @param xmlHeaders The headers. May be null.
   * @param body       The HTTP body. May be null.
   * @return The formatted JSON representation.
   */
  private static String formatHttpMessage(
      final String endpoint,
      final XmlHeaders xmlHeaders,
      final String body
  )
  {
    final StringBuilder sb = new StringBuilder( "{" );
    if( endpoint != null ) {
      sb.append( "\"endpoint\":" ).append( endpoint );
    }
    if( sb.length() > 1 ) {
      sb.append( "," );
    }
    sb.append( fromHeaders( xmlHeaders, false, null ) );

    if( body != null ) {
      sb.append( ",\"body\":\"" ).append( body ).append( "\"" );
    }
    else {
      sb.append( ",\"body\":null" );
    }

    return sb.append( "}" ).toString();
  }
}
