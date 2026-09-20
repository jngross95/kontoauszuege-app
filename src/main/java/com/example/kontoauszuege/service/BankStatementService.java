package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.model.BankContactDataObject;
import com.example.kontoauszuege.model.BankStatementDataObject;
import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.BankAccess.DlgCallback;
import com.example.kontoauszuege.service.BankAccess.KontoBuchung;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Comparator;
import java.util.ArrayList;
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
    private static final String DATE_FORMAT = "dd.MM.yyyy";

    public BankStatementService(DataAccessService dataAccessService,
                                BankAccountService bankAccountService,
                                BankContactService bankContactService) {
        this.dataAccessService = dataAccessService;
        this.bankAccountService = bankAccountService;
        this.bankContactService = bankContactService;
    }

    public List<BankStatementDataObject> getAllStatements() {
        List<BankStatementDataObject> all = dataAccessService.getAll(BankStatementDataObject.class);
        if (all == null) return List.of();
        List<BankStatementDataObject> mutable = new ArrayList<>(all);
        mutable.sort(Comparator
            .comparing(BankStatementDataObject::getBuchungsdatum, Comparator.nullsFirst(Date::compareTo))
            .thenComparing(s -> s.getEntity().getId(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .reversed());
        return mutable;
    }

    public BankStatementDataObject addStatement(BankStatementDataObject statement) {
        return dataAccessService.insert(statement);
    }

    public void receiveStmts(DlgCallback dlgCallback, String targetIban) throws Exception {
        String normTarget = (targetIban == null || targetIban.isBlank()) ? null : normalize(targetIban);

        List<BankStatementDataObject> bestehendeStatements = getAllStatements();
        List<BankAccountDataObject> konten = bankAccountService.getAllBankAccounts();
        List<BankContactDataObject> kontakte = bankContactService.getAllBankContacts();

        // If a specific IBAN was provided, only consider that account
        if (normTarget != null) {
            konten = konten.stream()
                    .filter(konto -> normTarget.equals(normalize(konto.getIban())))
                    .collect(java.util.stream.Collectors.toList());
        }

        HashMap<String, BankStatementDataObject> vorhandeneKeys = new HashMap<String, BankStatementDataObject>();
        for (BankStatementDataObject statement : bestehendeStatements) {
            vorhandeneKeys.put(buildStatementKey(statement), statement);
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
                    kontakt.getBankPin(),
                    dlgCallback)) {
                connection.connect();

                for (BankAccountDataObject konto : entry.getValue()) {
                    String iban = normalize(konto.getIban());
                    if (iban.isBlank()) {
                        continue;
                    }

                    // Startdatum ermitteln: Maximum (Buchungsdatum) der bestehenden
                    // Statements dieses Kontos und davon eine Woche abziehen.
                    Date startDate = bestehendeStatements.stream()
                            .filter(s -> iban.equals(normalize(s.getIban())))
                            .map(BankStatementDataObject::getBuchungsdatum)
                            .filter(Objects::nonNull)
                            .max(Date::compareTo)
                            .orElse(null);

                    // Vom Startdatum 7 Tage abziehen (Instant)
                    if (startDate != null) {
                        startDate = Date.from(startDate.toInstant().minus(7, ChronoUnit.DAYS));
                    }

                    List<KontoBuchung> buchungen = connection.UmsaetzeAbholen(iban, startDate);
                    for (KontoBuchung buchung : buchungen) {
                        BankStatementDataObject statement = toDataObject(iban, buchung);
                        String key = buildStatementKey(statement);
                        if (vorhandeneKeys.containsKey(key)) {
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
        statement.setEmpfaenger2(defaultString(buchung.Empfaenger2));
        statement.setEmpfaengerKontoNr(defaultString(buchung.EmpfaengerKontoNr));
        statement.setEmpfaengerBLZ(defaultString(buchung.EmpfaengerBLZ));
        statement.setBetrag(defaultBigDecimal(buchung.Betrag));
        statement.setVerwendungszweck(defaultString(buchung.Verwendungszweck));
        statement.setSaldo(defaultBigDecimal(buchung.Saldo));
        return statement;
    }

    public void deleteStatementsByIban(String iban) {
        if (iban == null || iban.isBlank()) {
            return;
        }
        String normalizedIban = normalize(iban);
        List<BankStatementDataObject> allStatements = getAllStatements();
        for (BankStatementDataObject statement : allStatements) {
            if (normalizedIban.equals(normalize(statement.getIban()))) {
                dataAccessService.delete(statement);
            }
        }
    }

    public void deleteAccountAndStatements(BankAccountDataObject bankAccountDataObject) {
        if (bankAccountDataObject != null && bankAccountDataObject.getIban() != null) {
            deleteStatementsByIban(bankAccountDataObject.getIban());
        }
        dataAccessService.delete(bankAccountDataObject);
    }

    /**
     * Exports all statements to a spreadsheet file. The file is created in the system
     * temp directory and named kontoauszuege-<GUID>.xlsx (content is XLSX).
     * @return the created File
     */
    public java.io.File exportAllStatementsAsSpreadsheet() throws Exception {
        List<BankStatementDataObject> statements = getAllStatements();

        String uuid = java.util.UUID.randomUUID().toString();
        String filename = "kontoauszuege-" + uuid + ".xlsx";
        String tmpdir = System.getProperty("java.io.tmpdir");
        java.io.File outFile = new java.io.File(tmpdir, filename);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = wb.createSheet("Kontoauszuege");

            // header
            var header = sheet.createRow(0);
            String[] cols = new String[]{"IBAN","Buchungsdatum","Wertstellungsdatum","Geschäftsvorfall","Empfänger","EmpfängerKontoNr","EmpfängerBLZ","Betrag","Verwendungszweck","Saldo"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat(DATE_FORMAT);

            int rowIdx = 1;
            for (BankStatementDataObject s : statements) {
                var row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getIban() != null ? s.getIban() : "");
                row.createCell(1).setCellValue(s.getBuchungsdatum() != null ? df.format(s.getBuchungsdatum()) : "");
                row.createCell(2).setCellValue(s.getWertstellungsdatum() != null ? df.format(s.getWertstellungsdatum()) : "");
                row.createCell(3).setCellValue(s.getGeschaeftsvorfall() != null ? s.getGeschaeftsvorfall() : "");
                row.createCell(4).setCellValue(s.getEmpfaengerUI() != null ? s.getEmpfaengerUI() : "");
                row.createCell(5).setCellValue(s.getEmpfaengerKontoNr() != null ? s.getEmpfaengerKontoNr() : "");
                row.createCell(6).setCellValue(s.getEmpfaengerBLZ() != null ? s.getEmpfaengerBLZ() : "");
                row.createCell(7).setCellValue(s.getBetrag() != null ? s.getBetrag().toPlainString() : "0");
                row.createCell(8).setCellValue(s.getVerwendungszweck() != null ? s.getVerwendungszweck() : "");
                row.createCell(9).setCellValue(s.getSaldo() != null ? s.getSaldo().toPlainString() : "0");
            }

            // autosize columns (best effort)
            for (int i = 0; i < cols.length; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception ignored) {
                }
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                wb.write(fos);
            }
        }

        return outFile;
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
