package org.example.stockhubspring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.util.Textos;

/** Dados de entrada do formulário de categoria (RN-11). O dono vem sempre da sessão. */
public class CategoriaForm {

    private Integer id;

    @NotBlank(message = "O nome da categoria é obrigatório.")
    @Size(min = 2, max = 120, message = "O nome da categoria deve ter entre 2 e 120 caracteres.")
    private String nome;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    private String descricao;

    public static CategoriaForm de(Categoria categoria) {
        CategoriaForm form = new CategoriaForm();
        form.id = categoria.getId();
        form.nome = categoria.getNome();
        form.descricao = categoria.getDescricao();
        return form;
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
}
