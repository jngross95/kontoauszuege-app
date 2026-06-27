package com.example.kontoauszuege.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generische Entität, die in der Tabelle {@code entity} gespeichert wird.
 * <p>
 * Hinweis: Die Annotation {@code @Entity} wird voll qualifiziert verwendet,
 * da der Klassenname {@code Entity} sonst die Annotation
 * {@code jakarta.persistence.Entity} verdecken würde.
 */
@jakarta.persistence.Entity
@Table(name = "entity")
public class Entity {

    /** Technischer Primärschlüssel (Autoincrement). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Fachlicher Schlüssel - eindeutig, nicht null, Default ist eine GUID. */
    @Column(name = "pk", nullable = false, unique = true, length = 255)
    private String pk = UUID.randomUUID().toString();

    /** Zeitstempel mit Datum und Uhrzeit bis Millisekunden. */
    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    /** Frei wählbarer Typ-Diskriminator. */
    @Column(name = "type")
    private String type;

    /** Nutzdaten als JSON-Dokument. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "json")
    private String data;

    public Entity() {
    }

    public Entity(String type, String data) {
        this.type = type;
        this.data = data;
    }

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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}

