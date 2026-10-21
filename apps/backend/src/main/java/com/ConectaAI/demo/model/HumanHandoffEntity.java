package com.ConectaAI.demo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "human_handoffs")
public class HumanHandoffEntity {

    @Id
    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Lob
    @Column(name = "resumo_pedido", nullable = false)
    private String resumoPedido;

    @Column(name = "flow_type", nullable = false)
    private String flowType;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private String status;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected HumanHandoffEntity() {
        // Required by JPA.
    }

    public HumanHandoffEntity(
        String customerId,
        String resumoPedido,
        String flowType,
        Instant criadoEm,
        String status,
        Instant atualizadoEm
    ) {
        this.customerId = customerId;
        this.resumoPedido = resumoPedido;
        this.flowType = flowType;
        this.criadoEm = criadoEm;
        this.status = status;
        this.atualizadoEm = atualizadoEm;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getResumoPedido() {
        return resumoPedido;
    }

    public String getFlowType() {
        return flowType;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getStatus() {
        return status;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void atualizarStatus(String status, Instant atualizadoEm) {
        this.status = status;
        this.atualizadoEm = atualizadoEm;
    }
}
