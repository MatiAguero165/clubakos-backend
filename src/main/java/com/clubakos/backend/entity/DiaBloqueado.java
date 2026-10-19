package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "dias_bloqueados")
@Data
public class DiaBloqueado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha; // El día que el club estará cerrado
}