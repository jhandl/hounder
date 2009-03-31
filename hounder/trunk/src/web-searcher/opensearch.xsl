<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
<xsl:output method="xml" encoding="UTF-8" indent="yes"/>
<xsl:template match="/SearchResults"
    xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/"
    xmlns:hounder="http://www.flaptor.com/opensearch1.1"
    xmlns:fn="http://www.w3.org/2005/02/xpath-functions">
<rss version="2.0" 
      xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/"
      xmlns:atom="http://www.w3.org/2005/Atom"
      xmlns:hounder="http://www.flaptor.com/opensearch1.1">
    <channel>
        <title>Hounder's search result for: <xsl:value-of select="query"/></title>
        
        <description>Hounder's search result for: <xsl:value-of select="query"/></description>
        <opensearch:totalResults><xsl:value-of select="totalResults"/></opensearch:totalResults>
        <opensearch:startIndex><xsl:value-of select="startIndex"/></opensearch:startIndex>
        <opensearch:itemsPerPage><xsl:value-of select="itemsPerPage"/></opensearch:itemsPerPage>
        <opensearch:Query role="request" startPage="1"><xsl:attribute name="searchTerms"><xsl:value-of select="query"/></xsl:attribute></opensearch:Query>
        
        <xsl:if test="status = 0">
            <xsl:for-each select="//result">
                <item>
                    <xsl:for-each select="*">
                        <xsl:choose>
                            <xsl:when test="name() = 'snippet_title'">
                                <title> <xsl:value-of select="current()"/> </title>
                            </xsl:when>
                            <xsl:when test="name() = 'url'">
                                <link> <xsl:value-of select="current()"/>i </link>
                            </xsl:when>
                            <xsl:when test="name() = 'snippet_text'">
                                <description> <xsl:value-of select="current()"/> </description>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="hounder:field">
                                    <xsl:attribute name="name"><xsl:value-of select="name()"/></xsl:attribute>
                                    <xsl:value-of select="current()"/>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </item>
            </xsl:for-each>
        </xsl:if>
        <xsl:if test="status != 0">
            <item>
                <title>Error!</title>
                <description> <xsl:value-of select="statusDesc"/></description>
            </item>
        </xsl:if>
    </channel>
</rss>  
</xsl:template>

</xsl:stylesheet>
