<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Entrar e criar conta (v3 "central de operações"). Rotas e campos inalterados:
      GET /login, GET /cadastro, POST /login (email, senha: tratado pelo Spring Security),
      POST /cadastro (nome, email, senha).
    Dados do model: erro, sucesso, modoCadastro, errosCampos (nome, email, senha, senhaDentroDoLimite), nome, email.
    A senha nunca volta preenchida (o input de senha não tem atributo value).
    Erro de login: uma mensagem única e genérica (não revela se o e-mail existe), em role="alert".
    Palco à esquerda (só a partir de 992 px): linhas de gráfico abstratas (nível de estoque em "dente de serra" e a linha
    do mínimo) que se desenham devagar; decoração pura (aria-hidden), parada com "menos movimento". js/entrar.js só
    avisa quando o Caps Lock está ligado no campo de senha.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Erros por campo (só existem no cadastro). A regra dos 72 bytes chega com o nome "senhaDentroDoLimite". --%>
<c:set var="eNome" value="${errosCampos['nome']}" />
<c:set var="eEmail" value="${errosCampos['email']}" />
<c:set var="eSenha" value="${not empty errosCampos['senha'] ? errosCampos['senha'] : errosCampos['senhaDentroDoLimite']}" />
<c:set var="eTipo" value="${errosCampos['tipoLojaValido']}" />
<c:set var="eRamo" value="${errosCampos['subtipoValido']}" />
<c:set var="temErrosCampos" value="${not empty eNome or not empty eEmail or not empty eSenha or not empty eTipo or not empty eRamo}" />
<c:set var="rotuloEnviar" value="${modoCadastro ? 'Criar conta' : 'Entrar'}" />

