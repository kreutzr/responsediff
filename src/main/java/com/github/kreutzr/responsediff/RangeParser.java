package com.github.kreutzr.responsediff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to handle value ranges
 */
public class RangeParser
{
  private static final Logger LOG = LoggerFactory.getLogger( RangeParser.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Tries to parses a range description which may be a range or not.
   * <p/>
   * Range expressions are coded as follows:
   * <pre> ["["|"]"](.*),(.*)["["|[]"] </pre>
   * e.g. "[x,y]", "[x,y[", "]x,y]", "]x,y["
   * <ul>
   * <li>"[x" means lower border including x</li>
   * <li>"]x" means lower border excluding x</li>
   * <li>"y[" means upper border excluding y</li>
   * <li>"y]" means upper border including y</li>
   * @param rangeDefinition The range description to parse.
   * @return A Range object if the passed value as a lower and an upper border. Otherwise null is returned.
   */
  public static Range parse( final String rangeDefinition )
  {
    if( rangeDefinition == null ) {
      return null;
    }

    final String value = rangeDefinition.trim();
    final String[] minmax = value.split( "," );
    if( minmax.length != 2 ) {
      return null;
    }

    final char   lb  = minmax[ 0 ].charAt( 0 );
    final String min = minmax[ 0 ].substring( 1 ).trim();
    final char   ub  = minmax[ 1 ].charAt( minmax[ 1 ].length() - 1 );
    final String max = minmax[ 1 ].substring( 0, minmax[ 1 ].length() - 1 ).trim();
    if( ( ( lb != '[' ) && ( lb != ']' ) )
     || ( ( ub != '[' ) && ( ub != ']' ) )
    ) {
      return null;
    }

    final RangeBorder lowerBorder = new RangeBorder(
      lb == '[' ? RangeType.INCLUSIVE : RangeType.EXCLUSIVE,
      min
    );
    final RangeBorder upperBorder = new RangeBorder(
      ub == '[' ? RangeType.EXCLUSIVE : RangeType.INCLUSIVE,
      max
    );

    final Range range = new Range( lowerBorder, upperBorder );

    if( LOG.isTraceEnabled() ) {
      LOG.trace( "parse( " + rangeDefinition + ") = " + range.toString() );
    }

    return range;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
  public static final void main( final String[] args )
  {
    String test = " [ x    ,    y     ] ";
    LOG.info( test  + " : " + RangeParser.parse( test ) );
    test = "[x,y[";
    LOG.info( test  + " : " + RangeParser.parse( test ) );
    test = "]x,y]";
    LOG.info( test  + " : " + RangeParser.parse( test ) );
    test = "]x,y[";
    LOG.info( test  + " : " + RangeParser.parse( test ) );
  }
*/
}
