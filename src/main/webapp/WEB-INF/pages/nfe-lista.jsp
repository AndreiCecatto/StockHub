<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Compras (NF-e): lista e envio do XML. Rota: GET /compras/nfe?pagina=N.
    Dados do NfeController: notas (id, numero, serie, chaveAcesso, emitidaEm, importadaEm, valorTotal, parceiro.nome),
    pendentes (token, chaveAcesso, criadaEm, expiraEm), paginaAtual, totalPaginas, totalNotas, tamanhoMaximoMb.
    Envio: js/nfe-envio.js faz fetch POST em /compras/nfe/enviar (FormData "arquivo", X-CSRF-TOKEN, X-Requested-With,
    Accept: application/json). Sem JavaScript o formulário envia comum (multipart) e o servidor redireciona.
    Cancelar uma pendente usa o modal de confirmação do layout (data-confirmar-exclusao).
    Sem script nem style inline; todo dado sai por c:out.
--%>
<fmt:setLocale value="pt_BR" scope="request" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="maxMb" value="${empty tamanhoMaximoMb ? 2 : tamanhoMaximoMb}" />
<fmt:formatNumber var="maxBytes" value="${maxMb * 1048576}" pattern="0" groupingUsed="false" />

<sh:layout titulo="Compras (NF-e)" ativo="compras">
    <jsp:attribute name="scripts">
        <script src="${ctx}/js/nfe-init.js"></script>
        <link rel="stylesheet" href="${ctx}/css/nfe.css">
        <script defer src="${ctx}/js/nfe-envio.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Compras (NF-e)"
            descricao="Envie o XML da nota fiscal do fornecedor, confira os itens e dê entrada no estoque de uma vez." />

        <section class="sh-card sh-secao" aria-labelledby="nfe-titulo-envio">
            <div class="sh-card__cab">
                <div>
                    <h2 class="sh-card__titulo" id="nfe-titulo-envio">Importar uma nota</h2>
                    <p class="sh-card__desc">Nada entra no estoque até você revisar e confirmar na próxima tela.</p>
                </div>
            </div>
            <div class="sh-card__corpo">
                <form id="nfe-form-envio" class="nfe-envio" method="post" action="${ctx}/compras/nfe/enviar" enctype="multipart/form-data"
                    data-nfe-envio data-url="<c:out value='${ctx}/compras/nfe/enviar'/>" data-max-bytes="<c:out value='${maxBytes}'/>"
                    data-max-mb="<c:out value='${maxMb}'/>" data-login="<c:out value='${ctx}/login'/>">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                    <div class="nfe-zona" data-nfe-zona>
                        <i class="bi bi-file-earmark-code nfe-zona__icone" aria-hidden="true"></i>
                        <label class="form-label nfe-zona__titulo" for="nfe-arquivo">Arquivo XML da NF-e <span class="sh-obrigatorio">(obrigatório)</span></label>
                        <input id="nfe-arquivo" class="form-control" type="file" name="arquivo" accept=".xml,text/xml,application/xml"
                            required aria-describedby="nfe-ajuda-arquivo nfe-envio-estado">
                        <p class="form-text mb-0" id="nfe-ajuda-arquivo">
                            Escolha o arquivo <strong>.xml</strong> da nota (até <c:out value="${maxMb}" /> MB)<span class="nfe-so-js"> ou arraste-o até aqui</span>.
                        </p>
                    </div>
                    <p class="nfe-envio__estado" id="nfe-envio-estado" role="status" data-nfe-estado></p>
                    <div class="sh-alert sh-alert--danger nfe-envio__erro" id="nfe-envio-erro" role="alert" tabindex="-1" data-nfe-erro hidden>
                        <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
                        <div class="sh-alert__corpo" data-nfe-erro-texto></div>
                    </div>
                    <div class="nfe-envio__acoes">
                        <button type="submit" class="btn btn-primary" data-nfe-enviar>
                            <i class="bi bi-upload" aria-hidden="true"></i> <span data-nfe-enviar-rotulo>Enviar e revisar</span>
                        </button>
                    </div>
                </form>
            </div>
        </section>

        <c:if test="${not empty pendentes}">
            <section class="sh-secao" aria-labelledby="nfe-titulo-pendentes">
                <h2 class="nfe-titulo-secao" id="nfe-titulo-pendentes">Aguardando sua revisão
                    <span class="sh-badge sh-badge--baixo"><i class="bi bi-hourglass-split" aria-hidden="true"></i> <c:out value="${fn:length(pendentes)}" /></span>
                </h2>
                <p class="nfe-sub">Estas notas foram enviadas, mas ainda não entraram no estoque. Cada uma expira no prazo indicado.</p>
                <ul class="nfe-pendentes">
                    <c:forEach var="p" items="${pendentes}">
                        <li class="nfe-pendente">
                            <div class="nfe-pendente__info">
                                <p class="nfe-pendente__chave"><span class="visually-hidden">Código da nota: </span><sh:chaveNfe valor="${p.chaveAcesso}" /></p>
                                <p class="nfe-pendente__meta">
                                    Enviada em <sh:dataHora valor="${p.criadaEm}" />
                                    <span class="nfe-sep" aria-hidden="true">&middot;</span>
                                    Expira em <sh:dataHora valor="${p.expiraEm}" />
                                </p>
                            </div>
                            <div class="nfe-pendente__acoes">
                                <a class="btn btn-primary btn-sm" href="<c:out value='${ctx}/compras/nfe/importacoes/${p.token}'/>">
                                    <i class="bi bi-list-check" aria-hidden="true"></i> Continuar revisão
                                </a>
                                <form method="post" class="m-0" action="<c:out value='${ctx}/compras/nfe/importacoes/${p.token}/cancelar'/>"
                                    data-confirmar-exclusao data-tipo="a importação pendente" data-nome="<c:out value='${p.chaveAcesso}'/>"
                                    data-aviso="Nada foi gravado no estoque. Para importar a nota depois, envie o XML de novo.">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                                    <button type="submit" class="btn btn-outline-danger btn-sm">
                                        <i class="bi bi-x-lg" aria-hidden="true"></i> Cancelar importação
                                    </button>
                                </form>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </section>
        </c:if>

        <section class="sh-secao" aria-labelledby="nfe-titulo-notas">
            <h2 class="nfe-titulo-secao" id="nfe-titulo-notas">Notas importadas</h2>
            <c:choose>
                <c:when test="${empty notas and totalNotas > 0}">
                    <div class="sh-table-wrap">
                        <sh:estadoVazio icone="journal-x" titulo="Esta página não tem notas"
                            texto="A lista tem menos páginas do que a que você pediu."
                            acaoUrl="/compras/nfe" acaoRotulo="Voltar ao início da lista" />
                    </div>
                </c:when>
                <c:when test="${empty notas}">
                    <div class="sh-table-wrap">
                        <sh:estadoVazio ilustracao="caixa" titulo="Nenhuma nota importada ainda"
                            texto="Envie o XML de uma nota de compra acima para dar entrada nos produtos sem digitar um por um." />
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="sh-table-wrap">
                        <table class="sh-table sh-table--cards nfe-tabela" role="table">
                            <caption class="visually-hidden">Notas fiscais importadas, <c:out value="${totalNotas}" /> no total</caption>
                            <thead role="rowgroup">
                                <tr role="row">
                                    <th scope="col" role="columnheader">Número da nota</th>
                                    <th scope="col" role="columnheader">Fornecedor</th>
                                    <th scope="col" role="columnheader">Data da compra</th>
                                    <th scope="col" role="columnheader">Importada no sistema em</th>
                                    <th scope="col" role="columnheader" class="sh-col-num">Total da nota</th>
                                    <th scope="col" role="columnheader"><span class="visually-hidden">Ações</span></th>
                                </tr>
                            </thead>
                            <tbody role="rowgroup">
                                <c:forEach var="n" items="${notas}">
                                    <tr role="row">
                                        <td role="cell" data-label="Número da nota">
                                            <a class="nfe-nota-link" href="<c:out value='${ctx}/compras/nfe/${n.id}'/>">
                                                Nota <c:out value="${n.numero}" />
                                            </a>
                                        </td>
                                        <td role="cell" data-label="Fornecedor"><span class="nfe-fornecedor"><c:out value="${n.parceiro.nome}" /></span></td>
                                        <td role="cell" data-label="Data da compra"><sh:dataHora valor="${n.emitidaEm}" soData="true" /></td>
                                        <td role="cell" data-label="Importada no sistema em"><sh:dataHora valor="${n.importadaEm}" /></td>
                                        <td role="cell" class="sh-col-num sh-mono" data-label="Total da nota"><strong>R$ <fmt:formatNumber value="${n.valorTotal}" pattern="#,##0.00" /></strong></td>
                                        <td role="cell" class="nfe-col-acao">
                                            <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${ctx}/compras/nfe/${n.id}'/>">
                                                Ver itens<span class="visually-hidden"> da nota <c:out value="${n.numero}" /></span>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <sh:paginacao paginaAtual="${paginaAtual}" totalPaginas="${totalPaginas}" totalItens="${totalNotas}"
                        tamanho="20" base="/compras/nfe" rotulo="Paginação das notas importadas" />
                </c:otherwise>
            </c:choose>
        </section>
    </jsp:body>
</sh:layout>
