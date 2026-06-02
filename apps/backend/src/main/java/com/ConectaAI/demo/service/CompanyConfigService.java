package com.ConectaAI.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CompanyConfigService {

    private static final String CONFIG_FILE = "empresa-config.json";

    private final EmpresaConfig config;

    public CompanyConfigService(ObjectMapper objectMapper) {
        this.config = carregarConfig(objectMapper);
    }

    private EmpresaConfig carregarConfig(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CONFIG_FILE);

        if (!resource.exists()) {
            throw new IllegalStateException("Arquivo de configuração da empresa não encontrado: " + CONFIG_FILE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            EmpresaConfig empresaConfig = objectMapper.readValue(inputStream, EmpresaConfig.class);
            validarConfig(empresaConfig);
            return empresaConfig;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar os dados da empresa do arquivo " + CONFIG_FILE, e);
        }
    }

    private void validarConfig(EmpresaConfig empresaConfig) {
        if (empresaConfig == null || empresaConfig.empresa == null) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": dados da empresa ausentes.");
        }

        if (isBlank(empresaConfig.empresa.nome)) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": nome da empresa ausente.");
        }

        if (empresaConfig.horarioFuncionamento == null || empresaConfig.horarioFuncionamento.isEmpty()) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": horário de funcionamento ausente.");
        }
    }

    public String getCompanyInfoFormattedForIa() {
        Empresa empresa = config.empresa;
        StringBuilder info = new StringBuilder();
        

        info.append("Empresa:\n")
            .append("- Nome: ").append(empresa.nome).append("\n")
            .append("- Telefone: ").append(valorOuIndisponivel(empresa.telefone)).append("\n")
            .append("- Formas de pagamento: ").append(formatarLista(empresa.formasPagamento)).append("\n")
            .append("- Endereço: ").append(formatarEndereco(empresa.endereco)).append("\n\n")
            .append("Horário de funcionamento:\n");

        for (Map.Entry<String, HorarioFuncionamento> entry : config.horarioFuncionamento.entrySet()) {
            info.append("- ")
                .append(entry.getKey())
                .append(": ")
                .append(formatarHorario(entry.getValue()))
                .append("\n");
        }

        return info.toString().trim();
    }

    public String getRespostaHorarioFuncionamento() {

        ZoneId zoneId = ZoneId.of(valorOuPadrao(config.empresa.timezone, "America/Fortaleza"));

        LocalDate hoje = LocalDate.now(zoneId);
        LocalTime agora = LocalTime.now(zoneId);
        String diaAtual = chaveDiaSemana(hoje.getDayOfWeek());
        HorarioFuncionamento horario = config.horarioFuncionamento.get(diaAtual);

        if (horario == null || isBlank(horario.abertura) || isBlank(horario.fechamento)) {
            return "Hoje não abrimos. Nosso funcionamento está configurado para os demais dias.";
        }

        LocalTime abertura = parseHorario(horario.abertura);
        LocalTime fechamento = parseHorario(horario.fechamento);
        String horarioFormatado = horario.abertura + " às " + horario.fechamento;

        if (estaAbertoAgora(agora, abertura, fechamento)) {
            return "Sim, estamos abertos. Hoje funcionamos das " + horarioFormatado + ".";
        }

        return "No momento estamos fechados. Hoje funcionamos das " + horarioFormatado + ".";
    }

    private String chaveDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "segunda";
            case TUESDAY -> "terca";
            case WEDNESDAY -> "quarta";
            case THURSDAY -> "quinta";
            case FRIDAY -> "sexta";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
    }

    private LocalTime parseHorario(String value) {
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Configuração inválida em " + CONFIG_FILE + ": horário em formato inválido '" + value + "'.",
                e
            );
        }
    }

    private boolean estaAbertoAgora(LocalTime agora, LocalTime abertura, LocalTime fechamento) {
        if (fechamento.isBefore(abertura)) {
            return !agora.isBefore(abertura) || agora.isBefore(fechamento);
        }

        return !agora.isBefore(abertura) && agora.isBefore(fechamento);
    }

    private String formatarEndereco(Endereco endereco) {
        if (endereco == null) {
            return "não informado";
        }

        StringJoiner enderecoFormatado = new StringJoiner(", ");
        adicionarSePresente(enderecoFormatado, endereco.logradouro);
        adicionarSePresente(enderecoFormatado, endereco.numero);
        adicionarSePresente(enderecoFormatado, endereco.bairro);
        adicionarSePresente(enderecoFormatado, endereco.cidade);
        adicionarSePresente(enderecoFormatado, endereco.estado);
        adicionarSePresente(enderecoFormatado, endereco.cep);

        String resultado = enderecoFormatado.toString();
        return resultado.isBlank() ? "não informado" : resultado;
    }

    private void adicionarSePresente(StringJoiner joiner, String value) {
        if (!isBlank(value)) {
            joiner.add(value);
        }
    }

    private String formatarHorario(HorarioFuncionamento horario) {
        if (horario == null || isBlank(horario.abertura) || isBlank(horario.fechamento)) {
            return "fechado";
        }

        return horario.abertura + " às " + horario.fechamento;
    }

    private String formatarLista(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "não informado";
        }

        return String.join(", ", values);
    }

    private String valorOuIndisponivel(String value) {
        return isBlank(value) ? "não informado" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EmpresaConfig {
        public Empresa empresa;
        public Map<String, HorarioFuncionamento> horarioFuncionamento;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Empresa {
        public String nome;
        public String telefone;
        public String timezone;
        public List<String> formasPagamento;
        public Endereco endereco;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Endereco {
        public String logradouro;
        public String numero;
        public String bairro;
        public String cidade;
        public String estado;
        public String cep;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class HorarioFuncionamento {
        public String abertura;
        public String fechamento;
    }

    private String valorOuPadrao(String value, String defaultValue) {
    return isBlank(value) ? defaultValue : value;
}
}
