<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY nbsp  "&#160;" >
]>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output omit-xml-declaration="yes"/>

  <!-- ========================================================================== -->

<xsl:template match="/XmlResponseDiffSetup"><xsl:call-template name="headline"/> <xsl:choose><xsl:when test="reportTitle != ''"><xsl:value-of select="reportTitle"/></xsl:when><xsl:otherwise><xsl:value-of select="description"/></xsl:otherwise></xsl:choose>
- <xsl:if test="reportTitle != ''"><xsl:value-of select="description"/> / </xsl:if>( <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="analysis/begin" /></xsl:call-template> ) -
:doctype: book
:encoding: utf-8
:lang: de
:toc: left
:toclevels: 5
:icons: font
:icon-set: fas

<xsl:apply-templates select="analysis"/>

<xsl:apply-templates select="testSet"/>

<xsl:apply-templates select="runtime"/>

</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="testSet">
<xsl:variable name="result">
  <xsl:choose>
    <xsl:when test="analysis">
      <xsl:choose>
        <!-- NOTE:  xsl:when : First match wins (only) -->
        <xsl:when test="analysis/skipCount>0">skip</xsl:when> <!-- Any skip is considered: Prio 1 -->
        <xsl:when test="analysis/failCount>0">fail</xsl:when> <!-- Any fail is considered: Prio 2 -->
        <xsl:when test="analysis/warnCount>0">warn</xsl:when> <!-- Any warn is considered: Prio 3 -->
        <xsl:otherwise>success</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>skip</xsl:otherwise> <!-- Skipped test sets do not have an analysis element -->
  </xsl:choose>
</xsl:variable>
<xsl:call-template name="insertTestSet"><xsl:with-param name="result" select="$result" /></xsl:call-template>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="insertTestSet"><xsl:param name="result" /><xsl:call-template name="headline"/><xsl:call-template name="substring-after-last">
<xsl:with-param name="string" select="./@id" />
<xsl:with-param name="delimiter" select="' / '" />
</xsl:call-template><xsl:choose>
<xsl:when test="( not(./@orga = 'true') and ( contains(./@report,$result) or contains(./@report,'all') ) ) or ( ./@orga = 'true' and ( not($result = 'success') or contains(./@report,'orga') ) )" >

[cols="15h,85"]
|===

| FileName | <xsl:value-of select="fileName"/>

<xsl:if test="./description != ''">
| Description | <xsl:value-of select="description"/>
</xsl:if>
| Order | <xsl:value-of select="./@order"/>
<xsl:if test="./@breakOnFailure">
| BreakOnFailure | <xsl:value-of select="./@breakOnFailure"/>
</xsl:if>
|===

<xsl:apply-templates select="analysis"/>

<xsl:apply-templates select="test"/>

<xsl:apply-templates select="testSet"/>
</xsl:when>
<xsl:otherwise>
&nbsp;<!-- Keep this together with line break for correct headline and TOC rendering -->

</xsl:otherwise>
</xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="runtime">

'''

*Legend*
|===
| icon:bomb[] | Test failed
| icon:bolt[] | Test has warning
| icon:ban[]  | Test was skipped
|===

'''

*Technical information*

ResponseDiff: Version <xsl:value-of select="buildVersion" /> (build-time: <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="buildTime" /></xsl:call-template>)

XSLT: <xsl:value-of select="system-property('xsl:version')"/>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="test">
<xsl:variable name="result">
  <xsl:choose>
    <xsl:when test="analysis">
      <xsl:choose>
        <!-- NOTE:  xsl:when : First match wins (only) -->
        <xsl:when test="analysis/failCount=1">fail</xsl:when> <!-- Most important to indicate -->
        <xsl:when test="analysis/warnCount>0">warn</xsl:when> <!-- More important than success -->
        <xsl:when test="analysis/successCount=1">success</xsl:when> <!-- Neither fail nor warn => Probably success -->
        <xsl:otherwise>skip</xsl:otherwise> <!-- Neither fail, nor warn nor success => Test was skipped -->
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>skip</xsl:otherwise> <!-- Skipped tests do not have an analysis element -->
  </xsl:choose>
