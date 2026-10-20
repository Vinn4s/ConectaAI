package com.ConectaAI.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GroqChatClient {

    private static final String CHAT_COMPLETIONS_URL =
        "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroqChatClient(
        @Value("${groq.api.key}") String apiKey,
        @Value("${groq.model}") String model,
        ObjectMapper objectMapper
    ) {
        this(apiKey, model, objectMapper, new RestTemplate());
    }

    GroqChatClient(
        String apiKey,
        String model,
        ObjectMapper objectMapper,
        RestTemplate restTemplate
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public String complete(List<Map<String, String>> messages) {
        return complete(messages, Map.of());
    }

    public String complete(
        List<Map<String, String>> messages,
        Map<String, Object> parameters
    ) {
        Map<String, Object> body = new HashMap<>(parameters);
        body.put("model", model);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            CHAT_COMPLETIONS_URL,
            request,
            String.class
        );

        return extractAssistantContent(response.getBody());
    }

    private String extractAssistantContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Groq returned an empty response body.");
        }

        GroqCompletionResponse response;
        try {
            response = objectMapper.readValue(responseBody, GroqCompletionResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Groq returned an invalid JSON response.", e);
        }

        if (response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Groq returned no completion choices.");
        }

        GroqAssistantMessage message = response.choices().get(0).message();
        if (message == null) {
            throw new IllegalStateException("Groq returned no assistant message.");
        }

        if (message.content() != null && !message.content().isBlank()) {
            return message.content();
        }

        if (message.refusal() != null && !message.refusal().isBlank()) {
            return message.refusal();
        }

        throw new IllegalStateException("Groq returned an empty assistant message.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroqCompletionResponse(List<GroqChoice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroqChoice(GroqAssistantMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroqAssistantMessage(String content, String refusal) {}
}
