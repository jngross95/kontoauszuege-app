package com.example.kontoauszuege.repository;

import com.example.kontoauszuege.model.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = EntityRepositoryTest.TestConfig.class)
class EntityRepositoryTest {

    @Configuration
    @EnableAutoConfiguration(excludeName = {
            "com.vaadin.flow.spring.SpringBootAutoConfiguration",
            "com.vaadin.flow.spring.SpringSecurityAutoConfiguration",
            "com.vaadin.flow.spring.VaadinScopesConfig"
    })
    @EntityScan(basePackageClasses = Entity.class)
    @EnableJpaRepositories(basePackageClasses = EntityRepository.class)
    static class TestConfig {
    }


    @Autowired
    private EntityRepository repository;

    @Test
    void speichertUndLiestEntityMitJsonUndDefaults() {
        Entity e = new Entity("KONTO", "{\"iban\":\"DE12\",\"betrag\":12.34}");
        Entity saved = repository.saveAndFlush(e);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPk()).isNotBlank();
        assertThat(saved.getDate()).isNotNull();

        Optional<Entity> reloaded = repository.findByPk(saved.getPk());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getType()).isEqualTo("KONTO");
        assertThat(reloaded.get().getData()).contains("\"iban\":\"DE12\"");
        assertThat(reloaded.get().getDate()).isInstanceOf(LocalDateTime.class);
    }
}

