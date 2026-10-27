<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Detalhe do produto na v3. Rota: GET /produtos/{id}.
    Dados do ProdutoController: produto (id, nome, descricao, preco, quantidade, estoqueMinimo, categoria.nome,
    valorTotal, nivelEstoque) e movimentacoesProduto (últimas movimentações do produto, mais nova primeiro; cada uma
    com criadoEm, tipo, quantidade, quantidadeAnterior, quantidadeNova, observacao). O histórico só é desenhado se o
    atributo existir; lista vazia mostra o estado vazio. Não acessa movimentacao.produto (carregamento tardio).

    A região <div data-peek> é o que o painel lateral da lista (js/peek.js) mostra sem sair da lista: saldo, medidor,
    tendência, dados, ações e histórico. Na página inteira ela aparece abaixo do cabeçalho (que tem as ações, inclusive
    Excluir); as ações "só do painel" (.v3-det-acoes--peek) ficam ocultas aqui e visíveis no painel (css/lote-produtos.css).
    Excluir não existe no painel (o modal de confirmação fica atrás do painel); ele é só desta página.
    Excluir: POST /produtos/{id}/excluir com CSRF e confirmação em modal (RN-08: exclui ou inativa).
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="nivel" value="${produto.nivelEstoque}" />
<c:set var="falta" value="${produto.estoqueMinimo + 1 - produto.quantidade}" />

<%-- Série do saldo para a tendência: mais antigo primeiro (a lista vem da mais nova para a mais antiga) --%>
<c:set var="serie" value="" />
<c:set var="pontos" value="0" />
<c:forEach var="mov" items="${movimentacoesProduto}">
    <c:set var="serie" value="${mov.quantidadeNova}${empty serie ? '' : ','}${serie}" />
    <c:set var="pontos" value="${pontos + 1}" />
