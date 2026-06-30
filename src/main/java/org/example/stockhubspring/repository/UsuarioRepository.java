package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmailAndAtivoTrue(String email);
}
