package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.DocumentDataObject;
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

    private final DocumentService documentService;
    private final PdfService pdfService;

    public DocumentController(DocumentService documentService, PdfService pdfService) {
        this.documentService = documentService;
        this.pdfService = pdfService;
    }

    @GetMapping(value = "/pdf/{pk}")
    public ResponseEntity<InputStreamResource> getPdfByPk(@PathVariable String pk) throws IOException {
        DocumentDataObject doc = documentService.getByPk(pk);
        if (doc == null || doc.getFilePath() == null || doc.getFileName() == null) {
            return ResponseEntity.notFound().build();
        }

        Path file = Paths.get(doc.getFilePath(), doc.getFileName());
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
        DocumentDataObject doc = documentService.getByPk(pk);
        if (doc == null || doc.getFilePath() == null || doc.getFileName() == null) {
            return ResponseEntity.notFound().build();
        }

        Path file = Paths.get(doc.getFilePath(), doc.getFileName());
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = Files.readAllBytes(file);
        pdfService.setPdf(bytes);
        String text = pdfService.extractText(page, xFrom, xTo, yFrom, yTo);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(text == null ? "" : text);
    }
}
