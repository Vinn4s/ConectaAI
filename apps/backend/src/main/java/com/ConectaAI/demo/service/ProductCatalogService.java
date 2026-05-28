package com.ConectaAI.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ConectaAI.demo.model.Produto;

@Service
public class ProductCatalogService {

    private final Map<String, Produto> produtos = criarCatalogo();

    private Map<String, Produto> criarCatalogo() {
        Map<String, Produto> catalogo = new LinkedHashMap<>();

        catalogo.put("arroz", new Produto(
            "arroz",
            20,
            "pacote",
            "pacotes",
            List.of("arroz")
        ));

        catalogo.put("feijão", new Produto(
            "feijão",
            8,
            "pacote",
            "pacotes",
            List.of("feijão", "feijao")
        ));

        catalogo.put("refrigerante", new Produto(
            "refrigerante",
            6,
            "unidade",
            "unidades",
            List.of("refrigerante", "refrigerantes", "refri")
        ));

        catalogo.put("chocolate", new Produto(
            "chocolate",
            5,
            "unidade",
            "unidades",
            List.of("chocolate", "chocolates")
        ));

        return Collections.unmodifiableMap(catalogo);
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
}