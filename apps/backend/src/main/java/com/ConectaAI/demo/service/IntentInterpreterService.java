package com.ConectaAI.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ConectaAI.demo.dto.IntentAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IntentInterpreterService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentAnalysis analyze(String message, boolean hasPendingOrder) {
        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.1-8b-instant");

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

            body.put("messages", messages);
            body.put("temperature", 0);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

            Map choice = (Map) ((List) response.getBody().get("choices")).get(0);
            Map messageMap = (Map) choice.get("message");

            String content = messageMap.get("content").toString();

            String json = extractJson(content);

            return objectMapper.readValue(json, IntentAnalysis.class);

        } catch (Exception e) {
            System.out.println("Erro ao interpretar intenção: " + e.getMessage());

            return new IntentAnalysis("GENERAL", 0.0);
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");

        if (start >= 0 && end >= start) {
            return content.substring(start, end + 1);
        }

        return "{\"intent\":\"GENERAL\",\"confidence\":0.0}";
    }
}