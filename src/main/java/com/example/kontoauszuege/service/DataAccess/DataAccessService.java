package com.example.kontoauszuege.service.DataAccess;

import com.example.kontoauszuege.model.Entity;
import com.example.kontoauszuege.repository.EntityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * Generischer Persistenz-Dienst für Fachobjekte, die von {@link DataObject} ableiten.
 * <p>
 * Jedes Fachobjekt wird als JSON (via {@link ObjectMapper}) serialisiert und in einem
 * {@link Entity} abgelegt. Der vollqualifizierte Klassenname dient als {@code type},
 * der die Datensätze eines Typs voneinander trennt. Beim Lesen werden die Entities
 * wieder in das jeweilige Fachobjekt deserialisiert.
 */
@Service
public class DataAccessService {

    private final EntityRepository repository;
    private final ObjectMapper objectMapper;

    public DataAccessService(EntityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Legt ein neues Fachobjekt an. Es wird stets ein neuer Datensatz erzeugt;
     * ein eventuell vorhandener {@code pk} am Objekt wird ignoriert (das Entity
     * vergibt eine neue GUID). Die erzeugten Werte ({@code id}, {@code pk}) werden
     * in das übergebene Objekt zurückgeschrieben.
     *
     * @return dasselbe (nun mit id/pk befüllte) Objekt
     */
    @Transactional
    public <T extends DataObject> T insert(T obj) {
        Entity entity = new Entity();
        entity.setType(obj.getClass().getName());
        entity.setData(toJson(obj));

        Entity saved = repository.save(entity);

        obj.setPk(saved.getPk());
        obj.setEntity(saved);
        return obj;
    }

    /**
     * Aktualisiert ein bereits vorhandenes Fachobjekt. Das Objekt muss zuvor über
     * {@link #insert} oder {@link #getAll} geladen worden sein, damit es das
     * zugehörige {@link Entity} kennt - dadurch ist kein erneutes {@code findByPk}
     * nötig.
     *
     * @return dasselbe Objekt
     * @throws IllegalStateException wenn kein zugrundeliegendes Entity vorhanden ist
     *                               oder sich id/pk unerwartet ändern
     */
    @Transactional
    public <T extends DataObject> T update(T obj) {
        Entity entity = obj.getEntity();
        Assert.state(entity != null,
                "update benötigt ein zuvor geladenes oder eingefügtes Objekt - für neue Objekte bitte insert verwenden");

        entity.setType(obj.getClass().getName());
        entity.setData(toJson(obj));

        var id = entity.getId();
        Entity saved = repository.save(entity);

        Assert.state(Objects.equals(id, saved.getId()),
                "id darf sich bei update nicht ändern");
        Assert.state(Objects.equals(obj.getPk(), saved.getPk()),
                "pk darf sich bei update nicht ändern");
        obj.setEntity(saved);
        return obj;
    }

    /**
     * Liest alle gespeicherten Objekte des angegebenen Typs.
     * <p>
     * Da Java-Generics zur Laufzeit gelöscht werden, muss die Zielklasse explizit
     * übergeben werden - sie bestimmt sowohl den Typ-Filter als auch das Ziel der
     * JSON-Deserialisierung.
     */
    @Transactional(readOnly = true)
    public <T extends DataObject> List<T> getAll(Class<T> type) {
        return repository.findByType(type.getName()).stream()
                .map(entity -> fromEntity(entity, type))
                .toList();
    }

    private String toJson(DataObject obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Fehler beim Serialisieren von " + obj.getClass().getName(), e);
        }
    }

    private <T extends DataObject> T fromEntity(Entity entity, Class<T> type) {
        try {
            T obj = objectMapper.readValue(entity.getData(), type);
            obj.setPk(entity.getPk());
            obj.setEntity(entity);
            return obj;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Fehler beim Deserialisieren von Entity id=" + entity.getId()
                            + " nach " + type.getName(), e);
        }
    }
}
