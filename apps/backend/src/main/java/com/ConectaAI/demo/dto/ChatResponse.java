package com.ConectaAI.demo.dto;

public class ChatResponse {

    private String message;
    private boolean transferToHuman;
    private String type;


    public ChatResponse(String message, boolean transferToHuman, String type) {
        this.message = message;
        this.transferToHuman = transferToHuman;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isTransferToHuman() {
        return transferToHuman;
    }

    public String getType() {
        return type;
    }

}
