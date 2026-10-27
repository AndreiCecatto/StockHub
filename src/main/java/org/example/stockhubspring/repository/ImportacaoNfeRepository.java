package org.example.stockhubspring.repository;

import jakarta.persistence.LockModeType;
import org.example.stockhubspring.dto.PendenteNfe;
import org.example.stockhubspring.model.ImportacaoNfe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Toda consulta é filtrada pela loja (usuario_codigo): o token só vale para a loja que o criou. */
public interface ImportacaoNfeRepository extends JpaRepository<ImportacaoNfe, Integer> {

    @Query("select i from ImportacaoNfe i where i.token = :token and i.usuario.codigo = :usuarioCodigo")
    Optional<ImportacaoNfe> buscar(@Param("token") UUID token, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** Com bloqueio de escrita: duas confirmações do mesmo token se enfileiram (a segunda vê CONFIRMADA). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImportacaoNfe i where i.token = :token and i.usuario.codigo = :usuarioCodigo")
    Optional<ImportacaoNfe> buscarComBloqueio(@Param("token") UUID token, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** Pendentes ainda válidas da loja, as mais recentes primeiro (sem o XML). */
    @Query("select new org.example.stockhubspring.dto.PendenteNfe(i.token, i.chaveAcesso, i.criadaEm, i.expiraEm) "
            + "from ImportacaoNfe i where i.usuario.codigo = :usuarioCodigo and i.status = "
            + "org.example.stockhubspring.model.StatusImportacao.PENDENTE and i.expiraEm > :agora "
            + "order by i.criadaEm desc, i.id desc")
    List<PendenteNfe> listarPendentes(@Param("usuarioCodigo") Integer usuarioCodigo, @Param("agora") LocalDateTime agora);

    /** Ids das pendentes válidas da loja, da mais antiga para a mais nova. */
    @Query("select i.id from ImportacaoNfe i where i.usuario.codigo = :usuarioCodigo and i.status = "
            + "org.example.stockhubspring.model.StatusImportacao.PENDENTE and i.expiraEm > :agora "
            + "order by i.criadaEm asc, i.id asc")
    List<Integer> idsPendentesValidas(@Param("usuarioCodigo") Integer usuarioCodigo, @Param("agora") LocalDateTime agora);

    /** Pendentes vencidas da loja viram CANCELADA (não apaga nada). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ImportacaoNfe i set i.xml = null, i.status = org.example.stockhubspring.model.StatusImportacao.CANCELADA "
            + "where i.usuario.codigo = :usuarioCodigo and i.status = "
            + "org.example.stockhubspring.model.StatusImportacao.PENDENTE and i.expiraEm <= :agora")
    int cancelarVencidas(@Param("usuarioCodigo") Integer usuarioCodigo, @Param("agora") LocalDateTime agora);

    /** Pendente ainda válida da loja para esta chave, se houver (reaproveitada em vez de duplicar o XML guardado). */
    @Query("select i from ImportacaoNfe i where i.usuario.codigo = :usuarioCodigo and i.chaveAcesso = :chave "
            + "and i.status = org.example.stockhubspring.model.StatusImportacao.PENDENTE and i.expiraEm > :agora "
            + "order by i.id desc")
    List<ImportacaoNfe> pendentesDaChave(@Param("usuarioCodigo") Integer usuarioCodigo, @Param("chave") String chave,
                                         @Param("agora") LocalDateTime agora);

    /** Zera o XML das importações já encerradas (nenhuma linha é apagada). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ImportacaoNfe i set i.xml = null where i.xml is not null and i.status <> "
            + "org.example.stockhubspring.model.StatusImportacao.PENDENTE")
    int zerarXmlEncerradas();

    /** Cancela as pendentes indicadas (sempre dentro da loja). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ImportacaoNfe i set i.xml = null, i.status = org.example.stockhubspring.model.StatusImportacao.CANCELADA "
            + "where i.usuario.codigo = :usuarioCodigo and i.id in :ids and i.status = "
            + "org.example.stockhubspring.model.StatusImportacao.PENDENTE")
    int cancelar(@Param("usuarioCodigo") Integer usuarioCodigo, @Param("ids") Collection<Integer> ids);
}
