<%@ tag description="CFOP em linguagem comum (Tipo da operação), com o número como detalhe" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:cfop codigo="${it.cfop}" />   ou   <sh:cfop codigo="5102" semNumero="true" />
    Lugar único do mapeamento CFOP -> texto. Os 4 dígitos são o código oficial; o número aparece em letra pequena
    e no title (dica ao passar o mouse). Código desconhecido sai como "Operação código NNNN".
    Sempre escapado com c:out.
--%>
<%@ attribute name="codigo" required="false" type="java.lang.String" %>
<%@ attribute name="semNumero" required="false" type="java.lang.Boolean" %>
<c:set var="cod" value="${fn:trim(codigo)}" />
<c:choose>
    <c:when test="${empty cod}">&mdash;</c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${cod == '1102' or cod == '2102' or cod == '1101' or cod == '2101' or cod == '1403' or cod == '2403' or cod == '1401' or cod == '2401'}"><c:set var="texto" value="Compra para revenda" /></c:when>
            <c:when test="${cod == '5102' or cod == '6102' or cod == '5101' or cod == '6101' or cod == '5405' or cod == '6404' or cod == '5403' or cod == '6403'}"><c:set var="texto" value="Venda de mercadoria" /></c:when>
            <c:when test="${cod == '1556' or cod == '2556' or cod == '1407' or cod == '2407'}"><c:set var="texto" value="Compra para uso ou consumo" /></c:when>
            <c:when test="${cod == '1551' or cod == '2551' or cod == '1406' or cod == '2406'}"><c:set var="texto" value="Compra de bem para o ativo" /></c:when>
            <c:when test="${cod == '1202' or cod == '2202' or cod == '1411' or cod == '2411' or cod == '5202' or cod == '6202' or cod == '5411' or cod == '6411'}"><c:set var="texto" value="Devolução" /></c:when>
            <c:when test="${cod == '5915' or cod == '6915' or cod == '1916' or cod == '2916'}"><c:set var="texto" value="Remessa para conserto" /></c:when>
            <c:when test="${cod == '1949' or cod == '2949' or cod == '5949' or cod == '6949'}"><c:set var="texto" value="Outra operação" /></c:when>
            <c:when test="${cod == '5910' or cod == '6910' or cod == '1910' or cod == '2910'}"><c:set var="texto" value="Bonificação ou brinde" /></c:when>
            <c:when test="${cod == '1152' or cod == '2152' or cod == '5152' or cod == '6152' or cod == '5151' or cod == '6151'}"><c:set var="texto" value="Transferência entre lojas" /></c:when>
            <c:when test="${cod == '1904' or cod == '2904' or cod == '5904' or cod == '6904' or cod == '1917' or cod == '2917'}"><c:set var="texto" value="Remessa em consignação" /></c:when>
            <c:otherwise><c:set var="texto" value="Operação código ${cod}" /></c:otherwise>
        </c:choose>
        <span class="nfe-cfop" title="Código fiscal da operação (CFOP): ${fn:escapeXml(cod)}"><c:out value="${texto}" /><c:if test="${empty semNumero or not semNumero}"> <small class="nfe-cfop__num sh-mono">(<c:out value="${cod}" />)</small></c:if></span>
    </c:otherwise>
</c:choose>
