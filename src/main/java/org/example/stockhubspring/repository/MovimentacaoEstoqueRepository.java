package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

    /** As duas primeiras movimentações do produto: basta para saber se há histórico além do inicial (RN-08). */
    List<MovimentacaoEstoque> findTop2ByProdutoIdOrderByIdAsc(Integer produtoId);
}
