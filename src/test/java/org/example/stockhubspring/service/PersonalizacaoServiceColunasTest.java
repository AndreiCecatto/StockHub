package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.BlocoTela;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.PreferenciaBloco;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.repository.PreferenciaBlocoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Personalização da lista de produtos (AD-20, RN-115 a RN-120), com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalizacaoServiceColunasTest {

    private static final int LOJA = 7;
    private static final TelaPersonalizavel PRODUTOS = TelaPersonalizavel.PRODUTOS;

    @Mock
    private PreferenciaBlocoRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private PersonalizacaoService service;

    @BeforeEach
    void base() {
        when(usuarioRepository.findTipoLojaByCodigo(LOJA)).thenReturn(Optional.of(TipoLoja.COMERCIO));
        when(usuarioRepository.findSubtipoByCodigo(LOJA)).thenReturn(Optional.empty());
        when(repository.findByUsuarioCodigoAndTela(anyInt(), anyString())).thenReturn(List.of());
    }

    private void subtipo(Subtipo s) {
        when(usuarioRepository.findSubtipoByCodigo(LOJA)).thenReturn(Optional.ofNullable(s));
    }

    private static PreferenciaBloco pref(String tela, String bloco, boolean visivel) {
        PreferenciaBloco p = new PreferenciaBloco();
        ReflectionTestUtils.setField(p, "usuarioCodigo", LOJA);
        ReflectionTestUtils.setField(p, "tela", tela);
        ReflectionTestUtils.setField(p, "bloco", bloco);
        ReflectionTestUtils.setField(p, "visivel", visivel);
        return p;
    }

    @Test
    @DisplayName("RN-115: sem linha gravada, toda coluna aplicável da lista aparece visível")
    void padraoTudoVisivel() {
        Map<String, Boolean> mapa = service.blocosVisiveis(LOJA, PRODUTOS);

        assertThat(mapa.keySet()).containsExactly("COL_CATEGORIA", "COL_PRECO", "COL_SITUACAO", "COL_DESCRICAO");
        assertThat(mapa).allSatisfy((k, v) -> assertThat(v).isTrue());
    }

    @Test
    @DisplayName("o padrão do início continua com seus blocos, sem colunas")
    void padraoInicioSemColunas() {
        assertThat(service.blocosVisiveis(LOJA, TelaPersonalizavel.INICIO).keySet())
                .noneMatch(k -> k.startsWith("COL_")).contains("TOTAIS");
    }

    @Test
    @DisplayName("RN-117: VESTUARIO mostra tamanho e cor, sem lote, validade ou série")
    void vestuario() {
        subtipo(Subtipo.VESTUARIO);
        assertThat(service.blocosVisiveis(LOJA, PRODUTOS).keySet())
                .contains("COL_TAMANHO", "COL_COR")
                .doesNotContain("COL_LOTE", "COL_VALIDADE", "COL_NUMERO_SERIE");
    }

    @Test
    @DisplayName("RN-117: FARMACIA mostra lote e validade")
    void farmacia() {
        subtipo(Subtipo.FARMACIA);
        assertThat(service.blocosVisiveis(LOJA, PRODUTOS).keySet())
                .contains("COL_LOTE", "COL_VALIDADE")
                .doesNotContain("COL_TAMANHO", "COL_COR", "COL_NUMERO_SERIE");
    }

    @Test
    @DisplayName("RN-117: MERCADO mostra só validade")
    void mercado() {
        subtipo(Subtipo.MERCADO);
        assertThat(service.blocosVisiveis(LOJA, PRODUTOS).keySet())
                .contains("COL_VALIDADE")
                .doesNotContain("COL_LOTE", "COL_TAMANHO", "COL_COR", "COL_NUMERO_SERIE");
    }

    @Test
    @DisplayName("RN-117: INFORMATICA mostra só número de série")
    void informatica() {
        subtipo(Subtipo.INFORMATICA);
        assertThat(service.blocosVisiveis(LOJA, PRODUTOS).keySet())
                .contains("COL_NUMERO_SERIE")
                .doesNotContain("COL_LOTE", "COL_VALIDADE", "COL_TAMANHO", "COL_COR");
    }

    @Test
    @DisplayName("RN-117: subtipo sem campos (PAPELARIA) e loja sem subtipo ficam só com as 4 colunas fixas")
    void semCampos() {
        subtipo(Subtipo.PAPELARIA);
        assertThat(service.blocosVisiveis(LOJA, PRODUTOS)).hasSize(4);
        subtipo(null);
        assertThat(service.blocosVisiveis(LOJA, PRODUTOS)).hasSize(4);
    }

    @Test
    @DisplayName("linha gravada oculta uma coluna; só essa vira falsa")
    void ocultaColuna() {
        when(repository.findByUsuarioCodigoAndTela(LOJA, "PRODUTOS"))
                .thenReturn(List.of(pref("PRODUTOS", "COL_PRECO", false)));

        Map<String, Boolean> mapa = service.blocosVisiveis(LOJA, PRODUTOS);

        assertThat(mapa.get("COL_PRECO")).isFalse();
        assertThat(mapa.get("COL_CATEGORIA")).isTrue();
    }

    @Test
    @DisplayName("RN-118: linha gravada de coluna que deixou de se aplicar (troca de subtipo) é ignorada ao ler")
    void linhaDeColunaForaDoRamoIgnorada() {
        subtipo(Subtipo.VESTUARIO);
        when(repository.findByUsuarioCodigoAndTela(LOJA, "PRODUTOS"))
                .thenReturn(List.of(pref("PRODUTOS", "COL_LOTE", false), pref("PRODUTOS", "COL_COR", false)));

        Map<String, Boolean> mapa = service.blocosVisiveis(LOJA, PRODUTOS);

        assertThat(mapa).doesNotContainKey("COL_LOTE");
        assertThat(mapa.get("COL_COR")).isFalse();
    }

    @Test
    @DisplayName("blocosAplicaveis da tela PRODUTOS segue o subtipo atual lido do banco")
    void aplicaveisSeguemSubtipo() {
        subtipo(Subtipo.FARMACIA);
        assertThat(service.blocosAplicaveis(LOJA, PRODUTOS))
                .contains(BlocoTela.COL_LOTE, BlocoTela.COL_VALIDADE).doesNotContain(BlocoTela.COL_COR);
        subtipo(Subtipo.VESTUARIO);
        assertThat(service.blocosAplicaveis(LOJA, PRODUTOS))
                .contains(BlocoTela.COL_COR).doesNotContain(BlocoTela.COL_LOTE);
    }

    @Test
    @DisplayName("salvar PRODUTOS grava só linhas de PRODUTOS: marcada visível, o resto oculta")
    void salvarProdutosNaoTocaInicio() {
        service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, Set.of("COL_CATEGORIA", "COL_PRECO"));

        verify(repository).gravar(LOJA, "PRODUTOS", "COL_CATEGORIA", true, 70);
        verify(repository).gravar(LOJA, "PRODUTOS", "COL_PRECO", true, 70);
        verify(repository).gravar(LOJA, "PRODUTOS", "COL_SITUACAO", false, 70);
        verify(repository).gravar(LOJA, "PRODUTOS", "COL_DESCRICAO", false, 70);
        verify(repository, never()).gravar(anyInt(), eq("INICIO"), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("salvar INICIO não grava nenhuma coluna de PRODUTOS")
    void salvarInicioNaoTocaProdutos() {
        service.salvar(LOJA, PapelMembro.GERENTE, 71, TelaPersonalizavel.INICIO, Set.of("TOTAIS"));

        verify(repository).gravar(LOJA, "INICIO", "TOTAIS", true, 71);
        verify(repository, never()).gravar(anyInt(), eq("PRODUTOS"), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("salvar PRODUTOS numa loja de farmácia grava também lote e validade")
    void salvarIncluiColunasDoRamo() {
        subtipo(Subtipo.FARMACIA);

        service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, Set.of("COL_LOTE"));

        verify(repository).gravar(LOJA, "PRODUTOS", "COL_LOTE", true, 70);
        verify(repository).gravar(LOJA, "PRODUTOS", "COL_VALIDADE", false, 70);
    }

    @Test
    @DisplayName("RN-118: COL_LOTE numa loja de vestuário é recusada e nada é gravado")
    void colunaDeOutroRamoRecusada() {
        subtipo(Subtipo.VESTUARIO);

        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, Set.of("COL_PRECO", "COL_LOTE")))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("loja sem subtipo recusa COL_VALIDADE")
    void semSubtipoRecusaValidade() {
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, Set.of("COL_VALIDADE")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("código de outra tela é recusado (início em PRODUTOS e coluna em INICIO)")
    void codigoDeOutraTelaRecusado() {
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, Set.of("TOTAIS")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, 70, TelaPersonalizavel.INICIO, Set.of("COL_PRECO")))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("texto estranho nos blocos é recusado")
    void textoEstranho() {
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, Set.of("<script>alert(1)</script>")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"VENDEDOR", "OPERADOR", "MECANICO"})
    @DisplayName("papel que não é dono nem gerente não salva a lista de produtos")
    void papelSemPermissao(PapelMembro papel) {
        assertThatThrownBy(() -> service.salvar(LOJA, papel, 70, PRODUTOS, Set.of("COL_PRECO")))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("papel nulo e tela nula são recusados")
    void papelOuTelaNulos() {
        assertThatThrownBy(() -> service.salvar(LOJA, null, 70, PRODUTOS, Set.of()))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, 70, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("nada marcado oculta todas as colunas aplicáveis")
    void nadaMarcado() {
        service.salvar(LOJA, PapelMembro.DONO, 70, PRODUTOS, null);

        verify(repository).gravar(LOJA, "PRODUTOS", "COL_PRECO", false, 70);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), eq(true), any());
    }
}
