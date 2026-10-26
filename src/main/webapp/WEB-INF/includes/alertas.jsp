<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Mensagens de feedback (flash "mensagem" de sucesso e "erro"). Sempre escapadas com c:out. --%>
<c:if test="${not empty mensagem}">
    <div class="alert alert-success" role="status"><c:out value="${mensagem}" /></div>
</c:if>
<c:if test="${not empty erro}">
    <div class="alert alert-danger" role="alert"><c:out value="${erro}" /></div>
</c:if>
