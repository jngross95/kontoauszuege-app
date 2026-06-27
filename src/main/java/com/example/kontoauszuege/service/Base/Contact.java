package com.example.kontoauszuege.service.Base;

import java.math.BigDecimal;
import java.util.Date;

public class Contact {
    public String op;   //Kl(Kontenliste)   Ums (Umsätze abholen)  Ueb (Ueberweisung ausführen)

    public String name; //Nur zu Logzwecken Source KontoName
    public String blz;
    public String user;
    public String bankPin;
    public String passportPin;

    //--- Ums+Ueb
    public String iban;   //Achtung: keine IBAN!! das was KL liefert!


    //----Ums

    public Date   startdate;


    //--- Ueb
    public String dstName;
    public String dstBic;
    public String dstIban;
    public BigDecimal btgValue;
    public String endToEndId;
    public String usage;
}
