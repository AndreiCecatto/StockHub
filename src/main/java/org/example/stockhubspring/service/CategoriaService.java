package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.CategoriaForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.util.Textos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Categorias da loja. Toda operação é escopada pelo usuário logado (RN-01). */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    /** Categorias com o total de produtos ativos de cada uma, numa única consulta. */
    @Transactional(readOnly = true)
    public List<Categoria> listar(String busca, Integer usuarioCodigo) {
        return categoriaRepository.listarComTotalDeProdutos(usuarioCodigo, Textos.padraoLike(busca)).stream()
                .map(linha -> {
                    Categoria categoria = (Categoria) linha[0];
                    categoria.setTotalProdutos((Long) linha[1]);
                    return categoria;
                })
                .toList();
    }

    /** Todas as categorias da loja, só para preencher seletores (sem totais). */
    @Transactional(readOnly = true)
    public List<Categoria> listarParaSelecao(Integer usuarioCodigo) {
        return categoriaRepository.findByUsuarioCodigoOrderByNomeAsc(usuarioCodigo);
    }

    @Transactional(readOnly = true)
    public Categoria buscarPorId(Integer id, Integer usuarioCodigo) {
        // Busca já filtrada pelo dono: "inexistente" e "de outra loja" respondem igual (RN-01).
        return categoriaRepository.findByIdAndUsuarioCodigo(id, usuarioCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    /** Cria (id nulo) ou atualiza a categoria da loja, com nome único (RN-10). */
    @Transactional
    public Categoria salvar(CategoriaForm form, Usuario usuario) {
        Categoria categoria;
        Integer ignorarId = -1;
        if (form.getId() != null) {
            // Edição: carrega a categoria DA LOJA LOGADA e copia só os campos permitidos.
            categoria = buscarPorId(form.getId(), usuario.getCodigo());
            ignorarId = categoria.getId();
        } else {
            // Nova: sempre da loja logada, nunca de um dono vindo do formulário.
            categoria = new Categoria();
            categoria.setUsuario(usuario);
        }

        String nome = Textos.limpar(form.getNome());
        if (nome == null) {
            throw new RegraNegocioException("O nome da categoria é obrigatório.");
        }
        if (categoriaRepository.contarNome(usuario.getCodigo(), Textos.normalizarNome(nome), ignorarId) > 0) {
            throw nomeDuplicado(nome);
        }
        categoria.setNome(nome);
        categoria.setDescricao(Textos.limpar(form.getDescricao()));
        try {
            // saveAndFlush: se outra requisição criou o mesmo nome agora, o índice único acusa aqui
            return categoriaRepository.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException e) {
            throw nomeDuplicado(nome);
        }
    }

    /**
     * Exclui a categoria (RN-09): só se não tiver produtos ativos. Produtos inativados mantêm a
     * referência (histórico), então também impedem a exclusão.
     */
    @Transactional
    public void excluir(Integer id, Integer usuarioCodigo) {
        Categoria categoria = buscarPorId(id, usuarioCodigo);
        long ativos = produtoRepository.countByCategoriaIdAndUsuarioCodigoAndAtivoTrue(id, usuarioCodigo);
        if (ativos > 0) {
            throw new RegraNegocioException("Não é possível excluir a categoria \"" + categoria.getNome()
                    + "\": " + (ativos == 1 ? "1 produto a usa." : ativos + " produtos a usam.")
                    + " Mova ou exclua esses produtos primeiro.");
        }
        long inativos = produtoRepository.countByCategoriaIdAndUsuarioCodigoAndAtivoFalse(id, usuarioCodigo);
        if (inativos > 0) {
            throw new RegraNegocioException("Não é possível excluir a categoria \"" + categoria.getNome()
                    + "\": ela é usada por " + (inativos == 1 ? "1 produto inativado" : inativos + " produtos inativados")
                    + ", mantidos para preservar o histórico de estoque.");
        }
        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Não é possível excluir a categoria \"" + categoria.getNome()
                    + "\": ela ainda está em uso.");
        }
    }

    private RegraNegocioException nomeDuplicado(String nome) {
        return new RegraNegocioException("Já existe uma categoria chamada \"" + nome + "\" na sua loja.");
    }
}
