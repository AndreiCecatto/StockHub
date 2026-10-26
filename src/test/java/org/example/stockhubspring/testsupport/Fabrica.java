package org.example.stockhubspring.testsupport;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;

import java.math.BigDecimal;

/** Monta objetos de domínio sintéticos para os testes (nunca usa dados reais). */
public final class Fabrica {

    private Fabrica() {
    }

    public static Usuario usuario(Integer codigo) {
        Usuario usuario = new Usuario();
        usuario.setCodigo(codigo);
        usuario.setNome("Loja " + codigo);
        usuario.setEmail("loja" + codigo + "@teste.invalid");
        usuario.setSenha("hash-nao-usado");
        usuario.setAtivo(true);
        return usuario;
    }

    public static Categoria categoria(Integer id, String nome, Usuario dono) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome(nome);
        categoria.setUsuario(dono);
        return categoria;
    }

    public static Produto produto(Integer id, String nome, int quantidade, int estoqueMinimo, Usuario dono) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(new BigDecimal("10.00"));
        produto.setQuantidade(quantidade);
        produto.setEstoqueMinimo(estoqueMinimo);
        produto.setAtivo(true);
        produto.setUsuario(dono);
        return produto;
    }
}
