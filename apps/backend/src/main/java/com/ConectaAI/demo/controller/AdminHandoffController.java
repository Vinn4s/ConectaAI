package com.ConectaAI.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ConectaAI.demo.service.HumanHandoffService;
import com.ConectaAI.demo.service.HumanHandoffService.HumanHandoff;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/handoffs")
public class AdminHandoffController {

    private final HumanHandoffService humanHandoffService;

    public AdminHandoffController(HumanHandoffService humanHandoffService) {
        this.humanHandoffService = humanHandoffService;
    }

    @GetMapping
    public List<HumanHandoff> listarAtendimentosPendentes() {
        return humanHandoffService.listarAtendimentosPendentes();
    }
}
