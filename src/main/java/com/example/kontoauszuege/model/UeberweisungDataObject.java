package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

import java.math.BigDecimal;

public class UeberweisungDataObject extends DataObject {

    private boolean ausgewaehlt = false;
    private UeberweisungStatus status = UeberweisungStatus.NEW;
    private String sender;

    private String empfaenger;
    private String empfaengerIban;
    private String empfaengerBic;
    private String verwendungszweck;
    private BigDecimal betrag;

    public UeberweisungDataObject() {
    }

    public UeberweisungDataObject(String sender, String empfaenger, String empfaengerIban,
                                  String empfaengerBic, String verwendungszweck, BigDecimal betrag) {
        this();
        this.sender = sender;
        this.empfaenger = empfaenger;
        this.empfaengerIban = empfaengerIban;
        this.empfaengerBic = empfaengerBic;
        this.verwendungszweck = verwendungszweck;
        this.betrag = betrag;
    }

    public boolean isAusgewaehlt()                    { return ausgewaehlt; }
    public void    setAusgewaehlt(boolean ausgewaehlt) { this.ausgewaehlt = ausgewaehlt; }

    public UeberweisungStatus getStatus()                    { return status; }
    public void               setStatus(UeberweisungStatus status) { this.status = status; }

    public String getSender()                     { return sender; }
    public void   setSender(String sender)        { this.sender = sender; }

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
}
