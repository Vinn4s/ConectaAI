package com.ConectaAI.demo.dto;

public class ChatRequest {
    private String customerId;
    private String message;

    public ChatRequest() {
    }

    public ChatRequest(String customerId, String message) {
        this.customerId = customerId;
        this.message = message;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getMessage() {
        return message;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
