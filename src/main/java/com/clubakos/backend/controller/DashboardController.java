package com.clubakos.backend.controller;

import com.clubakos.backend.entity.Gasto;
import com.clubakos.backend.entity.PagoCuota;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.entity.Notificacion;
import com.clubakos.backend.repository.GastoRepository;
import com.clubakos.backend.repository.PagoCuotaRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import com.clubakos.backend.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private PagoCuotaRepository pagoCuotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private com.clubakos.backend.service.EmailService emailService;

    private final String[] mesesNombres = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    // ==========================================
    // 1. GASTOS (EGRESOS) - CRUD
    // ==========================================
    @PostMapping("/gastos")
    public ResponseEntity<?> registrarGasto(@RequestBody Gasto gasto) {
        if (gasto.getFecha() == null || gasto.getMonto() == null)
            return ResponseEntity.badRequest().body("Fecha y monto son obligatorios.");
        gasto.setMesContable(mesesNombres[gasto.getFecha().getMonthValue() - 1] + " " + gasto.getFecha().getYear());
        return ResponseEntity.ok(gastoRepository.save(gasto));
    }

    @PutMapping("/gastos/{id}")
    public ResponseEntity<?> editarGasto(@PathVariable Long id, @RequestBody Gasto gastoActualizado) {
        Gasto gastoExistente = gastoRepository.findById(id).orElse(null);
        if (gastoExistente == null) return ResponseEntity.badRequest().body("Gasto no encontrado.");
        gastoExistente.setConcepto(gastoActualizado.getConcepto());
        gastoExistente.setMonto(gastoActualizado.getMonto());
        gastoExistente.setFecha(gastoActualizado.getFecha());
        gastoExistente.setMesContable(mesesNombres[gastoActualizado.getFecha().getMonthValue() - 1] + " " + gastoActualizado.getFecha().getYear());
        return ResponseEntity.ok(gastoRepository.save(gastoExistente));
    }

    @DeleteMapping("/gastos/{id}")
    public ResponseEntity<?> eliminarGasto(@PathVariable Long id) {
        if (!gastoRepository.existsById(id)) return ResponseEntity.badRequest().body("Gasto no encontrado.");
        gastoRepository.deleteById(id);
        return ResponseEntity.ok("Gasto eliminado correctamente.");
    }

    // ==========================================
    // 2. INGRESOS (COBROS) - CRUD
    // ==========================================
    @PostMapping("/ingresos")
    public ResponseEntity<?> registrarIngreso(@RequestBody Map<String, Object> payload) {
        try {
            PagoCuota pago = new PagoCuota();
            pago.setConcepto((String) payload.get("concepto"));
            pago.setMetodoPago((String) payload.get("metodoPago"));
            pago.setMonto(new BigDecimal(payload.get("monto").toString()));
            pago.setFechaRegistro(LocalDate.parse((String) payload.get("fecha")));

            if (payload.get("usuarioId") != null) {
                Long usuarioId = Long.valueOf(payload.get("usuarioId").toString());
                pago.setUsuario(usuarioRepository.findById(usuarioId).orElse(null));
            }

            if (payload.get("mesAnioCubierto") != null) {
                pago.setMesAnioCubierto((String) payload.get("mesAnioCubierto"));
            } else {
                pago.setMesAnioCubierto(mesesNombres[pago.getFechaRegistro().getMonthValue() - 1] + " " + pago.getFechaRegistro().getYear());
            }

            return ResponseEntity.ok(pagoCuotaRepository.save(pago));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar el ingreso: " + e.getMessage());
        }
    }

    @PutMapping("/ingresos/{id}")
    public ResponseEntity<?> editarIngreso(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            PagoCuota pago = pagoCuotaRepository.findById(id).orElse(null);
            if (pago == null) return ResponseEntity.badRequest().body("Ingreso no encontrado.");

            pago.setConcepto((String) payload.get("concepto"));
            pago.setMetodoPago((String) payload.get("metodoPago"));
            pago.setMonto(new BigDecimal(payload.get("monto").toString()));
            pago.setFechaRegistro(LocalDate.parse((String) payload.get("fecha")));

            if (payload.get("usuarioId") != null) {
                Long usuarioId = Long.valueOf(payload.get("usuarioId").toString());
                pago.setUsuario(usuarioRepository.findById(usuarioId).orElse(null));
            } else {
                pago.setUsuario(null);
            }

            if (payload.get("mesAnioCubierto") != null) {
                pago.setMesAnioCubierto((String) payload.get("mesAnioCubierto"));
            } else {
                pago.setMesAnioCubierto(mesesNombres[pago.getFechaRegistro().getMonthValue() - 1] + " " + pago.getFechaRegistro().getYear());
            }

            return ResponseEntity.ok(pagoCuotaRepository.save(pago));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al editar el ingreso: " + e.getMessage());
        }
    }

    @DeleteMapping("/ingresos/{id}")
    public ResponseEntity<?> eliminarIngreso(@PathVariable Long id) {
        if (!pagoCuotaRepository.existsById(id)) return ResponseEntity.badRequest().body("Ingreso no encontrado.");
        pagoCuotaRepository.deleteById(id);
        return ResponseEntity.ok("Ingreso eliminado correctamente.");
    }

    // ==========================================
    // 3. ENVIAR RECORDATORIO A MOROSOS
    // ==========================================
    @PostMapping("/recordatorios")
    public ResponseEntity<?> enviarRecordatorios(@RequestBody List<Long> idsMorosos) {
        for (Long id : idsMorosos) {
            Usuario usuarioMoroso = usuarioRepository.findById(id).orElse(null);

            if (usuarioMoroso != null) {
                // 1. Mensaje Interno (App)
                Notificacion alerta = new Notificacion();
                alerta.setTipo("ALERTA_USUARIO");
                alerta.setUsuarioDestinoId(id);
                alerta.setTitulo("Aviso de Cuota Pendiente");
                alerta.setMensaje("Hola " + usuarioMoroso.getNombre() + ". Te recordamos que registras cuotas pendientes de pago. Por favor regulariza tu situación para poder seguir reservando clases.");
                alerta.setEstado("Enviado");
                notificacionRepository.save(alerta);

                // 2. Correo Electrónico
                if (usuarioMoroso.getEmail() != null && !usuarioMoroso.getEmail().isEmpty()) {
                    String asunto = "Club AKOS - Aviso de Cuota Pendiente";
                    String cuerpo = "¡Hola " + usuarioMoroso.getNombre() + "!\n\n" +
                            "Esperamos que te encuentres muy bien.\n\n" +
                            "Te escribimos desde la administración del Club AKOS para recordarte que registras saldo pendiente en tu cuenta.\n" +
                            "Por favor, regulariza tu situación a la brevedad para evitar que el sistema bloquee tus próximas reservas de clases.\n\n" +
                            "Si ya realizaste el pago, por favor envíanos el comprobante y desestima este mensaje.\n\n" +
                            "Saludos cordiales,\nEquipo AKOS.";
                    emailService.enviarEmail(usuarioMoroso.getEmail(), asunto, cuerpo);
                }
            }
        }
        return ResponseEntity.ok("Recordatorios y correos enviados exitosamente.");
    }

    // ==========================================
    // 4. OBTENER RESUMEN FINANCIERO MENSUAL (CON LÓGICA DE MOROSIDAD EXACTA)
    // ==========================================
    @GetMapping("/resumen/{mesContable}")
    public ResponseEntity<?> obtenerResumenMensual(@PathVariable String mesContable) {

        List<Gasto> gastosDelMes = gastoRepository.findByMesContable(mesContable);
        BigDecimal totalEgresos = gastosDelMes.stream().map(Gasto::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PagoCuota> todosLosPagos = pagoCuotaRepository.findAll();
        BigDecimal totalIngresos = BigDecimal.ZERO;
        List<PagoCuota> ingresosDelMes = new ArrayList<>();

        for (PagoCuota pago : todosLosPagos) {
            if (pago.getFechaRegistro() != null) {
                String mesPago = mesesNombres[pago.getFechaRegistro().getMonthValue() - 1] + " " + pago.getFechaRegistro().getYear();
                if (mesPago.equals(mesContable)) {
                    totalIngresos = totalIngresos.add(pago.getMonto() != null ? pago.getMonto() : BigDecimal.ZERO);
                    ingresosDelMes.add(pago);
                }
            }
        }

        // --- C. Datos Demográficos y Cálculo de Morosidad Blindado ---
        long sociosActivos = 0;
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        List<Map<String, Object>> morosos = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (Usuario u : todosLosUsuarios) {
            String estado = u.getEstado();
            String tipo = u.getTipoUsuario();

            if (estado != null && tipo != null && "Activo".equalsIgnoreCase(estado) && tipo.contains("Socio")) {
                sociosActivos++;

                // 1. Traductor Definitivo de Cuota
                BigDecimal valorCuota = BigDecimal.ZERO;
                try {
                    if (u.getCuotaActual() != null && !u.getCuotaActual().trim().isEmpty()) {
                        String strVal = u.getCuotaActual().replaceAll("[^0-9.,]", "");
                        if (strVal.contains(",")) strVal = strVal.substring(0, strVal.indexOf(","));
                        strVal = strVal.replace(".", "");
                        if (!strVal.isEmpty()) valorCuota = new BigDecimal(strVal);
                    }
                } catch (Exception e) {
                    System.out.println("Error parseando cuota para: " + u.getNombre());
                }

                // --- NUEVO: Validar Fecha de Alta ---
                LocalDate fechaAltaUsuario = null;
                if (u.getFechaAlta() != null && !u.getFechaAlta().isEmpty()) {
                    try {
                        fechaAltaUsuario = LocalDate.parse(u.getFechaAlta());
                    } catch (Exception e) {
                        System.out.println("No se pudo parsear fecha alta de " + u.getNombre());
                    }
                }

                List<String> mesesAtrasados = new ArrayList<>();
                BigDecimal deudaTotal = BigDecimal.ZERO;

                // 2. Regla: Controlar el MES ANTERIOR
                LocalDate mesPasado = hoy.minusMonths(1);
                String mesPasadoStr = mesesNombres[mesPasado.getMonthValue() - 1] + " " + mesPasado.getYear();

                // Solo le cobramos el mes pasado si se inscribió ANTES o DURANTE el mes pasado
                boolean aplicaMesPasado = false;
                if (fechaAltaUsuario == null) {
                    aplicaMesPasado = true; // Si no hay fecha, cobramos por seguridad
                } else if (fechaAltaUsuario.isBefore(hoy.withDayOfMonth(1))) {
                    aplicaMesPasado = true; // Si se inscribió antes del 1ro de este mes
                }

                if (aplicaMesPasado && !pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(u.getId(), mesPasadoStr)) {
                    mesesAtrasados.add(mesPasadoStr);
                    deudaTotal = deudaTotal.add(valorCuota);
                }

                // 3. Regla: Controlar el MES ACTUAL (Solo si estamos a día 11 o más)
                if (hoy.getDayOfMonth() >= 11) {
                    String mesActualStr = mesesNombres[hoy.getMonthValue() - 1] + " " + hoy.getYear();

                    // Solo le cobramos este mes si se inscribió ESTE mes o ANTES
                    boolean aplicaMesActual = false;
                    if (fechaAltaUsuario == null) {
                        aplicaMesActual = true;
                    } else if (!fechaAltaUsuario.isAfter(hoy)) {
                        aplicaMesActual = true;
                    }

                    if (aplicaMesActual && !pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(u.getId(), mesActualStr)) {
                        mesesAtrasados.add(mesActualStr + " (Mes en curso)");
                        deudaTotal = deudaTotal.add(valorCuota);
                    }
                }

                // 4. Si debe meses, SE MUESTRA
                if (!mesesAtrasados.isEmpty()) {
                    Map<String, Object> moroso = new HashMap<>();
                    moroso.put("id", u.getId());
                    moroso.put("nombre", u.getNombre());
                    moroso.put("deudaTotal", deudaTotal);
                    moroso.put("mesesEnMora", mesesAtrasados.size());
                    moroso.put("detalleMeses", String.join(", ", mesesAtrasados));
                    morosos.add(moroso);
                }
            }
        }

        // --- D. Empaquetar JSON ---
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mes", mesContable);

        Map<String, Object> egresos = new HashMap<>();
        egresos.put("total", totalEgresos);
        egresos.put("cantidad", gastosDelMes.size());
        egresos.put("detalle", gastosDelMes);
        respuesta.put("egresos", egresos);

        Map<String, Object> ingresos = new HashMap<>();
        ingresos.put("total", totalIngresos);
        ingresos.put("cantidad", ingresosDelMes.size());
        ingresos.put("detalle", ingresosDelMes);
        respuesta.put("ingresos", ingresos);

        respuesta.put("sociosActivos", sociosActivos);
        respuesta.put("morosos", morosos);

        List<Map<String, Object>> usuariosBasicos = new ArrayList<>();
        for (Usuario u : todosLosUsuarios) {
            Map<String, Object> mapUsu = new HashMap<>();
            mapUsu.put("id", u.getId());
            mapUsu.put("nombre", u.getNombre());
            mapUsu.put("plan", u.getPlan());
            usuariosBasicos.add(mapUsu);
        }
        respuesta.put("usuarios", usuariosBasicos);

        return ResponseEntity.ok(respuesta);
    }

    // ==========================================
    // 5. AJUSTE MASIVO DE CUOTAS POR PORCENTAJE
    // ==========================================
    @PutMapping("/ajustar-cuotas")
    public ResponseEntity<?> ajustarCuotasMasivamente(@RequestBody Map<String, Object> payload) {
        try {
            double porcentaje = Double.parseDouble(payload.get("porcentaje").toString());
            String mesImpacto = payload.get("mesImpacto").toString();
            boolean notificar = (Boolean) payload.get("notificar");

            List<Usuario> usuarios = usuarioRepository.findAll();
            int actualizados = 0;

            for (Usuario u : usuarios) {
                // Solo aplicamos a socios Activos
                if ("Activo".equalsIgnoreCase(u.getEstado()) && u.getTipoUsuario() != null && u.getTipoUsuario().contains("Socio")) {

                    // 1. Extraer y limpiar la cuota actual (Ej: "140000" -> 140000.0)
                    String cuotaStr = u.getCuotaActual();
                    if (cuotaStr != null && !cuotaStr.trim().isEmpty()) {
                        cuotaStr = cuotaStr.replaceAll("[^0-9]", ""); // Dejamos solo números
                        if (!cuotaStr.isEmpty()) {
                            double cuotaActual = Double.parseDouble(cuotaStr);

                            // Si la cuota es mayor a 0, aplicamos el aumento
                            if (cuotaActual > 0) {
                                double aumento = cuotaActual * (porcentaje / 100.0);
                                long nuevaCuota = Math.round(cuotaActual + aumento); // Redondeamos para evitar decimales

                                u.setCuotaActual(String.valueOf(nuevaCuota));
                                usuarioRepository.save(u);
                                actualizados++;

                                // 2. Enviar Notificaciones si el admin marcó la casilla
                                if (notificar) {
                                    // Notificación en la App
                                    Notificacion alerta = new Notificacion();
                                    alerta.setTipo("AVISO_ADMINISTRATIVO");
                                    alerta.setUsuarioDestinoId(u.getId());
                                    alerta.setTitulo("Actualización de Cuota");
                                    alerta.setMensaje("Hola " + u.getNombre() + ". Te informamos que a partir del mes de " + mesImpacto + " el valor base de tu cuota será de $" + nuevaCuota + " (sin contemplar descuentos).");
                                    alerta.setEstado("Enviado");
                                    notificacionRepository.save(alerta);

                                    // Email
                                    if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                                        String asunto = "Club AKOS - Actualización de Cuota (" + mesImpacto + ")";
                                        String cuerpo = "¡Hola " + u.getNombre() + "!\n\n" +
                                                "Nos comunicamos desde la administración para informarte que, debido a los ajustes inflacionarios, hemos actualizado los valores de las cuotas del club.\n\n" +
                                                "A partir del mes de " + mesImpacto + ", el valor base de tu plan (sin aplicar descuentos por pago a término o grupo familiar) pasará a ser de: $" + nuevaCuota + ".\n\n" +
                                                "Te agradecemos por seguir siendo parte de Club AKOS.\n\n" +
                                                "Saludos cordiales,\nLa Comisión Directiva.";
                                        emailService.enviarEmail(u.getEmail(), asunto, cuerpo);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return ResponseEntity.ok("Ajuste aplicado con éxito a " + actualizados + " socios activos.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar el ajuste: " + e.getMessage());
        }
    }
}