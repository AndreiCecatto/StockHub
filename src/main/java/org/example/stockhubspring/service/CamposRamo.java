package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Subtipo;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mapa fechado "subtipo da loja -> campos de produto permitidos" (AD-17, RN-100). É código, não tabela:
 * subtipo sem entrada não tem campos extras. A oficina fica para a fatia 2.
 */
public final class CamposRamo {

    /** Campos opcionais do produto que dependem do ramo. */
    public enum Campo {
        TAMANHO, COR, LOTE, VALIDADE, NUMERO_SERIE,
        /** Veículo do equipamento do cliente (AD-18), só MECANICA e MOTO. */
        PLACA, MODELO
    }

    /** Campos que valem para uma loja, em forma amigável para a JSP ({@code camposRamo.validade}). */
    public record Permitidos(boolean tamanho, boolean cor, boolean lote, boolean validade, boolean numeroSerie,
                             boolean placa, boolean modelo) {

        public static final Permitidos NENHUM = new Permitidos(false, false, false, false, false);

        /** Construtor dos campos de produto (sem veículo). */
        public Permitidos(boolean tamanho, boolean cor, boolean lote, boolean validade, boolean numeroSerie) {
            this(tamanho, cor, lote, validade, numeroSerie, false, false);
        }

        public boolean permite(Campo campo) {
            return switch (campo) {
                case TAMANHO -> tamanho;
                case COR -> cor;
                case LOTE -> lote;
                case VALIDADE -> validade;
                case NUMERO_SERIE -> numeroSerie;
                case PLACA -> placa;
                case MODELO -> modelo;
            };
        }

        /** Verdadeiro se a loja guarda o veículo (placa ou modelo) no equipamento do cliente. */
        public boolean temVeiculo() {
            return placa || modelo;
        }

        /** Getter JavaBean para a EL: {@code ${camposRamo.temVeiculo}}. */
        public boolean isTemVeiculo() {
            return temVeiculo();
        }

        /** Verdadeiro se a loja usa pelo menos um campo extra de PRODUTO (placa e modelo não contam). */
        public boolean temAlgum() {
            return tamanho || cor || lote || validade || numeroSerie;
        }

        /** Getter JavaBean para a EL: {@code ${camposRamo.temAlgum}} só resolve com o prefixo "is". */
        public boolean isTemAlgum() {
            return temAlgum();
        }
    }

    public static final int TAMANHO_MAX = 20;
    public static final int COR_MAX = 40;
    public static final int LOTE_MAX = 40;
    public static final int NUMERO_SERIE_MAX = 60;
    public static final int PLACA_MAX = 7;
    public static final int MODELO_MAX = 80;

    private static final Map<Subtipo, Set<Campo>> MAPA = new EnumMap<>(Subtipo.class);

    static {
        MAPA.put(Subtipo.VESTUARIO, EnumSet.of(Campo.TAMANHO, Campo.COR));
        MAPA.put(Subtipo.FARMACIA, EnumSet.of(Campo.LOTE, Campo.VALIDADE));
        MAPA.put(Subtipo.MERCADO, EnumSet.of(Campo.VALIDADE));
        MAPA.put(Subtipo.INFORMATICA, EnumSet.of(Campo.NUMERO_SERIE));
        MAPA.put(Subtipo.MECANICA, EnumSet.of(Campo.PLACA, Campo.MODELO));
        MAPA.put(Subtipo.MOTO, EnumSet.of(Campo.PLACA, Campo.MODELO));
    }

    private CamposRamo() {
    }

    /** Campos permitidos para o subtipo (null ou sem entrada: nenhum). */
    public static Set<Campo> campos(Subtipo subtipo) {
        Set<Campo> campos = subtipo == null ? null : MAPA.get(subtipo);
        return campos == null ? EnumSet.noneOf(Campo.class) : EnumSet.copyOf(campos);
    }

    public static Permitidos permitidos(Subtipo subtipo) {
        Set<Campo> c = campos(subtipo);
        return new Permitidos(c.contains(Campo.TAMANHO), c.contains(Campo.COR), c.contains(Campo.LOTE),
                c.contains(Campo.VALIDADE), c.contains(Campo.NUMERO_SERIE), c.contains(Campo.PLACA),
                c.contains(Campo.MODELO));
    }
}
