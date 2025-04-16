package com.github.kreutzr.responsediff;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface that is invoked by the JsonTraverser class
 */
public interface _JsonTraverserListener
{
  public void notify( final JsonNode node, final String jsonPath );
}