</xsl:variable>
<xsl:call-template name="insertTest"><xsl:with-param name="result" select="$result" /></xsl:call-template>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="insertTest"><xsl:param name="result" /><xsl:call-template name="headline"/> <xsl:call-template name="headlineIcon"><xsl:with-param name="result" select="$result" /></xsl:call-template>&nbsp;<xsl:call-template name="substring-after-last">
<xsl:with-param name="string" select="./@id" />
<xsl:with-param name="delimiter" select="' / '" />
</xsl:call-template><xsl:choose>
<xsl:when test="( not(./@orga = 'true') and ( contains(./@report,$result) or contains(./@report,'all') ) ) or ( ./@orga = 'true' and ( ( $result != 'success' ) or contains(./@report,'orga') ) )" >
<xsl:if test="( (./description != '') or (./@waitBefore != '') or (./@ticketReference != '') or (./@breakOnFailure) )">

[cols="15h,85"]
|===

<xsl:if test="./description != ''">
| Description | <xsl:value-of select="description"/>
</xsl:if>
<xsl:if test="./@waitBefore != ''">
| Wait before | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="./@waitBefore" /></xsl:call-template>
</xsl:if>
<xsl:if test="./@ticketReference != ''">
| Ticket references | <xsl:call-template name="handle-ticket-reference"><xsl:with-param name="ticketReference" select="./@ticketReference" /></xsl:call-template>
</xsl:if>
<xsl:if test="./@breakOnFailure">
| BreakOnFailure | <xsl:value-of select="./@breakOnFailure"/>
</xsl:if>
|===
</xsl:if>

[source,curl]
----
<xsl:value-of select="request/curl"/>
----

<xsl:apply-templates select="analysis"/>

<xsl:apply-templates select="response/httpResponse"/>

<xsl:if test="response/httpResponse/originalResponse">
<xsl:apply-templates select="response/httpResponse/originalResponse" />
</xsl:if>

<xsl:if test="analysis/successCount!=1">
<xsl:apply-templates select="response/referenceResponse"/>

<xsl:apply-templates select="response/controlResponse"/>
</xsl:if>
</xsl:when>
<xsl:otherwise>
&nbsp;<!-- Keep this together with line break for correct headline and TOC rendering -->

</xsl:otherwise>
</xsl:choose>
</xsl:template>

<!-- ========================================================================== -->

<xsl:template name="headlineIcon"><xsl:param name="result" /><xsl:choose>
<xsl:when test="$result='success'"> icon:check[]</xsl:when>
<xsl:when test="$result='fail'"> icon:bomb[]</xsl:when>
<xsl:when test="$result='warn'"> icon:bolt[]</xsl:when>
<xsl:when test="$result='skip'"> icon:ban[]</xsl:when>
</xsl:choose>
</xsl:template>

<!-- ========================================================================== -->

<xsl:template name="handle-ticket-reference">
<xsl:param name="ticketReference" />
<xsl:choose><xsl:when test="$ticketReference and not($ticketReference = '')">
  <xsl:call-template name="split-ticket-reference">
    <xsl:with-param name="ticketReference" select="$ticketReference" />
    <xsl:with-param name="prefix" />
  </xsl:call-template>
  </xsl:when>
  <xsl:otherwise>-</xsl:otherwise>
</xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="split-ticket-reference">
<xsl:param name="ticketReference" />
<xsl:param name="prefix" />
<xsl:if test="string-length($ticketReference)">
  <xsl:value-of select="$prefix"/>

  <xsl:variable name="part" select="normalize-space(substring-before(concat($ticketReference,','), ','))"/>
  <xsl:variable name="urlReference"><xsl:choose>
    <xsl:when test="contains($part, ':')"><xsl:value-of select="normalize-space(substring-before($part, ':'))"/></xsl:when>
    <xsl:otherwise>default</xsl:otherwise>
  </xsl:choose></xsl:variable>
  <xsl:variable name="ticketId"><xsl:choose>
    <xsl:when test="contains($part, ':')"><xsl:value-of select="substring-after($part, ':')"/></xsl:when>
    <xsl:otherwise><xsl:value-of select="$part"/></xsl:otherwise>
  </xsl:choose></xsl:variable>
  <xsl:call-template name="render-ticket-reference">
    <xsl:with-param name="ticketId"  select="$ticketId"/>
    <xsl:with-param name="ticketUrl" select="/XmlResponseDiffSetup/ticketServiceUrls/ticketServiceUrl[@id=$urlReference]" />
  </xsl:call-template>

  <xsl:call-template name="split-ticket-reference">
    <xsl:with-param name="ticketReference" select="substring-after($ticketReference, ',')"/>
    <xsl:with-param name="prefix">, </xsl:with-param>
  </xsl:call-template>
