package com.owlvation.project.genedu.Note;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class NoteModel {

    private String documentId;
    private String title;
    private String content;
    private String imageUrl;
    private @ServerTimestamp Date timestamp;

    public NoteModel() {

    }

    public NoteModel(String title, String content) {
        this.title = title;
        this.content = content;
        this.timestamp = new Date();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
