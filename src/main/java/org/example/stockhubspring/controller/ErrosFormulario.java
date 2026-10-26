package org.example.stockhubspring.controller;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transforma os erros de validação em texto amigável para as telas. Resolve as mensagens pelo
 * {@link MessageSource} (messages.properties), assim erros de conversão (ex.: letra no campo de
 * preço) também saem em português, sem o texto técnico do Spring.
 */
@Component
public class ErrosFormulario {

    private final MessageSource messageSource;

    public ErrosFormulario(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Publica no model os erros do formulário: {@code erro} (todas as mensagens juntas, para o
     * alerta no topo) e {@code errosCampos} (mensagem por nome de campo, para mostrar junto ao campo).
     */
    public void publicar(BindingResult resultado, Model model) {
        model.addAttribute("erro", resumir(resultado));
        model.addAttribute("errosCampos", porCampo(resultado));
    }

    public String resumir(BindingResult resultado) {
        return resultado.getAllErrors().stream()
                .map(this::mensagem)
                .distinct()
                .collect(Collectors.joining(" "));
    }

    public Map<String, String> porCampo(BindingResult resultado) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : resultado.getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), mensagem(erro));
        }
        return erros;
    }

    private String mensagem(ObjectError erro) {
        return messageSource.getMessage(erro, LocaleContextHolder.getLocale());
    }
}
