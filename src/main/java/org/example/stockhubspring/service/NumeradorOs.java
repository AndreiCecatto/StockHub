package org.example.stockhubspring.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tira o próximo número de OS de uma loja (RN-78). O UPSERT é atômico e a linha do contador fica travada até o fim
 * da transação, então duas OS criadas ao mesmo tempo nunca repetem número.
 */
@Component
public class NumeradorOs {

    private final JdbcTemplate jdbc;

    public NumeradorOs(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int proximo(Integer lojaCodigo) {
        Integer numero = jdbc.queryForObject(
                "insert into os_contador (usuario_codigo, ultimo) values (?, 1) "
                        + "on conflict (usuario_codigo) do update set ultimo = os_contador.ultimo + 1 "
                        + "returning ultimo", Integer.class, lojaCodigo);
        return numero == null ? 1 : numero;
    }
}
