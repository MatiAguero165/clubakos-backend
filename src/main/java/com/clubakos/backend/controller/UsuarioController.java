package com.clubakos.backend.controller;

import com.clubakos.backend.entity.Notificacion;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.NotificacionRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import com.clubakos.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        String email = credenciales.get("email");
        String pin = credenciales.get("pin");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(404).body("El usuario no existe en la base de datos.");
        }

        Usuario usuario = usuarioOpt.get();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (encoder.matches(pin, usuario.getPassword())) {

            if ("Rechazado".equalsIgnoreCase(usuario.getEstado())) {
                return ResponseEntity.status(403).body("Tu perfil no fue aprobado. Requiere revisión. Por favor contacte al Administrador del Club AKOS.");
            }
            if ("Inactivo".equalsIgnoreCase(usuario.getEstado()) || "Pendiente".equalsIgnoreCase(usuario.getEstadoPerfil())) {
                return ResponseEntity.status(403).body("Tu perfil aún está en revisión. No podrás ingresar a la app hasta que la administración lo apruebe.");
            }

            Map<String, Object> respuesta = new HashMap<>();

            respuesta.put("id", usuario.getId());
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("email", usuario.getEmail());
            respuesta.put("tipo", usuario.getTipoUsuario() != null ? usuario.getTipoUsuario() : "Arquero Invitado");
            respuesta.put("plan", usuario.getPlan() != null ? usuario.getPlan() : "Sin plan");
            respuesta.put("estado", usuario.getEstado() != null ? usuario.getEstado() : "Inactivo");
            respuesta.put("maxClases", usuario.getClasesPorSemana() != null ? usuario.getClasesPorSemana() : 0);

            respuesta.put("dni", usuario.getDni());
            respuesta.put("telefono", usuario.getTelefono());
            respuesta.put("fechaNacimiento", usuario.getFechaNacimiento());
            respuesta.put("nacionalidad", usuario.getNacionalidad());
            respuesta.put("genero", usuario.getGenero());
            respuesta.put("direccion", usuario.getDireccion());
            respuesta.put("ciudadEstado", usuario.getCiudadEstado());
            respuesta.put("codigoPostal", usuario.getCodigoPostal());
            respuesta.put("contactoEmergencia", usuario.getContactoEmergencia());
            respuesta.put("parentezco", usuario.getParentezco());
            respuesta.put("ocupacion", usuario.getOcupacion());

            respuesta.put("manoCuerda", usuario.getManoCuerda());
            respuesta.put("librasReales", usuario.getLibrasReales());
            respuesta.put("drawLength", usuario.getDrawLength());
            respuesta.put("spineFlecha", usuario.getSpineFlecha());
            respuesta.put("braceHeight", usuario.getBraceHeight());
            respuesta.put("tiller", usuario.getTiller());
            respuesta.put("setupArco", usuario.getSetupArco());

            respuesta.put("fechaAlta", usuario.getFechaAlta());
            respuesta.put("cuotaActual", usuario.getCuotaActual());
            respuesta.put("clubDondeEntrena", usuario.getClubDondeEntrena());
            respuesta.put("fotoPerfil", usuario.getFotoPerfil());
            respuesta.put("grupoVinculados", usuario.getGrupoVinculados());

            return ResponseEntity.ok(respuesta);
        } else {
            return ResponseEntity.status(401).body("El PIN ingresado es incorrecto.");
        }
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        try {
            if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: El email ya está registrado.");
            }

            usuario.setEstadoPerfil("Pendiente");
            usuario.setEstado("Inactivo");

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            usuario.setPassword(encoder.encode(usuario.getPassword()));

            usuarioRepository.save(usuario);

            List<Usuario> administradores = usuarioRepository.findByTipoUsuario("Administrador AKOS");

            for (Usuario admin : administradores) {
                Notificacion notifAdmin = new Notificacion();
                notifAdmin.setTitulo("📢 Nuevo Registro Pendiente");
                notifAdmin.setMensaje("El usuario " + usuario.getNombre() + " solicita ingresar como " + usuario.getTipoUsuario() + ". Requiere revisión.");
                notifAdmin.setUsuarioDestinoId(admin.getId());
                notifAdmin.setTipo("ALERTA_USUARIO");
                notifAdmin.setEstado("Enviado");

                notificacionRepository.save(notifAdmin);
            }

            String emailAdmin = "info@clubakos.com";
            String asuntoAdmin = "NUEVA SOLICITUD: " + usuario.getNombre() + " (" + usuario.getTipoUsuario() + ")";
            String cuerpoAdmin = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px;'>"
                    + "<h2 style='color: #ff7f00;'>Nueva Solicitud de Ingreso</h2>"
                    + "<p>El usuario <b>" + usuario.getNombre() + "</b> ha creado una cuenta.</p>"
                    + "<p>Ingresa al panel de administración para revisar sus datos, asignarle un plan y aprobar su perfil.</p>"
                    + "</div>";
            emailService.enviarCorreoHtml(emailAdmin, asuntoAdmin, cuerpoAdmin);

            String asuntoUsuario = "¡Hemos recibido tu solicitud! - Club AKOS";
            String cuerpoUsuario = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px;'>"
                    + "<h2 style='color: #ff7f00;'>¡Hola " + usuario.getNombre() + "!</h2>"
                    + "<p>Hemos recibido tus datos correctamente.</p>"
                    + "<p>Tu perfil está siendo revisado por la administración. Te avisaremos por este medio en cuanto sea aprobado para que puedas ingresar a la aplicación.</p>"
                    + "</div>";
            emailService.enviarCorreoHtml(usuario.getEmail(), asuntoUsuario, cuerpoUsuario);

            return ResponseEntity.ok("Registro completado con éxito.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el registro: " + e.getMessage());
        }
    }

    @PostMapping("/recuperar-pin")
    public ResponseEntity<?> recuperarPin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Debes ingresar un email válido.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.trim());

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Si el email existe, se ha enviado el PIN.");
        }

        Usuario usuario = usuarioOpt.get();

        String nuevoPin = String.format("%04d", new java.util.Random().nextInt(10000));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        usuario.setPassword(encoder.encode(nuevoPin));
        usuarioRepository.save(usuario);

        String asunto = "Recuperación de PIN - Club AKOS";
        String cuerpo = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px;'>"
                + "<h2 style='color: #ff7f00;'>Hola " + usuario.getNombre() + "</h2>"
                + "<p>Has solicitado recuperar tu contraseña para la App de AKOS.</p>"
                + "<p>Tu nuevo PIN de acceso es: <br><br><b style='font-size: 28px; color: #1a1a1a;'>" + nuevoPin + "</b></p>"
                + "<p>Por favor, guarda este código en un lugar seguro.</p>"
                + "<p>Saludos,<br><b>Administración Club AKOS</b></p>"
                + "</div>";

        try {
            emailService.enviarCorreoHtml(usuario.getEmail(), asunto, cuerpo);
            return ResponseEntity.ok("¡Listo! Se envió la contraseña al mail registrado.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al intentar enviar el correo.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> obtenerUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        usuarios.sort((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
        return ResponseEntity.ok(usuarios);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarUsuario(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return ResponseEntity.badRequest().body("Usuario no encontrado.");

        String estadoAnterior = usuario.getEstado();

        // Datos Principales
        if (datos.containsKey("nombre")) usuario.setNombre((String) datos.get("nombre"));
        if (datos.containsKey("email")) usuario.setEmail((String) datos.get("email"));

        if (datos.containsKey("pin") && datos.get("pin") != null && !datos.get("pin").toString().trim().isEmpty()) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            usuario.setPassword(encoder.encode(datos.get("pin").toString().trim()));
        }

        // Roles y Estado
        if (datos.containsKey("tipoUsuario")) usuario.setTipoUsuario((String) datos.get("tipoUsuario"));
        if (datos.containsKey("plan")) usuario.setPlan((String) datos.get("plan"));
        if (datos.containsKey("estado")) usuario.setEstado((String) datos.get("estado"));
        if (datos.containsKey("clasesPorSemana")) usuario.setClasesPorSemana(Integer.parseInt(datos.get("clasesPorSemana").toString()));
        if (datos.containsKey("grupoVinculados")) usuario.setGrupoVinculados((String) datos.get("grupoVinculados"));

        // Datos Personales
        if (datos.containsKey("dni")) usuario.setDni((String) datos.get("dni"));
        if (datos.containsKey("fechaNacimiento")) usuario.setFechaNacimiento((String) datos.get("fechaNacimiento"));
        if (datos.containsKey("nacionalidad")) usuario.setNacionalidad((String) datos.get("nacionalidad"));
        if (datos.containsKey("genero")) usuario.setGenero((String) datos.get("genero"));
        if (datos.containsKey("telefono")) usuario.setTelefono((String) datos.get("telefono"));
        if (datos.containsKey("direccion")) usuario.setDireccion((String) datos.get("direccion"));
        if (datos.containsKey("ciudadEstado")) usuario.setCiudadEstado((String) datos.get("ciudadEstado"));
        if (datos.containsKey("codigoPostal")) usuario.setCodigoPostal((String) datos.get("codigoPostal"));
        if (datos.containsKey("contactoEmergencia")) usuario.setContactoEmergencia((String) datos.get("contactoEmergencia"));
        if (datos.containsKey("parentezco")) usuario.setParentezco((String) datos.get("parentezco"));
        if (datos.containsKey("ocupacion")) usuario.setOcupacion((String) datos.get("ocupacion"));

        // ¡LA LÍNEA FALTANTE!
        if (datos.containsKey("fotoPerfil")) usuario.setFotoPerfil((String) datos.get("fotoPerfil"));

        // Setup del Arco
        if (datos.containsKey("manoCuerda")) usuario.setManoCuerda((String) datos.get("manoCuerda"));
        if (datos.containsKey("librasReales")) usuario.setLibrasReales((String) datos.get("librasReales"));
        if (datos.containsKey("drawLength")) usuario.setDrawLength((String) datos.get("drawLength"));
        if (datos.containsKey("spineFlecha")) usuario.setSpineFlecha((String) datos.get("spineFlecha"));
        if (datos.containsKey("braceHeight")) usuario.setBraceHeight((String) datos.get("braceHeight"));
        if (datos.containsKey("tiller")) usuario.setTiller((String) datos.get("tiller"));
        if (datos.containsKey("setupArco")) usuario.setSetupArco((String) datos.get("setupArco"));

        // Finanzas y Club
        if (datos.containsKey("cuotaActual")) {
            String cuotaLimpia = datos.get("cuotaActual").toString().replaceAll("[^0-9]", "");
            usuario.setCuotaActual(cuotaLimpia);
        }

        String nuevoEstado = usuario.getEstado();
        if (estadoAnterior != null && !estadoAnterior.equals(nuevoEstado)) {

            if ("Activo".equals(nuevoEstado)) {
                usuario.setEstadoPerfil("Aprobado");

                if (usuario.getFechaAlta() == null || usuario.getFechaAlta().isEmpty()) {
                    usuario.setFechaAlta(java.time.LocalDate.now().toString());
                }

                if (usuario.getPlan() != null) {
                    String planLimpio = usuario.getPlan().toLowerCase();

                    if (planLimpio.contains("x1")) usuario.setClasesPorSemana(1);
                    else if (planLimpio.contains("x2")) usuario.setClasesPorSemana(2);
                    else if (planLimpio.contains("federado")) usuario.setClasesPorSemana(6);
                    else if (planLimpio.contains("invitado")) usuario.setClasesPorSemana(0);

                    List<Usuario> todos = usuarioRepository.findAll();
                    String cuotaEncontrada = "0";
                    for (Usuario u : todos) {
                        if ("Activo".equalsIgnoreCase(u.getEstado())
                                && u.getPlan() != null
                                && u.getPlan().equalsIgnoreCase(usuario.getPlan())
                                && u.getCuotaActual() != null
                                && !u.getCuotaActual().isEmpty()
                                && !u.getCuotaActual().equals("0")) {
                            cuotaEncontrada = u.getCuotaActual();
                            break;
                        }
                    }
                    usuario.setCuotaActual(cuotaEncontrada);
                }

                String asunto = "¡Bienvenido a Club AKOS! - Perfil Aprobado";
                String cuerpo = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px;'>"
                        + "<h2 style='color: #2ecc71;'>¡Tu perfil ha sido aprobado!</h2>"
                        + "<p>Hola <b>" + usuario.getNombre() + "</b>, ya puedes ingresar a la app de Club AKOS.</p>"
                        + "<p>Tu plan asignado es: <b>" + (usuario.getPlan() != null ? usuario.getPlan() : "No especificado") + "</b></p>"
                        + "<p>A partir de este momento puedes reservar clases y hacer uso de todos los beneficios de tu plan.</p>"
                        + "</div>";
                emailService.enviarCorreoHtml(usuario.getEmail(), asunto, cuerpo);

            } else if ("Rechazado".equals(nuevoEstado)) {
                usuario.setEstadoPerfil("Rechazado");

                String asunto = "Aviso sobre tu registro - Club AKOS";
                String cuerpo = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px;'>"
                        + "<h2 style='color: #e74c3c;'>Atención con tu registro</h2>"
                        + "<p>Hola <b>" + usuario.getNombre() + "</b>, tu perfil no fue aprobado. Requiere revisión.</p>"
                        + "<p>Por favor contacte al Administrador del Club AKOS para más información.</p>"
                        + "</div>";
                emailService.enviarCorreoHtml(usuario.getEmail(), asunto, cuerpo);
            }
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return ResponseEntity.ok(usuarioActualizado);
    }
}