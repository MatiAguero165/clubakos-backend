package com.clubakos.backend.controller;

import com.clubakos.backend.entity.Notificacion;
import com.clubakos.backend.entity.PlanilladoLiga;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.NotificacionRepository;
import com.clubakos.backend.repository.PagoCuotaRepository;
import com.clubakos.backend.repository.PlanilladoLigaRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/liga")
public class PlanilladoLigaController {

    @Autowired
    private PlanilladoLigaRepository planilladoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoCuotaRepository pagoCuotaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    private final String[] mesesNombres = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    private int obtenerNumeroMes(String mesNombre) {
        for (int i = 0; i < mesesNombres.length; i++) {
            if (mesesNombres[i].equalsIgnoreCase(mesNombre.trim())) return i + 1;
        }
        return 0;
    }

    @GetMapping("/verificar-acceso/{usuarioId}")
    public ResponseEntity<?> verificarAccesoLiga(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return ResponseEntity.badRequest().body("Usuario no encontrado.");

        boolean esStaff = usuario.getTipoUsuario() != null &&
                (usuario.getTipoUsuario().contains("Administrador") || usuario.getTipoUsuario().contains("Profesor"));

        LocalDate hoy = LocalDate.now();
        String mesActualStr = mesesNombres[hoy.getMonthValue() - 1] + " " + hoy.getYear();

        if (!esStaff) {
            LocalDate fechaAltaUsuario = null;
            if (usuario.getFechaAlta() != null && !usuario.getFechaAlta().isEmpty()) {
                try { fechaAltaUsuario = LocalDate.parse(usuario.getFechaAlta()); } catch (Exception e) { }
            }

            LocalDate mesPasado = hoy.minusMonths(1);
            String mesPasadoStr = mesesNombres[mesPasado.getMonthValue() - 1] + " " + mesPasado.getYear();

            boolean aplicaMesPasado = fechaAltaUsuario == null || fechaAltaUsuario.isBefore(hoy.withDayOfMonth(1));
            if (aplicaMesPasado && !pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(usuario.getId(), mesPasadoStr)) {
                return ResponseEntity.badRequest().body("Acceso denegado: Registras una cuota pendiente (" + mesPasadoStr + "). Regulariza tu situación para participar en la Liga.");
            }

            if (hoy.getDayOfMonth() > 10) {
                boolean aplicaMesActual = fechaAltaUsuario == null || !fechaAltaUsuario.isAfter(hoy);
                if (aplicaMesActual && !pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(usuario.getId(), mesActualStr)) {
                    return ResponseEntity.badRequest().body("Acceso denegado: Tu cuota de " + mesActualStr + " se encuentra vencida.");
                }
            }
        }

        boolean yaTienePlanilladoOficial = planilladoRepository.existsByUsuarioIdAndMesCompetenciaAndTipoRegistroAndEstadoIn(
                usuario.getId(), mesActualStr, "LIGA", Arrays.asList("Pendiente", "Aprobado")
        );
        if (yaTienePlanilladoOficial) {
            return ResponseEntity.badRequest().body("Ya has registrado tu puntaje oficial de la Liga correspondiente a este mes (" + mesActualStr + ").");
        }

        return ResponseEntity.ok("Acceso permitido");
    }

