package org.example.stockhubspring.repository;

import jakarta.persistence.criteria.Predicate;
import org.example.stockhubspring.dto.ClienteFiltro;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.util.Documentos;
import org.example.stockhubspring.util.Textos;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Filtros dinâmicos da lista de clientes: tudo parametrizado e sempre dentro da loja (RN-01, RN-97). */
public final class ClienteSpecs {

    private ClienteSpecs() {
    }

    public static Specification<Parceiro> de(Integer lojaCodigo, ClienteFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> e = new ArrayList<>();
            e.add(cb.equal(root.get("usuario").get("codigo"), lojaCodigo));
            e.add(cb.isTrue(root.get("cliente")));
            switch (filtro.situacao()) {
                case ATIVOS -> e.add(cb.isTrue(root.get("ativo")));
                case INATIVOS -> e.add(cb.isFalse(root.get("ativo")));
                default -> { }
            }
            if (filtro.comOs() != ClienteFiltro.ComOs.TODOS) {
                var sub = query.subquery(Integer.class);
                var os = sub.from(OrdemServico.class);
                sub.select(os.get("id")).where(
                        cb.equal(os.get("cliente"), root),
                        cb.equal(os.get("usuario").get("codigo"), lojaCodigo));
                e.add(filtro.comOs() == ClienteFiltro.ComOs.COM ? cb.exists(sub) : cb.not(cb.exists(sub)));
            }
            if (filtro.busca() != null) {
                e.add(busca(filtro.busca()).toPredicate(root, query, cb));
            }
            return cb.and(e.toArray(Predicate[]::new));
        };
    }

    /** Nome e fantasia (sem caixa nem acento), e-mail e, se o termo tiver dígitos, documento e telefones. */
    private static Specification<Parceiro> busca(String termo) {
        return (root, query, cb) -> {
            List<Predicate> ou = new ArrayList<>();
            String padraoTexto = Textos.padraoLike(Textos.semAcento(termo));
            for (String campo : new String[]{"nome", "nomeFantasia"}) {
                var simples = cb.lower(cb.function("translate", String.class, root.<String>get(campo),
                        cb.literal(Textos.ACENTUADAS), cb.literal(Textos.SEM_ACENTO)));
                ou.add(cb.like(simples, padraoTexto, Textos.ESCAPE_LIKE));
            }
            ou.add(cb.like(cb.lower(root.<String>get("email")), Textos.padraoLike(termo), Textos.ESCAPE_LIKE));
            String digitos = Documentos.soDigitos(termo);
            if (!digitos.isEmpty()) {
                String padraoDigitos = "%" + digitos + "%";
                ou.add(cb.like(root.<String>get("documento"), padraoDigitos));
                ou.add(cb.like(root.<String>get("telefone"), padraoDigitos));
                ou.add(cb.like(root.<String>get("whatsapp"), padraoDigitos));
            }
            return cb.or(ou.toArray(Predicate[]::new));
        };
    }
}
