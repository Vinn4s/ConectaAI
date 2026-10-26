package com.ConectaAI.demo.dto;

import java.util.List;

public record AdminProductRequest(
    String nome,
    Integer preco,
    String unidadeSingular,
    String unidadePlural,
    List<String> aliases
) {}
