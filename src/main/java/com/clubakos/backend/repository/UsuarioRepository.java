package com.clubakos.backend.repository;

import com.clubakos.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Al envolverlo en Optional, Spring Data maneja de forma segura
    // los casos donde el email ingresado no existe en la base de datos
    Optional<Usuario> findByEmail(String email);

    // Búsqueda de listas por Rol (ej. para encontrar a todos los Administradores)
    List<Usuario> findByTipoUsuario(String tipoUsuario);
}