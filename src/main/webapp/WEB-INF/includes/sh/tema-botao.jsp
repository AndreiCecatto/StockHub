<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Botão de tema compacto (menu suspenso), para as telas sem barra superior (login, cadastro, erro).
--%>
<div class="dropdown">
    <button type="button" class="btn btn-ghost" data-bs-toggle="dropdown" aria-expanded="false" id="sh-tema-botao">
        <i class="bi bi-circle-half" aria-hidden="true"></i> Tema
    </button>
    <div class="dropdown-menu dropdown-menu-end sh-menu-usuario" aria-labelledby="sh-tema-botao">
        <jsp:include page="/WEB-INF/includes/sh/tema-opcoes.jsp">
            <jsp:param name="classe" value="dropdown-item" />
        </jsp:include>
    </div>
</div>
