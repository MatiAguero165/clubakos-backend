package com.clubakos.backend.controller;

import com.clubakos.backend.entity.ClasePrueba;
import com.clubakos.backend.entity.Notificacion;
import com.clubakos.backend.repository.ClasePruebaRepository;
import com.clubakos.backend.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clase-prueba")
@CrossOrigin(origins = "*")
public class ClasePruebaController {

    @Autowired
    private ClasePruebaRepository clasePruebaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // DTO Interno para recibir el JSON anidado desde React Native
    public static class SolicitudDTO {
        public String nombre; public String dni; public String edad;
        public String email; public String telefono; public Integer cantidad;
        public String comentarios;
        public List<AcompananteDTO> acompanantes;
    }
    public static class AcompananteDTO { public String nombre; public String email; }

    @PostMapping("/solicitar")
    public ResponseEntity<?> registrarSolicitud(@RequestBody SolicitudDTO solicitud) {

        // ==========================================
        // 1. CONTROL DE DUPLICADOS (ANTI-FRAUDE)
        // ==========================================

        // Validación del solicitante principal
        if (clasePruebaRepository.existsByEmailIgnoreCase(solicitud.email)) {
            return ResponseEntity.badRequest().body("⛔ Solicitud Rechazada: El correo " + solicitud.email + " ya registra una Clase de Prueba previa.");
        }

        // Validación de los acompañantes extras
        if (solicitud.acompanantes != null) {
            for (int i = 0; i < solicitud.acompanantes.size(); i++) {
                AcompananteDTO acomp = solicitud.acompanantes.get(i);
                if (clasePruebaRepository.existsByEmailIgnoreCase(acomp.email)) {
                    return ResponseEntity.badRequest().body("⛔ Solicitud Rechazada: El acompañante #" + (i + 2) + " (" + acomp.email + ") ya ha tomado una Clase de Prueba previa.");
                }
            }
        }

        // ==========================================
        // 2. GENERACIÓN Y GUARDADO DE LA SOLICITUD
        // ==========================================

        // Si la tabla está vacía, evitamos error de null
        Long maxId = clasePruebaRepository.findAll().stream().mapToLong(ClasePrueba::getInscripcionId).max().orElse(0L);
        Long nuevoInscripcionId = maxId + 1;
        String[] letras = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};

        // Guardar Titular
        ClasePrueba titular = new ClasePrueba();
        titular.setInscripcionId(nuevoInscripcionId);
        titular.setPosicion(nuevoInscripcionId + "-A");
        titular.setNombre(solicitud.nombre);
        titular.setDni(solicitud.dni);
        titular.setEdad(solicitud.edad);
        titular.setEmail(solicitud.email);
        titular.setTelefono(solicitud.telefono);
        titular.setCantidadParticipantes(solicitud.cantidad);
        titular.setComentarios(solicitud.comentarios != null ? solicitud.comentarios : "-");
        titular.setEstado("Pendiente");
        titular.setTipo("Titular");
        clasePruebaRepository.save(titular);

        // Guardar Acompañantes
        if (solicitud.acompanantes != null) {
            for (int i = 0; i < solicitud.acompanantes.size(); i++) {
                AcompananteDTO acomp = solicitud.acompanantes.get(i);
                ClasePrueba extra = new ClasePrueba();
                extra.setInscripcionId(nuevoInscripcionId);
                extra.setPosicion(nuevoInscripcionId + "-" + letras[i + 1]);
                extra.setNombre(acomp.nombre);
                extra.setDni("-");
                extra.setEdad(solicitud.edad);
                extra.setEmail(acomp.email);
                extra.setTelefono(solicitud.telefono);
                extra.setCantidadParticipantes(solicitud.cantidad);
                extra.setComentarios("Acompañante de " + solicitud.nombre);
                extra.setEstado("Pendiente");
                extra.setTipo("Acompañante");
                clasePruebaRepository.save(extra);
            }
        }

        // ==========================================
        // 3. GENERAR NOTIFICACIÓN PARA ADMINISTRADORES
        // ==========================================
        Notificacion notif = new Notificacion();
        notif.setTipo("ALERTA_ADMIN");
        notif.setUsuarioDestinoId(null);
        notif.setTitulo("Nueva Clase de Prueba");
        notif.setMensaje("Nueva solicitud de Clase de Prueba #" + nuevoInscripcionId + " (" + solicitud.nombre + ") para " + solicitud.cantidad + " persona(s).");
        notif.setEstado("Enviado");
        notificacionRepository.save(notif);

        return ResponseEntity.ok("¡Tu solicitud ha sido enviada! Nos pondremos en contacto contigo a la brevedad.");
    }

    // ==========================================
    // NUEVO ENDPOINT: OBTENER SOLICITUDES PARA EL DROPDOWN DE GESTIÓN AVANZADA
    // ==========================================
    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerSolicitudesPendientes() {
        List<ClasePrueba> todas = clasePruebaRepository.findAll();
        List<Map<String, Object>> pendientes = new ArrayList<>();

        for (ClasePrueba cp : todas) {
            // Solo nos interesan los titulares que aún no fueron agendados
            if ("Pendiente".equalsIgnoreCase(cp.getEstado()) && "Titular".equalsIgnoreCase(cp.getTipo())) {
                int extras = cp.getCantidadParticipantes() - 1;
                String sufijo = extras > 0 ? " (+" + extras + " acomp.)" : "";

                Map<String, Object> dto = new HashMap<>();
                dto.put("id", "CP_" + cp.getInscripcionId());
                dto.put("label", "#" + cp.getInscripcionId() + " " + cp.getNombre() + sufijo);
                dto.put("cant", cp.getCantidadParticipantes());

                pendientes.add(dto);
            }
        }
        return ResponseEntity.ok(pendientes);
    }
}