package com.github.kreutzr.responsediff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * Interface that is invoked by the JsonTraverser class
 */
public interface JsonTraverserVisitor
{
  /**
   * Notification for a JSON node.
   * @param node The JSON node to visit.
   * @param parentType The node's parent node type. May be null.
   * @param jsonPath The node's path within the JSON structure.
   */
  public void notify( final JsonNode node, final JsonNodeType parentType, final String jsonPath );
}
