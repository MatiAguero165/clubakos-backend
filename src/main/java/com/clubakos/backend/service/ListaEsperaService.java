package com.clubakos.backend.service;

import com.clubakos.backend.entity.ClasePrueba;
import com.clubakos.backend.entity.Reserva;
import com.clubakos.backend.entity.Usuario;
import com.clubakos.backend.repository.ClasePruebaRepository;
import com.clubakos.backend.repository.ReservaRepository;
import com.clubakos.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListaEsperaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClasePruebaRepository clasePruebaRepository;

    @Autowired
    private EmailService emailService;

    // Se ejecuta automáticamente cada 60 segundos (1 minuto)
    @Scheduled(fixedRate = 60000)
    public void procesarCascadaEspera() {
        List<Reserva> enEspera = reservaRepository.findAll().stream()
                .filter(r -> "Aviso Espera".equals(r.getEstadoReserva()))
                .toList();

        LocalDateTime ahora = LocalDateTime.now();

        for (Reserva reserva : enEspera) {
            if (reserva.getFechaAvisoEspera() != null && reserva.getFechaAvisoEspera().plusMinutes(15).isBefore(ahora)) {

                reserva.setEstadoReserva("Cancelada");
                reservaRepository.save(reserva);

                List<Reserva> filaEspera = reservaRepository.findAll().stream()
                        .filter(r -> "Lista de Espera".equals(r.getEstadoReserva()) &&
                                r.getFecha().equals(reserva.getFecha()) &&
                                r.getHorario().getId().equals(reserva.getHorario().getId()))
                        .toList();

                if (!filaEspera.isEmpty()) {
                    Reserva siguiente = filaEspera.get(0);
                    siguiente.setEstadoReserva("Aviso Espera");
                    siguiente.setFechaAvisoEspera(ahora);
                    reservaRepository.save(siguiente);

                    String emailDestino = null;
                    if (siguiente.getUsuario() != null) {
                        emailDestino = siguiente.getUsuario().getEmail();
                    } else if (siguiente.getIdInvitadoPrueba() != null) {
                        List<ClasePrueba> pruebas = clasePruebaRepository.findAll();
                        for (ClasePrueba cp : pruebas) {
                            if (siguiente.getIdInvitadoPrueba().equals(cp.getPosicion())) {
                                emailDestino = cp.getEmail(); break;
                            }
                        }
                    }

                    if (emailDestino != null && emailDestino.contains("@")) {
                        String msj = "¡Se liberó un cupo! (Reserva: R" + siguiente.getId() + ") " +
                                "Tienes 15 minutos para confirmarlo en la App. ¡El primero que confirma se lo queda!";
                        emailService.enviarCorreoConAdjunto(emailDestino, "Cupo Liberado - Club AKOS", msj, null);
                    }
                }
            }
        }
    }
}