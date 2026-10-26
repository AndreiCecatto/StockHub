package org.example.stockhubspring.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/** Ajuda a montar paginação segura (página e tamanho limitados) sem repetir código nos services. */
public final class Paginas {

    private Paginas() {
    }

    /**
     * Executa a consulta paginada. A página vem em base 1 (como nas telas); valores fora do
     * intervalo são ajustados e, se a página pedida passar da última, devolve a última.
     */
    public static <T> Page<T> buscar(int pagina, int tamanho, int tamanhoMaximo, Sort ordenacao,
                                     Function<Pageable, Page<T>> consulta) {
        int tamanhoSeguro = Math.max(1, Math.min(tamanho, tamanhoMaximo));
        int indice = Math.max(1, pagina) - 1;
        Page<T> resultado = consulta.apply(PageRequest.of(indice, tamanhoSeguro, ordenacao));
        if (resultado.getTotalPages() > 0 && indice >= resultado.getTotalPages()) {
            resultado = consulta.apply(PageRequest.of(resultado.getTotalPages() - 1, tamanhoSeguro, ordenacao));
        }
        return resultado;
    }
}
