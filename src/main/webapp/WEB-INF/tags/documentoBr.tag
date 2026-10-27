<%@ tag description="CPF ou CNPJ (só dígitos) com a pontuação brasileira" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:documentoBr valor="${revisao.nota.emitente.documento}" />
    14 dígitos viram 00.000.000/0000-00; 11 viram 000.000.000-00; qualquer outro valor sai como veio.
    Vazio escreve um travessão. Sempre escapado com c:out.
--%>
<%@ attribute name="valor" required="false" type="java.lang.String" %>
<c:choose>
    <c:when test="${empty valor}">&mdash;</c:when>
    <c:when test="${fn:length(valor) == 14}"><span class="sh-mono" translate="no"><c:out value="${fn:substring(valor, 0, 2)}.${fn:substring(valor, 2, 5)}.${fn:substring(valor, 5, 8)}/${fn:substring(valor, 8, 12)}-${fn:substring(valor, 12, 14)}" /></span></c:when>
    <c:when test="${fn:length(valor) == 11}"><span class="sh-mono" translate="no"><c:out value="${fn:substring(valor, 0, 3)}.${fn:substring(valor, 3, 6)}.${fn:substring(valor, 6, 9)}-${fn:substring(valor, 9, 11)}" /></span></c:when>
    <c:otherwise><span class="sh-mono" translate="no"><c:out value="${valor}" /></span></c:otherwise>
</c:choose>
