package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.MembroLoja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembroLojaRepository extends JpaRepository<MembroLoja, Integer> {

    Optional<MembroLoja> findByEmailAndAtivoTrue(String email);

    /** Inclui inativos: o e-mail é único independentemente do status. */
    boolean existsByEmail(String email);

    /** Usada a cada requisição para cortar a sessão de quem foi desativado. */
    boolean existsByIdAndAtivoTrue(Integer id);

    /** Sempre escopada à loja (RN-01): nunca devolve membro de outra loja. */
    List<MembroLoja> findByLojaCodigoOrderByAtivoDescNomeAsc(Integer lojaCodigo);

    Optional<MembroLoja> findByIdAndLojaCodigo(Integer id, Integer lojaCodigo);
}
