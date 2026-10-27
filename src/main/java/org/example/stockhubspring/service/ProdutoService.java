package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.ProdutoSpecs;
import org.example.stockhubspring.util.BuscaDifusa;
import org.example.stockhubspring.util.Paginas;
import org.example.stockhubspring.util.Restricoes;
import org.example.stockhubspring.util.Textos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Produtos da loja. Toda operação é escopada pelo usuário logado (RN-01) e só enxerga produtos ativos. */
@Service
public class ProdutoService {

    /** Resultado de {@link #excluir}: apagado de vez ou apenas inativado (RN-08). */
    public enum ResultadoExclusao {
        EXCLUIDO,
        INATIVADO
    }

    public static final int TAMANHO_MAXIMO_PAGINA = ProdutoFiltro.TAMANHO_MAXIMO;
    /** Máximo de linhas de uma exportação CSV de produtos (proteção de memória). */
    public static final int LIMITE_EXPORTACAO = 10_000;
    public static final int BUSCA_TERMO_MINIMO = 2;
    public static final int BUSCA_LIMITE_PADRAO = 8;
    public static final int BUSCA_LIMITE_MAXIMO = 20;
    /** Quantos nomes a busca tolerante examina por consulta (teto de segurança; uma loja média tem bem menos). */
    public static final int BUSCA_TOLERANTE_MAXIMO_NOMES = 5000;

