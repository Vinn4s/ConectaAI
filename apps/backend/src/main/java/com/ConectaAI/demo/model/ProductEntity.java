package com.ConectaAI.demo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private int preco;

    @Column(name = "unidade_singular", nullable = false)
    private String unidadeSingular;

    @Column(name = "unidade_plural", nullable = false)
    private String unidadePlural;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_aliases", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "alias", nullable = false)
    private List<String> aliases = new ArrayList<>();

    protected ProductEntity() {
        // Required by JPA.
    }

    public ProductEntity(
        String nome,
        int preco,
        String unidadeSingular,
        String unidadePlural,
        List<String> aliases
    ) {
        this.nome = nome;
        this.preco = preco;
        this.unidadeSingular = unidadeSingular;
        this.unidadePlural = unidadePlural;
        this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public int getPreco() { return preco; }
    public String getUnidadeSingular() { return unidadeSingular; }
    public String getUnidadePlural() { return unidadePlural; }
    public List<String> getAliases() { return List.copyOf(aliases); }
}
