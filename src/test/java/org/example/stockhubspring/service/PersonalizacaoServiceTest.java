package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.BlocoTela;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.PreferenciaBloco;
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
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Personalização dos blocos do início (AD-19, RN-109 a RN-114), com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalizacaoServiceTest {

    private static final int LOJA = 7;
    private static final int MEMBRO = 107;

    @Mock
    private PreferenciaBlocoRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private PersonalizacaoService service;

    @BeforeEach
    void lojaDeEstoque() {
        when(usuarioRepository.findTipoLojaByCodigo(LOJA)).thenReturn(Optional.of(TipoLoja.ESTOQUE));
        when(repository.findByUsuarioCodigoAndTela(anyInt(), anyString())).thenReturn(List.of());
    }

    private static PreferenciaBloco pref(String bloco, boolean visivel) {
        PreferenciaBloco p = new PreferenciaBloco();
        ReflectionTestUtils.setField(p, "usuarioCodigo", LOJA);
        ReflectionTestUtils.setField(p, "tela", "INICIO");
        ReflectionTestUtils.setField(p, "bloco", bloco);
        ReflectionTestUtils.setField(p, "visivel", visivel);
        return p;
    }

    @Test
    @DisplayName("RN-109: sem nenhuma linha gravada, todo bloco aplicável aparece visível")
    void padraoTudoVisivel() {
        Map<String, Boolean> mapa = service.blocosVisiveis(LOJA, TelaPersonalizavel.INICIO);

        assertThat(mapa).isNotEmpty().allSatisfy((k, v) -> assertThat(v).isTrue());
        assertThat(mapa).doesNotContainKey("OS_FLUXO");
        assertThat(mapa.keySet()).containsExactlyElementsOf(
                BlocoTela.daTela(TelaPersonalizavel.INICIO, TipoLoja.ESTOQUE).stream().map(Enum::name).toList());
    }

    @Test
    @DisplayName("linha gravada como oculta esconde só aquele bloco")
    void linhaOcultaEsconde() {
        when(repository.findByUsuarioCodigoAndTela(LOJA, "INICIO")).thenReturn(List.of(pref("TOTAIS", false)));

        Map<String, Boolean> mapa = service.blocosVisiveis(LOJA, TelaPersonalizavel.INICIO);

        assertThat(mapa.get("TOTAIS")).isFalse();
        assertThat(mapa.get("ATALHOS")).isTrue();
    }

    @Test
    @DisplayName("linha de bloco desconhecido no banco é ignorada, sem erro e sem entrar no mapa")
    void linhaDesconhecidaIgnorada() {
        when(repository.findByUsuarioCodigoAndTela(LOJA, "INICIO"))
                .thenReturn(List.of(pref("BLOCO_QUE_NAO_EXISTE", false), pref("TOTAIS", true)));

        Map<String, Boolean> mapa = service.blocosVisiveis(LOJA, TelaPersonalizavel.INICIO);

        assertThat(mapa).doesNotContainKey("BLOCO_QUE_NAO_EXISTE");
        assertThat(mapa.get("TOTAIS")).isTrue();
    }

    @Test
    @DisplayName("linha de OS_FLUXO em loja sem módulo OS é ignorada")
    void linhaDeOsEmLojaSemModulo() {
        when(repository.findByUsuarioCodigoAndTela(LOJA, "INICIO")).thenReturn(List.of(pref("OS_FLUXO", false)));

        assertThat(service.blocosVisiveis(LOJA, TelaPersonalizavel.INICIO)).doesNotContainKey("OS_FLUXO");
    }

    @Test
    @DisplayName("loja de oficina enxerga OS_FLUXO e respeita a linha oculta")
    void oficinaTemOsFluxo() {
        when(usuarioRepository.findTipoLojaByCodigo(LOJA)).thenReturn(Optional.of(TipoLoja.OFICINA));
        when(repository.findByUsuarioCodigoAndTela(LOJA, "INICIO")).thenReturn(List.of(pref("OS_FLUXO", false)));

        assertThat(service.blocosVisiveis(LOJA, TelaPersonalizavel.INICIO)).containsEntry("OS_FLUXO", false);
        assertThat(service.blocosAplicaveis(LOJA, TelaPersonalizavel.INICIO)).contains(BlocoTela.OS_FLUXO);
    }

    @Test
    @DisplayName("loja não encontrada cai no tipo ESTOQUE (sem bloco de OS)")
    void lojaDesconhecidaViraEstoque() {
        when(usuarioRepository.findTipoLojaByCodigo(99)).thenReturn(Optional.empty());

        assertThat(service.blocosAplicaveis(99, TelaPersonalizavel.INICIO)).doesNotContain(BlocoTela.OS_FLUXO);
    }

    @Test
    @DisplayName("salvar grava uma linha por bloco aplicável: marcado visível, o resto oculto")
    void salvaMarcadosEOcultaOResto() {
        service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.INICIO, Set.of("ATALHOS", "TOTAIS"));

        for (BlocoTela b : BlocoTela.daTela(TelaPersonalizavel.INICIO, TipoLoja.ESTOQUE)) {
            boolean esperado = b == BlocoTela.ATALHOS || b == BlocoTela.TOTAIS;
            verify(repository).gravar(LOJA, "INICIO", b.name(), esperado, MEMBRO);
        }
        verify(repository, never()).gravar(anyInt(), anyString(), eq("OS_FLUXO"), anyBoolean(), any());
    }

    @Test
    @DisplayName("RN-113: salvar nunca apaga (nenhum delete no repositório)")
    void salvarNaoFazDelete() {
        service.salvar(LOJA, PapelMembro.GERENTE, MEMBRO, TelaPersonalizavel.INICIO, Set.of());

        verify(repository, never()).delete(any());
        verify(repository, never()).deleteAll();
        verify(repository, never()).deleteAll(any());
        verify(repository, never()).deleteById(any());
        verify(repository, never()).deleteAllInBatch();
        verify(repository, never()).deleteAllById(any());
        verify(repository, times(BlocoTela.daTela(TelaPersonalizavel.INICIO, TipoLoja.ESTOQUE).size()))
                .gravar(anyInt(), anyString(), anyString(), eq(false), any());
    }

    @Test
    @DisplayName("conjunto nulo equivale a nada marcado: tudo oculto")
    void conjuntoNuloOcultaTudo() {
        service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.INICIO, null);

        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), eq(true), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOO", "", "  ", "atalhos", "<script>alert(1)</script>", "ATALHOS; DROP TABLE usuario"})
    @DisplayName("código de bloco inválido é recusado e nada é gravado")
    void codigoInvalidoRecusado(String codigo) {
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.INICIO,
                Set.of("ATALHOS", codigo)))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("OS_FLUXO enviado por loja sem módulo OS é recusado")
    void osFluxoRecusadoSemModulo() {
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.INICIO,
                Set.of("OS_FLUXO")))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("OS_FLUXO ausente da lista de gravação em loja sem módulo OS")
    void osFluxoNaoGravadoSemModulo() {
        service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.INICIO, Set.of("ATALHOS"));

        verify(repository, never()).gravar(anyInt(), anyString(), eq("OS_FLUXO"), anyBoolean(), any());
    }

    @Test
    @DisplayName("loja de oficina pode marcar e ocultar OS_FLUXO")
    void oficinaGravaOsFluxo() {
        when(usuarioRepository.findTipoLojaByCodigo(LOJA)).thenReturn(Optional.of(TipoLoja.OFICINA));

        service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.INICIO, Set.of("OS_FLUXO"));

        verify(repository).gravar(LOJA, "INICIO", "OS_FLUXO", true, MEMBRO);
        verify(repository).gravar(LOJA, "INICIO", "ATALHOS", false, MEMBRO);
    }

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"VENDEDOR", "OPERADOR", "MECANICO"})
    @DisplayName("RN-110: papéis operacionais não personalizam")
    void papeisOperacionaisRecusados(PapelMembro papel) {
        assertThatThrownBy(() -> service.salvar(LOJA, papel, MEMBRO, TelaPersonalizavel.INICIO, Set.of("ATALHOS")))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("papel nulo e tela nula são recusados")
    void papelOuTelaNulos() {
        assertThatThrownBy(() -> service.salvar(LOJA, null, MEMBRO, TelaPersonalizavel.INICIO, Set.of()))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, MEMBRO, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("tela PRODUTOS ainda não tem blocos: salvar vazio não grava e marcar bloco do início é recusado")
    void telaSemBlocos() {
        service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.PRODUTOS, Set.of());
        verify(repository, never()).gravar(anyInt(), anyString(), anyString(), anyBoolean(), any());

        assertThatThrownBy(() -> service.salvar(LOJA, PapelMembro.DONO, MEMBRO, TelaPersonalizavel.PRODUTOS,
                Set.of("ATALHOS")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("BlocoTela.de: nulo e desconhecido viram null; espaços são aparados")
    void blocoDe() {
        assertThat(BlocoTela.de(null)).isNull();
        assertThat(BlocoTela.de("XYZ")).isNull();
        assertThat(BlocoTela.de(" TOTAIS ")).isEqualTo(BlocoTela.TOTAIS);
        assertThat(BlocoTela.OS_FLUXO.aplicavel(null)).isFalse();
        assertThat(BlocoTela.ATALHOS.aplicavel(null)).isTrue();
        assertThat(BlocoTela.OS_FLUXO.aplicavel(TipoLoja.MISTO)).isTrue();
        assertThat(BlocoTela.OS_FLUXO.aplicavel(TipoLoja.COMERCIO)).isFalse();
    }
}
