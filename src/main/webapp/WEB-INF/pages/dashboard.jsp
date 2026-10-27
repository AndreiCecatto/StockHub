<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Início (dashboard). Estrutura v1/v2 mantida (será refeita como narrativa de rolagem). Rotas "/" e "/dashboard".
    Dados do DashboardController (inalterados): resumo (totalProdutos, totalCategorias, produtosEstoqueBaixo que INCLUI o
    sem estoque, produtosSemEstoque, quantidadeTotalItens, valorTotalEstoque), produtosRecentes (6) e produtosEstoqueBaixo
    (6 de menor saldo; cada Produto traz categoria.nome e nivelEstoque).
    Ordem: título com a data, "o que fazer hoje" (reposição com ação rápida), saúde do
    estoque (anel interativo), gráfico de 14 dias, os números da loja e o que chegou por último.
    Dados vindos por fetch (js/inicio.js): GET /api/dashboard/movimentos?dias=14 -> [{dia, entradas, saidas, ajustes}].
    Se o endpoint não existir ou falhar, o gráfico mostra um estado de erro e o resto da tela segue normal.
    A data de hoje é escrita no navegador (js/tempo.js); o texto do servidor é o plano B.
    Sem script nem style inline: larguras por classes (.sh-w-N) e números animados por js/numeros.js (data-contar).
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Distribuição Normal / Baixo / Sem estoque (seção 4.12). produtosEstoqueBaixo inclui os sem estoque. --%>
<c:set var="nTotal" value="${resumo.totalProdutos}" />
<c:set var="nSem" value="${resumo.produtosSemEstoque}" />
<c:set var="nBaixo" value="${resumo.produtosEstoqueBaixo - resumo.produtosSemEstoque}" />
<c:set var="nNormal" value="${resumo.totalProdutos - resumo.produtosEstoqueBaixo}" />
<c:set var="nReposicao" value="${resumo.produtosEstoqueBaixo}" />
<c:set var="pNormal" value="0" />
<c:set var="pBaixo" value="0" />
<c:set var="pSem" value="0" />
<c:if test="${nTotal > 0}">
    <%-- Percentuais inteiros que fecham em 100: Sem e Baixo arredondados, Normal é o que sobra --%>
    <fmt:formatNumber var="pSem" value="${nSem * 100 / nTotal}" maxFractionDigits="0" groupingUsed="false" />
    <fmt:formatNumber var="pBaixo" value="${nBaixo * 100 / nTotal}" maxFractionDigits="0" groupingUsed="false" />
    <c:set var="pNormal" value="${100 - pSem - pBaixo}" />
    <c:if test="${pNormal < 0}"><c:set var="pNormal" value="0" /></c:if>
</c:if>
<%-- Anel: cada fatia é um traço de (p - 1) com um vão de 1, para as fatias não grudarem --%>
<c:set var="aNormal" value="${pNormal > 2 ? pNormal - 1 : pNormal}" />
<c:set var="aBaixo" value="${pBaixo > 2 ? pBaixo - 1 : pBaixo}" />
<c:set var="aSem" value="${pSem > 2 ? pSem - 1 : pSem}" />

