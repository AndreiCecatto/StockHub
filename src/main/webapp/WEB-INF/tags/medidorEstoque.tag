<%@ tag description="Medidor de saldo: barra até saldo / (2 x mínimo), com marcador no mínimo" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
    Uso: <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
    A largura sai por classes de degrau (.sh-w-0 ... .sh-w-100, múltiplos de 5), sem style inline.
    A barra enche até saldo / (2 x mínimo), no máximo 100 %, e o marcador fica fixo no mínimo (50 %).
    Mínimo 0: não desenha barra (a tela mostra só o número). A cor segue a regra RN-12 (mesma de NivelEstoque).
    role="img" com aria-label dizendo o saldo e o mínimo.
--%>
<%@ attribute name="saldo" required="true" type="java.lang.Integer" %>
<%@ attribute name="minimo" required="true" type="java.lang.Integer" %>
<c:if test="${minimo > 0}">
    <c:set var="pct" value="${saldo * 50 / minimo}" />
    <c:if test="${pct > 100}"><c:set var="pct" value="100" /></c:if>
    <c:if test="${pct < 0}"><c:set var="pct" value="0" /></c:if>
    <fmt:formatNumber var="passo" value="${pct / 5}" maxFractionDigits="0" groupingUsed="false" />
    <c:set var="degrau" value="${passo * 5}" />
    <c:if test="${saldo > 0 and degrau == 0}"><c:set var="degrau" value="5" /></c:if>
    <c:set var="tom" value="${saldo <= 0 ? 'sem' : (saldo <= minimo ? 'baixo' : 'normal')}" />
    <span class="sh-meter sh-meter--<c:out value='${tom}'/>" role="img"
        aria-label="Saldo <c:out value='${saldo}'/> de mínimo <c:out value='${minimo}'/>">
        <span class="sh-meter__fill sh-w-<c:out value='${degrau}'/>"></span>
        <span class="sh-meter__minimo"></span>
    </span>
</c:if>
