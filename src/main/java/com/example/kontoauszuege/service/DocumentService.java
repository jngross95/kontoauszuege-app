package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.model.DocumentState;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

    private final DataAccessService dataAccessService;
    private final FileSystemService fileSystemService;

    public DocumentService(DataAccessService dataAccessService, FileSystemService fileSystemService) {
        this.dataAccessService = dataAccessService;
        this.fileSystemService = fileSystemService;
    }

    @Transactional(readOnly = true)
    public List<DocumentDataObject> getAllDocuments() {
        return dataAccessService.getAll(DocumentDataObject.class).stream()
            .sorted(Comparator.comparing(DocumentDataObject::getFileModifyDate,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentDataObject getByPk(String pk) {
        if (pk == null) return null;
        return dataAccessService.getAll(DocumentDataObject.class).stream()
                .filter(d -> pk.equals(d.getPk()))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DocumentDataObject> getAllNewDocuments() {
        return dataAccessService.getAll(DocumentDataObject.class).stream()
            .filter(d -> d.getState() == DocumentState.NEW)
            .sorted(Comparator.comparing(DocumentDataObject::getFileModifyDate,
                Comparator.nullsLast(Comparator.naturalOrder())))
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
        
        Path inbox = Paths.get(fileSystemService.getBaseDir(), "inbox");
        if (!Files.exists(inbox)) {
           //create the inbox directory if it does not exist
           try {
               Files.createDirectories(inbox);
           } catch (IOException e) {
               throw new IllegalStateException("Fehler beim Erstellen des Inbox-Ordners: "+ inbox.toAbsolutePath().toString(), e);
           } 
        }

        // collect existing filenames of NEW documents to avoid duplicate imports
        List<DocumentDataObject> existing = getAllNewDocuments();

        // Lösche DB-Einträge, deren Datei nicht mehr auf der Platte existiert
        existing.removeIf(d -> {
            if (d.getFilePath() == null || d.getFileName() == null || !Files.exists(resolvePath(d.getFilePath(), d.getFileName()))) {
                try {
                    dataAccessService.delete(d);
                } catch (Exception e) {
                    LOG.warn("Fehler beim Löschen von fehlendem Dokument {}", d.getPk());
                }
                return true;
            }
            return false;
        });

        Set<String> existingNames = existing.stream()
                .map(DocumentDataObject::getFileName)
                .filter(n -> n != null)
                .collect(Collectors.toSet());

        try (Stream<Path> stream = Files.walk(inbox)) {
            List<Path> pdfs = stream
                    .filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());


            int count = 0;
            for (Path p : pdfs) {
                try {
                    String fname = p.getFileName().toString();
                    if (existingNames.contains(fname)) {
                        // skip files already present as NEW
                        continue;
                    }

                    DocumentDataObject doc = new DocumentDataObject();
                    doc.setFileName(fname);
                    Path parent = p.toAbsolutePath().normalize().getParent();
                    var baseDir = Path.of(this.fileSystemService.getBaseDir());
                    Path relParent = baseDir.toAbsolutePath().normalize().relativize(parent);
                    doc.setFilePath(relParent.toString().replace(File.separatorChar, '/'));
                    doc.setState(DocumentState.NEW);
                    doc.setFileModifyDate(Files.getLastModifiedTime(p).toInstant());

                    dataAccessService.insert(doc);
                    count++;
                    // add to set so duplicates in the same run are ignored
                    existingNames.add(fname);
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
            var actual = this.getByPk(doc.getPk());

            if(actual.getFilePath().equals("inbox")) {
                throw new IllegalStateException("in den inbox Ordner darf nicht ariviert werden. Bitte wähle einen anderen Ordner ");
            }

            var actualFile = resolvePath(actual.getFilePath(), actual.getFileName());
            var newFile = resolvePath(doc.getFilePath(), doc.getFileName());

            if(!actualFile.equals(newFile)) {
                // copy the file on disk
                if(Files.exists(newFile)) {
                    throw new IllegalStateException("Die Datei newFile existiert bereits: " + newFile.toAbsolutePath().toString());
                }
                Files.copy(actualFile, newFile, StandardCopyOption.COPY_ATTRIBUTES);
            }
            doc.setState(DocumentState.ARCHIVED);
            // DataAccessService.update expects an object previously inserted or loaded
            dataAccessService.update(doc);

            if(!actualFile.equals(newFile)) {
                Files.delete(actualFile);
            }
        } catch (Exception e) {
            LOG.warn("Fehler beim Archivieren von {}: {}", doc, e.toString());
            throw new RuntimeException(e);
        }
    }

    
    /**
     * Löscht ein Dokument anhand seines fachlichen Schlüssels (pk).
     * @return true wenn gelöscht wurde, false wenn nicht gefunden oder Fehler
     */
    @Transactional
    public boolean deleteDocument(String pk) {
        try {
            dataAccessService.delete(this.getByPk(pk));
            return true;
        } catch (Exception e) {
            LOG.warn("Fehler beim Löschen von pk={}: {}", pk, e.toString());
            return false;
        }
    }

    /**
     * Löscht alle NEW-Dokumente aus der DB, deren Datei nicht mehr auf der Platte existiert.
     * Erwartet, dass der Pfad in den Attributes unter dem Schlüssel "path" gespeichert ist.
     * Liefert die Anzahl gelöschter Einträge zurück.
     */
    @Transactional
    public int deleteMissingNewDocuments() {
        List<DocumentDataObject> docs = getAllNewDocuments();
        int deleted = 0;
        for (DocumentDataObject d : docs) {
            try {
                Path path = resolvePath(d.getFilePath(), d.getFileName());
                if (!Files.exists(path)) {
                    dataAccessService.delete(d);
                    deleted++;
                }
            } catch (Exception e) {
                LOG.warn("Fehler beim Prüfen/Löschen von {}: {}", d, e.toString());
            }
        }
        return deleted;
    }

    /**
     * Dearchiviert ein Dokument anhand seines fachlichen Schlüssels (pk).
     * Falls die Datei auf der Platte existiert, wird sie nach basePath/inbox verschoben.
     * Anschließend wird der DB-Eintrag gelöscht.
     *
     * @return true wenn erfolgreich, false wenn nicht gefunden oder Fehler
     */
    @Transactional
    public boolean unarchiveDocument(String pk) {
        DocumentDataObject doc = getByPk(pk);
        if (doc == null) {
            LOG.warn("unarchiveDocument: kein Dokument mit pk={} gefunden", pk);
            return false;
        }
        if(doc.getState() != DocumentState.ARCHIVED) {
            throw new RuntimeException("unarchiveDocument: Dokument mit pk=" + pk + " ist nicht ARCHIVED, sondern " + doc.getState());
        }
        try {
            Path source = resolvePath(doc.getFilePath(), doc.getFileName());
            if (Files.exists(source)) {
                Path inbox = Paths.get(fileSystemService.getBaseDir(), "inbox");
                Files.createDirectories(inbox);
                Path target = inbox.resolve(source.getFileName());
                if (Files.exists(target)) {
                    String originalName = source.getFileName().toString();
                    String guid = java.util.UUID.randomUUID().toString();
                    String newName = guid+"-"+originalName;
                    target = inbox.resolve(newName);
                }

                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            }
            dataAccessService.delete(doc);


            Files.delete(source);
            return true;
        } catch (Exception e) {
            LOG.warn("Fehler beim Dearchivieren von pk={}: {}", pk, e.toString());
            dataAccessService.delete(doc);
            return false;
        }
    }

    private Path resolvePath(String storedFilePath, String fileName) {
        return Paths.get(fileSystemService.getBaseDir(), storedFilePath, fileName);
    }
}
