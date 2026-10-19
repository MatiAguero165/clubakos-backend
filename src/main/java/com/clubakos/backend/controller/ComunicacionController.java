package com.clubakos.backend.controller;

import com.clubakos.backend.entity.Notificacion;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.NotificacionRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import com.clubakos.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/comunicacion")
@CrossOrigin(origins = "*")
public class ComunicacionController {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    // ==========================================
    // 1. ENVIAR COMUNICADO MASIVO
    // ==========================================
    @PostMapping("/masivo")
    public ResponseEntity<?> enviarComunicacionMasiva(
            @RequestParam("destinatariosTipo") String destinatariosTipo,
            @RequestParam("titulo") String titulo,
            @RequestParam("mensaje") String mensaje,
            // Recibimos como String para evitar fallos de parseo desde FormData
            @RequestParam(value = "enviarApp", defaultValue = "false") String enviarAppStr,
            @RequestParam(value = "enviarEmail", defaultValue = "false") String enviarEmailStr,
            @RequestParam(value = "enviarFijo", defaultValue = "false") String enviarFijoStr,
            @RequestParam(value = "adjunto", required = false) MultipartFile adjunto) {

        try {
            boolean enviarApp = Boolean.parseBoolean(enviarAppStr);
            boolean enviarEmail = Boolean.parseBoolean(enviarEmailStr);
            boolean enviarFijo = Boolean.parseBoolean(enviarFijoStr);

            int enviadosApp = 0;
            int enviadosEmail = 0;
            boolean fijoCreado = false;

            // 1. Crear Mensaje Fijo
            if (enviarFijo) {
                Notificacion fijo = new Notificacion();
                fijo.setTipo("MENSAJE_FIJO");
                fijo.setTitulo(titulo);
                fijo.setMensaje(mensaje);
                fijo.setEstado("Publicado");
                fijo.setFechaEnvio(LocalDateTime.now()); // Asegurar fecha
                notificacionRepository.save(fijo);
                fijoCreado = true;
            }

            if (!enviarApp && !enviarEmail) {
                return ResponseEntity.ok("Comunicado procesado: Mensaje fijado exitosamente.");
            }

            // 2. Procesar Destinatarios
            List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
            List<String> targets = Arrays.asList(destinatariosTipo.split(","));

            for (Usuario u : todosLosUsuarios) {
                String tipoUsu = u.getTipoUsuario() != null ? u.getTipoUsuario() : "";
                String estado = u.getEstado() != null ? u.getEstado() : "";
                String plan = u.getPlan() != null ? u.getPlan() : "";

                if (!"Activo".equalsIgnoreCase(estado)) continue;

                boolean debeRecibir = false;

                if (targets.contains("Todos") && !tipoUsu.toLowerCase().contains("admin")) {
                    debeRecibir = true;
                } else {
                    if (targets.contains("Socios AKOS") && tipoUsu.contains("Socio")) debeRecibir = true;
                    if (targets.contains("Socios Federados AKOS") && (tipoUsu.contains("Federado") || plan.contains("Federado"))) debeRecibir = true;
                    if (targets.contains("Arqueros Invitados") && tipoUsu.contains("Invitado")) debeRecibir = true;
                    if (targets.contains("Clase de Prueba") && tipoUsu.contains("Prueba")) debeRecibir = true;
                }

                if (debeRecibir) {
                    if (enviarApp) {
                        Notificacion alerta = new Notificacion();
                        alerta.setTipo("ALERTA_USUARIO");
                        alerta.setUsuarioDestinoId(u.getId());
                        alerta.setTitulo(titulo);
                        alerta.setMensaje(mensaje);
                        alerta.setEstado("Enviado");
                        alerta.setFechaEnvio(LocalDateTime.now()); // Asegurar fecha
                        notificacionRepository.save(alerta);
                        enviadosApp++;
                    }

                    if (enviarEmail && u.getEmail() != null && u.getEmail().contains("@")) {
                        String firmaHtml = "<br><br><hr><p style='font-size:12px;color:gray;'>Este es un comunicado oficial enviado desde la App del Club AKOS.</p>";
                        // Si hay adjunto, se envía; si no, se maneja internamente en el EmailService
                        if (adjunto != null && !adjunto.isEmpty()) {
                            emailService.enviarCorreoConAdjunto(u.getEmail(), "AKOS: " + titulo, mensaje + firmaHtml, adjunto);
                        } else {
                            emailService.enviarCorreoHtml(u.getEmail(), "AKOS: " + titulo, mensaje + firmaHtml);
                        }
                        enviadosEmail++;
                    }
                }
            }

            String msgResultado = "Comunicado procesado.\n";
            if (fijoCreado) msgResultado += "- Mensaje fijado en portada.\n";
            if (enviarApp) msgResultado += "- " + enviadosApp + " alertas enviadas.\n";
            if (enviarEmail) msgResultado += "- " + enviadosEmail + " e-mails despachados.";

            return ResponseEntity.ok(msgResultado.trim());

        } catch (Exception e) {
            e.printStackTrace(); // Esto imprimirá el error real en la consola de IntelliJ
            return ResponseEntity.status(500).body("Error interno al procesar el comunicado: " + e.getMessage());
        }
    }

    // ... (Mantén los métodos GET y PUT iguales) ...
    @GetMapping("/info-util")
    public ResponseEntity<List<Notificacion>> obtenerMensajesFijos() {
        // Solo traemos los que sigan en estado "Publicado"
        List<Notificacion> fijos = notificacionRepository.findByTipoOrderByFechaEnvioDesc("MENSAJE_FIJO");
        fijos.removeIf(n -> !"Publicado".equalsIgnoreCase(n.getEstado()));
        return ResponseEntity.ok(fijos);
    }

    @GetMapping("/alertas/{usuarioId}")
    public ResponseEntity<List<Notificacion>> obtenerAlertasUsuario(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        List<Notificacion> alertas = notificacionRepository.findByUsuarioDestinoIdAndEstado(usuarioId, "Enviado");
        if (usuario != null && usuario.getTipoUsuario() != null) {
            String rol = usuario.getTipoUsuario().toUpperCase();
            if (rol.contains("ADMIN") || rol.contains("PROFESOR")) {
                List<Notificacion> alertasAdmin = notificacionRepository.findByTipoAndEstado("ALERTA_ADMIN", "Enviado");
                alertas.addAll(alertasAdmin);
            }
        }
        alertas.sort((a, b) -> b.getFechaEnvio().compareTo(a.getFechaEnvio()));
        return ResponseEntity.ok(alertas);
    }

    @PutMapping("/alertas/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable Long id) {
        Notificacion notif = notificacionRepository.findById(id).orElse(null);
        if (notif != null) {
            notif.setEstado("Procesada");
            notificacionRepository.save(notif);
        }
        return ResponseEntity.ok().build();
    }
}