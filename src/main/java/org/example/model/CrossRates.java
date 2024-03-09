package org.example.model;

import javax.persistence.*;
import java.util.Date;

@Entity
public class CrossRates {
    @Id
    private int id;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;
    private String currencyCode;
    private int unit;
    private double crossRate;
    public CrossRates() {
    }

    public CrossRates(int id, Date createDate, String currencyCode, int unit, double crossRate) {
        this.id = id;
        this.createDate = createDate;
        this.currencyCode = currencyCode;
        this.unit = unit;
        this.crossRate = crossRate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public double getCrossRate() {
        return crossRate;
    }

    public void setCrossRate(double crossRate) {
        this.crossRate = crossRate;
    }


}
