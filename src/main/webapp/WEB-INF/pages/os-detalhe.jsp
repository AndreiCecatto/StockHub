<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Detalhe da ordem de serviço (RN-77 a RN-90). Rotas: GET /os/{id}; POST /os/{id}/dados, /pecas, /servicos,
    /pecas/{id}/remover, /servicos/{id}/remover, /avancar, /cancelar.
    Dados do OrdemServicoController: detalhe (os, pecas, servicos, subtotalPecas, subtotalServicos, subtotal()),
    responsaveis, produtos (só quando as peças ainda podem mudar). Papel: podeGerenciar (dono ou gerente).
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="os" value="${detalhe.os}" />
<c:set var="csrfNome"><c:out value="${_csrf.parameterName}" /></c:set>
<c:set var="csrfToken"><c:out value="${_csrf.token}" /></c:set>

<sh:layout titulo="OS ${os.numero}" ativo="os" largura="form" voltarUrl="/os" voltarRotulo="Ordens de serviço">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/os.css">
        <script defer src="${ctx}/js/os.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="OS ${os.numero}" descricao="${os.cliente.nome} · ${os.equipamento.descricao}">
            <a class="btn btn-outline-secondary" href="${ctx}/os/${os.id}/imprimir" target="_blank" rel="noopener">
                <i class="bi bi-printer" aria-hidden="true"></i> Imprimir
            </a>
            <c:if test="${os.status.cancelavel() and (os.status != 'CONCLUIDA' or podeGerenciar)}">
                <form method="post" action="${ctx}/os/${os.id}/cancelar" class="d-inline"
                      data-os-confirmar="Cancelar a OS ${os.numero}? As peças reservadas voltam ao estoque disponível.">
                    <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                    <button type="submit" class="btn btn-outline-danger">Cancelar OS</button>
                </form>
            </c:if>
            <c:if test="${not empty os.status.proximo()}">
                <form method="post" action="${ctx}/os/${os.id}/avancar" class="d-inline" data-sh-enviando="Salvando…">
                    <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                    <button type="submit" class="btn btn-primary"><c:out value="${os.status.acaoProximo}" /></button>
                </form>
            </c:if>
        </sh:cabecalhoPagina>

        <section class="sh-card sh-secao" aria-labelledby="titulo-andamento">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-andamento">Andamento
                    <span class="os-status os-status--${fn:toLowerCase(os.status.name())}"><c:out value="${os.status.rotulo}" /></span>
                </h2>
            </div>
            <ol class="os-passos">
                <li class="os-passo os-passo--feito"><span class="os-passo__nome">Orçamento</span><span class="os-passo__data"><sh:dataHora valor="${os.criadaEm}" /></span></li>
                <li class="os-passo ${not empty os.aprovadaEm ? 'os-passo--feito' : ''} ${os.status == 'ORCAMENTO' ? 'os-passo--atual' : ''}" ${os.status == 'ORCAMENTO' ? 'aria-current="step"' : ''}><span class="os-passo__nome">Aprovada</span><span class="os-passo__data"><sh:dataHora valor="${os.aprovadaEm}" /></span></li>
                <li class="os-passo ${not empty os.iniciadaEm ? 'os-passo--feito' : ''} ${os.status == 'APROVADA' ? 'os-passo--atual' : ''}" ${os.status == 'APROVADA' ? 'aria-current="step"' : ''}><span class="os-passo__nome">Em execução</span><span class="os-passo__data"><sh:dataHora valor="${os.iniciadaEm}" /></span></li>
                <li class="os-passo ${not empty os.concluidaEm ? 'os-passo--feito' : ''} ${os.status == 'EM_EXECUCAO' ? 'os-passo--atual' : ''}" ${os.status == 'EM_EXECUCAO' ? 'aria-current="step"' : ''}><span class="os-passo__nome">Concluída</span><span class="os-passo__data"><sh:dataHora valor="${os.concluidaEm}" /></span></li>
                <li class="os-passo ${not empty os.entregueEm ? 'os-passo--feito' : ''} ${os.status == 'CONCLUIDA' ? 'os-passo--atual' : ''}" ${os.status == 'CONCLUIDA' ? 'aria-current="step"' : ''}><span class="os-passo__nome">Entregue</span><span class="os-passo__data"><sh:dataHora valor="${os.entregueEm}" /></span></li>
            </ol>
            <c:if test="${os.status == 'CANCELADA'}">
                <div class="sh-alert sh-alert--danger os-cancelada" role="status">
                    <i class="bi bi-x-octagon-fill" aria-hidden="true"></i>
                    <div class="sh-alert__corpo">Esta ordem foi cancelada em <sh:dataHora valor="${os.canceladaEm}" />. As peças voltaram ao estoque.</div>
                </div>
            </c:if>
        </section>

        <section class="sh-card sh-secao" aria-labelledby="titulo-atendimento">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-atendimento">Atendimento</h2>
            </div>
            <c:choose>
                <c:when test="${os.status.editavel()}">
                    <div class="sh-card__corpo">
                        <form method="post" action="${ctx}/os/${os.id}/dados" data-sh-enviando="Salvando…">
                            <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                            <p class="os-sub mb-3">
                                <strong><c:out value="${os.cliente.nome}" /></strong> ·
                                <c:out value="${os.equipamento.descricao}" /><c:if test="${not empty os.equipamento.identificador}"> (<c:out value="${os.equipamento.identificador}" />)</c:if>
                            </p>
                            <div class="row g-3">
                                <div class="col-12">
                                    <label class="form-label" for="defeito">Defeito relatado</label>
                                    <textarea id="defeito" class="form-control" name="defeito" rows="2" maxlength="1000" required><c:out value="${os.defeito}" /></textarea>
                                </div>
                                <div class="col-12 col-md-5">
                                    <label class="form-label" for="responsavelId">Responsável</label>
                                    <select id="responsavelId" class="form-select" name="responsavelId">
                                        <c:forEach var="m" items="${responsaveis}">
                                            <option value="${m.id}" ${m.id == os.responsavel.id ? 'selected' : ''}><c:out value="${m.nome}" /></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-6 col-md-3">
                                    <label class="form-label" for="garantiaDias">Garantia (dias)</label>
                                    <input id="garantiaDias" class="form-control" type="number" name="garantiaDias" min="0" max="3650" value="${os.garantiaDias}">
                                </div>
                                <div class="col-6 col-md-4">
                                    <label class="form-label" for="desconto">Desconto (R$)</label>
                                    <input id="desconto" class="form-control" type="text" inputmode="decimal" name="desconto" autocomplete="off"
                                        value="<fmt:formatNumber value='${os.desconto}' pattern='0.00' />">
                                    <c:if test="${not podeGerenciar}"><div class="form-text">Você pode dar até 10% de desconto.</div></c:if>
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="observacoes">Observações</label>
                                    <input id="observacoes" class="form-control" type="text" name="observacoes" maxlength="1000" autocomplete="off" value="<c:out value='${os.observacoes}'/>">
                                </div>
                            </div>
                            <div class="sh-form-actions mb-acoes-form">
                                <button class="btn btn-outline-secondary" type="submit">Salvar dados</button>
                            </div>
                        </form>
                    </div>
                </c:when>
                <c:otherwise>
                    <dl class="os-dados">
                        <div><dt>Cliente</dt><dd><c:out value="${os.cliente.nome}" /></dd></div>
                        <div><dt>Equipamento</dt><dd><c:out value="${os.equipamento.descricao}" /><c:if test="${not empty os.equipamento.identificador}"> (<c:out value="${os.equipamento.identificador}" />)</c:if></dd></div>
                        <div><dt>Responsável</dt><dd><c:out value="${empty os.responsavel ? '—' : os.responsavel.nome}" /></dd></div>
                        <div><dt>Garantia</dt><dd><c:out value="${os.garantiaDias}" /> dias</dd></div>
                        <div class="os-dados__largo"><dt>Defeito relatado</dt><dd><c:out value="${os.defeito}" /></dd></div>
                        <c:if test="${not empty os.observacoes}"><div class="os-dados__largo"><dt>Observações</dt><dd><c:out value="${os.observacoes}" /></dd></div></c:if>
                    </dl>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="sh-card sh-secao" aria-labelledby="titulo-pecas">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-pecas">Peças</h2>
            </div>
            <c:choose>
                <c:when test="${empty detalhe.pecas}">
                    <p class="os-nota">Nenhuma peça nesta ordem.</p>
                </c:when>
                <c:otherwise>
                    <div class="sh-table-wrap">
                        <table class="sh-table sh-table--cards" role="table">
                            <caption class="visually-hidden">Peças da OS <c:out value="${os.numero}" /></caption>
                            <thead role="rowgroup">
                                <tr role="row">
                                    <th scope="col" role="columnheader">Peça</th>
                                    <th scope="col" role="columnheader" class="text-end">Qtd.</th>
                                    <th scope="col" role="columnheader" class="text-end">Preço</th>
                                    <th scope="col" role="columnheader" class="text-end">Valor</th>
                                    <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                                </tr>
                            </thead>
                            <tbody role="rowgroup">
                                <c:forEach var="p" items="${detalhe.pecas}">
                                    <tr role="row">
                                        <td role="cell">
                                            <c:out value="${p.produto.nome}" />
                                            <c:if test="${os.status == 'APROVADA' and not empty p.reservaId}"><span class="os-sub">Reservada no estoque</span></c:if>
                                        </td>
                                        <td role="cell" data-label="Qtd." class="text-end"><c:out value="${p.quantidade}" /></td>
                                        <td role="cell" data-label="Preço" class="text-end os-dinheiro"><fmt:formatNumber value="${p.precoUnitario}" type="currency" currencySymbol="R$ " /></td>
                                        <td role="cell" data-label="Valor" class="text-end os-dinheiro"><fmt:formatNumber value="${p.subtotal}" type="currency" currencySymbol="R$ " /></td>
                                        <td role="cell" data-label="" class="os-acoes-celula">
                                            <c:if test="${os.status.podeTrocarPecas()}">
                                                <form method="post" action="${ctx}/os/${os.id}/pecas/${p.id}/remover">
                                                    <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                                                    <button type="submit" class="btn btn-outline-secondary btn-sm">Retirar<span class="visually-hidden"> <c:out value="${p.produto.nome}" /></span></button>
                                                </form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
            <c:if test="${os.status.podeTrocarPecas()}">
                <form method="post" action="${ctx}/os/${os.id}/pecas" class="os-linha-form" data-sh-enviando="Incluindo…">
                    <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                    <div class="row g-3 align-items-end">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="produtoId">Incluir peça</label>
                            <select id="produtoId" class="form-select" name="produtoId" required>
                                <option value="">Escolha um produto</option>
                                <c:forEach var="pr" items="${produtos}">
                                    <option value="${pr.id}"><c:out value="${pr.nome}" /> (<c:out value="${pr.quantidade}" /> em estoque)</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-4 col-md-2">
                            <label class="form-label" for="quantidade">Qtd.</label>
                            <input id="quantidade" class="form-control" type="number" name="quantidade" min="1" value="1" required>
                        </div>
                        <div class="col-8 col-md-2">
                            <label class="form-label" for="preco">Preço (R$)</label>
                            <input id="preco" class="form-control" type="text" inputmode="decimal" name="preco" autocomplete="off" placeholder="do cadastro">
                        </div>
                        <div class="col-12 col-md-2">
                            <button class="btn btn-outline-secondary w-100" type="submit">Incluir</button>
                        </div>
                    </div>
                    <c:if test="${os.status == 'APROVADA'}"><div class="form-text">A peça incluída agora já fica reservada no estoque.</div></c:if>
                </form>
            </c:if>
        </section>

        <section class="sh-card sh-secao" aria-labelledby="titulo-servicos">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-servicos">Serviços e mão de obra</h2>
            </div>
            <c:choose>
                <c:when test="${empty detalhe.servicos}">
                    <p class="os-nota">Nenhum serviço nesta ordem.</p>
                </c:when>
                <c:otherwise>
                    <div class="sh-table-wrap">
                        <table class="sh-table sh-table--cards" role="table">
                            <caption class="visually-hidden">Serviços da OS <c:out value="${os.numero}" /></caption>
                            <thead role="rowgroup">
                                <tr role="row">
                                    <th scope="col" role="columnheader">Serviço</th>
                                    <th scope="col" role="columnheader" class="text-end">Valor</th>
                                    <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                                </tr>
                            </thead>
                            <tbody role="rowgroup">
                                <c:forEach var="s" items="${detalhe.servicos}">
                                    <tr role="row">
                                        <td role="cell"><c:out value="${s.descricao}" /></td>
                                        <td role="cell" data-label="Valor" class="text-end os-dinheiro"><fmt:formatNumber value="${s.valor}" type="currency" currencySymbol="R$ " /></td>
                                        <td role="cell" data-label="" class="os-acoes-celula">
                                            <c:if test="${os.status.editavel()}">
                                                <form method="post" action="${ctx}/os/${os.id}/servicos/${s.id}/remover">
                                                    <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                                                    <button type="submit" class="btn btn-outline-secondary btn-sm">Retirar<span class="visually-hidden"> <c:out value="${s.descricao}" /></span></button>
                                                </form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
            <c:if test="${os.status.editavel()}">
                <form method="post" action="${ctx}/os/${os.id}/servicos" class="os-linha-form" data-sh-enviando="Incluindo…">
                    <input type="hidden" name="${csrfNome}" value="${csrfToken}" />
                    <div class="row g-3 align-items-end">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="descricao">Incluir serviço</label>
                            <input id="descricao" class="form-control" type="text" name="descricao" maxlength="255" required autocomplete="off" placeholder="Ex.: Troca de óleo">
                        </div>
                        <div class="col-6 col-md-4">
                            <label class="form-label" for="valor">Valor (R$)</label>
                            <input id="valor" class="form-control" type="text" inputmode="decimal" name="valor" required autocomplete="off">
                        </div>
                        <div class="col-6 col-md-2">
                            <button class="btn btn-outline-secondary w-100" type="submit">Incluir</button>
                        </div>
                    </div>
                </form>
            </c:if>
            <dl class="os-totais">
                <div><dt>Peças</dt><dd><fmt:formatNumber value="${detalhe.subtotalPecas}" type="currency" currencySymbol="R$ " /></dd></div>
                <div><dt>Serviços</dt><dd><fmt:formatNumber value="${detalhe.subtotalServicos}" type="currency" currencySymbol="R$ " /></dd></div>
                <c:if test="${os.desconto.signum() > 0}"><div><dt>Desconto</dt><dd>- <fmt:formatNumber value="${os.desconto}" type="currency" currencySymbol="R$ " /></dd></div></c:if>
                <div class="os-totais__final"><dt>Total</dt><dd><fmt:formatNumber value="${os.total}" type="currency" currencySymbol="R$ " /></dd></div>
            </dl>
        </section>
    </jsp:body>
</sh:layout>
