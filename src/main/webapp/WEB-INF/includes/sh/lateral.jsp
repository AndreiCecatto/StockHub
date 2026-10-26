<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Barra lateral escura (a partir de 992 px; recolhível para 68 px, só ícones).
    O item atual vem do atributo de request "shAtivo" (definido por tags/layout.tag a partir de ativo="...").
    Os rótulos continuam no DOM quando recolhida (ficam visualmente ocultos pelo CSS).
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<div class="sh-lateral">
    <a class="sh-marca" href="${ctx}/dashboard">
        <span class="sh-marca__logo"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
        <span class="sh-marca__nome">StockHub</span>
    </a>
    <nav aria-label="Menu principal">
        <div class="sh-nav__grupo">
            <ul class="sh-nav__lista">
                <li>
                    <a class="sh-nav__link" href="${ctx}/dashboard" ${shAtivo == 'dashboard' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-house-door" aria-hidden="true"></i><span class="sh-nav__rotulo">Início</span>
                    </a>
                </li>
            </ul>
        </div>
        <div class="sh-nav__grupo">
            <p class="sh-nav__titulo" id="sh-nav-operacao">Operação</p>
            <ul class="sh-nav__lista" aria-labelledby="sh-nav-operacao">
                <li>
                    <a class="sh-nav__link" href="${ctx}/estoque" ${shAtivo == 'estoque' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-arrow-left-right" aria-hidden="true"></i><span class="sh-nav__rotulo">Movimentações</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/produtos?tamanho=25" ${shAtivo == 'produtos' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-box-seam" aria-hidden="true"></i><span class="sh-nav__rotulo">Produtos</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/categorias" ${shAtivo == 'categorias' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-tags" aria-hidden="true"></i><span class="sh-nav__rotulo">Categorias</span>
                    </a>
                </li>
            </ul>
        </div>
        <div class="sh-nav__grupo">
            <p class="sh-nav__titulo" id="sh-nav-relatorios">Alertas e relatórios</p>
            <ul class="sh-nav__lista" aria-labelledby="sh-nav-relatorios">
                <li>
                    <a class="sh-nav__link" href="${ctx}/estoque-baixo" ${shAtivo == 'estoqueBaixo' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-exclamation-triangle" aria-hidden="true"></i><span class="sh-nav__rotulo">Estoque baixo</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/unidades" ${shAtivo == 'unidades' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-stack" aria-hidden="true"></i><span class="sh-nav__rotulo">Unidades</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/valor-estoque" ${shAtivo == 'valorEstoque' ? 'aria-current="page"' : ''}>
                        <i class="bi bi-cash-coin" aria-hidden="true"></i><span class="sh-nav__rotulo">Valor do estoque</span>
                    </a>
                </li>
            </ul>
        </div>
    </nav>
    <div class="sh-lateral__rodape">
        <button type="button" class="sh-lateral__alternar" data-sh-alternar-lateral>
            <i class="bi bi-chevron-double-left" aria-hidden="true"></i>
            <span class="sh-lateral__alternar-rotulo">Recolher menu</span>
        </button>
    </div>
</div>
