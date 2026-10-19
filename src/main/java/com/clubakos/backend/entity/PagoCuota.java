package com.clubakos.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "pagos_cuotas")
public class PagoCuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación directa con el alumno (puede estar vacío si es un ingreso general como Merchandising)
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String concepto; // Ej: "Cuotas Socios", "Pago Clases de Prueba", etc.

    @Column(name = "metodo_pago")
    private String metodoPago; // Ej: "Efectivo", "Transferencia", etc.

    private BigDecimal monto;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro; // Fecha exacta del comprobante/pago

    @Column(name = "mes_anio_cubierto")
    private String mesAnioCubierto; // Fundamental para desbloquear reservas (Ej: "Septiembre 2026")
}