package org.example.stockhubspring.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.util.Textos;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de entrada do formulário de produto (RN-11). Não tem usuário nem "ativo": o dono vem
 * sempre da sessão, nunca do POST. {@code quantidade} só vale no cadastro (estoque inicial,
 * RN-04); na edição é ignorada (RN-03).
 */
@EstoqueInicialValido
public class ProdutoForm {

    /** Maior estoque inicial no cadastro (o mesmo limite de uma movimentação de entrada, RN-05). */
    public static final int ESTOQUE_INICIAL_MAXIMO = 1_000_000;

    private Integer id;

    @NotBlank(message = "O nome do produto é obrigatório.")
    @Size(min = 2, max = 120, message = "O nome do produto deve ter entre 2 e 120 caracteres.")
    private String nome;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    private String descricao;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.00", message = "O preço deve ser maior ou igual a zero.")
    @DecimalMax(value = "9999999.99", message = "O preço deve ser no máximo 9.999.999,99.")
    @Digits(integer = 7, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.")
    private BigDecimal preco = BigDecimal.ZERO;

    @Min(value = 0, message = "A quantidade deve ser maior ou igual a zero.")
    // O máximo (1.000.000) vale só no cadastro: ver @EstoqueInicialValido na classe
    private Integer quantidade = 0;

    @NotNull(message = "Informe o estoque mínimo.")
    @Min(value = 0, message = "O estoque mínimo deve ser maior ou igual a zero.")
    @Max(value = 2_000_000_000, message = "O estoque mínimo é grande demais.")
    private Integer estoqueMinimo = 0;

    @NotNull(message = "Selecione uma categoria.")
    private Integer categoriaId;

    // Campos por ramo (AD-17): o service só aplica os que o subtipo da loja permite; os demais são ignorados
    private String tamanho;
    private String cor;
    private String lote;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataValidade;
    private String numeroSerie;

    /** Preenche o formulário de edição a partir do produto (inclui o saldo atual, só para exibição). */
    public static ProdutoForm de(Produto produto) {
        ProdutoForm form = new ProdutoForm();
        form.id = produto.getId();
        form.nome = produto.getNome();
        form.descricao = produto.getDescricao();
        form.preco = produto.getPreco();
        form.quantidade = produto.getQuantidade();
        form.estoqueMinimo = produto.getEstoqueMinimo();
        form.categoriaId = produto.getCategoria() == null ? null : produto.getCategoria().getId();
        form.tamanho = produto.getTamanho();
        form.cor = produto.getCor();
        form.lote = produto.getLote();
        form.dataValidade = produto.getDataValidade();
        form.numeroSerie = produto.getNumeroSerie();
        return form;
    }

    /**
     * Compatibilidade com a JSP, que compara {@code produto.categoria.id} para marcar a opção
     * selecionada. Só leitura (não há setter, então o POST não consegue alterar nada por aqui).
     */
    public CategoriaSelecionada getCategoria() {
        return new CategoriaSelecionada(categoriaId);
    }

    /** Referência mínima à categoria escolhida (apenas o id). */
    public static final class CategoriaSelecionada {
        private final Integer id;

        CategoriaSelecionada(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Textos.limpar(nome);
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = Textos.limpar(descricao);
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Integer estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = Textos.limpar(tamanho);
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = Textos.limpar(cor);
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = Textos.limpar(lote);
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = Textos.limpar(numeroSerie);
    }
}
