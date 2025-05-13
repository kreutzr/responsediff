<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY nbsp  "&#160;" >
]>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output omit-xml-declaration="yes"/>

  <!-- ========================================================================== -->

<xsl:template match="/XmlResponseDiffSetup"><xsl:call-template name="headline"/> <xsl:value-of select="description"/>
(<xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="analysis/begin" /></xsl:call-template>)
:doctype: book
:encoding: utf-8
:lang: de
:toc: left
:toclevels: 5
:icons: font
:icon-set: fa-solid

<xsl:apply-templates select="analysis">
<xsl:with-param name="isTest">false</xsl:with-param>
</xsl:apply-templates>

<xsl:apply-templates select="testSet"/>

<xsl:apply-templates select="runtime"/>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="testSet"><xsl:call-template name="headline"/> TestSet: <xsl:call-template name="substring-after-last">
<xsl:with-param name="string" select="./@id" />
<xsl:with-param name="delimiter" select="' / '" />
</xsl:call-template>

[cols="10h,90"]
|===

| FileName | <xsl:value-of select="fileName"/>

| Description | <xsl:value-of select="description"/>

| Order | <xsl:value-of select="./@order"/>
<xsl:if test="./@breakOnFailure">
| BreakOnFailure | <xsl:value-of select="./@breakOnFailure"/>
</xsl:if>
|===

<xsl:apply-templates select="analysis">
<xsl:with-param name="isTest">false</xsl:with-param>
</xsl:apply-templates>

<xsl:apply-templates select="test"/>

<xsl:apply-templates select="testSet"/>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="runtime">

