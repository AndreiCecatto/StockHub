package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.ResumoEstoque;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    public DashboardService(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    public ResumoEstoque buscarResumo(Integer usuarioCodigo) {
        List<Produto> produtos = produtoService.listar(null, usuarioCodigo);
        ResumoEstoque resumo = new ResumoEstoque();
        resumo.setTotalProdutos(produtos.size());
        resumo.setTotalCategorias(categoriaService.listar(null, usuarioCodigo).size());
        resumo.setProdutosEstoqueBaixo(produtoService.contarEstoqueBaixo(usuarioCodigo));
        resumo.setQuantidadeTotalItens(produtos.stream().mapToInt(Produto::getQuantidade).sum());
        resumo.setValorTotalEstoque(produtos.stream()
                .map(Produto::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return resumo;
    }
}
