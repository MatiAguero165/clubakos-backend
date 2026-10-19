package com.clubakos.backend.controller;

import com.clubakos.backend.entity.DiaBloqueado;
import com.clubakos.backend.repository.DiaBloqueadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/calendario/admin")
public class CalendarioAdminController {

    @Autowired
    private DiaBloqueadoRepository diaBloqueadoRepository;

    @PostMapping("/bloqueo")
    @Transactional
    public ResponseEntity<?> toggleBloqueoDia(@RequestParam String fecha, @RequestParam String accion) {
        LocalDate fechaObj = LocalDate.parse(fecha);

        if ("bloquear".equalsIgnoreCase(accion)) {
            if (!diaBloqueadoRepository.existsByFecha(fechaObj)) {
                DiaBloqueado bloqueado = new DiaBloqueado();
                bloqueado.setFecha(fechaObj);
                diaBloqueadoRepository.save(bloqueado);
            }
            return ResponseEntity.ok("Día bloqueado exitosamente. Ya nadie podrá anotarse.");
        } else {
            diaBloqueadoRepository.deleteByFecha(fechaObj);
            return ResponseEntity.ok("Día desbloqueado. Las reservas vuelven a estar permitidas.");
        }
    }
}