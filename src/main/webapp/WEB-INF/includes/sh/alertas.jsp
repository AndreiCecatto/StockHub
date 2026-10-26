<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Mensagens do servidor (flash "mensagem" de sucesso e "erro"), no HTML: funciona sem JavaScript.
    Com JS, js/toast.js transforma a de sucesso (data-sh-toast) em toast; o erro fica aqui, persistente.
    Quando a tela também recebe "errosCampos", o erro de cada campo aparece junto ao campo (e a própria
    tela monta o resumo), então este alerta com as mensagens juntas é omitido para não repetir.
    Sempre escapado com c:out.
--%>
<c:if test="${not empty mensagem}">
    <div class="sh-alert sh-alert--success" role="status" data-sh-toast>
        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
        <div class="sh-alert__corpo"><c:out value="${mensagem}" /></div>
    </div>
</c:if>
<c:if test="${not empty erro and empty errosCampos}">
    <div class="sh-alert sh-alert--danger" role="alert" tabindex="-1">
        <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
        <div class="sh-alert__corpo"><c:out value="${erro}" /></div>
    </div>
</c:if>
