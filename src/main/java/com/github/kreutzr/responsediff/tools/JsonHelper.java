package com.github.kreutzr.responsediff.tools;

import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides an ObjectMapper for JSON with nodes sorted in alphabetical order.
 * <br/>
 * <b>NOTE:</b> ObjectMapper is thread safe
 */
public class JsonHelper
{
  private static class SortingNodeFactory extends JsonNodeFactory
  {
    private static final long serialVersionUID = 384983072767248696L;
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObjectNode objectNode() {
      return new ObjectNode( this, new TreeMap< String, JsonNode >() );
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final ObjectMapper MAPPER = createObjectMapper();
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * @return An (re-used) ObjectMapper for JSON with nodes sorted in alphabetical order. Never null. (Note: ObjectMapper is thread safe)
   */
  public static ObjectMapper provideObjectMapper()
  {
    return MAPPER;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return An new created ObjectMapper for JSON with nodes sorted in alphabetical order. Never null.
   * <p/>
   * <b>NOTE:</b> An ObjectMapper is an expensive object. You most probably want to use provideObjectMapper().
   */
  public static ObjectMapper createObjectMapper()
  {
    return new ObjectMapper()
      .setNodeFactory( new SortingNodeFactory() )    // Nodes in alphabetical order (required for comparison in Validator)
//      .setSerializationInclusion( Include.NON_NULL ) // Ignore null entries
      ;
  }
}
