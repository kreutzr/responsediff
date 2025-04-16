package com.github.kreutzr.responsediff;

public class ToXmlValue
{
  private static final String HEADER_PATH_PREFIX = "$." + ToJson.HEADERS_SUBPATH + ".";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static XmlValue fromHeader( final XmlHeader xmlHeader )
  {
    final XmlValue xmlValue = new XmlValue();

    xmlValue.setValue          ( xmlHeader.getValue() );
    xmlValue.setPath( HEADER_PATH_PREFIX + xmlHeader.getName().toLowerCase() );
    xmlValue.setType           ( xmlHeader.getType() );
    xmlValue.setEpsilon        ( xmlHeader.getEpsilon() );
    xmlValue.setTrim           ( xmlHeader.isTrim() );
    xmlValue.setIgnoreCase     ( xmlHeader.isIgnoreCase() );
    xmlValue.setMatch          ( xmlHeader.isMatch() );
    xmlValue.setCheckPathExists( xmlHeader.isCheckPathExists() );
    xmlValue.setCheckIsNull    ( xmlHeader.isCheckIsNull() );
    xmlValue.setCheckInverse   ( xmlHeader.isCheckInverse() );
//    xmlValue.setTicketReference( xmlHeader.getTicketReference() );
    return xmlValue;
  }
}
