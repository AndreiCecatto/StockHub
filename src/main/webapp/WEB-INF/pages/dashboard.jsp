<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Início (dashboard) no novo design, lote 2. Rotas "/" e "/dashboard".
    Dados do DashboardController: resumo (totalProdutos, totalCategorias, produtosEstoqueBaixo que INCLUI o sem estoque,
    produtosSemEstoque, quantidadeTotalItens, valorTotalEstoque), produtosRecentes (6) e produtosEstoqueBaixo (6 de
    menor saldo; cada Produto traz categoria.nome e nivelEstoque).
    Ordem: o que precisa de ação primeiro (reposição), depois a saúde do estoque, os totais e os recentes.
    Sem script nem style inline: larguras por classes (.sh-seg-N) e números animados por js/numeros.js (data-contar).
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Distribuição Normal / Baixo / Sem estoque (seção 4.12). produtosEstoqueBaixo inclui os sem estoque. --%>
<c:set var="nTotal" value="${resumo.totalProdutos}" />
<c:set var="nSem" value="${resumo.produtosSemEstoque}" />
<c:set var="nBaixo" value="${resumo.produtosEstoqueBaixo - resumo.produtosSemEstoque}" />
<c:set var="nNormal" value="${resumo.totalProdutos - resumo.produtosEstoqueBaixo}" />
<c:set var="nReposicao" value="${resumo.produtosEstoqueBaixo}" />
<c:if test="${nTotal > 0}">
    <%-- Percentuais inteiros que fecham em 100: Sem e Baixo arredondados, Normal é o que sobra --%>
    <fmt:formatNumber var="pSem" value="${nSem * 100 / nTotal}" maxFractionDigits="0" groupingUsed="false" />
    <fmt:formatNumber var="pBaixo" value="${nBaixo * 100 / nTotal}" maxFractionDigits="0" groupingUsed="false" />
    <c:set var="pNormal" value="${100 - pSem - pBaixo}" />
    <c:if test="${pNormal < 0}"><c:set var="pNormal" value="0" /></c:if>
</c:if>

