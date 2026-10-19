package com.clubakos.backend.repository;

import com.clubakos.backend.entity.PlanilladoLiga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanilladoLigaRepository extends JpaRepository<PlanilladoLiga, Long> {

    // Para "Ver Planillados Históricos" del usuario
    List<PlanilladoLiga> findByUsuarioIdOrderByFechaRegistroDesc(Long usuarioId);

    // Para la regla: 1 planillado Oficial por mes
    boolean existsByUsuarioIdAndMesCompetenciaAndTipoRegistroAndEstadoIn(
            Long usuarioId, String mesCompetencia, String tipoRegistro, List<String> estados);

    // Para armar el "Ranking de la Liga" (Solo Oficiales Aprobados)
    List<PlanilladoLiga> findByTipoRegistroAndEstado(String tipoRegistro, String estado);
}