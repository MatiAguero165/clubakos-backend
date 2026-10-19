package com.clubakos.backend.controller;

import com.clubakos.backend.entity.ClasePrueba;
import com.clubakos.backend.entity.HorarioClase;
import com.clubakos.backend.entity.Reserva;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.ClasePruebaRepository;
import com.clubakos.backend.repository.HorarioClaseRepository;
import com.clubakos.backend.repository.ReservaRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import com.clubakos.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reservas")
@CrossOrigin(origins = "*")
public class ReservaAdminController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ClasePruebaRepository clasePruebaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HorarioClaseRepository horarioRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/agendar")
    public ResponseEntity<?> agendarClaseAdmin(@RequestBody Map<String, Object> payload) {
        String tipoClase = (String) payload.get("tipoClase");
        String fecha = (String) payload.get("fecha");
        String horario = (String) payload.get("horario");
        String idObjetivo = String.valueOf(payload.get("idSeleccionado"));

        // RECUPERAMOS LA RECURRENCIA DEL FRONTEND
        String recurrencia = (String) payload.getOrDefault("recurrencia", "Sin recurrencia (Solo esta fecha)");

        String horaInicio = horario.substring(0, 5);
        String sede = horario.contains("Boedo") ? "Boedo" : "Otra Sede";

        HorarioClase horarioObj = horarioRepository.findAll().stream()
                .filter(h -> h.getHoraInicio().toString().startsWith(horaInicio) && h.getSede().equals(sede))
                .findFirst()
                .orElse(null);

        if (horarioObj == null) {
            return ResponseEntity.badRequest().body("Error: El horario seleccionado no existe en la base de datos.");
        }

        if ("Clase de Prueba".equalsIgnoreCase(tipoClase)) {
            Long idClasePrueba = Long.parseLong(idObjetivo.replace("CP_", ""));
            List<ClasePrueba> grupoPrueba = clasePruebaRepository.findByInscripcionId(idClasePrueba);

            if (grupoPrueba.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontró la solicitud de prueba.");
            }

            for (ClasePrueba cp : grupoPrueba) {
                Reserva nuevaReserva = new Reserva();
                nuevaReserva.setIdInvitadoPrueba(cp.getPosicion());
                nuevaReserva.setUsuario(null);
                nuevaReserva.setHorario(horarioObj);
                nuevaReserva.setFecha(LocalDate.parse(fecha));
                nuevaReserva.setEstadoReserva("Agendado");
                reservaRepository.save(nuevaReserva);

                cp.setEstado("Agendado");
                clasePruebaRepository.save(cp);

                if (cp.getEmail() != null && cp.getEmail().contains("@")) {
                    enviarCorreoConfirmacionPrueba(cp.getEmail(), cp.getNombre(), fecha, horario);
                }
            }
            return ResponseEntity.ok("Clase de prueba agendada exitosamente.");

        } else {
            // LÓGICA PARA ALUMNO REGULAR CON RECURRENCIA MÚLTIPLE
            Long idUsuario = Long.parseLong(idObjetivo);
            Usuario alumno = usuarioRepository.findById(idUsuario).orElse(null);

            if (alumno == null) {
                return ResponseEntity.badRequest().body("Error: Alumno no encontrado en la base de datos.");
            }

            LocalDate fechaActualBucle = LocalDate.parse(fecha);
            LocalDate fechaLimite = fechaActualBucle;

            // Calculamos hasta cuándo se debe repetir
            switch (recurrencia) {
                case "M1": fechaLimite = fechaActualBucle.plusMonths(1); break;
                case "M2": fechaLimite = fechaActualBucle.plusMonths(2); break;
                case "M3": fechaLimite = fechaActualBucle.plusMonths(3); break;
                case "M6": fechaLimite = fechaActualBucle.plusMonths(6); break;
                case "M12": fechaLimite = fechaActualBucle.plusMonths(12); break;
            }

            int clasesAgendadas = 0;

            // Bucle: Mientras la fecha a guardar no supere el límite de meses
            while (!fechaActualBucle.isAfter(fechaLimite)) {

                // Medida de seguridad: Si ya tiene clase ese mismo día a esa hora, lo saltamos
                boolean yaExiste = reservaRepository.existsByUsuarioIdAndHorarioIdAndFecha(alumno.getId(), horarioObj.getId(), fechaActualBucle);

                if (!yaExiste) {
                    Reserva nuevaReserva = new Reserva();
                    nuevaReserva.setUsuario(alumno);
                    nuevaReserva.setHorario(horarioObj);
                    nuevaReserva.setFecha(fechaActualBucle);
                    nuevaReserva.setEstadoReserva("Confirmada");
                    reservaRepository.save(nuevaReserva);
                    clasesAgendadas++;
                }

                // Si es solo una fecha, rompemos el bucle en la primera vuelta
                if ("Sin recurrencia (Solo esta fecha)".equals(recurrencia)) {
                    break;
                }

                // Sumamos exactamente 7 días para agendar la siguiente semana
                fechaActualBucle = fechaActualBucle.plusWeeks(1);
            }

            return ResponseEntity.ok("Proceso completado: Se agendaron " + clasesAgendadas + " clase(s) correctamente.");
        }
    }

    private void enviarCorreoConfirmacionPrueba(String email, String nombre, String fecha, String horario) {
        String asunto = "¡Tu Clase de Prueba en AKOS está confirmada!";
        String cuerpoHtml = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e1e8ed; border-radius: 12px; text-align: center;'>"
                + "<h2 style='color: #ff7f00; margin-top: 0;'>Club AKOS de Tiro con Arco</h2>"
                + "<p style='font-size: 15px; color: #2c3e50; text-align: left;'>Estimado/a <strong>" + nombre + "</strong>,</p>"
                + "<p style='font-size: 14px; color: #555; text-align: left; line-height: 1.5;'>"
                + "Nos alegra informarte que hemos reservado un lugar para tu Clase de Prueba en Club AKOS.</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-left: 4px solid #2ecc71; text-align: left; margin: 20px 0;'>"
                + "<p style='margin: 0 0 5px 0;'>📅 <strong>Fecha:</strong> " + fecha + "</p>"
                + "<p style='margin: 0;'>🕒 <strong>Horario y Sede:</strong> " + horario + "</p>"
                + "</div>"
                + "<p style='font-size: 14px; color: #555; text-align: left;'>Por favor, preséntate 10 minutos antes del horario indicado con ropa cómoda.</p>"
                + "<p style='font-size: 13px; color: #7f8c8d; border-top: 1px solid #eee; padding-top: 15px;'>"
                + "¡Te esperamos!<br><strong>Club AKOS de Tiro con Arco</strong></p>"
                + "</div>";

        emailService.enviarCorreoHtml(email, asunto, cuerpoHtml);
    }
}