package com.ConectaAI.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ConectaAI.demo.dto.IntentAnalysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IntentInterpreterService {

    private static final Set<String> SUPPORTED_INTENTS = Set.of(
        "BUY",
        "ADD_TO_ORDER",
        "REMOVE_FROM_ORDER",
        "REPLACE_ORDER",
        "ASK_PRODUCT_INFO",
        "CONFIRM_ORDER",
        "CANCEL_ORDER",
        "PRICE_OBJECTION",
        "ASK_HOURS",
        "GENERAL"
    );

    private final GroqChatClient groqChatClient;
    private final ObjectMapper objectMapper;

    public IntentInterpreterService(GroqChatClient groqChatClient, ObjectMapper objectMapper) {
        this.groqChatClient = groqChatClient;
        this.objectMapper = objectMapper;
    }

    public IntentAnalysis analyze(String message, boolean hasPendingOrder) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();

            messages.add(Map.of(
                "role", "system",
                "content",
                    "Você é um classificador de intenção para um chatbot de atendimento comercial. " +
                    "Sua única tarefa é analisar a mensagem do cliente e retornar APENAS um JSON válido. " +

                    "Intenções possíveis:\n" +
                    "BUY = cliente quer comprar, levar, pedir ou separar produto.\n" +
                    "ADD_TO_ORDER = cliente quer adicionar mais itens a um pedido pendente. Ex: coloca mais 1 chocolate, adiciona um refrigerante.\n" +
                    "REMOVE_FROM_ORDER = cliente quer remover item do pedido pendente. Ex: tira o arroz, remove o chocolate.\n" +
                    "REPLACE_ORDER = cliente quer trocar/substituir o pedido. Ex: troca por feijão, substitui por refrigerante.\n" +
                    "ASK_PRODUCT_INFO = cliente pergunta preço, estoque, disponibilidade ou se vende algum produto.\n" +
                    "CONFIRM_ORDER = cliente confirma um pedido pendente. Ex: sim, sim!, desejo manter sim, pode ser, confirmo, isso mesmo.\n" +
                    "CANCEL_ORDER = cliente cancela ou desiste de um pedido inteiro.\n" +
                    "PRICE_OBJECTION = cliente reclama ou comenta que algo está caro.\n" +
                    "ASK_HOURS = cliente pergunta se está aberto, funcionando ou horário.\n" +
                    "GENERAL = mensagem genérica, saudação, comentário solto ou algo que não se encaixa.\n\n" +

                    "Contexto:\n" +
                    "Existe pedido pendente? " + hasPendingOrder + "\n\n" +

                    "Regras importantes:\n" +
                    "- Se existe pedido pendente e o cliente disser algo como 'sim', 'sim!', 'desejo manter sim', 'quero sim', 'pode ser', classifique como CONFIRM_ORDER.\n" +
                    "- Se o cliente perguntar 'tem arroz?', 'quanto custa o feijão?', 'vocês vendem chocolate?', classifique como ASK_PRODUCT_INFO.\n" +
                    "- Se o cliente disser 'quero arroz', 'vou levar 2 chocolates', 'me separa um refrigerante', classifique como BUY.\n" +
                    "- Se o cliente disser 'caro', 'muito caro', 'que absurdo', classifique como PRICE_OBJECTION.\n\n" +
                    "- Se existe pedido pendente e o cliente disser 'coloca mais', 'adiciona', 'inclui', classifique como ADD_TO_ORDER.\n" +
                    "- Se existe pedido pendente e o cliente disser 'tira', 'remove', 'retira', classifique como REMOVE_FROM_ORDER.\n" +
                    "- Se existe pedido pendente e o cliente disser 'troca por', 'substitui por', classifique como REPLACE_ORDER.\n" +

                    "Formato obrigatório da resposta:\n" +
                    "{\"intent\":\"BUY|ADD_TO_ORDER|REMOVE_FROM_ORDER|REPLACE_ORDER|ASK_PRODUCT_INFO|CONFIRM_ORDER|CANCEL_ORDER|PRICE_OBJECTION|ASK_HOURS|GENERAL\",\"confidence\":0.0}"
            ));

            messages.add(Map.of(
                "role", "user",
                "content", message
            ));

            String content = groqChatClient.complete(
                messages,
                Map.of(
                    "temperature", 0,
                    "max_completion_tokens", 256,
                    "reasoning_effort", "low",
                    "include_reasoning", false,
                    "response_format", Map.of("type", "json_object")
                )
            );

            return parseIntent(content);

        } catch (Exception e) {
            System.out.println("Erro ao interpretar intenção: " + e.getMessage());

            return new IntentAnalysis("GENERAL", 0.0);
        }
    }

    private IntentAnalysis parseIntent(String content) throws JsonProcessingException {
        IntentAnalysis analysis = objectMapper.readValue(content, IntentAnalysis.class);
        String intent = analysis.getIntent();
        String normalizedIntent = intent == null ? "" : intent.trim().toUpperCase(Locale.ROOT);

        if (!SUPPORTED_INTENTS.contains(normalizedIntent)) {
            return new IntentAnalysis("GENERAL", 0.0);
        }

        double confidence = analysis.getConfidence();
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            confidence = 0.0;
        }

        return new IntentAnalysis(normalizedIntent, confidence);
    }
}