</c:forEach>

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
            <c:if test="${podeGerenciar}">
            <form class="m-0" method="post" action="<c:out value='${ctx}/produtos/${produto.id}/excluir'/>"
                data-confirmar-exclusao data-tipo="o produto" data-nome="<c:out value='${produto.nome}'/>"
                data-aviso="Sem movimentações além do estoque inicial, o produto é excluído de vez. Se já tem histórico, ele é inativado: some das listas e não aceita mais movimentação, e o histórico fica preservado.">
                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                <button type="submit" class="btn btn-outline-danger">
                    <i class="bi bi-trash3" aria-hidden="true"></i> Excluir
                </button>
            </form>
            </c:if>
        </sh:cabecalhoPagina>

        <div class="v3-det" data-peek data-peek-titulo="<c:out value='${produto.nome}'/>">
            <div class="v3-det__principal">
                <p class="v3-det-meta">
                    <span class="v3-det-meta__item"><i class="bi bi-tag" aria-hidden="true"></i> <c:out value="${produto.categoria.nome}" /></span>
                    <span class="v3-det-meta__item">Código <span class="v3-mono"><c:out value="${produto.id}" /></span></span>
                </p>

                <%-- ===== Saldo ===== --%>
                <section class="v3-hero" aria-labelledby="titulo-saldo">
                    <h2 class="sh-eyebrow" id="titulo-saldo">Saldo atual</h2>
                    <p class="v3-hero__linha">
                        <span class="v3-hero__num v3-mono" data-contar="<c:out value='${produto.quantidade}'/>" data-formato="inteiro"><fmt:formatNumber value="${produto.quantidade}" pattern="#,##0" /></span>
                        <span class="v3-hero__un">un.</span>
                        <sh:badgeEstoque nivel="${nivel}" />
                    </p>
                    <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                    <p class="v3-hero__apoio">
                        <c:choose>
                            <c:when test="${produto.estoqueMinimo <= 0}">Sem estoque mínimo definido: o produto só muda de situação ao zerar.</c:when>
                            <c:when test="${nivel == 'NORMAL'}">Mínimo de <span class="v3-mono"><fmt:formatNumber value="${produto.estoqueMinimo}" pattern="#,##0" /></span> un. Há <span class="v3-mono"><fmt:formatNumber value="${produto.quantidade - produto.estoqueMinimo}" pattern="#,##0" /></span> un. acima dele.</c:when>
                            <c:otherwise>Mínimo de <span class="v3-mono"><fmt:formatNumber value="${produto.estoqueMinimo}" pattern="#,##0" /></span> un. Repor ao menos <span class="v3-mono"><fmt:formatNumber value="${falta}" pattern="#,##0" /></span> un. deixa o produto acima do mínimo, na situação Normal.</c:otherwise>
                        </c:choose>
                    </p>
                    <c:if test="${pontos >= 2}">
                        <div class="v3-hero__spark">
                            <sh:sparkline valores="${serie}" largura="320" altura="56" area="true"
                                tom="${nivel == 'NORMAL' ? 'normal' : (nivel == 'BAIXO' ? 'baixo' : 'sem')}"
                                rotulo="Saldo ao longo das últimas ${pontos} movimentações" />
                            <p class="v3-hero__spark-leg">Saldo nas últimas <span class="v3-mono"><c:out value="${pontos}" /></span> movimentações</p>
                        </div>
                    </c:if>
                </section>

                <%-- Ações do painel (na página inteira elas ficam no cabeçalho) --%>
                <div class="v3-det-acoes v3-det-acoes--peek">
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
                </div>

                <%-- ===== Histórico (linha do tempo) ===== --%>
                <c:if test="${movimentacoesProduto != null}">
                    <section class="v3-hist" aria-labelledby="titulo-historico">
                        <div class="v3-hist__cab">
                            <h2 class="v3-secao-titulo" id="titulo-historico">Movimentações recentes</h2>
                            <c:if test="${not empty movimentacoesProduto}">
                                <a class="btn btn-ghost btn-sm" href="<c:out value='${ctx}/estoque?produto=${produto.id}'/>">
                                    Ver tudo no histórico <i class="bi bi-arrow-right" aria-hidden="true"></i>
                                </a>
                            </c:if>
                        </div>
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
                                                            <span class="sh-timeline__delta v3-mono sh-prod-delta--entrada">+<c:out value="${mov.quantidade}" /></span>
                                                        </c:when>
                                                        <c:when test="${mov.tipo == 'SAIDA'}">
                                                            <span class="sh-prod-mov__tipo">Saída</span>
                                                            <span class="sh-timeline__delta v3-mono sh-prod-delta--saida">&minus;<c:out value="${mov.quantidade}" /></span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="sh-prod-mov__tipo">Ajuste</span>
                                                            <span class="sh-timeline__delta v3-mono">= <c:out value="${mov.quantidade}" /> (contagem)</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                    <time class="sh-prod-mov__hora v3-mono" datetime="<c:out value='${fn:substring(iso, 0, 16)}'/>"><c:out value="${fn:substring(iso, 11, 16)}" /></time>
                                                </div>
                                                <p class="sh-prod-mov__saldo">
                                                    Saldo <span class="v3-mono"><c:out value="${mov.quantidadeAnterior}" /></span> <i class="bi bi-arrow-right" aria-hidden="true"></i><span class="visually-hidden">para</span> <span class="v3-mono"><c:out value="${mov.quantidadeNova}" /></span> un.
                                                </p>
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
                    </section>
                </c:if>
            </div>

            <aside class="v3-det__lado" aria-label="Dados do produto">
                <section class="v3-dados" aria-labelledby="titulo-dados">
                    <h2 class="v3-secao-titulo" id="titulo-dados">Dados</h2>
                    <dl class="v3-dl">
                        <div class="v3-dl__item">
                            <dt>Preço unitário</dt>
                            <dd class="v3-mono">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></dd>
                        </div>
                        <div class="v3-dl__item">
                            <dt>Valor em estoque</dt>
                            <dd class="v3-mono">R$ <fmt:formatNumber value="${produto.valorTotal}" pattern="#,##0.00" /></dd>
                        </div>
                        <div class="v3-dl__item">
                            <dt>Estoque mínimo</dt>
                            <dd class="v3-mono"><fmt:formatNumber value="${produto.estoqueMinimo}" pattern="#,##0" /> un.</dd>
                        </div>
                        <div class="v3-dl__item">
                            <dt>Categoria</dt>
                            <dd><c:out value="${produto.categoria.nome}" /></dd>
                        </div>
                    </dl>
                </section>

                <section class="v3-dados" aria-labelledby="titulo-descricao">
                    <h2 class="v3-secao-titulo" id="titulo-descricao">Descrição</h2>
                    <c:choose>
                        <c:when test="${empty produto.descricao}">
                            <p class="sh-prod-descricao sh-prod-descricao--vazia">Sem descrição cadastrada.</p>
                        </c:when>
                        <c:otherwise>
                            <p class="sh-prod-descricao"><c:out value="${produto.descricao}" /></p>
                        </c:otherwise>
                    </c:choose>
                </section>
            </aside>
        </div>
    </jsp:body>
</sh:layout>
