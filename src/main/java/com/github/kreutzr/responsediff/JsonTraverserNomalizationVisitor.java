package com.github.kreutzr.responsediff;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * Visitor that manipulates the underlying JSON to normalize it.
 * <p>
 * <b>Example:</b>
 * <ul>
 * <li> normalizeMap: "project.id" : "111" =&gt; "project" : { "id" : "111" }</li>
 * <li> normalizeArray: [ "project.id" : "111" ] =&gt; [ "project" : { "id" : "111" } ]</li>
 * </ul>
 */
public class JsonTraverserNomalizationVisitor implements JsonTraverserVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger( JsonTraverserNomalizationVisitor.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private boolean normalizeMap_;
  private boolean normalizeArray_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   * @param normalizeMap   Flag, if JSON map entries shall be normalized (true) or not (false).
   * @param normalizeArray Flag, if JSON array entries shall be normalized (true) or not (false).
   */
  public JsonTraverserNomalizationVisitor(
    final boolean normalizeMap,
    final boolean normalizeArray
  )
  {
    normalizeMap_   = normalizeMap;
    normalizeArray_ = normalizeArray;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void notify( final JsonNode node, final JsonNodeType parentType, final String path )
  {
    final JsonNodeType type = node.getNodeType();

    switch( type ) {
    case ARRAY   : // Nothing to do here
      break;
    case OBJECT  : normalizeMap( (ObjectNode) node, parentType, path );
      break;
    default :
      // NOTE: As a structure visitor we only expect to be called for ARRAY and OBJECT only.
      throw new RuntimeException( "Node type " + type.name() + " is not supported yet." );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void normalizeMap( final ObjectNode node, final JsonNodeType parentType, final String path )
  {
    if( !normalizeMap_ || ( parentType == JsonNodeType.ARRAY && !normalizeArray_ ) ) {
      return;
    }

    final Iterator< String > it = JsonTraverser.getKeySet( node ).iterator();
    while( it.hasNext() ) {
      final String key = it.next();

      final int pos = key.indexOf( "." );
      if( pos < 0 ) {
        continue;
      }

      final String   structureKey = key.substring( 0, pos );
      final String   keyTail      = key.substring( pos + 1 );
      final JsonNode value        = node.get( key );

      final JsonNode existingNode = node.get( structureKey );
      if( existingNode == null ) {
        // Prepare substructure
        final Map< String, JsonNode > map = new TreeMap<>();
        map.put( keyTail,  value );
        final ObjectNode structureNode = new ObjectNode( JsonHelper.provideObjectMapper().getNodeFactory(), map );

        // Insert substructure
        node.set( structureKey, structureNode );

        LOG.debug( "Created new substructure node \"" + path + "." + structureKey );

        // Remove existing key
        node.remove( key );
      }
      else {
        // Check for conflict
        final JsonNodeType type = existingNode.getNodeType();
        if( type != JsonNodeType.OBJECT ) {
          LOG.warn( "Key \"" + key+ "\" at path \"" + path + "\" can not be normalized because substructure \"" + structureKey + "\" exists but is not an object by of type \"" + type.name() + "\". Normalization of this node is skipped." );
          continue;
        }
        if( existingNode.get( keyTail ) != null ) {
          LOG.warn( "Key \"" + key+ "\" at path \"" + path + "\" can not be normalized because substructure \"" + structureKey + "\" exists and already holds an attribute  \"" + keyTail + "\". Normalization of this node is skipped." );
          continue;
        }

        // Extend substructure
        final ObjectNode existingStructure = (ObjectNode) existingNode;
        existingStructure.set( keyTail, value );

        // Remove existing key
        node.remove( key );
      }

      LOG.debug( "Extended substructure node \"" + path + "." + structureKey + " by \"" + keyTail + "\".");
    }
  }
}
