package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.model.BankContactDataObject;
import com.example.kontoauszuege.model.BankStatementDataObject;
import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.BankAccess.KontoBuchung;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class BankStatementService {

    private final DataAccessService dataAccessService;
    private final BankAccountService bankAccountService;
    private final BankContactService bankContactService;

    public BankStatementService(DataAccessService dataAccessService,
                                BankAccountService bankAccountService,
                                BankContactService bankContactService) {
        this.dataAccessService = dataAccessService;
        this.bankAccountService = bankAccountService;
        this.bankContactService = bankContactService;
    }

    public List<BankStatementDataObject> getAllStatements() {
        return dataAccessService.getAll(BankStatementDataObject.class);
    }

    public BankStatementDataObject addStatement(BankStatementDataObject statement) {
        return dataAccessService.insert(statement);
    }

    public void receiveStmts() throws Exception {
        List<BankStatementDataObject> bestehendeStatements = getAllStatements();
        List<BankAccountDataObject> konten = bankAccountService.getAllBankAccounts();
        List<BankContactDataObject> kontakte = bankContactService.getAllBankContacts();

        Set<String> vorhandeneKeys = new HashSet<>();
        for (BankStatementDataObject statement : bestehendeStatements) {
            vorhandeneKeys.add(buildStatementKey(statement));
        }

        Map<String, BankContactDataObject> kontaktNachBic = new HashMap<>();
        for (BankContactDataObject kontakt : kontakte) {
            String bic = normalize(kontakt.getBic());
            if (!bic.isBlank() && !kontaktNachBic.containsKey(bic)) {
                kontaktNachBic.put(bic, kontakt);
            }
        }

        Map<String, List<BankAccountDataObject>> kontenNachBic = konten.stream()
                .filter(konto -> !normalize(konto.getBic()).isBlank())
                .collect(java.util.stream.Collectors.groupingBy(konto -> normalize(konto.getBic())));

        for (Map.Entry<String, List<BankAccountDataObject>> entry : kontenNachBic.entrySet()) {
            String bic = entry.getKey();
            BankContactDataObject kontakt = kontaktNachBic.get(bic);
            if (kontakt == null) {
                continue;
            }

            try (BankConnection connection = new BankConnection(
                    kontakt.getName(),
                    kontakt.getBic(),
                    kontakt.getUser(),
                    kontakt.getBankPin())) {
                connection.connect();

                for (BankAccountDataObject konto : entry.getValue()) {
                    String iban = normalize(konto.getIban());
                    if (iban.isBlank()) {
                        continue;
                    }

                    List<KontoBuchung> buchungen = connection.UmsaetzeAbholen(iban, new Date(0));
                    for (KontoBuchung buchung : buchungen) {
                        BankStatementDataObject statement = toDataObject(iban, buchung);
                        String key = buildStatementKey(statement);
                        if (vorhandeneKeys.contains(key)) {
                            continue;
                        }

                        dataAccessService.insert(statement);
                    }
                }
            }
        }
    }

    private BankStatementDataObject toDataObject(String iban, KontoBuchung buchung) {
        BankStatementDataObject statement = new BankStatementDataObject();
        statement.setIban(iban);
        statement.setBuchungsdatum(buchung.Buchungsdatum);
        statement.setWertstellungsdatum(buchung.Wertstellungsdatum);
        statement.setGeschaeftsvorfall(defaultString(buchung.Geschaeftsvorfall));
        statement.setEmpfaenger(defaultString(buchung.Empfaenger));
        statement.setEmpfaengerKontoNr(defaultString(buchung.EmpfaengerKontoNr));
        statement.setEmpfaengerBLZ(defaultString(buchung.EmpfaengerBLZ));
        statement.setBetrag(defaultBigDecimal(buchung.Betrag));
        statement.setVerwendungszweck(defaultString(buchung.Verwendungszweck));
        statement.setSaldo(defaultBigDecimal(buchung.Saldo));
        return statement;
    }

    private String buildStatementKey(BankStatementDataObject statement) {
        return String.join("|",
                normalize(statement.getIban()),
                String.valueOf(timeMillis(statement.getBuchungsdatum())),
                String.valueOf(timeMillis(statement.getWertstellungsdatum())),
                normalize(statement.getGeschaeftsvorfall()),
                normalize(statement.getEmpfaenger()),
                normalize(statement.getEmpfaengerKontoNr()),
                normalize(statement.getEmpfaengerBLZ()),
                normalize(statement.getVerwendungszweck()),
                normalizeBigDecimal(statement.getBetrag()),
                normalizeBigDecimal(statement.getSaldo()));
    }

    private long timeMillis(Date date) {
        return date == null ? 0L : date.getTime();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeBigDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String defaultString(String value) {
        return Objects.toString(value, "");
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
