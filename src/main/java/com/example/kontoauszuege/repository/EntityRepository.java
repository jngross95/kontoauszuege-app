package com.example.kontoauszuege.repository;

import com.example.kontoauszuege.model.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring-Data-Repository für die Tabelle {@code entity}.
 */
@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {

    /** Findet einen Eintrag anhand des fachlichen Schlüssels (pk). */
    Optional<Entity> findByPk(String pk);

    /** Findet alle Einträge eines bestimmten Typs. */
    List<Entity> findByType(String type);
}

