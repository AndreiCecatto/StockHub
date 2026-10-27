package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.EquipamentoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Toda consulta é filtrada pela loja (usuario_codigo). */
public interface EquipamentoClienteRepository extends JpaRepository<EquipamentoCliente, Integer> {

    List<EquipamentoCliente> findByUsuarioCodigoAndParceiroIdOrderByIdAsc(Integer usuarioCodigo, Integer parceiroId);

    /** Veículo do cliente pela placa já normalizada (RN-107). */
    Optional<EquipamentoCliente> findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(Integer usuarioCodigo,
                                                                                         Integer parceiroId, String placa);
}
