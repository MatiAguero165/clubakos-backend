package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    // --- ROLES Y ESTADO ---
    @Column(name = "tipo_usuario")
    private String tipoUsuario; // "Socio AKOS", "Arquero Invitado", "Admin", etc.

    private String plan;
    private String estado;

    @Column(name = "clases_por_semana")
    private Integer clasesPorSemana;

    @Column(name = "creditos_recupero")
    private Integer creditosRecupero;

    // --- DATOS PERSONALES ---
    private String dni;
    private String direccion;
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private String fechaNacimiento;

    private String nacionalidad;
    private String genero;

    @Column(name = "ciudad_estado")
    private String ciudadEstado;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    @Column(name = "contacto_emergencia")
    private String contactoEmergencia;

    private String parentezco;
    private String ocupacion;

    // --- SETUP DEL ARCO ---
    @Column(name = "mano_cuerda")
    private String manoCuerda;

    @Column(name = "libras_reales")
    private String librasReales;

    @Column(name = "draw_length")
    private String drawLength;

    @Column(name = "spine_flecha")
    private String spineFlecha;

    @Column(name = "brace_height")
    private String braceHeight;

    private String tiller;

    @Column(name = "setup_arco", columnDefinition = "TEXT")
    private String setupArco;

    // --- DATOS DEL CLUB ---
    @Column(name = "fecha_alta")
    private String fechaAlta;

    @Column(name = "cuota_actual")
    private String cuotaActual;

    @Column(name = "pago_mes_corriente")
    private String pagoMesCorriente;

    @Column(name = "club_donde_entrena")
    private String clubDondeEntrena;

    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(name = "grupo_vinculados", columnDefinition = "TEXT")
    private String grupoVinculados;

    // --- NUEVOS CAMPOS EXCLUSIVOS PARA EL REGISTRO REFINADO ---

    // CONTACTO DE EMERGENCIA DETALLADO
    @Column(name = "emergencia_nombre")
    private String emergenciaNombre;

    @Column(name = "emergencia_parentesco")
    private String emergenciaParentesco;

    @Column(name = "emergencia_telefono")
    private String emergenciaTelefono;

    // ESTADO DE APROBACIÓN DEL PERFIL
    @Column(name = "estado_perfil")
    private String estadoPerfil = "Pendiente"; // Por defecto, todos entran en espera
}