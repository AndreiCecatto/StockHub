package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.PreferenciaBloco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreferenciaBlocoRepository extends JpaRepository<PreferenciaBloco, PreferenciaBloco.Chave> {

    List<PreferenciaBloco> findByUsuarioCodigoAndTela(Integer usuarioCodigo, String tela);

    /** Grava ou atualiza uma linha; nunca apaga (RN-113). */
    @Modifying
    @Query(value = """
            INSERT INTO preferencia_bloco (usuario_codigo, tela, bloco, visivel, atualizado_em, atualizado_por)
            VALUES (:loja, :tela, :bloco, :visivel, now(), :membro)
            ON CONFLICT (usuario_codigo, tela, bloco)
            DO UPDATE SET visivel = EXCLUDED.visivel, atualizado_em = now(), atualizado_por = EXCLUDED.atualizado_por
            """, nativeQuery = true)
    void gravar(@Param("loja") Integer loja, @Param("tela") String tela, @Param("bloco") String bloco,
                @Param("visivel") boolean visivel, @Param("membro") Integer membro);
}
