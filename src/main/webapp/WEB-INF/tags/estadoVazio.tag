<%@ tag description="Estado vazio: ícone, título curto, uma frase e um botão de ação" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:estadoVazio icone="box-seam" titulo="Nenhum produto ainda"
             texto="Cadastre seu primeiro produto para começar." acaoUrl="/produtos/novo" acaoRotulo="Novo produto" />
    icone: nome do Bootstrap Icons sem o prefixo "bi-" (padrão: inbox).
    acaoUrl: caminho SEM o contexto da aplicação (o tag acrescenta). Sem acaoUrl, não mostra botão.
    tom: "sucesso" para o vazio positivo ("Tudo em dia"); padrão neutro.
--%>
<%@ attribute name="titulo" required="true" type="java.lang.String" %>
<%@ attribute name="texto" required="false" type="java.lang.String" %>
<%@ attribute name="icone" required="false" type="java.lang.String" %>
<%@ attribute name="acaoUrl" required="false" type="java.lang.String" %>
<%@ attribute name="acaoRotulo" required="false" type="java.lang.String" %>
<%@ attribute name="tom" required="false" type="java.lang.String" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<div class="sh-vazio ${tom == 'sucesso' ? 'sh-vazio--sucesso' : ''}">
    <i class="bi bi-<c:out value='${empty icone ? "inbox" : icone}'/> sh-vazio__icone" aria-hidden="true"></i>
    <p class="sh-vazio__titulo"><c:out value="${titulo}" /></p>
    <c:if test="${not empty texto}">
        <p class="sh-vazio__texto"><c:out value="${texto}" /></p>
    </c:if>
    <c:if test="${not empty acaoUrl}">
        <a class="btn btn-primary" href="<c:out value='${ctx}${acaoUrl}'/>"><c:out value="${acaoRotulo}" /></a>
    </c:if>
</div>
