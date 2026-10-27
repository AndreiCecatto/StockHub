package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.NotaFiscal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Toda consulta é filtrada pela loja (usuario_codigo); o fornecedor vem junto porque as telas mostram o nome. */
public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Integer> {

    @EntityGraph(attributePaths = "parceiro")
    Optional<NotaFiscal> findByUsuarioCodigoAndChaveAcesso(Integer usuarioCodigo, String chaveAcesso);

    @EntityGraph(attributePaths = "parceiro")
    Optional<NotaFiscal> findByIdAndUsuarioCodigo(Integer id, Integer usuarioCodigo);

    @EntityGraph(attributePaths = "parceiro")
    Page<NotaFiscal> findByUsuarioCodigo(Integer usuarioCodigo, Pageable pageable);
}
