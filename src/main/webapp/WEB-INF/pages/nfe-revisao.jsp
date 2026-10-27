<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Revisão da importação de uma NF-e. Rota: GET /compras/nfe/importacoes/{token} (e o POST de confirmar, quando volta com erro).
    Dados do NfeController: revisao (RevisaoNfe: avisoVerificacao, avisos, lojaTemCnpj, destinatarioConfere, fornecedorConhecido,
    totalImportaveis, expiraEm, nota, itens), token, categorias, produtos (ativos), confirmacaoNfeForm (decisões iniciais);
    em erro de formato: erro e errosCampos (chaves como item[1].nome).
    Por item N (= numero): item[N].acao (VINCULAR|CRIAR|IGNORAR), item[N].produtoId, item[N].nome, item[N].categoriaId, item[N].preco.
    Itens FRACIONADO/IGNORADO só podem ser ignorados: saem travados, com um campo oculto acao=IGNORAR e o motivo.
    js/nfe-revisao.js mostra só o painel da ação escolhida (os campos dos outros painéis ficam desabilitados), conta o resumo e
    avisa quando a unidade do produto escolhido difere da nota (o custo do produto não é atualizado, como no servidor).
    Sem JavaScript: todos os painéis aparecem e o servidor usa só os campos da ação escolhida.
    O botão "Confirmar" fica na barra fixa (fora do formulário, ligado por form="nfe-form-confirmar"); "Cancelar" é outro formulário POST.
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="nota" value="${revisao.nota}" />
<c:set var="temErrosCampos" value="${not empty errosCampos}" />
<c:set var="totalItens" value="${fn:length(revisao.itens)}" />
<c:set var="semDest" value="${empty nota.destinatario}" />
<c:set var="destFalha" value="${revisao.lojaTemCnpj and revisao.destinatarioConfere == false}" />
<c:set var="destOk" value="${revisao.lojaTemCnpj and revisao.destinatarioConfere == true}" />

