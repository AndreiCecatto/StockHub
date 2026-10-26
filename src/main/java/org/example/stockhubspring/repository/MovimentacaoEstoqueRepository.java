package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {

    /** Histórico da loja, paginado. Produto e categoria vêm junto: a tela mostra os dois nomes. */
    @EntityGraph(attributePaths = {"produto", "produto.categoria"})
    Page<MovimentacaoEstoque> findByUsuarioCodigo(Integer usuarioCodigo, Pageable pageable);

    /** As duas primeiras movimentações do produto: basta para saber se há histórico além do inicial (RN-08). */
    List<MovimentacaoEstoque> findTop2ByProdutoIdOrderByIdAsc(Integer produtoId);
}
