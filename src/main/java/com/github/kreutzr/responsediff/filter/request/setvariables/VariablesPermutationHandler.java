package com.github.kreutzr.responsediff.filter.request.setvariables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariablesPermutationHandler
{
  private static final Logger LOG = LoggerFactory.getLogger( VariablesPermutationHandler.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Map< String, List< Object > > variables_ = null;
  private Map< String, Integer > variablesIndexByName_ = null;
  private List< String > variableNames_ = null;
  private int variablesNameIndex_ = 0;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void init( final Map< String, List< Object > > variables )
  {
    variables_ = variables;

    // Initialize index map
    variablesIndexByName_ = new TreeMap<>();
    Iterator< String > it = variables_.keySet().iterator();
    while( it.hasNext() ) {
      final String variableName = it.next();
      variablesIndexByName_.put( variableName, 0 );
    }

    // Initialize ordered(!) variable name list
    variableNames_ = new ArrayList<>();
    it = variablesIndexByName_.keySet().iterator();
    while( it.hasNext() ) {
      final String variableName = it.next();
      variableNames_.add( variableName );
    }

    variablesNameIndex_ = 0;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void next()
  {
    LOG.trace( "next()" );

    final String currentVariableName = variableNames_.get( variablesNameIndex_ );
    int nextVariableValueIndex = variablesIndexByName_.get( currentVariableName ) + 1;

    if( nextVariableValueIndex < variables_.get( currentVariableName ).size() ) {
      // Increase variable value index
      if( LOG.isTraceEnabled() ) {
        LOG.trace( "Updating value index of variable \"" + currentVariableName + "\" to " + nextVariableValueIndex );
      }
      variablesIndexByName_.put( currentVariableName, nextVariableValueIndex );
    }
    else {
      updateVariablesIndexMap( variablesNameIndex_ );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public Map< String, Integer > getIndexes()
  {
    return variablesIndexByName_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void updateVariablesIndexMap( final int oldIndex )
  {
    LOG.trace( "updateVariablesIndexMap()" );

    // Reset variables "below" current name index
    int resetIndex = oldIndex;
    int newIndex = findNext( oldIndex );

    if( newIndex < 0 ){
      // Complete reset
      resetIndex = variableNames_.size() - 1;
      variablesNameIndex_ = 0;
    }

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "Resetting variable value indexes below " + resetIndex );
    }
    for( int i=0; i <= resetIndex; i++ ) {
      variablesIndexByName_.put( variableNames_.get( i ), 0 );
    }

    if( newIndex < 0 ) {
      return;
    }

    final String currentVariableName = variableNames_.get( newIndex );
    int nextVariableValueIndex = variablesIndexByName_.get( currentVariableName ) + 1;

    if( nextVariableValueIndex < variables_.get( currentVariableName ).size() ) {
      // Increase variable value index
      if( LOG.isTraceEnabled() ) {
        LOG.trace( "Updating value index of variable \"" + currentVariableName + "\" to " + nextVariableValueIndex );
      }
      variablesIndexByName_.put( currentVariableName, nextVariableValueIndex );
    }
    else {
      updateVariablesIndexMap( newIndex );
    }

    LOG.trace( toString() );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private int findNext( final int oldIndex )
  {
    final int newIndex = oldIndex + 1;

    if( newIndex >= variableNames_.size() ) {
      // End of variables reached
      return -1;
    }

    final String name = variableNames_.get( newIndex );
    if( variables_.get( name ) == null || variables_.get( name ).isEmpty() ) {
      // Skip trivial variable value permutations
      return findNext( newIndex );
    }

    return newIndex;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String toString()
  {
    final StringBuffer sb = new StringBuffer( "{" ); // Open JSON structure

    sb.append( "\"variableNames\":" ).append( variableNames_.toString() )
      .append( "\"variablesNameIndex\":" ).append( variablesNameIndex_ );

    // Append index map
    sb.append( "\"variablesIndexByName\":{" );
    for( int i=0; i < variableNames_.size(); i++ ) {
      final String variableName = variableNames_.get( i );
      final int index = variablesIndexByName_.get( variableName );
      sb.append( "\"" ).append( variableName ).append( "\":" ).append( index );
      if( i < variableNames_.size() - 1 ) {
        sb.append( ", " );
      }
    }
    sb.append( "}");

    // Finalize JSON structure
    sb.append( "}" );

    return sb.toString();
  }
}
