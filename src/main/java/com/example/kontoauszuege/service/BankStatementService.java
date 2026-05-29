package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankStatement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BankStatementService {

    private final List<BankStatement> statements = new ArrayList<>();

    public BankStatementService() {
        initSampleData();
    }

    private void initSampleData() {
        statements.add(new BankStatement(1L,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1),
                "Max Mustermann", "Stadtwerke Musterstadt",
                "Stromrechnung Mai 2026 \n Kundennr. 123456",
                "DE89 3704 0044 0532 0130 00",
                new BigDecimal("-87.50"), "EUR",
                new BigDecimal("2312.45")));

        statements.add(new BankStatement(2L,
                LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 2),
                "Arbeitgeber GmbH", "Max Mustermann",
                "Gehalt April 2026",
                "DE27 2007 0000 0532 0130 00",
                new BigDecimal("2850.00"), "EUR",
                new BigDecimal("5162.45")));

        statements.add(new BankStatement(3L,
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 5),
                "Max Mustermann", "REWE Supermarkt",
                "Einkauf REWE 05.05.2026",
                "DE12 5001 0517 0648 4898 90",
                new BigDecimal("-63.20"), "EUR",
                new BigDecimal("5099.25")));

        statements.add(new BankStatement(4L,
                LocalDate.of(2026, 5, 8), LocalDate.of(2026, 5, 8),
                "Max Mustermann", "Vodafone GmbH",
                "Mobilfunk Vertragsnummer 987654 Mai 2026",
                "DE46 2007 0000 0660 7370 00",
                new BigDecimal("-35.99"), "EUR",
                new BigDecimal("5063.26")));

        statements.add(new BankStatement(5L,
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10),
                "Max Mustermann", "Amazon EU Sarl",
                "Bestellung 303-1234567-8901234",
                "LU76 0019 1200 3907 0000",
                new BigDecimal("-129.99"), "EUR",
                new BigDecimal("4933.27")));

        statements.add(new BankStatement(6L,
                LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 12),
                "Finanzamt München", "Max Mustermann",
                "Steuererstattung 2025 St-Nr. 133/815/08155",
                "DE91 7000 0000 0012 3456 78",
                new BigDecimal("540.00"), "EUR",
                new BigDecimal("5473.27")));

        statements.add(new BankStatement(7L,
                LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 15),
                "Max Mustermann", "Vermieter Hans Huber",
                "Miete Juni 2026 Wohnung Hauptstr. 1",
                "DE02 7001 0080 0619 3400 02",
                new BigDecimal("-950.00"), "EUR",
                new BigDecimal("4523.27")));

        statements.add(new BankStatement(8L,
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 18),
                "Max Mustermann", "Netflix International",
                "Netflix Abo Mai 2026",
                "LU32 0019 4006 4475 0000",
                new BigDecimal("-17.99"), "EUR",
                new BigDecimal("4505.28")));

        statements.add(new BankStatement(9L,
                LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20),
                "Deutsche Rentenversicherung", "Max Mustermann",
                "Rentenauskunft Vers-Nr. 65-170879-A-001",
                "DE12 1007 7777 0000 1234 56",
                new BigDecimal("215.50"), "EUR",
                new BigDecimal("4720.78")));

        statements.add(new BankStatement(10L,
                LocalDate.of(2026, 5, 22), LocalDate.of(2026, 5, 22),
                "Max Mustermann", "Tankstelle Shell AG",
                "Tanken 22.05.2026",
                "DE56 3006 0601 0001 6419 71",
                new BigDecimal("-78.40"), "EUR",
                new BigDecimal("4642.38")));

        statements.add(new BankStatement(11L,
                LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 25),
                "Max Mustermann", "GEZ Rundfunkbeitrag",
                "Rundfunkbeitrag Q2 2026 Beitragsnr. 123456789",
                "DE74 5001 0517 5427 5580 30",
                new BigDecimal("-55.08"), "EUR",
                new BigDecimal("4587.30")));

        statements.add(new BankStatement(12L,
                LocalDate.of(2026, 5, 28), LocalDate.of(2026, 5, 28),
                "Erika Mustermann", "Max Mustermann",
                "Rückzahlung Urlaub",
                "DE89 5001 0517 0707 0107 00",
                new BigDecimal("200.00"), "EUR",
                new BigDecimal("4787.30")));
    }

    public List<BankStatement> findAll() {
        return new ArrayList<>(statements);
    }

    public List<BankStatement> findByFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return findAll();
        }
        String lowerFilter = filter.toLowerCase();
        return statements.stream()
                .filter(s ->
                        (s.getAuftraggeber() != null && s.getAuftraggeber().toLowerCase().contains(lowerFilter)) ||
                        (s.getEmpfaenger() != null && s.getEmpfaenger().toLowerCase().contains(lowerFilter)) ||
                        (s.getVerwendungszweck() != null && s.getVerwendungszweck().toLowerCase().contains(lowerFilter)) ||
                        (s.getIban() != null && s.getIban().toLowerCase().contains(lowerFilter))
                )
                .toList();
    }
}
