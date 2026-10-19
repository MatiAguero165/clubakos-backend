package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "planillados_liga")
@Data
public class PlanilladoLiga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: ¿De quién es esta planilla?
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // ==========================================
    // METADATOS DEL REGISTRO
    // ==========================================
    private String tipoRegistro; // "LIGA" o "ENTRENAMIENTO"
    private String mesCompetencia; // Ej: "Septiembre 2026"
    private LocalDate fechaRegistro;
    private String estado; // "Pendiente", "Aprobado", "Rechazado"

    // ==========================================
    // CONFIGURACIÓN DE TIRO
    // ==========================================
    private String tipoCampo;     // "Indoor", "Aire Libre"
    private String categoria;     // "Recurvo Olimpico", "Raso", "Tradicional", etc.
    private String tipoDiana;     // "E80", "5 Spots", "Arq. Tradicional Turca - PUTA", etc.
    private String distancia;     // "18 mts", "50 mts", etc.
    private int cantidadFlechas;  // 3, 5, 6, 9 (Por tanda)

    // ==========================================
    // PUNTAJES FINALES (Resumen)
    // ==========================================
    private int puntajeS1;
    private int puntajeS2;
    private int puntajeTotal;

    private int cantidadX;
    private int cantidad10;
    private int cantidad9;

    // NUEVO: Para torneos AATA y Turca
    private int cantidad5;
    private int cantidad3;

    // ==========================================
    // DETALLES EXTRAS Y MULTIMEDIA
    // ==========================================
    @Column(columnDefinition = "TEXT")
    private String comentarios;

    // Guardaremos los links separados por comas si suben fotos de la planilla manual
    @Column(columnDefinition = "TEXT")
    private String urlsArchivos;

    // ==========================================
    // EL "CEREBRO" DIGITAL (Heatmap y Tablilla)
    // ==========================================
    // Aquí guardaremos el array completo: [{serie: 1, ronda: 1, str: "X", num: 10, x: 45.5, y: 50.1}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String flechasDetalle;
}