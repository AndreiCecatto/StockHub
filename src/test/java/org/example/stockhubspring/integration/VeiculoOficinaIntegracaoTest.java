package org.example.stockhubspring.integration;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.EquipamentoCliente;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.EquipamentoClienteRepository;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.OrdemServicoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** V14 e o veículo da oficina (AD-18, RN-104 a RN-108) contra o PostgreSQL, dentro da transação revertida. */
class VeiculoOficinaIntegracaoTest extends IntegracaoBase {

    private static final String CPF_VALIDO = "529.982.247-25";

    @Autowired
    private OrdemServicoService osService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private EquipamentoClienteRepository equipamentoRepository;
    @Autowired
    private UsuarioAtualService usuarioAtualService;

    private Usuario loja;
    private Usuario dono;
    private Parceiro cliente;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        loja.setTipoLoja(TipoLoja.OFICINA);
        loja.setSubtipo(Subtipo.MECANICA);
        usuarioRepository.saveAndFlush(loja);
        dono = comPapel(loja);
        cliente = clienteService.cadastrar(loja, CPF_VALIDO, "Maria de Teste", "RS");
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario comPapel(Usuario base) {
        Usuario u = new Usuario();
        u.setCodigo(base.getCodigo());
        u.setNome(base.getNome());
        u.setEmail(base.getEmail());
        u.setPapel(PapelMembro.DONO);
        return u;
    }

    @Test
    @DisplayName("V14: o CHECK do banco rejeita placa em formato inválido")
    void checkRejeitaFormatoInvalido() {
        EquipamentoCliente e = new EquipamentoCliente();
        e.setUsuario(loja);
        e.setParceiro(cliente);
        e.setDescricao("Uno");
        e.setPlaca("ab-123");
        assertThatThrownBy(() -> equipamentoRepository.saveAndFlush(e))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V14: o CHECK aceita os dois formatos de placa")
    void checkAceitaFormatosValidos() {
        for (String placa : new String[]{"ABC1234", "ABC1D23"}) {
            EquipamentoCliente e = new EquipamentoCliente();
            e.setUsuario(loja);
            e.setParceiro(cliente);
            e.setDescricao("Carro " + placa);
            e.setPlaca(placa);
            assertThat(equipamentoRepository.saveAndFlush(e).getId()).isNotNull();
        }
    }

    @Test
    @DisplayName("criar grava placa normalizada e modelo no banco e a busca por placa encontra o veículo")
    void gravaEBusca() {
        OrdemServico os = osService.criar(dono, cliente.getId(), "Fiat Uno", null, " abc-1d23 ", "Uno Mille",
                "Barulho", null, 0, null);
        limparSessao();

        var achado = equipamentoRepository.findFirstByUsuarioCodigoAndParceiroIdAndPlaca(
                loja.getCodigo(), cliente.getId(), "ABC1D23");
        assertThat(achado).isPresent();
        assertThat(achado.get().getId()).isEqualTo(os.getEquipamento().getId());
        assertThat(achado.get().getModelo()).isEqualTo("Uno Mille");
    }

    @Test
    @DisplayName("mesma placa no mesmo cliente reaproveita o veículo; em outro cliente cria outro")
    void reaproveitaPorCliente() {
        OrdemServico primeira = osService.criar(dono, cliente.getId(), "Fiat Uno", null, "ABC-1234", "Uno",
                "Barulho", null, 0, null);
        OrdemServico segunda = osService.criar(dono, cliente.getId(), "Outro nome", null, "abc1234", "Outro",
                "Freio", null, 0, null);
        assertThat(segunda.getEquipamento().getId()).isEqualTo(primeira.getEquipamento().getId());
        assertThat(segunda.getEquipamento().getModelo()).isEqualTo("Uno");

        Parceiro outro = clienteService.cadastrar(loja, "111.444.777-35", "Joao de Teste", "RS");
        OrdemServico daOutro = osService.criar(dono, outro.getId(), "Fiat Uno", null, "ABC1234", null,
                "Freio", null, 0, null);
        assertThat(daOutro.getEquipamento().getId()).isNotEqualTo(primeira.getEquipamento().getId());
    }

    @Test
    @DisplayName("placa inválida é recusada pelo service antes de chegar ao banco")
    void placaInvalidaNoService() {
        assertThatThrownBy(() -> osService.criar(dono, cliente.getId(), "Uno", null, "XYZ-99", null, "x", null, 0, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Placa inválida. Use o formato ABC-1234 ou ABC1D23.");
    }

    @Test
    @DisplayName("loja MECANICA logada, sem mock: camposPermitidos devolve placa e modelo")
    void camposDaLojaLogada() {
        limparSessao();
        UsuarioPrincipal principal = new UsuarioPrincipal(loja);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        var campos = osService.camposPermitidos(usuarioAtualService.getUsuarioAtual());

        assertThat(campos.placa()).isTrue();
        assertThat(campos.modelo()).isTrue();
        assertThat(campos.temVeiculo()).isTrue();
    }

    @Test
    @DisplayName("loja de outro ramo, sem mock: camposPermitidos não devolve veículo")
    void outroRamoSemVeiculo() {
        jdbc.update("update usuario set subtipo = 'ASSISTENCIA_TECNICA' where codigo = ?", loja.getCodigo());
        limparSessao();
        assertThat(osService.camposPermitidos(dono).temVeiculo()).isFalse();
    }
}
