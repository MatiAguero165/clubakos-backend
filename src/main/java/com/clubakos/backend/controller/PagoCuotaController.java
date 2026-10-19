package com.clubakos.backend.controller;

import com.clubakos.backend.entity.PagoCuota;
import com.clubakos.backend.repository.PagoCuotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoCuotaController {

    @Autowired
    private PagoCuotaRepository pagoCuotaRepository;

    // Obtener todos los pagos registrados en el club
    @GetMapping
    public List<PagoCuota> obtenerTodos() {
        return pagoCuotaRepository.findAll();
    }

    // Obtener los pagos de un usuario específico
    @GetMapping("/usuario/{usuarioId}")
    public List<PagoCuota> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return pagoCuotaRepository.findByUsuarioId(usuarioId);
    }

    // Registrar un nuevo pago de cuota
    @PostMapping
    public ResponseEntity<PagoCuota> registrarPago(@RequestBody PagoCuota pago) {
        if (pago.getFechaRegistro() == null) {
            pago.setFechaRegistro(java.time.LocalDate.now());
        }
        PagoCuota nuevoPago = pagoCuotaRepository.save(pago);
        return ResponseEntity.ok(nuevoPago);
    }
}