<sh:layout titulo="Início" ativo="dashboard">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/inicio.css">
        <script defer src="${ctx}/js/inicio.js"></script>
    </jsp:attribute>
    <jsp:body>
        <%-- ===== Saudação ===== --%>
        <section class="sh-hero" aria-labelledby="titulo-saudacao">
            <div class="sh-hero__texto">
                <p class="sh-hero__data" data-sh-data-hoje></p>
                <h1 class="sh-hero__titulo" id="titulo-saudacao">Visão geral</h1>
                <p class="sh-hero__frase">
                    <c:choose>
                        <c:when test="${nTotal == 0}">Nenhum produto cadastrado ainda. Comece criando uma categoria e o primeiro produto.</c:when>
                        <c:when test="${nReposicao == 0}"><strong>Nenhum produto abaixo do mínimo.</strong> O estoque está em dia.</c:when>
                        <c:when test="${nSem > 0 and nBaixo > 0}">
                            <strong><c:out value="${nSem}" /> <c:out value="${nSem == 1 ? 'produto acabou' : 'produtos acabaram'}" /></strong>
                            e <c:out value="${nBaixo}" /> <c:out value="${nBaixo == 1 ? 'está' : 'estão'}" /> abaixo do mínimo.
                        </c:when>
                        <c:when test="${nSem > 0}">
                            <strong><c:out value="${nSem}" /> <c:out value="${nSem == 1 ? 'produto acabou' : 'produtos acabaram'}" />.</strong>
                            Reposição necessária.
                        </c:when>
                        <c:otherwise>
                            <strong><c:out value="${nBaixo}" /> <c:out value="${nBaixo == 1 ? 'produto está' : 'produtos estão'}" /> abaixo do mínimo.</strong>
                            Programe a reposição.
                        </c:otherwise>
                    </c:choose>
                </p>
                <div class="sh-hero__acoes">
                    <a class="btn btn-primary" href="${ctx}/estoque?movTipo=ENTRADA">Registrar entrada</a>
                    <a class="btn btn-outline-secondary" href="${ctx}/estoque?movTipo=SAIDA">Registrar saída</a>
                    <a class="sh-hero__link" href="${ctx}/produtos/novo">Novo produto</a>
                </div>
            </div>
        </section>

        <c:choose>
            <%-- ===== Loja nova: primeiros passos ===== --%>
            <c:when test="${nTotal == 0}">
                <section class="sh-secao" aria-labelledby="titulo-passos">
                    <h2 class="sh-titulo-secao" id="titulo-passos">Primeiros passos</h2>
                    <p class="sh-subtitulo-secao">Em três etapas a sua loja está pronta para usar.</p>
                    <ol class="sh-passos sh-escalona">
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
                <div class="sh-inicio__grade">
                    <%-- ===== O que fazer hoje ===== --%>
                    <section class="sh-hoje" aria-labelledby="titulo-hoje">
                        <h2 class="sh-titulo-secao" id="titulo-hoje">O que fazer hoje</h2>
                        <c:choose>
                            <c:when test="${nReposicao == 0}">
                                <div class="sh-hoje__ok">
                                    <div>
                                        <p class="sh-hoje__ok-titulo">Nada para repor.</p>
                                        <p class="sh-hoje__ok-texto">Todos os <c:out value="${nTotal}" /> produtos estão acima do mínimo.</p>
                                        <a class="btn btn-outline-secondary btn-sm" href="${ctx}/produtos/novo">Novo produto</a>
                                    </div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <p class="sh-subtitulo-secao">
                                    <span class="sh-subtitulo-secao__numero" data-contar="<c:out value='${nReposicao}'/>" data-formato="inteiro"><c:out value="${nReposicao}" /></span>
                                    <c:out value="${nReposicao == 1 ? 'produto no mínimo ou abaixo' : 'produtos no mínimo ou abaixo'}" />,
                                    do mais urgente para o menos.
                                </p>
                                <ul class="sh-tarefas">
                                    <c:forEach var="produto" items="${produtosEstoqueBaixo}">
                                        <%-- Sugestão de quantidade: leva o saldo a 2x o mínimo (o medidor fica cheio); sem mínimo, 10 --%>
                                        <c:set var="sugestao" value="${produto.estoqueMinimo > 0 ? produto.estoqueMinimo * 2 - produto.quantidade : 10}" />
                                        <c:if test="${sugestao < 1}"><c:set var="sugestao" value="1" /></c:if>
                                        <li class="sh-tarefa sh-tarefa--${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sem' : 'baixo'}">
                                            <div class="sh-tarefa__info">
                                                <a class="sh-tarefa__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                                <span class="sh-tarefa__sub">
                                                    <c:out value="${produto.categoria.nome}" /> &middot;
                                                    <strong><c:out value="${produto.quantidade}" /></strong> un. (mínimo <c:out value="${produto.estoqueMinimo}" />)
                                                </span>
                                            </div>
                                            <div class="sh-tarefa__estado">
                                                <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                                                <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                            </div>
                                            <%-- Reposição rápida: o mesmo POST da tela de movimentações (entrada), com a quantidade sugerida --%>
                                            <form class="sh-repor" method="post" action="${ctx}/estoque/movimentar" data-sh-bloquear data-sh-enviando="Repondo…">
                                                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                <input type="hidden" name="produtoId" value="<c:out value='${produto.id}'/>">
                                                <input type="hidden" name="tipo" value="ENTRADA">
                                                <input type="hidden" name="observacao" value="Reposição rápida pelo início">
                                                <label class="visually-hidden" for="repor-<c:out value='${produto.id}'/>">Quantidade a repor de <c:out value="${produto.nome}" /></label>
                                                <input class="form-control sh-repor__qtd sh-num" id="repor-<c:out value='${produto.id}'/>" type="number" name="quantidade"
                                                    min="1" max="1000000" step="1" inputmode="numeric" required value="<c:out value='${sugestao}'/>">
                                                <button type="submit" class="btn btn-primary btn-sm">Repor</button>
                                            </form>
                                        </li>
                                    </c:forEach>
                                </ul>
                                <c:if test="${nReposicao > 6}">
                                    <p class="sh-hoje__mais">
                                        <a href="${ctx}/estoque-baixo">Ver os outros <c:out value="${nReposicao - 6}" /> na lista completa</a>
                                    </p>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </section>

                    <%-- ===== Saúde do estoque (anel interativo) ===== --%>
                    <section class="sh-saude-bloco sh-card" aria-labelledby="titulo-saude">
                        <h2 class="sh-titulo-secao" id="titulo-saude">Saúde do estoque</h2>
                        <p class="sh-subtitulo-secao">Passe o mouse ou foque numa linha para destacar a fatia.</p>
                        <div class="sh-anel" data-sh-anel>
                            <div class="sh-anel__desenho">
                                <%-- O anel é decorativo; a informação está na lista ao lado, em texto --%>
                                <svg class="sh-anel__svg" viewBox="0 0 42 42" aria-hidden="true" focusable="false">
                                    <circle class="sh-anel__trilho" cx="21" cy="21" r="15.9155"/>
                                    <c:if test="${nNormal > 0}">
                                        <circle class="sh-anel__fatia sh-anel__fatia--normal" data-sh-fatia="normal" cx="21" cy="21" r="15.9155"
                                            stroke-dasharray="<c:out value='${aNormal}'/> <c:out value='${100 - aNormal}'/>" stroke-dashoffset="25"/>
                                    </c:if>
                                    <c:if test="${nBaixo > 0}">
                                        <circle class="sh-anel__fatia sh-anel__fatia--baixo" data-sh-fatia="baixo" cx="21" cy="21" r="15.9155"
                                            stroke-dasharray="<c:out value='${aBaixo}'/> <c:out value='${100 - aBaixo}'/>" stroke-dashoffset="<c:out value='${25 - pNormal}'/>"/>
                                    </c:if>
                                    <c:if test="${nSem > 0}">
                                        <circle class="sh-anel__fatia sh-anel__fatia--sem" data-sh-fatia="sem" cx="21" cy="21" r="15.9155"
                                            stroke-dasharray="<c:out value='${aSem}'/> <c:out value='${100 - aSem}'/>" stroke-dashoffset="<c:out value='${25 - pNormal - pBaixo}'/>"/>
                                    </c:if>
                                </svg>
                                <div class="sh-anel__centro" aria-hidden="true">
                                    <span class="sh-anel__centro-bloco sh-anel__centro-bloco--geral">
                                        <span class="sh-anel__valor"><c:out value="${pNormal}" />%</span>
                                        <span class="sh-anel__rotulo">em dia</span>
                                    </span>
                                    <span class="sh-anel__centro-bloco sh-anel__centro-bloco--normal">
                                        <span class="sh-anel__valor"><c:out value="${nNormal}" /></span>
                                        <span class="sh-anel__rotulo">normais</span>
                                    </span>
                                    <span class="sh-anel__centro-bloco sh-anel__centro-bloco--baixo">
                                        <span class="sh-anel__valor"><c:out value="${nBaixo}" /></span>
                                        <span class="sh-anel__rotulo">no fim</span>
                                    </span>
                                    <span class="sh-anel__centro-bloco sh-anel__centro-bloco--sem">
                                        <span class="sh-anel__valor"><c:out value="${nSem}" /></span>
                                        <span class="sh-anel__rotulo">sem estoque</span>
                                    </span>
                                </div>
                            </div>
                            <ul class="sh-anel__legenda">
                                <li>
                                    <a class="sh-anel__item sh-tom--normal" data-sh-fatia-item="normal"
                                        href="${ctx}/produtos?nivelEstoque=ok&amp;tamanho=25">
                                        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                                        <span class="sh-anel__nome">Normal</span>
                                        <strong class="sh-num"><c:out value="${nNormal}" /></strong>
                                        <span class="sh-anel__pct">(<c:out value="${pNormal}" />%)</span>
                                    </a>
                                </li>
                                <li>
                                    <a class="sh-anel__item sh-tom--baixo" data-sh-fatia-item="baixo"
                                        href="${ctx}/produtos?nivelEstoque=baixo&amp;tamanho=25">
                                        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                                        <span class="sh-anel__nome">Baixo</span>
                                        <strong class="sh-num"><c:out value="${nBaixo}" /></strong>
                                        <span class="sh-anel__pct">(<c:out value="${pBaixo}" />%)</span>
                                    </a>
                                </li>
                                <li>
                                    <a class="sh-anel__item sh-tom--sem" data-sh-fatia-item="sem"
                                        href="${ctx}/produtos?nivelEstoque=semEstoque&amp;tamanho=25">
                                        <i class="bi bi-x-octagon-fill" aria-hidden="true"></i>
                                        <span class="sh-anel__nome">Sem estoque</span>
                                        <strong class="sh-num"><c:out value="${nSem}" /></strong>
                                        <span class="sh-anel__pct">(<c:out value="${pSem}" />%)</span>
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </section>
                </div>
            </c:otherwise>
        </c:choose>

        <%-- ===== Gráfico de 14 dias (SVG feito à mão, dados por fetch) ===== --%>
        <section class="sh-grafico sh-card" aria-labelledby="titulo-grafico" data-sh-grafico
            data-url="<c:out value='${ctx}/api/dashboard/movimentos?dias=14'/>">
            <div class="sh-grafico__cab">
                <div>
                    <h2 class="sh-titulo-secao" id="titulo-grafico">Entradas e saídas, 14 dias</h2>
                    <p class="sh-grafico__resumo" data-grafico-resumo>Carregando os movimentos&hellip;</p>
                </div>
                <ul class="sh-grafico__legenda" aria-hidden="true">
                    <li><span class="sh-grafico__amostra sh-grafico__amostra--entrada"></span>Entrou (acima da linha)</li>
                    <li><span class="sh-grafico__amostra sh-grafico__amostra--saida"></span>Saiu (abaixo)</li>
                </ul>
            </div>
            <div class="sh-grafico__area" data-grafico-area>
                <%-- Estados (o JS mostra um por vez). Sem JavaScript, só o aviso do noscript aparece. --%>
                <div class="sh-grafico__estado" data-grafico-estado="carregando" hidden>
                    <div class="sh-grafico__esqueleto" aria-hidden="true">
                        <span class="sh-esqueleto sh-esq-1"></span><span class="sh-esqueleto sh-esq-2"></span><span class="sh-esqueleto sh-esq-3"></span>
                        <span class="sh-esqueleto sh-esq-4"></span><span class="sh-esqueleto sh-esq-5"></span><span class="sh-esqueleto sh-esq-6"></span>
                        <span class="sh-esqueleto sh-esq-7"></span><span class="sh-esqueleto sh-esq-8"></span><span class="sh-esqueleto sh-esq-9"></span>
                        <span class="sh-esqueleto sh-esq-10"></span><span class="sh-esqueleto sh-esq-11"></span><span class="sh-esqueleto sh-esq-12"></span>
                        <span class="sh-esqueleto sh-esq-13"></span><span class="sh-esqueleto sh-esq-14"></span>
                    </div>
                </div>
                <div class="sh-grafico__estado sh-grafico__estado--msg" data-grafico-estado="erro" hidden>
                    <div>
                        <p class="sh-grafico__msg-titulo">Não foi possível carregar os movimentos.</p>
                        <p class="sh-grafico__msg-texto" data-grafico-erro-texto>O resto da tela continua valendo. Tente de novo em instantes.</p>
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-grafico-tentar>Tentar de novo</button>
                    </div>
                </div>
                <div class="sh-grafico__estado sh-grafico__estado--msg" data-grafico-estado="vazio" hidden>
                    <div>
                        <p class="sh-grafico__msg-titulo">Nenhum movimento nesses 14 dias.</p>
                        <p class="sh-grafico__msg-texto">Quando você registrar entradas e saídas, elas aparecem aqui, dia a dia.</p>
                        <a class="btn btn-primary btn-sm" href="${ctx}/estoque?movTipo=ENTRADA">Registrar uma entrada</a>
                    </div>
                </div>
                <div class="sh-grafico__quadro" data-grafico-quadro hidden></div>
                <div class="sh-grafico__dica" data-grafico-dica aria-hidden="true" hidden></div>
                <noscript><p class="sh-grafico__msg-texto">O gráfico precisa de JavaScript. Os números do dia a dia estão em <a href="${ctx}/estoque">Movimentações</a>.</p></noscript>
            </div>
            <details class="sh-grafico__tabela" data-grafico-tabela hidden>
                <summary>Ver os números em tabela</summary>
                <div class="table-responsive">
                    <table class="table table-sm sh-num">
                        <caption class="visually-hidden">Entradas, saídas e ajustes por dia, nos últimos 14 dias</caption>
                        <thead>
                            <tr><th scope="col">Dia</th><th scope="col" class="text-end">Entradas</th><th scope="col" class="text-end">Saídas</th><th scope="col" class="text-end">Ajustes</th></tr>
                        </thead>
                        <tbody data-grafico-linhas></tbody>
                    </table>
                </div>
            </details>
        </section>

        <%-- ===== Os números da loja (sem caixas: cada número é um link) ===== --%>
        <section class="sh-numeros" aria-labelledby="titulo-totais">
            <h2 class="visually-hidden" id="titulo-totais">Totais da loja</h2>
            <ul class="sh-numeros__lista sh-escalona">
                <li>
                    <a class="sh-numero" href="${ctx}/produtos?tamanho=25">
                        <span class="sh-numero__valor sh-num" data-contar="<c:out value='${resumo.totalProdutos}'/>" data-formato="inteiro"><c:out value="${resumo.totalProdutos}" /></span>
                        <span class="sh-numero__rotulo">produtos</span>
                        <span class="sh-numero__apoio"><c:out value="${nTotal == 0 ? 'cadastre o primeiro' : 'cadastrados'}" /></span>
                    </a>
                </li>
                <li>
                    <a class="sh-numero" href="${ctx}/unidades">
                        <span class="sh-numero__valor sh-num" data-contar="<c:out value='${resumo.quantidadeTotalItens}'/>" data-formato="inteiro"><c:out value="${resumo.quantidadeTotalItens}" /></span>
                        <span class="sh-numero__rotulo">unidades</span>
                        <span class="sh-numero__apoio">soma dos saldos</span>
                    </a>
                </li>
                <li>
                    <a class="sh-numero" href="${ctx}/valor-estoque">
                        <span class="sh-numero__valor sh-num" data-contar="<c:out value='${resumo.valorTotalEstoque}'/>" data-formato="moeda">R$ <fmt:formatNumber value="${resumo.valorTotalEstoque}" pattern="#,##0.00" /></span>
                        <span class="sh-numero__rotulo">em estoque</span>
                        <span class="sh-numero__apoio">preço vezes quantidade</span>
                    </a>
                </li>
                <li>
                    <a class="sh-numero" href="${ctx}/categorias">
                        <span class="sh-numero__valor sh-num" data-contar="<c:out value='${resumo.totalCategorias}'/>" data-formato="inteiro"><c:out value="${resumo.totalCategorias}" /></span>
                        <span class="sh-numero__rotulo">categorias</span>
                        <span class="sh-numero__apoio"><c:out value="${resumo.totalCategorias == 0 ? 'crie a primeira' : 'grupos de produtos'}" /></span>
                    </a>
                </li>
            </ul>
        </section>

        <%-- ===== Chegou por último ===== --%>
        <section class="sh-recentes" aria-labelledby="titulo-recentes">
            <div class="sh-recentes__cab">
                <h2 class="sh-titulo-secao" id="titulo-recentes">Chegou por último</h2>
                <a class="sh-recentes__todos" href="${ctx}/produtos?tamanho=25">Ver todos os produtos</a>
            </div>
            <c:choose>
                <c:when test="${empty produtosRecentes}">
                    <sh:estadoVazio ilustracao="caixa" titulo="Nenhum produto cadastrado"
                        texto="Cadastre o primeiro produto para acompanhar o saldo e receber o aviso de reposição."
                        acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                </c:when>
                <c:otherwise>
                    <ul class="sh-recentes__lista sh-escalona">
                        <c:forEach var="produto" items="${produtosRecentes}">
                            <li class="sh-recente">
                                <div class="sh-recente__info">
                                    <a class="sh-recente__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${produto.nome}" /></a>
                                    <span class="sh-recente__sub"><c:out value="${produto.categoria.nome}" /></span>
                                </div>
                                <span class="sh-recente__preco sh-num">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></span>
                                <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
