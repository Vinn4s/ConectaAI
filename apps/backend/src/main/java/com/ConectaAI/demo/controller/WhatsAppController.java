package com.ConectaAI.demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@CrossOrigin(origins = "*")
public class WhatsAppController {

    @PostMapping
    public void receberMensagem(@RequestBody Map<String, Object> payload) {
        System.out.println("🔥 MENSAGEM RECEBIDA DO WHATSAPP:");
        System.out.println(payload);
    }
}