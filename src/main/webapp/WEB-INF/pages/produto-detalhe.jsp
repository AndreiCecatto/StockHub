<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Detalhe do produto no novo design (lote 3). Rota: GET /produtos/{id}.
    Dados do ProdutoController: produto (id, nome, descricao, preco, quantidade, estoqueMinimo, categoria.nome,
    valorTotal, nivelEstoque) e movimentacoesProduto (últimas movimentações do produto, mais nova primeiro; cada uma
    com criadoEm, tipo, quantidade, quantidadeAnterior, quantidadeNova, observacao). O histórico só é desenhado se o
    atributo existir; lista vazia mostra o estado vazio. Não acessa movimentacao.produto (carregamento tardio).
    Excluir: POST /produtos/{id}/excluir com CSRF e confirmação em modal (RN-08: exclui ou inativa).
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="nivel" value="${produto.nivelEstoque}" />

<sh:layout titulo="${produto.nome}" ativo="produtos" voltarUrl="/produtos" voltarRotulo="Produtos">
    <jsp:attribute name="migalhas">
        <li class="breadcrumb-item"><a href="${ctx}/dashboard">Início</a></li>
        <li class="breadcrumb-item"><a href="${ctx}/produtos">Produtos</a></li>
        <li class="breadcrumb-item active" aria-current="page"><c:out value="${produto.nome}" /></li>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="${produto.nome}">
            <c:choose>
                <c:when test="${nivel == 'NORMAL'}">
                    <a class="btn btn-primary" href="<c:out value='${ctx}/estoque?movProduto=${produto.id}'/>">
                        <i class="bi bi-arrow-left-right" aria-hidden="true"></i> Movimentar
                    </a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-primary" href="<c:out value='${ctx}/estoque?movProduto=${produto.id}&movTipo=ENTRADA'/>">
                        <i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Repor estoque
                    </a>
                </c:otherwise>
            </c:choose>
            <a class="btn btn-outline-secondary" href="<c:out value='${ctx}/produtos/${produto.id}/editar'/>">
                <i class="bi bi-pencil" aria-hidden="true"></i> Editar
            </a>
            <form class="m-0" method="post" action="<c:out value='${ctx}/produtos/${produto.id}/excluir'/>"
                data-confirmar-exclusao data-tipo="o produto" data-nome="<c:out value='${produto.nome}'/>"
                data-aviso="Sem movimentações além do estoque inicial, o produto é excluído de vez. Se já tem histórico, ele é inativado: some das listas e não aceita mais movimentação, e o histórico fica preservado.">
                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                <button type="submit" class="btn btn-outline-danger">
                    <i class="bi bi-trash3" aria-hidden="true"></i> Excluir
                </button>
            </form>
        </sh:cabecalhoPagina>

        <p class="sh-prod-meta">
            <span class="sh-prod-meta__item"><i class="bi bi-tag" aria-hidden="true"></i> <c:out value="${produto.categoria.nome}" /></span>
            <sh:badgeEstoque nivel="${nivel}" />
            <span class="sh-prod-meta__item">Código <c:out value="${produto.id}" /></span>
        </p>

        <%-- ===== Números do produto ===== --%>
        <section aria-labelledby="titulo-numeros">
            <h2 class="visually-hidden" id="titulo-numeros">Saldo, preço e valor em estoque</h2>
            <div class="sh-metricas">
                <div class="sh-metric">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Saldo</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-stack"></i></span>
                    </span>
                    <span class="sh-metric__valor">
                        <span data-contar="<c:out value='${produto.quantidade}'/>" data-formato="inteiro"><c:out value="${produto.quantidade}" /></span>
                        <span class="sh-prod-un">un.</span>
                    </span>
                    <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                    <span class="sh-metric__apoio">
                        <c:choose>
                            <c:when test="${produto.estoqueMinimo > 0}">mínimo <c:out value="${produto.estoqueMinimo}" /> un.</c:when>
                            <c:otherwise>sem estoque mínimo definido</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <div class="sh-metric">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Preço unitário</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-tag"></i></span>
                    </span>
                    <span class="sh-metric__valor">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></span>
                    <span class="sh-metric__apoio">valor de venda de cada unidade</span>
                </div>
                <div class="sh-metric">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Valor em estoque</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-cash-coin"></i></span>
                    </span>
                    <span class="sh-metric__valor" data-contar="<c:out value='${produto.valorTotal}'/>" data-formato="moeda">R$ <fmt:formatNumber value="${produto.valorTotal}" pattern="#,##0.00" /></span>
                    <span class="sh-metric__apoio">preço vezes saldo</span>
                </div>
                <div class="sh-metric">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Estoque mínimo</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-exclamation-triangle"></i></span>
                    </span>
                    <span class="sh-metric__valor"><c:out value="${produto.estoqueMinimo}" /> <span class="sh-prod-un">un.</span></span>
                    <span class="sh-metric__apoio">no mínimo ou abaixo, o produto aparece como Baixo</span>
                </div>
            </div>
        </section>

        <%-- ===== Descrição ===== --%>
        <section class="sh-card sh-secao" aria-labelledby="titulo-descricao">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-descricao">Descrição</h2>
            </div>
            <div class="sh-card__corpo">
                <c:choose>
                    <c:when test="${empty produto.descricao}">
                        <p class="sh-prod-descricao sh-prod-descricao--vazia">Sem descrição cadastrada.</p>
                    </c:when>
                    <c:otherwise>
                        <p class="sh-prod-descricao"><c:out value="${produto.descricao}" /></p>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <%-- ===== Histórico do produto (linha do tempo) ===== --%>
        <c:if test="${movimentacoesProduto != null}">
            <section class="sh-card" aria-labelledby="titulo-historico">
                <div class="sh-card__cab">
                    <div>
                        <h2 class="sh-card__titulo" id="titulo-historico">Histórico do produto</h2>
                        <c:if test="${not empty movimentacoesProduto}">
                            <p class="sh-card__desc">
                                <c:choose>
                                    <c:when test="${fn:length(movimentacoesProduto) == 1}">Última movimentação, com o saldo antes e depois.</c:when>
                                    <c:otherwise>Últimas <c:out value="${fn:length(movimentacoesProduto)}" /> movimentações, da mais nova para a mais antiga, com o saldo antes e depois.</c:otherwise>
                                </c:choose>
                            </p>
                        </c:if>
                    </div>
                    <c:if test="${not empty movimentacoesProduto}">
                        <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${ctx}/estoque?produto=${produto.id}'/>">
                            Ver tudo no histórico <i class="bi bi-arrow-right" aria-hidden="true"></i>
                        </a>
                    </c:if>
                </div>
                <div class="sh-card__corpo">
                    <c:choose>
                        <c:when test="${empty movimentacoesProduto}">
                            <sh:estadoVazio icone="clock-history" titulo="Nenhuma movimentação ainda"
                                texto="Registre a primeira entrada, saída ou ajuste deste produto e ela aparece aqui."
                                acaoUrl="/estoque?movProduto=${produto.id}" acaoRotulo="Registrar movimentação" />
                        </c:when>
                        <c:otherwise>
                            <div class="sh-prod-hist">
                                <c:set var="diaAnterior" value="" />
                                <c:forEach var="mov" items="${movimentacoesProduto}" varStatus="passo">
                                    <%-- criadoEm é LocalDateTime: o texto vem como 2026-10-26T14:30:05; recorto data e hora --%>
                                    <c:set var="iso" value="${mov.criadoEm}" />
                                    <c:set var="diaChave" value="${fn:substring(iso, 0, 10)}" />
                                    <c:if test="${diaChave != diaAnterior}">
                                        <c:if test="${not passo.first}"></ul></c:if>
                                        <h3 class="sh-timeline__dia"><c:out value="${fn:substring(iso, 8, 10)}/${fn:substring(iso, 5, 7)}/${fn:substring(iso, 0, 4)}" /></h3>
                                        <ul class="sh-timeline">
                                        <c:set var="diaAnterior" value="${diaChave}" />
                                    </c:if>
                                    <li class="sh-timeline__item">
                                        <c:choose>
                                            <c:when test="${mov.tipo == 'ENTRADA'}">
                                                <span class="sh-timeline__icone sh-prod-tipo--entrada" aria-hidden="true"><i class="bi bi-box-arrow-in-down"></i></span>
                                            </c:when>
                                            <c:when test="${mov.tipo == 'SAIDA'}">
                                                <span class="sh-timeline__icone sh-prod-tipo--saida" aria-hidden="true"><i class="bi bi-box-arrow-up"></i></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="sh-timeline__icone sh-prod-tipo--ajuste" aria-hidden="true"><i class="bi bi-sliders"></i></span>
                                            </c:otherwise>
                                        </c:choose>
                                        <div>
                                            <div class="sh-prod-mov">
                                                <c:choose>
                                                    <c:when test="${mov.tipo == 'ENTRADA'}">
                                                        <span class="sh-prod-mov__tipo">Entrada</span>
                                                        <span class="sh-timeline__delta sh-prod-delta--entrada">+<c:out value="${mov.quantidade}" /> un.</span>
                                                    </c:when>
                                                    <c:when test="${mov.tipo == 'SAIDA'}">
                                                        <span class="sh-prod-mov__tipo">Saída</span>
                                                        <span class="sh-timeline__delta sh-prod-delta--saida">&minus;<c:out value="${mov.quantidade}" /> un.</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="sh-prod-mov__tipo">Ajuste</span>
                                                        <span class="sh-timeline__delta">= <c:out value="${mov.quantidade}" /> un. (contagem)</span>
                                                    </c:otherwise>
                                                </c:choose>
                                                <span class="sh-prod-mov__saldo">
                                                    Saldo <c:out value="${mov.quantidadeAnterior}" /> <i class="bi bi-arrow-right" aria-hidden="true"></i><span class="visually-hidden">para</span> <c:out value="${mov.quantidadeNova}" /> un.
                                                </span>
                                                <time class="sh-prod-mov__hora" datetime="<c:out value='${fn:substring(iso, 0, 16)}'/>"><c:out value="${fn:substring(iso, 11, 16)}" /></time>
                                            </div>
                                            <c:if test="${not empty mov.observacao}">
                                                <p class="sh-prod-mov__obs"><c:out value="${mov.observacao}" /></p>
                                            </c:if>
                                        </div>
                                    </li>
                                </c:forEach>
                                </ul>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>
        </c:if>
    </jsp:body>
</sh:layout>
