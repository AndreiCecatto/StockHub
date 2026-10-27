<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Painel lateral deslizante ("peek"): abre os detalhes de um item sem sair da lista. Esqueleto vazio, montado por js/peek.js.
    Contrato completo (atributos, eventos, API) em docs/design-system.md, seção "v3: central de operações".
    Resumo: um link com data-sh-peek abre o painel, que busca o href por GET e mostra a região marcada com data-peek.
    ARIA: <dialog> modal com rótulo (o título do painel); foco preso, Esc fecha, o foco volta ao gatilho.
    O conteúdo carregado é HTML da própria aplicação (já escapado no servidor), com script, style e atributos on* removidos.
--%>
<dialog class="sh-peek" id="sh-peek" aria-labelledby="sh-peek-titulo">
    <div class="sh-peek__caixa">
        <header class="sh-peek__cab">
            <h2 class="sh-peek__titulo" id="sh-peek-titulo" data-sh-peek-titulo-saida>Detalhes</h2>
            <a class="btn btn-ghost btn-sm sh-peek__pagina" href="#" data-sh-peek-pagina hidden>
                Abrir página <i class="bi bi-arrow-up-right" aria-hidden="true"></i>
            </a>
            <button type="button" class="btn btn-ghost btn-sm sh-peek__fechar" data-sh-peek-fechar aria-label="Fechar painel">
                <i class="bi bi-x-lg" aria-hidden="true"></i>
            </button>
        </header>
        <div class="sh-peek__corpo" tabindex="-1" data-sh-peek-corpo aria-busy="false"></div>
    </div>
</dialog>
