package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.model.BankContactDataObject;
import com.example.kontoauszuege.model.UeberweisungDataObject;
import com.example.kontoauszuege.model.UeberweisungStatus;
import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.BankAccess.DlgCallback;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UeberweisungService {

    private final DataAccessService dataAccessService;
    private final BankAccountService bankAccountService;
    private final BankContactService bankContactService;

    public UeberweisungService(DataAccessService dataAccessService,
                               BankAccountService bankAccountService,
                               BankContactService bankContactService) {
        this.dataAccessService = dataAccessService;
        this.bankAccountService = bankAccountService;
        this.bankContactService = bankContactService;
    }

    /**
     * Liefert alle gespeicherten Überweisungen.
     */
    public List<UeberweisungDataObject> findAll() {
        return dataAccessService.getAll(UeberweisungDataObject.class);
    }

    /**
     * Speichert eine neue Überweisung.
     */
    public UeberweisungDataObject add(UeberweisungDataObject ueberweisung) {
        return dataAccessService.insert(ueberweisung);
    }

    /**
     * Aktualisiert eine bestehende Überweisung.
     */
    public UeberweisungDataObject update(UeberweisungDataObject ueberweisung) {
        return dataAccessService.update(ueberweisung);
    }

    /**
     * Entfernt eine gespeicherte Überweisung.
     */
    public void remove(UeberweisungDataObject ueberweisung) {
        dataAccessService.delete(ueberweisung);
    }

    /**
     * Führt alle ausgewählten Überweisungen aus. Überweisungen werden nach der
     * zugehörigen Bankverbindung (Kontakt) gruppiert, sodass pro Bank nur eine
     * Verbindung aufgebaut werden muss.
     *
     * @param ueberweisungen die auszuführenden Überweisungen
     * @return die Status-Rückmeldungen der Bank je ausgeführter Überweisung
     */
    public List<String> ueberweisungenAusfuehren(List<UeberweisungDataObject> ueberweisungen, DlgCallback dlgCallback) throws Exception {
        List<String> ergebnisse = new ArrayList<>();
        if (ueberweisungen == null || ueberweisungen.isEmpty()) {
            return ergebnisse;
        }

        List<BankAccountDataObject> konten = bankAccountService.getAllBankAccounts();
        List<BankContactDataObject> kontakte = bankContactService.getAllBankContacts();

        // Konten nach IBAN und Kontakte nach BIC indizieren.
        Map<String, BankAccountDataObject> kontoNachIban = new HashMap<>();
        for (BankAccountDataObject konto : konten) {
            String iban = normalize(konto.getIban());
            if (!iban.isBlank()) {
                kontoNachIban.putIfAbsent(iban, konto);
            }
        }

        Map<String, BankContactDataObject> kontaktNachBic = new HashMap<>();
        for (BankContactDataObject kontakt : kontakte) {
            String bic = normalize(kontakt.getBic());
            if (!bic.isBlank()) {
                kontaktNachBic.putIfAbsent(bic, kontakt);
            }
        }

        // Ausgewählte Überweisungen nach Kontakt-BIC gruppieren.
        Map<String, List<UeberweisungDataObject>> nachKontaktBic = new HashMap<>();
        for (UeberweisungDataObject ueberweisung : ueberweisungen) {
            if (!ueberweisung.isAusgewaehlt()) {
                continue;
            }

            String senderIban = normalize(ueberweisung.getSender());
            BankAccountDataObject konto = kontoNachIban.get(senderIban);
            if (konto == null) {
                throw new Exception(String.format(
                        "Kein Konto für die Sender-IBAN '%s' gefunden.", ueberweisung.getSender()));
            }

            String bic = normalize(konto.getBic());
            if (!kontaktNachBic.containsKey(bic)) {
                throw new Exception(String.format(
                        "Keine Bankverbindung (Kontakt) für die BIC '%s' gefunden.", konto.getBic()));
            }

            nachKontaktBic.computeIfAbsent(bic, k -> new ArrayList<>()).add(ueberweisung);
        }

        // Pro Kontakt eine Verbindung aufbauen und die Überweisungen ausführen.
        for (Map.Entry<String, List<UeberweisungDataObject>> entry : nachKontaktBic.entrySet()) {
            BankContactDataObject kontakt = kontaktNachBic.get(entry.getKey());

            try (BankConnection connection = new BankConnection(
                    kontakt.getName(),
                    kontakt.getBic(),
                    kontakt.getUser(),
                    kontakt.getBankPin(), dlgCallback)) {
                connection.connect();

                for (UeberweisungDataObject ueberweisung : entry.getValue()) {
                    ueberweisung.setStatus(UeberweisungStatus.SENDING);
                    ueberweisung.setAusgewaehlt(false);
                    dataAccessService.update(ueberweisung);
                    try {
                        ergebnisse.add(fuehreEinzelueberweisungAus(connection, ueberweisung));
                        ueberweisung.setStatus(UeberweisungStatus.SENT);
                        dataAccessService.update(ueberweisung);
                    } catch (Exception ex) {
                        ueberweisung.setStatus(UeberweisungStatus.ERROR);
                        dataAccessService.update(ueberweisung);
                        throw ex;
                    }
                }
            }
        }

        return ergebnisse;
    }

    /**
     * Führt eine einzelne Überweisung über die bereits verbundene
     * {@link BankConnection} aus.
     */
    private String fuehreEinzelueberweisungAus(BankConnection connection,
                                               UeberweisungDataObject ueberweisung) throws Exception {
        String empfaengerIban = normalize(ueberweisung.getEmpfaengerIban());
        if (empfaengerIban.isBlank()) {
            throw new Exception("Empfänger-IBAN fehlt.");
        }

        BigDecimal betrag = ueberweisung.getBetrag();
        if (betrag == null || betrag.signum() <= 0) {
            throw new Exception(String.format(
                    "Ungültiger Betrag für die Überweisung an '%s'.", ueberweisung.getEmpfaenger()));
        }

        // Empfänger-BIC bestimmen, ggf. aus der IBAN ableiten.
        String empfaengerBic = normalize(ueberweisung.getEmpfaengerBic());
        if (empfaengerBic.isBlank()) {
            empfaengerBic = BankConnection.bicAusIban(empfaengerIban);
        }
        if (empfaengerBic == null || empfaengerBic.isBlank()) {
            throw new Exception(String.format(
                    "Empfänger-BIC konnte nicht ermittelt werden für die IBAN '%s'.", empfaengerIban));
        }

        return connection.UeberweisungAusfuehren(
                normalize(ueberweisung.getSender()),
                ueberweisung.getEmpfaenger(),
                empfaengerBic,
                empfaengerIban,
                betrag,
                null,
                ueberweisung.getVerwendungszweck());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
