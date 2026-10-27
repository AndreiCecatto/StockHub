<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Início "Visão geral" (v3: narrativa de rolagem sóbria). Rota "/dashboard" ("/" público é a apresentação).
    Dados do DashboardController (inalterados): resumo (totalProdutos, totalCategorias, produtosEstoqueBaixo que INCLUI o
    sem estoque, produtosSemEstoque, quantidadeTotalItens, valorTotalEstoque), produtosRecentes (6) e produtosEstoqueBaixo
    (6 de menor saldo; cada Produto traz categoria.nome e nivelEstoque).
    Ordem: (1) topo compacto: título, frase de estado e quatro indicadores que contam uma vez, tudo visível sem rolar;
    (2) "Requer ação" (reposição rápida por POST /estoque/movimentar, com CSRF) ao lado da "Saúde do estoque" (barra empilhada
    que cresce ao aparecer e destaca a fatia); (3) movimento de 14 dias (gráfico SVG de js/inicio.js, que se desenha ao
    rolar); (4) últimos produtos. Sem cenas fixas: o uso diário não depende de rolar. Estados: loja vazia (primeiros passos),
    tudo em dia, só Baixo e com ruptura.
    Dados do gráfico por fetch: GET /api/dashboard/movimentos?dias=14 -> [{dia, entradas, saidas, ajustes}] em UNIDADES.
    A data de hoje é escrita no navegador (js/tempo.js); o texto do servidor é o plano B.
    Sem script nem style inline: números animados por js/numeros.js (data-contar); revelações pelo kit de rolagem.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Distribuição Normal / Baixo / Sem estoque. produtosEstoqueBaixo inclui os sem estoque. --%>
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

