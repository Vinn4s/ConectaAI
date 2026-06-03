package com.ConectaAI.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ConectaAI.demo.dto.AdminConfigResponse;
import com.ConectaAI.demo.service.CompanyConfigService;
import com.ConectaAI.demo.service.ProductCatalogService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    private final CompanyConfigService companyConfigService;
    private final ProductCatalogService productCatalogService;

    public AdminConfigController(
        CompanyConfigService companyConfigService,
        ProductCatalogService productCatalogService
    ) {
        this.companyConfigService = companyConfigService;
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public AdminConfigResponse getConfig() {
        return new AdminConfigResponse(
            companyConfigService.getEmpresaInfo(),
            companyConfigService.getHorarioFuncionamento(),
            productCatalogService.getListaProdutos()
        );
    }
}
