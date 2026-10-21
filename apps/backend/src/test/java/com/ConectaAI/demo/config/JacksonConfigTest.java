package com.ConectaAI.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class JacksonConfigTest {

    @Test
    void serializesInstantAsIso8601String() throws Exception {
        String json = new JacksonConfig().objectMapper().writeValueAsString(
            Instant.parse("2026-10-20T21:30:00Z")
        );

        assertEquals("\"2026-10-20T21:30:00Z\"", json);
    }
}
