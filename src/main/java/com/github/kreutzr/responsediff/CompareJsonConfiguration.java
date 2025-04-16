package com.github.kreutzr.responsediff;

/**
 * Invocation configuration for ResponseDiff main class.
 */
public class CompareJsonConfiguration
{
  String  referenceFilePath_ = null;
  String  candidateFilePath_ = null;
  String  storeResultPath_   = null;
  boolean trim_              = true;
  boolean ignoreCase_        = false;
  double  epsilon_           = 0.00000001;
  String  ignorePaths_       = "";
  boolean sortArrays_        = false;
  String  sortArraysKeys_    = null;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getReferenceFilePath()
  {
    return referenceFilePath_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setReferenceFilePath( final String referenceFilePath )
  {
    referenceFilePath_ = referenceFilePath;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getCandidateFilePath()
  {
    return candidateFilePath_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setCandidateFilePath( final String candidateFilePath )
  {
    candidateFilePath_ = candidateFilePath;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getStoreResultPath()
  {
    return storeResultPath_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setStoreResultPath( final String storeResultPath )
  {
    storeResultPath_ = storeResultPath;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean isTrim()
  {
    return trim_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setTrim( final boolean trim )
  {
    trim_ = trim;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean isIgnoreCase()
  {
    return ignoreCase_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setIgnoreCase( final boolean ignoreCase )
  {
    ignoreCase_ = ignoreCase;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public double getEpsilon()
  {
    return epsilon_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setEpsilon( final double epsilon )
  {
    epsilon_ = epsilon;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getIgnorePaths()
  {
    return ignorePaths_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setIgnorePaths( final String ignorePaths )
  {
    ignorePaths_ = ignorePaths;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean isSortArrays()
  {
    return sortArrays_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setSortArrays( final boolean sortArrays )
  {
    sortArrays_ = sortArrays;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getSortArraysKeys()
  {
    return sortArraysKeys_;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void setSortArraysKeys( final String sortArraysKeys )
  {
    sortArraysKeys_ = sortArraysKeys;
  }
}
