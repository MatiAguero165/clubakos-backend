package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Data
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo; // "MENSAJE_FIJO" (Info Útil) o "ALERTA_USUARIO"

    private Long usuarioDestinoId; // Queda en null si es un mensaje fijo para todos

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    private String estado; // "Enviado", "Procesada" (Leída)

    private LocalDateTime fechaEnvio = LocalDateTime.now();
}