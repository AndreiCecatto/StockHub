<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Início, "a capa do dia" (rota "/dashboard"; "/" público é a apresentação). Estilo: css/dashboard.css; gráfico: js/dashboard.js.
    Dados do DashboardController: resumo (totalProdutos, totalCategorias, produtosEstoqueBaixo que INCLUI o sem estoque,
    produtosSemEstoque, quantidadeTotalItens, valorTotalEstoque), produtosRecentes (6), produtosEstoqueBaixo (6 de menor
    saldo; cada Produto traz categoria.nome e nivelEstoque), movimentacoesRecentes (8, produto já carregado) e, só nas
    lojas com o módulo OS, osPorStatus (Map StatusOs -> total) e osAndamento (até 5 OS aprovadas, em execução ou concluídas).
    Ordem de leitura: (1) uma frase que diz o estado do estoque e o primeiro passo; (2) a prateleira: uma marca por
    produto (círculo em dia, triângulo abaixo do mínimo, octógono sem estoque), problemas primeiro; (3) três números
    em linha; (4) "Fazer agora" (reposição rápida por POST /estoque/movimentar, com CSRF) ao lado de "Hoje na loja"
    (linha do tempo); (5) fluxo das OS, só em oficinas (sh:modulo OS); (6) gráfico de 14 dias e últimos produtos.
    Estados: loja vazia (primeiros passos), tudo em dia, só Baixo e com ruptura.
    Gráfico por fetch: GET /api/dashboard/movimentos?dias=14 -> [{dia, entradas, saidas, ajustes}] em UNIDADES.
    A data de hoje é escrita no navegador (js/tempo.js); datas relativas por time[data-relativo].
    Sem script nem style inline: números animados por js/numeros.js (data-contar).
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Distribuição Normal / Baixo / Sem estoque. produtosEstoqueBaixo inclui os sem estoque. --%>
<c:set var="nTotal" value="${resumo.totalProdutos}" />
<c:set var="nSem" value="${resumo.produtosSemEstoque}" />
<c:set var="nBaixo" value="${resumo.produtosEstoqueBaixo - resumo.produtosSemEstoque}" />
<c:set var="nNormal" value="${resumo.totalProdutos - resumo.produtosEstoqueBaixo}" />
<c:set var="nReposicao" value="${resumo.produtosEstoqueBaixo}" />

<%-- Prateleira: no máximo ~150 marcas. Acima disso, cada marca vale k produtos (arredondado para cima, mínimo 1 marca por estado). --%>
<c:set var="k" value="1" />
<c:set var="mSem" value="0" />
<c:set var="mBaixo" value="0" />
<c:set var="mNormal" value="0" />
<c:if test="${nTotal > 0}">
    <c:if test="${nTotal > 150}">
        <fmt:formatNumber var="k" value="${((nTotal + 149) - ((nTotal + 149) mod 150)) div 150}" maxFractionDigits="0" groupingUsed="false" />
    </c:if>
    <fmt:formatNumber var="mSem" value="${((nSem + k - 1) - ((nSem + k - 1) mod k)) div k}" maxFractionDigits="0" groupingUsed="false" />
    <fmt:formatNumber var="mBaixo" value="${((nBaixo + k - 1) - ((nBaixo + k - 1) mod k)) div k}" maxFractionDigits="0" groupingUsed="false" />
    <fmt:formatNumber var="mNormal" value="${((nNormal + k - 1) - ((nNormal + k - 1) mod k)) div k}" maxFractionDigits="0" groupingUsed="false" />
</c:if>

<%-- O primeiro da lista é o de menor saldo: é por ele que a frase manda começar --%>
<c:set var="primeiro" value="${empty produtosEstoqueBaixo ? null : produtosEstoqueBaixo[0]}" />

<%-- Quantas das últimas movimentações vieram de vendas (aparece só em lojas com o módulo de vendas) --%>
<c:set var="nVendas" value="0" />
<c:set var="nMov" value="0" />
<c:forEach var="m" items="${movimentacoesRecentes}">
    <c:set var="nMov" value="${nMov + 1}" />
    <c:if test="${m.tipoDocumento == 'VENDA'}"><c:set var="nVendas" value="${nVendas + 1}" /></c:if>
