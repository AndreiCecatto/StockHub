package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.EquipamentoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Toda consulta é filtrada pela loja (usuario_codigo). */
public interface EquipamentoClienteRepository extends JpaRepository<EquipamentoCliente, Integer> {

    List<EquipamentoCliente> findByUsuarioCodigoAndParceiroIdOrderByIdAsc(Integer usuarioCodigo, Integer parceiroId);
}
