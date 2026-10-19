package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = true) // Nullable solo para invitados
    private Usuario usuario;

    @Column(name = "id_invitado_prueba")
    private String idInvitadoPrueba; // Ej: "1-A". Evita romper el usuario_id

    @ManyToOne
    @JoinColumn(name = "horario_id", nullable = false)
    private HorarioClase horario;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "estado_reserva")
    private String estadoReserva;

    @Column(name = "estado_asistencia")
    private String estadoAsistencia;

    private LocalDateTime fechaAvisoEspera;

    @Column(name = "comentario_arquero", columnDefinition = "TEXT")
    private String comentarioArquero;

    @Column(name = "comentario_profesor", columnDefinition = "TEXT")
    private String comentarioProfesor;
}