<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Movimentações v3 ("central de operações"): console de lançamento à esquerda e histórico em forma de livro-razão à direita.
    Rotas e campos inalterados: GET /estoque (pagina, produto, tipo, de, ate), GET /estoque/exportar.csv,
    POST /estoque/movimentar (produtoId, tipo, quantidade, observacao).

    Dados do EstoqueController:
      produtos (id, nome, quantidade, estoqueMinimo; SEM a categoria), movimentacoes (criadoEm, produto.id/nome, tipo,
      quantidade, quantidadeAnterior, quantidadeNova, observacao), paginaAtual, totalPaginas, totalMovimentacoes, tipos,
      filtroProduto, filtroTipo, filtroDe, filtroAte, filtrosAtivos, parametrosFiltros,
      movimentacaoForm, erro, errosCampos (produtoId, tipo, quantidade, observacao), mensagem (flash de sucesso).

    Melhoria progressiva (tudo funciona sem JavaScript: POST comum, filtros por GET, validação nativa):
      js/combobox-produto.js  seletor de produto com busca (ARIA combobox) sobre o select;
      js/movimentacao.js      tipo, quantidade com - e +, prévia do saldo com medidor, motivos rápidos e atalhos;
      js/v3-estoque.js        envio por fetch (Accept: application/json + X-CSRF-TOKEN), item novo no topo do livro-razão,
                              último lançamento, toast, contagem de registros e respeito aos filtros ativos.
    O servidor ignora movProduto e movTipo: só o JS os lê e valida.
    Sem script nem style inline. Todo dado sai por c:out; o único POST leva o token CSRF.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Erros por campo (o servidor devolve só depois de recusar o envio) --%>
<c:set var="eProduto" value="${errosCampos['produtoId']}" />
<c:set var="eTipo" value="${errosCampos['tipo']}" />
<c:set var="eQuantidade" value="${errosCampos['quantidade']}" />
<c:set var="eObservacao" value="${errosCampos['observacao']}" />
<c:set var="temErrosCampos" value="${not empty eProduto or not empty eTipo or not empty eQuantidade or not empty eObservacao}" />

<%-- Tipo escolhido (Entrada é o padrão) e o que depende dele: tudo também vale sem JavaScript --%>
<c:set var="tipoSel" value="${empty movimentacaoForm.tipo ? 'ENTRADA' : movimentacaoForm.tipo}" />
<c:set var="ehAjuste" value="${tipoSel == 'AJUSTE'}" />

<%-- O item mais recente é o primeiro: só destaca depois do redirect de sucesso (flash "mensagem"), página 1 sem filtro --%>
<c:set var="destacarPrimeiro" value="${not empty mensagem and paginaAtual == 1 and not filtrosAtivos}" />

<%-- Datas legíveis no servidor (plano B sem JavaScript): "Hoje" e "Ontem" por comparação de aaaa-MM-dd --%>
<jsp:useBean id="agoraData" class="java.util.Date" />
<jsp:useBean id="ontemData" class="java.util.Date" />
<jsp:setProperty name="ontemData" property="time" value="${agoraData.time - 86400000}" />
<fmt:formatDate value="${agoraData}" pattern="yyyy-MM-dd" var="hojeIso" />
<fmt:formatDate value="${ontemData}" pattern="yyyy-MM-dd" var="ontemIso" />
<fmt:formatDate value="${agoraData}" pattern="yyyy" var="anoAtual" />

<fmt:setLocale value="pt_BR" scope="request" />

