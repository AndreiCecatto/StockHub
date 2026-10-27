<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Formulário de produto no novo design (lote 3). Rotas: GET /produtos/novo, GET /produtos/{id}/editar,
    POST /produtos (campos id, nome, descricao, categoriaId, preco, quantidade, estoqueMinimo: inalterados).
    Dados do ProdutoController: produto (ProdutoForm), categorias, erro, errosCampos (nome, descricao, categoriaId,
    preco, quantidade, estoqueMinimo). "edicao" é calculado aqui (produto.id != null).
    Cadastro: "Estoque inicial" (0 a 1.000.000; fica registrado como entrada, RN-04).
    Edição: o saldo é somente leitura (RN-03; o campo readonly ainda é enviado, mas o servidor o ignora) e muda só por
    movimentação. Excluir fica numa zona de perigo, num formulário separado com confirmação em modal (RN-08).
    Erros do servidor usam os mesmos ids (erro-nome...) e aria-describedby que o js/formularios.js usa no cliente.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="edicao" value="${produto.id != null}" />
<c:set var="eNome" value="${errosCampos['nome']}" />
<c:set var="eDescricao" value="${errosCampos['descricao']}" />
<c:set var="eCategoria" value="${errosCampos['categoriaId']}" />
<c:set var="ePreco" value="${errosCampos['preco']}" />
<c:set var="eQuantidade" value="${errosCampos['quantidade']}" />
<c:set var="eMinimo" value="${errosCampos['estoqueMinimo']}" />
<c:set var="eTamanho" value="${errosCampos['tamanho']}" />
<c:set var="eCor" value="${errosCampos['cor']}" />
<c:set var="eLote" value="${errosCampos['lote']}" />
<c:set var="eValidade" value="${errosCampos['dataValidade']}" />
<c:set var="eSerie" value="${errosCampos['numeroSerie']}" />
<c:set var="temErrosCampos" value="${not empty eTamanho or not empty eCor or not empty eLote or not empty eValidade or not empty eSerie or not empty eNome or not empty eDescricao or not empty eCategoria or not empty ePreco or not empty eQuantidade or not empty eMinimo}" />

