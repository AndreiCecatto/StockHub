package org.example.stockhubspring.repository;

import org.example.stockhubspring.dto.SaidaCategoria;
import org.example.stockhubspring.dto.SaidaComSaldo;
import org.example.stockhubspring.dto.SaidaProduto;
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
import java.util.Collection;
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

    /** Códigos, entre os dados, das movimentações que já foram estornadas (para marcar na tela). */
    @Query("select m.estornoDeId from MovimentacaoEstoque m where m.estornoDeId in :ids")
    List<Integer> idsEstornados(@Param("ids") Collection<Integer> ids);

    /** Esta movimentação já foi estornada? */
    boolean existsByEstornoDeId(Integer estornoDeId);

    /** Movimentação da loja pelo código (o isolamento por loja vale também aqui, RN-01). */
    @EntityGraph(attributePaths = {"produto"})
    java.util.Optional<MovimentacaoEstoque> findByIdAndUsuarioCodigo(Integer id, Integer usuarioCodigo);

    /** As duas primeiras movimentações do produto: basta para saber se há histórico além do inicial (RN-08). */
    List<MovimentacaoEstoque> findTop2ByProdutoIdOrderByIdAsc(Integer produtoId);

    // ------------------------------------------------------------------------------------------
    // Análises de estoque (RN-45 a RN-51): agregação no banco, sempre da loja, intervalo [inicio, fim)
    // ------------------------------------------------------------------------------------------

    /** Saídas por dia: [dia, unidades, valor ao custo gravado]. Só dias com saída (o service completa com zero). */
    @Query(value = """
            select cast(m.criado_em as date) as dia,
                   sum(m.quantidade) as unidades,
                   sum(m.quantidade * m.custo_unitario) as valor
            from movimentacao_estoque m
            where m.usuario_codigo = :usuarioCodigo and m.tipo = 'SAIDA'
              and m.estorno_de_id is null
              and not exists (select 1 from movimentacao_estoque e where e.estorno_de_id = m.id)
              and m.criado_em >= :inicio and m.criado_em < :fim
            group by cast(m.criado_em as date)
            order by 1
            """, nativeQuery = true)
    List<Object[]> saidasPorDia(@Param("usuarioCodigo") Integer usuarioCodigo,
                                @Param("inicio") LocalDateTime inicio,
                                @Param("fim") LocalDateTime fim);

    /** Saídas por produto, as de mais unidades primeiro. */
    @Query("select new org.example.stockhubspring.dto.SaidaProduto(p.id, p.nome, sum(m.quantidade), "
            + "sum(m.quantidade * m.custoUnitario)) "
            + "from MovimentacaoEstoque m join m.produto p "
            + "where m.usuario.codigo = :usuarioCodigo "
            + "and m.estornoDeId is null and not exists (select 1 from MovimentacaoEstoque e where e.estornoDeId = m.id) "
            + "and m.tipo = org.example.stockhubspring.model.TipoMovimentacao.SAIDA "
            + "and m.criadoEm >= :inicio and m.criadoEm < :fim "
            + "group by p.id, p.nome order by sum(m.quantidade) desc, p.nome")
    List<SaidaProduto> saidasPorProduto(@Param("usuarioCodigo") Integer usuarioCodigo,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim);

    /** Saídas por categoria (a atual do produto), as de mais valor primeiro. */
    @Query("select new org.example.stockhubspring.dto.SaidaCategoria(c.nome, sum(m.quantidade), "
            + "sum(m.quantidade * m.custoUnitario)) "
            + "from MovimentacaoEstoque m join m.produto p join p.categoria c "
            + "where m.usuario.codigo = :usuarioCodigo "
            + "and m.estornoDeId is null and not exists (select 1 from MovimentacaoEstoque e where e.estornoDeId = m.id) "
            + "and m.tipo = org.example.stockhubspring.model.TipoMovimentacao.SAIDA "
            + "and m.criadoEm >= :inicio and m.criadoEm < :fim "
            + "group by c.id, c.nome order by sum(m.quantidade * m.custoUnitario) desc, c.nome")
    List<SaidaCategoria> saidasPorCategoria(@Param("usuarioCodigo") Integer usuarioCodigo,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fim") LocalDateTime fim);

    /**
     * Caixa por mês: [aaaa-MM, compras, saídas], ambos em valor ao custo gravado. Compras são as ENTRADAS,
     * menos o "Estoque inicial" (saldo de abertura não é compra). Só meses com movimento.
     */
    @Query(value = """
            select to_char(m.criado_em, 'YYYY-MM') as mes,
                   coalesce(sum(case when m.tipo = 'ENTRADA'
                                      and lower(btrim(coalesce(m.observacao, ''))) <> 'estoque inicial'
                                     then m.quantidade * m.custo_unitario end), 0) as compras,
                   coalesce(sum(case when m.tipo = 'SAIDA' then m.quantidade * m.custo_unitario end), 0) as saidas
            from movimentacao_estoque m
            where m.usuario_codigo = :usuarioCodigo
              and m.estorno_de_id is null
              and not exists (select 1 from movimentacao_estoque e where e.estorno_de_id = m.id)
              and m.criado_em >= :inicio and m.criado_em < :fim
            group by to_char(m.criado_em, 'YYYY-MM')
            order by 1
            """, nativeQuery = true)
    List<Object[]> caixaPorMes(@Param("usuarioCodigo") Integer usuarioCodigo,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim);

    /** Produtos ativos com saldo que tiveram saída na janela, com o total que saiu (cobertura em dias, RN-49). */
    @Query("select new org.example.stockhubspring.dto.SaidaComSaldo(p.id, p.nome, p.quantidade, sum(m.quantidade)) "
            + "from MovimentacaoEstoque m join m.produto p "
            + "where m.usuario.codigo = :usuarioCodigo and p.usuario.codigo = :usuarioCodigo "
            + "and p.ativo = true and p.quantidade > 0 "
            + "and m.estornoDeId is null and not exists (select 1 from MovimentacaoEstoque e where e.estornoDeId = m.id) "
            + "and m.tipo = org.example.stockhubspring.model.TipoMovimentacao.SAIDA "
            + "and m.criadoEm >= :inicio and m.criadoEm < :fim "
            + "group by p.id, p.nome, p.quantidade")
    List<SaidaComSaldo> saidasComSaldo(@Param("usuarioCodigo") Integer usuarioCodigo,
                                       @Param("inicio") LocalDateTime inicio,
                                       @Param("fim") LocalDateTime fim);

    /** Primeira movimentação de cada produto da lista: [produtoId, data]. Conta os dias de quem nunca saiu. */
    @Query("select m.produto.id, min(m.criadoEm) from MovimentacaoEstoque m "
            + "where m.usuario.codigo = :usuarioCodigo and m.produto.id in :ids group by m.produto.id")
    List<Object[]> primeiraMovimentacao(@Param("usuarioCodigo") Integer usuarioCodigo,
                                        @Param("ids") Collection<Integer> ids);
}
