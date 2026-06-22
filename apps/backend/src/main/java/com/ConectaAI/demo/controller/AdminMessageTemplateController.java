package com.ConectaAI.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ConectaAI.demo.service.MessageTemplateService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/message-templates")
public class AdminMessageTemplateController {

    private final MessageTemplateService messageTemplateService;

    public AdminMessageTemplateController(MessageTemplateService messageTemplateService) {
        this.messageTemplateService = messageTemplateService;
    }

    @GetMapping("/preview")
    public ResponseEntity<?> preview(
        @RequestParam String flowType,
        @RequestParam String status
    ) {
        return messageTemplateService.buscarMensagem(flowType, status)
            .<ResponseEntity<?>>map(template -> ResponseEntity.ok(new PreviewResponse(
                template.flowType(),
                template.status(),
                template.mensagem()
            )))
            .orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                    "Template não encontrado para flowType: " + flowType + " e status: " + status
                )));
    }

    public record PreviewResponse(String flowType, String status, String mensagem) {}

    public record ErrorResponse(String erro) {}
}
