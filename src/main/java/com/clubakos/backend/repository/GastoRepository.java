package com.clubakos.backend.repository;

import com.clubakos.backend.entity.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Trae todos los gastos de un mes específico para armar la tarjeta roja del Dashboard
    List<Gasto> findByMesContable(String mesContable);
}