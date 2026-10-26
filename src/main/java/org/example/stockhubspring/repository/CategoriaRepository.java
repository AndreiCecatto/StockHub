package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findByUsuarioCodigoOrderByNomeAsc(Integer usuarioCodigo);

    Optional<Categoria> findByIdAndUsuarioCodigo(Integer id, Integer usuarioCodigo);

    long countByUsuarioCodigo(Integer usuarioCodigo);

    /**
     * Categorias da loja com o total de produtos ATIVOS de cada uma, em UMA consulta
     * (sem consultar categoria por categoria). O padrão já vem em minúsculas e escapado
     * (ver Textos.padraoLike). Cada linha: [0] = Categoria, [1] = Long.
     */
    @Query("select c, count(p) from Categoria c left join Produto p on p.categoria = c and p.ativo = true "
            + "where c.usuario.codigo = :usuarioCodigo and lower(c.nome) like :padrao escape '!' "
            + "group by c order by lower(c.nome)")
    List<Object[]> listarComTotalDeProdutos(@Param("usuarioCodigo") Integer usuarioCodigo,
                                            @Param("padrao") String padrao);

    /** Quantas categorias da loja têm o mesmo nome (RN-10), ignorando a própria na edição. */
    @Query("select count(c) from Categoria c where c.usuario.codigo = :usuarioCodigo "
            + "and lower(trim(c.nome)) = :nome and c.id <> :ignorarId")
    long contarNome(@Param("usuarioCodigo") Integer usuarioCodigo,
                    @Param("nome") String nomeNormalizado,
                    @Param("ignorarId") Integer ignorarId);
}
