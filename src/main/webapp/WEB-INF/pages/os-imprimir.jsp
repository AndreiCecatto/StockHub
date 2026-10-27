<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Página "Ordem de serviço" para imprimir (RN-90). Rota: GET /os/{id}/imprimir. Sem menus: só a folha da OS.
    Dados do OrdemServicoController: detalhe (os, pecas, servicos, subtotalPecas, subtotalServicos).
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="os" value="${detalhe.os}" />
<c:set var="shTitulo" value="Ordem de serviço ${os.numero}" scope="request" />
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <jsp:include page="/WEB-INF/includes/sh/head.jsp" />
    <link rel="stylesheet" href="${ctx}/css/os.css">
    <script defer src="${ctx}/js/os.js"></script>
</head>
<body class="sh-sem-inferior">
    <main id="conteudo" class="os-folha">
        <div class="os-folha__barra">
            <a class="btn btn-outline-secondary" href="${ctx}/os/${os.id}"><i class="bi bi-chevron-left" aria-hidden="true"></i> Voltar para a OS</a>
            <button type="button" class="btn btn-primary" data-os-imprimir><i class="bi bi-printer" aria-hidden="true"></i> Imprimir</button>
        </div>

        <header class="os-folha__cab">
            <div>
                <h1>Ordem de serviço <c:out value="${os.numero}" /></h1>
                <span class="os-sub"><c:out value="${os.usuario.nome}" /></span>
            </div>
            <div class="text-end">
                <span class="os-status os-status--${fn:toLowerCase(os.status.name())}"><c:out value="${os.status.rotulo}" /></span>
                <span class="os-sub">Aberta em <sh:dataHora valor="${os.criadaEm}" soData="true" /></span>
            </div>
        </header>

        <h2>Cliente e equipamento</h2>
        <p class="mb-1"><strong><c:out value="${os.cliente.nome}" /></strong> · <sh:documentoBr valor="${os.cliente.documento}" /></p>
        <p class="mb-1"><c:out value="${os.equipamento.descricao}" /><c:if test="${not empty os.equipamento.identificador}"> · <c:out value="${os.equipamento.identificador}" /></c:if></p>
        <c:if test="${not empty os.responsavel}"><p class="mb-1">Responsável: <c:out value="${os.responsavel.nome}" /></p></c:if>

        <h2>Defeito relatado</h2>
        <p><c:out value="${os.defeito}" /></p>

        <c:if test="${not empty detalhe.pecas}">
            <h2>Peças</h2>
            <table>
                <thead><tr><th scope="col">Peça</th><th scope="col" class="os-direita">Qtd.</th><th scope="col" class="os-direita">Preço</th><th scope="col" class="os-direita">Valor</th></tr></thead>
                <tbody>
                    <c:forEach var="p" items="${detalhe.pecas}">
                        <tr>
                            <td><c:out value="${p.produto.nome}" /></td>
                            <td class="os-direita"><c:out value="${p.quantidade}" /></td>
                            <td class="os-direita"><fmt:formatNumber value="${p.precoUnitario}" type="currency" currencySymbol="R$ " /></td>
                            <td class="os-direita"><fmt:formatNumber value="${p.subtotal}" type="currency" currencySymbol="R$ " /></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>

        <c:if test="${not empty detalhe.servicos}">
            <h2>Serviços e mão de obra</h2>
            <table>
                <thead><tr><th scope="col">Serviço</th><th scope="col" class="os-direita">Valor</th></tr></thead>
                <tbody>
                    <c:forEach var="s" items="${detalhe.servicos}">
                        <tr><td><c:out value="${s.descricao}" /></td><td class="os-direita"><fmt:formatNumber value="${s.valor}" type="currency" currencySymbol="R$ " /></td></tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>

        <dl class="os-totais">
            <div><dt>Peças</dt><dd><fmt:formatNumber value="${detalhe.subtotalPecas}" type="currency" currencySymbol="R$ " /></dd></div>
            <div><dt>Serviços</dt><dd><fmt:formatNumber value="${detalhe.subtotalServicos}" type="currency" currencySymbol="R$ " /></dd></div>
            <c:if test="${os.desconto.signum() > 0}"><div><dt>Desconto</dt><dd>- <fmt:formatNumber value="${os.desconto}" type="currency" currencySymbol="R$ " /></dd></div></c:if>
            <div class="os-totais__final"><dt>Total</dt><dd><fmt:formatNumber value="${os.total}" type="currency" currencySymbol="R$ " /></dd></div>
        </dl>

        <p><strong>Garantia:</strong> <c:out value="${os.garantiaDias}" /> dias<c:if test="${not empty os.concluidaEm}">, a partir da conclusão do serviço</c:if>.</p>
        <c:if test="${not empty os.observacoes}"><p><strong>Observações:</strong> <c:out value="${os.observacoes}" /></p></c:if>

        <div class="os-assinaturas">
            <div>Assinatura da loja</div>
            <div>Assinatura do cliente</div>
        </div>
    </main>
</body>
</html>
