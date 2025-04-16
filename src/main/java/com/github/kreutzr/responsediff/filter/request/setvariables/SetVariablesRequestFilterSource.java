package com.github.kreutzr.responsediff.filter.request.setvariables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SetVariablesRequestFilterSource 
{
  private Map< String, List< Object > > variables_ = new TreeMap<>();
  private List< Map< String, Object > > variableSets_ = new ArrayList<>(); 
  
  public void setVariables( final Map< String, List< Object > > variables ) {
    variables_ = variables;
  }
  
  public Map< String, List< Object > > getVariables()
  {
    return variables_;
  }
  
  public void setVariableSets( final List< Map< String, Object > > variableSets ) {
    variableSets_ = variableSets;
  }
  
  public List< Map< String, Object > > getVariableSets()
  {
    return variableSets_;
  }
}
