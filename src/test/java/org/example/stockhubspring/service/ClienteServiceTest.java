package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.ClienteFiltro;
import org.example.stockhubspring.dto.ClienteForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.OrdemServicoRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cadastro completo de cliente (RN-91 a RN-96) com os repositórios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteServiceTest {

    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";

    @Mock
    private ParceiroRepository parceiroRepository;
    @Mock
    private OrdemServicoRepository osRepository;

    private ClienteService service;
    private Usuario loja;

    @BeforeEach
    void preparar() {
        service = new ClienteService(parceiroRepository, osRepository);
        loja = Fabrica.usuario(7);
        when(parceiroRepository.saveAndFlush(any(Parceiro.class))).thenAnswer(i -> i.getArgument(0));
        when(parceiroRepository.save(any(Parceiro.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static ClienteForm form(String documento, String nome) {
        return ClienteForm.basico(documento, nome, null);
    }

    private static ClienteForm comCampo(String campo, String valor) {
        return new ClienteForm(
                null, null, "Ana",
                "nomeFantasia".equals(campo) ? valor : null, "email".equals(campo) ? valor : null,
                "telefone".equals(campo) ? valor : null, "whatsapp".equals(campo) ? valor : null,
                "cep".equals(campo) ? valor : null, null, null, null, null, null,
                "uf".equals(campo) ? valor : null, "data".equals(campo) ? valor : null, null, null);
    }

    private Parceiro clienteExistente(int id, Usuario dono, String documento) {
        Parceiro p = new Parceiro();
        p.setUsuario(dono);
        p.setNome("Antigo");
        p.setDocumento(documento);
        p.setCliente(true);
        p.setFornecedor(false);
        when(parceiroRepository.findByIdAndUsuarioCodigo(id, dono.getCodigo())).thenReturn(Optional.of(p));
        return p;
    }

    // ---------- cadastro ----------

    @Test
    @DisplayName("só o nome é obrigatório: cliente sem documento é cadastrado")
    void semDocumento() {
        Parceiro c = service.cadastrar(loja, form(null, "  Ana  "));

        assertThat(c.getNome()).isEqualTo("Ana");
        assertThat(c.getDocumento()).isNull();
        assertThat(c.getTipoPessoa()).isNull();
        assertThat(c.isCliente()).isTrue();
        assertThat(c.isAtivo()).isTrue();
        assertThat(c.getUsuario()).isSameAs(loja);
        verify(parceiroRepository, never()).findByUsuarioCodigoAndDocumento(any(), any());
    }

    @Test
    @DisplayName("sem nome não cadastra")
    void semNome() {
        assertThatThrownBy(() -> service.cadastrar(loja, form(CPF, "  ")))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("nome");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "11111111111", "52998224726", "11222333000100", "5299822472"})
    @DisplayName("CPF ou CNPJ informado e inválido é recusado")
    void documentoInvalido(String documento) {
        assertThatThrownBy(() -> service.cadastrar(loja, form(documento, "Ana")))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("CPF ou CNPJ");
        verify(parceiroRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("CPF e CNPJ válidos (com pontuação) são guardados só com dígitos e deduzem o tipo de pessoa")
    void documentoValidoDeduzTipo() {
        assertThat(service.cadastrar(loja, form("529.982.247-25", "Ana")).getDocumento()).isEqualTo(CPF);
        Parceiro empresa = service.cadastrar(loja, form("11.222.333/0001-81", "Empresa"));
        assertThat(empresa.getDocumento()).isEqualTo(CNPJ);
        assertThat(empresa.getTipoPessoa()).isEqualTo("JURIDICA");
        assertThat(service.cadastrar(loja, form(CPF, "Ana")).getTipoPessoa()).isEqualTo("FISICA");
    }

    @Test
    @DisplayName("tipo de pessoa que contradiz o documento é recusado; inválido também")
    void tipoDePessoa() {
        ClienteForm conflito = new ClienteForm("JURIDICA", CPF, "Ana", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        assertThatThrownBy(() -> service.cadastrar(loja, conflito))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("não combina");
        ClienteForm estranho = new ClienteForm("ROBO", null, "Ana", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        assertThatThrownBy(() -> service.cadastrar(loja, estranho)).isInstanceOf(RegraNegocioException.class);
        ClienteForm semDocumento = new ClienteForm("juridica", null, "Ana", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        assertThat(service.cadastrar(loja, semDocumento).getTipoPessoa()).isEqualTo("JURIDICA");
    }

    @Test
    @DisplayName("quem já é cliente da loja não se cadastra de novo com o mesmo documento")
    void unicoPorLoja() {
        Parceiro existente = new Parceiro();
        existente.setCliente(true);
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, CPF)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.cadastrar(loja, form(CPF, "Ana")))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe um cliente");
        verify(parceiroRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("fornecedor vira também cliente: mantém o nome e só recebe os campos que estavam vazios")
    void fornecedorViraCliente() {
        Parceiro fornecedor = new Parceiro();
        fornecedor.setNome("Nome do fornecedor");
        fornecedor.setDocumento(CNPJ);
        fornecedor.setTelefone("5530000000");
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, CNPJ)).thenReturn(Optional.of(fornecedor));
        ClienteForm f = new ClienteForm(null, CNPJ, "Outro nome", null, "Contato@Empresa.com", "5599990000", null,
                null, null, null, null, null, "Santa Maria", "rs", null, null, null);

        Parceiro c = service.cadastrar(loja, f);

        assertThat(c).isSameAs(fornecedor);
        assertThat(c.isCliente()).isTrue();
        assertThat(c.isFornecedor()).isTrue();
        assertThat(c.getNome()).isEqualTo("Nome do fornecedor");
        assertThat(c.getTelefone()).isEqualTo("5530000000");
        assertThat(c.getEmail()).isEqualTo("contato@empresa.com");
        assertThat(c.getCidade()).isEqualTo("Santa Maria");
        assertThat(c.getUf()).isEqualTo("RS");
    }

    @Test
    @DisplayName("campos opcionais: e-mail, telefone, CEP, UF e data são conferidos")
    void camposOpcionais() {
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("email", "sem-arroba")))
                .hasMessageContaining("e-mail");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("telefone", "1234")))
                .hasMessageContaining("telefone");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("whatsapp", "123456789012345")))
                .hasMessageContaining("WhatsApp");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("cep", "9701")))
                .hasMessageContaining("CEP");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("uf", "RSX")))
                .hasMessageContaining("UF");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("data", "31/12/1990")))
                .hasMessageContaining("data");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("data", LocalDate.now().plusDays(2).toString())))
                .hasMessageContaining("futura");
        assertThatThrownBy(() -> service.cadastrar(loja, comCampo("nomeFantasia", "x".repeat(256))))
                .hasMessageContaining("255");
        verify(parceiroRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("telefone, WhatsApp e CEP são guardados só com dígitos; +55 é descartado")
    void normalizaContato() {
        ClienteForm f = new ClienteForm(null, null, "Ana", null, null, "(55) 3222-1000", "+55 (55) 99999-0000",
                "97010-000", "Rua A", "10", null, "Centro", "Santa Maria", "rs", "1990-05-20", "obs", "indicação");

        Parceiro c = service.cadastrar(loja, f);

        assertThat(c.getTelefone()).isEqualTo("5532221000");
        assertThat(c.getWhatsapp()).isEqualTo("55999990000");
        assertThat(c.getCep()).isEqualTo("97010000");
        assertThat(c.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(c.getOrigem()).isEqualTo("indicação");
    }

    @Test
    @DisplayName("cadastro antigo de três campos continua exigindo o documento")
    void cadastroAntigoExigeDocumento() {
        assertThatThrownBy(() -> service.cadastrar(loja, "", "Ana", null)).isInstanceOf(RegraNegocioException.class);
        assertThat(service.cadastrar(loja, CPF, "Ana", "rs").getUf()).isEqualTo("RS");
    }

    // ---------- edição, situação e isolamento ----------

    @Test
    @DisplayName("cliente de outra loja (ou que não é cliente) é 404 na busca, edição e situação")
    void isolamentoPorLoja() {
        when(parceiroRepository.findByIdAndUsuarioCodigo(5, 7)).thenReturn(Optional.empty());
        Parceiro soFornecedor = new Parceiro();
        soFornecedor.setCliente(false);
        when(parceiroRepository.findByIdAndUsuarioCodigo(6, 7)).thenReturn(Optional.of(soFornecedor));

        assertThatThrownBy(() -> service.buscar(7, 5)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscar(7, 6)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.atualizar(7, 5, form(null, "Ana")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.alterarSituacao(7, 5, false))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(parceiroRepository, never()).saveAndFlush(any());
        verify(parceiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar troca os dados e aceita incluir o documento")
    void atualiza() {
        Parceiro p = clienteExistente(5, loja, null);

        service.atualizar(7, 5, form(CPF, "Ana Maria"));

        assertThat(p.getNome()).isEqualTo("Ana Maria");
        assertThat(p.getDocumento()).isEqualTo(CPF);
        assertThat(p.getTipoPessoa()).isEqualTo("FISICA");
        assertThat(p.getAtualizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("atualizar não aceita documento de outro cadastro da loja nem documento inválido")
    void atualizaUnicidade() {
        Parceiro p = clienteExistente(5, loja, null);
        Parceiro outro = new Parceiro();
        outro.setCliente(true);
        org.springframework.test.util.ReflectionTestUtils.setField(outro, "id", 99);
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, CPF)).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.atualizar(7, 5, form(CPF, "Ana"))).hasMessageContaining("Já existe um cliente");
        assertThatThrownBy(() -> service.atualizar(7, 5, form("11111111111", "Ana")))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(p.getDocumento()).isNull();
    }

    @Test
    @DisplayName("manter o próprio documento ao editar não é duplicidade")
    void mantemProprioDocumento() {
        Parceiro p = clienteExistente(5, loja, CPF);
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, CPF)).thenReturn(Optional.of(p));

        service.atualizar(7, 5, form("529.982.247-25", "Ana Nova"));

        assertThat(p.getNome()).isEqualTo("Ana Nova");
    }

    @Test
    @DisplayName("quem também é fornecedor não troca o CPF/CNPJ pela edição de cliente")
    void fornecedorTravaDocumento() {
        Parceiro p = clienteExistente(5, loja, CNPJ);
        p.setFornecedor(true);

        assertThatThrownBy(() -> service.atualizar(7, 5, form(CPF, "Ana")))
                .hasMessageContaining("também é fornecedor");
        assertThatThrownBy(() -> service.atualizar(7, 5, form(null, "Ana")))
                .hasMessageContaining("também é fornecedor");
        assertThat(p.getDocumento()).isEqualTo(CNPJ);
    }

    @Test
    @DisplayName("inativar e reativar só mudam a situação")
    void situacao() {
        Parceiro p = clienteExistente(5, loja, CPF);

        service.alterarSituacao(7, 5, false);
        assertThat(p.isAtivo()).isFalse();
        service.alterarSituacao(7, 5, true);
        assertThat(p.isAtivo()).isTrue();
        assertThat(p.getNome()).isEqualTo("Antigo");
    }

    // ---------- filtro ----------

    @Test
    @DisplayName("filtro desconhecido volta ao padrão e o tamanho da página é limitado")
    void filtroNormaliza() {
        ClienteFiltro f = ClienteFiltro.de("  ", "xx", "yy", "zz", -3);
        assertThat(f.busca()).isNull();
        assertThat(f.situacao()).isEqualTo(ClienteFiltro.Situacao.ATIVOS);
        assertThat(f.comOs()).isEqualTo(ClienteFiltro.ComOs.TODOS);
        assertThat(f.ordem()).isEqualTo(ClienteFiltro.Ordem.NOME);
        assertThat(f.pagina()).isEqualTo(1);
        assertThat(f.temFiltros()).isFalse();
        assertThat(new ClienteFiltro("a", null, null, null, 1, 5000).tamanho()).isEqualTo(ClienteFiltro.TAMANHO_MAXIMO);
        assertThat(ClienteFiltro.de("ana", "inativos", "sem", "recentes", 2).temFiltros()).isTrue();
    }
}