<sh:layout titulo="${edicao ? 'Editar produto' : 'Novo produto'}" ativo="produtos" formulario="true"
    voltarUrl="/produtos" voltarRotulo="Produtos">
    <jsp:attribute name="migalhas">
        <li class="breadcrumb-item"><a href="${ctx}/dashboard">Início</a></li>
        <li class="breadcrumb-item"><a href="${ctx}/produtos">Produtos</a></li>
        <c:if test="${edicao}">
            <li class="breadcrumb-item"><a href="<c:out value='${ctx}/produtos/${produto.id}'/>"><c:out value="${empty produto.nome ? 'Produto' : produto.nome}" /></a></li>
        </c:if>
        <li class="breadcrumb-item active" aria-current="page"><c:out value="${edicao ? 'Editar' : 'Novo produto'}" /></li>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <script src="${ctx}/js/v3-prod-init.js"></script>
        <link rel="stylesheet" href="${ctx}/css/lote-produtos.css">
        <script defer src="${ctx}/js/produtos.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="${edicao ? 'Editar produto' : 'Novo produto'}"
            descricao="${edicao ? 'Altere nome, categoria, preço e estoque mínimo. O saldo só muda por movimentação.' : 'Informe os dados do item e o saldo com que ele começa.'}" />

        <c:if test="${empty categorias}">
            <div class="sh-alert sh-alert--warning" role="status">
                <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                <div class="sh-alert__corpo">
                    <span class="sh-alert__titulo">Você ainda não tem categorias.</span>
                    Todo produto precisa de uma categoria.
                    <a href="${ctx}/categorias/novo">Criar uma categoria</a> e voltar aqui.
                </div>
            </div>
        </c:if>

        <form method="post" action="${ctx}/produtos" data-sh-validar data-sh-avisar-alteracoes data-sh-enviando="Salvando…">
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
            <input type="hidden" name="id" value="<c:out value='${produto.id}'/>">

            <%-- Resumo de erros: o servidor preenche quando há erro por campo; o JS preenche ao enviar --%>
            <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros ${temErrosCampos ? '' : 'hidden'}>
                <p class="sh-resumo-erros__titulo"><c:if test="${temErrosCampos}">Corrija os campos abaixo para salvar o produto:</c:if></p>
                <ul>
                    <c:if test="${not empty eNome}"><li><a href="#nome" data-sh-ir-campo="nome"><c:out value="${eNome}" /></a></li></c:if>
                    <c:if test="${not empty eDescricao}"><li><a href="#descricao" data-sh-ir-campo="descricao"><c:out value="${eDescricao}" /></a></li></c:if>
                    <c:if test="${not empty eCategoria}"><li><a href="#categoriaId" data-sh-ir-campo="categoriaId"><c:out value="${eCategoria}" /></a></li></c:if>
                    <c:if test="${not empty ePreco}"><li><a href="#preco" data-sh-ir-campo="preco"><c:out value="${ePreco}" /></a></li></c:if>
                    <c:if test="${not empty eQuantidade}"><li><a href="#quantidade" data-sh-ir-campo="quantidade"><c:out value="${eQuantidade}" /></a></li></c:if>
                    <c:if test="${not empty eMinimo}"><li><a href="#estoqueMinimo" data-sh-ir-campo="estoqueMinimo"><c:out value="${eMinimo}" /></a></li></c:if>
                    <c:if test="${not empty eTamanho}"><li><a href="#tamanho" data-sh-ir-campo="tamanho"><c:out value="${eTamanho}" /></a></li></c:if>
                    <c:if test="${not empty eCor}"><li><a href="#cor" data-sh-ir-campo="cor"><c:out value="${eCor}" /></a></li></c:if>
                    <c:if test="${not empty eLote}"><li><a href="#lote" data-sh-ir-campo="lote"><c:out value="${eLote}" /></a></li></c:if>
                    <c:if test="${not empty eValidade}"><li><a href="#dataValidade" data-sh-ir-campo="dataValidade"><c:out value="${eValidade}" /></a></li></c:if>
                    <c:if test="${not empty eSerie}"><li><a href="#numeroSerie" data-sh-ir-campo="numeroSerie"><c:out value="${eSerie}" /></a></li></c:if>
                </ul>
            </div>

            <div class="v3-form-grade">
            <div class="v3-form-grade__campos">
            <%-- ===== Informações ===== --%>
            <section class="sh-card sh-secao" aria-labelledby="titulo-informacoes">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="titulo-informacoes">Informações</h2>
                </div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label" for="nome">Nome do produto <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <input id="nome" class="form-control ${not empty eNome ? 'is-invalid' : ''}" type="text" name="nome"
                                value="<c:out value='${produto.nome}'/>" placeholder="Ex.: Café torrado 500 g" minlength="2" maxlength="120"
                                autocomplete="off" required
                                ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-nome"' : ''}>
                            <c:if test="${not empty eNome}">
                                <div id="erro-nome" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eNome}" /></span>
                                </div>
                            </c:if>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="descricao">Descrição</label>
                            <textarea id="descricao" class="form-control ${not empty eDescricao ? 'is-invalid' : ''}" name="descricao"
                                rows="4" maxlength="500" placeholder="Detalhes que ajudam a reconhecer o produto (opcional)"
                                data-sh-contador="contador-descricao"
                                ${not empty eDescricao ? 'aria-invalid="true"' : ''}
                                aria-describedby="contador-descricao${not empty eDescricao ? ' erro-descricao' : ''}"><c:out value="${produto.descricao}" /></textarea>
                            <c:if test="${not empty eDescricao}">
                                <div id="erro-descricao" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eDescricao}" /></span>
                                </div>
                            </c:if>
                            <div id="contador-descricao" class="sh-contador">Até 500 caracteres</div>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="categoriaId">Categoria <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <select id="categoriaId" class="form-select ${not empty eCategoria ? 'is-invalid' : ''}" name="categoriaId" required
                                ${not empty eCategoria ? 'aria-invalid="true" aria-describedby="erro-categoriaId"' : ''}>
                                <option value="">Selecione uma categoria</option>
                                <c:forEach var="categoria" items="${categorias}">
                                    <option value="<c:out value='${categoria.id}'/>" ${produto.categoria.id == categoria.id ? 'selected' : ''}><c:out value="${categoria.nome}" /></option>
                                </c:forEach>
                            </select>
                            <c:if test="${not empty eCategoria}">
                                <div id="erro-categoriaId" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eCategoria}" /></span>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </section>

            <%-- ===== Estoque e preço ===== --%>
            <section class="sh-card sh-secao" aria-labelledby="titulo-estoque">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="titulo-estoque">Estoque e preço</h2>
                </div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="preco">Preço <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <div class="input-group sh-prod-preco">
                                <span class="input-group-text" aria-hidden="true">R$</span>
                                <input id="preco" class="form-control ${not empty ePreco ? 'is-invalid' : ''}" type="number" name="preco"
                                    step="0.01" min="0" max="9999999.99" inputmode="decimal" placeholder="0,00"
                                    value="<c:out value='${produto.preco}'/>" required data-prod-preco
                                    aria-describedby="dica-preco${not empty ePreco ? ' erro-preco' : ''}"
                                    ${not empty ePreco ? 'aria-invalid="true"' : ''}>
                            </div>
                            <c:if test="${not empty ePreco}">
                                <div id="erro-preco" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${ePreco}" /></span>
                                </div>
                            </c:if>
                            <div id="dica-preco" class="form-text">Em reais, com até 2 casas decimais.</div>
                        </div>

                        <div class="col-12 col-md-4">
                            <c:choose>
                                <c:when test="${edicao}">
                                    <%-- Edição: o saldo só muda por movimentação (RN-03). Somente leitura. --%>
                                    <div class="sh-prod-saldo-atual">
                                        <label class="form-label" for="quantidade">Saldo atual (só leitura)</label>
                                        <input id="quantidade" class="form-control" type="number" name="quantidade"
                                            value="<c:out value='${produto.quantidade}'/>" readonly aria-readonly="true"
                                            aria-describedby="dica-quantidade${not empty eQuantidade ? ' erro-quantidade' : ''}">
                                        <p id="dica-quantidade" class="form-text mb-0">
                                            Para mudar o saldo, <a href="<c:out value='${ctx}/estoque?movProduto=${produto.id}'/>">registre uma movimentação</a>.
                                        </p>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <label class="form-label" for="quantidade">Estoque inicial <span class="sh-obrigatorio">(obrigatório)</span></label>
                                    <input id="quantidade" class="form-control ${not empty eQuantidade ? 'is-invalid' : ''}" type="number"
                                        name="quantidade" min="0" max="1000000" step="1" inputmode="numeric"
                                        value="<c:out value='${produto.quantidade}'/>" required
                                        aria-describedby="dica-quantidade${not empty eQuantidade ? ' erro-quantidade' : ''}"
                                        ${not empty eQuantidade ? 'aria-invalid="true"' : ''}>
                                    <div id="dica-quantidade" class="form-text">De 0 a 1.000.000. Fica registrado como uma entrada de estoque.</div>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty eQuantidade}">
                                <div id="erro-quantidade" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eQuantidade}" /></span>
                                </div>
                            </c:if>
                        </div>

                        <div class="col-12 col-md-4">
                            <label class="form-label" for="estoqueMinimo">Estoque mínimo <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <input id="estoqueMinimo" class="form-control ${not empty eMinimo ? 'is-invalid' : ''}" type="number"
                                name="estoqueMinimo" min="0" max="2000000000" step="1" inputmode="numeric"
                                value="<c:out value='${produto.estoqueMinimo}'/>" required
                                aria-describedby="dica-minimo${not empty eMinimo ? ' erro-estoqueMinimo' : ''}"
                                ${not empty eMinimo ? 'aria-invalid="true"' : ''}>
                            <c:if test="${not empty eMinimo}">
                                <div id="erro-estoqueMinimo" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eMinimo}" /></span>
                                </div>
                            </c:if>
                            <div id="dica-minimo" class="form-text">Quando o saldo chegar a este número ou ficar abaixo, o produto aparece como Baixo.</div>
                        </div>
                    </div>
                </div>
            </section>

            <%-- ===== Detalhes do ramo: só os campos que o ramo da loja usa (camposRamo); todos opcionais ===== --%>
            <c:if test="${camposRamo.temAlgum}">
            <section class="sh-card sh-secao" aria-labelledby="titulo-ramo">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="titulo-ramo">Detalhes do produto</h2>
                </div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <c:if test="${camposRamo.tamanho}">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="tamanho">Tamanho (opcional)</label>
                            <input id="tamanho" class="form-control ${not empty eTamanho ? 'is-invalid' : ''}" type="text" name="tamanho"
                                value="<c:out value='${produto.tamanho}'/>" maxlength="20" placeholder="Ex.: M, 42" autocomplete="off"
                                ${not empty eTamanho ? 'aria-invalid="true" aria-describedby="erro-tamanho"' : ''}>
                            <c:if test="${not empty eTamanho}">
                                <div id="erro-tamanho" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eTamanho}" /></span></div>
                            </c:if>
                        </div>
                        </c:if>
                        <c:if test="${camposRamo.cor}">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="cor">Cor (opcional)</label>
                            <input id="cor" class="form-control ${not empty eCor ? 'is-invalid' : ''}" type="text" name="cor"
                                value="<c:out value='${produto.cor}'/>" maxlength="40" placeholder="Ex.: Azul-marinho" autocomplete="off"
                                ${not empty eCor ? 'aria-invalid="true" aria-describedby="erro-cor"' : ''}>
                            <c:if test="${not empty eCor}">
                                <div id="erro-cor" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eCor}" /></span></div>
                            </c:if>
                        </div>
                        </c:if>
                        <c:if test="${camposRamo.lote}">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="lote">Lote (opcional)</label>
                            <input id="lote" class="form-control ${not empty eLote ? 'is-invalid' : ''}" type="text" name="lote"
                                value="<c:out value='${produto.lote}'/>" maxlength="40" autocomplete="off"
                                ${not empty eLote ? 'aria-invalid="true" aria-describedby="erro-lote"' : ''}>
                            <c:if test="${not empty eLote}">
                                <div id="erro-lote" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eLote}" /></span></div>
                            </c:if>
                        </div>
                        </c:if>
                        <c:if test="${camposRamo.validade}">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="dataValidade">Validade (opcional)</label>
                            <input id="dataValidade" class="form-control ${not empty eValidade ? 'is-invalid' : ''}" type="date" name="dataValidade"
                                value="<c:out value='${produto.dataValidade}'/>"
                                aria-describedby="dica-validade${not empty eValidade ? ' erro-dataValidade' : ''}"
                                ${not empty eValidade ? 'aria-invalid="true"' : ''}>
                            <c:if test="${not empty eValidade}">
                                <div id="erro-dataValidade" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eValidade}" /></span></div>
                            </c:if>
                            <div id="dica-validade" class="form-text">Deixe em branco se o produto não vence.</div>
                        </div>
                        </c:if>
                        <c:if test="${camposRamo.numeroSerie}">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="numeroSerie">Número de série (opcional)</label>
                            <input id="numeroSerie" class="form-control ${not empty eSerie ? 'is-invalid' : ''}" type="text" name="numeroSerie"
                                value="<c:out value='${produto.numeroSerie}'/>" maxlength="60" autocomplete="off"
                                ${not empty eSerie ? 'aria-invalid="true" aria-describedby="erro-numeroSerie"' : ''}>
                            <c:if test="${not empty eSerie}">
                                <div id="erro-numeroSerie" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eSerie}" /></span></div>
                            </c:if>
                        </div>
                        </c:if>
                    </div>
                </div>
            </section>
            </c:if>

            </div>

            <%-- ===== Prévia ao vivo: situação do estoque com os valores digitados ===== --%>
            <c:set var="pSaldo" value="${empty produto.quantidade ? 0 : produto.quantidade}" />
            <c:set var="pMin" value="${empty produto.estoqueMinimo ? 0 : produto.estoqueMinimo}" />
            <c:set var="pNivel" value="${pSaldo <= 0 ? 'SEM_ESTOQUE' : (pSaldo <= pMin ? 'BAIXO' : 'NORMAL')}" />
            <aside class="v3-previa v3-so-js" data-v3-previa-produto data-v3-nivel="<c:out value='${pNivel}'/>" aria-labelledby="titulo-previa">
                <h2 class="sh-eyebrow" id="titulo-previa">Prévia da situação</h2>
                <p class="v3-previa__linha">
                    <span class="v3-previa__num v3-mono" data-v3-prev-saldo><c:out value="${pSaldo}" /></span>
                    <span class="v3-previa__un">un.</span>
                    <span data-v3-prev-selo><sh:badgeEstoque nivel="${pNivel}" /></span>
                </p>
                <span class="sh-meter sh-meter--${pNivel == 'NORMAL' ? 'normal' : (pNivel == 'BAIXO' ? 'baixo' : 'sem')}" data-v3-prev-medidor ${pMin > 0 ? '' : 'hidden'} aria-hidden="true">
                    <span class="sh-meter__fill sh-w-0" data-v3-prev-enche></span>
                    <span class="sh-meter__minimo"></span>
                </span>
                <p class="v3-previa__texto" data-v3-prev-texto aria-live="polite"></p>
                <dl class="v3-dl v3-previa__dl">
                    <div class="v3-dl__item">
                        <dt>Valor em estoque</dt>
                        <dd class="v3-mono" data-v3-prev-valor>&mdash;</dd>
                    </div>
                </dl>
            </aside>
            </div>

            <div class="sh-form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/produtos">Cancelar</a>
                <button class="btn btn-primary" type="submit">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i> Salvar produto
                </button>
            </div>
        </form>

        <%-- ===== Zona de perigo (só na edição): formulário próprio, confirmação em modal (RN-08) ===== --%>
        <c:if test="${edicao and podeGerenciar}">
            <section class="sh-prod-perigo" aria-labelledby="titulo-perigo">
                <h2 class="sh-prod-perigo__titulo" id="titulo-perigo">Zona de perigo</h2>
                <p class="sh-prod-perigo__texto">
                    Sem movimentações além do estoque inicial, o produto é excluído de vez. Se já tem histórico, ele é
                    inativado: some das listas e não aceita mais movimentação, e o histórico fica preservado.
                </p>
                <form method="post" action="<c:out value='${ctx}/produtos/${produto.id}/excluir'/>"
                    data-confirmar-exclusao data-tipo="o produto" data-nome="<c:out value='${produto.nome}'/>"
                    data-aviso="Sem movimentações além do estoque inicial, o produto é excluído de vez. Se já tem histórico, ele é inativado: some das listas e não aceita mais movimentação, e o histórico fica preservado.">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                    <button type="submit" class="btn btn-outline-danger">
                        <i class="bi bi-trash3" aria-hidden="true"></i> Excluir produto
                    </button>
                </form>
            </section>
        </c:if>
    </jsp:body>
</sh:layout>
