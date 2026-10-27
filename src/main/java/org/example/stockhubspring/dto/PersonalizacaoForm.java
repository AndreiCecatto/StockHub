package org.example.stockhubspring.dto;

import java.util.ArrayList;
import java.util.List;

/** Formulário da personalização: códigos dos blocos marcados (os não enviados ficam ocultos). */
public class PersonalizacaoForm {

    private List<String> blocos = new ArrayList<>();

    public List<String> getBlocos() { return blocos; }

    public void setBlocos(List<String> blocos) {
        this.blocos = blocos == null ? new ArrayList<>() : blocos;
    }
}
