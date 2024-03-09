package org.example.entities;

import com.sun.istack.Nullable;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Banknote {
    @Id
    private int id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    private String currencyCode;

    private int unit;

    private double banknoteBuying;

    private double banknoteSelling;

    public Banknote() {
    }

    public Banknote(int id, Date createDate, String currencyCode, int unit, double banknoteBuying, double banknoteSelling) {
        this.id = id;
        this.createDate = createDate;
        this.currencyCode = currencyCode;
        this.unit = unit;
        this.banknoteBuying = banknoteBuying;
        this.banknoteSelling = banknoteSelling;
    }

    public Banknote(int id, Date createDate, String currencyCode, int unit) {
        this.id = id;
        this.createDate = createDate;
        this.currencyCode = currencyCode;
        this.unit = unit;
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

    public double getBanknoteBuying() {
        return banknoteBuying;
    }

    public void setBanknoteBuying(double banknoteBuying) {
        this.banknoteBuying = banknoteBuying;
    }

    public double getBanknoteSelling() {
        return banknoteSelling;
    }

    public void setBanknoteSelling(double banknoteSelling) {
        this.banknoteSelling = banknoteSelling;
    }

    @Override
    public String toString() {
        return "Banknote{" +
                "id=" + id +
                ", createDate=" + createDate +
                ", currencyCode='" + currencyCode + '\'' +
                ", unit=" + unit +
                ", banknoteBuying=" + banknoteBuying +
                ", banknoteSelling=" + banknoteSelling +
                '}';
    }
}
