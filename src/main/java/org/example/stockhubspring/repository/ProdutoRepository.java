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
}