<sh:layout titulo="Movimentações" ativo="estoque" largura="larga">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/v3-estoque.css">
        <script defer src="${ctx}/js/combobox-produto.js"></script>
        <script defer src="${ctx}/js/movimentacao.js"></script>
        <script defer src="${ctx}/js/v3-estoque.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Movimentações" descricao="Registre entradas, saídas e ajustes. Cada linha do histórico mostra o saldo antes e depois.">
            <a class="btn btn-outline-secondary" href="${ctx}/produtos?tamanho=25">
                <i class="bi bi-box-seam" aria-hidden="true"></i> Ver produtos
            </a>
        </sh:cabecalhoPagina>

        <div class="v3e">
            <%-- ==================== Console de lançamento ==================== --%>
            <section id="registrar" class="v3e-console" aria-labelledby="titulo-registrar">
                <header class="v3e-console__cab">
                    <p class="sh-eyebrow">Lançamento</p>
                    <h2 class="v3e-console__titulo" id="titulo-registrar">Registrar movimentação</h2>
                </header>

                <c:choose>
                    <%-- Sem produtos não há o que movimentar --%>
                    <c:when test="${empty produtos}">
                        <sh:estadoVazio icone="box-seam" titulo="Cadastre um produto para movimentar"
                            texto="Entradas, saídas e ajustes são sempre feitos sobre um produto."
                            acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                    </c:when>
                    <c:otherwise>
                        <form id="form-movimentacao" class="v3e-form" method="post" action="${ctx}/estoque/movimentar"
                            data-sh-validar data-sh-ctrl-enter data-sh-enviando="Registrando…">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

                            <%-- Último lançamento feito sem recarregar (o JS preenche) --%>
                            <div class="v3e-recibo" data-v3e-recibo hidden></div>

                            <%-- Erro de regra devolvido pelo envio sem recarregar (o JS preenche e leva o foco para cá) --%>
                            <div class="sh-alert sh-alert--danger v3e-alerta" role="alert" tabindex="-1" data-v3e-alerta hidden>
                                <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
                                <div class="sh-alert__corpo" data-v3e-alerta-texto></div>
                            </div>

                            <%-- Resumo de erros: o servidor preenche quando há erro por campo; o JS preenche ao enviar --%>
                            <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros ${temErrosCampos ? '' : 'hidden'}>
                                <p class="sh-resumo-erros__titulo"><c:if test="${temErrosCampos}">Corrija os campos abaixo para continuar:</c:if></p>
                                <ul>
                                    <c:if test="${not empty eProduto}">
                                        <li><a href="#produtoId" data-sh-ir-campo="produtoId"><c:out value="${eProduto}" /></a></li>
                                    </c:if>
                                    <c:if test="${not empty eTipo}">
                                        <li><a href="#tipo-<c:out value='${tipoSel}'/>" data-sh-ir-campo="tipo-<c:out value='${tipoSel}'/>"><c:out value="${eTipo}" /></a></li>
                                    </c:if>
                                    <c:if test="${not empty eQuantidade}">
                                        <li><a href="#quantidade" data-sh-ir-campo="quantidade"><c:out value="${eQuantidade}" /></a></li>
                                    </c:if>
                                    <c:if test="${not empty eObservacao}">
                                        <li><a href="#observacao" data-sh-ir-campo="observacao"><c:out value="${eObservacao}" /></a></li>
                                    </c:if>
                                </ul>
                            </div>

                            <%-- 1. Produto. O select é a fonte dos dados e o campo que vai no POST; com JS fica oculto
                                 e o seletor com busca (combobox) assume a frente. data-saldo e data-minimo vêm do servidor. --%>
                            <div class="sh-combo" data-sh-combo>
                                <label class="form-label" for="produtoId">Produto <span class="sh-obrigatorio">(obrigatório)</span></label>
                                <select class="form-select ${not empty eProduto ? 'is-invalid' : ''}" id="produtoId" name="produtoId" required
                                    data-sh-msg-obrigatorio="Escolha o produto da movimentação."
                                    ${not empty eProduto ? 'aria-invalid="true"' : ''}
                                    aria-describedby="erro-produtoId">
                                    <option value="">Selecione um produto</option>
                                    <c:forEach var="produto" items="${produtos}">
                                        <option value="<c:out value='${produto.id}'/>"
                                            data-nome="<c:out value='${produto.nome}'/>"
                                            data-saldo="<c:out value='${produto.quantidade}'/>"
                                            data-minimo="<c:out value='${produto.estoqueMinimo}'/>"
                                            ${movimentacaoForm.produtoId == produto.id ? 'selected' : ''}><c:out value="${produto.nome}" /> (saldo: <c:out value="${produto.quantidade}" /> un.)</option>
                                    </c:forEach>
                                </select>
                                <div id="erro-produtoId" class="sh-erro-campo" ${not empty eProduto ? '' : 'hidden'}>
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eProduto}" /></span>
                                </div>
                                <p class="form-text" id="dica-produto" hidden>Digite parte do nome para buscar. Setas e Enter escolhem.</p>
                            </div>

                            <%-- 2. Tipo: controle segmentado (três rádios com o mesmo name) --%>
                            <fieldset class="v3e-seg" aria-describedby="erro-tipo">
                                <legend class="form-label">Tipo de movimentação</legend>
                                <div class="v3e-seg__trilho">
                                    <span class="v3e-seg__marca" aria-hidden="true"></span>
                                    <div class="v3e-seg__item">
                                        <input class="v3e-seg__input" type="radio" name="tipo" id="tipo-ENTRADA" value="ENTRADA" required
                                            ${tipoSel == 'ENTRADA' ? 'checked' : ''}>
                                        <label class="v3e-seg__opcao" for="tipo-ENTRADA">
                                            <span class="v3e-seg__sinal sh-mono" aria-hidden="true">+</span>
                                            <span class="v3e-seg__nome">Entrada</span>
                                        </label>
                                    </div>
                                    <div class="v3e-seg__item">
                                        <input class="v3e-seg__input" type="radio" name="tipo" id="tipo-SAIDA" value="SAIDA" required
                                            ${tipoSel == 'SAIDA' ? 'checked' : ''}>
                                        <label class="v3e-seg__opcao" for="tipo-SAIDA">
                                            <span class="v3e-seg__sinal sh-mono" aria-hidden="true">&minus;</span>
                                            <span class="v3e-seg__nome">Saída</span>
                                        </label>
                                    </div>
                                    <div class="v3e-seg__item">
                                        <input class="v3e-seg__input" type="radio" name="tipo" id="tipo-AJUSTE" value="AJUSTE" required
                                            ${tipoSel == 'AJUSTE' ? 'checked' : ''}>
                                        <label class="v3e-seg__opcao" for="tipo-AJUSTE">
                                            <span class="v3e-seg__sinal sh-mono" aria-hidden="true">=</span>
                                            <span class="v3e-seg__nome">Ajuste</span>
                                        </label>
                                    </div>
                                </div>
                                <div id="erro-tipo" class="sh-erro-campo" ${not empty eTipo ? '' : 'hidden'}>
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eTipo}" /></span>
                                </div>
                            </fieldset>

                            <%-- 3. Quantidade (no ajuste vira "Contagem final": aceita 0 e vai até 2.000.000.000) --%>
                            <div>
                                <label class="form-label" for="quantidade" id="rotulo-quantidade"><span id="rotulo-quantidade-texto"><c:out value="${ehAjuste ? 'Contagem final' : 'Quantidade'}" /></span> <span class="sh-obrigatorio">(obrigatório)</span></label>
                                <div class="v3e-qtd">
                                    <button type="button" class="btn btn-outline-secondary v3e-qtd__botao" data-sh-qtd="-1"
                                        aria-label="Diminuir a quantidade em 1" hidden>
                                        <i class="bi bi-dash-lg" aria-hidden="true"></i>
                                    </button>
                                    <input class="form-control sh-mono ${not empty eQuantidade ? 'is-invalid' : ''}" id="quantidade" name="quantidade"
                                        type="number" step="1" inputmode="numeric" autocomplete="off" required
                                        min="${ehAjuste ? '0' : '1'}" max="${ehAjuste ? '2000000000' : '1000000'}"
                                        value="<c:out value='${movimentacaoForm.quantidade}'/>"
                                        data-sh-msg-obrigatorio="${ehAjuste ? 'Informe a contagem final.' : 'Informe a quantidade.'}"
                                        aria-describedby="dica-quantidade erro-quantidade"
                                        ${not empty eQuantidade ? 'aria-invalid="true"' : ''}>
                                    <button type="button" class="btn btn-outline-secondary v3e-qtd__botao" data-sh-qtd="1"
                                        aria-label="Aumentar a quantidade em 1" hidden>
                                        <i class="bi bi-plus-lg" aria-hidden="true"></i>
                                    </button>
                                </div>
                                <div id="erro-quantidade" class="sh-erro-campo" ${not empty eQuantidade ? '' : 'hidden'}>
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eQuantidade}" /></span>
                                </div>
                                <p id="dica-quantidade" class="form-text"><c:choose>
                                    <c:when test="${tipoSel == 'SAIDA'}">Quantidade que saiu do estoque. Mínimo 1.</c:when>
                                    <c:when test="${ehAjuste}">Saldo que o produto deve ter depois do ajuste (pode ser 0).</c:when>
                                    <c:otherwise>Quantidade que entrou no estoque. Mínimo 1.</c:otherwise>
                                </c:choose></p>
                            </div>

                            <%-- Prévia "saldo atual → saldo depois" com selo e medidor (só com JS; os nós são fixos, o JS só troca texto e classe) --%>
                            <div class="v3e-previa" id="previa-saldo" hidden>
                                <div class="v3e-previa__cab">
                                    <span class="v3e-previa__titulo">Saldo depois</span>
                                    <span id="previa-selo" class="v3e-previa__selo"></span>
                                </div>
                                <div class="v3e-previa__numeros" aria-hidden="true">
                                    <span class="v3e-previa__antes sh-mono" id="previa-antes">&mdash;</span>
                                    <i class="bi bi-arrow-right v3e-previa__seta"></i>
                                    <span class="v3e-previa__depois sh-mono" id="previa-depois">&mdash;</span>
                                    <span class="v3e-previa__delta sh-mono" id="previa-delta"></span>
                                </div>
                                <span class="sh-meter v3e-medidor" id="previa-medidor" aria-hidden="true" hidden>
                                    <span class="v3e-medidor__fill" id="previa-fill"></span>
                                    <span class="sh-meter__minimo"></span>
                                </span>
                                <p class="v3e-previa__texto" id="previa-texto"></p>
                                <p class="v3e-previa__aviso" id="previa-aviso" hidden></p>
                                <p class="visually-hidden" id="previa-leitor" aria-live="polite" aria-atomic="true"></p>
                            </div>

                            <%-- Motivos rápidos: preenchem a observação (só com JS). Nunca existe o texto reservado "Estoque inicial". --%>
                            <div class="v3e-motivos" id="motivos" hidden>
                                <p class="form-label" id="rotulo-motivos">Motivo rápido <span class="sh-obrigatorio">(opcional)</span></p>
                                <div class="v3e-chips" role="group" aria-labelledby="rotulo-motivos">
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Compra de fornecedor" data-tipo="ENTRADA">Compra de fornecedor</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Devolução de cliente" data-tipo="ENTRADA">Devolução de cliente</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Reposição" data-tipo="ENTRADA">Reposição</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Venda" data-tipo="SAIDA">Venda</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Perda" data-tipo="SAIDA">Perda</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Quebra" data-tipo="SAIDA">Quebra</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Vencimento" data-tipo="SAIDA">Vencimento</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Furto ou extravio" data-tipo="SAIDA">Furto ou extravio</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Correção de contagem" data-tipo="AJUSTE">Correção de contagem</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Inventário" data-tipo="AJUSTE">Inventário</button>
                                    <button type="button" class="v3e-chip" aria-pressed="false" data-motivo="Erro de lançamento" data-tipo="AJUSTE">Erro de lançamento</button>
                                </div>
                            </div>

                            <%-- 4. Observação (obrigatória no ajuste) --%>
                            <div>
                                <label class="form-label" for="observacao">Observação <span class="sh-obrigatorio" id="obs-obrigatoria" ${ehAjuste ? '' : 'hidden'}>(obrigatória no ajuste)</span></label>
                                <textarea class="form-control ${not empty eObservacao ? 'is-invalid' : ''}" id="observacao" name="observacao"
                                    rows="2" maxlength="255" ${ehAjuste ? 'required' : ''}
                                    placeholder="Ex.: compra do fornecedor, venda, perda ou correção"
                                    data-sh-contador="contador-observacao"
                                    data-sh-msg-obrigatorio="Descreva o motivo do ajuste."
                                    aria-describedby="contador-observacao erro-observacao"
                                    ${not empty eObservacao ? 'aria-invalid="true"' : ''}><c:out value="${movimentacaoForm.observacao}" /></textarea>
                                <div id="erro-observacao" class="sh-erro-campo" ${not empty eObservacao ? '' : 'hidden'}>
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eObservacao}" /></span>
                                </div>
                                <div id="contador-observacao" class="sh-contador"></div>
                            </div>

                            <div class="v3e-enviar">
                                <button type="submit" class="btn btn-primary btn-lg w-100">
                                    <i class="bi bi-check2" aria-hidden="true"></i>
                                    <span id="rotulo-botao">Registrar movimentação</span>
                                </button>
                                <p class="v3e-dica-teclas">
                                    <span><kbd class="sh-tecla">Ctrl</kbd> <kbd class="sh-tecla">Enter</kbd> registra</span>
                                    <span><kbd class="sh-tecla">E</kbd> <kbd class="sh-tecla">S</kbd> <kbd class="sh-tecla">A</kbd> escolhem o tipo</span>
                                </p>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </section>

            <%-- ==================== Histórico: livro-razão ==================== --%>
            <section class="v3e-razao-card" aria-labelledby="titulo-historico"
                data-v3e-razao data-v3e-pagina="<c:out value='${paginaAtual}'/>"
                data-v3e-pode-desfazer="<c:out value='${podeDesfazer ? "sim" : "nao"}'/>"
                data-v3e-filtro-produto="<c:out value='${filtroProduto}'/>" data-v3e-filtro-tipo="<c:out value='${filtroTipo}'/>"
                data-v3e-filtro-de="<c:out value='${filtroDe}'/>" data-v3e-filtro-ate="<c:out value='${filtroAte}'/>"
                data-v3e-tamanho="50">
                <header class="v3e-razao-card__cab">
                    <div>
                        <p class="sh-eyebrow">Livro-razão</p>
                        <h2 class="v3e-razao-card__titulo" id="titulo-historico">Histórico</h2>
                        <p class="v3e-razao-card__desc">
                            <span class="sh-mono" data-v3e-total="<c:out value='${totalMovimentacoes}'/>"><fmt:formatNumber value="${totalMovimentacoes}" pattern="#,##0" /></span>
                            <span data-v3e-total-rotulo><c:out value="${totalMovimentacoes == 1 ? 'registro' : 'registros'}" /></span><c:if test="${filtrosAtivos}"> com os filtros aplicados</c:if>,
                            do mais recente ao mais antigo.
                        </p>
                    </div>
                    <c:if test="${totalMovimentacoes > 0}">
                        <a class="btn btn-outline-secondary btn-sm"
                            href="<c:out value='${ctx}/estoque/exportar.csv?pagina=1${parametrosFiltros}'/>">
                            <i class="bi bi-download" aria-hidden="true"></i> Exportar CSV
                        </a>
                    </c:if>
                </header>

                <%-- Filtros (GET, funcionam sem JavaScript): produto, tipo, de, ate --%>
                <form class="v3e-filtros" method="get" action="${ctx}/estoque" aria-label="Filtros do histórico">
                    <div class="v3e-filtros__campo v3e-filtros__produto">
                        <label class="form-label" for="filtro-produto">Produto</label>
                        <select class="form-select" id="filtro-produto" name="produto">
                            <option value="">Todos os produtos</option>
                            <c:set var="produtoNaLista" value="${false}" />
                            <c:set var="nomeFiltroProduto" value="" />
                            <c:forEach var="produto" items="${produtos}">
                                <c:if test="${filtroProduto == produto.id}">
                                    <c:set var="produtoNaLista" value="${true}" />
                                    <c:set var="nomeFiltroProduto" value="${produto.nome}" />
                                </c:if>
                                <option value="<c:out value='${produto.id}'/>" ${filtroProduto == produto.id ? 'selected' : ''}><c:out value="${produto.nome}" /></option>
                            </c:forEach>
                            <%-- Produto filtrado que saiu da lista (inativo): mantém o filtro ao reenviar --%>
                            <c:if test="${not empty filtroProduto and not produtoNaLista}">
                                <option value="<c:out value='${filtroProduto}'/>" selected>Produto <c:out value="${filtroProduto}" /> (fora da lista)</option>
                            </c:if>
                        </select>
                    </div>
                    <div class="v3e-filtros__campo v3e-filtros__tipo">
                        <label class="form-label" for="filtro-tipo">Tipo</label>
                        <select class="form-select" id="filtro-tipo" name="tipo">
                            <option value="">Todos os tipos</option>
                            <option value="ENTRADA" ${filtroTipo == 'ENTRADA' ? 'selected' : ''}>Entrada</option>
                            <option value="SAIDA" ${filtroTipo == 'SAIDA' ? 'selected' : ''}>Saída</option>
                            <option value="AJUSTE" ${filtroTipo == 'AJUSTE' ? 'selected' : ''}>Ajuste</option>
                        </select>
                    </div>
                    <div class="v3e-filtros__campo">
                        <label class="form-label" for="filtro-de">De</label>
                        <input class="form-control sh-mono" id="filtro-de" name="de" type="date" value="<c:out value='${filtroDe}'/>">
                    </div>
                    <div class="v3e-filtros__campo">
                        <label class="form-label" for="filtro-ate">Até</label>
                        <input class="form-control sh-mono" id="filtro-ate" name="ate" type="date" value="<c:out value='${filtroAte}'/>">
                    </div>
                    <div class="v3e-filtros__acoes">
                        <button type="submit" class="btn btn-outline-secondary">
                            <i class="bi bi-funnel" aria-hidden="true"></i> Filtrar
                        </button>
                        <c:if test="${filtrosAtivos}">
                            <a class="btn btn-ghost" href="${ctx}/estoque">
                                <i class="bi bi-x-lg" aria-hidden="true"></i> Limpar filtros
                            </a>
                        </c:if>
                    </div>
                </form>

                <c:if test="${filtrosAtivos}">
                    <p class="v3e-ativos">
                        <span class="v3e-ativos__rotulo">Filtros ativos</span>
                        <c:if test="${not empty filtroProduto}"><span class="v3e-ativos__item">Produto <strong><c:out value="${empty nomeFiltroProduto ? filtroProduto : nomeFiltroProduto}" /></strong></span></c:if>
                        <c:if test="${not empty filtroTipo}"><span class="v3e-ativos__item">Tipo <strong><c:out value="${filtroTipo == 'ENTRADA' ? 'Entrada' : (filtroTipo == 'SAIDA' ? 'Saída' : 'Ajuste')}" /></strong></span></c:if>
                        <c:if test="${not empty filtroDe}"><span class="v3e-ativos__item">De <strong class="sh-mono"><c:out value="${fn:substring(filtroDe, 8, 10)}/${fn:substring(filtroDe, 5, 7)}/${fn:substring(filtroDe, 0, 4)}" /></strong></span></c:if>
                        <c:if test="${not empty filtroAte}"><span class="v3e-ativos__item">Até <strong class="sh-mono"><c:out value="${fn:substring(filtroAte, 8, 10)}/${fn:substring(filtroAte, 5, 7)}/${fn:substring(filtroAte, 0, 4)}" /></strong></span></c:if>
                    </p>
                </c:if>

                <%-- Estados vazios (um só aparece; o JS os esconde quando entra a primeira linha) --%>
                <c:if test="${empty movimentacoes}">
                    <div class="v3e-vazio" data-v3e-vazio>
                        <c:choose>
                            <c:when test="${filtrosAtivos}">
                                <sh:estadoVazio icone="funnel" titulo="Nenhuma movimentação com esses filtros"
                                    texto="Tente outro produto, tipo ou período."
                                    acaoUrl="/estoque" acaoRotulo="Limpar filtros" />
                            </c:when>
                            <c:when test="${empty produtos}">
                                <sh:estadoVazio icone="clock-history" titulo="Nenhuma movimentação ainda"
                                    texto="Cadastre um produto e registre a primeira entrada para começar o histórico."
                                    acaoUrl="/produtos/novo" acaoRotulo="Cadastrar produto" />
                            </c:when>
                            <c:otherwise>
                                <sh:estadoVazio icone="clock-history" titulo="Nenhuma movimentação ainda"
                                    texto="Registre a primeira entrada, saída ou ajuste para começar o histórico."
                                    acaoUrl="/estoque#registrar" acaoRotulo="Registrar a primeira" />
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:if>

                <%-- Colunas do livro-razão (decorativas: cada linha tem os rótulos escondidos para o leitor de tela) --%>
                <div class="v3e-colunas" aria-hidden="true" data-v3e-colunas ${empty movimentacoes ? 'hidden' : ''}>
                    <span>Hora</span><span>Tipo</span><span>Produto</span><span class="v3e-colunas__dir">Quantidade</span><span class="v3e-colunas__dir">Saldo</span>
                </div>

                <%-- Linhas agrupadas por dia. criadoEm é LocalDateTime (não é java.util.Date, então fmt:formatDate não serve):
                     toString() = "aaaa-MM-ddTHH:mm[:ss[.n]]", sempre com ao menos 16 caracteres, então fn:substring monta
                     dd/MM e HH:mm de forma segura. --%>
                <div class="v3e-grupos" data-v3e-grupos data-sh-lista-teclado>
                    <c:set var="diaAnterior" value="" />
                    <c:forEach var="mov" items="${movimentacoes}" varStatus="st">
                        <c:set var="dt" value="${mov.criadoEm}" />
                        <c:set var="dia" value="${fn:substring(dt, 0, 10)}" />
                        <c:if test="${dia != diaAnterior}">
                            <c:if test="${not st.first}"></ol></section></c:if>
                            <section class="v3e-dia" data-v3e-dia="<c:out value='${dia}'/>" aria-labelledby="dia-<c:out value='${dia}'/>">
                                <h3 class="v3e-dia__titulo" id="dia-<c:out value='${dia}'/>">
                                    <time datetime="<c:out value='${dia}'/>">
                                        <c:choose>
                                            <c:when test="${dia == hojeIso}"><strong>Hoje</strong> <span class="sh-mono"><c:out value="${fn:substring(dt, 8, 10)}/${fn:substring(dt, 5, 7)}" /></span></c:when>
                                            <c:when test="${dia == ontemIso}"><strong>Ontem</strong> <span class="sh-mono"><c:out value="${fn:substring(dt, 8, 10)}/${fn:substring(dt, 5, 7)}" /></span></c:when>
                                            <c:when test="${fn:substring(dt, 0, 4) == anoAtual}"><strong class="sh-mono"><c:out value="${fn:substring(dt, 8, 10)}/${fn:substring(dt, 5, 7)}" /></strong></c:when>
                                            <c:otherwise><strong class="sh-mono"><c:out value="${fn:substring(dt, 8, 10)}/${fn:substring(dt, 5, 7)}/${fn:substring(dt, 0, 4)}" /></strong></c:otherwise>
                                        </c:choose>
                                    </time>
                                    <span class="v3e-dia__n" data-v3e-dia-n></span>
                                </h3>
                                <ol class="v3e-lista">
                            <c:set var="diaAnterior" value="${dia}" />
                        </c:if>

                        <c:choose>
                            <c:when test="${mov.tipo == 'ENTRADA'}">
                                <c:set var="tCls" value="entrada" /><c:set var="tIcone" value="box-arrow-in-down" /><c:set var="tRotulo" value="Entrada" />
                            </c:when>
                            <c:when test="${mov.tipo == 'SAIDA'}">
                                <c:set var="tCls" value="saida" /><c:set var="tIcone" value="box-arrow-up" /><c:set var="tRotulo" value="Saída" />
                            </c:when>
                            <c:otherwise>
                                <c:set var="tCls" value="ajuste" /><c:set var="tIcone" value="sliders" /><c:set var="tRotulo" value="Ajuste" />
                            </c:otherwise>
                        </c:choose>

                        <li class="v3e-linha ${destacarPrimeiro and st.first ? 'v3e-linha--nova' : ''}" data-sh-linha>
                            <time class="v3e-hora sh-mono" datetime="<c:out value='${fn:substring(dt, 0, 16)}'/>"><span class="visually-hidden">às </span><c:out value="${fn:substring(dt, 11, 16)}" /></time>
                            <span class="v3e-tipo"><span class="sh-badge sh-badge--${tCls}"><i class="bi bi-${tIcone}" aria-hidden="true"></i><c:out value="${tRotulo}" /></span></span>
                            <div class="v3e-prod">
                                <a class="v3e-prod__nome" href="<c:out value='${ctx}/produtos/${mov.produto.id}'/>" data-sh-peek data-sh-peek-titulo="<c:out value='${mov.produto.nome}'/>"><c:out value="${mov.produto.nome}" /></a>
                                <c:if test="${not empty mov.observacao}">
                                    <p class="v3e-obs"><c:out value="${mov.observacao}" /></p>
                                </c:if>
                                <c:if test="${not empty mov.estornoDeId}">
                                    <p class="v3e-obs v3e-marca">Estorno da movimentação <span class="sh-mono">#<c:out value="${mov.estornoDeId}" /></span></p>
                                </c:if>
                                <c:if test="${jaDesfeitas.contains(mov.id)}">
                                    <p class="v3e-obs v3e-marca">Desfeita por um estorno</p>
                                </c:if>
                                <c:if test="${estornaveis.contains(mov.id)}">
                                    <details class="v3e-desfazer">
                                        <summary class="v3e-desfazer__abrir">Desfazer</summary>
                                        <form method="post" action="<c:out value='${ctx}/estoque/${mov.id}/estornar'/>" class="v3e-desfazer__form">
                                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                            <label class="visually-hidden" for="motivo-estorno-<c:out value='${mov.id}'/>">Motivo de desfazer</label>
                                            <input id="motivo-estorno-<c:out value='${mov.id}'/>" class="form-control form-control-sm" type="text" name="motivo"
                                                maxlength="200" required autocomplete="off" placeholder="Por que está desfazendo?">
                                            <button type="submit" class="btn btn-outline-secondary btn-sm">Confirmar</button>
                                        </form>
                                    </details>
                                </c:if>
                            </div>
                            <span class="v3e-qtd-col v3e-qtd-col--${tCls} sh-mono">
                                <span class="visually-hidden"><c:out value="${tRotulo}" /> de </span><c:choose>
                                    <c:when test="${mov.tipo == 'ENTRADA'}">+<fmt:formatNumber value="${mov.quantidade}" pattern="#,##0" /></c:when>
                                    <c:when test="${mov.tipo == 'SAIDA'}">&minus;<fmt:formatNumber value="${mov.quantidade}" pattern="#,##0" /></c:when>
                                    <c:otherwise>=<fmt:formatNumber value="${mov.quantidade}" pattern="#,##0" /></c:otherwise>
                                </c:choose><span class="visually-hidden"> un.<c:if test="${mov.tipo == 'AJUSTE'}"> (contagem final)</c:if></span>
                            </span>
                            <span class="v3e-saldo sh-mono">
                                <span class="visually-hidden">Saldo de </span><span class="v3e-saldo__antes"><fmt:formatNumber value="${mov.quantidadeAnterior}" pattern="#,##0" /></span>
                                <i class="bi bi-arrow-right" aria-hidden="true"></i><span class="visually-hidden"> para </span><strong><fmt:formatNumber value="${mov.quantidadeNova}" pattern="#,##0" /></strong>
                            </span>
                        </li>
                    </c:forEach>
                    <c:if test="${not empty movimentacoes}">
                                </ol>
                            </section>
                    </c:if>
                </div>

                <p class="v3e-rodape-teclas" aria-hidden="true">
                    <span><kbd class="sh-tecla">J</kbd> <kbd class="sh-tecla">K</kbd> navegam nas linhas</span>
                    <span><kbd class="sh-tecla">Enter</kbd> abre o produto</span>
                    <span><kbd class="sh-tecla">M</kbd> vai ao lançamento</span>
                    <span><kbd class="sh-tecla">F</kbd> filtra por produto</span>
                </p>

                <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalMovimentacoes}"
                    tamanho="50" base="/estoque" extra="${parametrosFiltros}" rotulo="Paginação do histórico de movimentações" />
            </section>
        </div>
    </jsp:body>
</sh:layout>
