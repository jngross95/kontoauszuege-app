package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DocumentDataObject extends DataObject {

    private String fileName;
    private String filePath;
    private DocumentState state = DocumentState.NEW;
    private Map<String, Object> attributes = new HashMap<>();
    // Zeitstempel der letzten Änderung der Datei (vom Dateisystem)
    private Instant fileModifyDate;
    private String archivDateiname;
    private String archivOrdner;
    private LocalDate datum;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public DocumentState getState() {
        return state;
    }

    public void setState(DocumentState state) {
        this.state = state;
    }

    public Instant getFileModifyDate() {
        return fileModifyDate;
    }

    public void setFileModifyDate(Instant fileModifyDate) {
        this.fileModifyDate = fileModifyDate;
    }

    public String getArchivDateiname() {
        return archivDateiname;
    }

    public void setArchivDateiname(String archivDateiname) {
        this.archivDateiname = archivDateiname;
    }

    public String getArchivOrdner() {
        return archivOrdner;
    }

    public void setArchivOrdner(String archivOrdner) {
        this.archivOrdner = archivOrdner;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }
}
