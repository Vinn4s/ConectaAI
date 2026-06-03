package com.ConectaAI.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class HumanHandoffService {

    private final Map<String, HumanHandoff> handoffsPendentes = new ConcurrentHashMap<>();

    public void registrarClienteAguardandoHumano(String customerId, String resumoPedido) {
        handoffsPendentes.put(
            customerId,
            new HumanHandoff(customerId, resumoPedido, Instant.now().toString())
        );
    }

    public boolean clienteAguardandoHumano(String customerId) {
        return handoffsPendentes.containsKey(customerId);
    }

    public List<HumanHandoff> listarAtendimentosPendentes() {
        return new ArrayList<>(handoffsPendentes.values());
    }

    public void removerCliente(String customerId) {
        handoffsPendentes.remove(customerId);
    }

    public record HumanHandoff(
        String customerId,
        String resumoPedido,
        String criadoEm
    ) {}
}
