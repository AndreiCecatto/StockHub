<%@ tag description="Telefone (só dígitos, com DDD) no formato brasileiro" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:telefoneBr valor="${cliente.telefone}" />
    11 dígitos viram (00) 00000-0000; 10 viram (00) 0000-0000; qualquer outro valor sai como veio.
    Vazio escreve um travessão. Sempre escapado com c:out.
--%>
<%@ attribute name="valor" required="false" type="java.lang.String" %>
<c:choose>
    <c:when test="${empty valor}">&mdash;</c:when>
    <c:when test="${fn:length(valor) == 11}"><span translate="no"><c:out value="(${fn:substring(valor, 0, 2)}) ${fn:substring(valor, 2, 7)}-${fn:substring(valor, 7, 11)}" /></span></c:when>
    <c:when test="${fn:length(valor) == 10}"><span translate="no"><c:out value="(${fn:substring(valor, 0, 2)}) ${fn:substring(valor, 2, 6)}-${fn:substring(valor, 6, 10)}" /></span></c:when>
    <c:otherwise><span translate="no"><c:out value="${valor}" /></span></c:otherwise>
</c:choose>
