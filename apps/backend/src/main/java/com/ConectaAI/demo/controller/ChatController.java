package com.ConectaAI.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ConectaAI.demo.dto.ChatRequest;
import com.ConectaAI.demo.dto.ChatResponse;
import com.ConectaAI.demo.service.ChatService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        System.out.println("Customer ID: " + request.getCustomerId());
        System.out.println("Mensagem: " + request.getMessage());
        return chatService.processMessage(request.getCustomerId(), request.getMessage());
    }
}