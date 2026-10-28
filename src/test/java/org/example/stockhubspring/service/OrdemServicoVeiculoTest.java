package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.EquipamentoCliente;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.EquipamentoClienteRepository;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.example.stockhubspring.repository.OrdemServicoRepository;
import org.example.stockhubspring.repository.OsPecaRepository;
import org.example.stockhubspring.repository.OsServicoRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Placa e modelo do veículo na criação da OS (AD-18, RN-104 a RN-108), com tudo simulado. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdemServicoVeiculoTest {

    private static final String MSG_PLACA = "Placa inválida. Use o formato ABC-1234 ou ABC1D23.";

    @Mock
    private OrdemServicoRepository osRepository;
    @Mock
    private OsPecaRepository pecaRepository;
    @Mock
    private OsServicoRepository servicoRepository;
    @Mock
    private EquipamentoClienteRepository equipamentoRepository;
    @Mock
    private ParceiroRepository parceiroRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MembroLojaRepository membroRepository;
    @Mock
    private EstoqueService estoque;
    @Mock
    private NumeradorOs numerador;
    @Mock
    private UsuarioRepository usuarioRepository;

    private OrdemServicoService service;
    private Usuario loja;

    @BeforeEach
    void preparar() {
        service = new OrdemServicoService(osRepository, pecaRepository, servicoRepository, equipamentoRepository,
                parceiroRepository, produtoRepository, membroRepository, estoque, numerador, usuarioRepository);
        loja = Fabrica.usuario(7);
        loja.setPapel(PapelMembro.DONO);
        cliente(4);
        subtipo(Subtipo.MECANICA);
        when(osRepository.saveAndFlush(any(OrdemServico.class))).thenAnswer(i -> i.getArgument(0));
        when(equipamentoRepository.saveAndFlush(any(EquipamentoCliente.class))).thenAnswer(i -> i.getArgument(0));
        when(equipamentoRepository.findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(equipamentoRepository.findByUsuarioCodigoAndParceiroIdOrderByIdAsc(any(), any())).thenReturn(List.of());
        when(numerador.proximo(anyInt())).thenReturn(1);
    }

    private Parceiro cliente(int id) {
        Parceiro p = new Parceiro();
        ReflectionTestUtils.setField(p, "id", id);
        p.setUsuario(loja);
        p.setCliente(true);
        when(parceiroRepository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    private void subtipo(Subtipo s) {
        when(usuarioRepository.findSubtipoByCodigo(7)).thenReturn(Optional.ofNullable(s));
    }

    private OrdemServico criar(String equipamento, String placa, String modelo) {
        return service.criar(loja, 4, equipamento, null, placa, modelo, "Não liga", null, 0, null);
    }

    private EquipamentoCliente veiculoGravado() {
        ArgumentCaptor<EquipamentoCliente> c = ArgumentCaptor.forClass(EquipamentoCliente.class);
        verify(equipamentoRepository).saveAndFlush(c.capture());
        return c.getValue();
    }

    // ---------- camposPermitidos ----------

    @Test
    @DisplayName("camposPermitidos usa o subtipo do banco, não o da sessão")
    void camposDoBanco() {
        loja.setSubtipo(Subtipo.FARMACIA);
        subtipo(Subtipo.MOTO);
        assertThat(service.camposPermitidos(loja).temVeiculo()).isTrue();
    }

    @Test
    @DisplayName("camposPermitidos sem subtipo no banco ignora a sessão e não libera campos")
    void camposSemSubtipoNoBanco() {
        when(usuarioRepository.findSubtipoByCodigo(7)).thenReturn(Optional.empty());
        loja.setSubtipo(Subtipo.MECANICA);
        assertThat(service.camposPermitidos(loja)).isEqualTo(CamposRamo.Permitidos.NENHUM);
        loja.setSubtipo(null);
        assertThat(service.camposPermitidos(loja)).isEqualTo(CamposRamo.Permitidos.NENHUM);
    }

    // ---------- normalizarPlaca ----------

    @ParameterizedTest
    @ValueSource(strings = {"ABC-1234", "abc1234", "ABC1D23", " abc-1d23 ", "abc 1d23"})
    @DisplayName("placas válidas nos dois formatos são aceitas")
    void placasValidas(String entrada) {
        String r = OrdemServicoService.normalizarPlaca(entrada);
        assertThat(r).matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}");
    }

    @Test
    @DisplayName("normaliza espaços, hífen e caixa")
    void normaliza() {
        assertThat(OrdemServicoService.normalizarPlaca(" abc-1d23 ")).isEqualTo("ABC1D23");
        assertThat(OrdemServicoService.normalizarPlaca("abc-1234")).isEqualTo("ABC1234");
    }

    @Test
    @DisplayName("placa vazia, em branco ou nula vira null")
    void placaVazia() {
        assertThat(OrdemServicoService.normalizarPlaca(null)).isNull();
        assertThat(OrdemServicoService.normalizarPlaca("")).isNull();
        assertThat(OrdemServicoService.normalizarPlaca("   ")).isNull();
        assertThat(OrdemServicoService.normalizarPlaca(" - ")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB-1234", "ABCD123", "ABC12", "123-ABCD", "ABC-12345", "A1C1234", "ABC1D2", "ÁBC-1234",
            "ABC_1234", "<script>", "ABC1234; drop"})
    @DisplayName("placas inválidas são recusadas com a mensagem da regra")
    void placasInvalidas(String entrada) {
        assertThatThrownBy(() -> OrdemServicoService.normalizarPlaca(entrada))
                .isInstanceOf(RegraNegocioException.class).hasMessage(MSG_PLACA);
    }

    // ---------- validarModelo ----------

    @Test
    @DisplayName("modelo: vazio vira null, 80 caracteres passa, 81 é recusado")
    void modeloTamanho() {
        assertThat(OrdemServicoService.validarModelo(null)).isNull();
        assertThat(OrdemServicoService.validarModelo("   ")).isNull();
        assertThat(OrdemServicoService.validarModelo("  Uno 1.0  ")).isEqualTo("Uno 1.0");
        assertThat(OrdemServicoService.validarModelo("a".repeat(80))).hasSize(80);
        assertThatThrownBy(() -> OrdemServicoService.validarModelo("a".repeat(81)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O modelo deve ter no máximo 80 caracteres.");
    }

    @Test
    @DisplayName("modelo com caractere de controle é recusado")
    void modeloControle() {
        assertThatThrownBy(() -> OrdemServicoService.validarModelo("Un\u0007o"))
                .isInstanceOf(RegraNegocioException.class).hasMessage("O modelo tem caracteres inválidos.");
        assertThatThrownBy(() -> OrdemServicoService.validarModelo("Uno\u0000X"))
                .isInstanceOf(RegraNegocioException.class).hasMessage("O modelo tem caracteres inválidos.");
    }

    // ---------- criar ----------

    @Test
    @DisplayName("MECANICA: placa válida é normalizada e gravada junto do modelo")
    void gravaVeiculo() {
        OrdemServico os = criar("Fiat Uno", " abc-1d23 ", "Uno Mille");
        EquipamentoCliente e = veiculoGravado();
        assertThat(e.getPlaca()).isEqualTo("ABC1D23");
        assertThat(e.getModelo()).isEqualTo("Uno Mille");
        assertThat(e.getDescricao()).isEqualTo("Fiat Uno");
        assertThat(os.getEquipamento()).isSameAs(e);
    }

    @Test
    @DisplayName("MOTO também grava placa e modelo")
    void motoGrava() {
        subtipo(Subtipo.MOTO);
        criar("CG 160", "abc1234", "Honda CG");
        EquipamentoCliente e = veiculoGravado();
        assertThat(e.getPlaca()).isEqualTo("ABC1234");
        assertThat(e.getModelo()).isEqualTo("Honda CG");
    }

    @Test
    @DisplayName("placa inválida recusa a criação sem numerar nem gravar nada")
    void placaInvalidaRecusa() {
        assertThatThrownBy(() -> criar("Uno", "XX-99", null))
                .isInstanceOf(RegraNegocioException.class).hasMessage(MSG_PLACA);
        verify(numerador, never()).proximo(anyInt());
        verify(equipamentoRepository, never()).saveAndFlush(any());
        verify(osRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("modelo de 81 caracteres recusa a criação")
    void modeloGrandeRecusa() {
        assertThatThrownBy(() -> criar("Uno", null, "m".repeat(81)))
                .isInstanceOf(RegraNegocioException.class).hasMessage("O modelo deve ter no máximo 80 caracteres.");
        verify(osRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("fora do ramo, placa e modelo são ignorados, até a placa inválida, sem erro")
    void ignoradosForaDoRamo() {
        subtipo(Subtipo.ELETRICA_ELETRONICA);
        OrdemServico os = criar("Notebook", "placa-lixo", "m".repeat(200));
        EquipamentoCliente e = veiculoGravado();
        assertThat(e.getPlaca()).isNull();
        assertThat(e.getModelo()).isNull();
        assertThat(os.getEquipamento().getDescricao()).isEqualTo("Notebook");
        verify(equipamentoRepository, never()).findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(any(), any(), any());
    }

    @Test
    @DisplayName("fora do ramo, só modelo sem descrição continua exigindo o equipamento")
    void modeloForaDoRamoNaoSubstituiDescricao() {
        subtipo(Subtipo.ASSISTENCIA_TECNICA);
        assertThatThrownBy(() -> criar(null, null, "Galaxy"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("equipamento");
    }

    @Test
    @DisplayName("MECANICA: só modelo, sem descrição, é aceito e a descrição passa a ser o modelo")
    void soModelo() {
        criar("  ", null, "Gol G5");
        EquipamentoCliente e = veiculoGravado();
        assertThat(e.getDescricao()).isEqualTo("Gol G5");
        assertThat(e.getModelo()).isEqualTo("Gol G5");
        assertThat(e.getPlaca()).isNull();
    }

    @Test
    @DisplayName("MECANICA sem descrição nem modelo continua recusada")
    void semNada() {
        assertThatThrownBy(() -> criar(null, "ABC-1234", null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("equipamento");
    }

    @Test
    @DisplayName("placa repetida no mesmo cliente reaproveita o veículo sem sobrescrever o modelo")
    void placaRepetidaReaproveita() {
        EquipamentoCliente existente = new EquipamentoCliente();
        existente.setDescricao("Fiat Uno");
        existente.setPlaca("ABC1D23");
        existente.setModelo("Uno Mille");
        when(equipamentoRepository.findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(7, 4, "ABC1D23"))
                .thenReturn(Optional.of(existente));

        OrdemServico os = criar("Outro nome", "abc-1d23", "Modelo novo");

        assertThat(os.getEquipamento()).isSameAs(existente);
        assertThat(existente.getModelo()).isEqualTo("Uno Mille");
        verify(equipamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("placa repetida com modelo vazio no cadastro preenche o modelo")
    void placaRepetidaPreencheModelo() {
        EquipamentoCliente existente = new EquipamentoCliente();
        existente.setDescricao("Fiat Uno");
        existente.setPlaca("ABC1D23");
        when(equipamentoRepository.findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(7, 4, "ABC1D23"))
                .thenReturn(Optional.of(existente));

        OrdemServico os = criar("Fiat Uno", "ABC1D23", "Uno Way");

        assertThat(os.getEquipamento()).isSameAs(existente);
        assertThat(existente.getModelo()).isEqualTo("Uno Way");
        verify(equipamentoRepository).saveAndFlush(existente);
    }

    @Test
    @DisplayName("a busca por placa é por loja e cliente, então clientes diferentes têm veículos separados")
    void clientesDiferentes() {
        Parceiro outro = cliente(5);
        EquipamentoCliente doPrimeiro = new EquipamentoCliente();
        doPrimeiro.setPlaca("ABC1D23");
        when(equipamentoRepository.findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(7, 4, "ABC1D23"))
                .thenReturn(Optional.of(doPrimeiro));

        OrdemServico os = service.criar(loja, 5, "Uno", null, "ABC1D23", null, "Não liga", null, 0, null);

        verify(equipamentoRepository).findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(eq(7), eq(5), eq("ABC1D23"));
        assertThat(os.getEquipamento()).isNotSameAs(doPrimeiro);
        assertThat(os.getEquipamento().getPlaca()).isEqualTo("ABC1D23");
        assertThat(os.getEquipamento().getParceiro()).isSameAs(outro);
    }

    @Test
    @DisplayName("OS sem placa funciona como antes: reaproveita por descrição e identificador")
    void semPlacaComoAntes() {
        EquipamentoCliente existente = new EquipamentoCliente();
        existente.setDescricao("fiat uno");
        when(equipamentoRepository.findByUsuarioCodigoAndParceiroIdOrderByIdAsc(7, 4)).thenReturn(List.of(existente));

        OrdemServico os = criar("Fiat Uno", null, null);

        assertThat(os.getEquipamento()).isSameAs(existente);
        verify(equipamentoRepository, never()).saveAndFlush(any());
        verify(equipamentoRepository, never()).findFirstByUsuarioCodigoAndParceiroIdAndPlacaOrderByIdAsc(any(), any(), any());
    }

    @Test
    @DisplayName("loja com veículo, sem placa: não reaproveita equipamento que já tem placa")
    void semPlacaNaoReaproveitaVeiculoComPlaca() {
        EquipamentoCliente comPlaca = new EquipamentoCliente();
        comPlaca.setDescricao("Fiat Uno");
        comPlaca.setPlaca("ABC1D23");
        when(equipamentoRepository.findByUsuarioCodigoAndParceiroIdOrderByIdAsc(7, 4)).thenReturn(List.of(comPlaca));

        OrdemServico os = criar("Fiat Uno", null, null);

        assertThat(os.getEquipamento()).isNotSameAs(comPlaca);
        assertThat(os.getEquipamento().getPlaca()).isNull();
        verify(equipamentoRepository).saveAndFlush(os.getEquipamento());
    }

    @Test
    @DisplayName("loja com veículo, sem placa: reaproveita o sem placa e preenche o modelo vazio")
    void semPlacaPreencheModelo() {
        EquipamentoCliente semPlaca = new EquipamentoCliente();
        semPlaca.setDescricao("Fiat Uno");
        when(equipamentoRepository.findByUsuarioCodigoAndParceiroIdOrderByIdAsc(7, 4)).thenReturn(List.of(semPlaca));

        OrdemServico os = criar("Fiat Uno", null, "Uno Way");

        assertThat(os.getEquipamento()).isSameAs(semPlaca);
        assertThat(semPlaca.getModelo()).isEqualTo("Uno Way");
        verify(equipamentoRepository).saveAndFlush(semPlaca);
    }

    @Test
    @DisplayName("placa e modelo vazios criam o equipamento com os dois nulos")
    void semPlacaNovo() {
        criar("Fiat Uno", "", "");
        EquipamentoCliente e = veiculoGravado();
        assertThat(e.getPlaca()).isNull();
        assertThat(e.getModelo()).isNull();
    }

    @Test
    @DisplayName("a sobrecarga antiga (sem placa e modelo) continua valendo")
    void sobrecargaAntiga() {
        OrdemServico os = service.criar(loja, 4, "Fiat Uno", "SN1", "Não liga", null, 0, null);
        assertThat(os.getEquipamento().getIdentificador()).isEqualTo("SN1");
        assertThat(os.getEquipamento().getPlaca()).isNull();
    }
}
