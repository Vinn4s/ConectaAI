package com.ConectaAI.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ConectaAI.demo.model.Produto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProductCatalogService {

    private static final String CONFIG_FILE = "empresa-config.json";

    private final Map<String, Produto> produtos;

    public ProductCatalogService(ObjectMapper objectMapper) {
        this.produtos = carregarCatalogo(objectMapper);
    }

    private Map<String, Produto> carregarCatalogo(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CONFIG_FILE);

        if (!resource.exists()) {
            throw new IllegalStateException("Arquivo de configuração da empresa não encontrado: " + CONFIG_FILE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            EmpresaConfig config = objectMapper.readValue(inputStream, EmpresaConfig.class);
            return criarCatalogo(config);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar o catálogo de produtos do arquivo " + CONFIG_FILE, e);
        }
    }

    private Map<String, Produto> criarCatalogo(EmpresaConfig config) {
        if (config == null || config.produtos == null || config.produtos.isEmpty()) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": lista de produtos ausente ou vazia.");
        }

        Map<String, Produto> catalogo = new LinkedHashMap<>();

        for (ProdutoConfig produtoConfig : config.produtos) {
            Produto produto = criarProduto(produtoConfig);

            if (catalogo.put(produto.nome(), produto) != null) {
                throw new IllegalStateException(
                    "Configuração inválida em " + CONFIG_FILE + ": produto duplicado '" + produto.nome() + "'."
                );
            }
        }

        return Collections.unmodifiableMap(catalogo);
    }

    private Produto criarProduto(ProdutoConfig produtoConfig) {
        if (produtoConfig == null
            || isBlank(produtoConfig.nome)
            || produtoConfig.preco == null
            || isBlank(produtoConfig.unidadeSingular)
            || isBlank(produtoConfig.unidadePlural)
            || produtoConfig.aliases == null
            || produtoConfig.aliases.isEmpty()) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": produto incompleto.");
        }

        return new Produto(
            produtoConfig.nome,
            produtoConfig.preco,
            produtoConfig.unidadeSingular,
            produtoConfig.unidadePlural,
            List.copyOf(produtoConfig.aliases)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public Map<String, Produto> getProdutos() {
        return produtos;
    }

    public List<Produto> getListaProdutos() {
        return new ArrayList<>(produtos.values());
    }

    public String getCatalogoFormatadoParaIa() {
        StringBuilder catalogoFormatado = new StringBuilder();

        for (Produto produto : produtos.values()) {
            catalogoFormatado
                .append("- ")
                .append(produto.nome())
                .append(": R$ ")
                .append(produto.preco())
                .append(" por ")
                .append(produto.unidadeSingular())
                .append("\n");
        }

        return catalogoFormatado.toString().trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EmpresaConfig {
        public List<ProdutoConfig> produtos;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ProdutoConfig {
        public String nome;
        public Integer preco;
        public String unidadeSingular;
        public String unidadePlural;
        public List<String> aliases;
    }
}
