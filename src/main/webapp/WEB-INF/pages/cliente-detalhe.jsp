<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Detalhe do cliente (RN-98). Rotas: GET /clientes/{id}; POST /clientes/{id}/inativar e /reativar.
    Dados do ClienteController: cliente (Parceiro), resumo (ClienteResumo: totalOs, osEmAndamento, osEntregues,
    totalEntregue, ultimaVisita, ultimas), temOs (módulo OS ligado).
    O resumo mostra só o que o modelo guarda: as ordens de serviço. A venda de balcão não registra o cliente.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="csrfNome"><c:out value="${_csrf.parameterName}" /></c:set>
<c:set var="csrfToken"><c:out value="${_csrf.token}" /></c:set>
<c:set var="nasc">${cliente.dataNascimento}</c:set>
<c:set var="temEndereco" value="${not empty cliente.logradouro or not empty cliente.bairro or not empty cliente.cidade or not empty cliente.cep}" />

<sh:layout titulo="${cliente.nome}" ativo="clientes" largura="form" voltarUrl="/clientes" voltarRotulo="Clientes">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/membros.css">
        <link rel="stylesheet" href="${ctx}/css/os.css">
        <link rel="stylesheet" href="${ctx}/css/clientes.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="${cliente.nome}" descricao="${not empty cliente.nomeFantasia ? cliente.nomeFantasia : 'Cadastro do cliente'}">
            <a class="btn btn-outline-secondary" href="${ctx}/clientes/${cliente.id}/editar"><i class="bi bi-pencil" aria-hidden="true"></i> Editar</a>
            <c:choose>
                <c:when test="${cliente.ativo}">
                    <form method="post" action="${ctx}/clientes/${cliente.id}/inativar" class="d-inline" data-sh-enviando="Salvando…">
                        <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                        <button type="submit" class="btn btn-outline-secondary">Inativar</button>
                    </form>
                    <c:if test="${temOs}"><a class="btn btn-primary" href="${ctx}/os/nova"><i class="bi bi-plus-lg" aria-hidden="true"></i> Nova ordem de serviço</a></c:if>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/clientes/${cliente.id}/reativar" class="d-inline" data-sh-enviando="Salvando…">
                        <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                        <button type="submit" class="btn btn-primary">Reativar cliente</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </sh:cabecalhoPagina>

        <c:if test="${not cliente.ativo}">
            <div class="sh-alert sh-alert--warning" role="status">
                <i class="bi bi-pause-circle-fill" aria-hidden="true"></i>
                <div class="sh-alert__corpo">Este cliente está inativo: não aparece na escolha de uma nova ordem de serviço. O histórico continua guardado.</div>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${temOs}">
                <h2 class="visually-hidden" id="cli-resumo">Resumo do cliente</h2>
                <ul class="sh-metricas cli-metricas" aria-labelledby="cli-resumo">
                    <li class="sh-metric">
                        <span class="sh-metric__topo"><span class="sh-metric__rotulo">Ordens de serviço</span><span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-wrench-adjustable"></i></span></span>
                        <span class="sh-metric__valor"><c:out value="${resumo.totalOs}" /></span>
                        <span class="sh-metric__apoio">última visita: <sh:dataHora valor="${resumo.ultimaVisita}" soData="true" /></span>
                    </li>
                    <li class="sh-metric">
                        <span class="sh-metric__topo"><span class="sh-metric__rotulo">Em andamento</span><span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-hourglass-split"></i></span></span>
                        <span class="sh-metric__valor"><c:out value="${resumo.osEmAndamento}" /></span>
                        <span class="sh-metric__apoio">ainda abertas</span>
                    </li>
                    <li class="sh-metric">
                        <span class="sh-metric__topo"><span class="sh-metric__rotulo">Entregues</span><span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-check2-circle"></i></span></span>
                        <span class="sh-metric__valor"><c:out value="${resumo.osEntregues}" /></span>
                        <span class="sh-metric__apoio">já concluídas</span>
                    </li>
                    <li class="sh-metric">
                        <span class="sh-metric__topo"><span class="sh-metric__rotulo">Total das entregues</span><span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-cash-coin"></i></span></span>
                        <span class="sh-metric__valor"><fmt:formatNumber value="${resumo.totalEntregue}" type="currency" currencySymbol="R$ " /></span>
                        <span class="sh-metric__apoio">só ordens de serviço</span>
                    </li>
                </ul>
                <p class="cli-nota cli-nota--solta">Conta só as ordens de serviço. A venda de balcão não guarda o cliente, por isso ainda não entra aqui.</p>
            </c:when>
            <c:otherwise>
                <p class="cli-faixa" role="note">
                    <i class="bi bi-info-circle" aria-hidden="true"></i>
                    <span>O histórico de compras ainda não é ligado ao cliente: a venda de balcão não registra quem comprou.</span>
                </p>
            </c:otherwise>
        </c:choose>

        <section class="sh-card sh-secao" aria-labelledby="cli-dados">
            <div class="sh-card__cab cli-cab"><h2 class="sh-card__titulo" id="cli-dados">Dados do cadastro</h2><c:choose><c:when test="${cliente.ativo}"><span class="sh-badge sh-badge--normal">Ativo</span></c:when><c:otherwise><span class="sh-badge sh-badge--sem">Inativo</span></c:otherwise></c:choose></div>
            <div class="sh-card__corpo">
                <dl class="cli-dados">
                    <div><dt>Tipo</dt><dd><c:out value="${cliente.tipoPessoa == 'FISICA' ? 'Pessoa física' : (cliente.tipoPessoa == 'JURIDICA' ? 'Empresa' : '—')}" /></dd></div>
                    <div><dt>CPF ou CNPJ</dt><dd><sh:documentoBr valor="${cliente.documento}" /></dd></div>
                    <div><dt>Telefone</dt><dd><sh:telefoneBr valor="${cliente.telefone}" /></dd></div>
                    <div><dt>WhatsApp</dt><dd><sh:telefoneBr valor="${cliente.whatsapp}" /></dd></div>
                    <div><dt>E-mail</dt><dd><c:choose><c:when test="${empty cliente.email}">&mdash;</c:when><c:otherwise><a class="cli-email" href="mailto:<c:out value='${cliente.email}'/>"><c:out value="${cliente.email}" /></a></c:otherwise></c:choose></dd></div>
                    <div><dt>Nascimento ou fundação</dt><dd>
                        <c:choose>
                            <c:when test="${fn:length(nasc) == 10}"><c:out value="${fn:substring(nasc, 8, 10)}/${fn:substring(nasc, 5, 7)}/${fn:substring(nasc, 0, 4)}" /></c:when>
                            <c:otherwise>&mdash;</c:otherwise>
                        </c:choose></dd></div>
                    <div><dt>Como chegou</dt><dd><c:out value="${empty cliente.origem ? '—' : cliente.origem}" /></dd></div>
                    <div><dt>Cliente desde</dt><dd><sh:dataHora valor="${cliente.criadoEm}" soData="true" /></dd></div>
                    <div class="cli-dados__largo"><dt>Endereço</dt><dd>
                        <c:choose>
                            <c:when test="${temEndereco}">
                                <c:out value="${cliente.logradouro}" /><c:if test="${not empty cliente.numero}">, <c:out value="${cliente.numero}" /></c:if><c:if test="${not empty cliente.complemento}"> (<c:out value="${cliente.complemento}" />)</c:if><c:if test="${not empty cliente.bairro}"> &middot; <c:out value="${cliente.bairro}" /></c:if><br>
                                <c:out value="${cliente.cidade}" /><c:if test="${not empty cliente.uf}"><c:if test="${not empty cliente.cidade}">/</c:if><c:out value="${cliente.uf}" /></c:if><c:if test="${not empty cliente.cep}"> &middot; CEP <c:out value="${fn:substring(cliente.cep, 0, 5)}-${fn:substring(cliente.cep, 5, 8)}" /></c:if>
                            </c:when>
                            <c:otherwise>&mdash;</c:otherwise>
                        </c:choose></dd></div>
                    <c:if test="${not empty cliente.observacoes}">
                        <div class="cli-dados__largo"><dt>Observações internas</dt><dd class="cli-obs"><c:out value="${cliente.observacoes}" /></dd></div>
                    </c:if>
                </dl>
            </div>
        </section>

        <c:if test="${temOs}">
            <section class="sh-card sh-secao" aria-labelledby="cli-os">
                <div class="sh-card__cab"><h2 class="sh-card__titulo" id="cli-os">Últimas ordens de serviço</h2></div>
                <c:choose>
                    <c:when test="${empty resumo.ultimas}">
                        <p class="mb-nota">Este cliente ainda não tem ordem de serviço.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="sh-table-wrap">
                            <table class="sh-table sh-table--cards" role="table">
                                <caption class="visually-hidden">Ordens de serviço mais recentes deste cliente</caption>
                                <thead role="rowgroup">
                                    <tr role="row">
                                        <th scope="col" role="columnheader">Número</th>
                                        <th scope="col" role="columnheader">Equipamento</th>
                                        <th scope="col" role="columnheader">Situação</th>
                                        <th scope="col" role="columnheader">Aberta em</th>
                                        <th scope="col" role="columnheader" class="text-end">Total</th>
                                    </tr>
                                </thead>
                                <tbody role="rowgroup">
                                    <c:forEach var="o" items="${resumo.ultimas}">
                                        <tr role="row">
                                            <td role="cell"><a class="os-numero" href="${ctx}/os/${o.id}">OS <c:out value="${o.numero}" /></a></td>
                                            <td role="cell" data-label="Equipamento"><c:out value="${o.equipamento.descricao}" /><c:if test="${not empty o.equipamento.placa or not empty o.equipamento.modelo}"><span class="os-sub d-block"><sh:veiculo equipamento="${o.equipamento}" /></span></c:if></td>
                                            <td role="cell" data-label="Situação"><span class="os-status os-status--${fn:toLowerCase(o.status.name())}"><c:out value="${o.status.rotulo}" /></span></td>
                                            <td role="cell" data-label="Aberta em"><sh:dataHora valor="${o.criadaEm}" soData="true" /></td>
                                            <td role="cell" data-label="Total" class="text-end os-dinheiro"><fmt:formatNumber value="${o.total}" type="currency" currencySymbol="R$ " /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                        <c:if test="${resumo.totalOs > fn:length(resumo.ultimas)}">
                            <p class="mb-nota">Mostrando as <c:out value="${fn:length(resumo.ultimas)}" /> mais recentes de <c:out value="${resumo.totalOs}" />. A lista completa está em <a href="${ctx}/os">Ordens de serviço</a>.</p>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:if>
    </jsp:body>
</sh:layout>
