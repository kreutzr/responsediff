package com.github.kreutzr.responsediff;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlToJson
{
  private static final String PATTERN_BASETYPE = "^[+-]?\\d*\\.?\\d*(E-\\d{0,2})?$|^true$|^false$";

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger( XmlToJson.class );

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private boolean preserveOrder_  = true;
  private boolean skipAttributes_ = false;

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public XmlToJson()
  {
    // Nothing to do here...
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setPreserveOrder( final boolean preserveOrder )
  {
    preserveOrder_ = preserveOrder;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setSkipAttributes( final boolean skipAttributes )
  {
    skipAttributes_ = skipAttributes;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String toJson( final String xml ) throws Exception
  {
    final DocumentBuilder db = XmlFileHandler.DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
    final Document doc = db.parse( new InputSource( new StringReader( xml ) ) );

    return toJson( doc.getDocumentElement() ).toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public StringBuilder toJson( final Document doc )
  {
    return toJson( doc.getDocumentElement() );
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private StringBuilder toJson( final Element elem )
  {
    final StringBuilder sb = new StringBuilder( "{" )
    // Handle element name
    .append( "\"" ).append( elem.getTagName() ).append("\":" );


    boolean isFirstEntry = true;

    if( !skipAttributes_ ) {
      sb.append( "{" );

      // Handle element attributes
      final NamedNodeMap attributes = elem.getAttributes();
      for( int i=0; i < attributes.getLength(); i++ ) {
        final Node node = attributes.item( i );
        final String attributeName  = node.getNodeName();
        final String attributeValue = node.getNodeValue();
        if( !isFirstEntry ) {
          sb.append(",");
        }
        sb.append( "\"@" ).append( attributeName ).append( "\":\"" ).append( attributeValue ).append( "\"" );
        isFirstEntry = false;
      }
    }

    // Handle element value
    final StringBuilder value = toJson( elem.getChildNodes() );
    if( value != null ) {
      if( !isFirstEntry ) {
        sb.append(",");
      }
      if( !skipAttributes_ ) {
        sb.append( "\"#value\":" );
      }
      sb.append( value );
    }

    if( !skipAttributes_ ) {
      sb.append( "}" );
    }

    sb.append( "}" );

    return sb;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private StringBuilder toJson( final NodeList nodeList )
  {
    if( nodeList == null || nodeList.getLength() == 0 ) {
      return null;
    }

    // Gather content nodes
    final List< StringBuilder > list = new ArrayList<>();
    final Set< String > keys = new HashSet<>();
    boolean skipOrderAllowed = !preserveOrder_;

    for( int i=0; i < nodeList.getLength(); i++ ) {
      final Node child = nodeList.item( i );

      String key = null;

      if( child instanceof Element ) {
        key = child.getNodeName();
        list.add( toJson( (Element) child ) );
      }
      else {
        key = "#text";
        final StringBuilder sb = new StringBuilder();
        list.add( sb.append( "{\"#text\":" ).append( mask( child.getNodeValue() ) ).append( "}" ) );
      }

      if( !keys.contains( key ) ) {
        keys.add(key);
      }
      else {
        skipOrderAllowed = false;
      }
    }

    // Handle simple text content value
    if( keys.size() == 1 && keys.contains( "#text" ) ) {
      final StringBuilder value = list.get( 0 );
      return new StringBuilder( value.substring( 9, value.length() - 1 ) ); // Remove leading "{\"#text\":" and trailing "}"
    }

    // Handle structured or multi content value
    String start = null;
    String end   = null;
    if( !skipOrderAllowed ) {
      start = "[";
      end   = "]";
    }
    else {
      start = "{";
      end   = "}";
    }

    final StringBuilder sb = new StringBuilder( start );

    boolean isFirstEntry = true;
    for( final StringBuilder part : list ) {
      if( !isFirstEntry ) {
        sb.append( "," );
      }
      if( skipOrderAllowed ) {
        String simplifiedPart = part.toString();
        simplifiedPart = simplifiedPart.substring( 1, simplifiedPart.length()-1 ); // Remove curly brackets
        sb.append( simplifiedPart );
      }
      else {
        sb.append( part );
      }

      isFirstEntry = false;
    }

    return sb.append( end );
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private StringBuilder mask( final String value )
  {
    final StringBuilder sb = new StringBuilder();

    if( isBaseType( value ) ) {
      sb.append( value );
    }
    else {
      sb.append( "\"" ).append( value ).append( "\"" );
    }

    return sb;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private boolean isBaseType( final String value )
  {
    final String trimmedValue = value.trim();

    return trimmedValue.matches( PATTERN_BASETYPE );
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
  public static void main( final String[] args ) throws Exception
  {
    final String xml = "<a type=\"A\">1.4<b type=\"B\">4.3E-02</b>true<b type=\"B\"><c type=\"C\">ddd</c><d>false</d></b>fff<b>567</b><b>hhh</b></a>";

    final DocumentBuilder db = XmlFileHandler.DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
    final Document doc = db.parse( new InputSource(new StringReader( xml ) ) );

    final XmlToJson xmlToJson = new XmlToJson();
    LOG.info( xmlToJson.toJson( doc ).toString() );
  }
*/
}