<sh:layout titulo="Início" ativo="dashboard">
    <jsp:body>
        <sh:cabecalhoPagina titulo="Início" descricao="O que precisa de atenção hoje no estoque da sua loja.">
            <a class="btn btn-primary" href="${ctx}/estoque">
                <i class="bi bi-arrow-left-right" aria-hidden="true"></i> Registrar movimentação
            </a>
            <a class="btn btn-outline-secondary" href="${ctx}/produtos/novo">
                <i class="bi bi-plus-lg" aria-hidden="true"></i> Novo produto
            </a>
        </sh:cabecalhoPagina>

        <c:choose>
            <%-- ===== Loja nova: primeiros passos ===== --%>
            <c:when test="${nTotal == 0}">
                <section class="sh-secao" aria-labelledby="titulo-passos">
                    <h2 class="sh-card__titulo" id="titulo-passos">Primeiros passos</h2>
                    <p class="sh-card__desc mb-3">Em três etapas a sua loja está pronta para usar.</p>
                    <ol class="sh-passos">
                        <li class="sh-passo ${resumo.totalCategorias > 0 ? 'is-feito' : ''}">
                            <span class="sh-passo__numero" aria-hidden="true">1</span>
                            <h3 class="sh-passo__titulo">Criar uma categoria</h3>
                            <p class="sh-passo__texto">Agrupe os produtos, por exemplo Bebidas ou Limpeza.</p>
                            <c:choose>
                                <c:when test="${resumo.totalCategorias > 0}">
                                    <span class="sh-passo__estado"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Concluído</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="btn btn-primary btn-sm" href="${ctx}/categorias/novo">Criar categoria</a>
                                </c:otherwise>
                            </c:choose>
                        </li>
                        <li class="sh-passo">
                            <span class="sh-passo__numero" aria-hidden="true">2</span>
                            <h3 class="sh-passo__titulo">Cadastrar o primeiro produto</h3>
                            <p class="sh-passo__texto">Informe nome, categoria, preço, estoque inicial e o mínimo que você quer manter.</p>
                            <a class="btn ${resumo.totalCategorias > 0 ? 'btn-primary' : 'btn-outline-secondary'} btn-sm"
                                href="${ctx}/produtos/novo">Cadastrar produto</a>
                        </li>
                        <li class="sh-passo">
                            <span class="sh-passo__numero" aria-hidden="true">3</span>
                            <h3 class="sh-passo__titulo">Registrar uma entrada</h3>
                            <p class="sh-passo__texto">Quando chegar mercadoria, registre a entrada e o saldo se atualiza.</p>
                            <span class="sh-passo__pendente">Disponível depois do primeiro produto.</span>
                        </li>
                    </ol>
                </section>
            </c:when>

            <c:otherwise>
                <%-- ===== Precisam de reposição (destaque) ===== --%>
                <section class="sh-reposicao ${nReposicao == 0 ? 'sh-reposicao--ok' : (nSem > 0 ? 'sh-reposicao--critico' : 'sh-reposicao--alerta')}"
                    aria-labelledby="titulo-reposicao">
                    <div class="sh-reposicao__cab">
                        <div class="sh-reposicao__resumo">
                            <span class="sh-reposicao__icone" aria-hidden="true">
                                <i class="bi ${nReposicao == 0 ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'}"></i>
                            </span>
                            <div>
                                <h2 class="sh-reposicao__titulo" id="titulo-reposicao">Precisam de reposição</h2>
                                <c:choose>
                                    <c:when test="${nReposicao == 0}">
                                        <p class="sh-reposicao__sub"><strong>Tudo em dia.</strong> Nenhum produto está no estoque mínimo ou abaixo dele.</p>
                                    </c:when>
                                    <c:otherwise>
                                        <p class="sh-reposicao__sub">
                                            <span class="sh-reposicao__numero" data-contar="<c:out value='${nReposicao}'/>" data-formato="inteiro"><c:out value="${nReposicao}" /></span>
                                            <c:out value="${nReposicao == 1 ? 'produto no mínimo ou abaixo' : 'produtos no mínimo ou abaixo'}" />
                                            <c:if test="${nSem > 0}">
                                                &middot; <strong><c:out value="${nSem}" /> sem estoque</strong>
                                            </c:if>
                                        </p>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <c:if test="${nReposicao > 0}">
                            <a class="btn btn-outline-secondary" href="${ctx}/estoque-baixo">
                                Ver todos <i class="bi bi-arrow-right" aria-hidden="true"></i>
                            </a>
                        </c:if>
                    </div>
                    <c:if test="${nReposicao > 0}">
                        <ul class="sh-lista">
                            <c:forEach var="produto" items="${produtosEstoqueBaixo}">
                                <li class="sh-lista__item sh-lista__item--repor">
                                    <div class="sh-lista__info">
                                        <a class="sh-lista__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                        <span class="sh-lista__sub"><c:out value="${produto.categoria.nome}" /></span>
                                    </div>
                                    <div class="sh-lista__saldo sh-saldo">
                                        <span class="sh-saldo__texto">
                                            <strong><c:out value="${produto.quantidade}" /></strong> un.
                                            <span class="sh-saldo__minimo">(mínimo <c:out value="${produto.estoqueMinimo}" />)</span>
                                        </span>
                                        <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                    </div>
                                    <div class="sh-lista__situacao">
                                        <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                                    </div>
                                    <div class="sh-lista__acao">
                                        <a class="btn btn-primary btn-sm"
                                            href="<c:out value='${ctx}/estoque?movProduto=${produto.id}&movTipo=ENTRADA'/>"
                                            aria-label="Repor <c:out value='${produto.nome}'/>">
                                            <i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Repor
                                        </a>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                        <c:if test="${nReposicao > 6}">
                            <div class="sh-card__corpo border-top">
                                <a href="${ctx}/estoque-baixo">Ver os outros <c:out value="${nReposicao - 6}" /> produtos na lista completa</a>
                            </div>
                        </c:if>
                    </c:if>
                </section>

                <%-- ===== Saúde do estoque: Normal / Baixo / Sem estoque ===== --%>
                <section class="sh-card sh-secao" aria-labelledby="titulo-saude">
                    <div class="sh-card__cab">
                        <div>
                            <h2 class="sh-card__titulo" id="titulo-saude">Saúde do estoque</h2>
                            <p class="sh-card__desc">Como os <c:out value="${nTotal}" /> produtos estão em relação ao mínimo.</p>
                        </div>
                    </div>
                    <div class="sh-card__corpo">
                        <%-- A barra é decorativa; a informação está na legenda em texto --%>
                        <div class="sh-saude" aria-hidden="true">
                            <c:if test="${nNormal > 0}"><span class="sh-saude__seg sh-saude__seg--normal sh-seg-<c:out value='${pNormal}'/>"></span></c:if>
                            <c:if test="${nBaixo > 0}"><span class="sh-saude__seg sh-saude__seg--baixo sh-seg-<c:out value='${pBaixo}'/>"></span></c:if>
                            <c:if test="${nSem > 0}"><span class="sh-saude__seg sh-saude__seg--sem sh-seg-<c:out value='${pSem}'/>"></span></c:if>
                        </div>
                        <ul class="sh-saude__legenda">
                            <li class="sh-tom--normal">
                                <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                                <span>Normal</span> <strong><c:out value="${nNormal}" /></strong>
                                <span class="sh-saude__pct">(<c:out value="${pNormal}" />%)</span>
                            </li>
                            <li class="sh-tom--baixo">
                                <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                                <span>Baixo</span> <strong><c:out value="${nBaixo}" /></strong>
                                <span class="sh-saude__pct">(<c:out value="${pBaixo}" />%)</span>
                            </li>
                            <li class="sh-tom--sem">
                                <i class="bi bi-x-octagon-fill" aria-hidden="true"></i>
                                <span>Sem estoque</span> <strong><c:out value="${nSem}" /></strong>
                                <span class="sh-saude__pct">(<c:out value="${pSem}" />%)</span>
                            </li>
                        </ul>
                    </div>
                </section>
            </c:otherwise>
        </c:choose>

        <%-- ===== Totais (cada cartão é um link para a lista) ===== --%>
        <section aria-labelledby="titulo-totais">
            <h2 class="visually-hidden" id="titulo-totais">Totais da loja</h2>
            <div class="sh-metricas">
                <a class="sh-metric" href="${ctx}/produtos?tamanho=25">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Produtos</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-box-seam"></i></span>
                    </span>
                    <span class="sh-metric__valor" data-contar="<c:out value='${resumo.totalProdutos}'/>" data-formato="inteiro"><c:out value="${resumo.totalProdutos}" /></span>
                    <span class="sh-metric__apoio"><c:out value="${nTotal == 0 ? 'Cadastre o primeiro produto' : 'itens cadastrados'}" /></span>
                </a>
                <a class="sh-metric" href="${ctx}/unidades">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Unidades</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-stack"></i></span>
                    </span>
                    <span class="sh-metric__valor" data-contar="<c:out value='${resumo.quantidadeTotalItens}'/>" data-formato="inteiro"><c:out value="${resumo.quantidadeTotalItens}" /></span>
                    <span class="sh-metric__apoio">no estoque, somando os produtos</span>
                </a>
                <a class="sh-metric" href="${ctx}/valor-estoque">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Valor do estoque</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-cash-coin"></i></span>
                    </span>
                    <span class="sh-metric__valor" data-contar="<c:out value='${resumo.valorTotalEstoque}'/>" data-formato="moeda">R$ <fmt:formatNumber value="${resumo.valorTotalEstoque}" pattern="#,##0.00" /></span>
                    <span class="sh-metric__apoio">preço vezes quantidade</span>
                </a>
                <a class="sh-metric" href="${ctx}/categorias">
                    <span class="sh-metric__topo">
                        <span class="sh-metric__rotulo">Categorias</span>
                        <span class="sh-metric__icone" aria-hidden="true"><i class="bi bi-tags"></i></span>
                    </span>
                    <span class="sh-metric__valor" data-contar="<c:out value='${resumo.totalCategorias}'/>" data-formato="inteiro"><c:out value="${resumo.totalCategorias}" /></span>
                    <span class="sh-metric__apoio"><c:out value="${resumo.totalCategorias == 0 ? 'Crie a primeira categoria' : 'grupos de produtos'}" /></span>
                </a>
            </div>
        </section>

        <%-- ===== Produtos recentes ===== --%>
        <section class="sh-card" aria-labelledby="titulo-recentes">
            <div class="sh-card__cab">
                <div>
                    <h2 class="sh-card__titulo" id="titulo-recentes">Produtos recentes</h2>
                    <p class="sh-card__desc">Os últimos itens cadastrados.</p>
                </div>
                <a class="btn btn-outline-secondary btn-sm" href="${ctx}/produtos?tamanho=25">Ver todos os produtos</a>
            </div>
            <c:choose>
                <c:when test="${empty produtosRecentes}">
                    <sh:estadoVazio icone="box-seam" titulo="Nenhum produto cadastrado"
                        texto="Cadastre o primeiro produto para acompanhar o saldo e receber o aviso de reposição."
                        acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                </c:when>
                <c:otherwise>
                    <ul class="sh-lista">
                        <c:forEach var="produto" items="${produtosRecentes}">
                            <li class="sh-lista__item sh-lista__item--recente">
                                <div class="sh-lista__info">
                                    <a class="sh-lista__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                    <span class="sh-lista__sub"><c:out value="${produto.categoria.nome}" /></span>
                                </div>
                                <div class="sh-lista__preco sh-num">
                                    R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" />
                                </div>
                                <div class="sh-lista__situacao">
                                    <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
