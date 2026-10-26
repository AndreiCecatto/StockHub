package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Busca ja filtrada pelo dono: nao distingue "inexistente" de "de outro usuario".
        return categoriaRepository.findByIdAndUsuarioCodigo(id, usuarioCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada."));
    }

    @Transactional
    public void salvar(Categoria formulario, Usuario usuario) {
        Categoria categoria;
        if (formulario.getId() != null) {
            // Edicao: carrega a categoria DO USUARIO LOGADO e copia so os campos permitidos.
            categoria = buscarPorId(formulario.getId(), usuario.getCodigo());
        } else {
            // Nova: sempre vinculada ao usuario logado (nunca ao que vier do formulario).
            categoria = new Categoria();
            categoria.setUsuario(usuario);
        }
        categoria.setNome(formulario.getNome() == null ? null : formulario.getNome().trim());
        categoria.setDescricao(formulario.getDescricao());
        categoriaRepository.save(categoria);
    }

    @Transactional
    public void excluir(Integer id, Integer usuarioCodigo) {
        Categoria categoria = buscarPorId(id, usuarioCodigo);
        categoriaRepository.delete(categoria);
    }
}
