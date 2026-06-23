package com.ConectaAI.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ConectaAI.demo.service.HumanHandoffService;
import com.ConectaAI.demo.service.HumanHandoffService.HumanHandoff;
import com.ConectaAI.demo.service.MessageTemplateService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/handoffs")
public class AdminHandoffController {

    private final HumanHandoffService humanHandoffService;
    private final MessageTemplateService messageTemplateService;

    public AdminHandoffController(
        HumanHandoffService humanHandoffService,
        MessageTemplateService messageTemplateService
    ) {
        this.humanHandoffService = humanHandoffService;
        this.messageTemplateService = messageTemplateService;
    }

    @GetMapping
    public List<HumanHandoff> listarAtendimentosPendentes() {
        return humanHandoffService.listarAtendimentosPendentes();
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<?> atualizarStatus(
        @PathVariable String customerId,
        @RequestBody AtualizarStatusRequest request
    ) {
        try {
            String status = request == null ? null : request.status();
            Optional<HumanHandoff> handoffAtualizado =
                humanHandoffService.atualizarStatus(customerId, status);

            if (handoffAtualizado.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(
                        "Atendimento humano pendente não encontrado para customerId: " + customerId
                    ));
            }

            HumanHandoff handoff = handoffAtualizado.get();
            String mensagemCliente = messageTemplateService
                .buscarMensagem(handoff.flowType(), handoff.status())
                .map(MessageTemplateService.MessageTemplate::mensagem)
                .orElse(null);

            return ResponseEntity.ok(AtualizarStatusResponse.from(handoff, mensagemCliente));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(exception.getMessage()));
        }
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> finalizarAtendimento(@PathVariable String customerId) {
        humanHandoffService.removerCliente(customerId);
        return ResponseEntity.noContent().build();
    }

    public record AtualizarStatusRequest(String status) {}

    public record AtualizarStatusResponse(
        String customerId,
        String resumoPedido,
        String flowType,
        String criadoEm,
        String status,
        String atualizadoEm,
        String mensagemCliente
    ) {
        public static AtualizarStatusResponse from(
            HumanHandoff handoff,
            String mensagemCliente
        ) {
            return new AtualizarStatusResponse(
                handoff.customerId(),
                handoff.resumoPedido(),
                handoff.flowType(),
                handoff.criadoEm(),
                handoff.status(),
                handoff.atualizadoEm(),
                mensagemCliente
            );
        }
    }

    public record ErrorResponse(String erro) {}
}
