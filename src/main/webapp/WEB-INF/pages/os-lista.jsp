<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Lista de ordens de serviço (RN-77). Rota: GET /os?status=. Só lojas com o módulo OS (404 nas demais).
    Dados do OrdemServicoController: ordens (OrdemServico), contagens (mapa StatusOs -> total), filtro (StatusOs ou
    vazio), todosStatus.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layout titulo="Ordens de serviço" ativo="os">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/os.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Ordens de serviço" descricao="Do orçamento à entrega, com as peças saindo do estoque na hora certa.">
            <a class="btn btn-primary" href="${ctx}/os/nova"><i class="bi bi-plus-lg" aria-hidden="true"></i> Nova ordem de serviço</a>
        </sh:cabecalhoPagina>

        <section class="sh-card sh-secao" aria-labelledby="titulo-os">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-os">Suas ordens</h2>
            </div>
            <nav class="os-filtro" aria-label="Filtrar por situação">
                <a class="sh-chip" href="${ctx}/os" ${empty filtro ? 'aria-current="page"' : ''}>Todas</a>
                <c:forEach var="s" items="${todosStatus}">
                    <a class="sh-chip" href="${ctx}/os?status=${s.name()}" ${filtro == s ? 'aria-current="page"' : ''}>
                        <c:out value="${s.rotulo}" /> <span class="os-filtro__contagem"><c:out value="${contagens[s]}" /></span>
                    </a>
                </c:forEach>
            </nav>
            <c:choose>
                <c:when test="${empty ordens}">
                    <p class="os-nota">
                        <c:choose>
                            <c:when test="${empty filtro}">Nenhuma ordem de serviço ainda. Crie a primeira com o botão acima.</c:when>
                            <c:otherwise>Nenhuma ordem com a situação "<c:out value="${filtro.rotulo}" />".</c:otherwise>
                        </c:choose>
                    </p>
                </c:when>
                <c:otherwise>
                    <div class="sh-table-wrap">
                        <table class="sh-table sh-table--cards" role="table">
                            <caption class="visually-hidden">Ordens de serviço, <c:out value="${fn:length(ordens)}" /> na lista</caption>
                            <thead role="rowgroup">
                                <tr role="row">
                                    <th scope="col" role="columnheader">Número</th>
                                    <th scope="col" role="columnheader">Cliente</th>
                                    <th scope="col" role="columnheader">Situação</th>
                                    <th scope="col" role="columnheader">Responsável</th>
                                    <th scope="col" role="columnheader">Aberta em</th>
                                    <th scope="col" role="columnheader" class="text-end">Total</th>
                                </tr>
                            </thead>
                            <tbody role="rowgroup">
                                <c:forEach var="o" items="${ordens}">
                                    <tr role="row">
                                        <td role="cell"><a class="os-numero" href="${ctx}/os/${o.id}">OS <c:out value="${o.numero}" /></a></td>
                                        <td role="cell" data-label="Cliente">
                                            <c:out value="${o.cliente.nome}" />
                                            <span class="os-sub"><c:out value="${o.equipamento.descricao}" /><c:if test="${not empty o.equipamento.identificador}"> (<c:out value="${o.equipamento.identificador}" />)</c:if></span>
                                        </td>
                                        <td role="cell" data-label="Situação"><span class="os-status os-status--${fn:toLowerCase(o.status.name())}"><c:out value="${o.status.rotulo}" /></span></td>
                                        <td role="cell" data-label="Responsável"><c:out value="${empty o.responsavel ? '—' : o.responsavel.nome}" /></td>
                                        <td role="cell" data-label="Aberta em"><sh:dataHora valor="${o.criadaEm}" soData="true" /></td>
                                        <td role="cell" data-label="Total" class="text-end os-dinheiro"><fmt:formatNumber value="${o.total}" type="currency" currencySymbol="R$ " /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
