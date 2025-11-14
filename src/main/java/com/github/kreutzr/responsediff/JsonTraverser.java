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
 * Visitors are notified for each JSON element.
 */
public class JsonTraverser
{
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger( JsonTraverser.class );

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final JsonNode root_;
  final List< JsonTraverserVisitor > structureVisitors_ = new ArrayList<>();
  final List< JsonTraverserVisitor > valueVisitors_     = new ArrayList<>();

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param json The JSON to traverse. Must not be null.
   * @throws JsonMappingException, JsonProcessingException If an error occurs an exception is thrown.
   */
  public JsonTraverser( final String json )
  throws JsonMappingException, JsonProcessingException
  {
    root_ = JsonHelper.provideObjectMapper().readTree( json );
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return The root node of the JSON passed over in the constructor.
   */
  public JsonNode getRoot()
  {
    return root_;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Adds a structure visitor to the JsonTraverser. A structure visitor is invoked before the sub-structure (map) is traversed.
   * @param visitor The visitor to add. Must not be null.
   * @return this.
   */
  public JsonTraverser addStructureVisitor( final JsonTraverserVisitor visitor )
  {
    structureVisitors_.add( visitor );
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Adds a value visitor to the JsonTraverser. A value visitor is invoked for leaf values only.
   * @param visitor The visitor to add. Must not be null.
   * @return this.
   */
  public JsonTraverser addValueVisitor( final JsonTraverserVisitor visitor )
  {
    valueVisitors_.add( visitor );
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Performs the traversal over the JSON structure passed over in the constructor.
   * @return this instance.
   */
  public JsonTraverser traverse()
  {
    if( structureVisitors_.isEmpty() && valueVisitors_.isEmpty() ) {
      return this;
    }

    iterate( root_, root_.getNodeType(), "$" );

    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void callStructureVisitors( final JsonNode node, final JsonNodeType parentNodeType, final String jsonPath )
  {
    for( final JsonTraverserVisitor visitor : structureVisitors_ ) {
      visitor.notify( node, parentNodeType, jsonPath );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void callValueVisitors( final JsonNode node, final JsonNodeType parentNodeType, final String jsonPath )
  {
    for( final JsonTraverserVisitor visitor : valueVisitors_ ) {
      visitor.notify( node, parentNodeType, jsonPath );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterate( final JsonNode node, final JsonNodeType parentNodeType, final String path )
  {
    final JsonNodeType type = node.getNodeType();

    switch( type ) {
    case ARRAY   : callStructureVisitors( node, parentNodeType, path ); iterateArray( node, path );
      break;
    case OBJECT  : callStructureVisitors( node, parentNodeType, path ); iterateMap( node, path );
      break;
    case BOOLEAN : callValueVisitors( node, parentNodeType, path );
      break;
    case NUMBER  : callValueVisitors( node, parentNodeType, path );
      break;
    case STRING  : callValueVisitors( node, parentNodeType, path );
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
      iterate( node.get( i ), JsonNodeType.ARRAY, JsonDiff.getArrayPath( path, i ) );
      i++;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void iterateMap( final JsonNode node, final String path )
  {
    final Set< String > keys = getKeySet( node );

    for( final String key : keys ) {
      iterate( node.get( key ), JsonNodeType.OBJECT, JsonDiff.getMapPath( path, key ) );
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static Set< String > getKeySet( final JsonNode jsonNode )
  {
    final Set< String > keys = new TreeSet<>();

    final Iterator< Map.Entry< String, JsonNode > > fields = jsonNode.properties().iterator();
    while(fields.hasNext()) {
      keys.add( fields.next().getKey() );
    }

    return keys;
  }
}
