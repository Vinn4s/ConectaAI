package com.ConectaAI.demo.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ConectaAI.demo.model.HumanHandoffEntity;
import com.ConectaAI.demo.repository.HumanHandoffRepository;

@Service
public class HumanHandoffService {

    public static final String FLOW_TYPE_ORDER_FULFILLMENT = "ORDER_FULFILLMENT";
    public static final String FLOW_TYPE_SALES_TRANSFER = "SALES_TRANSFER";
    public static final String FLOW_TYPE_SCHEDULING = "SCHEDULING";
    public static final String FLOW_TYPE_SERVICE_ORDER = "SERVICE_ORDER";
    public static final String FLOW_TYPE_SUPPORT = "SUPPORT";

    public static final List<String> FLOW_TYPES_PREVISTOS = List.of(
        FLOW_TYPE_ORDER_FULFILLMENT,
        FLOW_TYPE_SALES_TRANSFER,
        FLOW_TYPE_SCHEDULING,
        FLOW_TYPE_SERVICE_ORDER,
        FLOW_TYPE_SUPPORT
    );

    private static final List<String> STATUS_PERMITIDOS = List.of(
        "RECEIVED",
        "PREPARING",
        "OUT_FOR_DELIVERY",
        "DELIVERED"
    );

    private static final Set<String> STATUS_PERMITIDOS_SET = Set.copyOf(STATUS_PERMITIDOS);

    private static final String STATUS_PERMITIDOS_TEXTO = String.join(", ", STATUS_PERMITIDOS);

    private final HumanHandoffRepository humanHandoffRepository;

    public HumanHandoffService(HumanHandoffRepository humanHandoffRepository) {
        this.humanHandoffRepository = humanHandoffRepository;
    }

    @Transactional
    public void registrarClienteAguardandoHumano(String customerId, String resumoPedido) {
        Instant agora = Instant.now();

        humanHandoffRepository.save(
            new HumanHandoffEntity(
                customerId,
                resumoPedido,
                FLOW_TYPE_ORDER_FULFILLMENT,
                agora,
                "RECEIVED",
                agora
            )
        );
    }

    public boolean clienteAguardandoHumano(String customerId) {
        return humanHandoffRepository.existsById(customerId);
    }

    public List<HumanHandoff> listarAtendimentosPendentes() {
        return humanHandoffRepository.findAllByOrderByCriadoEmDesc().stream()
            .map(this::toHumanHandoff)
            .toList();
    }

    @Transactional
    public Optional<HumanHandoff> atualizarStatus(String customerId, String status) {
        String statusNormalizado = normalizarStatus(status);
        validarStatus(statusNormalizado);

        return humanHandoffRepository.findById(customerId)
            .map(handoff -> {
                handoff.atualizarStatus(statusNormalizado, Instant.now());
                return toHumanHandoff(handoff);
            });
    }

    @Transactional
    public void removerCliente(String customerId) {
        if (humanHandoffRepository.existsById(customerId)) {
            humanHandoffRepository.deleteById(customerId);
        }
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

    private HumanHandoff toHumanHandoff(HumanHandoffEntity handoff) {
        return new HumanHandoff(
            handoff.getCustomerId(),
            handoff.getResumoPedido(),
            handoff.getFlowType(),
            handoff.getCriadoEm(),
            handoff.getStatus(),
            handoff.getAtualizadoEm()
        );
    }

    public record HumanHandoff(
        String customerId,
        String resumoPedido,
        String flowType,
        Instant criadoEm,
        String status,
        Instant atualizadoEm
    ) {}
}
