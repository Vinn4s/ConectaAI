package com.ConectaAI.demo.dto;

import java.util.List;

import com.ConectaAI.demo.model.ProductEntity;

public record AdminProductResponse(
    Long id,
    String nome,
    int preco,
    String unidadeSingular,
    String unidadePlural,
    List<String> aliases,
    boolean ativo
) {
    public static AdminProductResponse from(ProductEntity product) {
        return new AdminProductResponse(
            product.getId(),
            product.getNome(),
            product.getPreco(),
            product.getUnidadeSingular(),
            product.getUnidadePlural(),
            product.getAliases(),
            product.isAtivo()
        );
    }
}
