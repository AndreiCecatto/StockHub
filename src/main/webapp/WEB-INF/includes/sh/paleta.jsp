<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Paleta de comandos (Ctrl+K ou Cmd+K, ou o botão da barra superior). Esqueleto vazio: js/paleta.js monta as opções
    (telas, ações e busca de produtos em GET /api/busca) com textContent, nunca com HTML de dado.
    ARIA: <dialog> modal com rótulo; o campo é um combobox (role="combobox", aria-controls, aria-activedescendant) e a
    lista é um listbox com grupos. O resultado é anunciado em #sh-paleta-status (aria-live polite).
    Sem JavaScript o botão que abre a paleta não faz nada: a barra superior traz um <noscript> com a busca comum.
--%>
<dialog class="sh-paleta" id="sh-paleta" aria-label="Paleta de comandos">
    <div class="sh-paleta__caixa">
        <div class="sh-paleta__topo">
            <i class="bi bi-search" aria-hidden="true"></i>
            <label class="visually-hidden" for="sh-paleta-entrada">Buscar produtos, telas e ações</label>
            <input id="sh-paleta-entrada" class="sh-paleta__entrada" type="text" role="combobox"
                aria-expanded="true" aria-controls="sh-paleta-lista" aria-autocomplete="list" aria-haspopup="listbox"
                autocomplete="off" spellcheck="false" autocapitalize="none" maxlength="120"
                placeholder="Buscar produto, ir para uma tela, fazer algo…">
            <button type="button" class="sh-paleta__fechar" data-sh-fechar-paleta aria-label="Fechar a paleta">Esc</button>
        </div>
        <div class="sh-paleta__lista" id="sh-paleta-lista" role="listbox" aria-label="Resultados"></div>
        <p class="sh-paleta__rodape" aria-hidden="true">
            <span><kbd>↑</kbd> <kbd>↓</kbd> navegar</span>
            <span><kbd>Enter</kbd> abrir</span>
            <span><kbd>Esc</kbd> fechar</span>
        </p>
        <div class="visually-hidden" id="sh-paleta-status" role="status" aria-live="polite"></div>
    </div>
</dialog>
