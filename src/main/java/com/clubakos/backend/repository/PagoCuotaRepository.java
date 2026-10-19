package com.clubakos.backend.repository;

import com.clubakos.backend.entity.PagoCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoCuotaRepository extends JpaRepository<PagoCuota, Long> {

    // 1. Obtener todo el historial de pagos de un usuario específico por su ID
    List<PagoCuota> findByUsuarioId(Long usuarioId);

    // 2. NUEVO: Verifica si el usuario ya pagó un mes específico para evitar morosidad (Ej: "Septiembre 2026")
    boolean existsByUsuarioIdAndMesAnioCubierto(Long usuarioId, String mesAnioCubierto);
}