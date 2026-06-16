package com.ConectaAI.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class HumanHandoffService {

    private static final List<String> STATUS_PERMITIDOS = List.of(
        "RECEIVED",
        "PREPARING",
        "OUT_FOR_DELIVERY",
        "DELIVERED"
    );

    private static final Set<String> STATUS_PERMITIDOS_SET = Set.copyOf(STATUS_PERMITIDOS);

    private static final String STATUS_PERMITIDOS_TEXTO = String.join(", ", STATUS_PERMITIDOS);

    private final Map<String, HumanHandoff> handoffsPendentes = new ConcurrentHashMap<>();

    public void registrarClienteAguardandoHumano(String customerId, String resumoPedido) {
        String agora = Instant.now().toString();

        handoffsPendentes.put(
            customerId,
            new HumanHandoff(customerId, resumoPedido, agora, "RECEIVED", agora)
        );
    }

    public boolean clienteAguardandoHumano(String customerId) {
        return handoffsPendentes.containsKey(customerId);
    }

    public List<HumanHandoff> listarAtendimentosPendentes() {
        return new ArrayList<>(handoffsPendentes.values());
    }

    public Optional<HumanHandoff> atualizarStatus(String customerId, String status) {
        String statusNormalizado = normalizarStatus(status);
        validarStatus(statusNormalizado);

        HumanHandoff handoffAtualizado = handoffsPendentes.computeIfPresent(
            customerId,
            (id, handoffAtual) -> new HumanHandoff(
                handoffAtual.customerId(),
                handoffAtual.resumoPedido(),
                handoffAtual.criadoEm(),
                statusNormalizado,
                Instant.now().toString()
            )
        );

        return Optional.ofNullable(handoffAtualizado);
    }

    public void removerCliente(String customerId) {
        handoffsPendentes.remove(customerId);
    }

    private String normalizarStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private void validarStatus(String status) {
        if (status.isBlank()) {
            throw new IllegalArgumentException(
                "Status é obrigatório. Status permitidos: " + STATUS_PERMITIDOS_TEXTO
            );
        }

        if (!STATUS_PERMITIDOS_SET.contains(status)) {
            throw new IllegalArgumentException(
                "Status inválido: " + status + ". Status permitidos: " + STATUS_PERMITIDOS_TEXTO
            );
        }
    }

    public record HumanHandoff(
        String customerId,
        String resumoPedido,
        String criadoEm,
        String status,
        String atualizadoEm
    ) {}
}
