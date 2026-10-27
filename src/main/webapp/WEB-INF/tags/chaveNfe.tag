<%@ tag description="Chave de acesso da NF-e (44 dígitos) em grupos de 4, para ler e conferir" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Uso: <sh:chaveNfe valor="${nota.chaveAcesso}" />
    Escreve <span class="sh-mono" translate="no"> com grupos separados por espaço (quebram de linha no celular).
    Se não tiver 44 caracteres, escreve o texto como veio. Sempre escapado com c:out.
--%>
<%@ attribute name="valor" required="false" type="java.lang.String" %>
<span class="sh-mono nfe-chave" translate="no"><c:choose>
    <c:when test="${fn:length(valor) == 44}"><c:forEach var="i" begin="0" end="10"><c:out value="${fn:substring(valor, i * 4, i * 4 + 4)}" /><c:if test="${i < 10}"> </c:if></c:forEach></c:when>
    <c:otherwise><c:out value="${valor}" /></c:otherwise>
</c:choose></span>
