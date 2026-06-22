package com.ConectaAI.demo.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class MessageTemplateService {

    private static final Map<String, Map<String, String>> TEMPLATES_POR_FLOW_TYPE = Map.of(
        HumanHandoffService.FLOW_TYPE_ORDER_FULFILLMENT,
        Map.of(
            "RECEIVED", "Seu pedido foi recebido e será processado em breve. 😊",
            "PREPARING", "Seu pedido está em separação no momento. 😊",
            "OUT_FOR_DELIVERY", "Boa notícia! Seu pedido saiu para entrega. 🛵",
            "DELIVERED", "Seu pedido foi marcado como entregue. 😊 Obrigado por comprar conosco!"
        )
    );

    public Optional<MessageTemplate> buscarMensagem(String flowType, String status) {
        String flowTypeNormalizado = normalizarChave(flowType);
        String statusNormalizado = normalizarChave(status);

        return Optional
            .ofNullable(TEMPLATES_POR_FLOW_TYPE.get(flowTypeNormalizado))
            .map(templatesPorStatus -> templatesPorStatus.get(statusNormalizado))
            .map(mensagem -> new MessageTemplate(flowTypeNormalizado, statusNormalizado, mensagem));
    }

    private String normalizarChave(String chave) {
        return chave == null ? "" : chave.trim().toUpperCase(Locale.ROOT);
    }

    public record MessageTemplate(String flowType, String status, String mensagem) {}
}