</xsl:if>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="render-ticket-reference">
<xsl:param name="ticketId" />
<xsl:param name="ticketUrl" />
<xsl:if test="string-length($ticketId)">link:<xsl:value-of select="$ticketUrl" /><xsl:value-of select="$ticketId"/>[<xsl:value-of select="$ticketId"/>]</xsl:if>
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
)"/></xsl:template>

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
<xsl:value-of select="substring-before($duration,'S')"/> [sec]</xsl:when>
</xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="search-and-replace">
  <xsl:param name="input"/>
  <xsl:param name="search-string"/>
  <xsl:param name="replace-string"/>
  <xsl:param name="only-first"/>
  <xsl:choose>
    <!-- Check if input contains search-string -->
    <xsl:when test="$search-string and contains($input,$search-string)">
      <!-- Keep head, replace first occurance and keep tail. -->
      <xsl:value-of select="substring-before($input,$search-string)"/>
      <xsl:value-of select="$replace-string"/>
      <xsl:if test="$only-first='false'">
        <xsl:call-template name="search-and-replace">
          <xsl:with-param name="input" select="substring-after($input,$search-string)"/>
          <xsl:with-param name="search-string" select="$search-string"/>
          <xsl:with-param name="replace-string" select="$replace-string"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="$only-first='true'">
        <!-- No (further) occurances of search-string. Therefore return current value. -->
        <xsl:value-of select="substring-after($input,$search-string)"/>
      </xsl:if>
    </xsl:when>
    <xsl:otherwise>
      <!-- No (further) occurances of search-string. Therefore return current value. -->
      <xsl:value-of select="$input"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="analysis">
*Analysis*
[cols="15h,18,15h,18,15h,19"]
|===
| Measure | Value | Measure | Value | Measure | Value

| begin       | <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="begin" /></xsl:call-template> | end | <xsl:call-template name="formatIsoDate"><xsl:with-param name="isoDateTime" select="end" /></xsl:call-template> | duration    | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="duration" /></xsl:call-template>

<xsl:if test="(avgDuration != '') and (minDuration != '') and (maxDuration != '')">
| avgDuration | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="avgDuration" /></xsl:call-template> | minDuration | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="minDuration" /></xsl:call-template> | maxDuration | <xsl:call-template name="formatDuration"><xsl:with-param name="duration" select="maxDuration" /></xsl:call-template>

</xsl:if>
| success     | <xsl:value-of select="successCount" /> | fail        | <xsl:value-of select="failCount" />    | skip        | <xsl:value-of select="skipCount" />

| total       | <xsl:value-of select="totalCount" />   | expectations| <xsl:value-of select="expectedCount" />| warn        | <xsl:value-of select="warnCount" />
|===

<xsl:apply-templates select="messages" />
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="httpResponse">
*Candidate HTTP Response*

Headers:
[cols="15,85"]
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
[cols="15,85"]
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
[cols="15,85"]
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

<xsl:template match="controlResponse">
*Control HTTP Response*

Headers:
[cols="15,85"]
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

<xsl:template match="messages">

*Error messages*
[cols="15,18,67"]
|===
| Level | JsonPath | Message

<xsl:apply-templates select="message"/>
|===
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="message">

| <xsl:value-of select="./@level" /><xsl:if test="./@executionContextConstraint != ''"> (context="<xsl:value-of select="./@executionContextConstraint" />")</xsl:if>
| <xsl:value-of select="./@path" />
| <xsl:call-template name="search-and-replace"> <!-- Replace first "*" of regular expressions to avoid confusing AsciiDoc. -->
  <xsl:with-param name="input" select="text()"/>
  <xsl:with-param name="search-string">*</xsl:with-param>
  <xsl:with-param name="replace-string">\*</xsl:with-param>
  <xsl:with-param name="only-first">true</xsl:with-param>
</xsl:call-template>
<xsl:apply-templates select="../../../response/ignore[@justExplain='true']">
  <xsl:with-param name="path"><xsl:value-of select="./@path" /></xsl:with-param>
</xsl:apply-templates>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="ignore">
<xsl:param name="path"/>
<xsl:if test="./path=$path">
 +
*Explanation* : +
<xsl:value-of select="./explanation"/>
</xsl:if>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template name="headline"><xsl:choose>
<xsl:when test="structureDepth =  1">= </xsl:when>
<xsl:when test="structureDepth =  2">== </xsl:when>
<xsl:when test="structureDepth =  3">=== </xsl:when>
<xsl:when test="structureDepth =  4">==== </xsl:when>
<xsl:when test="structureDepth =  5">===== </xsl:when>
<!-- AsciiDoc does only support 5 levels in the table of content -->
<xsl:otherwise>====== </xsl:otherwise></xsl:choose>
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
