<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Barra inferior do celular e do tablet (abaixo de 992 px): Início, Produtos, Movimentar (central), Alertas e Mais.
    "Mais" abre o painel #sh-mais (offcanvas). O item atual vem de "shAtivo".
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<nav class="sh-inferior" aria-label="Menu principal">
    <a class="sh-inferior__item" href="${ctx}/dashboard" ${shAtivo == 'dashboard' ? 'aria-current="page"' : ''}>
        <i class="bi bi-house-door" aria-hidden="true"></i><span>Início</span>
    </a>
    <a class="sh-inferior__item" href="${ctx}/produtos?tamanho=25" ${shAtivo == 'produtos' ? 'aria-current="page"' : ''}>
        <i class="bi bi-box-seam" aria-hidden="true"></i><span>Produtos</span>
    </a>
    <a class="sh-inferior__item sh-inferior__item--principal" href="${ctx}/estoque" ${shAtivo == 'estoque' ? 'aria-current="page"' : ''}>
        <span class="sh-inferior__icone"><i class="bi bi-arrow-left-right" aria-hidden="true"></i></span>
        <span>Movimentar</span>
    </a>
    <a class="sh-inferior__item" href="${ctx}/estoque-baixo" ${shAtivo == 'estoqueBaixo' ? 'aria-current="page"' : ''}>
        <i class="bi bi-exclamation-triangle" aria-hidden="true"></i><span>Alertas</span>
    </a>
    <button type="button" class="sh-inferior__item" data-bs-toggle="offcanvas" data-bs-target="#sh-mais"
        aria-controls="sh-mais">
        <i class="bi bi-three-dots" aria-hidden="true"></i><span>Mais</span>
    </button>
</nav>
