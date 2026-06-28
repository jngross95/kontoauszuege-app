package com.example.kontoauszuege.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Ueberweisung {

    private final String id;
    private boolean ausgewaehlt = true;
    private String sender;
    private String empfaenger;
    private String empfaengerIban;
    private String verwendungszweck;
    private BigDecimal betrag;

    public Ueberweisung() {
        this.id = UUID.randomUUID().toString();
    }

    public Ueberweisung(String sender, String empfaenger, String empfaengerIban,
                        String verwendungszweck, BigDecimal betrag) {
        this();
        this.sender = sender;
        this.empfaenger = empfaenger;
        this.empfaengerIban = empfaengerIban;
        this.verwendungszweck = verwendungszweck;
        this.betrag = betrag;
    }

    public String getId()                         { return id; }

    public boolean isAusgewaehlt()                    { return ausgewaehlt; }
    public void    setAusgewaehlt(boolean ausgewaehlt) { this.ausgewaehlt = ausgewaehlt; }

    public String getSender()                     { return sender; }
    public void   setSender(String sender)        { this.sender = sender; }

    public String getEmpfaenger()                        { return empfaenger; }
    public void   setEmpfaenger(String empfaenger)       { this.empfaenger = empfaenger; }

    public String getEmpfaengerIban()                        { return empfaengerIban; }
    public void   setEmpfaengerIban(String empfaengerIban)   { this.empfaengerIban = empfaengerIban; }

    public String getVerwendungszweck()                           { return verwendungszweck; }
    public void   setVerwendungszweck(String verwendungszweck)    { this.verwendungszweck = verwendungszweck; }

    public BigDecimal getBetrag()                    { return betrag; }
    public void       setBetrag(BigDecimal betrag)   { this.betrag = betrag; }
}
