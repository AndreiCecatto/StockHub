package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<Categoria> listar(String busca, Integer usuarioCodigo) {
        List<Categoria> categorias;
        if (busca != null && !busca.isBlank()) {
            categorias = categoriaRepository.findByUsuarioCodigoAndNomeContainingIgnoreCaseOrderByNomeAsc(usuarioCodigo, busca);
        } else {
            categorias = categoriaRepository.findByUsuarioCodigoOrderByNomeAsc(usuarioCodigo);
        }
        categorias.forEach(categoria -> categoria.setTotalProdutos(
                produtoRepository.countByCategoriaIdAndUsuarioCodigo(categoria.getId(), usuarioCodigo)));
        return categorias;
    }

    public Categoria buscarPorId(Integer id, Integer usuarioCodigo) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada."));
        if (!categoria.getUsuario().getCodigo().equals(usuarioCodigo)) {
            throw new IllegalArgumentException("Categoria nao pertence ao usuario atual.");
        }
        return categoria;
    }

    public void salvar(Categoria categoria, Usuario usuario) {
        categoria.setUsuario(usuario);
        categoriaRepository.save(categoria);
    }

    public void excluir(Integer id, Integer usuarioCodigo) {
        Categoria categoria = buscarPorId(id, usuarioCodigo);
        categoriaRepository.delete(categoria);
    }
}
