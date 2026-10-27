package org.example.stockhubspring.repository;

import jakarta.persistence.criteria.Predicate;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.util.Textos;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

/** Filtros dinâmicos de produto, montados no banco (nada é filtrado em memória). */
public final class ProdutoSpecs {

    private ProdutoSpecs() {
    }

    /** Produtos ATIVOS da loja (RN-01 e RN-08): base de toda listagem. */
    public static Specification<Produto> ativosDoUsuario(Integer usuarioCodigo) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("usuario").get("codigo"), usuarioCodigo),
                cb.isTrue(root.get("ativo")));
    }

    /** Busca por nome do produto, nome da categoria ou código exato do produto. */
    public static Specification<Produto> comBusca(String busca) {
        return (root, query, cb) -> {
            String padrao = Textos.padraoLike(busca);
            Predicate porNome = cb.like(cb.lower(root.get("nome")), padrao, Textos.ESCAPE_LIKE);
            Predicate porCategoria = cb.like(cb.lower(root.join("categoria").get("nome")), padrao, Textos.ESCAPE_LIKE);
            Integer codigo = parseCodigo(busca);
            if (codigo == null) {
                return cb.or(porNome, porCategoria);
            }
            return cb.or(porNome, porCategoria, cb.equal(root.get("id"), codigo));
        };
    }

    /**
     * Nome contendo o termo, sem diferenciar caixa nem acento (busca rápida). O banco aplica
     * translate() ao nome; o termo recebe a mesma tabela em {@link Textos#semAcento}. Curingas
     * digitados (% e _) são literais, como em {@link #comBusca}.
     */
    public static Specification<Produto> nomeContendoSemAcento(String termo) {
        return (root, query, cb) -> {
            var nomeSimples = cb.lower(cb.function("translate", String.class, root.get("nome"),
                    cb.literal(Textos.ACENTUADAS), cb.literal(Textos.SEM_ACENTO)));
            return cb.like(nomeSimples, Textos.padraoLike(Textos.semAcento(termo)), Textos.ESCAPE_LIKE);
        };
    }

    public static Specification<Produto> daCategoria(Integer categoriaId) {
        return (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    /** Produtos em qualquer um dos níveis dados (RN-12, definição única em {@link NivelEstoque}). */
    public static Specification<Produto> comNivel(NivelEstoque... niveis) {
        return (root, query, cb) -> cb.or(Arrays.stream(niveis)
                .map(nivel -> nivel.predicado(cb, root.get("quantidade"), root.get("estoqueMinimo")))
                .toArray(Predicate[]::new));
    }

    private static Integer parseCodigo(String busca) {
        String limpa = Textos.limpar(busca);
        if (limpa == null || limpa.length() > 9 || !limpa.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Integer.valueOf(limpa);
    }
}
