package com.example.kontoauszuege.service.BankAccess;

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
import org.springframework.util.Assert;

import java.io.File;
import java.math.BigDecimal;
// SimpleDateFormat removed (unused)
import java.time.LocalDate;
import java.time.ZoneId;
// unused imports removed
import java.util.*;


public class BankConnection implements AutoCloseable {
    BankInfo info;
    HBCIHandler handle;
    HBCIPassport passport;
    DlgCallback dlgCallback;
    // --- Zugangsdaten (zuvor in BankContact) ---
    final String name;       // Nur zu Logzwecken
    final String bic;
    final String user;
    final String bankPin;

    public BankConnection(String name, String bic, String user, String bankPin, DlgCallback callback) {
        this.name = name;
        this.bic = bic;
        this.user = user;
        this.bankPin = bankPin;
        this.dlgCallback = callback;
    }

    private boolean isTestBank(){
        return "0".equals(bic);
    }

    /**
     * Ermittelt die BIC zu einer (deutschen) IBAN anhand der in HBCI4Java
     * hinterlegten Bankenliste. Eine Initialisierung von HBCI4Java ist dafür
     * nicht erforderlich.
     *
     * @param iban die IBAN, mit oder ohne Leerzeichen
     * @return die ermittelte BIC oder {@code null}, wenn keine bestimmt werden kann
     */
    public static String bicAusIban(String iban) {
        if (iban == null) {
            return null;
        }
        String norm = iban.trim().replace(" ", "").toUpperCase(Locale.ROOT);
        // BLZ-Extraktion und Bankenliste gelten nur für deutsche IBANs.
        if (!norm.startsWith("DE") || norm.length() < 12) {
            return null;
        }


        String blz = norm.substring(4, 12);
        BankInfo info = HBCIUtils.getBankInfo(blz);
        if (info == null) {
            return null;
        }
        return info.getBic();
    }

    /**
     * Liefert Bank-Informationen (BIC, Name, BLZ) zur angegebenen BIC.
     * Wirft eine Exception, wenn nicht genau ein Eintrag gefunden wurde.
     */
    public static BankInformation GetBankInfos(String bicOrBlz) throws Exception {
        if (bicOrBlz == null) throw new IllegalArgumentException("bicOrBlz darf nicht null sein");
        var list = HBCIUtils.searchBankInfo(bicOrBlz);
        if (list == null) throw new Exception("Keine Bankliste verfügbar (searchBankInfo returned null)");
        if (list.size() != 1) {
            throw new Exception(String.format("Erwartet genau einen Treffer für BIC '%s', gefunden: %d", bicOrBlz, list.size()));
        }
        BankInfo info = list.getFirst();
        if (info == null) throw new Exception("BankInfo ist null");
        return new BankInformation(info.getBic(), info.getName(), info.getBlz());
    }

