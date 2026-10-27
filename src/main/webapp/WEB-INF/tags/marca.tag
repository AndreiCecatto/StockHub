<%@ tag description="Marca do StockHub: quadrado de acento com três barras de nível ascendente (SVG inline, decorativo)" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:marca />  ou  <sh:marca tamanho="40" classe="minha-classe" />
    tamanho: lado em pixels (padrão 28). classe: classes extras no <svg>.
    Decorativo (aria-hidden): o nome "StockHub" vem sempre escrito ao lado. As cores vêm do CSS (.sh-logo em css/layout.css).
    Substitui o mascote da v2.
--%>
<%@ attribute name="tamanho" required="false" type="java.lang.String" %>
<%@ attribute name="classe" required="false" type="java.lang.String" %>
<c:set var="lado" value="${empty tamanho ? '28' : tamanho}" />
<svg class="sh-logo <c:out value='${classe}'/>" width="<c:out value='${lado}'/>" height="<c:out value='${lado}'/>"
    viewBox="0 0 32 32" aria-hidden="true" focusable="false">
    <rect class="sh-logo__fundo" width="32" height="32" rx="8"/>
    <rect class="sh-logo__barra" x="7.5" y="16" width="4.5" height="8.5" rx="1.4"/>
    <rect class="sh-logo__barra" x="13.75" y="11" width="4.5" height="13.5" rx="1.4"/>
    <rect class="sh-logo__barra" x="20" y="6.5" width="4.5" height="18" rx="1.4"/>
</svg>
