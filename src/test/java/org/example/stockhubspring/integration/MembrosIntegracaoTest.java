package org.example.stockhubspring.integration;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.example.stockhubspring.service.MembroService;
import org.example.stockhubspring.service.UsuarioDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Loja x pessoa (V9, AD-13) no PostgreSQL real; tudo revertido no fim de cada teste. */
class MembrosIntegracaoTest extends IntegracaoBase {

    @Autowired
    private MembroLojaRepository membroRepository;
    @Autowired
    private MembroService membroService;
    @Autowired
    private UsuarioDetailsService detailsService;
    @Autowired
    private PasswordEncoder encoder;

    private static String email() {
        return "it-" + UUID.randomUUID() + "@teste.invalid";
    }

    private MembroLoja novoMembro(Usuario loja, PapelMembro papel) {
        return membroService.criar(loja.getCodigo(), PapelMembro.DONO, "Pessoa", email(), "senha-inicial-1", papel);
    }

    private static MembroLoja montar(Integer loja, String email, PapelMembro papel) {
        MembroLoja m = new MembroLoja();
        m.setLojaCodigo(loja);
        m.setNome("X");
        m.setEmail(email);
        m.setSenha("hash");
        m.setPapel(papel);
        return m;
    }

    @Test
    @DisplayName("cadastrar a loja cria o DONO ativo com o mesmo e-mail e o mesmo hash, e o login carrega a loja")
    void cadastroCriaDonoELogaNaLoja() {
        String email = email();
        Usuario loja = usuarioService.cadastrarLoja("Loja X", email, "senha-forte-123");
        limparSessao();

        MembroLoja dono = membroRepository.findByEmailAndAtivoTrue(email).orElseThrow();
        assertThat(dono.getPapel()).isEqualTo(PapelMembro.DONO);
        assertThat(dono.getLojaCodigo()).isEqualTo(loja.getCodigo());
        assertThat(dono.getSenha()).isEqualTo(usuarioRepository.findById(loja.getCodigo()).orElseThrow().getSenha());

        UsuarioPrincipal principal = (UsuarioPrincipal) detailsService.loadUserByUsername(email.toUpperCase());
        assertThat(principal.getCodigo()).isEqualTo(loja.getCodigo());
        assertThat(principal.getPapel()).isEqualTo(PapelMembro.DONO);
        assertThat(encoder.matches("senha-forte-123", principal.getPassword())).isTrue();
    }

    @Test
    @DisplayName("o banco garante um único DONO por loja")
    void umUnicoDonoPorLoja() {
        Usuario loja = usuarioService.cadastrarLoja("Loja X", email(), "senha-forte-123");
        limparSessao();

        MembroLoja segundoDono = montar(loja.getCodigo(), email(), PapelMembro.DONO);
        assertThatThrownBy(() -> membroRepository.saveAndFlush(segundoDono))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("e-mail de uma pessoa já usado em outra loja é recusado com mensagem genérica")
    void emailUnicoEntreLojas() {
        Usuario a = usuarioService.cadastrarLoja("Loja A", email(), "senha-forte-123");
        Usuario b = usuarioService.cadastrarLoja("Loja B", email(), "senha-forte-123");
        String repetido = email();
        membroService.criar(a.getCodigo(), PapelMembro.DONO, "Outra", repetido, "senha-inicial-1", PapelMembro.OPERADOR);

        assertThatThrownBy(() -> membroService.criar(b.getCodigo(), PapelMembro.DONO, "Outra", repetido,
                "senha-inicial-1", PapelMembro.OPERADOR))
                .hasMessage(MembroService.MSG_CRIACAO_GENERICA);
    }

    @Test
    @DisplayName("isolamento: a equipe listada é só da loja pedida e não se mexe em membro de outra loja")
    void isolamentoEntreLojas() {
        Usuario a = usuarioService.cadastrarLoja("Loja A", email(), "senha-forte-123");
        Usuario b = usuarioService.cadastrarLoja("Loja B", email(), "senha-forte-123");
        MembroLoja deA = novoMembro(a, PapelMembro.OPERADOR);
        MembroLoja deB = novoMembro(b, PapelMembro.OPERADOR);
        limparSessao();

        assertThat(membroService.listar(a.getCodigo())).extracting(MembroLoja::getId)
                .contains(deA.getId()).doesNotContain(deB.getId());
        assertThat(membroService.listar(b.getCodigo())).extracting(MembroLoja::getId)
                .contains(deB.getId()).doesNotContain(deA.getId());

        assertThatThrownBy(() -> membroService.desativar(a.getCodigo(), 0, PapelMembro.DONO, deB.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> membroService.reativar(a.getCodigo(), PapelMembro.DONO, deB.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(membroRepository.findById(deB.getId()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    @DisplayName("membro desativado não entra mais, mas continua no banco")
    void desativadoNaoEntra() {
        Usuario loja = usuarioService.cadastrarLoja("Loja A", email(), "senha-forte-123");
        MembroLoja op = novoMembro(loja, PapelMembro.OPERADOR);
        limparSessao();
        assertThat(detailsService.loadUserByUsername(op.getEmail()).isEnabled()).isTrue();

        membroService.desativar(loja.getCodigo(), 0, PapelMembro.DONO, op.getId());
        limparSessao();

        assertThatThrownBy(() -> detailsService.loadUserByUsername(op.getEmail()))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThat(membroRepository.existsByIdAndAtivoTrue(op.getId())).isFalse();
        assertThat(membroRepository.existsById(op.getId())).isTrue();
    }

    @Test
    @DisplayName("a movimentação grava quem fez (membro_id) e fica NULL quando não há membro")
    void movimentacaoGravaQuemFez() {
        Usuario loja = novoUsuario();
        MembroLoja pessoa = membroRepository.saveAndFlush(montar(loja.getCodigo(), email(), PapelMembro.OPERADOR));
        Categoria categoria = novaCategoria(loja, "Geral");
        Produto produto = novoProduto(loja, categoria, "Caneta", 10, 1);

        loja.setMembroId(pessoa.getId());
        MovimentacaoEstoque feita = estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 2, "Venda", loja);
        loja.setMembroId(null);
        MovimentacaoEstoque semMembro = estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 1, null, loja);
        limparSessao();

        assertThat(jdbc.queryForObject("select membro_id from movimentacao_estoque where id = ?", Integer.class,
                feita.getId())).isEqualTo(pessoa.getId());
        assertThat(jdbc.queryForObject("select membro_id from movimentacao_estoque where id = ?", Integer.class,
                semMembro.getId())).isNull();
    }
}
