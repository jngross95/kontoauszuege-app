package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.Ueberweisung;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class UeberweisungService {

    private final List<Ueberweisung> ueberweisungen = new ArrayList<>();

    public UeberweisungService() {
        ueberweisungen.add(new Ueberweisung(
                "Max Mustermann",
                "Stadtwerke Musterstadt",
                "DE89 3704 0044 0532 0130 00",
                "Miete Juni 2026",
                new BigDecimal("950.00")));
        ueberweisungen.add(new Ueberweisung(
                "Max Mustermann",
                "Energieversorger GmbH",
                "DE27 2007 0000 0532 0130 00",
                "Strom & Gas Mai 2026",
                new BigDecimal("87.50")));
    }

    public List<Ueberweisung> findAll() {
        return new ArrayList<>(ueberweisungen);
    }

    public void add(Ueberweisung u) {
        ueberweisungen.add(u);
    }

    public void remove(Ueberweisung u) {
        ueberweisungen.removeIf(e -> e.getId().equals(u.getId()));
    }

    public void update(Ueberweisung u) {
        for (int i = 0; i < ueberweisungen.size(); i++) {
            if (ueberweisungen.get(i).getId().equals(u.getId())) {
                ueberweisungen.set(i, u);
                return;
            }
        }
    }
}
