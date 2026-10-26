package org.example.stockhubspring.integration;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Última defesa: restrições e índices do banco (migration V2) e o cadastro de usuário (RN-14).
 * Cada violação aborta a transação do PostgreSQL, por isso cada teste provoca no máximo UMA violação
 * (e a transação do teste é revertida no fim).
 */
class BancoRestricoesIntegracaoTest extends IntegracaoBase {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioDetailsService usuarioDetailsService;

    private Usuario loja;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
    }

    private MovimentacaoEstoque movimentacao(Produto produto, TipoMovimentacao tipo, int quantidade, int anterior, int nova) {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto);
        mov.setUsuario(loja);
        mov.setTipo(tipo);
        mov.setQuantidade(quantidade);
        mov.setQuantidadeAnterior(anterior);
        mov.setQuantidadeNova(nova);
        return mov;
    }

    // ----- Índices únicos -----

    @Test
    @DisplayName("o banco recusa dois produtos ativos com o mesmo nome (sem caixa e sem espaços) na mesma loja")
    void bancoRecusaProdutoAtivoDuplicado() {
        novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> novoProduto(loja, categoria, "  CANETA ", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco aceita nome repetido quando um dos produtos está inativo ou é de outra loja")
    void bancoAceitaNomeRepetidoSeInativoOuOutraLoja() {
        Produto antigo = novoProduto(loja, categoria, "Caneta", 1, 0);
        antigo.setAtivo(false);
        produtoRepository.saveAndFlush(antigo);
        Usuario outra = novoUsuario();

        assertThatCode(() -> {
            novoProduto(loja, categoria, "Caneta", 1, 0);
            novoProduto(outra, novaCategoria(outra, "Geral"), "Caneta", 1, 0);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o banco recusa duas categorias com o mesmo nome (sem caixa e sem espaços) na mesma loja")
    void bancoRecusaCategoriaDuplicada() {
        assertThatThrownBy(() -> novaCategoria(loja, "  GERAL "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa dois usuários com o mesmo e-mail")
    void bancoRecusaEmailDuplicado() {
        Usuario copia = new Usuario();
        copia.setNome("Outra");
        copia.setEmail(loja.getEmail());
        copia.setSenha("x");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(copia))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- CHECKs -----

    @Test
    @DisplayName("o banco recusa saldo de produto negativo")
    void bancoRecusaSaldoNegativo() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> jdbc.update("update produto set quantidade = -1 where id = ?", produto.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa preço negativo")
    void bancoRecusaPrecoNegativo() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> jdbc.update("update produto set preco = -0.01 where id = ?", produto.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa preço acima de 9.999.999,99 (NUMERIC(9,2))")
    void bancoRecusaPrecoGrandeDemais() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> jdbc.update("update produto set preco = 10000000.00 where id = ?", produto.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa nome de produto com mais de 120 caracteres")
    void bancoRecusaNomeLongo() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> jdbc.update("update produto set nome = repeat('a', 121) where id = ?", produto.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa ENTRADA ou SAIDA com quantidade zero")
    void bancoRecusaEntradaComQuantidadeZero() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> movimentacaoRepository.saveAndFlush(
                movimentacao(produto, TipoMovimentacao.ENTRADA, 0, 1, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa saldo anterior ou novo negativo na movimentação")
    void bancoRecusaSaldoNegativoNaMovimentacao() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> movimentacaoRepository.saveAndFlush(
                movimentacao(produto, TipoMovimentacao.SAIDA, 5, 1, -4)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco aceita AJUSTE com contagem final zero")
    void bancoAceitaAjusteZero() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 3, 0);

        MovimentacaoEstoque salva = movimentacaoRepository.saveAndFlush(
                movimentacao(produto, TipoMovimentacao.AJUSTE, 0, 3, 0));

        assertThat(salva.getId()).isNotNull();
    }

    @Test
    @DisplayName("o banco recusa tipo de movimentação desconhecido")
    void bancoRecusaTipoDesconhecido() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 3, 0);

        assertThatThrownBy(() -> jdbc.update(
                "insert into movimentacao_estoque (produto_id, usuario_codigo, tipo, quantidade, quantidade_anterior,"
                        + " quantidade_nova, criado_em) values (?, ?, 'TRANSFERENCIA', 1, 3, 4, now())",
                produto.getId(), loja.getCodigo()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa apagar produto que tem movimentações (chave estrangeira)")
    void bancoRecusaApagarProdutoComHistorico() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 3, 0);
        movimentacaoRepository.saveAndFlush(movimentacao(produto, TipoMovimentacao.ENTRADA, 3, 0, 3));

        assertThatThrownBy(() -> jdbc.update("delete from produto where id = ?", produto.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa apagar categoria que tem produtos (chave estrangeira)")
    void bancoRecusaApagarCategoriaComProdutos() {
        novoProduto(loja, categoria, "Caneta", 3, 0);

        assertThatThrownBy(() -> jdbc.update("delete from categoria where id = ?", categoria.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ----- Cadastro de usuário no banco real (RN-14) -----

    private String emailNovo() {
        return "Cad-" + UUID.randomUUID() + "@Teste.Invalid";
    }

    @Test
    @DisplayName("cadastro grava e-mail em minúsculas e senha com hash BCrypt (nunca a senha digitada)")
    void cadastroGravaEmailNormalizadoESenhaComHash() {
        String email = emailNovo();

        Usuario criado = usuarioService.cadastrarLoja("  Loja Nova ", "  " + email + " ", "senha-forte-123");
        limparSessao();

        String emailGravado = jdbc.queryForObject("select email from usuario where codigo = ?", String.class,
                criado.getCodigo());
        String senhaGravada = jdbc.queryForObject("select senha from usuario where codigo = ?", String.class,
                criado.getCodigo());
        assertThat(emailGravado).isEqualTo(email.toLowerCase());
        assertThat(senhaGravada).isNotEqualTo("senha-forte-123").startsWith("$2");
        assertThat(passwordEncoder.matches("senha-forte-123", senhaGravada)).isTrue();
        assertThat(jdbc.queryForObject("select nome from usuario where codigo = ?", String.class, criado.getCodigo()))
                .isEqualTo("Loja Nova");
    }

    @Test
    @DisplayName("cadastro com e-mail já usado (mesmo com outra caixa) responde com a mensagem genérica")
    void cadastroDuplicadoMensagemGenerica() {
        String email = emailNovo();
        usuarioService.cadastrarLoja("Loja A", email, "senha-forte-123");
        limparSessao();

        assertThatThrownBy(() -> usuarioService.cadastrarLoja("Loja B", email.toUpperCase(), "outra-senha-123"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não foi possível concluir o cadastro. Verifique os dados e tente novamente.");
    }

    @Test
    @DisplayName("e-mail de usuário INATIVO também conta como duplicado")
    void emailDeInativoContaComoDuplicado() {
        String email = emailNovo();
        Usuario existente = usuarioService.cadastrarLoja("Loja A", email, "senha-forte-123");
        existente.setAtivo(false);
        usuarioRepository.saveAndFlush(existente);
        limparSessao();

        assertThatThrownBy(() -> usuarioService.cadastrarLoja("Loja B", email, "outra-senha-123"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não foi possível concluir o cadastro. Verifique os dados e tente novamente.");
    }

    @Test
    @DisplayName("login: carrega usuário ativo pelo e-mail normalizado e a senha confere com o hash")
    void loginCarregaUsuarioAtivo() {
        String email = emailNovo();
        usuarioService.cadastrarLoja("Loja A", email, "senha-forte-123");
        limparSessao();

        UserDetails detalhes = usuarioDetailsService.loadUserByUsername("  " + email.toUpperCase() + " ");

        assertThat(detalhes.getUsername()).isEqualTo(email.toLowerCase());
        assertThat(passwordEncoder.matches("senha-forte-123", detalhes.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("senha-errada-123", detalhes.getPassword())).isFalse();
    }

    @Test
    @DisplayName("login: usuário inativo não é carregado")
    void loginNaoCarregaInativo() {
        String email = emailNovo();
        Usuario criado = usuarioService.cadastrarLoja("Loja A", email, "senha-forte-123");
        criado.setAtivo(false);
        usuarioRepository.saveAndFlush(criado);
        limparSessao();

        assertThatThrownBy(() -> usuarioDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThat(usuarioRepository.existsByEmail(email.toLowerCase())).isTrue();
        assertThat(usuarioRepository.findByEmailAndAtivoTrue(email.toLowerCase())).isEmpty();
    }
}
