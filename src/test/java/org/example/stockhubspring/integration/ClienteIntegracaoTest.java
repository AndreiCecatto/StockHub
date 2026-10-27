package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.ClienteFiltro;
import org.example.stockhubspring.dto.ClienteForm;
import org.example.stockhubspring.dto.ClienteLinha;
import org.example.stockhubspring.dto.ClienteResumo;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.OrdemServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration V12 e o cadastro completo de cliente (RN-91 a RN-98) contra o PostgreSQL. Tudo dentro da transação do
 * teste, sempre revertida; dados sintéticos.
 */
class ClienteIntegracaoTest extends IntegracaoBase {

    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private OrdemServicoService osService;
    @Autowired
    private org.example.stockhubspring.repository.ParceiroRepository parceiroRepository;

    private Usuario loja;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
    }

    private static ClienteForm completo(String documento, String nome) {
        return new ClienteForm(null, documento, nome, "Fantasia " + nome, "Contato@Exemplo.com", "(55) 3222-1000",
                "(55) 99999-0000", "97010-000", "Rua das Flores", "100", "Sala 2", "Centro", "Santa Maria", "rs",
                "1985-03-10", "Prefere atendimento à tarde", "Indicação do João");
    }

    private Usuario comPapel(Usuario base, PapelMembro papel) {
        Usuario u = new Usuario();
        u.setCodigo(base.getCodigo());
        u.setNome(base.getNome());
        u.setEmail(base.getEmail());
        u.setPapel(papel);
        return u;
    }

    private List<String> nomes(Usuario daLoja, ClienteFiltro filtro) {
        return clienteService.listarPagina(daLoja.getCodigo(), filtro).stream()
                .map(l -> l.cliente().getNome()).toList();
    }

    @Test
    @DisplayName("V12: grava todos os campos novos e lê de volta, só com dígitos nos números")
    void gravaCamposNovos() {
        Parceiro c = clienteService.cadastrar(loja, completo(CPF, "Ana Souza"));
        limparSessao();

        Parceiro lido = parceiroRepository.findById(c.getId()).orElseThrow();
        assertThat(lido.getTipoPessoa()).isEqualTo("FISICA");
        assertThat(lido.getDocumento()).isEqualTo(CPF);
        assertThat(lido.getEmail()).isEqualTo("contato@exemplo.com");
        assertThat(lido.getTelefone()).isEqualTo("5532221000");
        assertThat(lido.getWhatsapp()).isEqualTo("55999990000");
        assertThat(lido.getCep()).isEqualTo("97010000");
        assertThat(lido.getLogradouro()).isEqualTo("Rua das Flores");
        assertThat(lido.getCidade()).isEqualTo("Santa Maria");
        assertThat(lido.getUf()).isEqualTo("RS");
        assertThat(lido.getDataNascimento()).isEqualTo(LocalDate.of(1985, 3, 10));
        assertThat(lido.getObservacoes()).isEqualTo("Prefere atendimento à tarde");
        assertThat(lido.getOrigem()).isEqualTo("Indicação do João");
        assertThat(lido.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("V12: cliente sem documento pode se repetir; com documento é único por loja")
    void documentoOpcionalEUnico() {
        Parceiro a = clienteService.cadastrar(loja, ClienteForm.basico(null, "Sem Documento A", null));
        Parceiro b = clienteService.cadastrar(loja, ClienteForm.basico("", "Sem Documento B", null));
        assertThat(a.getDocumento()).isNull();
        assertThat(b.getId()).isNotEqualTo(a.getId());

        clienteService.cadastrar(loja, ClienteForm.basico(CPF, "Ana", null));
        assertThatThrownBy(() -> clienteService.cadastrar(loja, ClienteForm.basico("529.982.247-25", "Outra", null)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");

        // outra loja pode ter o mesmo CPF
        Usuario outra = novoUsuario();
        assertThat(clienteService.cadastrar(outra, ClienteForm.basico(CPF, "Ana da outra", null)).getId()).isNotNull();
    }

    @Test
    @DisplayName("V12: o banco recusa telefone fora do formato")
    void restricoesDoBanco() {
        Parceiro c = clienteService.cadastrar(loja, ClienteForm.basico(null, "Ana", null));
        limparSessao();
        assertThatThrownBy(() -> jdbc.update("update parceiro set telefone = '12ab' where id = ?", c.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V12: tipo de pessoa inválido é recusado pelo banco")
    void tipoPessoaNoBanco() {
        Parceiro c = clienteService.cadastrar(loja, ClienteForm.basico(null, "Ana", null));
        limparSessao();
        assertThatThrownBy(() -> jdbc.update("update parceiro set tipo_pessoa = 'ROBO' where id = ?", c.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("lista: busca por nome (sem acento), documento, telefone e e-mail")
    void buscaPorCampos() {
        clienteService.cadastrar(loja, completo(CPF, "José Conceição"));
        clienteService.cadastrar(loja, new ClienteForm(null, CNPJ, "Mercado Boa Compra", null, "compras@boa.com",
                "5530001111", null, null, null, null, null, null, null, null, null, null, null));
        clienteService.cadastrar(loja, ClienteForm.basico(null, "Pedro Alves", null));

        assertThat(nomes(loja, ClienteFiltro.de("jose conceicao", null, null, null, 1))).containsExactly("José Conceição");
        assertThat(nomes(loja, ClienteFiltro.de("529.982", null, null, null, 1))).containsExactly("José Conceição");
        assertThat(nomes(loja, ClienteFiltro.de("(55) 3000-11", null, null, null, 1))).containsExactly("Mercado Boa Compra");
        assertThat(nomes(loja, ClienteFiltro.de("99999-0000", null, null, null, 1))).containsExactly("José Conceição");
        assertThat(nomes(loja, ClienteFiltro.de("COMPRAS@boa", null, null, null, 1))).containsExactly("Mercado Boa Compra");
        assertThat(nomes(loja, ClienteFiltro.de("Fantasia Jos", null, null, null, 1))).containsExactly("José Conceição");
        assertThat(nomes(loja, ClienteFiltro.de("50%", null, null, null, 1))).isEmpty();
        assertThat(nomes(loja, ClienteFiltro.de("'; drop table parceiro; --", null, null, null, 1))).isEmpty();
        assertThat(nomes(loja, ClienteFiltro.de(null, null, null, null, 1)))
                .containsExactly("José Conceição", "Mercado Boa Compra", "Pedro Alves");
    }

    @Test
    @DisplayName("lista: filtros de situação, com e sem OS, ordem e isolamento por loja")
    void filtrosEIsolamento() {
        Parceiro ana = clienteService.cadastrar(loja, ClienteForm.basico(CPF, "Ana", null));
        Parceiro bia = clienteService.cadastrar(loja, ClienteForm.basico(null, "Bia", null));
        Parceiro caio = clienteService.cadastrar(loja, ClienteForm.basico(CNPJ, "Caio", null));
        clienteService.alterarSituacao(loja.getCodigo(), caio.getId(), false);
        osService.criar(comPapel(loja, PapelMembro.DONO), ana.getId(), "Fiat Uno", null, "Barulho", null, 0, null);
        osService.criar(comPapel(loja, PapelMembro.DONO), ana.getId(), "Moto", null, "Não pega", null, 0, null);
        Usuario outra = novoUsuario();
        clienteService.cadastrar(outra, ClienteForm.basico(null, "Da outra loja", null));

        assertThat(nomes(loja, ClienteFiltro.de(null, null, null, null, 1))).containsExactly("Ana", "Bia");
        assertThat(nomes(loja, ClienteFiltro.de(null, "inativos", null, null, 1))).containsExactly("Caio");
        assertThat(nomes(loja, ClienteFiltro.de(null, "todos", null, null, 1))).containsExactly("Ana", "Bia", "Caio");
        assertThat(nomes(loja, ClienteFiltro.de(null, "todos", "com", null, 1))).containsExactly("Ana");
        assertThat(nomes(loja, ClienteFiltro.de(null, "todos", "sem", null, 1))).containsExactly("Bia", "Caio");
        assertThat(nomes(loja, ClienteFiltro.de(null, "todos", null, "recentes", 1))).containsExactly("Caio", "Bia", "Ana");
        assertThat(nomes(outra, ClienteFiltro.de(null, "todos", null, null, 1))).containsExactly("Da outra loja");

        List<ClienteLinha> linhas = clienteService.listarPagina(loja.getCodigo(),
                ClienteFiltro.de(null, "todos", null, null, 1)).getContent();
        assertThat(linhas).extracting(ClienteLinha::totalOs).containsExactly(2L, 0L, 0L);

        // o inativo some da escolha na OS e não abre OS nova
        assertThat(clienteService.listar(loja.getCodigo())).extracting(Parceiro::getNome).containsExactly("Ana", "Bia");
        assertThatThrownBy(() -> osService.criar(comPapel(loja, PapelMembro.DONO), caio.getId(), "Moto", null, "x", null, 0, null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("inativo");
        clienteService.alterarSituacao(loja.getCodigo(), caio.getId(), true);
        assertThat(osService.criar(comPapel(loja, PapelMembro.DONO), caio.getId(), "Moto", null, "x", null, 0, null)).isNotNull();
    }

    @Test
    @DisplayName("lista: paginação pega a última página quando a pedida passa do fim")
    void paginacao() {
        for (int i = 0; i < 3; i++) {
            clienteService.cadastrar(loja, ClienteForm.basico(null, "Cliente " + i, null));
        }
        var pagina = clienteService.listarPagina(loja.getCodigo(),
                new ClienteFiltro(null, null, null, null, 99, 2));
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getNumber()).isEqualTo(1);
        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("detalhe: resumo com OS, total entregue e última visita; outra loja é 404")
    void resumoEIsolamento() {
        Parceiro ana = clienteService.cadastrar(loja, ClienteForm.basico(CPF, "Ana", null));
        Usuario dono = comPapel(loja, PapelMembro.DONO);
        OrdemServico os1 = osService.criar(dono, ana.getId(), "Fiat Uno", null, "Barulho", null, 0, null);
        osService.adicionarServico(dono, os1.getId(), "Revisão", new BigDecimal("80.00"));
        for (int i = 0; i < 4; i++) {
            osService.avancar(dono, os1.getId());
        }
        osService.criar(dono, ana.getId(), "Moto", null, "Não pega", null, 0, null);
        OrdemServico os3 = osService.criar(dono, ana.getId(), "Bicicleta", null, "Pneu", null, 0, null);
        osService.cancelar(dono, os3.getId());

        ClienteResumo r = clienteService.resumo(loja.getCodigo(), ana.getId());

        assertThat(r.totalOs()).isEqualTo(3);
        assertThat(r.osEntregues()).isEqualTo(1);
        assertThat(r.osEmAndamento()).isEqualTo(1);
        assertThat(r.totalEntregue()).isEqualByComparingTo("80.00");
        assertThat(r.ultimaVisita()).isNotNull();
        assertThat(r.ultimas()).extracting(OrdemServico::getNumero).containsExactly(3, 2, 1);

        Usuario outra = novoUsuario();
        assertThatThrownBy(() -> clienteService.buscar(outra.getCodigo(), ana.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> clienteService.atualizar(outra.getCodigo(), ana.getId(), ClienteForm.basico(null, "X", null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> clienteService.alterarSituacao(outra.getCodigo(), ana.getId(), false))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(clienteService.resumo(outra.getCodigo(), ana.getId()).totalOs()).isZero();
    }

    @Test
    @DisplayName("resumo de cliente sem OS vem zerado, sem inventar valores")
    void resumoVazio() {
        Parceiro ana = clienteService.cadastrar(loja, ClienteForm.basico(null, "Ana", null));
        ClienteResumo r = clienteService.resumo(loja.getCodigo(), ana.getId());
        assertThat(r.totalOs()).isZero();
        assertThat(r.totalEntregue()).isEqualByComparingTo("0");
        assertThat(r.ultimaVisita()).isNull();
        assertThat(r.ultimas()).isEmpty();
    }

    @Test
    @DisplayName("editar atualiza os dados, inclusive limpar campos, e respeita a unicidade")
    void editar() {
        Parceiro ana = clienteService.cadastrar(loja, completo(CPF, "Ana"));
        Parceiro outro = clienteService.cadastrar(loja, ClienteForm.basico(CNPJ, "Empresa", null));

        clienteService.atualizar(loja.getCodigo(), ana.getId(), ClienteForm.basico(CPF, "Ana Maria", null));
        limparSessao();
        Parceiro lido = parceiroRepository.findById(ana.getId()).orElseThrow();
        assertThat(lido.getNome()).isEqualTo("Ana Maria");
        assertThat(lido.getEmail()).isNull();
        assertThat(lido.getAtualizadoEm()).isNotNull();

        assertThatThrownBy(() -> clienteService.atualizar(loja.getCodigo(), outro.getId(), ClienteForm.basico(CPF, "Empresa", null)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
    }
}
