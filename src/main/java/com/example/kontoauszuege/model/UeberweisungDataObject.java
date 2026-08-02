package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

import java.math.BigDecimal;

public class UeberweisungDataObject extends DataObject {

    private UeberweisungStatus status = UeberweisungStatus.NEW;
    private String senderIban;

    private String empfaenger;
    private String empfaengerIban;
    private String empfaengerBic;
    private String verwendungszweck;
    private BigDecimal betrag;
    private boolean instantPayment = false;

    public UeberweisungDataObject() {
    }

    public UeberweisungDataObject(String senderIban, String empfaenger, String empfaengerIban,
                                  String empfaengerBic, String verwendungszweck, BigDecimal betrag) {
        this();
        this.senderIban = senderIban;
        this.empfaenger = empfaenger;
        this.empfaengerIban = empfaengerIban;
        this.empfaengerBic = empfaengerBic;
        this.verwendungszweck = verwendungszweck;
        this.betrag = betrag;
    }

    public UeberweisungStatus getStatus()                    { return status; }
    public void               setStatus(UeberweisungStatus status) { this.status = status; }

    public String getSenderIban()                        { return senderIban; }
    public void   setSenderIban(String senderIban)        { this.senderIban = senderIban; }

    public String getEmpfaenger()                        { return empfaenger; }
    public void   setEmpfaenger(String empfaenger)       { this.empfaenger = empfaenger; }

    public String getEmpfaengerIban()                        { return empfaengerIban; }
    public void   setEmpfaengerIban(String empfaengerIban)   { this.empfaengerIban = empfaengerIban; }

    public String getEmpfaengerBic()                        { return empfaengerBic; }
    public void   setEmpfaengerBic(String empfaengerBic)    { this.empfaengerBic = empfaengerBic; }

    public String getVerwendungszweck()                           { return verwendungszweck; }
    public void   setVerwendungszweck(String verwendungszweck)    { this.verwendungszweck = verwendungszweck; }

    public BigDecimal getBetrag()                    { return betrag; }
    public void       setBetrag(BigDecimal betrag)   { this.betrag = betrag; }

    public boolean isInstantPayment() { return instantPayment; }
    public void setInstantPayment(boolean instantPayment) { this.instantPayment = instantPayment; }
}
