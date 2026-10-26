package com.ConectaAI.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ConectaAI.demo.dto.AdminProductRequest;
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
        List<ProductEntity> produtosPersistidos = getProdutosAtivos();

        Map<String, Produto> catalogo = new LinkedHashMap<>();
        for (ProductEntity produtoPersistido : produtosPersistidos) {
            Produto produto = toProduto(produtoPersistido);
            if (catalogo.put(produto.nome(), produto) != null) {
                throw new IllegalStateException("Catálogo persistido inválido: produto duplicado '" + produto.nome() + "'.");
            }
        }
        return catalogo;
    }

    /**
     * Initializes the database from the bundled JSON only on a truly empty catalog.
     * An intentionally fully deactivated catalog must remain empty for chat and IA.
     */
    private List<ProductEntity> getProdutosAtivos() {
        if (productRepository.count() == 0) {
            productRepository.saveAll(carregarCatalogoInicial());
        }
        return productRepository.findAllByAtivoTrueOrderByIdAsc();
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

    @Transactional
    public List<ProductEntity> listarProdutosAdministrativos() {
        garantirCatalogoInicializado();
        return productRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public ProductEntity criarProduto(AdminProductRequest request) {
        ProdutoNormalizado produto = validarEPadronizar(request);
        validarNomeDisponivel(produto.nome(), null);
        validarAliasesDisponiveis(produto.aliases(), null);

        return productRepository.save(new ProductEntity(
            produto.nome(),
            produto.preco(),
            produto.unidadeSingular(),
            produto.unidadePlural(),
            produto.aliases()
        ));
    }

    @Transactional
    public Optional<ProductEntity> atualizarProduto(Long id, AdminProductRequest request) {
        ProdutoNormalizado produto = validarEPadronizar(request);
        Optional<ProductEntity> existente = productRepository.findById(id);
        if (existente.isEmpty()) {
            return Optional.empty();
        }

        ProductEntity entity = existente.get();
        validarNomeDisponivel(produto.nome(), entity.getId());
        validarAliasesDisponiveis(produto.aliases(), entity.getId());
        entity.atualizar(
            produto.nome(),
            produto.preco(),
            produto.unidadeSingular(),
            produto.unidadePlural(),
            produto.aliases()
        );
        return Optional.of(productRepository.save(entity));
    }

    @Transactional
    public Optional<ProductEntity> definirAtivacao(Long id, boolean ativo) {
        return productRepository.findById(id).map(produto -> {
            produto.setAtivo(ativo);
            return productRepository.save(produto);
        });
    }

    private void garantirCatalogoInicializado() {
        if (productRepository.count() == 0) {
            productRepository.saveAll(carregarCatalogoInicial());
        }
    }

    private void validarNomeDisponivel(String nome, Long idAtual) {
        productRepository.findByNomeIgnoreCase(nome).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual)) {
                throw new IllegalArgumentException("Já existe um produto com este nome.");
            }
        });
    }

    private ProdutoNormalizado validarEPadronizar(AdminProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dados do produto são obrigatórios.");
        }

        String nome = textoObrigatorio(request.nome(), "Nome");
        String unidadeSingular = textoObrigatorio(request.unidadeSingular(), "Unidade singular");
        String unidadePlural = textoObrigatorio(request.unidadePlural(), "Unidade plural");
        if (request.preco() == null || request.preco() < 0) {
            throw new IllegalArgumentException("Preço deve ser um número maior ou igual a zero.");
        }
        if (request.aliases() == null) {
            throw new IllegalArgumentException("Informe ao menos um alias para o produto.");
        }

        LinkedHashSet<String> aliasesUnicos = new LinkedHashSet<>();
        for (String alias : request.aliases()) {
            if (alias != null && !alias.isBlank()) {
                aliasesUnicos.add(alias.trim().toLowerCase(Locale.ROOT));
            }
        }
        List<String> aliases = List.copyOf(aliasesUnicos);
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um alias para o produto.");
        }

        return new ProdutoNormalizado(nome, request.preco(), unidadeSingular, unidadePlural, aliases);
    }

    private String textoObrigatorio(String valor, String campo) {
        if (isBlank(valor)) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor.trim();
    }

    private void validarAliasesDisponiveis(List<String> aliases, Long idAtual) {
        for (ProductEntity produto : productRepository.findAllByOrderByIdAsc()) {
            if (produto.getId().equals(idAtual)) {
                continue;
            }
            boolean existeConflito = produto.getAliases().stream()
                .map(alias -> alias.toLowerCase(Locale.ROOT))
                .anyMatch(aliases::contains);
            if (existeConflito) {
                throw new IllegalArgumentException("Um ou mais aliases já pertencem a outro produto.");
            }
        }
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

    private record ProdutoNormalizado(
        String nome,
        int preco,
        String unidadeSingular,
        String unidadePlural,
        List<String> aliases
    ) {}
}
