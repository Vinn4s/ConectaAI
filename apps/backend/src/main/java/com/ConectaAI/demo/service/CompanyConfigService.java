package com.ConectaAI.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ConectaAI.demo.model.CompanyConfigEntity;
import com.ConectaAI.demo.model.CompanyConfigEntity.HorarioFuncionamentoEmbeddable;
import com.ConectaAI.demo.repository.CompanyConfigRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CompanyConfigService {

    private static final String CONFIG_FILE = "empresa-config.json";
    private static final long SINGLETON_CONFIG_ID = 1L;

    private final CompanyConfigRepository companyConfigRepository;
    private final ObjectMapper objectMapper;

    public CompanyConfigService(CompanyConfigRepository companyConfigRepository, ObjectMapper objectMapper) {
        this.companyConfigRepository = companyConfigRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the persisted singleton configuration. The bundled JSON is retained only as a
     * bootstrap source for existing deployments that do not yet have a database record.
     */
    private EmpresaConfig getConfig() {
        return companyConfigRepository.findById(SINGLETON_CONFIG_ID)
            .map(this::toConfig)
            .orElseGet(this::criarConfiguracaoInicial);
    }

    private EmpresaConfig criarConfiguracaoInicial() {
        EmpresaConfig configInicial = carregarConfig(objectMapper);
        CompanyConfigEntity configuracaoPersistida = companyConfigRepository.save(toEntity(configInicial));
        return toConfig(configuracaoPersistida);
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
        EmpresaConfig config = getConfig();
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

    public EmpresaInfo getEmpresaInfo() {
        EmpresaConfig config = getConfig();
        Empresa empresa = config.empresa;

        return new EmpresaInfo(
            empresa.nome,
            empresa.telefone,
            empresa.timezone,
            empresa.formasPagamento == null ? List.of() : List.copyOf(empresa.formasPagamento),
            criarEnderecoInfo(empresa.endereco)
        );
    }

    public Map<String, HorarioFuncionamentoInfo> getHorarioFuncionamento() {
        EmpresaConfig config = getConfig();
        Map<String, HorarioFuncionamentoInfo> horarios = new LinkedHashMap<>();

        for (Map.Entry<String, HorarioFuncionamento> entry : config.horarioFuncionamento.entrySet()) {
            HorarioFuncionamento horario = entry.getValue();
            horarios.put(
                entry.getKey(),
                new HorarioFuncionamentoInfo(
                    horario == null ? null : horario.abertura,
                    horario == null ? null : horario.fechamento
                )
            );
        }

        return Collections.unmodifiableMap(horarios);
    }

    private EnderecoInfo criarEnderecoInfo(Endereco endereco) {
        if (endereco == null) {
            return null;
        }

        return new EnderecoInfo(
            endereco.logradouro,
            endereco.numero,
            endereco.bairro,
            endereco.cidade,
            endereco.estado,
            endereco.cep
        );
    }

    public String getRespostaHorarioFuncionamento() {
        EmpresaConfig config = getConfig();

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

    private CompanyConfigEntity toEntity(EmpresaConfig config) {
        Empresa empresa = config.empresa;
        Endereco endereco = empresa.endereco;
        List<HorarioFuncionamentoEmbeddable> horarios = config.horarioFuncionamento.entrySet().stream()
            .map(entry -> {
                HorarioFuncionamento horario = entry.getValue();
                return new HorarioFuncionamentoEmbeddable(
                    entry.getKey(),
                    horario == null ? null : horario.abertura,
                    horario == null ? null : horario.fechamento
                );
            })
            .toList();

        return new CompanyConfigEntity(
            empresa.nome,
            empresa.telefone,
            empresa.timezone,
            endereco == null ? null : endereco.logradouro,
            endereco == null ? null : endereco.numero,
            endereco == null ? null : endereco.bairro,
            endereco == null ? null : endereco.cidade,
            endereco == null ? null : endereco.estado,
            endereco == null ? null : endereco.cep,
            empresa.formasPagamento,
            horarios
        );
    }

    private EmpresaConfig toConfig(CompanyConfigEntity entity) {
        Empresa empresa = new Empresa();
        empresa.nome = entity.getNome();
        empresa.telefone = entity.getTelefone();
        empresa.timezone = entity.getTimezone();
        empresa.formasPagamento = entity.getFormasPagamento();
        empresa.endereco = new Endereco();
        empresa.endereco.logradouro = entity.getLogradouro();
        empresa.endereco.numero = entity.getNumero();
        empresa.endereco.bairro = entity.getBairro();
        empresa.endereco.cidade = entity.getCidade();
        empresa.endereco.estado = entity.getEstado();
        empresa.endereco.cep = entity.getCep();

        EmpresaConfig config = new EmpresaConfig();
        config.empresa = empresa;
        config.horarioFuncionamento = new LinkedHashMap<>();
        for (HorarioFuncionamentoEmbeddable horarioPersistido : entity.getHorarioFuncionamento()) {
            HorarioFuncionamento horario = new HorarioFuncionamento();
            horario.abertura = horarioPersistido.getAbertura();
            horario.fechamento = horarioPersistido.getFechamento();
            config.horarioFuncionamento.put(horarioPersistido.getDiaSemana(), horario);
        }
        validarConfig(config);
        return config;
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

    public record EmpresaInfo(
        String nome,
        String telefone,
        String timezone,
        List<String> formasPagamento,
        EnderecoInfo endereco
    ) {}

    public record EnderecoInfo(
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep
    ) {}

    public record HorarioFuncionamentoInfo(
        String abertura,
        String fechamento
    ) {}

    private String valorOuPadrao(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }
}
