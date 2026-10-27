<%@ tag description="Data e hora de um LocalDateTime (ou Date) em dd/MM/aaaa HH:mm, dentro de <time>" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:dataHora valor="${nota.emitidaEm}" /> ou <sh:dataHora valor="${x}" soData="true" />
    LocalDateTime não é java.util.Date, então fmt:formatDate não serve: o tag corta o texto ISO (aaaa-MM-ddTHH:mm...).
    Valor nulo ou fora do formato escreve um travessão. Sempre escapado com c:out.
--%>
<%@ attribute name="valor" required="false" type="java.lang.Object" %>
<%@ attribute name="soData" required="false" type="java.lang.Boolean" %>
<%-- Corpo do c:set vira texto (toString): fn:length não aceita LocalDateTime --%>
<c:set var="iso">${valor}</c:set>
<c:choose>
    <c:when test="${not empty iso and fn:length(iso) >= 16 and fn:substring(iso, 10, 11) == 'T'}">
        <time datetime="<c:out value='${fn:substring(iso, 0, 16)}'/>"><c:out value="${fn:substring(iso, 8, 10)}/${fn:substring(iso, 5, 7)}/${fn:substring(iso, 0, 4)}" /><c:if test="${not soData}"> <c:out value="${fn:substring(iso, 11, 16)}" /></c:if></time>
    </c:when>
    <c:otherwise>&mdash;</c:otherwise>
</c:choose>
