package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "clase_de_prueba")
@Data
public class ClasePrueba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inscripcionId; // Agrupa al titular y sus acompañantes
    private String posicion; // Ej: "1-A" (Titular), "1-B" (Acompañante)

    private LocalDate fechaRegistro = LocalDate.now();

    private String nombre;
    private String dni;
    private String edad;
    private String email;
    private String telefono;
    private Integer cantidadParticipantes;

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    private String estado; // "Pendiente", "Agendado", "Cancelado"
    private String tipo; // "Titular" o "Acompañante"
}