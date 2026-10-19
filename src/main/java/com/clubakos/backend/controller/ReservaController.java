package com.clubakos.backend.controller;

import com.clubakos.backend.entity.HorarioClase;
import com.clubakos.backend.entity.Reserva;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.HorarioClaseRepository;
import com.clubakos.backend.repository.PagoCuotaRepository;
import com.clubakos.backend.repository.ReservaRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private HorarioClaseRepository horarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoCuotaRepository pagoCuotaRepository;

    @PostMapping
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva) {
        HorarioClase horario = horarioRepository.findById(reserva.getHorario().getId()).orElse(null);
        Usuario usuario = usuarioRepository.findById(reserva.getUsuario().getId()).orElse(null);

        if (horario == null || usuario == null) return ResponseEntity.badRequest().body("Horario o Usuario no válido.");

        boolean yaTieneReserva = reservaRepository.existsByUsuarioIdAndHorarioIdAndFecha(usuario.getId(), horario.getId(), reserva.getFecha());
        if (yaTieneReserva) return ResponseEntity.badRequest().body("Error: Ya tienes un lugar reservado para esta clase específica.");

        boolean yaTieneClaseEseDia = reservaRepository.existsByUsuarioIdAndFechaAndEstadoReserva(usuario.getId(), reserva.getFecha(), "Confirmada");
        if (yaTieneClaseEseDia) return ResponseEntity.badRequest().body("Error: Ya tienes una clase confirmada para esta fecha. Solo se permite una reserva diaria.");

        // --- INICIO REGLAS DE MOROSIDAD (BLINDADAS) ---
        LocalDate hoy = LocalDate.now();
        String[] mesesNombres = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

        // Verificamos si el usuario tiene fecha de alta para respetarla
        LocalDate fechaAltaUsuario = null;
        if (usuario.getFechaAlta() != null && !usuario.getFechaAlta().isEmpty()) {
            try {
                fechaAltaUsuario = LocalDate.parse(usuario.getFechaAlta());
            } catch (Exception e) {
                System.out.println("No se pudo parsear fecha alta de " + usuario.getNombre());
            }
        }

        // 1. REGLA MES PASADO
        LocalDate mesPasado = hoy.minusMonths(1);
        String mesPasadoStr = mesesNombres[mesPasado.getMonthValue() - 1] + " " + mesPasado.getYear();

        boolean aplicaMesPasado = false;
        if (fechaAltaUsuario == null) {
            aplicaMesPasado = true; // Si no sabemos cuándo entró, asumimos que debe pagar
        } else if (fechaAltaUsuario.isBefore(hoy.withDayOfMonth(1))) {
            aplicaMesPasado = true; // Solo paga si entró antes del 1ro de este mes
        }

        if (aplicaMesPasado && !pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(usuario.getId(), mesPasadoStr)) {
            return ResponseEntity.badRequest().body("Atención: Registras una cuota pendiente de pago (" + mesPasadoStr + "). Por favor regulariza tu situación para seguir reservando.");
        }

        // 2. REGLA MES EN CURSO (Día 11 en adelante)
        if (hoy.getDayOfMonth() > 10) {
            String mesActualStr = mesesNombres[hoy.getMonthValue() - 1] + " " + hoy.getYear();

            boolean aplicaMesActual = false;
            if (fechaAltaUsuario == null) {
                aplicaMesActual = true;
            } else if (!fechaAltaUsuario.isAfter(hoy)) {
                aplicaMesActual = true; // Aplica si se inscribió hoy o antes
            }

            if (aplicaMesActual && !pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(usuario.getId(), mesActualStr)) {
                return ResponseEntity.badRequest().body("Atención: Registras una cuota pendiente de pago (" + mesActualStr + "). El periodo de gracia venció el día 10. Por favor regulariza tu situación.");
            }
        }
        // --- FIN REGLAS DE MOROSIDAD ---

        int limiteSemanal = usuario.getClasesPorSemana() != null ? usuario.getClasesPorSemana() : 0;
        int creditosRecupero = usuario.getCreditosRecupero() != null ? usuario.getCreditosRecupero() : 0;
        if (limiteSemanal <= 0) return ResponseEntity.badRequest().body("Tu plan actual no te permite reservar clases (Límite: 0).");

        LocalDate inicioSemana = reserva.getFecha().with(DayOfWeek.MONDAY);
        LocalDate finSemana = reserva.getFecha().with(DayOfWeek.SUNDAY);
        long reservasEstaSemana = reservaRepository.countByUsuarioIdAndFechaBetweenAndEstadoReserva(usuario.getId(), inicioSemana, finSemana, "Confirmada");

        boolean usaCredito = false;
        if (reservasEstaSemana >= limiteSemanal) {
            if (creditosRecupero > 0) usaCredito = true;
            else return ResponseEntity.badRequest().body("Has alcanzado tu límite de " + limiteSemanal + " clases para esta semana y no tienes créditos de recupero.");
        }

        long ocupados = reservaRepository.countByHorarioIdAndFechaAndEstadoReserva(horario.getId(), reserva.getFecha(), "Confirmada");
        if (ocupados >= horario.getCupoMaximo()) return ResponseEntity.badRequest().body("La clase está llena. No hay cupos disponibles.");

        reserva.setUsuario(usuario);
        reserva.setEstadoReserva("Confirmada");
        reserva.setEstadoAsistencia("Pendiente");
        Reserva nuevaReserva = reservaRepository.save(reserva);

        if (usaCredito) {
            usuario.setCreditosRecupero(creditosRecupero - 1);
            usuarioRepository.save(usuario);
        }

        return ResponseEntity.ok(nuevaReserva);
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarReserva(@PathVariable Long id, @RequestParam Long idUsuarioSolicitante, @RequestParam(defaultValue = "false") boolean isAdmin) {
        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) return ResponseEntity.badRequest().body("Reserva no encontrada.");

        if (!isAdmin && reserva.getUsuario() != null && !reserva.getUsuario().getId().equals(idUsuarioSolicitante)) {
            return ResponseEntity.badRequest().body("No tienes permiso para cancelar esta reserva.");
        }

        if (reserva.getEstadoReserva().equals("Cancelada")) return ResponseEntity.badRequest().body("La reserva ya se encontraba cancelada.");

        if (!isAdmin) {
            LocalDateTime fechaHoraClase = LocalDateTime.of(reserva.getFecha(), reserva.getHorario().getHoraInicio());
            if (LocalDateTime.now().plusHours(12).isAfter(fechaHoraClase)) {
                return ResponseEntity.badRequest().body("El sistema no permite cancelaciones con menos de 12 horas de anticipación a la clase.");
            }
        }

        reserva.setEstadoReserva("Cancelada");
        reservaRepository.save(reserva);

        Usuario usuario = reserva.getUsuario();
        if (usuario != null) {
            int creditosActuales = usuario.getCreditosRecupero() != null ? usuario.getCreditosRecupero() : 0;
            usuario.setCreditosRecupero(creditosActuales + 1);
            usuarioRepository.save(usuario);
        }

        return ResponseEntity.ok("Reserva cancelada con éxito. Crédito devuelto al alumno.");
    }

    @PutMapping("/{id}/asistencia")
    public ResponseEntity<?> marcarAsistencia(@PathVariable Long id, @RequestParam String estadoAsistencia, @RequestParam(defaultValue = "false") boolean devolverCredito) {
        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) return ResponseEntity.badRequest().body("Reserva no encontrada.");

        reserva.setEstadoAsistencia(estadoAsistencia);
        reservaRepository.save(reserva);

        if (devolverCredito && estadoAsistencia.equals("Falta con Aviso") && reserva.getUsuario() != null) {
            Usuario usuario = reserva.getUsuario();
            int creditosActuales = usuario.getCreditosRecupero() != null ? usuario.getCreditosRecupero() : 0;
            usuario.setCreditosRecupero(creditosActuales + 1);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok("Asistencia registrada como '" + estadoAsistencia + "' y crédito devuelto exitosamente a la cuenta del alumno.");
        }

        return ResponseEntity.ok("Asistencia registrada como '" + estadoAsistencia + "'.");
    }

    @PutMapping("/{id}/comentarios")
    public ResponseEntity<?> guardarComentarios(@PathVariable Long id, @RequestBody Map<String, String> comentarios) {
        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) return ResponseEntity.badRequest().body("Reserva no encontrada.");
        if (comentarios.containsKey("comentarioArquero")) reserva.setComentarioArquero(comentarios.get("comentarioArquero"));
        if (comentarios.containsKey("comentarioProfesor")) reserva.setComentarioProfesor(comentarios.get("comentarioProfesor"));
        reservaRepository.save(reserva);
        return ResponseEntity.ok("Comentarios guardados exitosamente.");
    }
}