    /** Resultado da busca com sugestões: os {@code exatos} primeiros casam com o texto; o restante é "parecido". */
    public record ResultadoBusca(List<Produto> produtos, int exatos) {
    }

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EstoqueService estoqueService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          CategoriaRepository categoriaRepository,
                          MovimentacaoEstoqueRepository movimentacaoRepository,
                          EstoqueService estoqueService) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.estoqueService = estoqueService;
    }

    /** Listagem paginada com filtros e ordenação feitos no banco. */
    @Transactional(readOnly = true)
    public Page<Produto> listarPagina(ProdutoFiltro filtro, Integer usuarioCodigo) {
        Specification<Produto> especificacao = especificacao(filtro, usuarioCodigo);
        return Paginas.buscar(filtro.pagina(), filtro.tamanho(), TAMANHO_MAXIMO_PAGINA, ordem(filtro),
                pageable -> produtoRepository.findAll(especificacao, pageable));
    }

    /**
     * Produtos para exportação: mesmos filtros e ordenação da listagem, sem paginação, no máximo
     * {@link #LIMITE_EXPORTACAO} linhas. Se {@code getTotalElements()} for maior que
     * {@code getNumberOfElements()}, a exportação foi truncada pelo limite.
     */
    @Transactional(readOnly = true)
    public Page<Produto> listarParaExportar(ProdutoFiltro filtro, Integer usuarioCodigo) {
        return produtoRepository.findAll(especificacao(filtro, usuarioCodigo),
                PageRequest.of(0, LIMITE_EXPORTACAO, ordem(filtro)));
    }

    /**
     * Exportação só dos produtos marcados na tela. A restrição à loja e aos ativos (RN-01, RN-08) continua
     * valendo: ids de outra loja simplesmente não aparecem. Lista vazia equivale ao filtro inteiro.
     */
    @Transactional(readOnly = true)
    public Page<Produto> listarParaExportar(ProdutoFiltro filtro, Integer usuarioCodigo, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return listarParaExportar(filtro, usuarioCodigo);
        }
        Specification<Produto> porIds = especificacao(filtro, usuarioCodigo)
                .and((root, query, cb) -> root.get("id").in(ids));
        return produtoRepository.findAll(porIds, PageRequest.of(0, LIMITE_EXPORTACAO, ordem(filtro)));
    }

    /** Produtos ativos da loja (RN-01, RN-08) com os filtros da listagem. */
    private Specification<Produto> especificacao(ProdutoFiltro filtro, Integer usuarioCodigo) {
        Specification<Produto> spec = ProdutoSpecs.ativosDoUsuario(usuarioCodigo);
        if (filtro.busca() != null) {
            spec = spec.and(ProdutoSpecs.comBusca(filtro.busca()));
        }
        if (filtro.categoriaId() != null) {
            spec = spec.and(ProdutoSpecs.daCategoria(filtro.categoriaId()));
        }
        if (filtro.nivel() != null) {
            spec = spec.and(ProdutoSpecs.comNivel(filtro.nivel()));
        }
        return spec;
    }

    private Sort ordem(ProdutoFiltro filtro) {
        // Desempate por id mantém a paginação estável quando o campo escolhido tem valores repetidos
        Sort.Order principal = "asc".equals(filtro.direcao())
                ? Sort.Order.asc(filtro.propriedadeOrdenacao())
                : Sort.Order.desc(filtro.propriedadeOrdenacao());
        if ("nome".equals(filtro.ordenacao()) || "categoria".equals(filtro.ordenacao())) {
            principal = principal.ignoreCase();
        }
        return "id".equals(filtro.ordenacao()) ? Sort.by(principal) : Sort.by(principal, Sort.Order.asc("id"));
    }

    /** Últimos produtos cadastrados (painel). */
    @Transactional(readOnly = true)
    public List<Produto> listarRecentes(int limite, Integer usuarioCodigo) {
        return produtoRepository.findByUsuarioCodigoAndAtivoTrue(usuarioCodigo,
                PageRequest.of(0, Math.max(1, Math.min(limite, TAMANHO_MAXIMO_PAGINA)), Sort.by(Sort.Order.desc("id"))));
    }

    /**
     * Produtos que pedem reposição: níveis SEM_ESTOQUE e BAIXO (RN-12), do menor saldo para o maior.
     * Página em base 1.
     */
    @Transactional(readOnly = true)
    public Page<Produto> listarEstoqueBaixo(Integer usuarioCodigo, int pagina, int tamanho) {
        Specification<Produto> spec = ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and(ProdutoSpecs.comNivel(NivelEstoque.SEM_ESTOQUE, NivelEstoque.BAIXO));
        Sort ordem = Sort.by(Sort.Order.asc("quantidade"), Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("id"));
        return Paginas.buscar(pagina, tamanho, TAMANHO_MAXIMO_PAGINA, ordem,
                pageable -> produtoRepository.findAll(spec, pageable));
    }

    /**
     * Busca rápida por nome (caixa e acento ignorados), só produtos ativos da loja, em ordem de nome,
     * com a categoria já carregada. Termo com menos de 2 caracteres (após trim) devolve lista vazia;
     * o limite fica entre 1 e {@link #BUSCA_LIMITE_MAXIMO}.
     */
    @Transactional(readOnly = true)
    public List<Produto> buscarRapido(String termo, int limite, Integer usuarioCodigo) {
        String limpo = Textos.limpar(termo);
        if (limpo == null || limpo.length() < BUSCA_TERMO_MINIMO) {
            return List.of();
        }
        int tamanho = Math.max(1, Math.min(limite, BUSCA_LIMITE_MAXIMO));
        Specification<Produto> spec = ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and(ProdutoSpecs.nomeContendoSemAcento(limpo));
        Sort ordem = Sort.by(Sort.Order.asc("nome").ignoreCase(), Sort.Order.asc("id"));
        return produtoRepository.findAll(spec, PageRequest.of(0, tamanho, ordem)).getContent();
    }

    /**
     * Busca rápida que perdoa erro de digitação: devolve os produtos que contêm o texto (como em
     * {@link #buscarRapido}); só quando NENHUM contém, procura os de nome parecido
     * ({@link BuscaDifusa}), do mais ao menos parecido. Misturar os dois só traria ruído ("cane" achando "caderno").
     * Sempre restrito aos produtos ativos da loja.
     */
    @Transactional(readOnly = true)
    public ResultadoBusca buscarTolerante(String termo, int limite, Integer usuarioCodigo) {
        List<Produto> exatos = buscarRapido(termo, limite, usuarioCodigo);
        int tamanho = Math.max(1, Math.min(limite, BUSCA_LIMITE_MAXIMO));
        String limpo = Textos.limpar(termo);
        if (limpo == null || limpo.length() < BUSCA_TERMO_MINIMO || !exatos.isEmpty()) {
            return new ResultadoBusca(exatos, exatos.size());
        }
        Set<Integer> jaTem = new HashSet<>();
        exatos.forEach(p -> jaTem.add(p.getId()));
        record Candidato(Integer id, double nota, String nome) {
        }
        List<Candidato> parecidos = new ArrayList<>();
        for (var nome : produtoRepository.listarNomesAtivos(usuarioCodigo,
                PageRequest.of(0, BUSCA_TOLERANTE_MAXIMO_NOMES))) {
            double nota = BuscaDifusa.pontuar(limpo, nome.getNome());
            if (nota > BuscaDifusa.SEM_CORRESPONDENCIA && !jaTem.contains(nome.getId())) {
                parecidos.add(new Candidato(nome.getId(), nota, nome.getNome()));
            }
        }
        if (parecidos.isEmpty()) {
            return new ResultadoBusca(exatos, exatos.size());
        }
        parecidos.sort(Comparator.comparingDouble(Candidato::nota).reversed()
                .thenComparing(Candidato::nome, String.CASE_INSENSITIVE_ORDER));
        List<Integer> escolhidos = parecidos.stream().limit(tamanho - exatos.size()).map(Candidato::id).toList();
        // carrega só os escolhidos, de novo com o filtro da loja (a lista de nomes nunca vira acesso a outra loja)
        Specification<Produto> spec = ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and((root, query, cb) -> root.get("id").in(escolhidos));
        List<Produto> carregados = produtoRepository.findAll(spec, PageRequest.of(0, escolhidos.size())).getContent();
        List<Produto> ordenados = new ArrayList<>(exatos);
        for (Integer id : escolhidos) {
            carregados.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(ordenados::add);
        }
        return new ResultadoBusca(ordenados, exatos.size());
    }

    /** Produtos ativos em ordem de nome, para o seletor da tela de movimentação. */
    @Transactional(readOnly = true)
    public List<Produto> listarParaSelecao(Integer usuarioCodigo) {
        return produtoRepository.findByUsuarioCodigoAndAtivoTrueOrderByNomeAsc(usuarioCodigo);
    }

    /** Produto ativo da loja (com a categoria carregada). */
    @Transactional(readOnly = true)
    public Produto buscarPorId(Integer id, Integer usuarioCodigo) {
        return produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(id, usuarioCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
    }

    /**
     * Cria (id nulo) ou atualiza o produto da loja.
     * <ul>
     *   <li>Criação: o saldo inicial vira uma ENTRADA "Estoque inicial" (RN-04);</li>
     *   <li>Edição: NÃO altera a quantidade (RN-03), mesmo que ela venha no formulário.</li>
     * </ul>
     */
    @Transactional
    public Produto salvar(ProdutoForm form, Usuario usuario) {
        String nome = Textos.limpar(form.getNome());
        if (nome == null) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        // A categoria também precisa ser da loja logada (RN-01)
        Categoria categoria = categoriaRepository.findByIdAndUsuarioCodigo(form.getCategoriaId(), usuario.getCodigo())
                .orElseThrow(() -> new RegraNegocioException("Categoria não encontrada. Selecione outra."));

        if (form.getId() == null) {
            return criar(form, nome, categoria, usuario);
        }
        return atualizar(form, nome, categoria, usuario);
    }

    private Produto criar(ProdutoForm form, String nome, Categoria categoria, Usuario usuario) {
        int estoqueInicial = form.getQuantidade() == null ? 0 : form.getQuantidade();
        if (estoqueInicial < 0) {
            throw new RegraNegocioException("A quantidade deve ser maior ou igual a zero.");
        }
        if (estoqueInicial > ProdutoForm.ESTOQUE_INICIAL_MAXIMO) {
            throw new RegraNegocioException("O estoque inicial deve ser no máximo 1.000.000. "
                    + "Cadastre o produto e registre entradas adicionais.");
        }
        validarNomeUnico(nome, usuario.getCodigo(), -1);

        Produto produto = new Produto();
        produto.setUsuario(usuario);
        preencher(produto, form, nome, categoria);
        produto.setQuantidade(0); // o saldo só nasce por movimentação (RN-03)
        produto.setAtivo(true);
        produto = gravar(produto, nome);

        if (estoqueInicial > 0) {
            // Caminho interno: o motivo "Estoque inicial" é reservado e recusado em movimentar()
            estoqueService.registrarEstoqueInicial(produto.getId(), estoqueInicial, usuario);
        }
        return produto;
    }

    private Produto atualizar(ProdutoForm form, String nome, Categoria categoria, Usuario usuario) {
        // Edição: carrega o produto DA LOJA LOGADA (evita IDOR); o id do formulário nunca é confiável
        Produto produto = buscarPorId(form.getId(), usuario.getCodigo());
        validarNomeUnico(nome, usuario.getCodigo(), produto.getId());
        preencher(produto, form, nome, categoria);
        return gravar(produto, nome);
    }

    /** Copia os campos editáveis. Quantidade e ativo ficam de fora de propósito. */
    private void preencher(Produto produto, ProdutoForm form, String nome, Categoria categoria) {
        produto.setNome(nome);
        produto.setDescricao(Textos.limpar(form.getDescricao()));
        produto.setPreco(form.getPreco());
        produto.setEstoqueMinimo(form.getEstoqueMinimo() == null ? 0 : form.getEstoqueMinimo());
        produto.setCategoria(categoria);
    }

    private Produto gravar(Produto produto, String nome) {
        try {
            // saveAndFlush: se outra requisição criou o mesmo nome agora, o índice único acusa aqui
            return produtoRepository.saveAndFlush(produto);
        } catch (DataIntegrityViolationException e) {
            if (Restricoes.violou(e, Restricoes.UX_PRODUTO_NOME_ATIVO)) {
                throw nomeDuplicado(nome);
            }
            // Outra violação (ex.: categoria removida no meio do caminho): sem vazar detalhes do banco
            throw new RegraNegocioException(Restricoes.MENSAGEM_GENERICA);
        }
    }

    /**
     * Exclusão de produto (RN-08): sem movimentação além do estoque inicial, apaga de vez;
     * com histórico, apenas inativa (some das listagens e do painel; o histórico permanece).
     */
    @Transactional
    public ResultadoExclusao excluir(Integer id, Integer usuarioCodigo) {
        // Bloqueia a linha: impede uma movimentação concorrente entre a decisão e a exclusão
        Produto produto = produtoRepository.buscarAtivoComBloqueio(id, usuarioCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));

        List<MovimentacaoEstoque> historico = movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(id);
        boolean semHistoricoReal = historico.isEmpty()
                || (historico.size() == 1 && ehEstoqueInicial(historico.get(0)));

        // Produto com reserva (mesmo cancelada) guarda histórico de venda: só inativa (RN-08)
        if (semHistoricoReal && !produtoRepository.temReserva(id)) {
            // Só pode haver o registro de estoque inicial: sai antes do produto (chave estrangeira)
            movimentacaoRepository.deleteAll(historico);
            produtoRepository.delete(produto);
            produtoRepository.flush();
            return ResultadoExclusao.EXCLUIDO;
        }
        produto.setAtivo(false);
        produtoRepository.flush();
        return ResultadoExclusao.INATIVADO;
    }

    private boolean ehEstoqueInicial(MovimentacaoEstoque movimentacao) {
        return movimentacao.getTipo() == TipoMovimentacao.ENTRADA
                && movimentacao.getQuantidadeAnterior() == 0
                && EstoqueService.MOTIVO_ESTOQUE_INICIAL.equals(movimentacao.getObservacao());
    }

    /**
     * RN-10: dois produtos ativos da mesma loja não podem ter o mesmo nome. O banco aplica
     * {@code lower(trim())} aos dois lados (igual ao índice único), então o nome vai sem normalizar.
     */
    private void validarNomeUnico(String nome, Integer usuarioCodigo, Integer ignorarId) {
        if (produtoRepository.contarNomeAtivo(usuarioCodigo, nome, ignorarId) > 0) {
            throw nomeDuplicado(nome);
        }
    }

    private RegraNegocioException nomeDuplicado(String nome) {
        return new RegraNegocioException("Já existe um produto ativo chamado \"" + nome + "\" na sua loja.");
    }
}
