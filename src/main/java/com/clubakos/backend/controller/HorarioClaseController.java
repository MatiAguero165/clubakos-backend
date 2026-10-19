package com.clubakos.backend.controller;

import com.clubakos.backend.entity.HorarioClase;
import com.clubakos.backend.entity.Reserva;
import com.clubakos.backend.entity.DiaBloqueado;
import com.clubakos.backend.repository.HorarioClaseRepository;
import com.clubakos.backend.repository.ReservaRepository;
import com.clubakos.backend.repository.DiaBloqueadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/horarios")
public class HorarioClaseController {

    @Autowired
    private HorarioClaseRepository horarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private DiaBloqueadoRepository diaBloqueadoRepository;

    @GetMapping
    public List<HorarioClase> obtenerTodos() {
        return horarioRepository.findAll();
    }

    @PostMapping
    public HorarioClase crearHorario(@RequestBody HorarioClase horario) {
        return horarioRepository.save(horario);
    }

    // ==========================================
    // NUEVO: ACTUALIZAR CUPO O ESTADO DEL HORARIO
    // ==========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarHorario(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        HorarioClase horario = horarioRepository.findById(id).orElse(null);
        if (horario == null) {
            return ResponseEntity.badRequest().body("Horario no encontrado.");
        }

        if (datos.containsKey("cupoMaximo")) {
            horario.setCupoMaximo(Integer.parseInt(datos.get("cupoMaximo").toString()));
        }
        if (datos.containsKey("estado")) {
            horario.setEstado(datos.get("estado").toString());
        }

        horarioRepository.save(horario);
        return ResponseEntity.ok(horario);
    }

    // ==========================================
    // NUEVO: BLOQUEAR UN DÍA COMPLETO
    // ==========================================
    @PostMapping("/bloquear")
    public ResponseEntity<?> bloquearDia(@RequestBody Map<String, String> datos) {
        try {
            String fechaStr = datos.get("fecha");
            LocalDate fecha = LocalDate.parse(fechaStr);

            // Verificamos que no esté bloqueado ya para evitar duplicados en la base de datos
            boolean yaBloqueado = diaBloqueadoRepository.findAll().stream()
                    .anyMatch(d -> d.getFecha().equals(fecha));

            if (!yaBloqueado) {
                DiaBloqueado nuevoBloqueo = new DiaBloqueado();
                nuevoBloqueo.setFecha(fecha);
                diaBloqueadoRepository.save(nuevoBloqueo);
            }

            return ResponseEntity.ok("Día bloqueado exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el bloqueo del día.");
        }
    }

    // ==========================================
    // LECTURA DEL CALENDARIO (Mantenido intacto)
    // ==========================================
    @GetMapping("/disponibilidad")
    public ResponseEntity<?> obtenerCalendario(
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {

        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);

            List<HorarioClase> horariosBase = horarioRepository.findAll().stream()
                    .filter(h -> "Activo".equalsIgnoreCase(h.getEstado()))
                    .toList();

            List<Reserva> reservasDeLaSemana = reservaRepository.findByFechaBetween(inicio, fin);

            List<String> diasBloqueados = diaBloqueadoRepository.findAll().stream()
                    .map(d -> d.getFecha().toString())
                    .toList();

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("horarios", horariosBase);
            respuesta.put("reservas", reservasDeLaSemana);
            respuesta.put("diasBloqueados", diasBloqueados);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el calendario.");
        }
    }
}