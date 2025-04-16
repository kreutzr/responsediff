<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY nbsp  "&#160;" >
]>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output omit-xml-declaration="yes"/>

  <xsl:template match="/XmlResponseDiffSetup">
  <html>
  <head>
  <style>
    table, th, td {
       border-width: 1px;
       border-color: black;
       border-style: solid;
    }

    td {
       text-align: right;
       margin : 0px;
    }
  </style>
  </head>
  <body>
	  <h1>TestRun: "<xsl:value-of select="description"/>"</h1>
	
	  <xsl:apply-templates select="analysis"/>
	
	  <ul>
     <li><xsl:apply-templates select="testSet"/></li>
     <li><xsl:apply-templates select="test"/></li>
     </ul>
  </body>
  </html>
  </xsl:template>

  <xsl:template match="testSet">
     <h2>TestSet: "<xsl:value-of select="./@id"/>"</h2>
     FileName: <xsl:value-of select="fileName"/><br/>
     Description: <xsl:value-of select="description"/><br/>

     project:<xsl:value-of select="./@project"/><br/>
     order:<xsl:value-of select="./@order"/><br/>
     iterations:<xsl:value-of select="./@iterations"/><br/>

     <xsl:apply-templates select="analysis"/>

     <ul>
     <li><xsl:apply-templates select="testSet"/></li>
     <li><xsl:apply-templates select="test"/></li>
     </ul>
  </xsl:template>

  <xsl:template match="test">
     <h3>Test: "<xsl:value-of select="./@id"/>"</h3>
     Description: <xsl:value-of select="description"/><br/>
     <pre>CURL: "<xsl:value-of select="request/curl"/>"</pre>
     <div>

     <xsl:variable name="ticketUrl"><xsl:value-of select="/XmlResponseDiffSetup/ticketServiceUrl" /><xsl:value-of select="./@ticketReference"/></xsl:variable>
     reference: <a href="{$ticketUrl}"><xsl:value-of select="$ticketUrl" /></a><br/>
     iterations:<xsl:value-of select="./@iterations"/><br/>
     </div>
     <xsl:apply-templates select="analysis"/>

     <xsl:apply-templates select="response/httpResponse"/>
  </xsl:template>

  <xsl:template match="analysis">
     <table>
     <thead>
        <tr>
          <th>begin</th>
          <th>end</th>
          <th>duration</th>
          <th>minDuration</th>
          <th>maxDuration</th>
          <th>avgDuration</th>
          <th>success</th>
          <th>fail</th>
          <th>skip</th>
          <th>total</th>
        </tr>
     </thead>
     <tbody>
        <tr>
          <td><xsl:value-of select="begin" /></td>
          <td><xsl:value-of select="end" /></td>
          <td><xsl:value-of select="duration" /></td>
          <td><xsl:value-of select="minDuration" /></td>
          <td><xsl:value-of select="maxDuration" /></td>
          <td><xsl:value-of select="avgDuration" /></td>
          <td><xsl:value-of select="successCount" /></td>
          <td><xsl:value-of select="failCount" /></td>
          <td><xsl:value-of select="skipCount" /></td>
          <td><xsl:value-of select="totalCount" /></td>
        </tr>
     </tbody>
     </table>

     <xsl:apply-templates select="messages" />
  </xsl:template>

  <xsl:template match="httpResponse">
    <h4>HTTP Response:</h4>
    <div>
    Status:  <xsl:value-of select="httpStatus" /><br/>
    Headers: <xsl:apply-templates select="headers/header" /><br/>
    Body:    <xsl:value-of select="body" /><br/>
    </div>
  </xsl:template>

  <xsl:template match="header">
    <xsl:value-of select="./@name" />=<xsl:value-of select="text()" />&nbsp;
  </xsl:template>

  <xsl:template match="messages">
     <table>
     <thead>
        <tr>
          <th>level</th>
          <th>JsonPath</th>
          <th>message</th>
        </tr>
     </thead>
     <tbody>
     <xsl:apply-templates select="message"/>
     </tbody>
     </table>
  </xsl:template>

  <xsl:template match="message">
        <tr>
          <td><xsl:value-of select="./@level" /></td>
          <td><xsl:value-of select="./@path" /></td>
          <td><xsl:value-of select="text()" /></td>
        </tr>
  </xsl:template>

</xsl:stylesheet>