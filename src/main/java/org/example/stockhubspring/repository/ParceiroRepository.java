package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Parceiro;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** Toda consulta é filtrada pela loja (usuario_codigo). */
public interface ParceiroRepository extends JpaRepository<Parceiro, Integer>, JpaSpecificationExecutor<Parceiro> {

    Optional<Parceiro> findByUsuarioCodigoAndDocumento(Integer usuarioCodigo, String documento);

    /** Parceiro da loja pelo id: de outra loja se comporta como inexistente (RN-01). */
    Optional<Parceiro> findByIdAndUsuarioCodigo(Integer id, Integer usuarioCodigo);

    /** Clientes ATIVOS da loja por nome, para escolher na OS. */
    List<Parceiro> findByUsuarioCodigoAndClienteTrueAndAtivoTrueOrderByNomeAsc(Integer usuarioCodigo, Pageable pageable);

    /** Clientes da loja por nome (a quantidade vem no Pageable). */
    List<Parceiro> findByUsuarioCodigoAndClienteTrueOrderByNomeAsc(Integer usuarioCodigo, Pageable pageable);
}
