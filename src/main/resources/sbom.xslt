<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY nbsp  "&#160;" >
]>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:cyc="http://cyclonedx.org/schema/bom/1.6"
    exclude-result-prefixes="cyc"
  >
  <xsl:output omit-xml-declaration="yes"/>

  <!-- ========================================================================== -->

<xsl:template match="/">
<html>
<head>
<style>
body {
  font-family: "Helvetica", sans-serif;
}
table, th, td {
  border: 1px solid;
}
table {
  width: 100%;
}
th, td {
  padding: 6px;
}
th {
  background-color: #EFEFEF;
}
</style>
</head>
<body>
<h1>SBOM</h1>
<table style="width:30%">
<tr><th>Project</th><td><xsl:value-of select="cyc:bom/cyc:metadata/cyc:component/cyc:name"/></td></tr>
<tr><th>Group  </th><td><xsl:value-of select="cyc:bom/cyc:metadata/cyc:component/cyc:group"/></td></tr>
<tr><th>Version</th><td><xsl:value-of select="cyc:bom/cyc:metadata/cyc:component/cyc:version"/></td></tr>
<tr><th>Created</th><td><xsl:value-of select="cyc:bom/cyc:metadata/cyc:timestamp"/></td></tr>
</table>
<p/>

3rd party libraries and licenses used:
<p/>

<table>
<tr><th>Group</th><th>Name</th><th>Version</th><th>License</th><th>Description</th><th>Library project</th></tr>
  <xsl:apply-templates select="cyc:bom/cyc:components/cyc:component">
    <xsl:sort select="cyc:group"/>
    <xsl:sort select="cyc:name"/>
 </xsl:apply-templates>
</table>
</body>
</html>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="cyc:component">
<tr>
  <td><xsl:value-of select="cyc:group"/></td>
  <td><xsl:value-of select="cyc:name"/></td>
  <td><xsl:value-of select="cyc:version"/></td>
  <td><xsl:apply-templates select="cyc:licenses/cyc:license"/></td>
  <td><xsl:value-of select="cyc:description"/></td>
  <td><xsl:apply-templates select="cyc:externalReferences"/></td>
</tr>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="cyc:license">
<span>
  <xsl:choose>
  <xsl:when test="cyc:url">
    <xsl:element name="a"><xsl:attribute name="href"><xsl:value-of select="cyc:url"/></xsl:attribute><xsl:value-of select="cyc:id"/></xsl:element>
  </xsl:when>
  <xsl:otherwise>
    <xsl:value-of select="cyc:id"/>
  </xsl:otherwise>
  </xsl:choose>
  <br/>
</span>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="cyc:externalReferences">
<span>
  <xsl:if test="cyc:reference/@type='vcs'">
    <xsl:element name="a"><xsl:attribute name="href"><xsl:value-of select="cyc:reference[@type='vcs']/cyc:url"/></xsl:attribute><xsl:value-of select="cyc:reference[@type='vcs']/cyc:url"/></xsl:element>
  </xsl:if>
  <br/>
</span>
</xsl:template>

  <!-- ========================================================================== -->

</xsl:stylesheet>
