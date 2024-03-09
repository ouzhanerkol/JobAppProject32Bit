package org.example.entities;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Forex {
    @Id
    private int id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    private String currencyCode;

    private int unit;

    private double forexBuying;

    private double forexSelling;

    public Forex() {
    }

    public Forex(int id, Date createDate, String currencyCode, int unit, double forexBuying, double forexSelling) {
        this.id = id;
        this.createDate = createDate;
        this.currencyCode = currencyCode;
        this.unit = unit;
        this.forexBuying = forexBuying;
        this.forexSelling = forexSelling;
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

    public double getForexBuying() {
        return forexBuying;
    }

    public void setForexBuying(double forexBuying) {
        this.forexBuying = forexBuying;
    }

    public double getForexSelling() {
        return forexSelling;
    }

    public void setForexSelling(double forexSelling) {
        this.forexSelling = forexSelling;
    }

    @Override
    public String toString() {
        return "Forex{" +
                "id=" + id +
                ", createDate=" + createDate +
                ", currencyCode='" + currencyCode + '\'' +
                ", unit=" + unit +
                ", forexBuying=" + forexBuying +
                ", forexSelling=" + forexSelling +
                '}';
    }
}
