package com.example.finalprojectappraisal.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class PdfDocument {

    private String documentId;
    private String fileName;
    private String url;
    @ServerTimestamp
    private Date timestamp;

    public PdfDocument() {
        // בנאי ריק נדרש ל-Firebase
    }

    public PdfDocument(String documentId, String fileName, String url, Date timestamp) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.url = url;
        this.timestamp = timestamp;
    }

    // getters
    public String getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    // setters
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}