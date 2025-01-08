package com.owlvation.project.genedu.Tool.ChatAi;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private boolean isTyping;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
        this.isTyping = false;
    }

    public ChatMessage(boolean isTyping) {
        this.message = "Typing...";
        this.isUser = false;
        this.isTyping = isTyping;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean isTyping() {
        return isTyping;
    }
}
