package com.clubakos.backend.repository;

import com.clubakos.backend.entity.DiaBloqueado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface DiaBloqueadoRepository extends JpaRepository<DiaBloqueado, Long> {
    boolean existsByFecha(LocalDate fecha);
    void deleteByFecha(LocalDate fecha);
}