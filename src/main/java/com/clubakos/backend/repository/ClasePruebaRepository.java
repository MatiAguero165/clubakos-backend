package com.clubakos.backend.repository;

import com.clubakos.backend.entity.ClasePrueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClasePruebaRepository extends JpaRepository<ClasePrueba, Long> {

    // Método necesario para agendar la clase (El que te daba error)
    List<ClasePrueba> findByInscripcionId(Long inscripcionId);

    // Método para la protección anti-fraude (Que ya estabas usando)
    boolean existsByEmailIgnoreCase(String email);

}