<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Barra lateral enxuta (a partir de 992 px; recolhível para 64 px, só ícones).
    O item atual vem do atributo de request "shAtivo" (definido por tags/layout.tag a partir de ativo="...").
    O marcador (.sh-nav__marcador) só existe no item atual: na transição entre páginas ele desliza de um item ao outro
    (view-transition-name em css/layout.css). Os rótulos continuam no DOM quando recolhida (ficam visualmente ocultos);
    o atributo title mostra o nome do item ao passar o mouse sobre os ícones.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<aside class="sh-lateral" aria-label="Barra lateral">
    <a class="sh-marca" href="${ctx}/dashboard">
        <sh:marca tamanho="28" />
        <span class="sh-marca__texto">
            <span class="sh-marca__nome">StockHub</span>
        </span>
    </a>
    <nav aria-label="Menu principal">
        <div class="sh-nav__grupo">
            <ul class="sh-nav__lista">
                <li>
                    <a class="sh-nav__link" href="${ctx}/dashboard" title="Início" ${shAtivo == 'dashboard' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'dashboard'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-house-door" aria-hidden="true"></i><span class="sh-nav__rotulo">Início</span>
                    </a>
                </li>
            </ul>
        </div>
        <div class="sh-nav__grupo">
            <p class="sh-nav__titulo" id="sh-nav-operacao">Operação</p>
            <ul class="sh-nav__lista" aria-labelledby="sh-nav-operacao">
                <li>
                    <a class="sh-nav__link" href="${ctx}/estoque" title="Movimentações" ${shAtivo == 'estoque' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'estoque'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-arrow-left-right" aria-hidden="true"></i><span class="sh-nav__rotulo">Movimentações</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/produtos?tamanho=25" title="Produtos" ${shAtivo == 'produtos' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'produtos'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-box-seam" aria-hidden="true"></i><span class="sh-nav__rotulo">Produtos</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/categorias" title="Categorias" ${shAtivo == 'categorias' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'categorias'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-tags" aria-hidden="true"></i><span class="sh-nav__rotulo">Categorias</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/compras/nfe" title="Compras (NF-e)" ${shAtivo == 'compras' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'compras'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-receipt" aria-hidden="true"></i><span class="sh-nav__rotulo">Compras (NF-e)</span>
                    </a>
                </li>
            </ul>
        </div>
        <div class="sh-nav__grupo">
            <p class="sh-nav__titulo" id="sh-nav-relatorios">Alertas e relatórios</p>
            <ul class="sh-nav__lista" aria-labelledby="sh-nav-relatorios">
                <li>
                    <a class="sh-nav__link" href="${ctx}/estoque-baixo" title="Estoque baixo" ${shAtivo == 'estoqueBaixo' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'estoqueBaixo'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-exclamation-triangle" aria-hidden="true"></i><span class="sh-nav__rotulo">Estoque baixo</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/unidades" title="Unidades" ${shAtivo == 'unidades' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'unidades'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
                        <i class="bi bi-stack" aria-hidden="true"></i><span class="sh-nav__rotulo">Unidades</span>
                    </a>
                </li>
                <li>
                    <a class="sh-nav__link" href="${ctx}/valor-estoque" title="Valor do estoque" ${shAtivo == 'valorEstoque' ? 'aria-current="page"' : ''}>
                        <c:if test="${shAtivo == 'valorEstoque'}"><span class="sh-nav__marcador" aria-hidden="true"></span></c:if>
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
</aside>
