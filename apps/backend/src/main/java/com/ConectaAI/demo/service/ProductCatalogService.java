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
import com.ConectaAI.demo.model.ProductEntity;
import com.ConectaAI.demo.repository.ProductRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProductCatalogService {

    private static final String CONFIG_FILE = "empresa-config.json";

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public ProductCatalogService(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the persisted catalog. The bundled JSON remains only as a bootstrap source
     * for installations that do not yet have products in the database.
     */
    private Map<String, Produto> getCatalogo() {
        List<ProductEntity> produtosPersistidos = productRepository.findAllByOrderByIdAsc();
        if (produtosPersistidos.isEmpty()) {
            produtosPersistidos = productRepository.saveAll(carregarCatalogoInicial());
        }

        Map<String, Produto> catalogo = new LinkedHashMap<>();
        for (ProductEntity produtoPersistido : produtosPersistidos) {
            Produto produto = toProduto(produtoPersistido);
            if (catalogo.put(produto.nome(), produto) != null) {
                throw new IllegalStateException("Catálogo persistido inválido: produto duplicado '" + produto.nome() + "'.");
            }
        }
        return catalogo;
    }

    private List<ProductEntity> carregarCatalogoInicial() {
        ClassPathResource resource = new ClassPathResource(CONFIG_FILE);

        if (!resource.exists()) {
            throw new IllegalStateException("Arquivo de configuração da empresa não encontrado: " + CONFIG_FILE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            EmpresaConfig config = objectMapper.readValue(inputStream, EmpresaConfig.class);
            return criarProdutosPersistidos(config);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar o catálogo de produtos do arquivo " + CONFIG_FILE, e);
        }
    }

    private List<ProductEntity> criarProdutosPersistidos(EmpresaConfig config) {
        if (config == null || config.produtos == null || config.produtos.isEmpty()) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": lista de produtos ausente ou vazia.");
        }

        Map<String, ProductEntity> catalogo = new LinkedHashMap<>();

        for (ProdutoConfig produtoConfig : config.produtos) {
            ProductEntity produto = criarProdutoPersistido(produtoConfig);

            if (catalogo.put(produto.getNome(), produto) != null) {
                throw new IllegalStateException(
                    "Configuração inválida em " + CONFIG_FILE + ": produto duplicado '" + produto.getNome() + "'."
                );
            }
        }

        return new ArrayList<>(catalogo.values());
    }

    private ProductEntity criarProdutoPersistido(ProdutoConfig produtoConfig) {
        if (produtoConfig == null
            || isBlank(produtoConfig.nome)
            || produtoConfig.preco == null
            || isBlank(produtoConfig.unidadeSingular)
            || isBlank(produtoConfig.unidadePlural)
            || produtoConfig.aliases == null
            || produtoConfig.aliases.isEmpty()) {
            throw new IllegalStateException("Configuração inválida em " + CONFIG_FILE + ": produto incompleto.");
        }

        return new ProductEntity(
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
        return Collections.unmodifiableMap(new LinkedHashMap<>(getCatalogo()));
    }

    public List<Produto> getListaProdutos() {
        return new ArrayList<>(getCatalogo().values());
    }

    public String getCatalogoFormatadoParaIa() {
        StringBuilder catalogoFormatado = new StringBuilder();

        for (Produto produto : getCatalogo().values()) {
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

    private Produto toProduto(ProductEntity produtoPersistido) {
        List<String> aliases = produtoPersistido.getAliases();
        if (isBlank(produtoPersistido.getNome())
            || isBlank(produtoPersistido.getUnidadeSingular())
            || isBlank(produtoPersistido.getUnidadePlural())
            || aliases.isEmpty()) {
            throw new IllegalStateException("Catálogo persistido inválido: produto incompleto.");
        }

        return new Produto(
            produtoPersistido.getNome(),
            produtoPersistido.getPreco(),
            produtoPersistido.getUnidadeSingular(),
            produtoPersistido.getUnidadePlural(),
            aliases
        );
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
