<%@ tag description="Layout base v3: barra lateral, barra superior com barra de comando, barra inferior (celular), painel peek, toasts e scripts" body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Uso (em WEB-INF/pages/*.jsp):

      <%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
      <sh:layout titulo="Produtos" ativo="produtos">
          <jsp:attribute name="migalhas">...itens <li class="breadcrumb-item">...</li> da trilha (opcional)...</jsp:attribute>
          <jsp:attribute name="scripts"><script defer src="...js"></script></jsp:attribute>
          <jsp:body> ...conteúdo da página... </jsp:body>
      </sh:layout>

    Atributos:
      titulo       texto do <title> (obrigatório)
      ativo        item do menu atual: dashboard, estoque, produtos, categorias, estoqueBaixo, unidades, valorEstoque
      largura      "form" estreita o conteúdo para 960 px (formulários); "larga" abre para 1440 px; padrão 1200 px
      formulario   true nas telas de formulário: a barra inferior some e entra a barra de ações fixa
      voltarUrl    caminho (sem o contexto) do nível acima; vira o link "‹ Produtos" da barra superior no celular
      voltarRotulo texto do link acima
      migalhas     (fragmento) <li> da trilha; só em páginas com profundidade 2 ou mais
      scripts      (fragmento) <script defer src> específicos da página

    Peças que o layout já inclui: painel lateral "peek" (includes/sh/peek.jsp, js/peek.js), paleta de comandos,
    modais de exclusão e de atalhos, área de toasts (#sh-toasts) e o anúncio para leitores de tela (#sh-anuncio).
    Mensagens do servidor (mensagem, erro) saem no topo do conteúdo (includes/sh/alertas.jsp).
    Os formulários de saída (logout) usam POST com token CSRF.
--%>
<%@ attribute name="titulo" required="true" type="java.lang.String" %>
<%@ attribute name="ativo" required="false" type="java.lang.String" %>
<%@ attribute name="largura" required="false" type="java.lang.String" %>
<%@ attribute name="formulario" required="false" type="java.lang.Boolean" %>
<%@ attribute name="voltarUrl" required="false" type="java.lang.String" %>
<%@ attribute name="voltarRotulo" required="false" type="java.lang.String" %>
<%@ attribute name="migalhas" required="false" fragment="true" %>
<%@ attribute name="scripts" required="false" fragment="true" %>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="shTitulo" value="${titulo}" scope="request" />
<c:set var="shAtivo" value="${ativo}" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <jsp:include page="/WEB-INF/includes/sh/head.jsp" />
    <jsp:invoke fragment="scripts" />
</head>
<body class="${formulario ? 'sh-sem-inferior' : ''}" data-sh-ctx="<c:out value='${ctx}'/>">
    <a class="sh-pular" href="#conteudo">Pular para o conteúdo</a>
    <div class="sh-app">
        <jsp:include page="/WEB-INF/includes/sh/lateral.jsp" />
        <div class="sh-coluna">
            <header class="sh-topbar">
                <div class="sh-topbar__esq">
                    <c:choose>
                        <c:when test="${not empty voltarUrl}">
                            <a class="sh-topbar__voltar" href="<c:out value='${ctx}${voltarUrl}'/>">
                                <i class="bi bi-chevron-left" aria-hidden="true"></i>
                                <span><c:out value="${voltarRotulo}" /></span>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <a class="sh-topbar__marca" href="${ctx}/dashboard">
                                <sh:marca tamanho="28" />
                                <span>StockHub</span>
                            </a>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${not empty migalhas}">
                        <nav class="sh-migalhas" aria-label="Trilha de navegação">
                            <ol class="breadcrumb">
                                <jsp:invoke fragment="migalhas" />
                            </ol>
                        </nav>
                    </c:if>
                </div>
                <%-- Barra de comando (Ctrl K): parece um campo de busca, mas é um botão. Sem JavaScript, vale a busca comum. --%>
                <button type="button" class="sh-paleta-gatilho" data-sh-abrir-paleta data-sh-busca-global
                    aria-haspopup="dialog" aria-keyshortcuts="Control+K Meta+K" aria-label="Buscar ou executar um comando">
                    <i class="bi bi-search" aria-hidden="true"></i>
                    <span class="sh-paleta-gatilho__texto">Buscar ou executar um comando&hellip;</span>
                    <kbd class="sh-paleta-gatilho__tecla" data-sh-tecla-paleta aria-hidden="true">Ctrl K</kbd>
                </button>
                <noscript>
                    <form class="sh-busca-global" role="search" action="${ctx}/produtos" method="get">
                        <label class="visually-hidden" for="sh-busca-global">Buscar produtos</label>
                        <input id="sh-busca-global" class="form-control" type="search" name="busca" maxlength="120"
                            placeholder="Buscar produto" autocomplete="off">
                    </form>
                </noscript>
                <div class="sh-topbar__acoes">
                    <div class="dropdown sh-topbar__novo">
                        <button type="button" class="btn btn-primary btn-sm" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="bi bi-plus-lg" aria-hidden="true"></i> Novo
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-2" href="${ctx}/produtos/novo"
                                    data-sh-atalho-novo="padrao">
                                    <i class="bi bi-box-seam" aria-hidden="true"></i> Produto
                                </a>
                            </li>
                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-2" href="${ctx}/categorias/novo">
                                    <i class="bi bi-tags" aria-hidden="true"></i> Categoria
                                </a>
                            </li>
                        </ul>
                    </div>
                    <div class="dropdown">
                        <button type="button" class="sh-usuario" data-bs-toggle="dropdown" aria-expanded="false"
                            aria-label="Menu da conta <c:out value='${usuarioLogado.nome}'/>">
                            <span class="sh-usuario__inicial" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(usuarioLogado.nome, 0, 1))}" /></span>
                            <span class="sh-usuario__nome"><c:out value="${usuarioLogado.nome}" /></span>
                            <i class="bi bi-chevron-down" aria-hidden="true"></i>
                        </button>
                        <div class="dropdown-menu dropdown-menu-end sh-menu-usuario">
                            <div class="sh-menu-usuario__loja">Conta<strong><c:out value="${usuarioLogado.nome}" /></strong></div>
                            <p class="sh-menu-usuario__titulo" id="sh-menu-tema">Tema</p>
                            <div role="group" aria-labelledby="sh-menu-tema">
                                <jsp:include page="/WEB-INF/includes/sh/tema-opcoes.jsp">
                                    <jsp:param name="classe" value="dropdown-item" />
                                </jsp:include>
                            </div>
                            <hr class="dropdown-divider">
                            <button type="button" class="dropdown-item" data-sh-abrir-paleta>
                                <i class="bi bi-command" aria-hidden="true"></i> Barra de comando
                            </button>
                            <button type="button" class="dropdown-item" data-sh-abrir-atalhos>
                                <i class="bi bi-keyboard" aria-hidden="true"></i> Atalhos de teclado
                            </button>
                            <form method="post" action="${ctx}/logout" class="m-0">
                                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                <button type="submit" class="dropdown-item">
                                    <i class="bi bi-box-arrow-right" aria-hidden="true"></i> Sair
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </header>
            <main id="conteudo" class="sh-main" tabindex="-1">
                <div class="sh-container ${largura == 'form' ? 'sh-container--form' : (largura == 'larga' ? 'sh-container--larga' : '')}">
                    <jsp:include page="/WEB-INF/includes/sh/alertas.jsp" />
                    <jsp:doBody />
                </div>
            </main>
        </div>
    </div>
    <jsp:include page="/WEB-INF/includes/sh/inferior.jsp" />
    <jsp:include page="/WEB-INF/includes/sh/mais.jsp" />
    <jsp:include page="/WEB-INF/includes/sh/modal-exclusao.jsp" />
    <jsp:include page="/WEB-INF/includes/sh/modal-atalhos.jsp" />
    <jsp:include page="/WEB-INF/includes/sh/paleta.jsp" />
    <jsp:include page="/WEB-INF/includes/sh/peek.jsp" />
    <div id="sh-toasts" class="sh-toasts" aria-live="polite" aria-relevant="additions"></div>
    <div id="sh-anuncio" class="visually-hidden" role="status"></div>
</body>
</html>
