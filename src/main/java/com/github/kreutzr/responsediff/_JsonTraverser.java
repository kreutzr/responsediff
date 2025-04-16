package com.github.kreutzr.responsediff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * Allows traversal over all JSON elements.
 * Listeners are notified for each JSON element.
 */
public class _JsonTraverser
{
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger( _JsonTraverser.class );

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final JsonNode root_;
  final List< _JsonTraverserListener > listeners_ = new ArrayList<>();

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param json The JSON to traverse. Must not be null.
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  public _JsonTraverser( final String json )
  throws JsonMappingException, JsonProcessingException
  {
    root_ = JsonHelper.provideObjectMapper().readTree( json );
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Adds a listener to the JsonTraverser
   * @param listener The listener to add. Must not be null.
   * @return this.
   */
  public _JsonTraverser addListener( final _JsonTraverserListener listener )
  {
    listeners_.add( listener );
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void traverse()
  {
    if( listeners_.isEmpty() ) {
      return;
    }

    iterate( root_, "$" );
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void callListeners( final JsonNode node, final String jsonPath )
  {
    for( final _JsonTraverserListener listener : listeners_ ) {
      listener.notify( node, jsonPath );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterate( final JsonNode node, final String path )
  {
    final JsonNodeType type = node.getNodeType();

    switch( type ) {
    case ARRAY   : iterateArray( node, path );
      break;
    case OBJECT  : iterateMap( node, path );
      break;
    case BOOLEAN : callListeners( node, path );
      break;
    case NUMBER  : callListeners( node, path );
      break;
    case STRING  : callListeners( node, path );
      break;
    case NULL    : // Do nothing because we checked canType != refType already, so here both types are NULL.
      break;
    case BINARY  : // fall through
    case POJO    : // fall through
    case MISSING : // fall through
    default :
      throw new RuntimeException( "Node type " + type.name() + " is not supported yet." );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterateArray( final JsonNode node, final String path )
  {
    final int length = node.size();

    int i=0;
    while( i < length) {
      iterate( node.get( i ), JsonDiff.getArrayPath( path, i ) );
      i++;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterateMap( final JsonNode node, final String path )
  {
    final Set< String > keys = getKeySet( node );

    for( final String key : keys ) {
      iterate( node.get( key ), JsonDiff.getMapPath( path, key ) );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Set< String > getKeySet( final JsonNode jsonNode )
  {
    final Set< String > keys = new TreeSet<>();

    final Iterator< Map.Entry< String, JsonNode > > fields = jsonNode.fields();
    while(fields.hasNext()) {
        keys.add( fields.next().getKey() );
    }

    return keys;
  }
}
