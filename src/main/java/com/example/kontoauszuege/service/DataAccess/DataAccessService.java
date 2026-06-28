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

        obj.setId(saved.getId());
        obj.setPk(saved.getPk());
        return obj;
    }

    /**
     * Aktualisiert ein bereits vorhandenes Fachobjekt. Das Objekt muss über einen
     * gültigen {@code pk} verfügen, der auf einen existierenden Datensatz verweist.
     *
     * @return dasselbe (mit aktualisierter id/pk befüllte) Objekt
     * @throws IllegalArgumentException wenn kein {@code pk} gesetzt ist
     * @throws IllegalStateException    wenn zum {@code pk} kein Datensatz existiert
     */
    @Transactional
    public <T extends DataObject> T update(T obj) {
        if (obj.getPk() == null) {
            throw new IllegalArgumentException(
                    "update benötigt ein Objekt mit gesetztem pk - für neue Objekte bitte insert verwenden");
        }

        Entity entity = repository.findByPk(obj.getPk())
                .orElseThrow(() -> new IllegalStateException(
                        "Kein Datensatz mit pk=" + obj.getPk() + " gefunden"));

        entity.setType(obj.getClass().getName());
        entity.setData(toJson(obj));

        Entity saved = repository.save(entity);

        Assert.state(Objects.equals(obj.getId(), saved.getId()),
                "id darf sich bei update nicht ändern");
        Assert.state(Objects.equals(obj.getPk(), saved.getPk()),
                "pk darf sich bei update nicht ändern");
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
            obj.setId(entity.getId());
            obj.setPk(entity.getPk());
            return obj;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Fehler beim Deserialisieren von Entity id=" + entity.getId()
                            + " nach " + type.getName(), e);
        }
    }
}
