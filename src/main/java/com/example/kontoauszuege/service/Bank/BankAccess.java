package com.example.kontoauszuege.service.Bank;

import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.*;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


public class BankAccess implements AutoCloseable {
    BankInfo info;
    HBCIHandler handle;
    HBCIPassport passport;

    private  static Date startOfDay(Date date)
    {
        if (date == null)
            return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date == null ? new Date() : date);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTime();
    }

    void connect(BankContact contact) throws Exception {
        // Server-Adresse angeben. Koennen wir entweder manuell eintragen oder direkt von HBCI4Java ermitteln lassen
        var bi = HBCIUtils.searchBankInfo(contact.blz);
        if (bi.size() == 0) {
            throw new Exception(String.format("Keine BankInfo gefunden für die BLZ/BIC: %s", contact.blz));
        } else if (bi.size() > 1) {
            throw new Exception(String.format("mehrere  Banken zu '%s' gefunden", contact.blz));
        }

        info = bi.getFirst();
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");


        System.out.println(String.format("!connect:  name='%s' blz=%s user=%s", contact.name, contact.blz, contact.user));

        Properties props = new Properties();
        HBCIUtils.init(props, new MyHBCICallback(contact));

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe unten).
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.

        final File passportFile = new File(String.format("passport2-%s-%s.dat", contact.blz, contact.user));


        System.out.println(String.format("passport-file = %s", passportFile.getAbsolutePath()));

        // Wir setzen die Kernel-Parameter zur Laufzeit. Wir koennten sie alternativ
        // auch oben in "props" setzen.
        HBCIUtils.setParam("client.passport.default", "PinTan"); // Legt als Verfahren PIN/TAN fest.
        HBCIUtils.setParam("client.passport.PinTan.filename", passportFile.getAbsolutePath());
        HBCIUtils.setParam("client.passport.PinTan.init", "1");

        // Erzeugen des Passport-Objektes.
        passport = AbstractHBCIPassport.getInstance();

        // Konfigurieren des Passport-Objektes.
        // Das kann alternativ auch alles ueber den Callback unten geschehen

        // Das Land.
        passport.setCountry("DE");


        passport.setHost(info.getPinTanAddress());

        // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja ueber HTTPS laeuft.
        passport.setPort(443);

        // Art der Nachrichten-Codierung. Bei Chipkarte/Schluesseldatei wird
        // "None" verwendet. Bei PIN/TAN kommt "Base64" zum Einsatz.
        passport.setFilterType("Base64");

        // Das Handle ist die eigentliche HBCI-Verbindung zum Server

        // Verbindung zum Server aufbauen
        handle = new HBCIHandler(HBCIVersion.HBCI_300.getId(), passport);
    }

    /**
     * Gibt die belegten Ressourcen wieder frei. Sowohl das HBCI-Handle als auch
     * das Passport-Objekt werden geschlossen, sofern sie gesetzt sind.
     * Dadurch kann diese Klasse in einem try-with-resources verwendet werden.
     */
    @Override
    public void close() {
        if (handle != null) {
            try {
                handle.close();
            } catch (Exception ex) {
                System.out.println("Fehler beim Schliessen des HBCI-Handles: " + ex);
            } finally {
                handle = null;
            }
        }

        if (passport != null) {
            try {
                passport.close();
            } catch (Exception ex) {
                System.out.println("Fehler beim Schliessen des Passports: " + ex);
            } finally {
                passport = null;
            }
        }
    }

    List<BankAccount> getAccounts() throws Exception {
        Konto[] konten = passport.getAccounts();
        //passport.getBPD().getProperty("TAN2StepParams");
        return Arrays.stream(konten).map(x->new BankAccount(x.iban)).toList();
    }

    Konto getKonto(String iban) throws Exception {
        Konto[] konten = passport.getAccounts();

        passport.getBPD().getProperty("TAN2StepParams");

        if (konten == null || konten.length == 0) {
            throw new Exception("Keine Konten ermittelbar");
        }

        log("Anzahl Konten: " + konten.length);
        Konto k = null;

        for (int i = 0; i < konten.length; i++) {
            log(" Konto: " + konten[i].number);
            if(konten[i].iban.equals(iban))
            {
                k = konten[i];
            }
        }

        if(k == null)
        {
            throw new Exception(String.format("Konto nicht gefunden: %s" , iban));
        }
        return k;
    }

    List<KontoBuchung> UmsaetzeAbholen(
            String iban,
            Date startdate) throws Exception {
        var k = getKonto(iban);
/*
            // 1. Auftrag fuer das Abrufen des Saldos erzeugen
            HBCIJob saldoJob = handle.newJob("SaldoReq");
            saldoJob.setParam("my",k); // festlegen, welches Konto abgefragt werden soll.
            saldoJob.addToQueue(); // Zur Liste der auszufuehrenden Auftraege hinzufuegen
*/
        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen


        HBCIJob umsatzJob = handle.newJob("KUmsAllCamt");//Camt

        Date myDate = startdate;

        var saldoDatum = startOfDay(myDate);


        //Calendar myCalendar2 = new GregorianCalendar(2019, 9, 21);
        //Date myDate2 = myCalendar.getTime();

        //umsatzJob.setParam("enddate", myDate2);
        //---umsatzJob.setParam("my",k); // festlegen, welches Konto abgefragt werden soll.

        var bic = info.getBic();
        umsatzJob.setParam("my",  k);

        umsatzJob.setParam("my.bic",  bic);
        var kiban = HBCIUtils.getIBANForKonto(k);
        umsatzJob.setParam("my.iban", kiban);

        umsatzJob.setParam("startdate", saldoDatum);




        umsatzJob.addToQueue(); // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
        // werden. Z.Bsp. Ueberweisungen.

        // Alle Auftraege aus der Liste ausfuehren.
        HBCIExecStatus status = handle.execute();

        // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
        if (!status.isOK()) {
            throw new Exception(String.format("Fehler beim handle.execute: %s" , status.toString()));
        }
/*
            // Auswertung des Saldo-Abrufs.
            GVRSaldoReq saldoResult = (GVRSaldoReq) saldoJob.getJobResult();
            if (!saldoResult.isOK())
                error(saldoResult.toString());

            Value s = saldoResult.getEntries()[0].ready.value;
            log("Saldo: " + s.toString());
*/

        // Das Ergebnis des Jobs koennen wir auf "GVRKUms" casten. Jobs des Typs "KUmsAll"
        // liefern immer diesen Typ.
        GVRKUms result = (GVRKUms) umsatzJob.getJobResult();

        // Pruefen, ob der Abruf der Umsaetze geklappt hat
        if (!result.isOK()) {
            throw new Exception(String.format("Fehler beim umsatzJob.getJobResult: %s" , result.toString()));
        }

        var retmsg = result.getJobStatus().toString();
        System.out.println("result.getJobStatus: "+retmsg);

        // Alle Umsatzbuchungen ausgeben
        List<GVRKUms.UmsLine> buchungen = result.getFlatData();

        System.out.println("anzahl buchungen: "+buchungen.size());

        KontoBuchung[] kblist = new KontoBuchung[buchungen.size()];
        int i=0;
        for (GVRKUms.UmsLine buchung:buchungen)
        {
            var kb = new KontoBuchung();
            kblist[i] = kb;
            i++;

            kb.Betrag = buchung.value.getBigDecimalValue();

            //EUR!!


            kb.Buchungsdatum = buchung.bdate;

            if(buchung.other != null) {

                if(buchung.other.name != null) {
                    kb.Empfaenger = buchung.other.name;
                }

                if(buchung.other.bic != null) {
                    kb.EmpfaengerBLZ = buchung.other.bic;
                }

                if(buchung.other.iban != null) {
                    kb.EmpfaengerKontoNr = buchung.other.iban;
                }
            }
            if(buchung.text != null) {
                kb.Geschaeftsvorfall = buchung.text;
            }
            if(buchung.saldo != null && buchung.saldo.value!= null) {
                kb.Saldo = buchung.saldo.value.getBigDecimalValue();
            }
            kb.Wertstellungsdatum = buchung.valuta;

            StringBuilder sbusage = new StringBuilder();
            if(buchung.usage!=null) {
                for (String u : buchung.usage) {
                    sbusage.append(u);
                }
            }
            kb.Verwendungszweck = "";
            if(buchung.endToEndId != null && !buchung.endToEndId.equals("NOTPROVIDED")) {
                kb.Verwendungszweck = buchung.endToEndId;
                if (kb.Verwendungszweck.length() > 0) {
                    kb.Verwendungszweck += "\n";
                }
            }
            kb.Verwendungszweck +=  sbusage.toString();
        }

        System.out.println("------------- Kontoauszüge holen erfolgreich  --------------- ");

        return Arrays.stream(kblist).toList();
    }

    String  UeberweisungAusfuehren(
            String iban,

            String dstName,
            String dstBic,
            String dstIban,
            BigDecimal btgValue,
            String endToEndId,
            String usage) throws Exception {

        var src = getKonto(iban);

        HBCIJob umsatzJob =  handle.newJob("UebSEPA");

        var bic = info.getBic();
        //src.name = HBCIProperties.replace(src.name,HBCIProperties.TEXT_REPLACEMENTS_SEPA);

        umsatzJob.setParam("src",  src);

        umsatzJob.setParam("src.bic",  bic);
        //var iban = HBCIUtils.getIBANForKonto(src);
        umsatzJob.setParam("src.iban", iban);

        org.kapott.hbci.structures.Konto dst = new org.kapott.hbci.structures.Konto();

        if( !HBCIUtils.checkIBANCRC(dstIban))
        {
            throw new Exception(String.format("CRC Check fehlgeschlagen für die iban:'%s'. Empfänger Name: %s" ,
                    dstIban, dstName));
        }

        dst.iban = dstIban;
        dst.bic = dstBic;
        dst.name = dstName;
        umsatzJob.setParam("dst",dst);
        umsatzJob.setParam("btg",new Value(btgValue,"EUR"));

        if (usage != null && usage.length() > 0)
            umsatzJob.setParam("usage",usage);


        if (endToEndId != null && endToEndId.trim().length() > 0)
            umsatzJob.setParam("endtoendid",  endToEndId);


        umsatzJob.addToQueue(); // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        System.out.println("before handle.execute");

        HBCIExecStatus status = handle.execute();

        // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
        if (!status.isOK()) {
            throw new Exception(String.format("Fehler beim handle.execute: %s" , status.toString()));
        }

        var result =  umsatzJob.getJobResult();

        // Pruefen, ob der Abruf  geklappt hat
        if (!result.isOK()) {
            throw new Exception(String.format("Fehler beim umsatzJob.getJobResult: %s" , result.toString()));
        }

        var ret = result.getJobStatus().toString();
        System.out.println("--------- JobResult: --------\n"+ret);

        System.out.println("------ Kontoauszüge holen erfolgreich ------\n");
        return ret;
    }


    //----------------------------
    /**
     * Gibt die angegebene Meldung aus.
     * @param msg die Meldung.
     */
    private static void log(String msg)
    {
        System.out.println(msg);
    }

    /**
     * Beendet das Programm mit der angegebenen Fehler-Meldung.
     * @param msg die Meldung.
     */
    private static void error(String msg)
    {
        System.err.println(msg);
        System.exit(1);
    }

    private static class MyHBCICallback extends AbstractHBCICallback
    {
        BankContact contact;
        MyHBCICallback(BankContact c)
        {
            this.contact = c;
        }
        /**
         * @see org.kapott.hbci.callback.HBCICallback#log(String, int, Date, StackTraceElement)
         */
        @Override
        public void log(String msg, int level, Date date, StackTraceElement trace)
        {
            // Ausgabe von Log-Meldungen bei Bedarf
            System.out.println(msg);
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#callback(org.kapott.hbci.passport.HBCIPassport, int, String, int, StringBuffer)
         */
        @Override
        public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData)
        {
            System.out.println(String.format("callback: reason=%d msg='%s' retData='%s'",reason,msg, retData.toString()));

            // Diese Funktion ist wichtig. Ueber die fragt HBCI4Java die benoetigten Daten von uns ab.
            switch (reason)
            {
                // Mit dem Passwort verschluesselt HBCI4Java die Passport-Datei.
                // Wir nehmen hier der Einfachheit halber direkt die PIN. In der Praxis
                // sollte hier aber ein staerkeres Passwort genutzt werden.
                // Die Ergebnis-Daten muessen in dem StringBuffer "retData" platziert werden.
                case NEED_PASSPHRASE_LOAD:
                case NEED_PASSPHRASE_SAVE:
                    retData.replace(0,retData.length(),contact.bankPin);
                    break;

                // PIN wird benoetigt
                case NEED_PT_PIN:
                    retData.replace(0,retData.length(),contact.bankPin);
                    break;

                // BLZ wird benoetigt
                case NEED_BLZ:
                    retData.replace(0,retData.length(),contact.blz);
                    break;

                // Die Benutzerkennung
                case NEED_USERID:
                    retData.replace(0,retData.length(),contact.user);
                    break;

                // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
                // Bei manchen Banken kann man die auch leer lassen
                case NEED_CUSTOMERID:
                    retData.replace(0,retData.length(),contact.user);
                    break;

                ////////////////////////////////////////////////////////////////////////
                // Die folgenden Callbacks sind nur fuer die Ausfuehrung TAN-pflichtiger
                // Geschaeftsvorfaelle bei der Verwendung des PIN/TAN-Verfahrens noetig.
                // Z.Bsp. beim Versand einer Ueberweisung
                // "NEED_PT_SECMECH" kann jedoch auch bereits vorher auftreten.

                // HBCI4Java benoetigt die TAN per PhotoTAN-Verfahren
                // Liefert die anzuzeigende PhotoTAN-Grafik, die mit der entsprechenden
                // Smartphone-App der Bank fotografiert werden muss, um die TAN
                // zu generieren. Eine Implementierung muss diese Grafik anzeigen
                // sowie ein Eingabefeld fuer die TAN. Der Callback muss dann die vom
                // User eingegebene TAN zurueckliefern (nachdem dieser die Grafik
                // fotografiert und die App ihm die TAN angezeigt hat)
                case NEED_PT_PHOTOTAN:
                    // Die Klasse "MatrixCode" kann zum Parsen der Daten verwendet werden
                    try
                    {
                        MatrixCode code = new MatrixCode(retData.toString());

                        // Liefert den Mime-Type der grafik (i.d.R. "image/png").
                        String type = code.getMimetype();
                        System.out.println("Mimetype: "+type);
                        // Der Stream enthaelt jetzt die Binaer-Daten des Bildes
                        byte[] image = code.getImage();
                        // InputStream stream = new ByteArrayInputStream();
                        var dlg = new Dlg(contact.name,"TAN: ", msg, image);
                        String tan = dlg.tan;
                        // .... Hier Dialog mit der Grafik anzeigen und User-Eingabe der TAN
                        // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
                        // den bankspezifischen Text mit den Instruktionen fuer den User.
                        // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
                        // werden.

                        retData.replace(0,retData.length(),tan);
                    }
                    catch (Exception e)
                    {
                        throw new HBCI_Exception(e);
                    }

                    break;

                case NEED_PT_QRTAN:
                    // Die Klasse "QRCode" kann zum Parsen der Daten verwendet werden
                    try
                    {
                        QRCode code = new QRCode(retData.toString(),msg);
                        String type = code.getMimetype();
                        System.out.println("Mimetype: "+type);
                        byte[] image = code.getImage();


                        var dlg = new Dlg(contact.name, "TAN:", msg, image);
                        String tan = dlg.tan;
                        // Der Stream enthaelt jetzt die Binaer-Daten des Bildes
                        // InputStream stream = new ByteArrayInputStream(code.getImage());

                        // .... Hier Dialog mit der Grafik anzeigen und User-Eingabe der TAN
                        // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
                        // den bankspezifischen Text mit den Instruktionen fuer den User.
                        // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
                        // werden. Da Sparkassen den eigentlichen Bild u.U. auch in msg verpacken,
                        // sollte zur Anzeige nicht der originale Text verwendet werden sondern
                        // der von QRCode - dort ist dann die ggf. enthaltene Base64-codierte QR-Grafik entfernt
                        // msg = code.getMessage();

                        retData.replace(0,retData.length(),tan);
                    }
                    catch (Exception e)
                    {
                        throw new HBCI_Exception(e);
                    }

                    break;

                // HBCI4Java benoetigt den Code des verwendenden TAN-Verfahren (smsTAN,
                // chipTAN optisch, photoTAN,...)
                // I.d.R. ist das eine dreistellige mit "9" beginnende Ziffer
                case NEED_PT_SECMECH:

                    // Als Parameter werden die verfuegbaren TAN-Verfahren uebergeben.
                    // Der Aufbau des String ist wie folgt:
                    // <code1>:<name1>|<code2>:<name2>|...
                    // Bsp:
                    // 911:smsTAN|920:chipTAN optisch|955:photoTAN
                    // String options = retData.toString();

                    // Der Callback muss den Code des zu verwendenden TAN-Verfahrens
                    // zurueckliefern
                    // In "code" muss der 3-stellige Code des vom User gemaess obigen
                    // Optionen ausgewaehlte Verfahren eingetragen werden

                    try {
                        var dlg = new Dlg(contact.name, "Medium Nr: ",
                                String.format("'%s'\n\nWerte:  '%s'", msg,retData.toString()), null);

                        String code = dlg.tan;
                        retData.replace(0, retData.length(), code);
                    }
                    catch(Exception e)
                    {
                        throw new HBCI_Exception(e);
                    }
                    break;

                // HBCI4Java benoetigt die TAN per smsTAN/chipTAN/weiteren TAN-Verfahren
                case NEED_PT_TAN:

                    // Wenn per "retData" Daten uebergeben wurden, dann enthalten diese
                    // den fuer chipTAN optisch zu verwendenden Flickercode.
                    // Falls nicht, ist es eine TAN-Abfrage, fuer die keine weiteren
                    // Parameter benoetigt werden (z.Bsp. beim smsTAN-Verfahren)

                    // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
                    // den bankspezifischen Text mit den Instruktionen fuer den User.
                    // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
                    // werden.

                    String flicker = retData.toString();
                    if (flicker != null && flicker.length() > 0)
                    {
                        // Ist chipTAN optisch. Es muss ein animierter Barcode angezeigt
                        // werden. Hierfuer kann die Hilfsklasse "FlickerRenderer" verwendet
                        // werden. Diese enthalt bereits das Parsen. Es muss lediglich die
                        // Methode "paint" ueberschrieben werden.
                        // FlickerRenderer renderer = new FlickerRenderer(flicker);

                        // Hier TAN-Abfrage mit dem animierten Barcode anzeigen sowie
                        // Eingabefeld fuer die TAN
                        String tan = null;
                        retData.replace(0,retData.length(),tan);
                    }
                    else
                    {
                        // Ist smsTAN, iTAN, o.ae.
                        // Dialog zur TAN-Eingabe anzeigen mit dem Text aus "msg".

                        try {
                            var dlg = new Dlg(contact.name, "TAN:", msg, null);
                            String tan = dlg.tan;
                            retData.replace(0, retData.length(), tan);
                        }
                        catch(Exception e)
                        {
                            throw new HBCI_Exception(e);
                        }
                    }

                    break;

                // Beim Verfahren smsTAN ist es moeglich, mehrere Handynummern mit
                // Aliasnamen bei der Bank zu hinterlegen. Auch wenn nur eine Handy-
                // Nummer bei der Bank hinterlegt ist, kann es durchaus passieren,
                // dass die Bank dennoch die Aufforderung zur Auswahl des TAN-Mediums
                // sendet.
                case NEED_PT_TANMEDIA:

                    // Als Parameter werden die verfuegbaren TAN-Medien uebergeben.
                    // Der Aufbau des String ist wie folgt:
                    // <name1>|<name2>|...
                    // Bsp:
                    // Privathandy|Firmenhandy
                    // String options = retData.toString();

                    // Der Callback muss den vom User ausgewaehlten Aliasnamen
                    // zurueckliefern. Falls "options" kein "|" enthaelt, ist davon
                    // auszugehen, dass nur eine moegliche Option existiert. In dem
                    // Fall ist keine Auswahl noetig und "retData" kann unveraendert
                    // bleiben
                    try {
                        /*
                        var dlg = new Base.Dlg(contact.name, "TanMedium: ",
                                String.format("'%s'\n\nWerte:  '%s'", msg,retData.toString()), null);

                        String code = dlg.tan;
                        */

                        String code = "";
                        retData.replace(0, retData.length(), code);
                    }
                    catch(Exception e)
                    {
                        throw new HBCI_Exception(e);
                    }

                    break;
                //
                ////////////////////////////////////////////////////////////////////////

                // Manche Fehlermeldungen werden hier ausgegeben
                case HAVE_ERROR:
                    BankAccess.log(msg);
                    break;
                case NEED_COUNTRY:
                    retData.replace(0,retData.length(),"DE");
                case NEED_FILTER:
                    retData.replace(0,retData.length(),"Base64");
                default:

                    // Wir brauchen nicht alle der Callbacks
                    break;

            }
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#status(org.kapott.hbci.passport.HBCIPassport, int, Object[])
         */
        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o)
        {
            StringBuilder sb = new StringBuilder();

            if(o!= null) {
                for (Object x : o) {
                    sb.append(x.toString());
                }
            }
            System.out.println(String.format("statusTag=%d  o=%s",statusTag, sb.toString()   ));
        }

    }
}