    @PostMapping
    public ResponseEntity<?> guardarPlanillado(@RequestBody PlanilladoLiga planillado) {
        Usuario usuario = usuarioRepository.findById(planillado.getUsuario().getId()).orElse(null);
        if (usuario == null) return ResponseEntity.badRequest().body("Usuario no válido.");

        LocalDate hoy = LocalDate.now();
        String mesActualStr = mesesNombres[hoy.getMonthValue() - 1] + " " + hoy.getYear();

        if ("LIGA".equalsIgnoreCase(planillado.getTipoRegistro())) {
            boolean yaTienePlanilladoOficial = planilladoRepository.existsByUsuarioIdAndMesCompetenciaAndTipoRegistroAndEstadoIn(
                    usuario.getId(), mesActualStr, "LIGA", Arrays.asList("Pendiente", "Aprobado")
            );
            if (yaTienePlanilladoOficial) {
                return ResponseEntity.badRequest().body("Ya tienes un planillado Oficial pendiente o aprobado para este mes.");
            }
            planillado.setEstado("Pendiente");
        } else if ("ENTRENAMIENTO".equalsIgnoreCase(planillado.getTipoRegistro())) {
            planillado.setEstado("Aprobado");
        } else {
            return ResponseEntity.badRequest().body("Tipo de registro inválido.");
        }

        planillado.setUsuario(usuario);
        planillado.setMesCompetencia(mesActualStr);
        planillado.setFechaRegistro(hoy);

        PlanilladoLiga guardado = planilladoRepository.save(planillado);

        String mensajeExito;
        if ("LIGA".equalsIgnoreCase(guardado.getTipoRegistro())) {
            String mensajeAdmin = "Hay un nuevo planillado para la Liga del arquero " + usuario.getNombre() + " (ID: P" + guardado.getId() + ").";
            List<Usuario> todos = usuarioRepository.findAll();
            for (Usuario u : todos) {
                if (u.getTipoUsuario() != null && u.getTipoUsuario().contains("Administrador")) {
                    Notificacion notif = new Notificacion();
                    notif.setTitulo("🎯 Nuevo Planillado de Liga");
                    notif.setMensaje(mensajeAdmin);
                    notif.setUsuarioDestinoId(u.getId());
                    notif.setTipo("ALERTA_USUARIO");
                    notif.setEstado("Enviado");
                    notificacionRepository.save(notif);
                }
            }
            mensajeExito = "Planillado guardado con éxito.\n\nRecuerda que hasta no estar aprobado por los administradores, no lo verás en la tabla de posiciones.";
        } else {
            mensajeExito = "Planillado de entrenamiento guardado exitosamente.";
        }

        return ResponseEntity.ok(mensajeExito);
    }