<sh:layout titulo="Revisar nota fiscal" ativo="compras" largura="larga" formulario="true"
    voltarUrl="/compras/nfe" voltarRotulo="Compras (NF-e)">
    <jsp:attribute name="migalhas">
        <li class="breadcrumb-item"><a href="${ctx}/dashboard">Início</a></li>
        <li class="breadcrumb-item"><a href="${ctx}/compras/nfe">Compras (NF-e)</a></li>
        <li class="breadcrumb-item active" aria-current="page">Revisar nota</li>
    </jsp:attribute>
    <jsp:attribute name="scripts">
        <script src="${ctx}/js/nfe-init.js"></script>
        <link rel="stylesheet" href="${ctx}/css/nfe.css">
        <script defer src="${ctx}/js/nfe-revisao.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Revisar nota fiscal"
            descricao="Confira a nota e decida o que fazer com cada item. Nada entra no estoque até você confirmar." />

        <%-- Aviso de verificação: sempre visível; o sistema nunca afirma que a nota é autêntica --%>
        <div class="sh-alert sh-alert--warning nfe-aviso-verificacao" role="note">
            <i class="bi bi-shield-exclamation" aria-hidden="true"></i>
            <div class="sh-alert__corpo">
                <span class="sh-alert__titulo">Nota não verificada</span>
                <c:out value="${revisao.avisoVerificacao}" />
            </div>
        </div>

        <c:if test="${not empty revisao.avisos}">
            <div class="sh-alert sh-alert--warning" role="note">
                <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                <div class="sh-alert__corpo">
                    <span class="sh-alert__titulo">Avisos sobre esta nota</span>
                    <ul class="nfe-lista-avisos">
                        <c:forEach var="aviso" items="${revisao.avisos}"><li><c:out value="${aviso}" /></li></c:forEach>
                    </ul>
                </div>
            </div>
        </c:if>

        <%-- Dados da nota --%>
        <section class="sh-card sh-secao" aria-labelledby="nfe-titulo-nota">
            <div class="sh-card__cab">
                <div>
                    <h2 class="sh-card__titulo" id="nfe-titulo-nota">Dados da nota</h2>
                    <p class="sh-card__desc">Lidos do arquivo XML. Confira com a nota que chegou na loja.</p>
                </div>
                <p class="nfe-expira">
                    <i class="bi bi-hourglass-split" aria-hidden="true"></i>
                    Esta revisão expira em <sh:dataHora valor="${revisao.expiraEm}" />
                </p>
            </div>
            <div class="sh-card__corpo">
                <dl class="nfe-dados">
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Fornecedor</dt>
                        <dd>
                            <strong><c:out value="${nota.emitente.nome}" /></strong>
                            <c:if test="${not empty nota.emitente.fantasia and nota.emitente.fantasia != nota.emitente.nome}">
                                <span class="nfe-fantasia">(<c:out value="${nota.emitente.fantasia}" />)</span>
                            </c:if>
                            <span class="nfe-doc"><sh:documentoBr valor="${nota.emitente.documento}" />
                                <c:if test="${not empty nota.emitente.uf}"> &middot; <c:out value="${nota.emitente.uf}" /></c:if></span>
                            <span class="nfe-fornecedor-estado">
                                <c:choose>
                                    <c:when test="${revisao.fornecedorConhecido}"><span class="sh-badge sh-badge--normal"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Fornecedor já cadastrado</span></c:when>
                                    <c:otherwise><span class="sh-badge sh-badge--ajuste"><i class="bi bi-plus-circle" aria-hidden="true"></i> Primeira nota deste fornecedor</span></c:otherwise>
                                </c:choose>
                            </span>
                        </dd>
                    </div>
                    <div class="nfe-dado">
                        <dt>Número da nota</dt>
                        <dd class="sh-mono"><c:out value="${nota.numero}" /></dd>
                    </div>
                    <div class="nfe-dado">
                        <dt>Data da compra</dt>
                        <dd><sh:dataHora valor="${nota.emitidaEm}" /></dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Tipo da operação</dt>
                        <dd><c:out value="${nota.naturezaOperacao}" /></dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Código da nota</dt>
                        <dd><sh:chaveNfe valor="${nota.chaveAcesso}" /></dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Comprador</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${semDest}">Não informado no arquivo.</c:when>
                                <c:otherwise>
                                    <strong><c:out value="${nota.destinatario.nome}" /></strong>
                                    <span class="nfe-doc"><sh:documentoBr valor="${nota.destinatario.documento}" />
                                        <c:if test="${not empty nota.destinatario.uf}"> &middot; <c:out value="${nota.destinatario.uf}" /></c:if></span>
                                </c:otherwise>
                            </c:choose>
                            <span class="nfe-fornecedor-estado">
                                <c:choose>
                                    <c:when test="${destOk}"><span class="sh-badge sh-badge--normal"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Confere com o CNPJ da loja</span></c:when>
                                    <c:when test="${destFalha}"><span class="sh-badge sh-badge--sem"><i class="bi bi-x-octagon-fill" aria-hidden="true"></i> Diferente do CNPJ da loja</span></c:when>
                                    <c:otherwise><span class="sh-badge sh-badge--baixo"><i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i> Não conferido: loja sem CNPJ</span></c:otherwise>
                                </c:choose>
                            </span>
                        </dd>
                    </div>
                    <div class="nfe-dado nfe-dado--largo">
                        <dt>Detalhes fiscais</dt>
                        <dd>
                        <details class="nfe-detalhes">
                        <summary>Ver série, modelo e verificações</summary>
                        <p class="nfe-detalhes__linha">Série <span class="sh-mono"><c:out value="${nota.serie}" /></span> &middot; modelo <span class="sh-mono"><c:out value="${nota.modelo}" /></span></p>
                        <div class="nfe-presencas">
                            <span class="nfe-presenca">
                                <i class="bi ${nota.assinaturaPresente ? 'bi-patch-question' : 'bi-dash-circle'}" aria-hidden="true"></i>
                                <c:choose>
                                    <c:when test="${nota.assinaturaPresente}">Tem bloco de assinatura digital (não verificado)</c:when>
                                    <c:otherwise>Sem bloco de assinatura digital</c:otherwise>
                                </c:choose>
                            </span>
                            <span class="nfe-presenca">
                                <i class="bi ${nota.protocoloPresente ? 'bi-patch-question' : 'bi-dash-circle'}" aria-hidden="true"></i>
                                <c:choose>
                                    <c:when test="${nota.protocoloPresente}">Tem protocolo de autorização (não consultado na SEFAZ)</c:when>
                                    <c:otherwise>Sem protocolo de autorização</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        </details>
                        </dd>
                    </div>
                </dl>
                <dl class="nfe-valores">
                    <div><dt>Produtos</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorProdutos}" pattern="#,##0.00" /></dd></div>
                    <div><dt>Frete</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorFrete}" pattern="#,##0.00" /></dd></div>
                    <c:if test="${nota.valorSeguro != null and nota.valorSeguro > 0}"><div><dt>Seguro</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorSeguro}" pattern="#,##0.00" /></dd></div></c:if>
                    <c:if test="${nota.valorOutras != null and nota.valorOutras > 0}"><div><dt>Outras despesas</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorOutras}" pattern="#,##0.00" /></dd></div></c:if>
                    <div><dt>Desconto</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorDesconto}" pattern="#,##0.00" /></dd></div>
                    <div class="nfe-valores__total"><dt>Total da nota</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${nota.valorTotal}" pattern="#,##0.00" /></dd></div>
                </dl>
                <sh:cobrancaNfe pagamentos="${nota.pagamentos}" parcelas="${nota.parcelas}" />
            </div>
        </section>

        <%-- Resumo (atualizado pelo JavaScript conforme as escolhas) --%>
        <section class="nfe-resumo" aria-labelledby="nfe-titulo-resumo" data-nfe-resumo>
            <h2 class="visually-hidden" id="nfe-titulo-resumo">Resumo das suas escolhas</h2>
            <dl class="nfe-resumo__lista" aria-live="polite" aria-atomic="true">
                <div class="nfe-resumo__fig nfe-resumo__fig--entra">
                    <dt>Entram no estoque</dt>
                    <dd class="sh-mono" data-nfe-conta="entram">&ndash;</dd>
                    <dd class="nfe-resumo__nota">de <c:out value="${revisao.totalImportaveis}" /> que podem entrar</dd>
                </div>
                <div class="nfe-resumo__fig">
                    <dt>Produtos novos</dt>
                    <dd class="sh-mono" data-nfe-conta="criam">&ndash;</dd>
                    <dd class="nfe-resumo__nota">serão criados</dd>
                </div>
                <div class="nfe-resumo__fig">
                    <dt>Ignorados</dt>
                    <dd class="sh-mono" data-nfe-conta="ignoram">&ndash;</dd>
                    <dd class="nfe-resumo__nota">de <c:out value="${totalItens}" /> itens na nota</dd>
                </div>
            </dl>
        </section>

        <form id="nfe-form-confirmar" method="post"
            action="<c:out value='${ctx}/compras/nfe/importacoes/${token}/confirmar'/>" data-sh-validar data-sh-enviando="Importando…"
            data-nfe-revisao>
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

            <%-- Resumo de erros: dentro do formulário para o formularios.js preenchê-lo na validação do navegador;
                 vem preenchido do servidor quando a confirmação volta com erro de formato (errosCampos) --%>
            <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros ${temErrosCampos ? '' : 'hidden'}>
                <p class="sh-resumo-erros__titulo"><c:if test="${temErrosCampos}">Corrija os campos abaixo para continuar:</c:if></p>
                <ul>
                    <c:forEach var="e" items="${errosCampos}">
                        <c:set var="idCampo" value="${fn:replace(fn:replace(fn:replace(e.key, '[', '-'), ']', ''), '.', '-')}" />
                        <li><a href="#<c:out value='${idCampo}'/>" data-sh-ir-campo="<c:out value='${idCampo}'/>"><c:out value="${e.value}" /></a></li>
                    </c:forEach>
                </ul>
            </div>

            <h2 class="nfe-titulo-secao" id="nfe-titulo-itens">Itens da nota <span class="nfe-contagem">(<c:out value="${totalItens}" />)</span></h2>
            <ol class="nfe-itens" aria-labelledby="nfe-titulo-itens">
                <c:forEach var="it" items="${revisao.itens}">
                    <c:set var="n" value="${it.numero}" />
                    <c:set var="dec" value="${confirmacaoNfeForm.item[n]}" />
                    <c:set var="travado" value="${it.situacao != 'IMPORTADO' or it.bloqueado}" />
                    <li class="nfe-item ${travado ? 'nfe-item--travado' : ''}" data-nfe-item="<c:out value='${n}'/>"
                        data-unidade="<c:out value='${it.unidade}'/>" data-sugerido="<c:out value='${it.produtoSugeridoId}'/>">
                        <div class="nfe-item__cab">
                            <span class="nfe-item__numero">Item <c:out value="${n}" /></span>
                            <h3 class="nfe-item__nome"><c:out value="${it.descricao}" /></h3>
                            <c:choose>
                                <c:when test="${it.situacao == 'FRACIONADO'}">
                                    <span class="sh-badge sh-badge--baixo"><i class="bi bi-lock-fill" aria-hidden="true"></i> Não entra: quantidade fracionada</span>
                                </c:when>
                                <c:when test="${travado}">
                                    <span class="sh-badge sh-badge--sem"><i class="bi bi-slash-circle-fill" aria-hidden="true"></i> Recusado</span>
                                </c:when>
                            </c:choose>
                        </div>

                        <dl class="nfe-item__dados">
                            <div><dt>Quantidade</dt><dd class="sh-mono"><fmt:formatNumber value="${it.quantidade}" pattern="#,##0.####" /> <c:out value="${it.unidade}" /></dd></div>
                            <div><dt>Valor unitário</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${it.valorUnitario}" pattern="#,##0.00##" /></dd></div>
                            <c:if test="${it.desconto != null and it.desconto > 0}">
                                <div><dt>Desconto</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${it.desconto}" pattern="#,##0.00" /></dd></div>
                            </c:if>
                            <div><dt>Total</dt><dd class="sh-mono"><strong>R$ <fmt:formatNumber value="${it.valorTotal}" pattern="#,##0.00" /></strong></dd></div>
                            <c:if test="${it.rateio != null and it.rateio > 0}">
                                <div><dt title="Parte do frete, seguro e outras despesas da nota que coube a este item">Frete e outras despesas</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${it.rateio}" pattern="#,##0.00" /></dd></div>
                            </c:if>
                            <div><dt>Custo unitário</dt><dd class="sh-mono">R$ <fmt:formatNumber value="${it.custoUnitario}" pattern="#,##0.00##" /></dd></div>
                            <c:if test="${not empty it.codigoFornecedor}"><div><dt>Código do fornecedor</dt><dd class="sh-mono"><c:out value="${it.codigoFornecedor}" /></dd></div></c:if>
                            <c:if test="${not empty it.ean}"><div><dt>Código de barras</dt><dd class="sh-mono"><c:out value="${it.ean}" /></dd></div></c:if>
                            <c:if test="${not empty it.ncm}"><div><dt title="NCM">Classificação fiscal</dt><dd class="sh-mono"><c:out value="${it.ncm}" /></dd></div></c:if>
                            <c:if test="${not empty it.cfop}"><div><dt>Tipo da operação</dt><dd><sh:cfop codigo="${it.cfop}" /></dd></div></c:if>
                        </dl>

                        <c:forEach var="av" items="${it.avisos}">
                            <%-- O aviso de unidade é mostrado pelo painel de vínculo (abaixo), que também reage à troca de produto --%>
                            <c:if test="${not (it.unidadeDiferente and fn:startsWith(av, 'A unidade da nota'))}">
                                <%-- Avisos "será gravado" (EAN, NCM) valem para o produto sugerido: o JS os esconde se o dono trocar o produto --%>
                                <div class="sh-alert sh-alert--warning nfe-item__aviso" role="note"
                                    ${fn:contains(av, 'será gravado') ? 'data-nfe-so-sugerido' : ''}>
                                    <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                                    <div class="sh-alert__corpo"><c:out value="${av}" /></div>
                                </div>
                            </c:if>
                        </c:forEach>

                        <c:choose>
                            <c:when test="${travado}">
                                <input type="hidden" name="item[<c:out value='${n}'/>].acao" value="IGNORAR">
                                <p class="nfe-travado">
                                    <i class="bi bi-lock" aria-hidden="true"></i>
                                    <span>
                                        <strong>Este item será ignorado.</strong>
                                        <c:choose>
                                            <c:when test="${not empty it.motivoBloqueio}"><c:out value="${it.motivoBloqueio}" /></c:when>
                                            <c:when test="${it.situacao == 'FRACIONADO'}">O estoque aceita só quantidades inteiras. O item fica registrado na nota, mas não entra no estoque: lance a entrada à mão em Movimentações.</c:when>
                                            <c:otherwise>Este item não pode ser importado.</c:otherwise>
                                        </c:choose>
                                    </span>
                                </p>
                            </c:when>
                            <c:otherwise>
                                <c:set var="idAcao" value="item-${n}-acao" />
                                <fieldset class="nfe-acao">
                                    <legend class="nfe-acao__legenda">O que fazer com este item?</legend>
                                    <div class="nfe-seg" role="presentation">
                                        <input class="visually-hidden" type="radio" name="item[<c:out value='${n}'/>].acao" id="${idAcao}-VINCULAR" value="VINCULAR" data-nfe-acao ${dec.acao == 'VINCULAR' ? 'checked' : ''}>
                                        <label class="nfe-seg__opcao" for="${idAcao}-VINCULAR"><i class="bi bi-link-45deg" aria-hidden="true"></i> Vincular a produto existente</label>
                                        <input class="visually-hidden" type="radio" name="item[<c:out value='${n}'/>].acao" id="${idAcao}-CRIAR" value="CRIAR" data-nfe-acao ${dec.acao == 'CRIAR' ? 'checked' : ''}>
                                        <label class="nfe-seg__opcao" for="${idAcao}-CRIAR"><i class="bi bi-plus-circle" aria-hidden="true"></i> Criar produto novo</label>
                                        <input class="visually-hidden" type="radio" name="item[<c:out value='${n}'/>].acao" id="${idAcao}-IGNORAR" value="IGNORAR" data-nfe-acao ${dec.acao == 'IGNORAR' ? 'checked' : ''}>
                                        <label class="nfe-seg__opcao" for="${idAcao}-IGNORAR"><i class="bi bi-slash-circle" aria-hidden="true"></i> Ignorar item</label>
                                    </div>
                                </fieldset>

                                <%-- Painel: vincular --%>
                                <div class="nfe-painel" data-nfe-painel="VINCULAR" ${dec.acao == 'VINCULAR' ? '' : 'hidden'}>
                                    <c:set var="sugerido" value="${it.produtoSugeridoId != null}" />
                                    <c:choose>
                                        <c:when test="${sugerido}">
                                            <p class="nfe-sugestao">
                                                <i class="bi bi-lightbulb" aria-hidden="true"></i>
                                                <span>
                                                    <strong>Sugestão:</strong> <c:out value="${it.produtoSugeridoNome}" />
                                                    <span class="nfe-sugestao__motivo">
                                                        &ndash;
                                                        <c:choose>
                                                            <c:when test="${it.motivoSugestao == 'EAN'}">tem o mesmo código de barras.</c:when>
                                                            <c:when test="${it.motivoSugestao == 'CODIGO_FORNECEDOR'}">já foi comprado deste fornecedor com o mesmo código.</c:when>
                                                            <c:when test="${it.motivoSugestao == 'NOME'}">tem nome parecido. Confira se é o mesmo produto.</c:when>
                                                            <c:otherwise>sugerido pelo sistema.</c:otherwise>
                                                        </c:choose>
                                                    </span>
                                                </span>
                                            </p>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="nfe-sugestao nfe-sugestao--nenhuma">
                                                <i class="bi bi-info-circle" aria-hidden="true"></i>
                                                <span>Nenhum produto parecido foi encontrado. Escolha um da lista ou crie um produto novo.</span>
                                            </p>
                                        </c:otherwise>
                                    </c:choose>
                                    <c:set var="kProd" value="item[${n}].produtoId" />
                                    <c:set var="eProd" value="${errosCampos[kProd]}" />
                                    <label class="form-label" for="item-${n}-produtoId">Produto que recebe a entrada <span class="sh-obrigatorio">(obrigatório)</span></label>
                                    <select id="item-${n}-produtoId" class="form-select ${not empty eProd ? 'is-invalid' : ''}" name="item[<c:out value='${n}'/>].produtoId"
                                        data-nfe-produto data-sh-msg-obrigatorio="Escolha o produto do item ${n} que recebe a entrada."
                                        ${not empty eProd ? 'aria-invalid="true" aria-describedby="erro-item-' += n += '-produtoId"' : ''}>
                                        <option value="">Escolha um produto&hellip;</option>
                                        <c:forEach var="p" items="${produtos}">
                                            <option value="<c:out value='${p.id}'/>" data-unidade="<c:out value='${p.unidade}'/>"
                                                ${dec.produtoId == p.id ? 'selected' : ''}><c:out value="${p.nome}" /> (<c:out value="${p.unidade}" />)<c:if test="${it.produtoSugeridoId == p.id}"> &ndash; sugerido</c:if></option>
                                        </c:forEach>
                                    </select>
                                    <c:if test="${not empty eProd}">
                                        <div id="erro-item-${n}-produtoId" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span class="sh-erro-campo__texto"><c:out value="${eProd}" /></span></div>
                                    </c:if>
                                    <div class="sh-alert sh-alert--warning nfe-aviso-unidade" role="note" data-nfe-aviso-unidade
                                        ${(it.unidadeDiferente and dec.produtoId == it.produtoSugeridoId) ? '' : 'hidden'}>
                                        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
                                        <div class="sh-alert__corpo">
                                            <span class="sh-alert__titulo">Unidade diferente da nota</span>
                                            <span data-nfe-aviso-unidade-texto>A unidade da nota (<c:out value="${it.unidade}" />) é diferente da unidade do produto<c:if test="${sugerido}"> (<c:out value="${it.produtoSugeridoUnidade}" />)</c:if>.</span>
                                            O <strong>custo do produto não será atualizado</strong> e não há conversão automática: a quantidade da nota entra como está. Confira a quantidade.
                                        </div>
                                    </div>
                                </div>

                                <%-- Painel: criar --%>
                                <div class="nfe-painel" data-nfe-painel="CRIAR" ${dec.acao == 'CRIAR' ? '' : 'hidden'}>
                                    <c:set var="kNome" value="item[${n}].nome" />
                                    <c:set var="kCat" value="item[${n}].categoriaId" />
                                    <c:set var="kPreco" value="item[${n}].preco" />
                                    <c:set var="eNome" value="${errosCampos[kNome]}" />
                                    <c:set var="eCat" value="${errosCampos[kCat]}" />
                                    <c:set var="ePreco" value="${errosCampos[kPreco]}" />
                                    <div class="row g-3">
                                        <div class="col-12 col-lg-6">
                                            <label class="form-label" for="item-${n}-nome">Nome do produto <span class="sh-obrigatorio">(obrigatório)</span></label>
                                            <input id="item-${n}-nome" class="form-control ${not empty eNome ? 'is-invalid' : ''}" type="text" name="item[<c:out value='${n}'/>].nome"
                                                value="<c:out value='${dec.nome}'/>" maxlength="120" autocomplete="off" data-nfe-criar
                                                ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-item-' += n += '-nome"' : ''}>
                                            <c:if test="${not empty eNome}">
                                                <div id="erro-item-${n}-nome" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span class="sh-erro-campo__texto"><c:out value="${eNome}" /></span></div>
                                            </c:if>
                                        </div>
                                        <div class="col-12 col-sm-6 col-lg-3">
                                            <label class="form-label" for="item-${n}-categoriaId">Categoria <span class="sh-obrigatorio">(obrigatório)</span></label>
                                            <select id="item-${n}-categoriaId" class="form-select ${not empty eCat ? 'is-invalid' : ''}" name="item[<c:out value='${n}'/>].categoriaId" data-nfe-criar
                                                ${not empty eCat ? 'aria-invalid="true" aria-describedby="erro-item-' += n += '-categoriaId"' : ''}>
                                                <option value="">Escolha&hellip;</option>
                                                <c:forEach var="cat" items="${categorias}">
                                                    <option value="<c:out value='${cat.id}'/>" ${dec.categoriaId == cat.id ? 'selected' : ''}><c:out value="${cat.nome}" /></option>
                                                </c:forEach>
                                            </select>
                                            <c:if test="${not empty eCat}">
                                                <div id="erro-item-${n}-categoriaId" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span class="sh-erro-campo__texto"><c:out value="${eCat}" /></span></div>
                                            </c:if>
                                        </div>
                                        <div class="col-12 col-sm-6 col-lg-3">
                                            <label class="form-label" for="item-${n}-preco">Preço de venda <span class="sh-obrigatorio">(obrigatório)</span></label>
                                            <div class="input-group">
                                                <span class="input-group-text" aria-hidden="true">R$</span>
                                                <input id="item-${n}-preco" class="form-control sh-mono ${not empty ePreco ? 'is-invalid' : ''}" type="number" name="item[<c:out value='${n}'/>].preco"
                                                    value="<c:out value='${dec.preco}'/>" min="0.01" max="9999999.99" step="0.01" inputmode="decimal" data-nfe-criar
                                                    aria-describedby="dica-item-${n}-preco${not empty ePreco ? ' erro-item-' += n += '-preco' : ''}"
                                                    ${not empty ePreco ? 'aria-invalid="true"' : ''}>
                                            </div>
                                            <c:if test="${not empty ePreco}">
                                                <div id="erro-item-${n}-preco" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span class="sh-erro-campo__texto"><c:out value="${ePreco}" /></span></div>
                                            </c:if>
                                            <div id="dica-item-${n}-preco" class="form-text">Custo desta compra: R$ <fmt:formatNumber value="${it.custoUnitario}" pattern="#,##0.00##" /> por <c:out value="${it.unidade}" />.</div>
                                        </div>
                                    </div>
                                </div>

                                <%-- Painel: ignorar --%>
                                <div class="nfe-painel" data-nfe-painel="IGNORAR" ${dec.acao == 'IGNORAR' ? '' : 'hidden'}>
                                    <p class="nfe-ignorar"><i class="bi bi-slash-circle" aria-hidden="true"></i> Este item não será importado e não muda o estoque. Ele continua registrado na nota.</p>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </li>
                </c:forEach>
            </ol>

            <%-- Confirmação do destinatário --%>
            <c:set var="eConf" value="${errosCampos['confirmouDestinatario']}" />
            <section class="sh-card sh-secao nfe-confirma" aria-labelledby="nfe-titulo-confirma">
                <div class="sh-card__cab">
                    <h2 class="sh-card__titulo" id="nfe-titulo-confirma">Confirme que a nota é da sua loja</h2>
                </div>
                <div class="sh-card__corpo">
                    <c:choose>
                        <c:when test="${destFalha}">
                            <div class="sh-alert sh-alert--danger nfe-confirma__aviso" role="note">
                                <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
                                <div class="sh-alert__corpo">
                                    <span class="sh-alert__titulo">O destinatário desta nota não é o CNPJ da sua loja</span>
                                    Provavelmente esta nota foi emitida para outra empresa. Se importar, os produtos entram no seu estoque mesmo assim. Só continue se tiver certeza de que a mercadoria chegou na sua loja.
                                </div>
                            </div>
                        </c:when>
                        <c:when test="${destOk}">
                            <p class="nfe-confirma__ok"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> O destinatário da nota confere com o CNPJ cadastrado na sua loja.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="sh-alert sh-alert--info nfe-confirma__aviso" role="note">
                                <i class="bi bi-info-circle-fill" aria-hidden="true"></i>
                                <div class="sh-alert__corpo">
                                    <span class="sh-alert__titulo">Sua loja não tem CNPJ cadastrado</span>
                                    Por isso o sistema não consegue conferir o destinatário da nota. Veja o nome e o documento do destinatário em &ldquo;Dados da nota&rdquo; e confirme você mesmo.
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div class="form-check nfe-confirma__check">
                        <input class="form-check-input ${not empty eConf ? 'is-invalid' : ''}" type="checkbox" id="confirmouDestinatario" name="confirmouDestinatario" value="true"
                            data-nfe-confirma required data-sh-msg-obrigatorio="Marque a confirmação de que a nota é da sua loja para importar."
                            ${confirmacaoNfeForm.confirmouDestinatario ? 'checked' : ''}
                            ${not empty eConf ? 'aria-invalid="true" aria-describedby="erro-confirmouDestinatario"' : ''}>
                        <label class="form-check-label" for="confirmouDestinatario">
                            <c:choose>
                                <c:when test="${destFalha}">Confirmo que esta nota é da minha loja, mesmo com o destinatário diferente.</c:when>
                                <c:when test="${destOk}">Confirmo que esta nota de compra é da minha loja.</c:when>
                                <c:otherwise>Conferi o destinatário e confirmo que esta nota de compra é da minha loja.</c:otherwise>
                            </c:choose>
                            <span class="sh-obrigatorio">(obrigatório)</span>
                        </label>
                    </div>
                    <c:if test="${not empty eConf}">
                        <div id="erro-confirmouDestinatario" class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span class="sh-erro-campo__texto"><c:out value="${eConf}" /></span></div>
                    </c:if>
                </div>
            </section>
        </form>

        <%-- Barra fixa: Cancelar é outro formulário POST (com o modal de confirmação); Confirmar envia o formulário acima --%>
        <div class="sh-form-actions nfe-barra">
            <p class="nfe-barra__resumo" aria-hidden="true" data-nfe-resumo-curto></p>
            <p class="nfe-barra__bloqueio" id="nfe-bloqueio" data-nfe-bloqueio hidden>Escolha vincular ou criar em ao menos um item para poder confirmar.</p>
            <form method="post" class="m-0" action="<c:out value='${ctx}/compras/nfe/importacoes/${token}/cancelar'/>"
                data-confirmar-exclusao data-tipo="a importação pendente" data-nome="NF <c:out value='${nota.numero}'/> de <c:out value='${nota.emitente.nome}'/>"
                data-aviso="Nada foi gravado no estoque. Para importar a nota depois, envie o XML de novo.">
                <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                <button type="submit" class="btn btn-outline-secondary">Cancelar importação</button>
            </form>
            <button type="submit" class="btn btn-primary" form="nfe-form-confirmar" data-nfe-confirmar aria-describedby="nfe-bloqueio">
                <i class="bi bi-check2-circle" aria-hidden="true"></i> <span data-nfe-confirmar-rotulo>Confirmar e dar entrada</span>
            </button>
        </div>
    </jsp:body>
</sh:layout>
