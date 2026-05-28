package com.ConectaAI.demo.model;

import java.util.List;

public record Produto(
    String nome,
    int preco,
    String unidadeSingular,
    String unidadePlural,
    List<String> aliases
) {}