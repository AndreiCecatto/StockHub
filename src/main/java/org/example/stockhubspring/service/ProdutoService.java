package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void salvar(Produto formulario, Integer categoriaId, Usuario usuario) {
        if (formulario.getNome() == null || formulario.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do produto e obrigatorio.");
        }
        if (formulario.getPreco() == null || formulario.getPreco().signum() < 0
                || formulario.getQuantidade() < 0 || formulario.getEstoqueMinimo() < 0) {
            throw new IllegalArgumentException("Preco, quantidade e estoque minimo nao podem ser negativos.");
        }

        // A categoria tambem precisa pertencer ao usuario logado.
        Categoria categoria = categoriaService.buscarPorId(categoriaId, usuario.getCodigo());

        Produto produto;
        if (formulario.getId() != null) {
            // Edicao: carrega o produto DO USUARIO LOGADO (evita IDOR) e copia so os campos permitidos.
            produto = buscarPorId(formulario.getId(), usuario.getCodigo());
        } else {
            // Novo: sempre vinculado ao usuario logado, ignorando qualquer usuario vindo do formulario.
            produto = new Produto();
            produto.setUsuario(usuario);
        }
        produto.setNome(formulario.getNome().trim());
        produto.setDescricao(formulario.getDescricao());
        produto.setPreco(formulario.getPreco());
        produto.setQuantidade(formulario.getQuantidade());
        produto.setEstoqueMinimo(formulario.getEstoqueMinimo());
        produto.setCategoria(categoria);
        produtoRepository.save(produto);
    }

    @Transactional
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
