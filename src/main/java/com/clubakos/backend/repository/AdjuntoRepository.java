package com.clubakos.backend.repository;

import com.clubakos.backend.entity.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {
}
