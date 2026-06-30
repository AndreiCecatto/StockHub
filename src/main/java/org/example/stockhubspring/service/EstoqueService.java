package org.example.stockhubspring.service;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {

    private final ProdutoService produtoService;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(ProdutoService produtoService,
                          ProdutoRepository produtoRepository,
                          MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoService = produtoService;
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<MovimentacaoEstoque> listarMovimentacoes(Integer usuarioCodigo) {
        return movimentacaoRepository.findByUsuarioCodigoOrderByCriadoEmDesc(usuarioCodigo);
    }

    @Transactional
    public void movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade, String observacao, Usuario usuario) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }

        Produto produto = produtoService.buscarPorId(produtoId, usuario.getCodigo());
        if (tipo == TipoMovimentacao.ENTRADA) {
            produto.setQuantidade(produto.getQuantidade() + quantidade);
        } else if (tipo == TipoMovimentacao.SAIDA) {
            if (produto.getQuantidade() < quantidade) {
                throw new IllegalArgumentException("Nao ha estoque suficiente para esta saida.");
            }
            produto.setQuantidade(produto.getQuantidade() - quantidade);
        } else if (tipo == TipoMovimentacao.AJUSTE) {
            produto.setQuantidade(quantidade);
        }

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(produto);
        movimentacao.setUsuario(usuario);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setObservacao(observacao);

        produtoRepository.save(produto);
        movimentacaoRepository.save(movimentacao);
    }
}
