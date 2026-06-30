package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {
    List<Produto> findByUsuarioCodigoOrderByIdDesc(Integer usuarioCodigo);

    List<Produto> findByUsuarioCodigoAndNomeContainingIgnoreCaseOrderByIdDesc(Integer usuarioCodigo, String nome);

    Optional<Produto> findByIdAndUsuarioCodigo(Integer id, Integer usuarioCodigo);

    long countByUsuarioCodigo(Integer usuarioCodigo);

    long countByCategoriaIdAndUsuarioCodigo(Integer categoriaId, Integer usuarioCodigo);

    @Query("select p from Produto p where p.usuario.codigo = :usuarioCodigo and p.quantidade <= p.estoqueMinimo order by p.quantidade asc, p.nome asc")
    List<Produto> listarEstoqueBaixo(@Param("usuarioCodigo") Integer usuarioCodigo);

    @Query("select count(p) from Produto p where p.usuario.codigo = :usuarioCodigo and p.quantidade <= p.estoqueMinimo")
    long contarEstoqueBaixo(@Param("usuarioCodigo") Integer usuarioCodigo);
}
