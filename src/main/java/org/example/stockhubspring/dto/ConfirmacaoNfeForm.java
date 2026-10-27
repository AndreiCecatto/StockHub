package org.example.stockhubspring.dto;

import jakarta.validation.Valid;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Confirmação da importação: só as decisões por item e a confirmação do destinatário. Nada da nota
 * (itens, quantidades, valores) vem do navegador: o service relê o XML guardado.
 */
public class ConfirmacaoNfeForm {

    /** Checkbox "esta nota é minha": a confirmação é recusada sem ele. */
    private boolean confirmouDestinatario;

    /** Decisões indexadas pelo número do item da nota (1..990): {@code item[1].acao} etc. */
    private Map<Integer, @Valid DecisaoItemNfeForm> item = new LinkedHashMap<>();

    public boolean isConfirmouDestinatario() {
        return confirmouDestinatario;
    }

    public void setConfirmouDestinatario(boolean confirmouDestinatario) {
        this.confirmouDestinatario = confirmouDestinatario;
    }

    public Map<Integer, DecisaoItemNfeForm> getItem() {
        return item;
    }

    public void setItem(Map<Integer, DecisaoItemNfeForm> item) {
        this.item = item;
    }
}