<sh:layout titulo="Início" ativo="dashboard">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/inicio.css">
        <script defer src="${ctx}/js/inicio.js"></script>
    </jsp:attribute>
    <jsp:body>
        <%-- ===== Topo compacto: título, frase de estado e indicadores ===== --%>
        <section class="in-topo" aria-labelledby="titulo-saudacao">
            <div class="in-topo__linha">
                <div class="in-topo__texto">
                    <p class="sh-eyebrow" data-sh-data-hoje></p>
                    <h1 class="sh-titulo-pagina" id="titulo-saudacao">Visão geral</h1>
                    <p class="in-topo__frase">
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
                </div>
                <div class="in-topo__acoes">
                    <a class="btn btn-primary" href="${ctx}/estoque?movTipo=ENTRADA"><i class="bi bi-plus-lg" aria-hidden="true"></i> Registrar entrada</a>
                    <a class="btn btn-outline-secondary" href="${ctx}/estoque?movTipo=SAIDA">Registrar saída</a>
                    <a class="btn btn-ghost" href="${ctx}/produtos/novo">Novo produto</a>
                </div>
            </div>

            <h2 class="visually-hidden">Indicadores da loja</h2>
            <ul class="in-kpis">
                <li>
                    <a class="in-kpi" href="${ctx}/valor-estoque">
                        <span class="in-kpi__rotulo">Valor em estoque</span>
                        <span class="in-kpi__valor sh-mono" data-contar="<c:out value='${resumo.valorTotalEstoque}'/>" data-formato="moeda">R$ <fmt:formatNumber value="${resumo.valorTotalEstoque}" pattern="#,##0.00" /></span>
                        <span class="in-kpi__apoio">preço vezes quantidade</span>
                    </a>
                </li>
                <li>
                    <a class="in-kpi" href="${ctx}/unidades">
                        <span class="in-kpi__rotulo">Unidades</span>
                        <span class="in-kpi__valor sh-mono" data-contar="<c:out value='${resumo.quantidadeTotalItens}'/>" data-formato="inteiro"><c:out value="${resumo.quantidadeTotalItens}" /></span>
                        <span class="in-kpi__apoio">soma dos saldos</span>
                    </a>
                </li>
                <li>
                    <a class="in-kpi" href="${ctx}/produtos?tamanho=25">
                        <span class="in-kpi__rotulo">Produtos</span>
                        <span class="in-kpi__valor sh-mono" data-contar="<c:out value='${resumo.totalProdutos}'/>" data-formato="inteiro"><c:out value="${resumo.totalProdutos}" /></span>
                        <span class="in-kpi__apoio"><c:choose>
                                <c:when test="${nTotal == 0}">cadastre o primeiro</c:when>
                                <c:otherwise>em <c:out value="${resumo.totalCategorias}" /> <c:out value="${resumo.totalCategorias == 1 ? 'categoria' : 'categorias'}" /></c:otherwise>
                            </c:choose></span>
                    </a>
                </li>
                <li>
                    <a class="in-kpi ${nSem > 0 ? 'in-kpi--sem' : (nReposicao > 0 ? 'in-kpi--baixo' : '')}" href="${ctx}/estoque-baixo">
                        <span class="in-kpi__rotulo">A repor</span>
                        <span class="in-kpi__valor sh-mono" data-contar="<c:out value='${nReposicao}'/>" data-formato="inteiro"><c:out value="${nReposicao}" /></span>
                        <span class="in-kpi__apoio">
                            <c:choose>
                                <c:when test="${nReposicao == 0}">tudo em dia</c:when>
                                <c:when test="${nSem > 0}"><c:out value="${nSem}" /> sem estoque</c:when>
                                <c:otherwise>no mínimo ou abaixo</c:otherwise>
                            </c:choose>
                        </span>
                    </a>
                </li>
            </ul>
        </section>

        <c:choose>
            <%-- ===== Loja nova: primeiros passos ===== --%>
            <c:when test="${nTotal == 0}">
                <section class="in-secao sh-revelar" aria-labelledby="titulo-passos">
                    <h2 class="sh-titulo-secao-v3" id="titulo-passos">Primeiros passos</h2>
                    <p class="in-sub">Em três etapas a sua loja está pronta para usar.</p>
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
                <div class="in-grade">
                    <%-- ===== Requer ação ===== --%>
                    <section class="in-secao in-acao" aria-labelledby="titulo-hoje">
                        <div class="in-secao__cab">
                            <h2 class="sh-titulo-secao-v3" id="titulo-hoje">Requer ação</h2>
                            <c:if test="${nSem > 0 and nBaixo > 0}">
                                <div class="in-filtro" role="group" aria-label="Filtrar a lista" data-in-filtro>
                                    <button type="button" class="in-filtro__op" data-filtro="todos" aria-pressed="true">Todos</button>
                                    <button type="button" class="in-filtro__op" data-filtro="sem" aria-pressed="false">Sem estoque</button>
                                    <button type="button" class="in-filtro__op" data-filtro="baixo" aria-pressed="false">Baixo</button>
                                </div>
                            </c:if>
                        </div>
                        <c:choose>
                            <c:when test="${nReposicao == 0}">
                                <div class="in-ok sh-revelar">
                                    <i class="bi bi-check-circle" aria-hidden="true"></i>
                                    <div>
                                        <p class="in-ok__titulo">Nada para repor.</p>
                                        <p class="in-ok__texto">Todos os <c:out value="${nTotal}" /> produtos estão acima do mínimo.</p>
                                        <a class="btn btn-outline-secondary btn-sm" href="${ctx}/produtos/novo">Novo produto</a>
                                    </div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <p class="in-sub">
                                    <span class="sh-num in-sub__num"><c:out value="${nReposicao}" /></span>
                                    <c:out value="${nReposicao == 1 ? 'produto no mínimo ou abaixo' : 'produtos no mínimo ou abaixo'}" />,
                                    do mais urgente para o menos.
                                </p>
                                <p class="visually-hidden" role="status" aria-live="polite" data-in-filtro-status></p>
                                <ul class="in-tarefas sh-revelar-grupo" data-in-tarefas>
                                    <c:forEach var="produto" items="${produtosEstoqueBaixo}">
                                        <%-- Sugestão de quantidade: leva o saldo a 2x o mínimo (o medidor fica cheio); sem mínimo, 10 --%>
                                        <c:set var="sugestao" value="${produto.estoqueMinimo > 0 ? produto.estoqueMinimo * 2 - produto.quantidade : 10}" />
                                        <c:if test="${sugestao < 1}"><c:set var="sugestao" value="1" /></c:if>
                                        <li class="in-tarefa in-tarefa--${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sem' : 'baixo'}"
                                            data-nivel="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sem' : 'baixo'}">
                                            <div class="in-tarefa__info">
                                                <a class="in-tarefa__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"
                                                    data-sh-peek data-sh-peek-titulo="<c:out value='${produto.nome}'/>"><c:out value="${produto.nome}" /></a>
                                                <span class="in-tarefa__sub">
                                                    <c:out value="${produto.categoria.nome}" /> &middot;
                                                    <strong class="sh-mono"><c:out value="${produto.quantidade}" /></strong> un. (mínimo <span class="sh-mono"><c:out value="${produto.estoqueMinimo}" /></span>)
                                                </span>
                                            </div>
                                            <div class="in-tarefa__estado">
                                                <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                                                <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                            </div>
                                            <%-- Reposição rápida: o mesmo POST da tela de movimentações (entrada), com a quantidade sugerida --%>
                                            <form class="in-repor" method="post" action="${ctx}/estoque/movimentar" data-sh-bloquear data-sh-enviando="Repondo…">
                                                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                <input type="hidden" name="produtoId" value="<c:out value='${produto.id}'/>">
                                                <input type="hidden" name="tipo" value="ENTRADA">
                                                <input type="hidden" name="observacao" value="Reposição rápida pelo início">
                                                <label class="visually-hidden" for="repor-<c:out value='${produto.id}'/>">Quantidade a repor de <c:out value="${produto.nome}" /></label>
                                                <input class="form-control in-repor__qtd sh-mono" id="repor-<c:out value='${produto.id}'/>" type="number" name="quantidade"
                                                    min="1" max="1000000" step="1" inputmode="numeric" required value="<c:out value='${sugestao}'/>">
                                                <button type="submit" class="btn btn-primary btn-sm">Repor</button>
                                            </form>
                                        </li>
                                    </c:forEach>
                                </ul>
                                <c:if test="${nReposicao > 6}">
                                    <p class="in-mais">
                                        <a href="${ctx}/estoque-baixo">Ver os outros <c:out value="${nReposicao - 6}" /> na lista completa</a>
                                    </p>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </section>

                    <%-- ===== Saúde do estoque: barra empilhada que cresce ao aparecer ===== --%>
                    <section class="in-secao in-saude" aria-labelledby="titulo-saude">
                        <h2 class="sh-titulo-secao-v3" id="titulo-saude">Saúde do estoque</h2>
                        <div class="in-saude__cartao sh-card sh-revelar" data-sh-anel>
                            <p class="in-saude__grande">
                                <span class="sh-mono in-saude__pct" data-contar="<c:out value='${pNormal}'/>" data-formato="percentual"><c:out value="${pNormal}" /> %</span>
                                <span class="in-saude__rotulo">dos produtos estão em dia</span>
                            </p>
                            <%-- A barra é decorativa; a informação está na lista abaixo, em texto --%>
                            <svg class="in-saude__barra sh-desenhar" viewBox="0 0 100 10" preserveAspectRatio="none" aria-hidden="true" focusable="false">
                                <rect class="in-saude__trilho" x="0" y="0" width="100" height="10"/>
                                <c:if test="${nNormal > 0}">
                                    <rect class="in-saude__seg in-saude__seg--normal sh-barra sh-barra--h" data-sh-fatia="normal" x="0" y="0" width="<c:out value='${pNormal}'/>" height="10"/>
                                </c:if>
                                <c:if test="${nBaixo > 0}">
                                    <rect class="in-saude__seg in-saude__seg--baixo sh-barra sh-barra--h" data-sh-fatia="baixo" x="<c:out value='${pNormal}'/>" y="0" width="<c:out value='${pBaixo}'/>" height="10"/>
                                </c:if>
                                <c:if test="${nSem > 0}">
                                    <rect class="in-saude__seg in-saude__seg--sem sh-barra sh-barra--h" data-sh-fatia="sem" x="<c:out value='${pNormal + pBaixo}'/>" y="0" width="<c:out value='${pSem}'/>" height="10"/>
                                </c:if>
                            </svg>
                            <ul class="in-saude__legenda">
                                <li>
                                    <a class="in-saude__item sh-tom--normal" data-sh-fatia-item="normal" href="${ctx}/produtos?nivelEstoque=ok&amp;tamanho=25">
                                        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                                        <span class="in-saude__nome">Normal</span>
                                        <strong class="sh-mono"><c:out value="${nNormal}" /></strong>
                                        <span class="in-saude__p">(<c:out value="${pNormal}" />%)</span>
                                    </a>
                                </li>
                                <li>
                                    <a class="in-saude__item sh-tom--baixo" data-sh-fatia-item="baixo" href="${ctx}/produtos?nivelEstoque=baixo&amp;tamanho=25">
                                        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                                        <span class="in-saude__nome">Baixo</span>
                                        <strong class="sh-mono"><c:out value="${nBaixo}" /></strong>
                                        <span class="in-saude__p">(<c:out value="${pBaixo}" />%)</span>
                                    </a>
                                </li>
                                <li>
                                    <a class="in-saude__item sh-tom--sem" data-sh-fatia-item="sem" href="${ctx}/produtos?nivelEstoque=semEstoque&amp;tamanho=25">
                                        <i class="bi bi-x-octagon-fill" aria-hidden="true"></i>
                                        <span class="in-saude__nome">Sem estoque</span>
                                        <strong class="sh-mono"><c:out value="${nSem}" /></strong>
                                        <span class="in-saude__p">(<c:out value="${pSem}" />%)</span>
                                    </a>
                                </li>
                            </ul>
                            <p class="in-saude__dica">Cada linha abre a lista de produtos com esse filtro.</p>
                        </div>
                    </section>
                </div>
            </c:otherwise>
        </c:choose>

        <%-- ===== Movimento de 14 dias (SVG feito à mão, dados por fetch, se desenha ao rolar) ===== --%>
        <section class="in-secao in-grafico sh-card sh-revelar" aria-labelledby="titulo-grafico" data-sh-grafico
            data-url="<c:out value='${ctx}/api/dashboard/movimentos?dias=14'/>">
            <div class="in-grafico__cab">
                <div>
                    <h2 class="sh-titulo-secao-v3" id="titulo-grafico">Movimento, 14 dias</h2>
                    <p class="in-grafico__resumo" data-grafico-resumo>Carregando os movimentos&hellip;</p>
                </div>
                <ul class="in-grafico__legenda" aria-hidden="true">
                    <li><span class="in-grafico__amostra in-grafico__amostra--entrada"></span>Entrou (acima da linha)</li>
                    <li><span class="in-grafico__amostra in-grafico__amostra--saida"></span>Saiu (abaixo)</li>
                    <li><span class="in-grafico__amostra in-grafico__amostra--ajuste"></span>Ajuste</li>
                </ul>
            </div>
            <div class="in-grafico__area" data-grafico-area>
                <%-- Estados (o JS mostra um por vez). Sem JavaScript, só o aviso do noscript aparece. --%>
                <div class="in-grafico__estado" data-grafico-estado="carregando" hidden>
                    <div class="in-grafico__esqueleto" aria-hidden="true">
                        <span class="sh-esqueleto in-esq-1"></span><span class="sh-esqueleto in-esq-2"></span><span class="sh-esqueleto in-esq-3"></span>
                        <span class="sh-esqueleto in-esq-4"></span><span class="sh-esqueleto in-esq-5"></span><span class="sh-esqueleto in-esq-6"></span>
                        <span class="sh-esqueleto in-esq-7"></span><span class="sh-esqueleto in-esq-8"></span><span class="sh-esqueleto in-esq-9"></span>
                        <span class="sh-esqueleto in-esq-10"></span><span class="sh-esqueleto in-esq-11"></span><span class="sh-esqueleto in-esq-12"></span>
                        <span class="sh-esqueleto in-esq-13"></span><span class="sh-esqueleto in-esq-14"></span>
                    </div>
                </div>
                <div class="in-grafico__estado in-grafico__estado--msg" data-grafico-estado="erro" hidden>
                    <div>
                        <p class="in-grafico__msg-titulo">Não foi possível carregar os movimentos.</p>
                        <p class="in-grafico__msg-texto" data-grafico-erro-texto>O resto da tela continua valendo. Tente de novo em instantes.</p>
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-grafico-tentar>Tentar de novo</button>
                    </div>
                </div>
                <div class="in-grafico__estado in-grafico__estado--msg" data-grafico-estado="vazio" hidden>
                    <div>
                        <p class="in-grafico__msg-titulo">Nenhum movimento nesses 14 dias.</p>
                        <p class="in-grafico__msg-texto">Quando você registrar entradas e saídas, elas aparecem aqui, dia a dia.</p>
                        <a class="btn btn-primary btn-sm" href="${ctx}/estoque?movTipo=ENTRADA">Registrar uma entrada</a>
                    </div>
                </div>
                <div class="in-grafico__quadro" data-grafico-quadro hidden></div>
                <div class="in-grafico__dica" data-grafico-dica aria-hidden="true" hidden></div>
                <noscript><p class="in-grafico__msg-texto">O gráfico precisa de JavaScript. Os números do dia a dia estão em <a href="${ctx}/estoque">Movimentações</a>.</p></noscript>
            </div>
            <details class="in-grafico__tabela" data-grafico-tabela hidden>
                <summary>Ver os números em tabela</summary>
                <div class="table-responsive">
                    <table class="table table-sm sh-num">
                        <caption class="visually-hidden">Entradas, saídas e ajustes por dia, em unidades, nos últimos 14 dias</caption>
                        <thead>
                            <tr><th scope="col">Dia</th><th scope="col" class="text-end">Entradas</th><th scope="col" class="text-end">Saídas</th><th scope="col" class="text-end">Ajustes</th></tr>
                        </thead>
                        <tbody data-grafico-linhas></tbody>
                    </table>
                </div>
            </details>
        </section>

        <%-- ===== Últimos produtos ===== --%>
        <section class="in-secao in-recentes" aria-labelledby="titulo-recentes">
            <div class="in-secao__cab">
                <h2 class="sh-titulo-secao-v3" id="titulo-recentes">Últimos produtos</h2>
                <a class="in-recentes__todos" href="${ctx}/produtos?tamanho=25">Ver todos os produtos</a>
            </div>
            <c:choose>
                <c:when test="${empty produtosRecentes}">
                    <sh:estadoVazio ilustracao="caixa" titulo="Nenhum produto cadastrado"
                        texto="Cadastre o primeiro produto para acompanhar o saldo e receber o aviso de reposição."
                        acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                </c:when>
                <c:otherwise>
                    <ul class="in-recentes__lista sh-revelar-grupo">
                        <c:forEach var="produto" items="${produtosRecentes}">
                            <li class="in-recente">
                                <div class="in-recente__info">
                                    <a class="in-recente__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"
                                        data-sh-peek data-sh-peek-titulo="<c:out value='${produto.nome}'/>"><c:out value="${produto.nome}" /></a>
                                    <span class="in-recente__sub"><c:out value="${produto.categoria.nome}" /></span>
                                </div>
                                <span class="in-recente__preco sh-mono">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></span>
                                <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