'''

ResponseDiff: Version <xsl:value-of select="buildVersion" /> (build-time: <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="buildTime" /></xsl:call-template>)

XSLT: <xsl:value-of select="system-property('xsl:version')"/>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="test">
<xsl:variable name="result">
  <xsl:choose>
    <xsl:when test="analysis">
      <xsl:choose>
        <xsl:when test="analysis/successCount=1">success</xsl:when>
        <xsl:when test="analysis/failCount=1">fail</xsl:when>
        <xsl:when test="analysis/skipCount>=1">skip</xsl:when>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>skip</xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="report" select ="./@report" />

<xsl:if test="contains($report,$result) or contains($report,'all')" ><xsl:call-template name="insertTest">
<xsl:with-param name="result" select="$result" /></xsl:call-template></xsl:if>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="insertTest"><xsl:param name="result" /><xsl:call-template name="headline"/> <xsl:call-template name="headlineIcon"><xsl:with-param name="result" select="$result" /></xsl:call-template> Test: <xsl:call-template name="substring-after-last">
<xsl:with-param name="string" select="./@id" />
<xsl:with-param name="delimiter" select="' / '" />
</xsl:call-template>

<xsl:variable name="ticketUrl"><xsl:value-of select="/XmlResponseDiffSetup/ticketServiceUrl" /></xsl:variable>

[cols="10h,90"]
|===

| Description | <xsl:value-of select="description"/>

| Ticket references | <xsl:call-template name="handle-ticket-reference"><xsl:with-param name="ticketReference" select="./@ticketReference" /><xsl:with-param name="ticketUrl" select="$ticketUrl" /></xsl:call-template>
<xsl:if test="./@breakOnFailure">
| BreakOnFailure | <xsl:value-of select="./@breakOnFailure"/>
</xsl:if>
|===

[source,curl]
----
<xsl:value-of select="request/curl"/>
----

<xsl:apply-templates select="analysis">
<xsl:with-param name="isTest">true</xsl:with-param>
</xsl:apply-templates>

<xsl:apply-templates select="response/httpResponse"/>

<xsl:if test="response/httpResponse/originalResponse">
<xsl:apply-templates select="response/httpResponse/originalResponse" />
</xsl:if>

<xsl:apply-templates select="response/referenceResponse"/>

</xsl:template>

<!-- ========================================================================== -->

<xsl:template name="headlineIcon"><xsl:param name="result" /><xsl:choose>
<xsl:when test="$result='success'"> icon:check[]</xsl:when>
<xsl:when test="$result='fail'"> icon:warning[]</xsl:when>
<xsl:when test="$result='skip'"> icon:ban[]</xsl:when>
</xsl:choose>
</xsl:template>

<!-- ========================================================================== -->

<xsl:template name="handle-ticket-reference">
<xsl:param name="ticketReference" />
<xsl:param name="ticketUrl" />
<xsl:choose><xsl:when test="$ticketReference and not($ticketReference = '')">
  <xsl:call-template name="split-ticket-reference">
    <xsl:with-param name="ticketReference" select="$ticketReference" />
    <xsl:with-param name="ticketUrl" select="$ticketUrl" />
  </xsl:call-template>
  </xsl:when>
  <xsl:otherwise>-</xsl:otherwise>
</xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="split-ticket-reference">
<xsl:param name="ticketReference" />
<xsl:param name="ticketUrl" />
<xsl:if test="string-length($ticketReference)">
   <xsl:variable name="part" select="normalize-space(substring-before(concat($ticketReference,','), ','))"/>link:<xsl:value-of select="$ticketUrl" /><xsl:value-of select="$part"/>[<xsl:value-of select="$part"/>]<xsl:if test="contains($ticketReference, ',')">, </xsl:if><xsl:call-template name="split-ticket-reference">
     <xsl:with-param name="ticketReference" select="substring-after($ticketReference, ',')"/>
     <xsl:with-param name="ticketUrl" select="$ticketUrl" />
   </xsl:call-template>
</xsl:if>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="formatIsoDate">
<xsl:param name="isoDateTime" />
<!-- Change date format as you like -->
<xsl:value-of select="concat(
    substring($isoDateTime,1,4),'-',
    substring($isoDateTime,6,2),'-',
    substring($isoDateTime,9,2),', ',
    substring($isoDateTime,12,2),':',
    substring($isoDateTime,15,2),':',
    substring($isoDateTime,18,2)
)"/>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="formatDuration">
<xsl:param name="duration" />
	<xsl:call-template name="durationStep">
		<xsl:with-param name="duration"><xsl:value-of select="substring-after(translate($duration,',','.'),'P')"/></xsl:with-param>
	</xsl:call-template>
</xsl:template>


<!-- Time -->
<xsl:template name="durationStep">
<xsl:param name="duration"/>
<xsl:choose>
<xsl:when test="contains($duration,'T')">
<xsl:call-template name="durationDateStep"><xsl:with-param name="duration" select="substring-before($duration,'T')"/></xsl:call-template>
<xsl:call-template name="durationTimeStep"><xsl:with-param name="duration" select="substring-after($duration,'T')"/></xsl:call-template>
</xsl:when>
<xsl:otherwise>
<xsl:call-template name="durationDateStep"><xsl:with-param name="duration" select="$duration"/></xsl:call-template>
</xsl:otherwise>
</xsl:choose>
</xsl:template>


<xsl:template name="durationDateStep">
<xsl:param name="duration"/>
<xsl:choose>
<!-- Years -->
<xsl:when test="contains($duration,'Y')">
<xsl:value-of select="substring-before($duration,'Y')"/> [year] <xsl:call-template name="durationDateStep"><xsl:with-param name="duration" select="substring-after($duration,'Y')"/></xsl:call-template>
</xsl:when>
<!-- Months -->
<xsl:when test="contains($duration,'M')">
<xsl:value-of select="substring-before($duration,'M')"/> [month] <xsl:call-template name="durationDateStep"><xsl:with-param name="duration" select="substring-after($duration,'M')"/></xsl:call-template>
</xsl:when>
<!-- Weeks -->
<xsl:when test="contains($duration,'W')">
<xsl:value-of select="substring-before($duration,'W')"/> [week] <xsl:call-template name="durationDateStep"><xsl:with-param name="duration" select="substring-after($duration,'W')"/></xsl:call-template>
</xsl:when>
<!-- Days -->
<xsl:when test="contains($duration,'D')">
<xsl:value-of select="substring-before($duration,'D')"/> [day] <xsl:call-template name="durationDateStep"><xsl:with-param name="duration" select="substring-after($duration,'D')"/></xsl:call-template>
</xsl:when>
</xsl:choose>
</xsl:template>


<xsl:template name="durationTimeStep">
<xsl:param name="duration"/>
<xsl:choose>
<!-- Hours -->
<xsl:when test="contains($duration,'H')">
<xsl:value-of select="substring-before($duration,'H')"/> [h] <xsl:call-template name="durationTimeStep"><xsl:with-param name="duration" select="substring-after($duration,'H')"/></xsl:call-template>
</xsl:when>
<!-- Minutes -->
<xsl:when test="contains($duration,'M')">
<xsl:value-of select="substring-before($duration,'M')"/> [min] <xsl:call-template name="durationTimeStep"><xsl:with-param name="duration" select="substring-after($duration,'M')"/></xsl:call-template>
</xsl:when>
<!-- Seconds -->
<xsl:when test="contains($duration,'S')">
<xsl:value-of select="substring-before($duration,'S')"/> [sec]
</xsl:when>
</xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="analysis">
<xsl:param name="isTest" />
*Analysis*
[cols="10h,23,10h,23,10h,24"]
|===
| Measure | Value | Measure | Value | Measure | Value

| begin       | <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="begin" /></xsl:call-template> | end | <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="end" /></xsl:call-template> | duration    | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="duration" /></xsl:call-template> 

| avgDuration | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="avgDuration" /></xsl:call-template> | minDuration | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="minDuration" /></xsl:call-template>  | maxDuration | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="maxDuration" /></xsl:call-template>

| success     | <xsl:value-of select="successCount" /> | fail        | <xsl:value-of select="failCount" /> | skip        | <xsl:value-of select="skipCount" />

| total       | <xsl:value-of select="totalCount" />   | expectations| <xsl:value-of select="expectedCount" />|             |
|===


<xsl:if test="$isTest='true'">
<xsl:call-template name="handleIgnores" />
</xsl:if>

<xsl:apply-templates select="messages" />
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="httpResponse">
*Candidate HTTP Response*

Headers:
[cols="20,80"]
|===
| Name | Value

<xsl:apply-templates select="headers/header" />
|===

<xsl:choose>
<xsl:when test="download">
Download:

xref:<xsl:value-of select="download/filename" />[] (<xsl:value-of select="download/size" /> bytes)

</xsl:when>
<xsl:otherwise>
Body:
[source,json]
----
<xsl:value-of select="body" />
----
</xsl:otherwise>
</xsl:choose>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="originalResponse">
*Original candidate HTTP Response*

Headers:
[cols="20,80"]
|===
| Name | Value

<xsl:apply-templates select="headers/header" />
|===

<xsl:choose>
<xsl:when test="download">
Download:

xref:<xsl:value-of select="download/filename" />[] (<xsl:value-of select="download/size" /> bytes)

</xsl:when>
<xsl:otherwise>
Body:
[source,json]
----
<xsl:value-of select="body" />
----
</xsl:otherwise>
</xsl:choose>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="referenceResponse">
*Reference HTTP Response*

Headers:
[cols="20,80"]
|===
| Name | Value

<xsl:apply-templates select="headers/header" />
|===

<xsl:choose>
<xsl:when test="download">
Download:

xref:<xsl:value-of select="download/filename" />[] (<xsl:value-of select="download/size" /> bytes)

</xsl:when>
<xsl:otherwise>
Body:
[source,json]
----
<xsl:value-of select="body" />
----
</xsl:otherwise>
</xsl:choose>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="header">

| <xsl:value-of select="./@name" /> | <xsl:value-of select="text()" />
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="handleIgnores">
<xsl:if test="../response/ignore[@justExplain='true']">

*Error explanations*
[cols="25,75"]
|===
| JsonPath | Message

  <xsl:apply-templates select="../response/ignore[@justExplain='true']" />
|===
</xsl:if>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="ignore">

| <xsl:value-of select="header"/><xsl:value-of select="path"/>
| <xsl:value-of select="explanation"/>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="messages">

*Error messages*
[cols="10,15,75"]
|===
| Level | JsonPath | Message

<xsl:apply-templates select="message"/>
|===
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="message">

| <xsl:value-of select="./@level" />
| <xsl:value-of select="./@path" />
| <xsl:value-of select="text()" />
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="headline"><xsl:choose>
<xsl:when test="structureDepth =  1">= </xsl:when>
<xsl:when test="structureDepth =  2">== </xsl:when>
<xsl:when test="structureDepth =  3">=== </xsl:when>
<xsl:when test="structureDepth =  4">==== </xsl:when>
<xsl:when test="structureDepth =  5">===== </xsl:when>
<xsl:when test="structureDepth =  6">====== </xsl:when>
<xsl:when test="structureDepth =  7">======= </xsl:when>
<xsl:when test="structureDepth =  8">======== </xsl:when>
<xsl:when test="structureDepth =  9">========= </xsl:when>
<xsl:when test="structureDepth = 10">========== </xsl:when>
<xsl:when test="structureDepth = 11">=========== </xsl:when>
<xsl:when test="structureDepth = 12">============ </xsl:when>
<xsl:otherwise>============= (<xsl:value-of select="structureDepth"/>) </xsl:otherwise></xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="substring-after-last">
    <xsl:param name="string" />
    <xsl:param name="delimiter" />
    <xsl:choose>
      <xsl:when test="contains($string, $delimiter)">
        <xsl:call-template name="substring-after-last">
          <xsl:with-param name="string"
            select="substring-after($string, $delimiter)" />
          <xsl:with-param name="delimiter" select="$delimiter" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$string" /></xsl:otherwise>
    </xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

</xsl:stylesheet>
