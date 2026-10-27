package org.example.stockhubspring.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Importação pendente para a lista da tela (sem o XML, que nunca vai para a tela). */
public record PendenteNfe(UUID token, String chaveAcesso, LocalDateTime criadaEm, LocalDateTime expiraEm) {
}
