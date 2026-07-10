package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DocumentDataObject extends DataObject {

    private String fileName;
    private DocumentState state = DocumentState.NEW;
    private Map<String, Object> attributes = new HashMap<>();
    // Zeitstempel der letzten Änderung der Datei (vom Dateisystem)
    private Instant fileModifyDate;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public DocumentState getState() {
        return state;
    }

    public void setState(DocumentState state) {
        this.state = state;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Instant getFileModifyDate() {
        return fileModifyDate;
    }

    public void setFileModifyDate(Instant fileModifyDate) {
        this.fileModifyDate = fileModifyDate;
    }
}
