package org.example.stockhubspring.service;

import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.ResumoEstoque;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.ProdutoSpecs;
import org.example.stockhubspring.repository.ResumoProdutos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Totais do painel, todos calculados no banco por consultas agregadas (RN-13), só de produtos ativos. */
@Service
public class DashboardService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public DashboardService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public ResumoEstoque buscarResumo(Integer usuarioCodigo) {
        ResumoProdutos totais = produtoRepository.resumir(usuarioCodigo);

        ResumoEstoque resumo = new ResumoEstoque();
        resumo.setTotalProdutos(valorOuZero(totais.getTotalProdutos()));
        resumo.setQuantidadeTotalItens(valorOuZero(totais.getTotalUnidades()));
        resumo.setValorTotalEstoque(totais.getValorTotal() == null ? BigDecimal.ZERO : totais.getValorTotal());
        resumo.setTotalCategorias(categoriaRepository.countByUsuarioCodigo(usuarioCodigo));

        // Níveis pela definição única de NivelEstoque (RN-12)
        long semEstoque = produtoRepository.count(ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and(ProdutoSpecs.comNivel(NivelEstoque.SEM_ESTOQUE)));
        long baixo = produtoRepository.count(ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and(ProdutoSpecs.comNivel(NivelEstoque.BAIXO)));
        resumo.setProdutosSemEstoque(semEstoque);
        resumo.setProdutosEstoqueBaixo(semEstoque + baixo);
        return resumo;
    }

    private static long valorOuZero(Long valor) {
        return valor == null ? 0L : valor;
    }
}