    private  static Date startOfDay(Date date)
    {
        Date past = Date.from(
                LocalDate.of(2024, 1, 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );

        Calendar cal = Calendar.getInstance();
        cal.setTime(date == null ? past : date);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTime();
    }


    private static BankConnection currentConnection = null;

    public static void init() {
        Properties props = new Properties();
        // Registrierung/Produktkennung für HBCI4Java setzen
        //props.setProperty("client.product.name", "2955BDAE0301F8FFB6C78C101");
        // Registrierung/Produktkennung für HBCI4Java setzen
       // props.setProperty("client.product.name", "2955BDAE0301F8FFB6C78C101");
        HBCIUtils.init(props, new MyHBCICallback());
    }

    public void connect() throws Exception {
        if(isTestBank())
            return;

        if(currentConnection != null)
            throw new Exception("Parallele Bank-Connections sind nicht erlaubt.");
        currentConnection = this;

        // Server-Adresse angeben. Koennen wir entweder manuell eintragen oder direkt von HBCI4Java ermitteln lassen
        var bi = HBCIUtils.searchBankInfo(bic);
        if (bi.size() == 0) {
            throw new Exception(String.format("Keine BankInfo gefunden für die BIC: %s", bic));
        } else if (bi.size() > 1) {
            throw new Exception(String.format("mehrere  Banken zu '%s' gefunden", bic));
        }

        info = bi.getFirst();
        // assert: info.bic == bic (BIC der ermittelten Bank stimmt mit übergebenem Wert überein)
        Assert.state(Objects.equals(info.getBic(), bic),
                "info.bic == bic: BIC der ermittelten Bank ('" + info.getBic()
                        + "') stimmt nicht mit der übergebenen BIC ('" + bic + "') überein");
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        // SimpleDateFormat not used here, remove to avoid unused-variable warning


        System.out.println(String.format("!connect:  name='%s' blz=%s user=%s", name, bic, user));

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe unten).
        // Standardmäßig im Benutzerverzeichnis unter ~/.jbanking ablegen.

        final File jbankingDir = new File(System.getProperty("user.home"), ".jbanking");
        if (!jbankingDir.exists()) {
            boolean ok = jbankingDir.mkdirs();
            if (!ok) {
                System.out.println("Warnung: Konnte Verzeichnis ~/.jbanking nicht anlegen: " + jbankingDir.getAbsolutePath());
            }
        }

        final File passportFile = new File(jbankingDir, String.format("passport2-%s-%s.dat", bic, user));

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
        currentConnection = null;
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

    public List<BankAccount> getAccounts() throws Exception {
        if(isTestBank()) {
            return List.of(new BankAccount("iban1", "0"), new BankAccount("iban2", "0"));
        }
        Konto[] konten = passport.getAccounts();
        //passport.getBPD().getProperty("TAN2StepParams");
        return Arrays.stream(konten).map(x->new BankAccount(x.iban, x.bic)).toList();
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
            if(Objects.equals(konten[i].iban, iban))
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

    public List<KontoBuchung> UmsaetzeAbholen(
            String iban,
            Date startdate) throws Exception {
        if (isTestBank()) {
            return getTestUmsaetze(iban);
        }

        var k = getKonto(iban);
/*
            // 1. Auftrag fuer das Abrufen des Saldos erzeugen
            HBCIJob saldoJob = handle.newJob("SaldoReq");
            saldoJob.setParam("my",k); // festlegen, welches Konto abgefragt werden soll.
            saldoJob.addToQueue(); // Zur Liste der auszufuehrenden Auftraege hinzufuegen
*/
        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen


        HBCIJob<?> umsatzJob = handle.newJob("KUmsAllCamt");//Camt

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

    /**
     * Liefert Test-Umsätze für die Test-Bank (BIC "0").
        * Liefert je nach Konto
     * unterschiedliche Buchungen für die Test-IBANs "iban1" und "iban2".
     */
    private List<KontoBuchung> getTestUmsaetze(String iban) {
        List<KontoBuchung> umsaetze = new ArrayList<>();

        if ("IBAN1".equals(iban)) {
            umsaetze.add(testBuchung(
                    LocalDate.of(2026, 5, 1), "Stadtwerke Musterstadt",
                    "DE89 3704 0044 0532 0130 00",
                    "Stromrechnung Mai 2026\nKundennr. 123456",
                    new BigDecimal("-87.50"), new BigDecimal("2312.45")));

            umsaetze.add(testBuchung(
                    LocalDate.of(2026, 5, 2), "Arbeitgeber GmbH",
                    "DE27 2007 0000 0532 0130 00",
                    "Gehalt April 2026",
                    new BigDecimal("2850.00"), new BigDecimal("5162.45")));

            umsaetze.add(testBuchung(
                    LocalDate.of(2026, 5, 5), "REWE Supermarkt",
                    "DE12 5001 0517 0648 4898 90",
                    "Einkauf REWE 05.05.2026",
                    new BigDecimal("-63.20"), new BigDecimal("5099.25")));
        } else if ("IBAN2".equals(iban)) {
            umsaetze.add(testBuchung(
                    LocalDate.of(2026, 5, 8), "Vodafone GmbH",
                    "DE46 2007 0000 0660 7370 00",
                    "Mobilfunk Vertragsnummer 987654 Mai 2026",
                    new BigDecimal("-35.99"), new BigDecimal("1240.10")));

            umsaetze.add(testBuchung(
                    LocalDate.of(2026, 5, 12), "Finanzamt München",
                    "DE91 7000 0000 0012 3456 78",
                    "Steuererstattung 2025 St-Nr. 133/815/08155",
                    new BigDecimal("540.00"), new BigDecimal("1780.10")));

            umsaetze.add(testBuchung(
                    LocalDate.of(2026, 5, 15), "Vermieter Hans Huber",
                    "DE02 7001 0080 0619 3400 02",
                    "Miete Juni 2026 Wohnung Hauptstr. 1",
                    new BigDecimal("-950.00"), new BigDecimal("830.10")));
        }

        return umsaetze;
    }

    /**
     * Erzeugt eine einzelne Test-Buchung (KontoBuchung) aus den übergebenen Werten.
     */
    private static KontoBuchung testBuchung(LocalDate datum, String empfaenger,
                                            String empfaengerKontoNr, String verwendungszweck,
                                            BigDecimal betrag, BigDecimal saldo) {
        var kb = new KontoBuchung();
        Date d = toDate(datum);
        kb.Buchungsdatum = d;
        kb.Wertstellungsdatum = d;
        kb.Empfaenger = empfaenger;
        kb.EmpfaengerKontoNr = empfaengerKontoNr;
        kb.Verwendungszweck = verwendungszweck;
        kb.Betrag = betrag;
        kb.Saldo = saldo;
        return kb;
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public String  UeberweisungAusfuehren(
            String iban,

            String dstName,
            String dstBic,
            String dstIban,
            BigDecimal btgValue,
            String endToEndId,
            String usage) throws Exception {

        if(isTestBank())
            return "ok";

        var src = getKonto(iban);

        HBCIJob<?> umsatzJob =  handle.newJob("UebSEPA");

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
    @SuppressWarnings("unused")
    private static void error(String msg)
    {
        System.err.println(msg);
        System.exit(1);
    }

    private static class MyHBCICallback extends AbstractHBCICallback
    {
        MyHBCICallback()
        {
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
            var currentConnection= BankConnection.currentConnection;

            System.out.println(String.format("callback: reason=%d msg='%s' retData='%s'",reason,msg, retData.toString()));

            // Diese Funktion ist wichtig. Ueber die fragt HBCI4Java die benoetigten Daten von uns ab.
            switch (reason)
            {
                //callback: reason=37 msg='Ohne die Beachtung des Ergebnisses der Empfängerüberprüfung könnte der Betrag auf ein Konto gelangen, dessen Inhaber nicht dem eingegebenen Empfänger entspricht. In diesem Fall besteht kein Erstattungsanspruch gegen uns; eine Haftung der beteiligten Zahlungsdienstleister ist ausgeschlossen.' retData=''
                case HAVE_VOP_RESULT:

                    var rr = currentConnection.dlgCallback.dlg(currentConnection.name, "Empfängerüberprüfung (true=ja, false=nein)", msg, null);
                        // Bei Ok einfach "true" zurückgeben, bei Abbrechen wird Exception geworfen
                        retData.replace(0, retData.length(), rr);

                    break;
                // Mit dem Passwort verschluesselt HBCI4Java die Passport-Datei.
                // Wir nehmen hier der Einfachheit halber direkt die PIN. In der Praxis
                // sollte hier aber ein staerkeres Passwort genutzt werden.
                // Die Ergebnis-Daten muessen in dem StringBuffer "retData" platziert werden.
                case NEED_PASSPHRASE_LOAD:
                case NEED_PASSPHRASE_SAVE:
                    retData.replace(0,retData.length(),currentConnection.bankPin);
                    break;

                // PIN wird benoetigt
                case NEED_PT_PIN:
                    retData.replace(0,retData.length(),currentConnection.bankPin);
                    break;

                // BLZ wird benoetigt
                case NEED_BLZ:
                    retData.replace(0,retData.length(), currentConnection.info.getBlz());
                    break;

                // Die Benutzerkennung
                case NEED_USERID:
                    retData.replace(0,retData.length(),currentConnection.user);
                    break;

                // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
                // Bei manchen Banken kann man die auch leer lassen
                case NEED_CUSTOMERID:
                    retData.replace(0,retData.length(),currentConnection.user);
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
                        //var dlg = new Dlg(currentConnection.name,"TAN: ", msg, image);
                        //String tan = dlg.tan;
                        String tan = currentConnection.dlgCallback.dlg(currentConnection.name, "TAN: ", msg, image);

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


                        //var dlg = new Dlg(currentConnection.name, "TAN:", msg, image);
                        //String tan = dlg.tan;
                        String tan = currentConnection.dlgCallback.dlg(currentConnection.name, "TAN: ", msg, image);

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
                        //var dlg = new Dlg(currentConnection.name, "Medium Nr: ",
                        //        String.format("'%s'\n\nWerte:  '%s'", msg,retData.toString()), null);


                        //String code = dlg.tan;
                        String code = currentConnection.dlgCallback.dlg(currentConnection.name, "Medium Nr: ",
                                String.format("'%s'\n\nWerte:  '%s'", msg,retData.toString()), null);

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
                            //var dlg = new Dlg(currentConnection.name, "TAN:", msg, null);
                            //String tan = dlg.tan;

                            String tan = currentConnection.dlgCallback.dlg(currentConnection.name, "TAN: ", msg, null);
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

                        //var dlg = new Dlg(currentConnection.name, "TanMedium: ",
                        //        String.format("'%s'\n\nWerte:  '%s'", msg,retData.toString()), null);

                        //String code = dlg.tan;

                        String code = currentConnection.dlgCallback.dlg(currentConnection.name, "TanMedium: ",
                                String.format("'%s'\n\nWerte:  '%s'", msg,retData.toString()), null);

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
                    BankConnection.log(msg);
                    break;
                case NEED_COUNTRY:
                    retData.replace(0,retData.length(),"DE");
                case NEED_FILTER:
                    retData.replace(0,retData.length(),"Base64");
                case NEED_HOST:
                    retData.replace(0,retData.length(),currentConnection.info.getPinTanAddress());
                    break;
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
