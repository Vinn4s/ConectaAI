package com.ConectaAI.demo.dto;

import java.util.List;
import java.util.Map;

import com.ConectaAI.demo.model.Produto;
import com.ConectaAI.demo.service.CompanyConfigService.EmpresaInfo;
import com.ConectaAI.demo.service.CompanyConfigService.HorarioFuncionamentoInfo;

public record AdminConfigResponse(
    EmpresaInfo empresa,
    Map<String, HorarioFuncionamentoInfo> horarioFuncionamento,
    List<Produto> produtos
) {}