</c:forEach>

<sh:layout titulo="Início" ativo="dashboard">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/dashboard.css">
        <script defer src="${ctx}/js/dashboard.js"></script>
    </jsp:attribute>
    <jsp:body>
        <%-- ===== Capa: a frase do dia, o primeiro passo e as ações ===== --%>
        <header class="db-capa">
            <div class="db-capa__linha">
                <div class="db-capa__texto">
                    <p class="db-data" data-sh-data-hoje></p>
                    <h1 class="db-titulo" id="titulo-capa">
                        <c:choose>
                            <c:when test="${nTotal == 0}">Vamos cadastrar o <em>primeiro produto</em>.</c:when>
                            <c:when test="${nReposicao == 0}"><em>Tudo em dia</em>, nenhum produto abaixo do mínimo.</c:when>
                            <c:when test="${nSem > 0 and nBaixo > 0}">
                                <c:out value="${nSem}" /> <em><c:out value="${nSem == 1 ? 'produto acabou' : 'produtos acabaram'}" /></em>
                                e <c:out value="${nBaixo}" /> <c:out value="${nBaixo == 1 ? 'está' : 'estão'}" /> abaixo do mínimo.
                            </c:when>
                            <c:when test="${nSem > 0}">
                                <c:out value="${nSem}" /> <em><c:out value="${nSem == 1 ? 'produto acabou' : 'produtos acabaram'}" /></em>.
                            </c:when>
                            <c:otherwise>
                                <c:out value="${nBaixo}" /> <c:out value="${nBaixo == 1 ? 'produto está' : 'produtos estão'}" /> <em>abaixo do mínimo</em>.
                            </c:otherwise>
                        </c:choose>
                    </h1>
                    <p class="db-lead">
                        <c:choose>
                            <c:when test="${nTotal == 0}">Crie uma categoria e o primeiro produto. O resto desta tela se preenche sozinho.</c:when>
                            <c:when test="${nReposicao == 0}">Todos os <c:out value="${nTotal}" /> produtos estão acima do mínimo. Registre entradas e saídas para manter assim.</c:when>
                            <c:when test="${primeiro != null and primeiro.quantidade == 0}">
                                Comece por <a class="db-link" href="<c:out value='${ctx}/produtos/${primeiro.id}'/>"><c:out value="${primeiro.nome}" /></a>, que está sem estoque.
                            </c:when>
                            <c:when test="${primeiro != null}">
                                Comece por <a class="db-link" href="<c:out value='${ctx}/produtos/${primeiro.id}'/>"><c:out value="${primeiro.nome}" /></a>:
                                restam <strong><c:out value="${primeiro.quantidade}" /></strong> e o mínimo é <c:out value="${primeiro.estoqueMinimo}" />.
                            </c:when>
                        </c:choose>
                    </p>
                </div>
                <c:if test="${blocos['ATALHOS']}">
                <div class="db-acoes">
                    <sh:modulo nome="OS">
                        <a class="btn btn-primary" href="${ctx}/os/nova"><i class="bi bi-plus-lg" aria-hidden="true"></i> Nova ordem de serviço</a>
                    </sh:modulo>
                    <a class="btn ${modulos['OS'] ? 'btn-outline-secondary' : 'btn-primary'}" href="${ctx}/estoque?movTipo=ENTRADA"><i class="bi bi-box-arrow-in-down" aria-hidden="true"></i> Registrar entrada</a>
                    <a class="btn btn-outline-secondary" href="${ctx}/estoque?movTipo=SAIDA">Registrar saída</a>
                    <sh:modulo nome="VENDAS">
                        <a class="btn btn-ghost" href="${ctx}/clientes">Clientes</a>
                    </sh:modulo>
                    <a class="btn btn-ghost" href="${ctx}/produtos/novo">Novo produto</a>
                </div>
                </c:if>
            </div>

            <c:if test="${nTotal > 0}">
                <c:if test="${blocos['PRATELEIRA']}">
                <%-- A prateleira: uma marca por produto, problemas primeiro. As marcas são decorativas; a informação está na legenda, em texto. --%>
                <div class="db-prateleira">
                    <ul class="db-prateleira__marcas" aria-hidden="true">
                        <c:forEach begin="1" end="${mSem}"><li class="db-marca db-marca--sem"></li></c:forEach>
                        <c:forEach begin="1" end="${mBaixo}"><li class="db-marca db-marca--baixo"></li></c:forEach>
                        <c:forEach begin="1" end="${mNormal}"><li class="db-marca db-marca--normal"></li></c:forEach>
                    </ul>
                    <h2 class="visually-hidden">Situação dos produtos</h2>
                    <ul class="db-legenda">
                        <c:if test="${nSem > 0}">
                            <li><a href="${ctx}/produtos?nivelEstoque=semEstoque&amp;tamanho=25" data-nivel="sem">
                                <span class="db-marca db-marca--sem" aria-hidden="true"></span>
                                <strong><c:out value="${nSem}" /></strong> sem estoque</a></li>
                        </c:if>
                        <c:if test="${nBaixo > 0}">
                            <li><a href="${ctx}/produtos?nivelEstoque=baixo&amp;tamanho=25" data-nivel="baixo">
                                <span class="db-marca db-marca--baixo" aria-hidden="true"></span>
                                <strong><c:out value="${nBaixo}" /></strong> abaixo do mínimo</a></li>
                        </c:if>
                        <li><a href="${ctx}/produtos?nivelEstoque=ok&amp;tamanho=25" data-nivel="normal">
                            <span class="db-marca db-marca--normal" aria-hidden="true"></span>
                            <strong><c:out value="${nNormal}" /></strong> em dia</a></li>
                    </ul>
                    <c:if test="${k > 1}">
                        <p class="db-prateleira__nota">Cada marca representa até <c:out value="${k}" /> produtos.</p>
                    </c:if>
                </div>
                </c:if>

                <c:if test="${blocos['TOTAIS']}">
                <h2 class="visually-hidden">Totais da loja</h2>
                <ul class="db-livro">
                    <li>
                        <a href="${ctx}/valor-estoque">
                            <span class="db-livro__valor" data-contar="<c:out value='${resumo.valorTotalEstoque}'/>" data-formato="moeda">R$ <fmt:formatNumber value="${resumo.valorTotalEstoque}" pattern="#,##0.00" /></span>
                            <span class="db-livro__rotulo">em estoque</span>
                        </a>
                    </li>
                    <li>
                        <a href="${ctx}/unidades">
                            <span class="db-livro__valor" data-contar="<c:out value='${resumo.quantidadeTotalItens}'/>" data-formato="inteiro"><c:out value="${resumo.quantidadeTotalItens}" /></span>
                            <span class="db-livro__rotulo"><c:out value="${resumo.quantidadeTotalItens == 1 ? 'unidade guardada' : 'unidades guardadas'}" /></span>
                        </a>
                    </li>
                    <li>
                        <a href="${ctx}/produtos?tamanho=25">
                            <span class="db-livro__valor" data-contar="<c:out value='${nTotal}'/>" data-formato="inteiro"><c:out value="${nTotal}" /></span>
                            <span class="db-livro__rotulo"><c:out value="${nTotal == 1 ? 'produto' : 'produtos'}" /> em <c:out value="${resumo.totalCategorias}" /> <c:out value="${resumo.totalCategorias == 1 ? 'categoria' : 'categorias'}" /></span>
                        </a>
                    </li>
                </ul>
                <%-- Barra segmentada: largura proporcional ao número de produtos (viewBox = total; sem style inline). Forma e texto vêm da legenda abaixo. --%>
                <div class="db-segmentos">
                    <svg class="db-segmentos__barra" viewBox="0 0 ${nTotal} 1" preserveAspectRatio="none" role="img"
                        aria-label="${nSem} sem estoque, ${nBaixo} abaixo do mínimo, ${nNormal} em dia">
                        <c:set var="xBaixo" value="${nSem}" />
                        <c:set var="xNormal" value="${nSem + nBaixo}" />
                        <c:if test="${nSem > 0}"><rect class="db-segmentos__sem" x="0" y="0" width="${nSem}" height="1" /></c:if>
                        <c:if test="${nBaixo > 0}"><rect class="db-segmentos__baixo" x="${xBaixo}" y="0" width="${nBaixo}" height="1" /></c:if>
                        <c:if test="${nNormal > 0}"><rect class="db-segmentos__normal" x="${xNormal}" y="0" width="${nNormal}" height="1" /></c:if>
                    </svg>
                    <p class="db-segmentos__legenda">
                        <span class="db-tag db-tag--sem"><c:out value="${nSem}" /> sem estoque</span>
                        <span class="db-tag db-tag--baixo"><c:out value="${nBaixo}" /> abaixo do mínimo</span>
                        <span class="db-tag db-tag--ok"><c:out value="${nNormal}" /> em dia</span>
                    </p>
                </div>
                </c:if>
            </c:if>
        </header>

        <c:choose>
            <%-- ===== Loja nova: primeiros passos ===== --%>
            <c:when test="${nTotal == 0}">
                <section class="db-secao" aria-labelledby="titulo-passos">
                    <div class="db-secao__cab">
                        <h2 class="db-h2" id="titulo-passos">Primeiros passos</h2>
                        <p class="db-sub">Em três etapas a sua loja está pronta para usar.</p>
                    </div>
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
                <%-- Com os dois blocos, duas colunas; com um só, ele ocupa a linha inteira (db-colunas sem modificador = 1 coluna) --%>
                <c:if test="${blocos['FAZER_AGORA'] or blocos['HOJE_NA_LOJA']}">
                <div class="db-colunas ${blocos['FAZER_AGORA'] and blocos['HOJE_NA_LOJA'] ? 'db-colunas--7-5' : ''}">
                    <%-- ===== Fazer agora ===== --%>
                    <c:if test="${blocos['FAZER_AGORA']}">
                    <section class="db-secao" aria-labelledby="titulo-fazer">
                        <div class="db-secao__cab">
                            <h2 class="db-h2" id="titulo-fazer">A repor hoje</h2>
                            <c:if test="${nReposicao > 0}">
                                <a class="db-link" href="${ctx}/estoque-baixo">Ver estoque baixo</a>
                            </c:if>
                        </div>
                        <c:choose>
                            <c:when test="${nReposicao == 0}">
                                <div class="db-ok">
                                    <p class="db-ok__titulo"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Nada para repor.</p>
                                    <p class="db-ok__texto">Quando um produto chegar ao mínimo, ele aparece aqui com a reposição pronta.</p>
                                    <a class="btn btn-outline-secondary btn-sm" href="${ctx}/estoque?movTipo=ENTRADA">Registrar uma entrada</a>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <ul class="db-fila">
                                    <c:forEach var="produto" items="${produtosEstoqueBaixo}">
                                        <%-- Sugestão de quantidade: leva o saldo a 2x o mínimo (o medidor fica cheio); sem mínimo, 10 --%>
                                        <c:set var="sugestao" value="${produto.estoqueMinimo > 0 ? produto.estoqueMinimo * 2 - produto.quantidade : 10}" />
                                        <c:if test="${sugestao < 1}"><c:set var="sugestao" value="1" /></c:if>
                                        <c:set var="nivel" value="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sem' : 'baixo'}" />
                                        <li class="db-item db-item--${nivel}">
                                            <div class="db-item__corpo">
                                                <a class="db-item__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"
                                                    data-sh-peek data-sh-peek-titulo="<c:out value='${produto.nome}'/>"><c:out value="${produto.nome}" /></a>
                                                <span class="db-item__estado">
                                                    <%-- Tag de estado: forma (octógono ou triângulo) + texto; a cor só reforça --%>
                                                    <span class="db-tag db-tag--${nivel}"><c:out value="${nivel == 'sem' ? 'Sem estoque' : 'Abaixo do mínimo'}" /></span>
                                                    &middot; mínimo <span class="sh-mono"><c:out value="${produto.estoqueMinimo}" /></span>
                                                    &middot; <c:out value="${produto.categoria.nome}" />
                                                </span>
                                                <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                            </div>
                                            <span class="db-item__saldo"><span class="visually-hidden">Saldo: </span><c:out value="${produto.quantidade}" /> un.</span>
                                            <%-- Reposição rápida: o mesmo POST da tela de movimentações (entrada), com a quantidade sugerida --%>
                                            <form class="db-repor" method="post" action="${ctx}/estoque/movimentar" data-sh-bloquear data-sh-enviando="Repondo…">
                                                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                <input type="hidden" name="produtoId" value="<c:out value='${produto.id}'/>">
                                                <input type="hidden" name="tipo" value="ENTRADA">
                                                <input type="hidden" name="observacao" value="Reposição rápida pelo início">
                                                <label class="visually-hidden" for="repor-<c:out value='${produto.id}'/>">Quantidade a repor de <c:out value="${produto.nome}" /></label>
                                                <input class="form-control db-repor__qtd sh-mono" id="repor-<c:out value='${produto.id}'/>" type="number" name="quantidade"
                                                    min="1" max="1000000" step="1" inputmode="numeric" required data-sh-sem-stepper value="<c:out value='${sugestao}'/>">
                                                <span class="db-repor__un" aria-hidden="true">un.</span>
                                                <button type="submit" class="btn btn-primary btn-sm">Repor</button>
                                            </form>
                                        </li>
                                    </c:forEach>
                                </ul>
                                <c:if test="${nReposicao > 6}">
                                    <p class="db-mais">
                                        <a class="db-link" href="${ctx}/estoque-baixo">Ver os outros <c:out value="${nReposicao - 6}" /> na lista completa</a>
                                    </p>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </section>
                    </c:if>

                    <%-- ===== Hoje na loja: linha do tempo ===== --%>
                    <c:if test="${blocos['HOJE_NA_LOJA']}">
                    <section class="db-secao" aria-labelledby="titulo-hoje">
                        <div class="db-secao__cab">
                            <h2 class="db-h2" id="titulo-hoje">Hoje na loja</h2>
                            <a class="db-link" href="${ctx}/estoque">Ver todas</a>
                        </div>
                        <sh:modulo nome="VENDAS">
                            <c:if test="${nMov > 0}">
                                <p class="db-sub">
                                    <c:choose>
                                        <c:when test="${nVendas == 0}">Nenhuma venda entre as últimas <c:out value="${nMov}" /> movimentações.</c:when>
                                        <c:otherwise><c:out value="${nVendas}" /> das últimas <c:out value="${nMov}" /> movimentações <c:out value="${nVendas == 1 ? 'foi venda' : 'foram vendas'}" />.</c:otherwise>
                                    </c:choose>
                                </p>
                            </c:if>
                        </sh:modulo>
                        <c:choose>
                            <c:when test="${empty movimentacoesRecentes}">
                                <p class="db-vazio">Nenhuma movimentação ainda. Registre a primeira entrada ou saída e ela aparece aqui.</p>
                                <p><a class="btn btn-outline-secondary btn-sm" href="${ctx}/estoque">Registrar movimentação</a></p>
                            </c:when>
                            <c:otherwise>
                                <ol class="db-linha">
                                    <c:forEach var="m" items="${movimentacoesRecentes}">
                                        <c:set var="tipoClasse" value="${m.tipo == 'ENTRADA' ? 'entrada' : (m.tipo == 'SAIDA' ? 'saida' : 'ajuste')}" />
                                        <c:set var="iso">${m.criadoEm}</c:set>
                                        <li class="db-evento db-evento--${tipoClasse}">
                                            <div class="db-evento__quando">
                                                <time datetime="<c:out value='${fn:substring(iso, 0, 16)}'/>" data-relativo><c:out value="${fn:substring(iso, 8, 10)}/${fn:substring(iso, 5, 7)} ${fn:substring(iso, 11, 16)}" /></time>
                                                <c:if test="${m.tipoDocumento != 'MANUAL'}"><span class="db-evento__etiqueta"><c:out value="${m.tipoDocumento.rotulo}" /></span></c:if>
                                                <c:if test="${m.estornoDeId != null}"><span class="db-evento__etiqueta">Estorno</span></c:if>
                                            </div>
                                            <p class="db-evento__texto">
                                                <c:choose>
                                                    <c:when test="${m.tipo == 'ENTRADA'}"><span class="db-evento__tipo">Entrada</span> de <c:out value="${m.quantidade}" /> un. de</c:when>
                                                    <c:when test="${m.tipo == 'SAIDA'}"><span class="db-evento__tipo">Saída</span> de <c:out value="${m.quantidade}" /> un. de</c:when>
                                                    <c:otherwise><span class="db-evento__tipo">Ajuste</span> do saldo de</c:otherwise>
                                                </c:choose>
                                                <a href="<c:out value='${ctx}/produtos/${m.produto.id}'/>" data-sh-peek data-sh-peek-titulo="<c:out value='${m.produto.nome}'/>"><c:out value="${m.produto.nome}" /></a>
                                            </p>
                                            <span class="db-evento__saldo">Saldo de <c:out value="${m.quantidadeAnterior}" /> para <c:out value="${m.quantidadeNova}" /> un.</span>
                                        </li>
                                    </c:forEach>
                                </ol>
                            </c:otherwise>
                        </c:choose>
                    </section>
                    </c:if>
                </div>
                </c:if>

                <%-- ===== Oficina: fluxo das ordens de serviço (só com o módulo OS e o bloco visível) ===== --%>
                <c:if test="${blocos['OS_FLUXO']}">
                <sh:modulo nome="OS">
                    <section class="db-secao" aria-labelledby="titulo-os">
                        <div class="db-secao__cab">
                            <h2 class="db-h2" id="titulo-os">Ordens de serviço</h2>
                            <a class="db-link" href="${ctx}/os">Ver todas</a>
                        </div>
                        <ol class="db-fluxo">
                            <c:forEach var="e" items="${osPorStatus}">
                                <c:if test="${e.key != 'ENTREGUE' and e.key != 'CANCELADA'}">
                                    <li>
                                        <a class="db-etapa ${e.value > 0 ? 'db-etapa--ativa' : ''}" href="${ctx}/os?status=${e.key.name()}">
                                            <span class="db-etapa__n"><c:out value="${e.value}" /></span>
                                            <span class="db-etapa__nome"><c:out value="${e.key.rotulo}" /></span>
                                            <span class="db-etapa__desc"><c:out value="${e.key.descricao}" /></span>
                                        </a>
                                    </li>
                                </c:if>
                            </c:forEach>
                        </ol>
                        <c:choose>
                            <c:when test="${empty osAndamento}">
                                <p class="db-vazio">Nenhuma ordem em andamento. <a class="db-link" href="${ctx}/os/nova">Abrir uma ordem de serviço</a></p>
                            </c:when>
                            <c:otherwise>
                                <ul class="db-os">
                                    <c:forEach var="o" items="${osAndamento}">
                                        <li class="db-os__item">
                                            <a class="db-os__nome" href="${ctx}/os/${o.id}">OS <c:out value="${o.numero}" />: <c:out value="${o.equipamento.descricao}" /></a>
                                            <span class="db-os__sub"><c:out value="${o.cliente.nome}" /></span>
                                            <span class="db-os__status"><c:out value="${o.status.rotulo}" /></span>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </c:otherwise>
                        </c:choose>
                    </section>
                </sh:modulo>
                </c:if>

                <c:if test="${blocos['GRAFICO_14_DIAS'] or blocos['CADASTRADOS_ULTIMO']}">
                <div class="db-colunas ${blocos['GRAFICO_14_DIAS'] and blocos['CADASTRADOS_ULTIMO'] ? 'db-colunas--8-4' : ''}">
                    <%-- ===== Ritmo de 14 dias (SVG feito à mão, dados por fetch) ===== --%>
                    <c:if test="${blocos['GRAFICO_14_DIAS']}">
                    <section class="db-secao db-ritmo" aria-labelledby="titulo-grafico" data-sh-grafico
                        data-url="<c:out value='${ctx}/api/dashboard/movimentos?dias=14'/>">
                        <div class="db-grafico__cab">
                            <div>
                                <h2 class="db-h2" id="titulo-grafico">Ritmo dos últimos 14 dias</h2>
                                <p class="db-grafico__resumo" data-grafico-resumo>Carregando os movimentos&hellip;</p>
                            </div>
                            <ul class="db-grafico__legenda" aria-hidden="true">
                                <li><span class="db-grafico__amostra db-grafico__amostra--entrada"></span>Entrou (acima da linha)</li>
                                <li><span class="db-grafico__amostra db-grafico__amostra--saida"></span>Saiu (abaixo)</li>
                                <li><span class="db-grafico__amostra db-grafico__amostra--ajuste"></span>Ajuste</li>
                            </ul>
                        </div>
                        <div class="db-grafico__area" data-grafico-area>
                            <%-- Estados (o JS mostra um por vez). Sem JavaScript, só o aviso do noscript aparece. --%>
                            <div class="db-grafico__estado" data-grafico-estado="carregando" hidden>
                                <div class="db-grafico__esqueleto" aria-hidden="true">
                                    <span class="sh-esqueleto db-esq-1"></span><span class="sh-esqueleto db-esq-2"></span><span class="sh-esqueleto db-esq-3"></span>
                                    <span class="sh-esqueleto db-esq-4"></span><span class="sh-esqueleto db-esq-5"></span><span class="sh-esqueleto db-esq-6"></span>
                                    <span class="sh-esqueleto db-esq-7"></span><span class="sh-esqueleto db-esq-8"></span><span class="sh-esqueleto db-esq-9"></span>
                                    <span class="sh-esqueleto db-esq-10"></span><span class="sh-esqueleto db-esq-11"></span><span class="sh-esqueleto db-esq-12"></span>
                                    <span class="sh-esqueleto db-esq-13"></span><span class="sh-esqueleto db-esq-14"></span>
                                </div>
                            </div>
                            <div class="db-grafico__estado db-grafico__estado--msg" data-grafico-estado="erro" hidden>
                                <div>
                                    <p class="db-grafico__msg-titulo">Não foi possível carregar os movimentos.</p>
                                    <p class="db-grafico__msg-texto" data-grafico-erro-texto>O resto da tela continua valendo. Tente de novo em instantes.</p>
                                    <button type="button" class="btn btn-outline-secondary btn-sm" data-grafico-tentar>Tentar de novo</button>
                                </div>
                            </div>
                            <div class="db-grafico__estado db-grafico__estado--msg" data-grafico-estado="vazio" hidden>
                                <div>
                                    <p class="db-grafico__msg-titulo">Nenhum movimento nesses 14 dias.</p>
                                    <p class="db-grafico__msg-texto">Quando você registrar entradas e saídas, elas aparecem aqui, dia a dia.</p>
                                    <a class="btn btn-primary btn-sm" href="${ctx}/estoque?movTipo=ENTRADA">Registrar uma entrada</a>
                                </div>
                            </div>
                            <div class="db-grafico__quadro" data-grafico-quadro hidden></div>
                            <div class="db-grafico__dica" data-grafico-dica aria-hidden="true" hidden></div>
                            <noscript><p class="db-grafico__msg-texto">O gráfico precisa de JavaScript. Os números do dia a dia estão em <a href="${ctx}/estoque">Movimentações</a>.</p></noscript>
                        </div>
                        <details class="db-grafico__tabela" data-grafico-tabela hidden>
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
                    </c:if>

                    <%-- ===== Cadastrados por último ===== --%>
                    <c:if test="${blocos['CADASTRADOS_ULTIMO']}">
                    <section class="db-secao" aria-labelledby="titulo-recentes">
                        <div class="db-secao__cab">
                            <h2 class="db-h2" id="titulo-recentes">Cadastrados por último</h2>
                            <a class="db-link" href="${ctx}/produtos?tamanho=25">Ver todos</a>
                        </div>
                        <ul class="db-recentes">
                            <c:forEach var="produto" items="${produtosRecentes}">
                                <li class="db-recente">
                                    <div class="db-recente__corpo">
                                        <a class="db-recente__nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"
                                            data-sh-peek data-sh-peek-titulo="<c:out value='${produto.nome}'/>"><c:out value="${produto.nome}" /></a>
                                        <span class="db-recente__sub"><c:out value="${produto.categoria.nome}" /> &middot; R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></span>
                                    </div>
                                    <sh:badgeEstoque nivel="${produto.nivelEstoque}" />
                                </li>
                            </c:forEach>
                        </ul>
                    </section>
                    </c:if>
                </div>
                </c:if>
            </c:otherwise>
        </c:choose>

        <c:if test="${podeGerenciar}">
            <p class="db-sub mt-4"><a class="db-link" href="${ctx}/conta/personalizacao"><i class="bi bi-sliders" aria-hidden="true"></i> Personalizar início</a></p>
        </c:if>
    </jsp:body>
</sh:layout>
