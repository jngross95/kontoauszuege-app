package com.example.kontoauszuege.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BankStatement {

    private Long id;
    private LocalDate buchungsdatum;
    private LocalDate valutadatum;
    private String auftraggeber;
    private String empfaenger;
    private String verwendungszweck;
    private String iban;
    private BigDecimal betrag;
    private String waehrung;
    private BigDecimal kontostand;

    public BankStatement() {}

    public BankStatement(Long id, LocalDate buchungsdatum, LocalDate valutadatum,
                         String auftraggeber, String empfaenger, String verwendungszweck,
                         String iban, BigDecimal betrag, String waehrung, BigDecimal kontostand) {
        this.id = id;
        this.buchungsdatum = buchungsdatum;
        this.valutadatum = valutadatum;
        this.auftraggeber = auftraggeber;
        this.empfaenger = empfaenger;
        this.verwendungszweck = verwendungszweck;
        this.iban = iban;
        this.betrag = betrag;
        this.waehrung = waehrung;
        this.kontostand = kontostand;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getBuchungsdatum() { return buchungsdatum; }
    public void setBuchungsdatum(LocalDate buchungsdatum) { this.buchungsdatum = buchungsdatum; }

    public LocalDate getValutadatum() { return valutadatum; }
    public void setValutadatum(LocalDate valutadatum) { this.valutadatum = valutadatum; }

    public String getAuftraggeber() { return auftraggeber; }
    public void setAuftraggeber(String auftraggeber) { this.auftraggeber = auftraggeber; }

    public String getEmpfaenger() { return empfaenger; }
    public void setEmpfaenger(String empfaenger) { this.empfaenger = empfaenger; }

    public String getVerwendungszweck() { return verwendungszweck; }
    public void setVerwendungszweck(String verwendungszweck) { this.verwendungszweck = verwendungszweck; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    public String getWaehrung() { return waehrung; }
    public void setWaehrung(String waehrung) { this.waehrung = waehrung; }

    public BigDecimal getKontostand() { return kontostand; }
    public void setKontostand(BigDecimal kontostand) { this.kontostand = kontostand; }
}
