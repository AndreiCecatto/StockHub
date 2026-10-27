package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.ProdutoBusca;
import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/** Campos de produto por ramo (AD-17, RN-99 a RN-103): CamposRamo, ProdutoService e ProdutoBusca. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProdutoCamposRamoTest {

    private static final int LOJA = 7;

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Mock
    private EstoqueService estoqueService;

    private ProdutoService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(produtoRepository, categoriaRepository, movimentacaoRepository, estoqueService);
        usuario = Fabrica.usuario(LOJA);
        Categoria categoria = Fabrica.categoria(3, "Geral", usuario);
        when(categoriaRepository.findByIdAndUsuarioCodigo(3, LOJA)).thenReturn(Optional.of(categoria));
        when(produtoRepository.saveAndFlush(any(Produto.class))).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(55);
            }
            return p;
        });
        when(produtoRepository.contarNomeAtivo(anyInt(), any(), anyInt())).thenReturn(0L);
    }

    /** Formulário com TODOS os campos por ramo preenchidos. */
    private ProdutoForm formCompleto() {
        ProdutoForm form = new ProdutoForm();
        form.setNome("Item");
        form.setPreco(new BigDecimal("10.00"));
        form.setQuantidade(0);
        form.setEstoqueMinimo(1);
        form.setCategoriaId(3);
        form.setTamanho("M");
        form.setCor("Azul");
        form.setLote("L-001");
        form.setDataValidade(LocalDate.of(2030, 5, 20));
        form.setNumeroSerie("SN-123");
        return form;
    }

    private Produto existente() {
        Produto p = Fabrica.produto(9, "Item", 5, 1, usuario);
        p.setCategoria(Fabrica.categoria(3, "Geral", usuario));
        when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(9, LOJA)).thenReturn(Optional.of(p));
        return p;
    }

    // ---------- CamposRamo ----------

    @Test
    @DisplayName("mapa: cada subtipo libera só os seus campos; sem subtipo ou outros, nenhum")
    void mapaDeCampos() {
        assertThat(CamposRamo.permitidos(Subtipo.VESTUARIO))
                .isEqualTo(new CamposRamo.Permitidos(true, true, false, false, false));
        assertThat(CamposRamo.permitidos(Subtipo.FARMACIA))
                .isEqualTo(new CamposRamo.Permitidos(false, false, true, true, false));
        assertThat(CamposRamo.permitidos(Subtipo.MERCADO))
                .isEqualTo(new CamposRamo.Permitidos(false, false, false, true, false));
        assertThat(CamposRamo.permitidos(Subtipo.INFORMATICA))
                .isEqualTo(new CamposRamo.Permitidos(false, false, false, false, true));
        assertThat(CamposRamo.permitidos(Subtipo.PAPELARIA)).isEqualTo(CamposRamo.Permitidos.NENHUM);
        assertThat(CamposRamo.permitidos(Subtipo.MECANICA).temAlgum()).isFalse();
        assertThat(CamposRamo.permitidos(null)).isEqualTo(CamposRamo.Permitidos.NENHUM);
        assertThat(CamposRamo.campos(null)).isEmpty();
    }

    @Test
    @DisplayName("camposPermitidos(usuario) segue o subtipo da loja")
    void camposPermitidosDoUsuario() {
        usuario.setSubtipo(Subtipo.FARMACIA);
        assertThat(service.camposPermitidos(usuario).lote()).isTrue();
        assertThat(service.camposPermitidos(usuario).tamanho()).isFalse();
    }

    // ---------- ProdutoService.salvar ----------

    @Test
    @DisplayName("campos de outro ramo são ignorados: vestuário grava tamanho e cor, não lote/validade/série")
    void ignoraCampoDeOutroRamo() {
        usuario.setSubtipo(Subtipo.VESTUARIO);

        Produto salvo = service.salvar(formCompleto(), usuario);

        assertThat(salvo.getTamanho()).isEqualTo("M");
        assertThat(salvo.getCor()).isEqualTo("Azul");
        assertThat(salvo.getLote()).isNull();
        assertThat(salvo.getDataValidade()).isNull();
        assertThat(salvo.getNumeroSerie()).isNull();
    }

    @Test
    @DisplayName("farmácia grava lote e validade; mercado só validade; informática só número de série")
    void gravaOsCamposDoRamo() {
        usuario.setSubtipo(Subtipo.FARMACIA);
        Produto farmacia = service.salvar(formCompleto(), usuario);
        assertThat(farmacia.getLote()).isEqualTo("L-001");
        assertThat(farmacia.getDataValidade()).isEqualTo(LocalDate.of(2030, 5, 20));
        assertThat(farmacia.getTamanho()).isNull();

        usuario.setSubtipo(Subtipo.MERCADO);
        Produto mercado = service.salvar(formCompleto(), usuario);
        assertThat(mercado.getDataValidade()).isEqualTo(LocalDate.of(2030, 5, 20));
        assertThat(mercado.getLote()).isNull();

        usuario.setSubtipo(Subtipo.INFORMATICA);
        Produto info = service.salvar(formCompleto(), usuario);
        assertThat(info.getNumeroSerie()).isEqualTo("SN-123");
        assertThat(info.getDataValidade()).isNull();
    }

    @Test
    @DisplayName("loja sem subtipo (ou ramo sem campos) fica inalterada: nenhum campo extra é gravado")
    void lojaSemSubtipoInalterada() {
        Produto semSubtipo = service.salvar(formCompleto(), usuario);
        assertThat(semSubtipo.getTamanho()).isNull();
        assertThat(semSubtipo.getCor()).isNull();
        assertThat(semSubtipo.getLote()).isNull();
        assertThat(semSubtipo.getDataValidade()).isNull();
        assertThat(semSubtipo.getNumeroSerie()).isNull();

        usuario.setSubtipo(Subtipo.PAPELARIA);
        Produto papelaria = service.salvar(formCompleto(), usuario);
        assertThat(papelaria.getCor()).isNull();
        assertThat(papelaria.getNumeroSerie()).isNull();
    }

    @Test
    @DisplayName("texto vazio ou só espaços vira null")
    void textoVazioViraNull() {
        usuario.setSubtipo(Subtipo.VESTUARIO);
        ProdutoForm form = formCompleto();
        form.setTamanho("   ");
        form.setCor("");

        Produto salvo = service.salvar(form, usuario);

        assertThat(salvo.getTamanho()).isNull();
        assertThat(salvo.getCor()).isNull();
    }

    @Test
    @DisplayName("limites de tamanho: no máximo vale, um caractere a mais dá RegraNegocioException")
    void limitesDeTamanho() {
        usuario.setSubtipo(Subtipo.VESTUARIO);
        ProdutoForm noLimite = formCompleto();
        noLimite.setTamanho("T".repeat(CamposRamo.TAMANHO_MAX));
        noLimite.setCor("C".repeat(CamposRamo.COR_MAX));
        assertThat(service.salvar(noLimite, usuario).getCor()).hasSize(40);

        ProdutoForm tamanhoGrande = formCompleto();
        tamanhoGrande.setTamanho("T".repeat(CamposRamo.TAMANHO_MAX + 1));
        assertThatThrownBy(() -> service.salvar(tamanhoGrande, usuario))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("20");

        ProdutoForm corGrande = formCompleto();
        corGrande.setCor("C".repeat(CamposRamo.COR_MAX + 1));
        assertThatThrownBy(() -> service.salvar(corGrande, usuario))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("40");

        usuario.setSubtipo(Subtipo.FARMACIA);
        ProdutoForm loteGrande = formCompleto();
        loteGrande.setLote("L".repeat(CamposRamo.LOTE_MAX + 1));
        assertThatThrownBy(() -> service.salvar(loteGrande, usuario))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("40");

        usuario.setSubtipo(Subtipo.INFORMATICA);
        ProdutoForm serieGrande = formCompleto();
        serieGrande.setNumeroSerie("S".repeat(CamposRamo.NUMERO_SERIE_MAX + 1));
        assertThatThrownBy(() -> service.salvar(serieGrande, usuario))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("60");
    }

    @Test
    @DisplayName("texto acima do máximo e validade fora da faixa em campo de OUTRO ramo são ignorados, sem erro")
    void valoresInvalidosDeOutroRamoNaoValidam() {
        usuario.setSubtipo(Subtipo.VESTUARIO);
        ProdutoForm form = formCompleto();
        form.setNumeroSerie("S".repeat(500));
        form.setLote("L".repeat(500));
        form.setDataValidade(LocalDate.of(1900, 1, 1));

        Produto salvo = service.salvar(form, usuario);

        assertThat(salvo.getNumeroSerie()).isNull();
        assertThat(salvo.getDataValidade()).isNull();
    }

    @Test
    @DisplayName("validade: bordas 2000-01-01 e 2100-12-31 valem; fora da faixa dá erro; vazia é aceita")
    void faixaDaValidade() {
        usuario.setSubtipo(Subtipo.MERCADO);

        for (LocalDate ok : new LocalDate[]{LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31)}) {
            ProdutoForm form = formCompleto();
            form.setDataValidade(ok);
            assertThat(service.salvar(form, usuario).getDataValidade()).isEqualTo(ok);
        }
        for (LocalDate ruim : new LocalDate[]{LocalDate.of(1999, 12, 31), LocalDate.of(2101, 1, 1)}) {
            ProdutoForm form = formCompleto();
            form.setDataValidade(ruim);
            assertThatThrownBy(() -> service.salvar(form, usuario))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("validade");
        }
        ProdutoForm semData = formCompleto();
        semData.setDataValidade(null);
        assertThat(service.salvar(semData, usuario).getDataValidade()).isNull();
    }

    @Test
    @DisplayName("trocar o subtipo preserva os valores gravados dos campos que deixaram de valer")
    void trocarSubtipoPreservaValores() {
        Produto produto = existente();
        produto.setTamanho("G");
        produto.setCor("Verde");
        produto.setLote("L-OLD");
        ProdutoForm form = formCompleto();
        form.setId(9);
        form.setTamanho("PP"); // a loja não usa mais tamanho depois da troca: não pode ser tocado

        // A loja era vestuário e virou farmácia: tamanho/cor deixam de valer
        usuario.setSubtipo(Subtipo.FARMACIA);
        Produto salvo = service.salvar(form, usuario);

        assertThat(salvo.getTamanho()).isEqualTo("G");
        assertThat(salvo.getCor()).isEqualTo("Verde");
        assertThat(salvo.getLote()).isEqualTo("L-001"); // este vale e foi atualizado
        assertThat(salvo.getDataValidade()).isEqualTo(LocalDate.of(2030, 5, 20));
    }

    @Test
    @DisplayName("edição sem subtipo não apaga nada que já estava gravado")
    void edicaoSemSubtipoNaoApaga() {
        Produto produto = existente();
        produto.setNumeroSerie("SN-9");
        produto.setDataValidade(LocalDate.of(2031, 1, 1));
        ProdutoForm form = formCompleto();
        form.setId(9);
        form.setNumeroSerie(null);
        form.setDataValidade(null);

        Produto salvo = service.salvar(form, usuario);

        assertThat(salvo.getNumeroSerie()).isEqualTo("SN-9");
        assertThat(salvo.getDataValidade()).isEqualTo(LocalDate.of(2031, 1, 1));
    }

    @Test
    @DisplayName("campo permitido enviado vazio na edição limpa o valor (vira null)")
    void campoPermitidoVazioLimpa() {
        usuario.setSubtipo(Subtipo.INFORMATICA);
        Produto produto = existente();
        produto.setNumeroSerie("SN-9");
        ProdutoForm form = formCompleto();
        form.setId(9);
        form.setNumeroSerie("");

        assertThat(service.salvar(form, usuario).getNumeroSerie()).isNull();
    }

    @Test
    @DisplayName("produto antigo com todos os campos null segue funcionando na edição")
    void produtoAntigoComNulls() {
        usuario.setSubtipo(Subtipo.VESTUARIO);
        Produto produto = existente();
        ProdutoForm form = ProdutoForm.de(produto);
        form.setCategoriaId(3);

        Produto salvo = service.salvar(form, usuario);

        assertThat(salvo.getTamanho()).isNull();
        assertThat(salvo.getCor()).isNull();
    }

    @Test
    @DisplayName("ProdutoForm.de copia os campos por ramo do produto")
    void formCopiaCampos() {
        Produto produto = existente();
        produto.setTamanho("M");
        produto.setLote("L1");
        produto.setDataValidade(LocalDate.of(2030, 1, 1));

        ProdutoForm form = ProdutoForm.de(produto);

        assertThat(form.getTamanho()).isEqualTo("M");
        assertThat(form.getLote()).isEqualTo("L1");
        assertThat(form.getDataValidade()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(form.getCor()).isNull();
    }

    // ---------- ProdutoBusca ----------

    private Produto produtoCompleto() {
        Produto p = Fabrica.produto(4, "Camisa", 3, 1, usuario);
        p.setCategoria(Fabrica.categoria(3, "Roupas", usuario));
        p.setTamanho("M");
        p.setCor("Azul");
        p.setLote("L-1");
        p.setDataValidade(LocalDate.of(2030, 1, 1));
        p.setNumeroSerie("SN-1");
        return p;
    }

    @Test
    @DisplayName("ProdutoBusca só preenche o que o ramo permite")
    void buscaSoPreencheOPermitido() {
        ProdutoBusca vest = ProdutoBusca.de(produtoCompleto(), CamposRamo.permitidos(Subtipo.VESTUARIO));
        assertThat(vest.tamanho()).isEqualTo("M");
        assertThat(vest.cor()).isEqualTo("Azul");
        assertThat(vest.lote()).isNull();
        assertThat(vest.validade()).isNull();
        assertThat(vest.numeroSerie()).isNull();
        assertThat(vest.aproximado()).isFalse();

        ProdutoBusca farm = ProdutoBusca.de(produtoCompleto(), CamposRamo.permitidos(Subtipo.FARMACIA));
        assertThat(farm.lote()).isEqualTo("L-1");
        assertThat(farm.validade()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(farm.tamanho()).isNull();

        ProdutoBusca info = ProdutoBusca.de(produtoCompleto(), CamposRamo.permitidos(Subtipo.INFORMATICA));
        assertThat(info.numeroSerie()).isEqualTo("SN-1");
        assertThat(info.cor()).isNull();
    }

    @Test
    @DisplayName("ProdutoBusca sem permitidos (sobrecarga simples) não expõe nenhum campo extra")
    void buscaSemPermitidosNaoExpoe() {
        ProdutoBusca b = ProdutoBusca.de(produtoCompleto());
        assertThat(b.tamanho()).isNull();
        assertThat(b.cor()).isNull();
        assertThat(b.lote()).isNull();
        assertThat(b.validade()).isNull();
        assertThat(b.numeroSerie()).isNull();
        assertThat(b.url()).isEqualTo("/produtos/4");
        assertThat(b.categoria()).isEqualTo("Roupas");
    }

    @Test
    @DisplayName("ProdutoBusca.parecido marca aproximado e respeita os permitidos")
    void buscaParecido() {
        ProdutoBusca p = ProdutoBusca.parecido(produtoCompleto(), CamposRamo.permitidos(Subtipo.MERCADO));
        assertThat(p.aproximado()).isTrue();
        assertThat(p.validade()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(p.cor()).isNull();

        ProdutoBusca nenhum = ProdutoBusca.parecido(produtoCompleto());
        assertThat(nenhum.aproximado()).isTrue();
        assertThat(nenhum.validade()).isNull();
    }

    @Test
    @DisplayName("ProdutoBusca com produto antigo (campos null) não quebra")
    void buscaProdutoAntigo() {
        Produto p = Fabrica.produto(4, "Velho", 3, 1, usuario);
        p.setCategoria(Fabrica.categoria(3, "Geral", usuario));
        ProdutoBusca b = ProdutoBusca.de(p, CamposRamo.permitidos(Subtipo.FARMACIA));
        assertThat(b.lote()).isNull();
        assertThat(b.validade()).isNull();
    }
}
