package com.ConectaAI.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ConectaAI.demo.dto.AdminProductRequest;
import com.ConectaAI.demo.dto.AdminProductResponse;
import com.ConectaAI.demo.service.ProductCatalogService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductCatalogService productCatalogService;

    public AdminProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public List<AdminProductResponse> listar() {
        return productCatalogService.listarProdutosAdministrativos().stream()
            .map(AdminProductResponse::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody AdminProductRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminProductResponse.from(productCatalogService.criarProduto(request)));
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody AdminProductRequest request) {
        try {
            return productCatalogService.atualizarProduto(id, request)
                .<ResponseEntity<?>>map(product -> ResponseEntity.ok(AdminProductResponse.from(product)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Produto não encontrado.")));
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        }
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<?> definirAtivacao(@PathVariable Long id, @RequestBody AtualizarAtivacaoRequest request) {
        if (request == null || request.ativo() == null) {
            return badRequest(new IllegalArgumentException("Informe o estado ativo do produto."));
        }
        return productCatalogService.definirAtivacao(id, request.ativo())
            .<ResponseEntity<?>>map(product -> ResponseEntity.ok(AdminProductResponse.from(product)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Produto não encontrado.")));
    }

    private ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    public record AtualizarAtivacaoRequest(Boolean ativo) {}
    public record ErrorResponse(String erro) {}
}
