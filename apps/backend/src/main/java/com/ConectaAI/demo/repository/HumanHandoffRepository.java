package com.ConectaAI.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ConectaAI.demo.model.HumanHandoffEntity;

public interface HumanHandoffRepository extends JpaRepository<HumanHandoffEntity, String> {

    List<HumanHandoffEntity> findAllByOrderByCriadoEmDesc();
}
