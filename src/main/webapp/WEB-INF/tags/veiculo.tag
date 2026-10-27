<%@ tag description="Veículo da OS: modelo - placa (placa antiga com hífen, Mercosul sem hífen)" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:veiculo equipamento="${os.equipamento}" />   (rotulo="false" tira o prefixo "Veículo: ")
    Escreve "Veículo: modelo - placa" só quando o equipamento tem modelo ou placa; senão não escreve nada.
    Placa de 7 caracteres no formato antigo (AAA9999) sai AAA-9999; Mercosul (AAA9A99) sai sem hífen. Tudo com c:out.
--%>
<%@ attribute name="equipamento" required="false" type="java.lang.Object" %>
<%@ attribute name="rotulo" required="false" type="java.lang.Boolean" %>
<c:set var="bruta" value="${fn:toUpperCase(fn:replace(equipamento.placa, '-', ''))}" />
<c:choose>
    <c:when test="${fn:length(bruta) == 7 and fn:contains('0123456789', fn:substring(bruta, 4, 5))}"><c:set var="placaFmt" value="${fn:substring(bruta, 0, 3)}-${fn:substring(bruta, 3, 7)}" /></c:when>
    <c:otherwise><c:set var="placaFmt" value="${bruta}" /></c:otherwise>
</c:choose>
<c:if test="${not empty equipamento.modelo or not empty bruta}">
    <span class="sh-veiculo"><c:if test="${rotulo != false}">Veículo: </c:if><c:if test="${not empty equipamento.modelo}"><c:out value="${equipamento.modelo}" /></c:if><c:if test="${not empty equipamento.modelo and not empty bruta}"> - </c:if><c:if test="${not empty bruta}"><span class="sh-mono" translate="no"><c:out value="${placaFmt}" /></span></c:if></span>
</c:if>
