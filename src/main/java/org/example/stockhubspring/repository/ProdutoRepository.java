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

import java.util.List;
import java.util.Optional;

/**
 * Toda consulta é filtrada pela loja (usuario_codigo) e, nas listagens, só traz produtos ativos.
 * A categoria vem junto (EntityGraph) porque as telas mostram o nome dela e o
 * open-in-view está desligado.
 */
public interface ProdutoRepository extends JpaRepository<Produto, Integer>, JpaSpecificationExecutor<Produto> {

    @EntityGraph(attributePaths = "categoria")
    Optional<Produto> findByIdAndUsuarioCodigoAndAtivoTrue(Integer id, Integer usuarioCodigo);

    /** Leitura com bloqueio de escrita na linha (RN-07): usada por toda alteração de saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id and p.usuario.codigo = :usuarioCodigo and p.ativo = true")
    Optional<Produto> buscarAtivoComBloqueio(@Param("id") Integer id, @Param("usuarioCodigo") Integer usuarioCodigo);

    /** Página filtrada/ordenada por Specification, com a categoria já carregada. */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Produto> findAll(Specification<Produto> spec, Pageable pageable);

    /** Produtos mais recentes (a ordenação vem no Pageable); sem consulta de contagem. */
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByUsuarioCodigoAndAtivoTrue(Integer usuarioCodigo, Pageable pageable);

    /** Lista simples (nome do produto) para o seletor de movimentação. */
    List<Produto> findByUsuarioCodigoAndAtivoTrueOrderByNomeAsc(Integer usuarioCodigo);

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
}
