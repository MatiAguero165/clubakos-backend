package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos_club")
@Data
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha; // Fecha en que ocurrió el gasto
    private String mesContable; // Ej: "Septiembre 2026"
    private String concepto; // "Alquiler", "Luz", "Dianas", etc.

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    private BigDecimal monto;

    // --- LOS DOS CAMPOS CLAVE PARA EL TABLERO ---
    private String metodoPago; // <-- Faltaba este puente para que Spring Boot lo lea
    private String adminRegistra;

    private LocalDateTime fechaRegistro = LocalDateTime.now(); // Marca de tiempo del sistema
}