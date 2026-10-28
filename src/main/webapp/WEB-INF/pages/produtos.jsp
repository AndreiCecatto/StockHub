<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Lista de produtos na v3 "central de operações". Rota: GET /produtos. Parâmetros: busca, categoriaId, nivelEstoque
    (ok, baixo, semEstoque), ordenacao (id, nome, preco, quantidade, categoria), direcao (asc, desc), tamanho, pagina.
    Dados do ProdutoController: produtos, categorias, busca, categoriaId, nivelEstoque, ordenacao, direcao,
    direcaoInversa, paginaAtual, totalPaginas, totalProdutosFiltro, tamanho, parametrosFiltros (que NÃO traz direcao:
    a paginação acrescenta), colunas (Map código -> visível, personalização da loja, RN-115 a RN-120; só traz as colunas
    aplicáveis à loja: chave ausente = a coluna não existe nessa loja e não é desenhada). O atributo camposRamo não é mais enviado:
    as colunas de ramo vêm só de `colunas`. Chaves: COL_CATEGORIA,
    COL_PRECO, COL_SITUACAO, COL_DESCRICAO (todas as lojas) e COL_TAMANHO, COL_COR, COL_LOTE, COL_VALIDADE, COL_NUMERO_SERIE
    (de ramo). Nome, saldo (ajuste rápido e medidor), ações, seleção e paginação nunca somem. Flash: mensagem (toast) e erro (inline), pelo alertas.jsp do layout.

    Funciona sem JavaScript: o filtro é um GET comum, o nome do produto é um link para a página inteira e o saldo é um número.
    Com JavaScript (js/v3-prod-init.js marca a página, js/produtos.js e js/v3-prod-ajuste.js):
      - busca ao digitar troca só a região [data-regiao-viva];
      - clicar na linha ou no nome abre o painel peek (data-sh-peek; Ctrl, Cmd e botão do meio abrem a página);
      - j, k e Enter na lista (data-sh-lista-teclado fica na região, que é permanente);
      - ajuste rápido do saldo (−, +, número) por POST /estoque/movimentar em JSON, com atualização otimista;
      - seleção de linhas (a única ação em lote é exportar o CSV dos filtros) e visão compacta ou confortável.
    Elementos só de JS levam a classe v3-so-js (ocultos sem JS); os que JS substitui levam v3-so-semjs.
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="temFiltros" value="${not empty busca or not empty categoriaId or not empty nivelEstoque}" />

<%-- Colunas visíveis (personalização da loja). Chave ausente em "colunas" = coluna que a loja não tem. --%>
<c:set var="verCategoria" value="${colunas['COL_CATEGORIA'] == true}" />
<c:set var="verPreco" value="${colunas['COL_PRECO'] == true}" />
<c:set var="verSituacao" value="${colunas['COL_SITUACAO'] == true}" />
<c:set var="verDescricao" value="${colunas['COL_DESCRICAO'] == true}" />
<c:set var="verTamanho" value="${colunas['COL_TAMANHO'] == true}" />
<c:set var="verCor" value="${colunas['COL_COR'] == true}" />
<c:set var="verLote" value="${colunas['COL_LOTE'] == true}" />
<c:set var="verValidade" value="${colunas['COL_VALIDADE'] == true}" />
<c:set var="verSerie" value="${colunas['COL_NUMERO_SERIE'] == true}" />

<%-- Rótulos dos filtros ativos --%>
<c:set var="categoriaNome" value="" />
<c:if test="${not empty categoriaId}">
    <c:forEach var="cat" items="${categorias}">
        <c:if test="${cat.id == categoriaId}"><c:set var="categoriaNome" value="${cat.nome}" /></c:if>
    </c:forEach>
</c:if>
<c:set var="nivelRotulo" value="${nivelEstoque == 'ok' ? 'Normal' : (nivelEstoque == 'baixo' ? 'Baixo' : 'Sem estoque')}" />
<c:set var="ordemRotulo" value="${ordenacao == 'nome' ? 'nome' : (ordenacao == 'preco' ? 'preço' : (ordenacao == 'quantidade' ? 'saldo' : (ordenacao == 'categoria' ? 'categoria' : 'data de cadastro')))}" />

