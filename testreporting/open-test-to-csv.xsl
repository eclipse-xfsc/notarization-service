<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:fn="http://www.w3.org/2005/xpath-functions"
        version="3.0"
        expand-text="yes"

        xmlns:csv="csv:csv"

        xmlns:otc="https://schemas.opentest4j.org/reporting/core/0.2.0"
        xmlns:ote="https://schemas.opentest4j.org/reporting/events/0.2.0"
        xmlns:otgit="https://schemas.opentest4j.org/reporting/git/0.2.0"
        xmlns:otjava="https://schemas.opentest4j.org/reporting/java/0.2.0"
        xmlns:junit="https://schemas.junit.org/open-test-reporting"

        xmlns:agg="https://not.xfsc.eu/test-report-aggregate"
>

    <xsl:output method="text" encoding="utf-8"/>
    <xsl:strip-space elements="*"/>

    <xsl:param name="moduleName" required="true" />

    <xsl:variable name="delimiter" select="','"/>

    <csv:columns>
        <column>Module</column>
        <column>TestClass</column>
        <column>TestMethod</column>
        <column>Requirement</column>
        <column>Time</column>
        <column>Result</column>
    </csv:columns>

    <xsl:template match="/">
        <xsl:variable name="phase-1-result">
            <xsl:apply-templates select="/" mode="phase-1"/>
        </xsl:variable>
        <xsl:apply-templates select="$phase-1-result" mode="phase-csv"/>
    </xsl:template>

    <xsl:template match="/" mode="phase-1">
        <!-- in junit tests, the interesting event is <e:started id="nnn"> an has type <junit:type>TEST</junit:type> -->
        <!-- starting from there, the information needed to fill the csv row can be collected -->
        <!-- one test can have multiple requirements, we emit one row per requirement -->
        <!-- in case that there is no requirement defined, we leave the requirement field empty -->
        <!-- we want to emit the following xml for easy csv transformation -->
        <!--
            <agg:aggregate>
                <agg:test>
                    <agg:Module>...</agg:Module>
                    <agg:TestClass>...</agg:TestClass>
                    <agg:TestMethod>...</agg:TestMethod>
                    <agg:Requirement>...</agg:Requirement>
                    <agg:Time>...</agg:Time>
                    <agg:Result>...</agg:Result>
                </agg:test>
            </agg:aggregate>
        -->

        <!-- Output row datasets for each matched property -->
        <agg:aggregate>
            <xsl:apply-templates select="ote:events/ote:started[otc:metadata/junit:type/text() = 'TEST']" mode="phase-1" />
        </agg:aggregate>
    </xsl:template>

    <xsl:template match="ote:started" mode="phase-1">
        <xsl:variable name="testId">
            <xsl:value-of select="./@id" />
        </xsl:variable>
        <xsl:variable name="testClass">
            <xsl:value-of select="otc:sources/otjava:methodSource/@className" />
        </xsl:variable>
        <xsl:variable name="testFun">
            <xsl:value-of select="otc:metadata/junit:legacyReportingName/text()" />
        </xsl:variable>
        <xsl:variable name="time">
            <xsl:value-of select="@time" />
        </xsl:variable>
        <xsl:variable name="result">
            <xsl:value-of select="../ote:finished[@id = $testId]/otc:result/@status" />
        </xsl:variable>

        <xsl:if test="count(../ote:reported[@id = $testId]/otc:attachments/otc:data/otc:entry[@key = 'REQUIREMENT']) = 0">
            <agg:test>
                <agg:Module><xsl:value-of select="$moduleName" /></agg:Module>
                <agg:TestClass><xsl:value-of select="$testClass" /></agg:TestClass>
                <agg:TestMethod><xsl:value-of select="$testFun" /></agg:TestMethod>
                <agg:Requirement />
                <agg:Time><xsl:value-of select="$time" /></agg:Time>
                <agg:Result><xsl:value-of select="$result" /></agg:Result>
            </agg:test>
        </xsl:if>

        <xsl:for-each select="../ote:reported[@id = $testId]/otc:attachments/otc:data/otc:entry[@key = 'REQUIREMENT']">
            <agg:test>
                <agg:Module><xsl:value-of select="$moduleName" /></agg:Module>
                <agg:TestClass><xsl:value-of select="$testClass" /></agg:TestClass>
                <agg:TestMethod><xsl:value-of select="$testFun" /></agg:TestMethod>
                <agg:Requirement><xsl:value-of select="text()" /></agg:Requirement>
                <agg:Time><xsl:value-of select="$time" /></agg:Time>
                <agg:Result><xsl:value-of select="$result" /></agg:Result>
            </agg:test>
        </xsl:for-each>

    </xsl:template>


    <!-- adaption of https://stackoverflow.com/a/9394064 -->

    <xsl:template match="/" mode="phase-csv">
        <!-- Output the CSV header -->
        <xsl:for-each select="document('')/*/csv:columns/*">
            <xsl:value-of select="."/>
            <xsl:if test="position() != last()">
                <xsl:value-of select="$delimiter"/>
            </xsl:if>
        </xsl:for-each>
        <xsl:text>&#xa;</xsl:text>

        <!-- Output rows for each matched property -->
        <xsl:apply-templates select="agg:aggregate/agg:test" mode="phase-csv"/>
    </xsl:template>

    <xsl:template match="agg:test" mode="phase-csv">
        <xsl:variable name="property" select="."/>

        <!-- Loop through the columns in order  -->
        <xsl:for-each select="document('')/*/csv:columns/*">
            <!-- Extract the column name and value  -->
            <xsl:variable name="column" select="."/>
            <xsl:variable name="value" select="$property/*[fn:local-name() = $column]"/>

            <!-- Quote the value if required -->
            <xsl:choose>
                <xsl:when test="contains($value, '&quot;')">
                    <xsl:variable name="x" select="replace($value, '&quot;',  '&quot;&quot;')"/>
                    <xsl:value-of select="concat('&quot;', $x, '&quot;')"/>
                </xsl:when>
                <xsl:when test="contains($value, $delimiter)">
                    <xsl:value-of select="concat('&quot;', $value, '&quot;')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$value"/>
                </xsl:otherwise>
            </xsl:choose>

            <!-- Add the delimiter unless we are the last expression -->
            <xsl:if test="position() != last()">
                <xsl:value-of select="$delimiter"/>
            </xsl:if>
        </xsl:for-each>

        <!-- Add a newline at the end of the record -->
        <xsl:text>&#xa;</xsl:text>
    </xsl:template>

</xsl:stylesheet>
