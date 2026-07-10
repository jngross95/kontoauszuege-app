package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.Entity;
import com.example.kontoauszuege.repository.EntityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final EntityRepository repository;
    private final ObjectMapper objectMapper;
    private final PdfService pdfService;

    public DocumentController(EntityRepository repository, ObjectMapper objectMapper, PdfService pdfService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.pdfService = pdfService;
    }

    @GetMapping(value = "/pdf/{pk}")
    public ResponseEntity<InputStreamResource> getPdfByPk(@PathVariable String pk) throws IOException {
        Entity entity = repository.findByPk(pk).orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        JsonNode root = objectMapper.readTree(entity.getData());
        JsonNode attrs = root.path("attributes");
        String path = attrs.path("path").asText(null);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        Path file = Paths.get(path);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(file))
                .body(resource);
    }

    @GetMapping(value = "/pdf/{pk}/extract")
    public ResponseEntity<String> extractTextFromPdf(@PathVariable String pk,
                                                     @RequestParam("xFrom") double xFrom,
                                                     @RequestParam("xTo") double xTo,
                                                     @RequestParam("yFrom") double yFrom,
                                                     @RequestParam("yTo") double yTo,
                                                     @RequestParam(value = "page", required = false, defaultValue = "1") int page) throws IOException {
        Entity entity = repository.findByPk(pk).orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        JsonNode root = objectMapper.readTree(entity.getData());
        JsonNode attrs = root.path("attributes");
        String path = attrs.path("path").asText(null);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        Path file = Paths.get(path);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = Files.readAllBytes(file);
        pdfService.setPdf(bytes);
        String text = pdfService.extractText(page, xFrom, xTo, yFrom, yTo);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(text == null ? "" : text);
    }
}