    @GetMapping("/historial/{usuarioId}")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long usuarioId) {
        List<PlanilladoLiga> historial = planilladoRepository.findByUsuarioIdOrderByFechaRegistroDesc(usuarioId);
        return ResponseEntity.ok(historial);
    }

    // ==========================================
    // RUTAS CRÍTICAS PARA LA AUDITORÍA
    // ==========================================
    @GetMapping("/detalle/{id}")
    public ResponseEntity<?> obtenerDetallePlanillado(@PathVariable Long id) {
        PlanilladoLiga planillado = planilladoRepository.findById(id).orElse(null);
        if (planillado == null) return ResponseEntity.badRequest().body("Planillado no encontrado.");
        return ResponseEntity.ok(planillado);
    }

    @PutMapping("/revisar/{id}")
    public ResponseEntity<?> revisarPlanillado(@PathVariable Long id, @RequestParam String estado) {
        PlanilladoLiga planillado = planilladoRepository.findById(id).orElse(null);
        if (planillado == null) return ResponseEntity.badRequest().body("Planillado no encontrado.");
        if (!estado.equals("Aprobado") && !estado.equals("Rechazado")) return ResponseEntity.badRequest().body("Estado inválido.");
        planillado.setEstado(estado);
        planilladoRepository.save(planillado);
        return ResponseEntity.ok("Planillado " + estado.toLowerCase() + " correctamente.");
    }

    @GetMapping("/ranking")
    public ResponseEntity<?> obtenerRanking(
            @RequestParam(required = false) String mesLimite,
            @RequestParam(required = false) String anioLimite) {

        List<PlanilladoLiga> aprobados = planilladoRepository.findByTipoRegistroAndEstado("LIGA", "Aprobado");

        int mesFiltroNum = mesLimite != null ? obtenerNumeroMes(mesLimite) : 12;
        String anioFiltro = anioLimite != null ? anioLimite : String.valueOf(LocalDate.now().getYear());

        List<PlanilladoLiga> filtrados = new ArrayList<>();
        for (PlanilladoLiga p : aprobados) {
            if (p.getMesCompetencia() != null && p.getMesCompetencia().contains(anioFiltro)) {
                String[] partes = p.getMesCompetencia().split(" ");
                int mesPlanillaNum = obtenerNumeroMes(partes[0]);
                if (mesPlanillaNum > 0 && mesPlanillaNum <= mesFiltroNum) {
                    filtrados.add(p);
                }
            }
        }

        Map<String, Map<Long, Map<String, Object>>> tablaCascada = new HashMap<>();

        for (PlanilladoLiga p : filtrados) {
            String modalidad = p.getTipoCampo() != null ? p.getTipoCampo() : "";
            String categoria = p.getCategoria() != null ? p.getCategoria() : "";
            String diana = p.getTipoDiana() != null ? p.getTipoDiana() : "";
            String distancia = p.getDistancia() != null ? p.getDistancia() : "";

            diana = diana.replace(" Recortado", "");
            boolean isAATA = modalidad.contains("5 flechas");
            if (isAATA && (diana.contains("E80") || diana.contains("5 Spot"))) {
                diana = "E80 / 5 Spots";
            }

            boolean isTurca = modalidad.contains("9 flechas") || categoria.contains("Turco");
            if (isTurca) {
                modalidad = "Arquería Turca (Unificada)";
            }

            String llaveCascada = modalidad + " | " + categoria + " | " + diana + " | " + distancia;

            tablaCascada.putIfAbsent(llaveCascada, new HashMap<>());
            Map<Long, Map<String, Object>> arquerosEnCascada = tablaCascada.get(llaveCascada);

            Long uId = p.getUsuario().getId();
            arquerosEnCascada.putIfAbsent(uId, new HashMap<>(Map.of(
                    "usuarioId", uId,
                    "nombre", p.getUsuario().getNombre(),
                    "club", p.getUsuario().getClubDondeEntrena() != null ? p.getUsuario().getClubDondeEntrena() : "Club AKOS",
                    "puntajeTotal", 0,
                    "cantidadX", 0,
                    "cantidad10", 0,
                    "cantidad9", 0,
                    "cantidad5", 0,
                    "isAATA", isAATA,
                    "isTurca", isTurca
            )));

            Map<String, Object> stats = arquerosEnCascada.get(uId);
            stats.put("puntajeTotal", (int) stats.get("puntajeTotal") + p.getPuntajeTotal());
            stats.put("cantidadX", (int) stats.get("cantidadX") + p.getCantidadX());
            stats.put("cantidad10", (int) stats.get("cantidad10") + p.getCantidad10());
            stats.put("cantidad9", (int) stats.get("cantidad9") + p.getCantidad9());
            stats.put("cantidad5", (int) stats.get("cantidad5") + p.getCantidad5());
        }

        List<Map<String, Object>> respuestaFinal = new ArrayList<>();

        for (Map.Entry<String, Map<Long, Map<String, Object>>> entryCascada : tablaCascada.entrySet()) {
            String llave = entryCascada.getKey();
            List<Map<String, Object>> listaArqueros = new ArrayList<>(entryCascada.getValue().values());

            listaArqueros.sort((a, b) -> {
                int ptA = (int) a.get("puntajeTotal");
                int ptB = (int) b.get("puntajeTotal");
                if (ptA != ptB) return Integer.compare(ptB, ptA);

                int xA = (int) a.get("cantidadX");
                int xB = (int) b.get("cantidadX");
                if (xA != xB) return Integer.compare(xB, xA);

                boolean isAATA = (boolean) a.get("isAATA");
                boolean isTurca = (boolean) a.get("isTurca");

                if (isAATA || isTurca) {
                    int c5A = (int) a.get("cantidad5");
                    int c5B = (int) b.get("cantidad5");
                    return Integer.compare(c5B, c5A);
                } else {
                    int c10A = (int) a.get("cantidad10");
                    int c10B = (int) b.get("cantidad10");
                    if (c10A != c10B) return Integer.compare(c10B, c10A);

                    int c9A = (int) a.get("cantidad9");
                    int c9B = (int) b.get("cantidad9");
                    return Integer.compare(c9B, c9A);
                }
            });

            for (int i = 0; i < listaArqueros.size(); i++) {
                listaArqueros.get(i).put("posicion", i + 1);
            }

            Map<String, Object> bloqueCascada = new HashMap<>();
            bloqueCascada.put("tituloCategoria", llave);
            bloqueCascada.put("esAataOTurca", listaArqueros.isEmpty() ? false : ((boolean) listaArqueros.get(0).get("isAATA") || (boolean) listaArqueros.get(0).get("isTurca")));
            bloqueCascada.put("ranking", listaArqueros);

            respuestaFinal.add(bloqueCascada);
        }

        respuestaFinal.sort((a, b) -> ((String) a.get("tituloCategoria")).compareTo((String) b.get("tituloCategoria")));

        return ResponseEntity.ok(respuestaFinal);
    }
}