<%-- URLs montadas com c:url/c:param (codificação automática). Todas começam em /produtos e já têm "?" --%>
<c:url var="urlOrdem" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlSemBusca" value="/produtos">
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlSemCategoria" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlSemNivel" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlLimpar" value="/produtos">
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<c:url var="urlRecentes" value="/produtos">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="id" />
    <c:param name="direcao" value="desc" />
    <c:param name="tamanho" value="${tamanho}" />
</c:url>
<%-- Exportação CSV com os mesmos filtros e a mesma ordem (sem paginação) --%>
<c:url var="urlCsv" value="/produtos/exportar.csv">
    <c:if test="${not empty busca}"><c:param name="busca" value="${busca}" /></c:if>
    <c:if test="${not empty categoriaId}"><c:param name="categoriaId" value="${categoriaId}" /></c:if>
    <c:if test="${not empty nivelEstoque}"><c:param name="nivelEstoque" value="${nivelEstoque}" /></c:if>
    <c:param name="ordenacao" value="${ordenacao}" />
    <c:param name="direcao" value="${direcao}" />
</c:url>
<%-- A paginação precisa do filtro inteiro, inclusive direcao (parametrosFiltros já vem codificado) --%>
<c:set var="extraPaginacao" value="${parametrosFiltros}&direcao=${direcao}" />

