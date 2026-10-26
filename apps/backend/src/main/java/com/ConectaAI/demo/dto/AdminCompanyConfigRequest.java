package com.ConectaAI.demo.dto;

import java.util.List;
import java.util.Map;

public record AdminCompanyConfigRequest(
    Empresa empresa,
    Map<String, HorarioFuncionamento> horarioFuncionamento
) {
    public record Empresa(
        String nome,
        String telefone,
        String timezone,
        List<String> formasPagamento,
        Endereco endereco
    ) {}

    public record Endereco(
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep
    ) {}

    public record HorarioFuncionamento(String abertura, String fechamento) {}
}
