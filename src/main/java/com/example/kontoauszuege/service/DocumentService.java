package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.model.DocumentState;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

    private final DataAccessService dataAccessService;

    public DocumentService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    @Transactional(readOnly = true)
    public List<DocumentDataObject> getAllNewDocuments() {
        return dataAccessService.getAll(DocumentDataObject.class).stream()
                .filter(d -> d.getState() == DocumentState.NEW)
                .collect(Collectors.toList());
    }

    /**
     * Liest rekursiv alle PDF-Dateien in ~/banking/inbox/ und legt für jede
     * ein DocumentDataObject in der DB an.
     *
     * @return Anzahl importierter Dokumente
     */
    @Transactional
    public int ImportInbox() {
        Path inbox = Paths.get(System.getProperty("user.home"), ".jbanking", "inbox");
        if (!Files.exists(inbox)) {
            LOG.info("Inbox-Verzeichnis existiert nicht: {}", inbox);
            return 0;
        }

        try (Stream<Path> stream = Files.walk(inbox)) {
            List<Path> pdfs = stream
                    .filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());

            int count = 0;
            for (Path p : pdfs) {
                try {
                    DocumentDataObject doc = new DocumentDataObject();
                    doc.setFileName(p.getFileName().toString());
                    doc.setState(DocumentState.NEW);

                    Map<String, Object> attrs = new HashMap<>();
                    attrs.put("path", p.toAbsolutePath().toString());
                    attrs.put("size", Files.size(p));
                    attrs.put("lastModifiedMillis", Files.getLastModifiedTime(p).toMillis());
                    doc.setAttributes(attrs);

                    dataAccessService.insert(doc);
                    count++;
                } catch (Exception e) {
                    LOG.warn("Fehler beim Importieren von {}: {}", p, e.toString());
                }
            }
            return count;
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Durchsuchen des Inbox-Ordners", e);
        }
    }

    @Transactional
    public void archiveDocument(DocumentDataObject doc) {
        if (doc == null) return;
        try {
            doc.setState(DocumentState.ARCHIVED);
            // DataAccessService.update expects an object previously inserted or loaded
            dataAccessService.update(doc);
        } catch (Exception e) {
            LOG.warn("Fehler beim Archivieren von {}: {}", doc, e.toString());
            throw e;
        }
    }
}
