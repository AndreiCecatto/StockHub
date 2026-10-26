<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Painel "Mais" do celular (offcanvas inferior do Bootstrap): Categorias, Unidades, Valor do estoque,
    tema, atalhos de teclado e Sair (POST com token CSRF).
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<div class="offcanvas offcanvas-bottom sh-mais" tabindex="-1" id="sh-mais" aria-labelledby="sh-mais-titulo">
    <div class="offcanvas-header">
        <h2 class="offcanvas-title" id="sh-mais-titulo">Mais opções</h2>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Fechar"></button>
    </div>
    <div class="offcanvas-body pt-0">
        <p class="sh-mais__titulo mt-0">Loja: <strong><c:out value="${usuarioLogado.nome}" /></strong></p>
        <nav aria-label="Mais opções">
            <ul class="sh-mais__lista">
                <li>
                    <a class="sh-mais__link" href="${ctx}/categorias" ${shAtivo == 'categorias' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-tags" aria-hidden="true"></i> Categorias
                    </a>
                </li>
                <li>
                    <a class="sh-mais__link" href="${ctx}/unidades" ${shAtivo == 'unidades' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-stack" aria-hidden="true"></i> Unidades
                    </a>
                </li>
                <li>
                    <a class="sh-mais__link" href="${ctx}/valor-estoque" ${shAtivo == 'valorEstoque' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-cash-coin" aria-hidden="true"></i> Valor do estoque
                    </a>
                </li>
            </ul>
        </nav>
        <p class="sh-mais__titulo" id="sh-mais-tema">Tema</p>
        <div role="group" aria-labelledby="sh-mais-tema">
            <jsp:include page="/WEB-INF/includes/sh/tema-opcoes.jsp">
                <jsp:param name="classe" value="sh-mais__link" />
            </jsp:include>
        </div>
        <ul class="sh-mais__lista mt-3">
            <li>
                <button type="button" class="sh-mais__link" data-sh-abrir-atalhos>
                    <i class="bi bi-keyboard" aria-hidden="true"></i> Atalhos de teclado
                </button>
            </li>
            <li>
                <form method="post" action="${ctx}/logout" class="m-0">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                    <button type="submit" class="sh-mais__link">
                        <i class="bi bi-box-arrow-right" aria-hidden="true"></i> Sair
                    </button>
                </form>
            </li>
        </ul>
    </div>
</div>
