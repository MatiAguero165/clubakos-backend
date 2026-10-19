package com.clubakos.backend.service;

import com.clubakos.backend.entity.Notificacion;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.NotificacionRepository;
import com.clubakos.backend.repository.PagoCuotaRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecordatorioAutomaticoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PagoCuotaRepository pagoCuotaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private EmailService emailService;

    private final String[] mesesNombres = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    // CRON: Segundos, Minutos, Horas, Día del mes, Mes, Día de la semana.
    // "0 0 0 8 * ?" -> Se ejecuta el día 8 de CADA mes a las 00:00 hs (48hs antes del 10)
    @Scheduled(cron = "0 0 0 8 * ?")
    public void enviarAvisosPreventivos() {
        LocalDate hoy = LocalDate.now();
        String mesActualStr = mesesNombres[hoy.getMonthValue() - 1] + " " + hoy.getYear();

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario u : usuarios) {
            // Filtramos solo socios activos
            if ("Activo".equalsIgnoreCase(u.getEstado()) && u.getTipoUsuario() != null && u.getTipoUsuario().contains("Socio")) {

                // Revisamos si ya pagó el mes en curso
                boolean yaPago = pagoCuotaRepository.existsByUsuarioIdAndMesAnioCubierto(u.getId(), mesActualStr);

                if (!yaPago) {
                    // 1. Notificación en la App
                    Notificacion alerta = new Notificacion();
                    alerta.setTipo("ALERTA_PREVENTIVA");
                    alerta.setUsuarioDestinoId(u.getId());
                    alerta.setTitulo("⏳ Vencimiento Próximo");
                    alerta.setMensaje("¡Hola! Te recordamos que en 48hs vence el plazo para abonar la cuota de " + mesActualStr + ". Evita el bloqueo de tus reservas de clases.");
                    alerta.setEstado("Enviado");
                    notificacionRepository.save(alerta);

                    // 2. Envío de Email
                    if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                        String asunto = "Club AKOS - Vencimiento próximo (" + mesActualStr + ")";
                        String cuerpo = "¡Hola " + u.getNombre() + "!\n\n" +
                                "Te escribimos para recordarte que el día 10 vence el plazo para abonar la cuota correspondiente a " + mesActualStr + ".\n" +
                                "Te invitamos a regularizarla a la brevedad para seguir disfrutando de tus clases sin interrupciones y evitar que el sistema bloquee temporalmente tus reservas.\n\n" +
                                "¡Te esperamos en el club!\n\n" +
                                "Saludos cordiales,\nAdministración Club AKOS";

                        emailService.enviarEmail(u.getEmail(), asunto, cuerpo);
                    }
                }
            }
        }
        System.out.println("Robot Ejecutado: Avisos del día 8 enviados para el mes " + mesActualStr);
    }
}