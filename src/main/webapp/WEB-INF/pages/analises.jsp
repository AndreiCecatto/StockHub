<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Análises de estoque. Rota: GET /analises?periodo=30|90|365 (qualquer outro valor vira 30).
    Dados do AnaliseController: painel (PainelAnalises, sempre só da loja logada).
    Como funciona: cada gráfico nasce de uma TABELA desta página (a alternativa para leitor de tela e para quem
    está sem JavaScript). O js/analises.js lê a tabela apontada em data-tabela e desenha o SVG; os números
    crus ficam em data-v (ponto decimal) e o texto da célula é o formatado em pt-BR.
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="p" value="${painel.periodo}" />
<c:set var="rotuloPeriodo" value="${p == 365 ? 'no último ano' : (p == 90 ? 'nos últimos 90 dias' : 'nos últimos 30 dias')}" />

<sh:layout titulo="Análises" ativo="analises">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/v3-rel.css">
        <link rel="stylesheet" href="${ctx}/css/analises.css">
        <script defer src="${ctx}/js/analises.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Análises" descricao="Para onde vai o seu estoque e o seu dinheiro." />

        <nav class="v3r-abas an-periodo" aria-label="Período das análises">
            <a class="v3r-abas__item" href="${ctx}/analises?periodo=30" ${p == 30 ? 'aria-current="page"' : ''}>30 dias</a>
            <a class="v3r-abas__item" href="${ctx}/analises?periodo=90" ${p == 90 ? 'aria-current="page"' : ''}>90 dias</a>
            <a class="v3r-abas__item" href="${ctx}/analises?periodo=365" ${p == 365 ? 'aria-current="page"' : ''}>1 ano</a>
        </nav>
        <p class="an-nota-periodo">O período vale para &ldquo;O que mais sai&rdquo; e para &ldquo;Compras e saídas&rdquo;. As outras análises têm janela própria.</p>

        <%-- ===================== 1. Dinheiro parado no estoque ===================== --%>
        <section class="an-secao" aria-labelledby="an-h-valor">
            <header class="an-secao__cab">
                <h2 id="an-h-valor" class="an-secao__titulo">Dinheiro parado no estoque</h2>
                <p class="an-secao__desc">Quanto vale o que você tem guardado hoje: quantidade em estoque vezes o custo de cada produto.</p>
            </header>
            <div class="an-hero">
                <p class="an-hero__valor sh-mono">R$ <fmt:formatNumber value="${painel.valorEstoque}" pattern="#,##0.00" /></p>
                <p class="an-hero__nota">
                    em <fmt:formatNumber value="${painel.produtosComSaldo}" pattern="#,##0" />
                    <c:out value="${painel.produtosComSaldo == 1 ? 'produto com saldo' : 'produtos com saldo'}" />
                    <c:if test="${painel.produtosSemCusto > 0}">
                        <span class="an-aviso"><i class="bi bi-info-circle" aria-hidden="true"></i>
                            <fmt:formatNumber value="${painel.produtosSemCusto}" pattern="#,##0" />
                            <c:out value="${painel.produtosSemCusto == 1 ? 'produto está sem custo informado e não entra na conta' : 'produtos estão sem custo informado e não entram na conta'}" />.
                        </span>
                    </c:if>
                </p>
            </div>
            <c:choose>
                <c:when test="${empty painel.valorPorCategoria}">
                    <p class="an-vazio">Sem produtos com saldo no estoque. Registre uma entrada para ver o valor por categoria.</p>
                </c:when>
                <c:otherwise>
                    <h3 class="an-sub">Por categoria</h3>
                    <figure class="an-fig" data-grafico="barras" data-tabela="an-t-valor" data-formato="moeda" data-coluna="3"
                            data-rotulo-acessivel="Dinheiro parado por categoria">
                        <div class="an-fig__area"></div>
                    </figure>
                    <details class="an-tabela" open>
                        <summary>Ver como tabela</summary>
                        <div class="sh-table-wrap">
                            <table class="sh-table" id="an-t-valor">
                                <caption class="visually-hidden">Dinheiro parado no estoque por categoria</caption>
                                <thead><tr>
                                    <th scope="col">Categoria</th>
                                    <th scope="col" class="sh-col-num">Produtos</th>
                                    <th scope="col" class="sh-col-num">Unidades</th>
                                    <th scope="col" class="sh-col-num">Valor</th>
                                </tr></thead>
                                <tbody>
                                    <c:forEach var="cat" items="${painel.valorPorCategoria}">
                                        <tr>
                                            <th scope="row"><c:out value="${cat.categoria}" /></th>
                                            <td class="sh-col-num sh-mono" data-v="${cat.produtos}"><fmt:formatNumber value="${cat.produtos}" pattern="#,##0" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${cat.unidades}"><fmt:formatNumber value="${cat.unidades}" pattern="#,##0" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${cat.valor}">R$ <fmt:formatNumber value="${cat.valor}" pattern="#,##0.00" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- ===================== 2. O que mais sai ===================== --%>
        <section class="an-secao" aria-labelledby="an-h-saidas">
            <header class="an-secao__cab">
                <h2 id="an-h-saidas" class="an-secao__titulo">O que mais sai</h2>
                <p class="an-secao__desc">O que saiu do estoque <c:out value="${rotuloPeriodo}" />, pelo custo dos produtos na data da saída.</p>
            </header>
            <c:choose>
                <c:when test="${painel.unidadesSaidas == 0}">
                    <p class="an-vazio">Nenhuma saída <c:out value="${rotuloPeriodo}" />. Quando houver, o ritmo de saída aparece aqui.</p>
                </c:when>
                <c:otherwise>
                    <dl class="an-numeros">
                        <div class="an-numero">
                            <dt>Unidades que saíram</dt>
                            <dd class="sh-mono"><fmt:formatNumber value="${painel.unidadesSaidas}" pattern="#,##0" /></dd>
                        </div>
                        <div class="an-numero">
                            <dt>Valor que saiu</dt>
                            <dd class="sh-mono">R$ <fmt:formatNumber value="${painel.valorSaidas}" pattern="#,##0.00" /></dd>
                        </div>
                    </dl>

                    <h3 class="an-sub">Saídas por dia</h3>
                    <figure class="an-fig" data-grafico="colunas" data-tabela="an-t-dia" data-formato="inteiro" data-coluna="1"
                            data-unidade="unidades" data-rotulo-acessivel="Unidades que saíram por dia">
                        <div class="an-fig__area"></div>
                    </figure>
                    <details class="an-tabela" open>
                        <summary>Ver como tabela (<c:out value="${fn:length(painel.saidasPorDia)}" /> dias)</summary>
                        <div class="sh-table-wrap an-tabela__rolagem">
                            <table class="sh-table" id="an-t-dia">
                                <caption class="visually-hidden">Saídas por dia <c:out value="${rotuloPeriodo}" /></caption>
                                <thead><tr>
                                    <th scope="col">Dia</th>
                                    <th scope="col" class="sh-col-num">Unidades</th>
                                    <th scope="col" class="sh-col-num">Valor</th>
                                </tr></thead>
                                <tbody>
                                    <c:forEach var="d" items="${painel.saidasPorDia}">
                                        <c:set var="iso" value="${d.dia}" />
                                        <tr>
                                            <th scope="row" data-r="${iso}"><c:out value="${fn:substring(iso, 8, 10)}/${fn:substring(iso, 5, 7)}/${fn:substring(iso, 0, 4)}" /></th>
                                            <td class="sh-col-num sh-mono" data-v="${d.unidades}"><fmt:formatNumber value="${d.unidades}" pattern="#,##0" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${d.valor}">R$ <fmt:formatNumber value="${d.valor}" pattern="#,##0.00" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>

                    <div class="an-duas">
                        <div>
                            <h3 class="an-sub">Os 10 produtos que mais saem</h3>
                            <figure class="an-fig" data-grafico="barras" data-tabela="an-t-top" data-formato="inteiro" data-coluna="1"
                                    data-unidade="unidades" data-rotulo-acessivel="Produtos que mais saem, em unidades">
                                <div class="an-fig__area"></div>
                            </figure>
                            <details class="an-tabela" open>
                                <summary>Ver como tabela</summary>
                                <div class="sh-table-wrap">
                                    <table class="sh-table" id="an-t-top">
                                        <caption class="visually-hidden">Produtos que mais saem <c:out value="${rotuloPeriodo}" /></caption>
                                        <thead><tr>
                                            <th scope="col">Produto</th>
                                            <th scope="col" class="sh-col-num">Unidades</th>
                                            <th scope="col" class="sh-col-num">Valor</th>
                                        </tr></thead>
                                        <tbody>
                                            <c:forEach var="t" items="${painel.topProdutos}">
                                                <tr>
                                                    <th scope="row"><c:out value="${t.nome}" /></th>
                                                    <td class="sh-col-num sh-mono" data-v="${t.unidades}"><fmt:formatNumber value="${t.unidades}" pattern="#,##0" /></td>
                                                    <td class="sh-col-num sh-mono" data-v="${t.valor}">R$ <fmt:formatNumber value="${t.valor}" pattern="#,##0.00" /></td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </details>
                        </div>
                        <div>
                            <h3 class="an-sub">Por categoria</h3>
                            <figure class="an-fig" data-grafico="barras" data-tabela="an-t-scat" data-formato="moeda" data-coluna="2"
                                    data-rotulo-acessivel="Valor que saiu por categoria">
                                <div class="an-fig__area"></div>
                            </figure>
                            <details class="an-tabela" open>
                                <summary>Ver como tabela</summary>
                                <div class="sh-table-wrap">
                                    <table class="sh-table" id="an-t-scat">
                                        <caption class="visually-hidden">Saídas por categoria <c:out value="${rotuloPeriodo}" /></caption>
                                        <thead><tr>
                                            <th scope="col">Categoria</th>
                                            <th scope="col" class="sh-col-num">Unidades</th>
                                            <th scope="col" class="sh-col-num">Valor</th>
                                        </tr></thead>
                                        <tbody>
                                            <c:forEach var="s" items="${painel.saidasPorCategoria}">
                                                <tr>
                                                    <th scope="row"><c:out value="${s.categoria}" /></th>
                                                    <td class="sh-col-num sh-mono" data-v="${s.unidades}"><fmt:formatNumber value="${s.unidades}" pattern="#,##0" /></td>
                                                    <td class="sh-col-num sh-mono" data-v="${s.valor}">R$ <fmt:formatNumber value="${s.valor}" pattern="#,##0.00" /></td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </details>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- ===================== 3. Compras e saídas ===================== --%>
        <section class="an-secao" aria-labelledby="an-h-caixa">
            <header class="an-secao__cab">
                <h2 id="an-h-caixa" class="an-secao__titulo">Compras e saídas</h2>
                <p class="an-secao__desc">O que entrou por compra e o que saiu do estoque nos últimos <c:out value="${painel.mesesCaixa}" /> meses, sempre pelo custo do produto. O estoque inicial não conta como compra.</p>
            </header>
            <c:set var="temCaixa" value="false" />
            <c:forEach var="m" items="${painel.caixa}">
                <c:if test="${m.compras.signum() != 0 or m.saidas.signum() != 0}"><c:set var="temCaixa" value="true" /></c:if>
            </c:forEach>
            <c:choose>
                <c:when test="${not temCaixa}">
                    <p class="an-vazio">Sem compras nem saídas nesses meses.</p>
                </c:when>
                <c:otherwise>
                    <figure class="an-fig" data-grafico="duplas" data-tabela="an-t-caixa" data-formato="moeda" data-colunas="1,2"
                            data-rotulo-acessivel="Compras e saídas por mês">
                        <div class="an-fig__area"></div>
                    </figure>
                    <details class="an-tabela" open>
                        <summary>Ver como tabela</summary>
                        <div class="sh-table-wrap">
                            <table class="sh-table" id="an-t-caixa">
                                <caption class="visually-hidden">Compras e saídas por mês</caption>
                                <thead><tr>
                                    <th scope="col">Mês</th>
                                    <th scope="col" class="sh-col-num">Compras</th>
                                    <th scope="col" class="sh-col-num">Saídas</th>
                                </tr></thead>
                                <tbody>
                                    <c:forEach var="m" items="${painel.caixa}">
                                        <c:set var="iso" value="${m.mes}" />
                                        <tr>
                                            <th scope="row" data-r="${iso}"><c:out value="${fn:substring(iso, 5, 7)}/${fn:substring(iso, 0, 4)}" /></th>
                                            <td class="sh-col-num sh-mono" data-v="${m.compras}">R$ <fmt:formatNumber value="${m.compras}" pattern="#,##0.00" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${m.saidas}">R$ <fmt:formatNumber value="${m.saidas}" pattern="#,##0.00" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- ===================== 4. Vai acabar em breve ===================== --%>
        <section class="an-secao" aria-labelledby="an-h-acabar">
            <header class="an-secao__cab">
                <h2 id="an-h-acabar" class="an-secao__titulo">Vai acabar em breve</h2>
                <p class="an-secao__desc">Por quantos dias o saldo de cada produto dura, no ritmo de saída dos últimos 30 dias. Abaixo de 7 dias, vale repor.</p>
            </header>
            <c:choose>
                <c:when test="${empty painel.cobertura}">
                    <p class="an-vazio">Nenhum produto com saldo teve saída nos últimos 30 dias, então ainda não dá para estimar.</p>
                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test="${painel.totalEmAlerta > 0}">
                            <p class="an-situacao an-situacao--alerta" role="status">
                                <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                                <fmt:formatNumber value="${painel.totalEmAlerta}" pattern="#,##0" />
                                <c:out value="${painel.totalEmAlerta == 1 ? 'produto acaba' : 'produtos acabam'}" /> em menos de 7 dias.
                            </p>
                        </c:when>
                        <c:otherwise>
                            <p class="an-situacao an-situacao--ok" role="status">
                                <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                                Nenhum produto acaba em menos de 7 dias.
                            </p>
                        </c:otherwise>
                    </c:choose>
                    <figure class="an-fig" data-grafico="barras" data-tabela="an-t-cobertura" data-formato="dias" data-coluna="3"
                            data-limite="7" data-maximo="60" data-rotulo-acessivel="Dias que o saldo de cada produto dura">
                        <div class="an-fig__area"></div>
                    </figure>
                    <p class="an-legenda">A linha marca 7 dias. As barras terminam em 60 dias, mesmo que o saldo dure mais.</p>
                    <details class="an-tabela" open>
                        <summary>Ver como tabela</summary>
                        <div class="sh-table-wrap">
                            <table class="sh-table" id="an-t-cobertura">
                                <caption class="visually-hidden">Dias de cobertura por produto, dos que acabam primeiro aos que duram mais</caption>
                                <thead><tr>
                                    <th scope="col">Produto</th>
                                    <th scope="col" class="sh-col-num">Saldo</th>
                                    <th scope="col" class="sh-col-num">Sai por dia</th>
                                    <th scope="col" class="sh-col-num">Dura</th>
                                    <th scope="col">Situação</th>
                                </tr></thead>
                                <tbody>
                                    <c:forEach var="i" items="${painel.cobertura}">
                                        <tr ${i.alerta ? 'data-alerta' : ''}>
                                            <th scope="row"><c:out value="${i.nome}" /></th>
                                            <td class="sh-col-num sh-mono" data-v="${i.saldo}"><fmt:formatNumber value="${i.saldo}" pattern="#,##0" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${i.mediaDiaria}"><fmt:formatNumber value="${i.mediaDiaria}" pattern="#,##0.00" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${i.dias}"><fmt:formatNumber value="${i.dias}" pattern="#,##0.0" /> dias</td>
                                            <td><c:out value="${i.alerta ? 'Acaba em breve' : 'Tranquilo'}" /></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- ===================== 5. Parados ===================== --%>
        <section class="an-secao" aria-labelledby="an-h-parados">
            <header class="an-secao__cab">
                <h2 id="an-h-parados" class="an-secao__titulo">Parados há 60 dias ou mais</h2>
                <p class="an-secao__desc">Produtos com saldo em que nada saiu em pelo menos 60 dias. Os mais caros aparecem primeiro.</p>
            </header>
            <c:choose>
                <c:when test="${empty painel.parados}">
                    <p class="an-situacao an-situacao--ok" role="status">
                        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                        Nenhum produto parado. Tudo o que está no estoque saiu nos últimos 60 dias.
                    </p>
                </c:when>
                <c:otherwise>
                    <p class="an-resumo-parados">
                        <strong class="sh-mono">R$ <fmt:formatNumber value="${painel.valorParado}" pattern="#,##0.00" /></strong>
                        parados em <fmt:formatNumber value="${painel.totalParados}" pattern="#,##0" />
                        <c:out value="${painel.totalParados == 1 ? 'produto' : 'produtos'}" />.
                        <c:if test="${painel.totalParados > fn:length(painel.parados)}">
                            A lista mostra os <c:out value="${fn:length(painel.parados)}" /> de maior valor.
                        </c:if>
                    </p>
                    <div class="sh-table-wrap">
                        <table class="sh-table" id="an-t-parados">
                            <caption class="visually-hidden">Produtos parados há 60 dias ou mais</caption>
                            <thead><tr>
                                <th scope="col">Produto</th>
                                <th scope="col" class="sh-col-num">Saldo</th>
                                <th scope="col" class="sh-col-num">Valor parado</th>
                                <th scope="col" class="sh-col-num">Última saída</th>
                                <th scope="col" class="sh-col-num">Parado há</th>
                            </tr></thead>
                            <tbody>
                                <c:forEach var="x" items="${painel.parados}">
                                    <tr>
                                        <th scope="row"><a class="an-link" href="<c:out value='${ctx}/produtos/${x.produtoId}'/>" data-sh-peek data-sh-peek-titulo="<c:out value='${x.nome}'/>"><c:out value="${x.nome}" /></a></th>
                                        <td class="sh-col-num sh-mono"><fmt:formatNumber value="${x.saldo}" pattern="#,##0" /></td>
                                        <td class="sh-col-num sh-mono">R$ <fmt:formatNumber value="${x.valor}" pattern="#,##0.00" /></td>
                                        <td class="sh-col-num sh-mono">
                                            <c:choose>
                                                <c:when test="${x.ultimaSaida != null}">
                                                    <c:set var="iso" value="${x.ultimaSaida}" />
                                                    <c:out value="${fn:substring(iso, 8, 10)}/${fn:substring(iso, 5, 7)}/${fn:substring(iso, 0, 4)}" />
                                                </c:when>
                                                <c:otherwise>nunca saiu</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="sh-col-num sh-mono">
                                            <c:choose>
                                                <c:when test="${x.diasParado != null}"><fmt:formatNumber value="${x.diasParado}" pattern="#,##0" /> dias</c:when>
                                                <c:otherwise>sem histórico</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- ===================== 6. Curva ABC ===================== --%>
        <section class="an-secao" aria-labelledby="an-h-abc">
            <header class="an-secao__cab">
                <h2 id="an-h-abc" class="an-secao__titulo">Os produtos que mais pesam</h2>
                <p class="an-secao__desc">Nos últimos 90 dias, quais produtos concentram o valor que saiu. A: os que somam os primeiros 80%. B: até 95%. C: o resto.</p>
            </header>
            <c:choose>
                <c:when test="${empty painel.curvaAbc}">
                    <p class="an-vazio">Sem saídas com custo informado nos últimos 90 dias.</p>
                </c:when>
                <c:otherwise>
                    <p class="an-resumo-parados">
                        <strong class="sh-mono"><c:out value="${painel.produtosClasseA}" /></strong>
                        <c:out value="${painel.produtosClasseA == 1 ? 'produto responde' : 'produtos respondem'}" />
                        por <strong class="sh-mono"><fmt:formatNumber value="${painel.percentualClasseA}" pattern="#,##0" />%</strong> do valor que saiu.
                    </p>
                    <figure class="an-fig" data-grafico="pilha" data-tabela="an-t-abc" data-coluna="3" data-classe="1"
                            data-rotulo-acessivel="Parte do valor de saída em cada classe da curva ABC">
                        <div class="an-fig__area"></div>
                    </figure>
                    <details class="an-tabela">
                        <summary>Ver como tabela (<c:out value="${fn:length(painel.curvaAbc)}" /> produtos)</summary>
                        <div class="sh-table-wrap an-tabela__rolagem">
                            <table class="sh-table" id="an-t-abc">
                                <caption class="visually-hidden">Curva ABC por valor de saída em 90 dias</caption>
                                <thead><tr>
                                    <th scope="col">Produto</th>
                                    <th scope="col">Classe</th>
                                    <th scope="col" class="sh-col-num">Valor que saiu</th>
                                    <th scope="col" class="sh-col-num">Parte do total</th>
                                    <th scope="col" class="sh-col-num">Soma até aqui</th>
                                </tr></thead>
                                <tbody>
                                    <c:forEach var="i" items="${painel.curvaAbc}">
                                        <tr>
                                            <th scope="row"><c:out value="${i.nome}" /></th>
                                            <td data-c="${i.classe}"><c:out value="${i.classe}" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${i.valor}">R$ <fmt:formatNumber value="${i.valor}" pattern="#,##0.00" /></td>
                                            <td class="sh-col-num sh-mono" data-v="${i.percentual}"><fmt:formatNumber value="${i.percentual}" pattern="#,##0.0" />%</td>
                                            <td class="sh-col-num sh-mono" data-v="${i.acumulado}"><fmt:formatNumber value="${i.acumulado}" pattern="#,##0.0" />%</td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
