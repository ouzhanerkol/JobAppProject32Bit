<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:template match="/">
        <Data>
            <xsl:attribute name="Date">
                <xsl:value-of select="Tarih_Date/@Date"/>
            </xsl:attribute>
            <Forex>
                <xsl:for-each select="Tarih_Date/Currency">
                    <xsl:sort select="@CurrencyCode"/>
                    <Currency Pair="{@CurrencyCode}/TRY">
                        <xsl:attribute name="Unit">
                            <xsl:value-of select="Unit"/>
                        </xsl:attribute>
                        <xsl:attribute name="Buy">
                            <xsl:value-of select="ForexBuying"/>
                        </xsl:attribute>
                        <xsl:attribute name="Sell">
                            <xsl:value-of select="ForexSelling"/>
                        </xsl:attribute>
                    </Currency>
                </xsl:for-each>
            </Forex>
            <Banknote>
                <xsl:for-each select="Tarih_Date/Currency">
                    <xsl:choose>
                        <xsl:when test="not(BanknoteBuying = '')">
                            <Currency Pair="{@CurrencyCode}/TRY">
                                <xsl:attribute name="Unit">
                                    <xsl:value-of select="Unit"/>
                                </xsl:attribute>
                                <xsl:attribute name="Buy">
                                    <xsl:value-of select="BanknoteBuying"/>
                                </xsl:attribute>
                                <xsl:attribute name="Sell">
                                    <xsl:value-of select="BanknoteSelling"/>
                                </xsl:attribute>
                            </Currency>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Banknote>
            <Cross>
                <xsl:for-each select="Tarih_Date/Currency">
                    <xsl:choose>
                        <xsl:when test="@CrossOrder != 0">
                            <xsl:choose>
                                <xsl:when test="CrossRateUSD = ''">
                                    <Currency Pair="{@CurrencyCode}/USD">
                                        <xsl:attribute name="CrossOrder">
                                            <xsl:value-of select="@CrossOrder"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="Unit">
                                            <xsl:value-of select="Unit"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="Rate">
                                            <xsl:value-of select="CrossRateOther"/>
                                        </xsl:attribute>
                                    </Currency>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Currency Pair="USD/{@CurrencyCode}">
                                        <xsl:attribute name="CrossOrder">
                                            <xsl:value-of select="@CrossOrder"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="Unit">
                                            <xsl:value-of select="Unit"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="Rate">
                                            <xsl:value-of select="CrossRateUSD"/>
                                        </xsl:attribute>
                                    </Currency>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Cross>
            <Information>
                <xsl:for-each select="Tarih_Date/Currency">
                    <xsl:choose>
                        <xsl:when test="position()=last()">
                            <Currency Pair="SDR/USD">
                                <xsl:attribute name="Unit">
                                    <xsl:value-of select="Unit"/>
                                </xsl:attribute>
                                <xsl:attribute name="Rate">
                                    <xsl:value-of select="CrossRateOther"/>
                                </xsl:attribute>
                            </Currency>
                            <Currency Pair="SDR/TRY">
                                <xsl:attribute name="Unit">
                                    <xsl:value-of select="Unit"/>
                                </xsl:attribute>
                                <xsl:attribute name="Rate">
                                    <xsl:value-of select="ForexBuying"/>
                                </xsl:attribute>
                            </Currency>
                        </xsl:when>
                    </xsl:choose>
                </xsl:for-each>
            </Information>
        </Data>
    </xsl:template>
</xsl:stylesheet>