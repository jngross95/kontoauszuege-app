package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

import java.math.BigDecimal;
import java.util.Date;

public class BankStatementDataObject extends DataObject {

    private String iban;

    private Date buchungsdatum = new Date();
    private Date wertstellungsdatum = new Date();
    private String geschaeftsvorfall = "";
    private String empfaenger = "";
    private String empfaengerKontoNr = "";
    private String empfaengerBLZ = "";
    private BigDecimal betrag = new BigDecimal(0);
    private String verwendungszweck = "";
    private BigDecimal saldo = new BigDecimal(0);

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Date getBuchungsdatum() {
        return buchungsdatum;
    }

    public void setBuchungsdatum(Date buchungsdatum) {
        this.buchungsdatum = buchungsdatum;
    }

    public Date getWertstellungsdatum() {
        return wertstellungsdatum;
    }

    public void setWertstellungsdatum(Date wertstellungsdatum) {
        this.wertstellungsdatum = wertstellungsdatum;
    }

    public String getGeschaeftsvorfall() {
        return geschaeftsvorfall;
    }

    public void setGeschaeftsvorfall(String geschaeftsvorfall) {
        this.geschaeftsvorfall = geschaeftsvorfall;
    }

    public String getEmpfaenger() {
        return empfaenger;
    }

    public void setEmpfaenger(String empfaenger) {
        this.empfaenger = empfaenger;
    }

    public String getEmpfaengerKontoNr() {
        return empfaengerKontoNr;
    }

    public void setEmpfaengerKontoNr(String empfaengerKontoNr) {
        this.empfaengerKontoNr = empfaengerKontoNr;
    }

    public String getEmpfaengerBLZ() {
        return empfaengerBLZ;
    }

    public void setEmpfaengerBLZ(String empfaengerBLZ) {
        this.empfaengerBLZ = empfaengerBLZ;
    }

    public BigDecimal getBetrag() {
        return betrag;
    }

    public void setBetrag(BigDecimal betrag) {
        this.betrag = betrag;
    }

    public String getVerwendungszweck() {
        return verwendungszweck;
    }

    public void setVerwendungszweck(String verwendungszweck) {
        this.verwendungszweck = verwendungszweck;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
}
