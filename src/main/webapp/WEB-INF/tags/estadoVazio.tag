<%@ tag description="Estado vazio: ilustração desenhada à mão, título curto, uma frase e um botão de ação" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Uso: <sh:estadoVazio ilustracao="caixa" titulo="Nenhum produto ainda"
             texto="Cadastre seu primeiro produto para começar." acaoUrl="/produtos/novo" acaoRotulo="Novo produto" />
    ilustracao: caixa (mascote dormindo, padrão), lupa (mascote atenta com lupa, para "nada encontrado"),
                etiquetas (etiquetas soltas, para categorias) ou ok (mascote contente com visto, para o vazio positivo).
                Sem este atributo, sai do "icone" (v1): search vira lupa, tags vira etiquetas e tom="sucesso" vira ok.
    icone: nome do Bootstrap Icons sem o prefixo "bi-"; só serve para escolher a ilustração (não é mais desenhado).
    acaoUrl: caminho SEM o contexto da aplicação (o tag acrescenta). Sem acaoUrl, não mostra botão.
    tom: "sucesso" para o vazio positivo ("Tudo em dia"); padrão neutro.
    As ilustrações são SVG inline, decorativas (aria-hidden), com traço levemente irregular de propósito.
--%>
<%@ attribute name="titulo" required="true" type="java.lang.String" %>
<%@ attribute name="texto" required="false" type="java.lang.String" %>
<%@ attribute name="icone" required="false" type="java.lang.String" %>
<%@ attribute name="ilustracao" required="false" type="java.lang.String" %>
<%@ attribute name="acaoUrl" required="false" type="java.lang.String" %>
<%@ attribute name="acaoRotulo" required="false" type="java.lang.String" %>
<%@ attribute name="tom" required="false" type="java.lang.String" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="ilu" value="${not empty ilustracao ? ilustracao : (tom == 'sucesso' ? 'ok' : (icone == 'search' or icone == 'funnel' ? 'lupa' : (icone == 'tags' or icone == 'tag' ? 'etiquetas' : 'caixa')))}" />
<div class="sh-vazio ${tom == 'sucesso' ? 'sh-vazio--sucesso' : ''}">
    <c:choose>
        <c:when test="${ilu == 'lupa'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 190" aria-hidden="true" focusable="false">
                <path class="il-tinta-claro" d="M18 176 L222 175"/>
                <sh:mascote estado="atenta" tamanho="m" classe="sh-mascote--na-ilustracao sh-mascote--esq" />
                <%-- lupa desenhada à mão, por cima do mascote --%>
                <circle class="il-vidro" cx="178" cy="96" r="28"/>
                <path class="il-traco" d="M166 84 C170 79 177 77 183 79"/>
                <path class="il-cabo" d="M197 117 L221 146 C224 150 230 147 228 142 L207 110 Z"/>
            </svg>
        </c:when>
        <c:when test="${ilu == 'etiquetas'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 190" aria-hidden="true" focusable="false">
                <path class="il-tinta-claro" d="M120 8 C118 30 126 38 121 60"/>
                <g class="il-etiqueta-a">
                    <path class="il-papel" d="M64 70 L126 62 L156 116 L99 160 L56 126 Z"/>
                    <circle class="il-traco" cx="123" cy="76" r="5"/>
                    <path class="il-traco" d="M92 112 L120 104 M88 126 L108 120"/>
                </g>
                <g class="il-etiqueta-b">
                    <path class="il-papel" d="M150 96 L204 106 L208 162 L150 168 L136 130 Z"/>
                    <circle class="il-traco" cx="190" cy="118" r="5"/>
                    <path class="il-traco" d="M160 138 L190 140 M162 152 L184 154"/>
                </g>
                <path class="il-tinta-claro" d="M18 178 L222 177"/>
            </svg>
        </c:when>
        <c:when test="${ilu == 'ok'}">
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 190" aria-hidden="true" focusable="false">
                <path class="il-marca" d="M172 40 C182 52 186 60 192 70 C206 46 218 30 232 18"/>
                <sh:mascote estado="contente" tamanho="m" classe="sh-mascote--na-ilustracao" />
            </svg>
        </c:when>
        <c:otherwise>
            <svg class="sh-vazio__ilustra" viewBox="0 0 240 190" aria-hidden="true" focusable="false">
                <path class="il-tinta-claro" d="M18 178 L222 177"/>
                <sh:mascote estado="dormindo" tamanho="m" classe="sh-mascote--na-ilustracao" />
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