<sh:layoutPublico titulo="${modoCadastro ? 'Criar conta' : 'Entrar'}">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/entrar.css">
        <script defer src="${ctx}/js/entrar.js"></script>
        <script defer src="${ctx}/js/tipo-loja.js"></script>
    </jsp:attribute>
    <jsp:body>
    <div class="sh-auth">
        <%-- Palco: só em telas largas (no celular fica só o formulário) --%>
        <section class="sh-auth__palco" aria-label="Sobre o StockHub">
            <a class="sh-auth__marca" href="${ctx}/login">
                <sh:marca tamanho="32" />
                <span>StockHub</span>
            </a>
            <svg class="sh-auth__grafico" viewBox="0 0 640 360" aria-hidden="true" focusable="false" preserveAspectRatio="xMidYMid slice">
                <g class="sh-auth__grade">
                    <path d="M0 60 H640 M0 120 H640 M0 180 H640 M0 240 H640 M0 300 H640"/>
                    <path d="M80 0 V360 M160 0 V360 M240 0 V360 M320 0 V360 M400 0 V360 M480 0 V360 M560 0 V360"/>
                </g>
                <path class="sh-auth__minimo" d="M0 252 H640"/>
                <polyline class="sh-auth__linha sh-auth__linha--1" pathLength="1"
                    points="0,70 70,118 140,176 200,238 202,92 280,142 350,205 410,262 412,112 490,160 560,214 640,258"/>
                <polyline class="sh-auth__linha sh-auth__linha--2" pathLength="1"
                    points="0,214 90,196 170,226 250,184 330,206 410,162 490,178 570,136 640,122"/>
                <circle class="sh-auth__ponto" cx="640" cy="258" r="4.5"/>
                <circle class="sh-auth__ponto sh-auth__ponto--2" cx="640" cy="122" r="4.5"/>
            </svg>
            <div class="sh-auth__texto">
                <p class="sh-auth__eyebrow">Controle de estoque</p>
                <p class="sh-auth__frase">Saldos, movimentações e alertas em um só lugar.</p>
                <ul class="sh-auth__fatos">
                    <li><i class="bi bi-activity" aria-hidden="true"></i> Saldo atualizado a cada movimentação.</li>
                    <li><i class="bi bi-bell" aria-hidden="true"></i> Alertas quando o estoque fica abaixo do mínimo.</li>
                    <li><i class="bi bi-clock-history" aria-hidden="true"></i> Histórico completo: o que mudou e quando.</li>
                </ul>
            </div>
        </section>

        <div class="sh-auth__lado">
            <div class="sh-auth__tema">
                <jsp:include page="/WEB-INF/includes/sh/tema-botao.jsp" />
            </div>

            <div class="sh-auth__caixa">
                <div class="sh-auth__logo-mobile">
                    <sh:marca tamanho="32" />
                    <span>StockHub</span>
                </div>

                <div>
                    <h1 class="sh-auth__titulo"><c:out value="${modoCadastro ? 'Criar conta' : 'Entrar'}" /></h1>
                    <p class="sh-auth__sub"><c:choose>
                        <c:when test="${modoCadastro}">Cadastre sua loja para controlar o estoque.</c:when>
                        <c:otherwise>Acesse o controle de estoque da sua loja.</c:otherwise>
                    </c:choose></p>
                </div>

                <c:if test="${not empty sucesso}">
                    <div class="sh-alert sh-alert--success" role="status">
                        <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                        <div class="sh-alert__corpo"><c:out value="${sucesso}" /></div>
                    </div>
                </c:if>
                <c:if test="${not empty erro and not temErrosCampos}">
                    <div class="sh-alert sh-alert--danger" role="alert" tabindex="-1">
                        <i class="bi bi-exclamation-octagon-fill" aria-hidden="true"></i>
                        <div class="sh-alert__corpo"><c:out value="${erro}" /></div>
                    </div>
                </c:if>

                <form action="${ctx}${modoCadastro ? '/cadastro' : '/login'}" method="post" data-sh-validar
                    data-sh-enviando="${modoCadastro ? 'Criando…' : 'Entrando…'}">
                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>" />

                    <%-- Resumo de erros: o servidor preenche quando há erro por campo; o JS preenche ao enviar --%>
                    <div class="sh-resumo-erros" role="alert" tabindex="-1" data-sh-resumo-erros ${temErrosCampos ? '' : 'hidden'}>
                        <p class="sh-resumo-erros__titulo"><c:if test="${temErrosCampos}">Corrija os campos abaixo para continuar:</c:if></p>
                        <ul>
                            <c:if test="${not empty eNome}">
                                <li><a href="#nome" data-sh-ir-campo="nome"><c:out value="${eNome}" /></a></li>
                            </c:if>
                            <c:if test="${not empty eEmail}">
                                <li><a href="#email" data-sh-ir-campo="email"><c:out value="${eEmail}" /></a></li>
                            </c:if>
                            <c:if test="${not empty eSenha}">
                                <li><a href="#senha" data-sh-ir-campo="senha"><c:out value="${eSenha}" /></a></li>
                            </c:if>
                            <c:if test="${not empty eTipo}"><li><c:out value="${eTipo}" /></li></c:if>
                            <c:if test="${not empty eRamo}"><li><a href="#subtipo"><c:out value="${eRamo}" /></a></li></c:if>
                        </ul>
                    </div>

                    <c:if test="${modoCadastro}">
                        <div class="mb-3">
                            <label class="form-label" for="nome">Nome da loja <span class="sh-obrigatorio">(obrigatório)</span></label>
                            <input id="nome" type="text" class="form-control ${not empty eNome ? 'is-invalid' : ''}" name="nome"
                                value="<c:out value='${nome}'/>" autocomplete="organization" maxlength="255" required
                                ${not empty eNome ? 'aria-invalid="true" aria-describedby="erro-nome"' : ''}>
                            <c:if test="${not empty eNome}">
                                <div id="erro-nome" class="sh-erro-campo">
                                    <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                    <span class="sh-erro-campo__texto"><c:out value="${eNome}" /></span>
                                </div>
                            </c:if>
                        </div>
                    </c:if>

                    <div class="mb-3">
                        <label class="form-label" for="email">E-mail <span class="sh-obrigatorio">(obrigatório)</span></label>
                        <input id="email" type="email" class="form-control ${not empty eEmail ? 'is-invalid' : ''}" name="email"
                            value="<c:out value='${email}'/>" placeholder="voce@empresa.com" autocomplete="username"
                            inputmode="email" autocapitalize="none" spellcheck="false" required
                            ${not empty eEmail ? 'aria-invalid="true" aria-describedby="erro-email"' : ''}>
                        <c:if test="${not empty eEmail}">
                            <div id="erro-email" class="sh-erro-campo">
                                <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                <span class="sh-erro-campo__texto"><c:out value="${eEmail}" /></span>
                            </div>
                        </c:if>
                    </div>
                    <c:if test="${modoCadastro}">
                        <%-- Tipo e ramo da loja (RN-43/44): cartões de rádio; sem escolha vale "Só controle de estoque" --%>
                        <c:set var="tipoSel" value="${empty tipoEscolhido ? 'ESTOQUE' : tipoEscolhido}" />
                        <fieldset class="sh-tipos mb-3" data-sh-tipos>
                            <legend class="form-label">Como é a sua loja?</legend>
                            <div class="sh-tipos__lista">
                                <label class="sh-tipo">
                                    <input type="radio" name="tipoLoja" value="ESTOQUE" ${tipoSel == 'ESTOQUE' ? 'checked' : ''}>
                                    <span class="sh-tipo__corpo">
                                        <i class="bi bi-box-seam" aria-hidden="true"></i>
                                        <span class="sh-tipo__texto"><strong>Só controle de estoque</strong>
                                            <small>Produtos, entradas e saídas, notas de compra e alertas.</small></span>
                                    </span>
                                </label>
                                <label class="sh-tipo">
                                    <input type="radio" name="tipoLoja" value="COMERCIO" ${tipoSel == 'COMERCIO' ? 'checked' : ''}>
                                    <span class="sh-tipo__corpo">
                                        <i class="bi bi-shop" aria-hidden="true"></i>
                                        <span class="sh-tipo__texto"><strong>Comércio</strong>
                                            <small>Vende produtos. Estoque e, em breve, vendas e clientes.</small></span>
                                    </span>
                                </label>
                                <label class="sh-tipo">
                                    <input type="radio" name="tipoLoja" value="OFICINA" ${tipoSel == 'OFICINA' ? 'checked' : ''}>
                                    <span class="sh-tipo__corpo">
                                        <i class="bi bi-tools" aria-hidden="true"></i>
                                        <span class="sh-tipo__texto"><strong>Oficina e serviços</strong>
                                            <small>Conserta e presta serviço. Estoque de peças e, em breve, ordens de serviço.</small></span>
                                    </span>
                                </label>
                                <label class="sh-tipo">
                                    <input type="radio" name="tipoLoja" value="MISTO" ${tipoSel == 'MISTO' ? 'checked' : ''}>
                                    <span class="sh-tipo__corpo">
                                        <i class="bi bi-grid" aria-hidden="true"></i>
                                        <span class="sh-tipo__texto"><strong>Comércio e serviços</strong>
                                            <small>Vende e também presta serviço. Tudo ligado.</small></span>
                                    </span>
                                </label>
                            </div>
                            <div class="sh-tipos__ramo mt-3" data-sh-ramo>
                                <label class="form-label" for="subtipo">Ramo <span class="sh-opcional">(opcional)</span></label>
                                <select id="subtipo" name="subtipo" data-selecionado="<c:out value='${subtipoEscolhido}'/>" class="form-select ${not empty eRamo ? 'is-invalid' : ''}">
                                    <option value="">Prefiro não informar</option>
                                    <optgroup label="Estoque" data-tipos="ESTOQUE">
                                        <option value="ALMOXARIFADO">Almoxarifado</option>
                                        <option value="DEPOSITO">Depósito</option>
                                        <option value="ESTOQUE_ESCRITORIO">Estoque de escritório</option>
                                    </optgroup>
                                    <optgroup label="Comércio" data-tipos="COMERCIO MISTO">
                                        <option value="MERCADO">Mercado</option>
                                        <option value="PAPELARIA">Papelaria</option>
                                        <option value="MATERIAL_CONSTRUCAO">Material de construção</option>
                                        <option value="FARMACIA">Farmácia</option>
                                        <option value="VESTUARIO">Vestuário</option>
                                        <option value="INFORMATICA">Informática</option>
                                        <option value="OUTRO_COMERCIO">Outro comércio</option>
                                    </optgroup>
                                    <optgroup label="Serviços" data-tipos="OFICINA MISTO">
                                        <option value="MECANICA">Mecânica</option>
                                        <option value="MOTO">Moto</option>
                                        <option value="ELETRICA_ELETRONICA">Elétrica e eletrônica</option>
                                        <option value="ASSISTENCIA_TECNICA">Assistência técnica</option>
                                        <option value="REFRIGERACAO">Refrigeração</option>
                                        <option value="OUTRO_SERVICO">Outro serviço</option>
                                    </optgroup>
                                </select>
                                <c:if test="${not empty eRamo}">
                                    <div class="sh-erro-campo"><i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                        <span class="sh-erro-campo__texto"><c:out value="${eRamo}" /></span></div>
                                </c:if>
                            </div>
                        </fieldset>
                    </c:if>


                    <div class="mb-3">
                        <label class="form-label" for="senha">Senha <span class="sh-obrigatorio">(obrigatório)</span></label>
                        <div class="sh-senha">
                            <c:choose>
                                <c:when test="${modoCadastro}">
                                    <%-- A senha nunca volta preenchida depois de um erro (sem atributo value) --%>
                                    <input id="senha" type="password" class="form-control ${not empty eSenha ? 'is-invalid' : ''}" name="senha"
                                        minlength="8" autocomplete="new-password"
                                        aria-describedby="regras-senha${not empty eSenha ? ' erro-senha' : ''}"
                                        ${not empty eSenha ? 'aria-invalid="true"' : ''} required>
                                </c:when>
                                <c:otherwise>
                                    <input id="senha" type="password" class="form-control" name="senha"
                                        autocomplete="current-password" required>
                                </c:otherwise>
                            </c:choose>
                            <button type="button" class="sh-senha__alternar" data-sh-senha-alternar aria-controls="senha"
                                aria-pressed="false" aria-label="Mostrar senha">
                                <i class="bi bi-eye" aria-hidden="true"></i>
                            </button>
                        </div>
                        <%-- Aviso de Caps Lock (js/entrar.js escreve aqui; a região é anunciada ao mudar) --%>
                        <div class="sh-auth__caps" data-sh-caps aria-live="polite"></div>
                        <c:if test="${not empty eSenha}">
                            <div id="erro-senha" class="sh-erro-campo">
                                <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
                                <span class="sh-erro-campo__texto"><c:out value="${eSenha}" /></span>
                            </div>
                        </c:if>
                        <c:if test="${modoCadastro}">
                            <ul class="sh-regras" id="regras-senha" data-sh-regras-senha data-campo="senha">
                                <li data-regra="minimo">
                                    <i class="bi bi-circle sh-regras__pendente" aria-hidden="true"></i>
                                    <i class="bi bi-check-circle-fill sh-regras__ok" aria-hidden="true"></i>
                                    <span>Pelo menos 8 caracteres</span>
                                    <span class="visually-hidden sh-regras__estado">(pendente)</span>
                                </li>
                                <li data-regra="maximo">
                                    <i class="bi bi-circle sh-regras__pendente" aria-hidden="true"></i>
                                    <i class="bi bi-check-circle-fill sh-regras__ok" aria-hidden="true"></i>
                                    <span>No máximo 72 bytes (acentos e símbolos contam mais de um)</span>
                                    <span class="visually-hidden sh-regras__estado">(pendente)</span>
                                </li>
                            </ul>
                        </c:if>
                    </div>

                    <button type="submit" class="btn btn-primary btn-lg w-100 sh-auth__enviar">
                        <span><c:out value="${rotuloEnviar}" /></span>
                    </button>
                </form>

                <p class="sh-auth__troca">
                    <c:choose>
                        <c:when test="${modoCadastro}">
                            Já tem conta? <a href="${ctx}/login">Entrar</a>
                        </c:when>
                        <c:otherwise>
                            Ainda não tem conta? <a href="${ctx}/cadastro">Criar conta</a>
                        </c:otherwise>
                    </c:choose>
                </p>
            </div>
        </div>
    </div>
    </jsp:body>
</sh:layoutPublico>
