package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.ReservaEstoque;
import org.example.stockhubspring.model.StatusReserva;
import org.example.stockhubspring.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Toda consulta é filtrada pela loja (usuario_codigo). */
public interface ReservaEstoqueRepository extends JpaRepository<ReservaEstoque, Integer> {

    /** Só o produto da reserva (sem carregar a reserva): serve para travar o produto antes de mexer nela. */
    @Query("select r.produto.id from ReservaEstoque r where r.id = :id and r.usuario.codigo = :usuarioCodigo")
    Optional<Integer> buscarProdutoId(@Param("id") Integer id, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** Soma das reservas ATIVAS do produto (0 se não houver). */
    @Query("select coalesce(sum(r.quantidade), 0) from ReservaEstoque r "
            + "where r.produto.id = :produtoId and r.usuario.codigo = :usuarioCodigo "
            + "and r.status = org.example.stockhubspring.model.StatusReserva.ATIVA")
    long somarAtivas(@Param("produtoId") Integer produtoId, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** Reservas de um documento (ids em ordem), para baixar ou liberar o documento inteiro. */
    @Query("select r.id as id, r.produto.id as produtoId from ReservaEstoque r "
            + "where r.usuario.codigo = :usuarioCodigo and r.documentoTipo = :tipo and r.documentoId = :documentoId "
            + "and r.status = :status order by r.produto.id, r.id")
    List<ReservaDoDocumento> listarDoDocumento(@Param("usuarioCodigo") Integer usuarioCodigo,
                                               @Param("tipo") TipoDocumento tipo,
                                               @Param("documentoId") Integer documentoId,
                                               @Param("status") StatusReserva status);

    interface ReservaDoDocumento {
        Integer getId();

        Integer getProdutoId();
    }
}
