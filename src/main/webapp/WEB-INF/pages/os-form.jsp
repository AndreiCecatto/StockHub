<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Nova ordem de serviço (RN-78, RN-79). Rotas: GET /os/nova e POST /os.
    Dados do OrdemServicoController: clientes (Parceiro), responsaveis (MembroLoja); flash: erro, clienteDigitado,
    equipamentoDigitado, placaDigitada, modeloDigitado, identificadorDigitado, defeitoDigitado, garantiaDigitada, observacoesDigitadas.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layout titulo="Nova ordem de serviço" ativo="os" largura="form" formulario="true" voltarUrl="/os" voltarRotulo="Ordens de serviço">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/os.css">
        <script defer src="${ctx}/js/os.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="Nova ordem de serviço" descricao="Comece pelo cliente e pelo defeito. As peças e os serviços entram depois, no orçamento." />

        <section class="sh-card sh-secao sh-secao-form" aria-labelledby="titulo-nova-os">
            <div class="sh-card__cab">
                <h2 class="sh-card__titulo" id="titulo-nova-os">Dados do atendimento</h2>
            </div>
            <div class="sh-card__corpo">
                <c:choose>
                    <c:when test="${empty clientes}">
                        <p class="mb-0">Você ainda não tem clientes. <a href="${ctx}/clientes">Cadastre o primeiro cliente</a> e volte aqui.</p>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/os" data-sh-enviando="Criando…">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />
                            <div class="row g-3">
                                <div class="col-12 col-md-6">
                                    <label class="form-label" for="clienteId">Cliente</label>
                                    <select id="clienteId" class="form-select" name="clienteId" required>
                                        <option value="">Escolha o cliente</option>
                                        <c:forEach var="cli" items="${clientes}">
                                            <option value="${cli.id}" ${clienteDigitado == cli.id ? 'selected' : ''}><c:out value="${cli.nome}" /></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-12 col-md-6">
                                    <label class="form-label" for="responsavelId">Responsável</label>
                                    <select id="responsavelId" class="form-select" name="responsavelId">
                                        <c:forEach var="m" items="${responsaveis}">
                                            <option value="${m.id}" ${m.id == usuarioLogado.membroId ? 'selected' : ''}><c:out value="${m.nome}" /> (<c:out value="${m.papel.rotulo}" />)</option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <c:choose>
                                    <c:when test="${camposRamo.temVeiculo}">
                                        <div class="col-12 col-md-4">
                                            <label class="form-label" for="placa">Placa <span class="sh-opcional">(opcional)</span></label>
                                            <input id="placa" class="form-control text-uppercase" type="text" name="placa" maxlength="8"
                                                pattern="[A-Za-z]{3}-?[0-9]{4}|[A-Za-z]{3}[0-9][A-Za-z][0-9]{2}" title="Formato ABC-1234 ou ABC1D23"
                                                autocomplete="off" autocapitalize="characters" spellcheck="false" aria-describedby="placa-dica"
                                                data-os-placa value="<c:out value='${placaDigitada}'/>">
                                            <div class="form-text" id="placa-dica">Formato ABC-1234 ou ABC1D23 (Mercosul).</div>
                                        </div>
                                        <div class="col-12 col-md-8">
                                            <label class="form-label" for="modelo">Modelo <span class="sh-opcional">(opcional)</span></label>
                                            <input id="modelo" class="form-control" type="text" name="modelo" maxlength="80"
                                                autocomplete="off" value="<c:out value='${modeloDigitado}'/>" placeholder="Ex.: Fiat Uno 2012 ou Honda CG 160">
                                        </div>
                                        <div class="col-12 col-md-8">
                                            <label class="form-label" for="equipamento">Descrição do veículo <span class="sh-opcional">(opcional)</span></label>
                                            <input id="equipamento" class="form-control" type="text" name="equipamento" maxlength="255"
                                                autocomplete="off" aria-describedby="equipamento-dica" value="<c:out value='${equipamentoDigitado}'/>">
                                            <div class="form-text" id="equipamento-dica">Se ficar em branco, usamos o modelo como descrição.</div>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="col-12 col-md-8">
                                            <label class="form-label" for="equipamento">Equipamento ou veículo</label>
                                            <input id="equipamento" class="form-control" type="text" name="equipamento" maxlength="255" required
                                                autocomplete="off" value="<c:out value='${equipamentoDigitado}'/>" placeholder="Ex.: Notebook Dell Inspiron ou Fiat Uno">
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                                <div class="col-12 col-md-4">
                                    <c:choose>
                                        <c:when test="${camposRamo.temVeiculo}">
                                            <label class="form-label" for="identificador">Chassi ou nº de série <span class="sh-opcional">(opcional)</span></label>
                                            <input id="identificador" class="form-control" type="text" name="identificador" maxlength="60"
                                                autocomplete="off" aria-describedby="identificador-dica" value="<c:out value='${identificadorDigitado}'/>">
                                            <div class="form-text" id="identificador-dica">Chassi do veículo ou número de série de uma peça.</div>
                                        </c:when>
                                        <c:otherwise>
                                            <label class="form-label" for="identificador">Placa ou série <span class="sh-opcional">(opcional)</span></label>
                                            <input id="identificador" class="form-control" type="text" name="identificador" maxlength="60"
                                                autocomplete="off" value="<c:out value='${identificadorDigitado}'/>">
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="defeito">Defeito relatado</label>
                                    <textarea id="defeito" class="form-control" name="defeito" rows="3" maxlength="1000" required
                                        placeholder="O que o cliente disse que está acontecendo"><c:out value="${defeitoDigitado}" /></textarea>
                                </div>
                                <div class="col-12 col-md-4">
                                    <label class="form-label" for="garantiaDias">Garantia (dias)</label>
                                    <input id="garantiaDias" class="form-control" type="number" name="garantiaDias" min="0" max="3650"
                                        value="<c:out value='${empty garantiaDigitada ? 0 : garantiaDigitada}'/>">
                                </div>
                                <div class="col-12 col-md-8">
                                    <label class="form-label" for="observacoes">Observações <span class="sh-opcional">(opcional)</span></label>
                                    <input id="observacoes" class="form-control" type="text" name="observacoes" maxlength="1000"
                                        autocomplete="off" value="<c:out value='${observacoesDigitadas}'/>">
                                </div>
                            </div>
                            <div class="sh-form-actions mb-acoes-form">
                                <a class="btn btn-outline-secondary" href="${ctx}/os">Cancelar</a>
                                <button class="btn btn-primary" type="submit">Criar ordem de serviço</button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </jsp:body>
</sh:layout>
