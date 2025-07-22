package com.github.kreutzr.responsediff;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.kreutzr.responsediff.tools.Converter;
import com.jayway.jsonpath.DocumentContext;

/**
 * Visitor that checks whenever a notified JSON path matches a XmlValue JSON path, the notified value matches the expected XmlValue value.
 * Any mismatch is reported to a given JsonDiff object.
 */
public class _JsonTraverserValidationVistor implements JsonTraverserVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger( _JsonTraverserValidationVistor.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final DocumentContext context_;
  final JsonPathHelper jsonPathHelper_;
  final List< XmlValue > xmlValues_;
  final JsonDiff relevantDiff_;
  final JsonDiff whiteNoiseDiff_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param context The DocumentContext to use. Must not be null.
   * @param xmlValues The XmlValues to lookup. Must not be null.
   * @param relevantDiff The JsonDiff to report into. Must not be null.
   * @param whiteNoiseDiff A whiteNoise JsonDiff to use. May be null.
   */
  public _JsonTraverserValidationVistor(
    final DocumentContext context,
    final List< XmlValue > xmlValues,
    final JsonDiff relevantDiff,
    final JsonDiff whiteNoiseDiff
  )
  {
    context_        = context;
    jsonPathHelper_ = new JsonPathHelper( context_ );
    xmlValues_      = xmlValues;
    relevantDiff_   = relevantDiff;
    whiteNoiseDiff_ = whiteNoiseDiff;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public JsonDiff getJsonDiff()
  {
    return relevantDiff_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void notify( final JsonNode node, final JsonNodeType parentNodeType, final String jsonPath )
  {
    for( final XmlValue xmlValue : xmlValues_ ) {
      // NEEDS FIX A: This is where paths with subqueries (e.g. "$[*].attributes[?(@.attributeName=='SERIES_ID')].attributeValue")
      //              fail because the approach with contains is not sufficient!
      if( JsonPathHelper.contains( xmlValue.getPath(), jsonPath ) ) {
        checkValue( node, xmlValue, jsonPath );
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void checkValue( final JsonNode node, final XmlValue xmlValue, final String jsonPath )
  {
    if( LOG.isTraceEnabled() ) {
      LOG.trace( "checkValue( " + jsonPath + " )" );
    }

    final JsonNodeType type = node.getNodeType();

    switch( type ) {
      case BOOLEAN : compareBoolean( node, xmlValue, jsonPath );
        break;
      case NUMBER  : compareNumber( node, xmlValue, jsonPath );
        break;
      case STRING  : compareString( node, xmlValue, jsonPath );
        break;
      case NULL    : // fall through
      case ARRAY   : // fall through
      case OBJECT  : // fall through
      case BINARY  : // fall through
      case POJO    : // fall through
      case MISSING : // fall through
      default :
        throw new RuntimeException( "Node type " + type.name() + " is not supported yet." );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void compareBoolean( final JsonNode node, final XmlValue xmlValue, final String path )
  {
    final XmlValueType expectedType = xmlValue.getType();
    if( expectedType != XmlValueType.BOOLEAN ) {
      final StringBuilder sb = new StringBuilder()
        .append( "Type expected: " ).append( expectedType.name() )
        .append( " but was: " ).append( node.getNodeType().name() );

        relevantDiff_.getChanges().add( new JsonDiffEntry( path, "", "", null, sb.toString() ) );
      return;
    }

    final XmlValue innerXmlValue = cloneXmlValue( xmlValue, path );
    final List< JsonDiffEntry > whiteNoiseEntries = ValidationHandler.checkExpected( jsonPathHelper_, innerXmlValue, null, null, relevantDiff_, 0 );
    if( whiteNoiseDiff_ != null && !whiteNoiseEntries.isEmpty() ) {
      whiteNoiseDiff_.getChanges().addAll( whiteNoiseEntries );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void compareNumber( final JsonNode node, final XmlValue xmlValue, final String path )
  {
    final XmlValueType expectedType = xmlValue.getType();
    if( expectedType != XmlValueType.DOUBLE
     && expectedType != XmlValueType.INT
     && expectedType != XmlValueType.LONG

    ) {
      final StringBuilder sb = new StringBuilder()
        .append( "Type expected: " ).append( expectedType.name() )
        .append( " but was: " ).append( node.getNodeType().name() );

        relevantDiff_.getChanges().add( new JsonDiffEntry( path, "", "", null, sb.toString() ) );
      return;
    }

    final XmlValue innerXmlValue = cloneXmlValue( xmlValue, path );
    final double epsilon = Converter.asDouble( xmlValue.getEpsilon(), Constants.EPSILON );
    final List< JsonDiffEntry > whiteNoiseEntries = ValidationHandler.checkExpected( jsonPathHelper_, innerXmlValue, null, null, relevantDiff_, epsilon );
    if( whiteNoiseDiff_ != null && !whiteNoiseEntries.isEmpty() ) {
      whiteNoiseDiff_.getChanges().addAll( whiteNoiseEntries );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void compareString( final JsonNode node, final XmlValue xmlValue, final String path )
  {
    final XmlValueType expectedType = xmlValue.getType();
    if( expectedType != XmlValueType.DATE
     && expectedType != XmlValueType.DATETIME
     && expectedType != XmlValueType.DURATION
     && expectedType != XmlValueType.STRING
     ) {
       final StringBuilder sb = new StringBuilder()
         .append( "Type expected: " ).append( expectedType )
         .append( " but was: " ).append( node.getNodeType().name() );

         relevantDiff_.getChanges().add( new JsonDiffEntry( path, "", "", null, sb.toString() ) );
       return;
     }

    final XmlValue innerXmlValue = cloneXmlValue( xmlValue, path );
    final List< JsonDiffEntry > whiteNoiseEntries = ValidationHandler.checkExpected( jsonPathHelper_, innerXmlValue, null, null, relevantDiff_, 0 );
    if( whiteNoiseDiff_ != null && !whiteNoiseEntries.isEmpty() ) {
      whiteNoiseDiff_.getChanges().addAll( whiteNoiseEntries );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private XmlValue cloneXmlValue( final XmlValue xmlValue, final String path )
  {
    final XmlValue result = new XmlValue();
    result.setValue          ( xmlValue.getValue() );
    result.setPath           ( path );
    result.setType           ( xmlValue.getType() );
    result.setEpsilon        ( xmlValue.getEpsilon());
    result.setTrim           ( xmlValue.isTrim());
    result.setIgnoreCase     ( xmlValue.isIgnoreCase());
    result.setMatch          ( xmlValue.isMatch());
    result.setCheckPathExists( xmlValue.isCheckPathExists() );
    result.setCheckIsNull    ( xmlValue.isCheckIsNull());
    result.setCheckInverse   ( xmlValue.isCheckInverse() );
    result.setTicketReference( xmlValue.getTicketReference() );

    return result;
  }
}
