package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "horarios_clases")
public class HorarioClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sede; // Ej: "Boedo"

    @Column(name = "dia_semana", nullable = false)
    private String diaSemana; // Ej: "Lunes", "Martes"

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio; // Ej: 09:00:00

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin; // Ej: 11:00:00

    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo; // Ej: 10

    private String estado; // Ej: "Activo", "Inactivo"
}