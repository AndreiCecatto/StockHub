package org.example.stockhubspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Produto de uma loja. O saldo ({@code quantidade}) só muda por
 * {@code EstoqueService.movimentar} (RN-03). @DynamicUpdate faz o UPDATE gravar só as colunas
 * alteradas: editar o nome não reescreve o saldo e não pisa numa movimentação concorrente.
 */
@Entity
@Table(name = "produto")
@DynamicUpdate
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "O nome do produto é obrigatório.")
    @Size(max = 120, message = "O nome do produto deve ter no máximo 120 caracteres.")
    @Column(nullable = false, length = 120)
    private String nome;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    @Column(length = 500)
    private String descricao;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.00", message = "O preço deve ser maior ou igual a zero.")
    @DecimalMax(value = "9999999.99", message = "O preço deve ser no máximo 9.999.999,99.")
    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal preco = BigDecimal.ZERO;

    @Min(value = 0, message = "A quantidade deve ser maior ou igual a zero.")
    @Column(nullable = false)
    private int quantidade;

    @Min(value = 0, message = "O estoque mínimo deve ser maior ou igual a zero.")
    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    // false = produto inativado (RN-08): some das listagens, mas o histórico permanece
    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo", nullable = false)
    private Usuario usuario;

    /** Nível de estoque (RN-12). */
    public NivelEstoque getNivelEstoque() {
        return NivelEstoque.calcular(quantidade, estoqueMinimo);
    }

    /** Verdadeiro só no nível BAIXO (saldo maior que zero e dentro do mínimo); sem estoque é outro nível. */
    public boolean isEstoqueBaixo() {
        return getNivelEstoque() == NivelEstoque.BAIXO;
    }

    public BigDecimal getValorTotal() {
        return preco.multiply(BigDecimal.valueOf(quantidade));
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
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
