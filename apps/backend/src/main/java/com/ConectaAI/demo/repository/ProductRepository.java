package com.ConectaAI.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ConectaAI.demo.model.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findAllByOrderByIdAsc();

    List<ProductEntity> findAllByAtivoTrueOrderByIdAsc();

    Optional<ProductEntity> findByNomeIgnoreCase(String nome);
}
