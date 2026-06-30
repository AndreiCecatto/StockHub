package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaService categoriaService) {
        this.produtoRepository = produtoRepository;
        this.categoriaService = categoriaService;
    }

    public List<Produto> listar(String busca, Integer usuarioCodigo) {
        if (busca != null && !busca.isBlank()) {
            return produtoRepository.findByUsuarioCodigoAndNomeContainingIgnoreCaseOrderByIdDesc(usuarioCodigo, busca);
        }
        return produtoRepository.findByUsuarioCodigoOrderByIdDesc(usuarioCodigo);
    }

    public List<Produto> listarFiltrado(String busca, Integer categoriaId, String nivelEstoque,
                                        String ordenacao, String direcao, Integer usuarioCodigo) {
        Comparator<Produto> comparador = comparador(ordenacao);
        if ("desc".equalsIgnoreCase(direcao)) {
            comparador = comparador.reversed();
        }

        return produtoRepository.findByUsuarioCodigoOrderByIdDesc(usuarioCodigo).stream()
                .filter(produto -> busca == null || busca.isBlank()
                        || contem(produto.getNome(), busca)
                        || contem(produto.getCategoria().getNome(), busca)
                        || String.valueOf(produto.getId()).contains(busca))
                .filter(produto -> categoriaId == null || categoriaId <= 0 || produto.getCategoria().getId().equals(categoriaId))
                .filter(produto -> filtrarNivel(produto, nivelEstoque))
                .sorted(comparador)
                .toList();
    }

    public List<Produto> listarRecentes(Integer limite, Integer usuarioCodigo) {
        return produtoRepository.findByUsuarioCodigoOrderByIdDesc(usuarioCodigo).stream()
                .limit(limite)
                .toList();
    }

    public List<Produto> listarEstoqueBaixo(Integer usuarioCodigo) {
        return produtoRepository.listarEstoqueBaixo(usuarioCodigo);
    }

    public Produto buscarPorId(Integer id, Integer usuarioCodigo) {
        return produtoRepository.findByIdAndUsuarioCodigo(id, usuarioCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));
    }

    public void salvar(Produto produto, Integer categoriaId, Usuario usuario) {
        Categoria categoria = categoriaService.buscarPorId(categoriaId, usuario.getCodigo());
        produto.setCategoria(categoria);
        produto.setUsuario(usuario);
        produtoRepository.save(produto);
    }

    public void excluir(Integer id, Integer usuarioCodigo) {
        Produto produto = buscarPorId(id, usuarioCodigo);
        produtoRepository.delete(produto);
    }

    public long contar(Integer usuarioCodigo) {
        return produtoRepository.countByUsuarioCodigo(usuarioCodigo);
    }

    public long contarEstoqueBaixo(Integer usuarioCodigo) {
        return produtoRepository.contarEstoqueBaixo(usuarioCodigo);
    }

    private boolean contem(String valor, String busca) {
        return valor != null && valor.toLowerCase().contains(busca.toLowerCase());
    }

    private boolean filtrarNivel(Produto produto, String nivelEstoque) {
        if (nivelEstoque == null || nivelEstoque.isBlank()) {
            return true;
        }
        if ("semEstoque".equals(nivelEstoque)) {
            return produto.getQuantidade() == 0;
        }
        if ("baixo".equals(nivelEstoque)) {
            return produto.getQuantidade() > 0 && produto.getQuantidade() <= produto.getEstoqueMinimo();
        }
        if ("ok".equals(nivelEstoque)) {
            return produto.getQuantidade() > produto.getEstoqueMinimo();
        }
        return true;
    }

    private Comparator<Produto> comparador(String ordenacao) {
        if ("nome".equals(ordenacao)) {
            return Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
        }
        if ("preco".equals(ordenacao)) {
            return Comparator.comparing(Produto::getPreco);
        }
        if ("quantidade".equals(ordenacao)) {
            return Comparator.comparingInt(Produto::getQuantidade);
        }
        if ("categoria".equals(ordenacao)) {
            return Comparator.comparing(produto -> produto.getCategoria().getNome(), String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing(Produto::getId);
    }
}
