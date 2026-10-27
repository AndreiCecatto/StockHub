package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.model.TipoLoja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmailAndAtivoTrue(String email);

    // Inclui usuarios inativos (o e-mail e unico independentemente do status).
    boolean existsByEmail(String email);

    // Leitura leve (só o tipo) usada em toda página para decidir o menu; a sessão não guarda o tipo.
    @Query("select u.tipoLoja from Usuario u where u.codigo = :codigo")
    Optional<TipoLoja> findTipoLojaByCodigo(Integer codigo);
}
