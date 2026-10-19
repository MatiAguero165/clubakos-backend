package com.clubakos.backend.repository;

import com.clubakos.backend.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    boolean existsByUsuarioIdAndHorarioIdAndFecha(Long usuarioId, Long horarioId, LocalDate fecha);
    boolean existsByUsuarioIdAndFechaAndEstadoReserva(Long usuarioId, LocalDate fecha, String estadoReserva);
    long countByUsuarioIdAndFechaBetweenAndEstadoReserva(Long usuarioId, LocalDate inicio, LocalDate fin, String estadoReserva);
    long countByHorarioIdAndFechaAndEstadoReserva(Long horarioId, LocalDate fecha, String estadoReserva);
    List<Reserva> findByFechaBetween(LocalDate inicio, LocalDate fin);
}