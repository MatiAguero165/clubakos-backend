package com.clubakos.backend.repository;

import com.clubakos.backend.entity.HorarioClase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioClaseRepository extends JpaRepository<HorarioClase, Long> {

    // Busca todas las clases de un día específico (Ej: todos los "Lunes")
    List<HorarioClase> findByDiaSemana(String diaSemana);

    // Busca todas las clases de una sede en un día particular
    List<HorarioClase> findBySedeAndDiaSemana(String sede, String diaSemana);
}
