package com.ConectaAI.demo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_configurations")
public class CompanyConfigEntity {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String nome;

    private String telefone;
    private String timezone;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_config_payment_methods", joinColumns = @JoinColumn(name = "company_config_id"))
    @OrderColumn(name = "position")
    @Column(name = "payment_method", nullable = false)
    private List<String> formasPagamento = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_config_business_hours", joinColumns = @JoinColumn(name = "company_config_id"))
    @OrderColumn(name = "position")
    private List<HorarioFuncionamentoEmbeddable> horarioFuncionamento = new ArrayList<>();

    protected CompanyConfigEntity() {
        // Required by JPA.
    }

    public CompanyConfigEntity(
        String nome,
        String telefone,
        String timezone,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        List<String> formasPagamento,
        List<HorarioFuncionamentoEmbeddable> horarioFuncionamento
    ) {
        this.nome = nome;
        this.telefone = telefone;
        this.timezone = timezone;
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.cep = cep;
        this.formasPagamento = formasPagamento == null ? new ArrayList<>() : new ArrayList<>(formasPagamento);
        this.horarioFuncionamento = horarioFuncionamento == null ? new ArrayList<>() : new ArrayList<>(horarioFuncionamento);
    }

    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getTimezone() { return timezone; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
    public String getCep() { return cep; }
    public List<String> getFormasPagamento() { return List.copyOf(formasPagamento); }
    public List<HorarioFuncionamentoEmbeddable> getHorarioFuncionamento() { return List.copyOf(horarioFuncionamento); }

    @Embeddable
    public static class HorarioFuncionamentoEmbeddable {

        @Column(name = "dia_semana", nullable = false)
        private String diaSemana;

        @Column(name = "abertura")
        private String abertura;

        @Column(name = "fechamento")
        private String fechamento;

        protected HorarioFuncionamentoEmbeddable() {
            // Required by JPA.
        }

        public HorarioFuncionamentoEmbeddable(String diaSemana, String abertura, String fechamento) {
            this.diaSemana = diaSemana;
            this.abertura = abertura;
            this.fechamento = fechamento;
        }

        public String getDiaSemana() { return diaSemana; }
        public String getAbertura() { return abertura; }
        public String getFechamento() { return fechamento; }
    }
}
