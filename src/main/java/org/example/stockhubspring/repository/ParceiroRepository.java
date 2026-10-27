package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Parceiro;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Toda consulta é filtrada pela loja (usuario_codigo). */
public interface ParceiroRepository extends JpaRepository<Parceiro, Integer> {

    Optional<Parceiro> findByUsuarioCodigoAndDocumento(Integer usuarioCodigo, String documento);

    /** Clientes da loja por nome (a quantidade vem no Pageable). */
    List<Parceiro> findByUsuarioCodigoAndClienteTrueOrderByNomeAsc(Integer usuarioCodigo, Pageable pageable);
}
