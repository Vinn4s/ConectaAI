package com.ConectaAI.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

class GroqChatClientTest {

    private static final String CHAT_COMPLETIONS_URL =
        "https://api.groq.com/openai/v1/chat/completions";

    @Test
    void completeReturnsAssistantTextFromGroqJsonResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo(CHAT_COMPLETIONS_URL))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(
                """
                {
                  "id": "chatcmpl-test",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "Resposta de teste",
                      "reasoning": "campo adicional ignorado"
                    },
                    "finish_reason": "stop"
                  }],
                  "usage": {"prompt_tokens": 4, "completion_tokens": 3}
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        GroqChatClient client = new GroqChatClient(
            "test-api-key",
            "openai/gpt-oss-20b",
            new ObjectMapper(),
            restTemplate
        );

        String result = client.complete(List.of(Map.of(
            "role", "user",
            "content", "Oi"
        )));

        assertEquals("Resposta de teste", result);
        mockServer.verify();
    }
}
