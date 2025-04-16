<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY nbsp  "&#160;" >
]>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output omit-xml-declaration="yes"/>

  <!-- ========================================================================== -->

<xsl:template match="/licenseSummary">
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
<h1>Licenses</h1>

3rd party libraries and licenses used:
<p/>

<table>
<tr><th>GroupId</th><th>ArtifactId</th><th>Version</th><th>License</th></tr>
  <xsl:apply-templates select="//dependency"/>
</table>
</body>
</html>
</xsl:template>

  <!-- ========================================================================== -->

<xsl:template match="dependency">
<tr>
  <td><xsl:value-of select="groupId"/></td>
  <td><xsl:value-of select="artifactId"/></td>
  <td><xsl:value-of select="version"/></td>
  <td><xsl:apply-templates select="licenses/license"/></td>
</tr>  
</xsl:template>

  <!-- ========================================================================== -->
      
<xsl:template match="license">
<span>
  <xsl:element name="a"><xsl:attribute name="href"><xsl:value-of select="url"/></xsl:attribute><xsl:value-of select="name"/></xsl:element>
  <br/>
</span>
</xsl:template>

  <!-- ========================================================================== -->

</xsl:stylesheet>
