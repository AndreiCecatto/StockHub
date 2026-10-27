<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Cadastro e edição de cliente (RN-91 a RN-96). Rotas: GET /clientes/novo, POST /clientes,
    GET /clientes/{id}/editar, POST /clientes/{id}.
    Dados do ClienteController: form (ClienteForm: o que foi digitado ou o cadastro atual), editando, cliente (só na
    edição), documentoTravado (fornecedor com CPF/CNPJ: o documento não muda aqui). Flash: erro (mostrado pelo layout).
    Só o nome é obrigatório. js/clientes.js põe as máscaras de CPF/CNPJ, telefone e CEP e confere os dígitos do documento.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="sufixo" value="${editando ? '/' : ''}${editando ? cliente.id : ''}" />

<sh:layout titulo="${editando ? 'Editar cliente' : 'Novo cliente'}" ativo="clientes" largura="form" formulario="true"
    voltarUrl="/clientes${sufixo}" voltarRotulo="${editando ? 'Cliente' : 'Clientes'}">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/membros.css">
        <link rel="stylesheet" href="${ctx}/css/clientes.css">
        <script defer src="${ctx}/js/clientes.js"></script>
    </jsp:attribute>
    <jsp:body>
        <sh:cabecalhoPagina titulo="${editando ? 'Editar cliente' : 'Novo cliente'}"
            descricao="${editando ? 'Atualize os dados de contato e de endereço.' : 'Só o nome é obrigatório. O resto você pode completar depois.'}" />

        <form method="post" action="<c:out value='${ctx}/clientes${sufixo}'/>" data-sh-validar data-sh-enviando="Salvando…" data-cliente-form>
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

            <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros hidden>
                <p class="sh-resumo-erros__titulo"></p>
                <ul></ul>
            </div>

            <section class="sh-card sh-secao" aria-labelledby="cli-quem">
                <div class="sh-card__cab"><h2 class="sh-card__titulo" id="cli-quem">Quem é o cliente</h2></div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12">
                            <span class="form-label d-block" id="rot-tipo">Tipo de pessoa <span class="sh-opcional">(opcional)</span></span>
                            <div class="sh-seg" role="radiogroup" aria-labelledby="rot-tipo">
                                <input type="radio" id="tipo-nenhum" name="tipoPessoa" value="" ${empty form.tipoPessoa ? 'checked' : ''}>
                                <label for="tipo-nenhum">Não informar</label>
                                <input type="radio" id="tipo-fisica" name="tipoPessoa" value="FISICA" ${form.tipoPessoa == 'FISICA' ? 'checked' : ''}>
                                <label for="tipo-fisica">Pessoa física</label>
                                <input type="radio" id="tipo-juridica" name="tipoPessoa" value="JURIDICA" ${form.tipoPessoa == 'JURIDICA' ? 'checked' : ''}>
                                <label for="tipo-juridica">Empresa</label>
                            </div>
                        </div>
                        <div class="col-12 col-md-8">
                            <label class="form-label" for="nome">Nome <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <input id="nome" class="form-control" type="text" name="nome" maxlength="255" required autocomplete="off"
                                value="<c:out value='${form.nome}'/>" placeholder="Ex.: Maria Oliveira ou Mercado Boa Compra">
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="documento">CPF ou CNPJ <span class="sh-opcional">(opcional)</span></label>
                            <input id="documento" class="form-control" type="text" name="documento" maxlength="18" inputmode="numeric"
                                autocomplete="off" value="<c:out value='${form.documento}'/>" data-mascara="documento"
                                ${documentoTravado ? 'readonly' : ''} aria-describedby="aj-documento">
                            <div class="form-text" id="aj-documento">
                                <c:choose>
                                    <c:when test="${documentoTravado}">Este cadastro também é fornecedor, por isso o documento não muda aqui.</c:when>
                                    <c:otherwise>Digite só os números. Se preencher, conferimos se está certo.</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <div class="col-12 col-md-8">
                            <label class="form-label" for="nomeFantasia">Nome fantasia <span class="sh-opcional">(opcional, para empresas)</span></label>
                            <input id="nomeFantasia" class="form-control" type="text" name="nomeFantasia" maxlength="255" autocomplete="off"
                                value="<c:out value='${form.nomeFantasia}'/>">
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="dataNascimento">Nascimento ou fundação <span class="sh-opcional">(opcional)</span></label>
                            <input id="dataNascimento" class="form-control" type="date" name="dataNascimento" min="1900-01-01"
                                value="<c:out value='${form.dataNascimento}'/>">
                        </div>
                    </div>
                </div>
            </section>

            <section class="sh-card sh-secao" aria-labelledby="cli-contato">
                <div class="sh-card__cab"><h2 class="sh-card__titulo" id="cli-contato">Como falar com ele</h2></div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="telefone">Telefone <span class="sh-opcional">(opcional)</span></label>
                            <input id="telefone" class="form-control" type="tel" name="telefone" maxlength="16" inputmode="tel" autocomplete="off"
                                value="<c:out value='${form.telefone}'/>" data-mascara="telefone" placeholder="(55) 3000-0000"
                                aria-describedby="aj-telefone">
                            <div class="form-text" id="aj-telefone">Com o DDD.</div>
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="whatsapp">WhatsApp <span class="sh-opcional">(opcional)</span></label>
                            <input id="whatsapp" class="form-control" type="tel" name="whatsapp" maxlength="16" inputmode="tel" autocomplete="off"
                                value="<c:out value='${form.whatsapp}'/>" data-mascara="telefone" placeholder="(55) 99999-0000">
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="email">E-mail <span class="sh-opcional">(opcional)</span></label>
                            <input id="email" class="form-control" type="email" name="email" maxlength="255" autocomplete="off"
                                value="<c:out value='${form.email}'/>" placeholder="nome@exemplo.com">
                        </div>
                    </div>
                </div>
            </section>

            <section class="sh-card sh-secao" aria-labelledby="cli-endereco">
                <div class="sh-card__cab"><h2 class="sh-card__titulo" id="cli-endereco">Onde mora ou fica</h2></div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12 col-md-3">
                            <label class="form-label" for="cep">CEP <span class="sh-opcional">(opcional)</span></label>
                            <input id="cep" class="form-control" type="text" name="cep" maxlength="9" inputmode="numeric" autocomplete="off"
                                value="<c:out value='${form.cep}'/>" data-mascara="cep" placeholder="00000-000">
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="logradouro">Rua ou avenida</label>
                            <input id="logradouro" class="form-control" type="text" name="logradouro" maxlength="255" autocomplete="off"
                                value="<c:out value='${form.logradouro}'/>">
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label" for="numero">Número</label>
                            <input id="numero" class="form-control" type="text" name="numero" maxlength="20" autocomplete="off"
                                value="<c:out value='${form.numero}'/>">
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="complemento">Complemento</label>
                            <input id="complemento" class="form-control" type="text" name="complemento" maxlength="100" autocomplete="off"
                                value="<c:out value='${form.complemento}'/>" placeholder="Apto, sala, bloco">
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label" for="bairro">Bairro</label>
                            <input id="bairro" class="form-control" type="text" name="bairro" maxlength="100" autocomplete="off"
                                value="<c:out value='${form.bairro}'/>">
                        </div>
                        <div class="col-8 col-md-3">
                            <label class="form-label" for="cidade">Cidade</label>
                            <input id="cidade" class="form-control" type="text" name="cidade" maxlength="100" autocomplete="off"
                                value="<c:out value='${form.cidade}'/>">
                        </div>
                        <div class="col-4 col-md-1">
                            <label class="form-label" for="uf">UF</label>
                            <select id="uf" class="form-select" name="uf">
                                <option value="">--</option>
                                <c:forTokens var="sigla" delims="," items="AC,AL,AP,AM,BA,CE,DF,ES,GO,MA,MT,MS,MG,PA,PB,PR,PE,PI,RJ,RN,RS,RO,RR,SC,SP,SE,TO">
                                    <option value="${sigla}" ${fn:toUpperCase(form.uf) == sigla ? 'selected' : ''}>${sigla}</option>
                                </c:forTokens>
                            </select>
                        </div>
                    </div>
                </div>
            </section>

            <section class="sh-card sh-secao" aria-labelledby="cli-outros">
                <div class="sh-card__cab"><h2 class="sh-card__titulo" id="cli-outros">Outras informações</h2></div>
                <div class="sh-card__corpo">
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="origem">Como chegou até você <span class="sh-opcional">(opcional)</span></label>
                            <input id="origem" class="form-control" type="text" name="origem" maxlength="120" autocomplete="off"
                                value="<c:out value='${form.origem}'/>" placeholder="Ex.: indicação do João, Instagram, passou na porta">
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="observacoes">Observações internas <span class="sh-opcional">(opcional)</span></label>
                            <textarea id="observacoes" class="form-control" name="observacoes" rows="3" maxlength="1000"
                                aria-describedby="aj-obs"><c:out value="${form.observacoes}" /></textarea>
                            <div class="form-text" id="aj-obs">Só a sua equipe vê. Não aparece na ordem de serviço impressa.</div>
                        </div>
                    </div>
                    <div class="sh-form-actions mb-acoes-form">
                        <a class="btn btn-outline-secondary" href="<c:out value='${ctx}/clientes${sufixo}'/>">Cancelar</a>
                        <button class="btn btn-primary" type="submit">
                            <i class="bi bi-check-lg" aria-hidden="true"></i> <c:out value="${editando ? 'Salvar alterações' : 'Cadastrar cliente'}" />
                        </button>
                    </div>
                </div>
            </section>
        </form>
    </jsp:body>
</sh:layout>
