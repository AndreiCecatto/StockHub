package org.example.stockhubspring.repository;

import jakarta.persistence.LockModeType;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.StatusOs;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Toda consulta é filtrada pela loja (usuario_codigo, RN-01). */
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Integer> {

    @Query("select o from OrdemServico o join fetch o.usuario join fetch o.cliente join fetch o.equipamento "
            + "left join fetch o.responsavel where o.id = :id and o.usuario.codigo = :loja")
    Optional<OrdemServico> buscar(@Param("id") Integer id, @Param("loja") Integer loja);

    /** Trava a linha da OS: duas ações sobre a mesma OS se enfileiram (sem join fetch, que não combina com a trava). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id and o.usuario.codigo = :loja")
    Optional<OrdemServico> buscarComBloqueio(@Param("id") Integer id, @Param("loja") Integer loja);

    @Query("select o from OrdemServico o join fetch o.cliente join fetch o.equipamento "
            + "left join fetch o.responsavel where o.usuario.codigo = :loja "
            + "and (:status is null or o.status = :status) order by o.numero desc")
    List<OrdemServico> listar(@Param("loja") Integer loja, @Param("status") StatusOs status, Pageable pageable);

    @Query("select o.status as status, count(o) as total from OrdemServico o where o.usuario.codigo = :loja "
            + "group by o.status")
    List<ContagemStatus> contarPorStatus(@Param("loja") Integer loja);

    interface ContagemStatus {
        StatusOs getStatus();

        long getTotal();
    }

    // ---- resumo do cliente (RN-98) ----

    long countByUsuarioCodigoAndClienteId(Integer loja, Integer clienteId);

    long countByUsuarioCodigoAndClienteIdAndStatus(Integer loja, Integer clienteId, StatusOs status);

    long countByUsuarioCodigoAndClienteIdAndStatusIn(Integer loja, Integer clienteId, java.util.Collection<StatusOs> status);

    @Query("select sum(o.total) from OrdemServico o where o.usuario.codigo = :loja and o.cliente.id = :cliente "
            + "and o.status = org.example.stockhubspring.model.StatusOs.ENTREGUE")
    java.math.BigDecimal somarEntregues(@Param("loja") Integer loja, @Param("cliente") Integer cliente);

    @Query("select max(o.criadaEm) from OrdemServico o where o.usuario.codigo = :loja and o.cliente.id = :cliente")
    java.time.LocalDateTime ultimaAbertura(@Param("loja") Integer loja, @Param("cliente") Integer cliente);

    @Query("select o from OrdemServico o join fetch o.equipamento where o.usuario.codigo = :loja "
            + "and o.cliente.id = :cliente order by o.numero desc")
    List<OrdemServico> doCliente(@Param("loja") Integer loja, @Param("cliente") Integer cliente, Pageable pageable);

    /** Quantas OS cada cliente da lista tem (uma consulta para a página inteira). */
    @Query("select o.cliente.id as clienteId, count(o) as total from OrdemServico o where o.usuario.codigo = :loja "
            + "and o.cliente.id in :ids group by o.cliente.id")
    List<ContagemCliente> contarPorClientes(@Param("loja") Integer loja, @Param("ids") java.util.Collection<Integer> ids);

    interface ContagemCliente {
        Integer getClienteId();

        long getTotal();
    }
}
