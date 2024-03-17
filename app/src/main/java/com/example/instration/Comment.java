package com.example.instration;
public class Comment {

    private String commentText;
    private String authorName;

    public Comment() {
        // Constructor vacío requerido para Firestore
    }

    public Comment(String commentText, String authorName) {
        this.commentText = commentText;
        this.authorName = authorName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }
}
