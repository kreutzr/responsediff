package com.github.kreutzr.responsediff;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.kreutzr.responsediff.filter.DiffFilter;

/**
 * Holds the outer configuration for a ResponseDiff run.
 */
public class OuterContext
{
    private String candidateServiceUrl_;
    private List< XmlHeader > candidateHeaders_;
    private String referenceServiceUrl_;
    private List< XmlHeader > referenceHeaders_;
    private String controlServiceUrl_;
    private List< XmlHeader > controlHeaders_;
    private Map< String, DiffFilter > filterRegistry_;
    private Pattern testIdPattern_;
    private long timeoutMs_;
    private double epsilon_;
    private String storeReportPath_;
    private boolean reportWhiteNoise_;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public OuterContext(
      final String            candidateServiceUrl,
      final List< XmlHeader > candidateHeaders,
      final String            referenceServiceUrl,
      final List< XmlHeader > referenceHeaders,
      final String            controlServiceUrl,
      final List< XmlHeader > controlHeaders,
      final Map< String, DiffFilter > filterRegistry,
      final Pattern           testIdPattern,
      final long              timeoutMs,
      final double            epsilon,
      final String            storeReportPath,
      final boolean           reportWhiteNoise
    )
    {
      candidateServiceUrl_ = candidateServiceUrl;
      candidateHeaders_    = candidateHeaders;
      referenceServiceUrl_ = referenceServiceUrl;
      referenceHeaders_    = referenceHeaders;
      controlServiceUrl_   = controlServiceUrl;
      controlHeaders_      = controlHeaders;
      filterRegistry_      = filterRegistry;
      testIdPattern_       = testIdPattern;
      timeoutMs_           = timeoutMs;
      epsilon_             = epsilon;
      storeReportPath_     = storeReportPath;
      reportWhiteNoise_    = reportWhiteNoise;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getCandidateServiceUrl()
    {
        return candidateServiceUrl_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setCandidateServiceUrl( final String serviceUrl )
    {
        candidateServiceUrl_ = serviceUrl;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List< XmlHeader > getCandidateHeaders()
    {
      return candidateHeaders_;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setCandidateHeaders( final List< XmlHeader > candidateHeaders )
    {
      candidateHeaders_ = candidateHeaders;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getReferenceServiceUrl()
    {
        return referenceServiceUrl_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setReferenceServiceUrl( final String serviceUrl )
    {
        referenceServiceUrl_ = serviceUrl;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List< XmlHeader > getReferenceHeaders()
    {
      return referenceHeaders_;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setReferenceHeaders( final List< XmlHeader > referenceHeaders )
    {
      referenceHeaders_ = referenceHeaders;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getControlServiceUrl()
    {
        return controlServiceUrl_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setControlServiceUrl( final String serviceUrl )
    {
        controlServiceUrl_ = serviceUrl;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List< XmlHeader > getControlHeaders()
    {
      return controlHeaders_;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setControlHeaders( final List< XmlHeader > controlHeaders )
    {
      controlHeaders_ = controlHeaders;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map< String, DiffFilter > getFilterRegistry()
    {
        return filterRegistry_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setFilterRegistry( final Map< String, DiffFilter > filterRegistry )
    {
        filterRegistry_ = filterRegistry;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Pattern getTestIdPattern()
    {
        return testIdPattern_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setTestIdPattern( final Pattern testIdPattern )
    {
      testIdPattern_ = testIdPattern;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public long getTimeoutMs()
    {
        return timeoutMs_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setTimeoutMs( final long timeoutMs )
    {
        timeoutMs_ = timeoutMs;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public double getEpsilon()
    {
        return epsilon_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setEpsilon( final double epsilon )
    {
        epsilon_ = epsilon;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getStroreReportPath()
    {
        return storeReportPath_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setStoreReportPath( final String storeReportPath )
    {
        storeReportPath_ = storeReportPath;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean getReportWhiteNoise()
    {
        return reportWhiteNoise_;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setReportWhiteNoise( final boolean reportWhiteNoise )
    {
        reportWhiteNoise_ = reportWhiteNoise;
    }
}
