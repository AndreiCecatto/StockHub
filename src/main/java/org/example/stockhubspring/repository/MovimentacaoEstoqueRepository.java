package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, Integer>, JpaSpecificationExecutor<MovimentacaoEstoque> {

    /**
     * Histórico da loja filtrado por Specification, paginado. Produto e categoria vêm junto
     * (uma só consulta, sem N+1): a tela mostra os dois nomes e o open-in-view está desligado.
     */
    @Override
    @EntityGraph(attributePaths = {"produto", "produto.categoria"})
    Page<MovimentacaoEstoque> findAll(Specification<MovimentacaoEstoque> spec, Pageable pageable);

    /** Últimas movimentações de UM produto da loja (a ordem e o limite vêm no Pageable); sem consulta de contagem. */
    @EntityGraph(attributePaths = {"produto", "produto.categoria"})
    List<MovimentacaoEstoque> findByProdutoIdAndUsuarioCodigo(Integer produtoId, Integer usuarioCodigo, Pageable pageable);

    /**
     * Movimentos agrupados por dia, numa só consulta no banco: [dia, unidades de ENTRADA, unidades de SAIDA,
     * quantidade de AJUSTES]. Só dias com movimento (o service completa os demais com zero).
     * Intervalo [inicio, fim) sobre criado_em, sempre da loja (RN-01).
     */
    @Query(value = """
            select cast(m.criado_em as date) as dia,
                   coalesce(sum(case when m.tipo = 'ENTRADA' then m.quantidade else 0 end), 0) as entradas,
                   coalesce(sum(case when m.tipo = 'SAIDA' then m.quantidade else 0 end), 0) as saidas,
                   count(case when m.tipo = 'AJUSTE' then 1 end) as ajustes
            from movimentacao_estoque m
            where m.usuario_codigo = :usuarioCodigo
              and m.criado_em >= :inicio and m.criado_em < :fim
            group by cast(m.criado_em as date)
            order by 1
            """, nativeQuery = true)
    List<Object[]> somarPorDia(@Param("usuarioCodigo") Integer usuarioCodigo,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim);

    /** As duas primeiras movimentações do produto: basta para saber se há histórico além do inicial (RN-08). */
    List<MovimentacaoEstoque> findTop2ByProdutoIdOrderByIdAsc(Integer produtoId);
}
