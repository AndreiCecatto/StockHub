package org.example.stockhubspring.repository;

import jakarta.persistence.LockModeType;
import org.example.stockhubspring.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.example.stockhubspring.dto.ProdutoParado;
import org.example.stockhubspring.dto.ValorCategoria;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Toda consulta é filtrada pela loja (usuario_codigo) e, nas listagens, só traz produtos ativos.
 * A categoria vem junto (EntityGraph) porque as telas mostram o nome dela e o
 * open-in-view está desligado.
 */
public interface ProdutoRepository extends JpaRepository<Produto, Integer>, JpaSpecificationExecutor<Produto> {

    /** Só o id e o nome (sem categoria nem outras colunas): matéria-prima da busca tolerante a erro de digitação. */
    interface NomeDoProduto {
        Integer getId();

        String getNome();
    }

    @Query("select p.id as id, p.nome as nome from Produto p where p.usuario.codigo = :usuarioCodigo "
            + "and p.ativo = true order by p.id")
    List<NomeDoProduto> listarNomesAtivos(@Param("usuarioCodigo") Integer usuarioCodigo, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Produto> findByIdAndUsuarioCodigoAndAtivoTrue(Integer id, Integer usuarioCodigo);

    /** Leitura com bloqueio de escrita na linha (RN-07): usada por toda alteração de saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id and p.usuario.codigo = :usuarioCodigo and p.ativo = true")
    Optional<Produto> buscarAtivoComBloqueio(@Param("id") Integer id, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** Igual a buscarAtivoComBloqueio, mas também acha produto inativo (liberar reserva de produto já inativado). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id and p.usuario.codigo = :usuarioCodigo")
    Optional<Produto> buscarQualquerComBloqueio(@Param("id") Integer id, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** O produto tem alguma reserva, em qualquer situação? Quem tem não pode ser apagado de vez (RN-08). */
    @Query(value = "select exists (select 1 from reserva_estoque r where r.produto_id = :id)", nativeQuery = true)
    boolean temReserva(@Param("id") Integer id);

    /** Página filtrada/ordenada por Specification, com a categoria já carregada. */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Produto> findAll(Specification<Produto> spec, Pageable pageable);

    /** Produtos mais recentes (a ordenação vem no Pageable); sem consulta de contagem. */
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByUsuarioCodigoAndAtivoTrue(Integer usuarioCodigo, Pageable pageable);

    /** Lista para o seletor de movimentação (nome do produto e da categoria, numa só consulta). */
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByUsuarioCodigoAndAtivoTrueOrderByNomeAsc(Integer usuarioCodigo);

    /** Produto ativo da loja com o código de barras dado (o mais antigo, se houver mais de um). */
    Optional<Produto> findFirstByUsuarioCodigoAndEanAndAtivoTrueOrderByIdAsc(Integer usuarioCodigo, String ean);

    /** Produtos ativos da loja com qualquer um destes códigos de barras, em UMA consulta (do mais antigo ao mais novo). */
    List<Produto> findByUsuarioCodigoAndAtivoTrueAndEanInOrderByIdAsc(Integer usuarioCodigo, Collection<String> eans);

    /** Produtos ativos da loja com estes ids, em UMA consulta (categoria vem junto). */
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByUsuarioCodigoAndAtivoTrueAndIdIn(Integer usuarioCodigo, Collection<Integer> ids);

    long countByCategoriaIdAndUsuarioCodigoAndAtivoTrue(Integer categoriaId, Integer usuarioCodigo);

    long countByCategoriaIdAndUsuarioCodigoAndAtivoFalse(Integer categoriaId, Integer usuarioCodigo);

    /** Quantos produtos ATIVOS têm o mesmo nome (RN-10), ignorando o próprio produto na edição. */
    @Query("select count(p) from Produto p where p.usuario.codigo = :usuarioCodigo and p.ativo = true "
            + "and lower(trim(p.nome)) = lower(trim(:nome)) and p.id <> :ignorarId")
    long contarNomeAtivo(@Param("usuarioCodigo") Integer usuarioCodigo,
                         @Param("nome") String nome,
                         @Param("ignorarId") Integer ignorarId);

    /** Totais do painel em UMA consulta agregada (RN-13): produtos, unidades e valor do estoque. */
    @Query("select count(p) as totalProdutos, sum(p.quantidade) as totalUnidades, "
            + "sum(p.preco * p.quantidade) as valorTotal "
            + "from Produto p where p.usuario.codigo = :usuarioCodigo and p.ativo = true")
    ResumoProdutos resumir(@Param("usuarioCodigo") Integer usuarioCodigo);

    // ------------------------------------------------------------------------------------------
    // Análises de estoque (RN-45 a RN-51): tudo agregado no banco e restrito à loja
    // ------------------------------------------------------------------------------------------

    /** Dinheiro no estoque por categoria: saldo x custo, só produtos ativos com saldo (RN-46). */
    @Query("select new org.example.stockhubspring.dto.ValorCategoria(c.nome, count(p), sum(p.quantidade), "
            + "sum(p.custo * p.quantidade)) "
            + "from Produto p join p.categoria c "
            + "where p.usuario.codigo = :usuarioCodigo and p.ativo = true and p.quantidade > 0 "
            + "group by c.id, c.nome order by sum(p.custo * p.quantidade) desc, c.nome")
    List<ValorCategoria> valorPorCategoria(@Param("usuarioCodigo") Integer usuarioCodigo);

    /** Produtos ativos com saldo e custo zero: o valor deles não aparece no dinheiro parado (RN-46). */
    @Query("select count(p) from Produto p where p.usuario.codigo = :usuarioCodigo and p.ativo = true "
            + "and p.quantidade > 0 and p.custo = 0")
    long contarComSaldoSemCusto(@Param("usuarioCodigo") Integer usuarioCodigo);

    /**
     * Produtos ativos com saldo, sem saída desde {@code limite} e que já estavam no estoque antes dele
     * (primeira movimentação anterior ao limite, ou sem nenhuma movimentação). O mais caro primeiro (RN-50).
     */
    @Query("select new org.example.stockhubspring.dto.ProdutoParado(p.id, p.nome, p.quantidade, "
            + "p.custo * p.quantidade, "
            + "(select max(s.criadoEm) from MovimentacaoEstoque s where s.produto = p "
            + "and s.tipo = org.example.stockhubspring.model.TipoMovimentacao.SAIDA)) "
            + "from Produto p "
            + "where p.usuario.codigo = :usuarioCodigo and p.ativo = true and p.quantidade > 0 "
            + "and not exists (select 1 from MovimentacaoEstoque s where s.produto = p "
            + "and s.tipo = org.example.stockhubspring.model.TipoMovimentacao.SAIDA and s.criadoEm >= :limite) "
            + "and (not exists (select 1 from MovimentacaoEstoque a where a.produto = p) "
            + "or exists (select 1 from MovimentacaoEstoque b where b.produto = p and b.criadoEm < :limite)) "
            + "order by p.custo * p.quantidade desc, p.nome")
    List<ProdutoParado> listarParados(@Param("usuarioCodigo") Integer usuarioCodigo,
                                      @Param("limite") LocalDateTime limite);
}
