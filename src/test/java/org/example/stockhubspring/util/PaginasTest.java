package org.example.stockhubspring.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginasTest {

    private final Sort ordem = Sort.by("id");

    @Test
    @DisplayName("página e tamanho inválidos são corrigidos (base 1, tamanho entre 1 e o máximo)")
    void corrigePaginaETamanhoInvalidos() {
        List<Pageable> pedidos = new ArrayList<>();
        Paginas.buscar(-4, 5000, 100, ordem, pageable -> {
            pedidos.add(pageable);
            return new PageImpl<>(Collections.<String>emptyList(), pageable, 0);
        });
        Paginas.buscar(1, 0, 100, ordem, pageable -> {
            pedidos.add(pageable);
            return new PageImpl<>(Collections.<String>emptyList(), pageable, 0);
        });
        assertThat(pedidos.get(0).getPageNumber()).isZero();
        assertThat(pedidos.get(0).getPageSize()).isEqualTo(100);
        assertThat(pedidos.get(1).getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("página além da última devolve a última página")
    void paginaAlemDaUltimaDevolveAUltima() {
        List<String> todos = List.of("a", "b", "c", "d", "e");
        Page<String> resultado = Paginas.buscar(99, 2, 100, ordem, pageable -> {
            int inicio = (int) Math.min(pageable.getOffset(), todos.size());
            int fim = Math.min(inicio + pageable.getPageSize(), todos.size());
            return new PageImpl<>(todos.subList(inicio, fim), pageable, todos.size());
        });
        assertThat(resultado.getNumber()).isEqualTo(2);
        assertThat(resultado.getContent()).containsExactly("e");
    }
}
