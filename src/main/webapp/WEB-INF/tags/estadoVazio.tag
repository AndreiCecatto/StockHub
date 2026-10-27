<%@ tag description="Estado vazio: ilustração geométrica de traço fino, título curto, uma frase e um botão de ação" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:estadoVazio ilustracao="caixa" titulo="Nenhum produto ainda"
             texto="Cadastre o primeiro produto para começar." acaoUrl="/produtos/novo" acaoRotulo="Novo produto" />
    ilustracao: caixa (volume isométrico, padrão), lupa (nada encontrado), etiquetas (categorias),
                ok (vazio positivo, com visto) ou erro (falha ao carregar).
                Sem este atributo, sai do "icone" (v1): search vira lupa, tags vira etiquetas e tom="sucesso" vira ok.
    icone: nome do Bootstrap Icons sem o prefixo "bi-"; só serve para escolher a ilustração (não é desenhado).
    acaoUrl: caminho SEM o contexto da aplicação (o tag acrescenta). Sem acaoUrl, não mostra botão.
    tom: "sucesso" para o vazio positivo ("Tudo em dia") ou "erro" (usa role="alert"); padrão neutro.
    A API é a mesma da v1/v2. As ilustrações são SVG inline decorativas (aria-hidden), sem personagem: formas
    geométricas de traço fino que se desenham uma vez (css/movimento.css; sem animação com "menos movimento").
--%>
<%@ attribute name="titulo" required="true" type="java.lang.String" %>
<%@ attribute name="texto" required="false" type="java.lang.String" %>
<%@ attribute name="icone" required="false" type="java.lang.String" %>
<%@ attribute name="ilustracao" required="false" type="java.lang.String" %>
<%@ attribute name="acaoUrl" required="false" type="java.lang.String" %>
<%@ attribute name="acaoRotulo" required="false" type="java.lang.String" %>
<%@ attribute name="tom" required="false" type="java.lang.String" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="ilu" value="${not empty ilustracao ? ilustracao : (tom == 'sucesso' ? 'ok' : (tom == 'erro' ? 'erro' : (icone == 'search' or icone == 'funnel' ? 'lupa' : (icone == 'tags' or icone == 'tag' ? 'etiquetas' : 'caixa'))))}" />
<div class="sh-vazio ${tom == 'sucesso' ? 'sh-vazio--sucesso' : (tom == 'erro' ? 'sh-vazio--erro' : '')}" ${tom == 'erro' ? 'role="alert"' : ''}>
    <c:choose>
        <c:when test="${ilu == 'lupa'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 160" aria-hidden="true" focusable="false">
                <path class="il-fio-suave" d="M20 146 H220"/>
                <rect class="il-plano-suave" x="28" y="34" width="104" height="14" rx="4"/>
                <rect class="il-plano-suave" x="28" y="58" width="76" height="14" rx="4"/>
                <rect class="il-plano-suave" x="28" y="82" width="92" height="14" rx="4"/>
                <rect class="il-plano-suave" x="28" y="106" width="60" height="14" rx="4"/>
                <g class="il-surge">
                    <circle class="il-plano" cx="146" cy="76" r="36"/>
                    <path class="il-fio-acento il-desenha" pathLength="1" d="M132 76 H160"/>
                    <path class="il-fio il-desenha-2" pathLength="1" d="M172 102 L204 134"/>
                </g>
            </svg>
        </c:when>
        <c:when test="${ilu == 'etiquetas'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 160" aria-hidden="true" focusable="false">
                <path class="il-fio-suave" d="M20 146 H220"/>
                <path class="il-plano-suave" d="M92 44 H156 L190 78 L156 112 H92 Z" transform="translate(14 14)"/>
                <g class="il-surge">
                    <path class="il-plano" d="M52 38 H124 L158 72 L124 106 H52 Z"/>
                    <circle class="il-fio" cx="70" cy="72" r="6"/>
                    <path class="il-fio-acento il-desenha" pathLength="1" d="M88 62 H116 M88 74 H108 M88 86 H112"/>
                </g>
            </svg>
        </c:when>
        <c:when test="${ilu == 'ok'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 160" aria-hidden="true" focusable="false">
                <path class="il-fio-suave" d="M20 146 H220"/>
                <circle class="il-fio-suave" cx="120" cy="76" r="62"/>
                <circle class="il-plano il-surge" cx="120" cy="76" r="46"/>
                <path class="il-fio-ok il-desenha" pathLength="1" d="M99 77 L114 92 L142 62"/>
            </svg>
        </c:when>
        <c:when test="${ilu == 'erro'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 160" aria-hidden="true" focusable="false">
                <path class="il-fio-suave" d="M20 146 H220"/>
                <path class="il-plano il-surge" d="M120 24 L176 120 H64 Z" stroke-linejoin="round"/>
                <path class="il-fio-erro il-desenha" pathLength="1" d="M120 56 V92"/>
                <circle class="il-fio-erro" cx="120" cy="107" r="1.5"/>
            </svg>
        </c:when>
        <c:otherwise>
            <%-- volume isométrico: três faces, uma aresta de acento e um volume tracejado indicando "adicionar" --%>
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 160" aria-hidden="true" focusable="false">
                <path class="il-fio-suave" d="M20 146 H220"/>
                <path class="il-fio-acento il-surge" d="M120 14 L148 30 L120 46 L92 30 Z" stroke-dasharray="3 4"/>
                <g class="il-surge">
                    <path class="il-plano" d="M120 56 L176 84 L120 112 L64 84 Z"/>
                    <path class="il-plano-suave" d="M64 84 L120 112 V148 L64 120 Z"/>
                    <path class="il-plano-suave" d="M176 84 L120 112 V148 L176 120 Z"/>
                </g>
                <path class="il-fio-acento il-desenha" pathLength="1" d="M120 112 V148"/>
            </svg>
        </c:otherwise>
    </c:choose>
    <p class="sh-vazio__titulo"><c:out value="${titulo}" /></p>
    <c:if test="${not empty texto}">
        <p class="sh-vazio__texto"><c:out value="${texto}" /></p>
    </c:if>
    <c:if test="${not empty acaoUrl}">
        <a class="btn btn-primary" href="<c:out value='${ctx}${acaoUrl}'/>"><c:out value="${acaoRotulo}" /></a>
    </c:if>
</div>
