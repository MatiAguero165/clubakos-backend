package com.clubakos.backend.repository;

import com.clubakos.backend.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Busca mensajes fijos de la sección "Información Útil"
    List<Notificacion> findByTipoOrderByFechaEnvioDesc(String tipo);

    // Busca notificaciones pendientes de un usuario específico
    List<Notificacion> findByUsuarioDestinoIdAndEstado(Long usuarioDestinoId, String estado);

    // NUEVO: Busca notificaciones por Tipo y Estado (Para traer las ALERTA_ADMIN pendientes a los profesores)
    List<Notificacion> findByTipoAndEstado(String tipo, String estado);
}