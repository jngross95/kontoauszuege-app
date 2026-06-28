package com.example.kontoauszuege.service.DataAccess;

import com.example.kontoauszuege.model.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Basisklasse für alle persistierbaren Fachobjekte.
 * <p>
 * {@code id} und {@code pk} stammen aus dem zugehörigen {@code Entity} und werden
 * deshalb nicht in das JSON der {@code data}-Spalte serialisiert.
 */
public class DataObject {

    /** Fachlicher Schlüssel (GUID) des zugrundeliegenden Entity. */
    @JsonIgnore
    private String pk;

    /**
     * Referenz auf das zugrundeliegende persistente {@link Entity}.
     * <p>
     * Wird beim Einfügen und Laden gesetzt, damit ein {@code update} den Datensatz
     * direkt aktualisieren kann, ohne ihn erneut über {@code findByPk} zu laden.
     */
    @JsonIgnore
    private Entity entity;


    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}