<sh:layout titulo="Produtos" ativo="produtos" largura="larga">
    <jsp:attribute name="scripts">
        <script src="${ctx}/js/v3-prod-init.js"></script>
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
        <script defer src="${ctx}/js/produtos.js"></script>
        <script defer src="${ctx}/js/v3-prod-ajuste.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Produtos" descricao="Saldo, situação e movimentação de cada item do catálogo.">
            <a class="btn btn-primary" href="${ctx}/produtos/novo" data-sh-atalho-novo="pagina">
                <i class="bi bi-plus-lg" aria-hidden="true"></i> Novo produto
            </a>
        </sh:cabecalhoPagina>

        <%-- ===== Filtros (GET comum; com JS a lista troca ao digitar) ===== --%>
        <form class="v3-filtros" method="get" action="${ctx}/produtos" role="search"
            aria-label="Filtrar produtos" data-busca-viva>
            <div class="v3-filtros__busca">
                <label class="visually-hidden" for="busca">Buscar por nome</label>
                <div class="v3-busca">
                    <i class="bi bi-search v3-busca__lupa" aria-hidden="true"></i>
                    <input id="busca" class="form-control" type="search" name="busca" maxlength="120"
                        value="<c:out value='${busca}'/>" placeholder="Buscar por nome" autocomplete="off"
                        aria-keyshortcuts="/" data-sh-busca-lista data-sh-limpar-esc>
                    <span class="v3-busca__estado v3-so-js" data-busca-indicador hidden>
                        <span class="v3-giro" aria-hidden="true"></span>
                        <span>Atualizando</span>
                    </span>
                </div>
                <button class="btn btn-outline-secondary v3-so-semjs" type="submit">
                    <i class="bi bi-search" aria-hidden="true"></i> Buscar
                </button>
            </div>

            <div class="v3-filtros__linha">
                <fieldset class="v3-grupo v3-grupo--chips">
                    <legend class="v3-legenda">Situação do estoque</legend>
                    <div class="sh-chips">
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-todos" name="nivelEstoque"
                                value="" ${empty nivelEstoque ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-todos"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i>Todos</label>
                        </span>
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-ok" name="nivelEstoque"
                                value="ok" ${nivelEstoque == 'ok' ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-ok"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i><i class="bi bi-check-circle-fill v3-chip-ico v3-chip-ico--normal" aria-hidden="true"></i>Normal</label>
                        </span>
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-baixo" name="nivelEstoque"
                                value="baixo" ${nivelEstoque == 'baixo' ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-baixo"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i><i class="bi bi-exclamation-triangle-fill v3-chip-ico v3-chip-ico--baixo" aria-hidden="true"></i>Baixo</label>
                        </span>
                        <span class="sh-prod-chip">
                            <input class="visually-hidden sh-prod-chip__input" type="radio" id="nivel-sem" name="nivelEstoque"
                                value="semEstoque" ${nivelEstoque == 'semEstoque' ? 'checked' : ''}>
                            <label class="sh-chip" for="nivel-sem"><i class="bi bi-check-lg sh-prod-chip__visto" aria-hidden="true"></i><i class="bi bi-x-octagon-fill v3-chip-ico v3-chip-ico--sem" aria-hidden="true"></i>Sem estoque</label>
                        </span>
                    </div>
                </fieldset>

                <div class="v3-grupo">
                    <label class="v3-legenda" for="categoriaId">Categoria</label>
                    <select id="categoriaId" class="form-select" name="categoriaId">
                        <option value="">Todas</option>
                        <c:forEach var="categoria" items="${categorias}">
                            <option value="<c:out value='${categoria.id}'/>" ${categoriaId == categoria.id ? 'selected' : ''}><c:out value="${categoria.nome}" /></option>
                        </c:forEach>
                    </select>
                </div>

                <div class="v3-grupo v3-grupo--tamanho">
                    <label class="v3-legenda" for="tamanho">Itens por página</label>
                    <select id="tamanho" class="form-select" name="tamanho">
                        <c:if test="${tamanho != 10 and tamanho != 25 and tamanho != 50 and tamanho != 100}">
                            <option value="<c:out value='${tamanho}'/>" selected><c:out value="${tamanho}" /></option>
                        </c:if>
                        <option value="10" ${tamanho == 10 ? 'selected' : ''}>10</option>
                        <option value="25" ${tamanho == 25 ? 'selected' : ''}>25</option>
                        <option value="50" ${tamanho == 50 ? 'selected' : ''}>50</option>
                        <option value="100" ${tamanho == 100 ? 'selected' : ''}>100</option>
                    </select>
                </div>

                <%-- Ordenação sem o cabeçalho da tabela (celular e telas médias, onde a coluna Categoria some).
                     Escondidos por CSS no computador largo, continuam enviando a ordem atual junto com o formulário. --%>
                <div class="v3-ordem-movel">
                    <div class="v3-grupo">
                        <label class="v3-legenda" for="ordenacao">Ordenar por</label>
                        <select id="ordenacao" class="form-select" name="ordenacao">
                            <option value="id" ${ordenacao == 'id' ? 'selected' : ''}>Mais recentes</option>
                            <option value="nome" ${ordenacao == 'nome' ? 'selected' : ''}>Nome</option>
                            <c:if test="${verPreco or ordenacao == 'preco'}"><option value="preco" ${ordenacao == 'preco' ? 'selected' : ''}>Preço</option></c:if>
                            <option value="quantidade" ${ordenacao == 'quantidade' ? 'selected' : ''}>Saldo</option>
                            <c:if test="${verCategoria or ordenacao == 'categoria'}"><option value="categoria" ${ordenacao == 'categoria' ? 'selected' : ''}>Categoria</option></c:if>
                        </select>
                    </div>
                    <div class="v3-grupo">
                        <label class="v3-legenda" for="direcao">Ordem</label>
                        <select id="direcao" class="form-select" name="direcao">
                            <option value="asc" ${direcao == 'asc' ? 'selected' : ''}>Crescente</option>
                            <option value="desc" ${direcao == 'desc' ? 'selected' : ''}>Decrescente</option>
                        </select>
                    </div>
                </div>
            </div>
        </form>

        <%-- Falha ao atualizar a lista ao digitar (o JS mostra; Buscar continua funcionando) --%>
        <div class="sh-alert sh-alert--danger v3-erro-busca" role="alert" hidden data-busca-erro>
            <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
            <div class="sh-alert__corpo">Não foi possível atualizar a lista. Confira a conexão e tente de novo.</div>
            <button type="button" class="btn btn-outline-secondary btn-sm" data-busca-tentar>Tentar de novo</button>
        </div>

        <%-- Dica de uso do ajuste rápido, lida pelos leitores de tela junto de cada campo de saldo --%>
        <p id="v3-dica-ajuste" class="visually-hidden">Digite a contagem final e tecle Enter para ajustar o saldo. Seta para cima soma uma unidade, seta para baixo tira uma. Esc cancela a digitação.</p>

        <div class="v3-vivo" data-v3-lista-produtos>
            <div class="v3-progresso" data-busca-progresso aria-hidden="true"></div>
            <%-- ===== Região trocada pela busca ao digitar (permanente; só o conteúdo muda) ===== --%>
            <div data-regiao-viva data-sh-lista-teclado>
                <div class="v3-barra">
                    <div class="v3-barra__esq">
                        <p class="v3-contagem" data-contagem><strong class="v3-mono"><fmt:formatNumber value="${totalProdutosFiltro}" pattern="#,##0" /></strong> <c:out value="${totalProdutosFiltro == 1 ? 'produto' : 'produtos'}" /></p>
                        <span class="v3-ordem-texto">
                            Ordenados por <c:out value="${ordemRotulo}" />,
                            <c:out value="${direcao == 'asc' ? 'crescente' : 'decrescente'}" />
                            <c:if test="${ordenacao != 'id'}">
                                &middot; <a href="<c:out value='${urlRecentes}'/>">Mais recentes primeiro</a>
                            </c:if>
                        </span>
                    </div>
                    <div class="v3-barra__acoes">
                        <div class="v3-seg v3-so-js" role="group" aria-label="Densidade da lista">
                            <button type="button" class="v3-seg__op" data-v3-densidade="compacta" aria-pressed="false">Compacta</button>
                            <button type="button" class="v3-seg__op" data-v3-densidade="confortavel" aria-pressed="true">Confortável</button>
                        </div>
                        <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${urlCsv}'/>" data-v3-csv>
                            <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
                        </a>
                    </div>
                </div>

                <c:if test="${temFiltros}">
                    <ul class="sh-chips v3-ativos" aria-label="Filtros ativos">
                        <c:if test="${not empty busca}">
                            <li>
                                <a class="sh-chip" href="<c:out value='${urlSemBusca}'/>">
                                    Nome: <c:out value="${busca}" />
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                    <span class="visually-hidden">Remover filtro de nome</span>
                                </a>
                            </li>
                        </c:if>
                        <c:if test="${not empty categoriaId}">
                            <li>
                                <a class="sh-chip" href="<c:out value='${urlSemCategoria}'/>">
                                    Categoria: <c:out value="${empty categoriaNome ? 'indisponível' : categoriaNome}" />
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                    <span class="visually-hidden">Remover filtro de categoria</span>
                                </a>
                            </li>
                        </c:if>
                        <c:if test="${not empty nivelEstoque}">
                            <li>
                                <a class="sh-chip" href="<c:out value='${urlSemNivel}'/>">
                                    Situação: <c:out value="${nivelRotulo}" />
                                    <i class="bi bi-x-lg" aria-hidden="true"></i>
                                    <span class="visually-hidden">Remover filtro de situação do estoque</span>
                                </a>
                            </li>
                        </c:if>
                        <li><a class="btn btn-ghost btn-sm" href="<c:out value='${urlLimpar}'/>">Limpar filtros</a></li>
                    </ul>
                </c:if>

                <c:choose>
                    <%-- ===== Vazio ===== --%>
                    <c:when test="${empty produtos}">
                        <div class="sh-card">
                            <c:choose>
                                <c:when test="${totalProdutosFiltro > 0}">
                                    <sh:estadoVazio icone="journal-x" titulo="Esta página não tem produtos"
                                        texto="A lista tem menos páginas do que a pedida."
                                        acaoUrl="/produtos?tamanho=${tamanho}" acaoRotulo="Voltar ao início da lista" />
                                </c:when>
                                <c:when test="${temFiltros}">
                                    <sh:estadoVazio icone="search" titulo="Nenhum produto encontrado"
                                        texto="Nenhum item combina com a busca e os filtros atuais. Ajuste o nome ou limpe os filtros."
                                        acaoUrl="/produtos?tamanho=${tamanho}" acaoRotulo="Limpar filtros" />
                                </c:when>
                                <c:when test="${empty categorias}">
                                    <sh:estadoVazio icone="tags" titulo="Crie uma categoria para começar"
                                        texto="Todo produto pertence a uma categoria, como Bebidas ou Limpeza. Crie a primeira e depois cadastre os produtos."
                                        acaoUrl="/categorias/novo" acaoRotulo="Criar categoria" />
                                </c:when>
                                <c:otherwise>
                                    <sh:estadoVazio icone="box-seam" titulo="Nenhum produto cadastrado"
                                        texto="Cadastre o primeiro produto para acompanhar o saldo e receber o aviso de reposição."
                                        acaoUrl="/produtos/novo" acaoRotulo="Novo produto" />
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>

                    <%-- ===== Tabela (vira cartões no celular) ===== --%>
                    <c:otherwise>
                        <div class="sh-table-wrap v3-tabela-wrap">
                            <table class="sh-table sh-table--cards v3-tabela" role="table">
                                <caption class="visually-hidden">Lista de produtos, <c:out value="${totalProdutosFiltro}" /> no total. Clique numa linha para ver os detalhes sem sair da lista.</caption>
                                <thead role="rowgroup">
                                    <tr role="row">
                                        <th scope="col" role="columnheader" class="v3-c-sel v3-so-js">
                                            <input type="checkbox" class="form-check-input" aria-label="Selecionar todos os produtos desta página" data-v3-sel-todos>
                                        </th>
                                        <sh:colunaOrdenavel campo="nome" rotulo="Produto" ordenacaoAtual="${ordenacao}"
                                            direcaoAtual="${direcao}" urlBase="${urlOrdem}" classe="v3-c-prod" />
                                        <c:if test="${verCategoria}">
                                            <sh:colunaOrdenavel campo="categoria" rotulo="Categoria" ordenacaoAtual="${ordenacao}"
                                                direcaoAtual="${direcao}" urlBase="${urlOrdem}" classe="v3-c-cat" />
                                        </c:if>
                                        <c:if test="${verPreco}">
                                            <sh:colunaOrdenavel campo="preco" rotulo="Preço" ordenacaoAtual="${ordenacao}"
                                                direcaoAtual="${direcao}" urlBase="${urlOrdem}" numerico="true" classe="v3-c-preco" />
                                        </c:if>
                                        <sh:colunaOrdenavel campo="quantidade" rotulo="Saldo" ordenacaoAtual="${ordenacao}"
                                            direcaoAtual="${direcao}" urlBase="${urlOrdem}" numerico="true" classe="v3-c-saldo" />
                                        <c:if test="${verSituacao}"><th scope="col" role="columnheader" class="v3-c-sit">Situação</th></c:if>
                                        <c:if test="${verTamanho}"><th scope="col" role="columnheader" class="v3-c-ramo">Tamanho</th></c:if>
                                        <c:if test="${verCor}"><th scope="col" role="columnheader" class="v3-c-ramo">Cor</th></c:if>
                                        <c:if test="${verLote}"><th scope="col" role="columnheader" class="v3-c-ramo">Lote</th></c:if>
                                        <c:if test="${verValidade}"><th scope="col" role="columnheader" class="v3-c-ramo">Validade</th></c:if>
                                        <c:if test="${verSerie}"><th scope="col" role="columnheader" class="v3-c-ramo">Série</th></c:if>
                                        <th scope="col" role="columnheader" class="v3-c-acoes"><span class="visually-hidden">Ações</span></th>
                                    </tr>
                                </thead>
                                <tbody role="rowgroup">
                                    <c:forEach var="produto" items="${produtos}">
                                        <tr role="row" data-v3-linha data-produto-id="<c:out value='${produto.id}'/>"
                                            data-nome="<c:out value='${produto.nome}'/>" data-minimo="<c:out value='${produto.estoqueMinimo}'/>"
                                            data-saldo="<c:out value='${produto.quantidade}'/>"
                                            class="${produto.nivelEstoque == 'SEM_ESTOQUE' ? 'sh-linha--sem' : (produto.nivelEstoque == 'BAIXO' ? 'sh-linha--baixo' : '')}">
                                            <td role="cell" class="v3-c-sel v3-so-js">
                                                <input type="checkbox" class="form-check-input" data-v3-sel value="<c:out value='${produto.id}'/>"
                                                    aria-label="Selecionar <c:out value='${produto.nome}'/>">
                                            </td>
                                            <td role="cell" class="v3-c-prod">
                                                <a class="v3-prod-nome" href="<c:out value='${ctx}/produtos/${produto.id}'/>"
                                                    data-sh-peek data-sh-linha-link data-sh-peek-titulo="<c:out value='${produto.nome}'/>"><c:out value="${produto.nome}" /></a>
                                                <c:if test="${verCategoria or verPreco}">
                                                    <span class="v3-prod-meta">
                                                        <c:if test="${verCategoria}"><c:out value="${produto.categoria.nome}" /></c:if>
                                                        <c:if test="${verCategoria and verPreco}"> &middot; </c:if>
                                                        <c:if test="${verPreco}"><span class="v3-mono">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></span></c:if>
                                                    </span>
                                                </c:if>
                                                <c:if test="${verTamanho or verCor or verLote or verValidade or verSerie}">
                                                    <span class="v3-prod-ramo">
                                                        <c:if test="${verTamanho and not empty produto.tamanho}"><span>Tam. <c:out value="${produto.tamanho}" /></span></c:if>
                                                        <c:if test="${verCor and not empty produto.cor}"><span>Cor <c:out value="${produto.cor}" /></span></c:if>
                                                        <c:if test="${verLote and not empty produto.lote}"><span>Lote <c:out value="${produto.lote}" /></span></c:if>
                                                        <c:if test="${verValidade and not empty produto.dataValidade}">
                                                            <fmt:parseDate value="${produto.dataValidade}" pattern="yyyy-MM-dd" var="dataValMeta" type="date" />
                                                            <span>Val. <fmt:formatDate value="${dataValMeta}" pattern="dd/MM/yyyy" /></span>
                                                        </c:if>
                                                        <c:if test="${verSerie and not empty produto.numeroSerie}"><span>Série <c:out value="${produto.numeroSerie}" /></span></c:if>
                                                    </span>
                                                </c:if>
                                                <c:if test="${verDescricao and not empty produto.descricao}">
                                                    <span class="sh-prod-desc"><c:out value="${produto.descricao}" /></span>
                                                </c:if>
                                            </td>
                                            <c:if test="${verCategoria}"><td role="cell" class="v3-c-cat" data-label="Categoria"><c:out value="${produto.categoria.nome}" /></td></c:if>
                                            <c:if test="${verPreco}"><td role="cell" class="v3-c-preco sh-col-num v3-mono" data-label="Preço">R$ <fmt:formatNumber value="${produto.preco}" pattern="#,##0.00" /></td></c:if>
                                            <td role="cell" class="v3-c-saldo" data-label="Saldo">
                                                <div class="v3-saldo">
                                                    <span class="v3-saldo-fixo v3-so-semjs"><strong class="v3-mono"><fmt:formatNumber value="${produto.quantidade}" pattern="#,##0" /></strong> un.</span>
                                                    <div class="v3-aj v3-so-js" role="group" data-v3-aj
                                                        aria-label="Ajuste rápido do saldo de <c:out value='${produto.nome}'/>">
                                                        <button type="button" class="v3-aj__btn" data-v3-menos aria-disabled="${produto.quantidade <= 0}"
                                                            aria-label="Tirar 1 unidade de <c:out value='${produto.nome}'/>">
                                                            <i class="bi bi-dash-lg" aria-hidden="true"></i>
                                                        </button>
                                                        <input class="v3-aj__num v3-mono" type="text" inputmode="numeric" autocomplete="off" spellcheck="false"
                                                            value="<fmt:formatNumber value='${produto.quantidade}' pattern='#,##0' />" data-v3-num
                                                            aria-label="Saldo de <c:out value='${produto.nome}'/>, em unidades"
                                                            aria-describedby="v3-dica-ajuste">
                                                        <button type="button" class="v3-aj__btn" data-v3-mais
                                                            aria-label="Somar 1 unidade em <c:out value='${produto.nome}'/>">
                                                            <i class="bi bi-plus-lg" aria-hidden="true"></i>
                                                        </button>
                                                        <div class="v3-aj__confirma" data-v3-confirma hidden>
                                                            <span class="v3-aj__confirma-txt">Contar <strong class="v3-mono" data-v3-alvo></strong> un.</span>
                                                            <button type="button" class="v3-aj__ok" data-v3-ok>Confirmar<span class="visually-hidden"> contagem de <c:out value="${produto.nome}" /></span></button>
                                                            <button type="button" class="v3-aj__cancela" data-v3-cancelar aria-label="Cancelar contagem de <c:out value='${produto.nome}'/>"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
                                                        </div>
                                                    </div>
                                                    <div class="v3-saldo__linha">
                                                        <sh:medidorEstoque saldo="${produto.quantidade}" minimo="${produto.estoqueMinimo}" />
                                                        <c:if test="${produto.estoqueMinimo > 0}">
                                                            <span class="v3-saldo__min">mín. <span class="v3-mono"><fmt:formatNumber value="${produto.estoqueMinimo}" pattern="#,##0" /></span></span>
                                                        </c:if>
                                                    </div>
                                                </div>
                                            </td>
                                            <c:if test="${verSituacao}"><td role="cell" class="v3-c-sit" data-label="Situação"><span data-v3-selo><sh:badgeEstoque nivel="${produto.nivelEstoque}" /></span></td></c:if>
                                            <c:if test="${verTamanho}"><td role="cell" class="v3-c-ramo" data-label="Tamanho"><c:out value="${empty produto.tamanho ? '—' : produto.tamanho}" /></td></c:if>
                                            <c:if test="${verCor}"><td role="cell" class="v3-c-ramo" data-label="Cor"><c:out value="${empty produto.cor ? '—' : produto.cor}" /></td></c:if>
                                            <c:if test="${verLote}"><td role="cell" class="v3-c-ramo" data-label="Lote"><c:out value="${empty produto.lote ? '—' : produto.lote}" /></td></c:if>
                                            <c:if test="${verValidade}">
                                                <td role="cell" class="v3-c-ramo" data-label="Validade">
                                                    <c:choose>
                                                        <c:when test="${not empty produto.dataValidade}">
                                                            <fmt:parseDate value="${produto.dataValidade}" pattern="yyyy-MM-dd" var="dataVal" type="date" />
                                                            <fmt:formatDate value="${dataVal}" pattern="dd/MM/yyyy" />
                                                        </c:when>
                                                        <c:otherwise>—</c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </c:if>
                                            <c:if test="${verSerie}"><td role="cell" class="v3-c-ramo" data-label="Série"><c:out value="${empty produto.numeroSerie ? '—' : produto.numeroSerie}" /></td></c:if>
                                            <td role="cell" class="v3-c-acoes" data-label="">
                                                <div class="v3-acoes">
                                                    <a class="btn btn-ghost btn-sm v3-icone" href="<c:out value='${ctx}/estoque?movProduto=${produto.id}'/>" title="Movimentar">
                                                        <i class="bi bi-arrow-left-right" aria-hidden="true"></i><span class="v3-acao__txt">Movimentar</span><span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                    </a>
                                                    <a class="btn btn-ghost btn-sm v3-icone" href="<c:out value='${ctx}/produtos/${produto.id}/editar'/>" title="Editar">
                                                        <i class="bi bi-pencil" aria-hidden="true"></i><span class="v3-acao__txt">Editar</span><span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                    </a>
                                                    <c:if test="${podeGerenciar}">
                                                    <form method="post" action="<c:out value='${ctx}/produtos/${produto.id}/excluir'/>"
                                                        data-confirmar-exclusao data-tipo="o produto" data-nome="<c:out value='${produto.nome}'/>"
                                                        data-aviso="Sem movimentações além do estoque inicial, o produto é excluído de vez. Se já tem histórico, ele é inativado: some das listas e não aceita mais movimentação, e o histórico fica preservado.">
                                                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                                        <button type="submit" class="btn btn-ghost btn-ghost--perigo btn-sm v3-icone" title="Excluir">
                                                            <i class="bi bi-trash3" aria-hidden="true"></i><span class="v3-acao__txt">Excluir</span><span class="visually-hidden"> <c:out value="${produto.nome}" /></span>
                                                        </button>
                                                    </form>
                                                    </c:if>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                            <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalProdutosFiltro}"
                                tamanho="${tamanho}" base="/produtos" extra="${extraPaginacao}" rotulo="Paginação de produtos" />
                        </div>

                        <p class="v3-dica-teclas v3-so-js">
                            <kbd>j</kbd> <kbd>k</kbd> navegam pelas linhas, <kbd>Enter</kbd> abre os detalhes. No saldo, <kbd>&uarr;</kbd> <kbd>&darr;</kbd> somam ou tiram uma unidade.
                        </p>

                        <%-- Barra de seleção (só JS). A única ação em lote é exportar o CSV dos filtros atuais. --%>
                        <div class="v3-lote" role="region" aria-label="Ações para os produtos selecionados" data-v3-lote hidden>
                            <p class="v3-lote__txt">
                                <strong class="v3-mono" data-v3-lote-n>0</strong> <span data-v3-lote-rotulo>selecionados</span>
                                <span class="v3-lote__nota" data-v3-lote-nota>A exportação traz os <fmt:formatNumber value="${totalProdutosFiltro}" pattern="#,##0" /> produtos do filtro atual.</span>
                            </p>
                            <div class="v3-lote__acoes">
                                <a class="btn btn-primary btn-sm" href="<c:out value='${urlCsv}'/>" data-v3-lote-csv>
                                    <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
                                </a>
                                <button type="button" class="btn btn-ghost btn-sm" data-v3-lote-limpar>Limpar seleção</button>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- Esqueleto: só aparece se a atualização ao digitar demorar --%>
            <div class="v3-esqueleto" data-busca-esqueleto hidden aria-hidden="true">
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
                <span class="sh-esqueleto v3-esq-linha"></span>
            </div>
        </div>
    </jsp:body>
</sh:layout>
