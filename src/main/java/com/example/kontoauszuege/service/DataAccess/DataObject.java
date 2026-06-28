package com.example.kontoauszuege.service.DataAccess;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Basisklasse für alle persistierbaren Fachobjekte.
 * <p>
 * {@code id} und {@code pk} stammen aus dem zugehörigen {@code Entity} und werden
 * deshalb nicht in das JSON der {@code data}-Spalte serialisiert.
 */
public class DataObject {

    /** Technischer Schlüssel des zugrundeliegenden Entity (Autoincrement). */
    @JsonIgnore
    private Long id;

    /** Fachlicher Schlüssel (GUID) des zugrundeliegenden Entity. */
    @JsonIgnore
    private String pk;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }
}
