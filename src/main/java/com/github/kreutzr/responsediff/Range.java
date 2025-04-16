package com.github.kreutzr.responsediff;

/**
 * Helper class to handle value ranges
 */
public class Range
{
  final RangeBorder lowerBorder_;
  final RangeBorder upperBorder_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public Range(
    final RangeBorder lowerBorder,
    final RangeBorder upperBorder
  )
  {
    lowerBorder_ = lowerBorder;
    upperBorder_ = upperBorder;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public RangeBorder getLowerBorder()
  {
    return lowerBorder_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public RangeBorder getUpperBorder()
  {
    return upperBorder_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    
    sb.append( ( lowerBorder_.getType() == RangeType.INCLUSIVE ) ? "[ " : "] " )
      .append( lowerBorder_.getValue() )
      .append( ", " )
      .append( upperBorder_.getValue() )
      .append( ( upperBorder_.getType() == RangeType.INCLUSIVE ) ? " ]" : " [" );
    
    return sb.toString();
  }
}
