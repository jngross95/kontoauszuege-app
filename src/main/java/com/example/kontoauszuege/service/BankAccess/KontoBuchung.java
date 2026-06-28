package com.example.kontoauszuege.service.BankAccess;

import java.math.BigDecimal;
import java.util.Date;

public class KontoBuchung {
    public Date Buchungsdatum=new Date();
    public Date Wertstellungsdatum=new Date();
    public String Geschaeftsvorfall ="";
    public String Empfaenger="";
    public String EmpfaengerKontoNr="";
    public String EmpfaengerBLZ="";
    public BigDecimal Betrag = new BigDecimal(0);
    public String Verwendungszweck="";
    public BigDecimal Saldo= new BigDecimal(0);
}