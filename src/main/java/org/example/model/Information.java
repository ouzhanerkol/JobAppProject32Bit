package org.example.model;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Information {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;
    private String currencyCode;
    private int unit;
    private double informationUSD;
    private double informationTRY;

    public Information() {

    }

    public Information(Date createDate, String currencyCode, int unit, double informationUSD, double informationTRY) {
        this.createDate = createDate;
        this.currencyCode = currencyCode;
        this.unit = unit;
        this.informationUSD = informationUSD;
        this.informationTRY = informationTRY;
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

    public double getInformationUSD() {
        return informationUSD;
    }

    public void setInformationUSD(double informationRate) {
        this.informationUSD = informationRate;
    }

    public double getInformationTRY() {
        return informationTRY;
    }

    public void setInformationTRY(double informationTRY) {
        this.informationTRY = informationTRY;
    }

    @Override
    public String toString() {
        return "Information{" +
                "id=" + id +
                ", createDate=" + createDate +
                ", currencyCode='" + currencyCode + '\'' +
                ", unit=" + unit +
                ", informationRate=" + informationUSD +
                '}';
    }
}
