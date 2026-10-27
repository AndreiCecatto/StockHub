package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.OrdemServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumo do cliente no detalhe (RN-98). Só o que o modelo sustenta: as ordens de serviço. A venda de balcão não
 * guarda o cliente, então não entra aqui.
 *
 * @param totalOs       todas as ordens do cliente
 * @param osEmAndamento orçamento, aprovada, em execução ou concluída (ainda não entregue nem cancelada)
 * @param osEntregues   ordens já entregues
 * @param totalEntregue soma das ordens entregues
 * @param ultimaVisita  abertura da OS mais recente (null se nunca teve OS)
 * @param ultimas       as ordens mais recentes
 */
public record ClienteResumo(long totalOs, long osEmAndamento, long osEntregues, BigDecimal totalEntregue,
                            LocalDateTime ultimaVisita, List<OrdemServico> ultimas) {
}
