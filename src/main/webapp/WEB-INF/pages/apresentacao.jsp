<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>
<%--
    Apresentação pública do StockHub (GET "/", sem nada no model; quem já está logado vai para /dashboard).
    Narrativa de rolagem com o kit de rolagem (docs/design-system.md, seção 12.5):
      hero | 01 estoque (cena fixa, escura) | 02 movimentações (cena fixa) | 03 alertas (cena fixa, escura)
           | 04 atalhos (cena fixa) | 05 confiança (escura) | chamada final e rodapé.
    TUDO o que aparece como produto, saldo, movimentação ou número é MAQUETE ILUSTRATIVA com dados fictícios (marcada na tela
    como "exemplo"). Não há cliente, depoimento nem métrica de mercado. As afirmações de "confiável" são verificáveis no código
    (docs/regras-negocio.md: RN-01, RN-02, RN-06, RN-14; SecurityConfig: BCrypt e CSRF; exportação CSV; tema; teclado).
    As maquetes são decorativas (aria-hidden): a informação equivalente está escrita em texto ao lado.
    Seções escuras usam data-bs-theme="dark" (os tokens do tema escuro valem só dentro da seção, nos dois temas).
    Sem JavaScript: nada fica escondido (o estado final é o padrão). Menos movimento e celular: as cenas viram blocos comuns
    no estado final. Sem script nem style inline. Recursos só por caminhos públicos (/css, /js, /vendor, /img).
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<sh:layoutPublico titulo="Controle de estoque" classe="ap-pagina">
    <jsp:attribute name="scripts">
        <link rel="stylesheet" href="${ctx}/css/apresentacao.css">
        <script defer src="${ctx}/js/apresentacao.js"></script>
    </jsp:attribute>
    <jsp:body>
    <div class="sh-progresso-pagina" aria-hidden="true"></div>

    <%-- ===================== Barra superior pública ===================== --%>
    <header class="ap-barra" data-ap-barra>
        <div class="ap-barra__in">
            <a class="ap-barra__marca" href="${ctx}/" aria-label="StockHub, início da apresentação">
                <sh:marca tamanho="28" />
                <span>StockHub</span>
            </a>
            <nav class="ap-barra__nav" aria-label="Seções desta página">
                <ul>
                    <li><a href="#estoque">Estoque</a></li>
                    <li><a href="#movimentacoes">Movimentações</a></li>
                    <li><a href="#alertas">Alertas</a></li>
                    <li><a href="#atalhos">Atalhos</a></li>
                    <li><a href="#confianca">Confiança</a></li>
                </ul>
            </nav>
            <div class="ap-barra__acoes">
                <div class="ap-barra__tema"><jsp:include page="/WEB-INF/includes/sh/tema-botao.jsp" /></div>
                <a class="btn btn-ghost" href="${ctx}/login">Entrar</a>
                <a class="btn btn-primary" href="${ctx}/cadastro">Criar conta</a>
            </div>
        </div>
    </header>

    <%-- ===================== Hero ===================== --%>
    <section class="ap-secao ap-hero" id="inicio" aria-labelledby="ap-titulo">
        <div class="ap-hero__texto">
            <p class="sh-eyebrow ap-entra ap-entra--1">Controle de estoque para lojas</p>
            <h1 class="sh-hero-texto ap-hero__titulo" id="ap-titulo">
                <span class="ap-entra ap-entra--2">Saiba o que tem,</span>
                <span class="ap-entra ap-entra--3">o que falta</span>
                <span class="ap-entra ap-entra--4 ap-acento">e o que saiu.</span>
            </h1>
            <p class="sh-lead ap-hero__sub ap-entra ap-entra--5">
                O StockHub mostra o saldo de cada produto, avisa antes de acabar e guarda o histórico de cada movimentação.
            </p>
            <div class="ap-hero__ctas ap-entra ap-entra--6">
                <a class="btn btn-primary btn-lg" href="${ctx}/cadastro">Criar conta</a>
                <a class="btn btn-outline-secondary btn-lg" href="${ctx}/login">Entrar</a>
            </div>
        </div>

        <figure class="ap-hero__figura ap-entra ap-entra--7">
            <div class="ap-painel" aria-hidden="true">
                <div class="ap-painel__topo">
                    <span class="ap-painel__marca"><sh:marca tamanho="20" /> StockHub</span>
                    <span class="ap-painel__busca"><i class="bi bi-search"></i> Buscar ou executar um comando&hellip; <kbd data-ap-tecla>Ctrl K</kbd></span>
                </div>
                <div class="ap-painel__corpo">
                    <p class="ap-painel__titulo">Visão geral</p>
                    <div class="ap-kpis">
                        <div class="ap-kpi">
                            <span class="ap-kpi__valor sh-mono" data-contar="48920" data-formato="moeda" data-contar-duracao="1400">R$ 48.920,00</span>
                            <span class="ap-kpi__rotulo">em estoque</span>
                        </div>
                        <div class="ap-kpi">
                            <span class="ap-kpi__valor sh-mono" data-contar="1284" data-formato="inteiro" data-contar-duracao="1400">1.284</span>
                            <span class="ap-kpi__rotulo">unidades</span>
                        </div>
                        <div class="ap-kpi">
                            <span class="ap-kpi__valor sh-mono" data-contar="86" data-formato="inteiro" data-contar-duracao="1400">86</span>
                            <span class="ap-kpi__rotulo">produtos</span>
                        </div>
                        <div class="ap-kpi ap-kpi--alerta">
                            <span class="ap-kpi__valor sh-mono" data-contar="7" data-formato="inteiro" data-contar-duracao="1400">7</span>
                            <span class="ap-kpi__rotulo">a repor</span>
                        </div>
                    </div>
                    <div class="ap-painel__grade">
                        <div class="ap-bloco">
                            <p class="ap-bloco__titulo">Requer ação</p>
                            <ul class="ap-mini">
                                <li><span class="ap-mini__nome">Sabão em pó 1 kg</span><sh:badgeEstoque nivel="SEM_ESTOQUE" /><sh:medidorEstoque saldo="0" minimo="15" /></li>
                                <li><span class="ap-mini__nome">Café torrado 500 g</span><sh:badgeEstoque nivel="BAIXO" /><sh:medidorEstoque saldo="18" minimo="20" /></li>
                                <li><span class="ap-mini__nome">Detergente neutro 500 ml</span><sh:badgeEstoque nivel="BAIXO" /><sh:medidorEstoque saldo="28" minimo="30" /></li>
                            </ul>
                        </div>
                        <div class="ap-bloco">
                            <p class="ap-bloco__titulo">Movimento, 14 dias</p>
                            <sh:miniBarras valores="4,9,6,14,11,3,8,16,12,7,10,18,9,13" largura="260" altura="64" destaque="ultimo" classe="ap-grafico-mini" rotulo="Movimento de exemplo" />
                        </div>
                    </div>
                </div>
            </div>
            <figcaption class="ap-fig-legenda">Maquete ilustrativa, com dados fictícios.</figcaption>
        </figure>
    </section>

    <%-- ===================== 01 Estoque (cena fixa, escura) ===================== --%>
    <section class="ap-secao ap-escuro sh-cena ap-cena ap-cena--estoque" id="estoque" data-bs-theme="dark" aria-labelledby="ap-t-estoque">
        <div class="sh-cena__palco ap-palco">
            <div class="ap-palco__grade">
                <div class="ap-texto sh-revelar-grupo">
                    <p class="sh-eyebrow">01 &middot; Estoque</p>
                    <h2 class="sh-display" id="ap-t-estoque">O saldo certo, a qualquer hora.</h2>
                    <p class="sh-lead">
                        Cada produto mostra quanto tem, quanto precisa ter e em que situação está. O selo muda sozinho quando o saldo muda.
                    </p>
                    <ul class="ap-legenda">
                        <li><sh:badgeEstoque nivel="NORMAL" /><span>O saldo está acima do mínimo.</span></li>
                        <li><sh:badgeEstoque nivel="BAIXO" /><span>O saldo chegou ao mínimo ou ficou abaixo.</span></li>
                        <li><sh:badgeEstoque nivel="SEM_ESTOQUE" /><span>O saldo chegou a zero.</span></li>
                    </ul>
                </div>

                <div class="ap-demo" aria-hidden="true">
                    <div class="ap-cartao">
                        <div class="ap-cartao__cab">
                            <span class="ap-cartao__titulo">Produtos <em>(exemplo)</em></span>
                            <span class="ap-contador">
                                <span class="ap-pilha ap-pilha--num">
                                    <span class="ap-n sh-cena-fade ap-c0">0</span>
                                    <span class="ap-n sh-cena-fade ap-c1">1</span>
                                    <span class="ap-n sh-cena-fade ap-c2">2</span>
                                    <span class="ap-n sh-cena-fade ap-c3">3</span>
                                </span>
                                <span class="ap-contador__rotulo">pedem atenção</span>
                            </span>
                        </div>

                        <div class="ap-prods">
                            <div class="ap-prod ap-prod--det">
                                <div class="ap-prod__nome"><strong>Detergente neutro 500 ml</strong><span>Limpeza &middot; mínimo 30</span></div>
                                <span class="ap-pilha ap-prod__saldo sh-mono">
                                    <span class="sh-cena-fade ap-ini">62</span><span class="sh-cena-fade ap-fim">28</span>
                                </span>
                                <span class="sh-meter ap-med">
                                    <span class="ap-med__f ap-med__f--n sh-cena-fade ap-ini"></span><span class="ap-med__f ap-med__f--b sh-cena-fade ap-fim"></span>
                                    <span class="sh-meter__minimo"></span>
                                </span>
                                <span class="ap-pilha ap-prod__selo">
                                    <span class="sh-cena-fade ap-ini"><sh:badgeEstoque nivel="NORMAL" /></span><span class="sh-cena-fade ap-fim"><sh:badgeEstoque nivel="BAIXO" /></span>
                                </span>
                            </div>
                            <div class="ap-prod ap-prod--sab">
                                <div class="ap-prod__nome"><strong>Sabão em pó 1 kg</strong><span>Limpeza &middot; mínimo 15</span></div>
                                <span class="ap-pilha ap-prod__saldo sh-mono">
                                    <span class="sh-cena-fade ap-ini">35</span><span class="sh-cena-fade ap-fim">0</span>
                                </span>
                                <span class="sh-meter ap-med">
                                    <span class="ap-med__f ap-med__f--n sh-cena-fade ap-ini"></span><span class="ap-med__f ap-med__f--s sh-cena-fade ap-fim"></span>
                                    <span class="sh-meter__minimo"></span>
                                </span>
                                <span class="ap-pilha ap-prod__selo">
                                    <span class="sh-cena-fade ap-ini"><sh:badgeEstoque nivel="NORMAL" /></span><span class="sh-cena-fade ap-fim"><sh:badgeEstoque nivel="SEM_ESTOQUE" /></span>
                                </span>
                            </div>
                            <div class="ap-prod ap-prod--caf">
                                <div class="ap-prod__nome"><strong>Café torrado 500 g</strong><span>Mercearia &middot; mínimo 20</span></div>
                                <span class="ap-pilha ap-prod__saldo sh-mono">
                                    <span class="sh-cena-fade ap-ini">64</span><span class="sh-cena-fade ap-fim">18</span>
                                </span>
                                <span class="sh-meter ap-med">
                                    <span class="ap-med__f ap-med__f--n sh-cena-fade ap-ini"></span><span class="ap-med__f ap-med__f--b sh-cena-fade ap-fim"></span>
                                    <span class="sh-meter__minimo"></span>
                                </span>
                                <span class="ap-pilha ap-prod__selo">
                                    <span class="sh-cena-fade ap-ini"><sh:badgeEstoque nivel="NORMAL" /></span><span class="sh-cena-fade ap-fim"><sh:badgeEstoque nivel="BAIXO" /></span>
                                </span>
                            </div>
                            <div class="ap-prod ap-prod--fixo">
                                <div class="ap-prod__nome"><strong>Arroz tipo 1, 5 kg</strong><span>Mercearia &middot; mínimo 20</span></div>
                                <span class="ap-prod__saldo sh-mono">48</span>
                                <span class="sh-meter ap-med"><span class="ap-med__f ap-med__f--n ap-med__f--cheio"></span><span class="sh-meter__minimo"></span></span>
                                <span class="ap-prod__selo"><sh:badgeEstoque nivel="NORMAL" /></span>
                            </div>
                            <div class="ap-prod ap-prod--fixo">
                                <div class="ap-prod__nome"><strong>Água mineral 1,5 L</strong><span>Bebidas &middot; mínimo 60</span></div>
                                <span class="ap-prod__saldo sh-mono">120</span>
                                <span class="sh-meter ap-med"><span class="ap-med__f ap-med__f--n ap-med__f--cheio"></span><span class="sh-meter__minimo"></span></span>
                                <span class="ap-prod__selo"><sh:badgeEstoque nivel="NORMAL" /></span>
                            </div>
                        </div>
                    </div>
                    <p class="ap-fig-legenda">Maquete ilustrativa, com dados fictícios.</p>
                </div>
            </div>
        </div>
    </section>

    <%-- ===================== 02 Movimentações (cena fixa) ===================== --%>
    <section class="ap-secao sh-cena ap-cena ap-cena--mov" id="movimentacoes" aria-labelledby="ap-t-mov">
        <div class="sh-cena__palco ap-palco">
            <div class="ap-palco__grade">
                <div class="ap-texto sh-revelar-grupo">
                    <p class="sh-eyebrow">02 &middot; Movimentações</p>
                    <h2 class="sh-display" id="ap-t-mov">Cada movimentação, registrada.</h2>
                    <p class="sh-lead">
                        O saldo só muda por uma entrada, uma saída ou um ajuste, sempre com o saldo anterior e o novo.
                        O que foi gravado não se edita nem se apaga: um erro se corrige com outra movimentação.
                    </p>
                    <ul class="ap-pontos">
                        <li><i class="bi bi-arrow-right-circle" aria-hidden="true"></i>Saldo anterior e novo em cada linha do histórico.</li>
                        <li><i class="bi bi-pencil-square" aria-hidden="true"></i>O ajuste exige um motivo.</li>
                        <li><i class="bi bi-slash-circle" aria-hidden="true"></i>Uma saída maior que o saldo é recusada e nada é gravado.</li>
                    </ul>
                </div>

                <div class="ap-demo" aria-hidden="true">
                    <div class="ap-cartao">
                        <div class="ap-cartao__cab">
                            <span class="ap-cartao__titulo">Arroz tipo 1, 5 kg <em>(exemplo)</em></span>
                            <span class="ap-saldo-topo">
                                <span class="ap-saldo-topo__rotulo">Saldo</span>
                                <span class="ap-pilha ap-pilha--num sh-mono">
                                    <span class="ap-n sh-cena-fade ap-s0">20</span>
                                    <span class="ap-n sh-cena-fade ap-s1">60</span>
                                    <span class="ap-n sh-cena-fade ap-s2">48</span>
                                    <span class="ap-n sh-cena-fade ap-s3">45</span>
                                </span>
                            </span>
                        </div>
                        <ol class="ap-razao">
                            <li class="ap-mov ap-mov--1">
                                <span class="ap-mov__no sh-cena-escala"></span>
                                <div class="ap-mov__corpo sh-cena-sobe">
                                    <div class="ap-mov__linha1"><span class="sh-badge sh-badge--entrada"><i class="bi bi-plus-circle-fill"></i>Entrada</span><span class="ap-mov__qtd sh-mono">+40</span><span class="ap-mov__hora sh-mono">09:12</span></div>
                                    <div class="ap-mov__saldos sh-mono"><span class="sh-cena-fade ap-ant">20</span><i class="bi bi-arrow-right sh-cena-fade ap-seta"></i><strong class="sh-cena-fade ap-novo">60</strong></div>
                                    <p class="ap-mov__obs">Reposição do fornecedor</p>
                                </div>
                            </li>
                            <li class="ap-mov ap-mov--2">
                                <span class="ap-mov__no sh-cena-escala"></span>
                                <div class="ap-mov__corpo sh-cena-sobe">
                                    <div class="ap-mov__linha1"><span class="sh-badge sh-badge--saida"><i class="bi bi-dash-circle-fill"></i>Saída</span><span class="ap-mov__qtd sh-mono">&minus;12</span><span class="ap-mov__hora sh-mono">11:40</span></div>
                                    <div class="ap-mov__saldos sh-mono"><span class="sh-cena-fade ap-ant">60</span><i class="bi bi-arrow-right sh-cena-fade ap-seta"></i><strong class="sh-cena-fade ap-novo">48</strong></div>
                                    <p class="ap-mov__obs">Venda do balcão</p>
                                </div>
                            </li>
                            <li class="ap-mov ap-mov--3">
                                <span class="ap-mov__no sh-cena-escala"></span>
                                <div class="ap-mov__corpo sh-cena-sobe">
                                    <div class="ap-mov__linha1"><span class="sh-badge sh-badge--ajuste"><i class="bi bi-pencil-fill"></i>Ajuste</span><span class="ap-mov__qtd sh-mono">&minus;3</span><span class="ap-mov__hora sh-mono">17:05</span></div>
                                    <div class="ap-mov__saldos sh-mono"><span class="sh-cena-fade ap-ant">48</span><i class="bi bi-arrow-right sh-cena-fade ap-seta"></i><strong class="sh-cena-fade ap-novo">45</strong></div>
                                    <p class="ap-mov__obs"><span class="ap-motivo">Motivo</span> Contagem física</p>
                                </div>
                            </li>
                            <li class="ap-mov ap-mov--4 ap-mov--recusa">
                                <span class="ap-mov__no sh-cena-escala"></span>
                                <div class="ap-mov__corpo sh-cena-sobe">
                                    <div class="ap-mov__linha1"><span class="sh-badge sh-badge--sem"><i class="bi bi-x-octagon-fill"></i>Recusada</span><span class="ap-mov__qtd sh-mono">&minus;50</span></div>
                                    <p class="ap-mov__obs">Saldo insuficiente (45). Nada foi gravado.</p>
                                </div>
                            </li>
                        </ol>
                        <p class="ap-trava sh-cena-fade"><i class="bi bi-lock-fill" aria-hidden="true"></i> Histórico imutável: sem edição, sem exclusão.</p>
                    </div>
                    <p class="ap-fig-legenda">Maquete ilustrativa, com dados fictícios.</p>
                </div>
            </div>
        </div>
    </section>

    <%-- ===================== 03 Alertas (cena fixa, escura) ===================== --%>
    <section class="ap-secao ap-escuro sh-cena ap-cena ap-cena--alertas" id="alertas" data-bs-theme="dark" aria-labelledby="ap-t-alertas">
        <div class="sh-cena__palco ap-palco">
            <div class="ap-palco__grade">
                <div class="ap-texto sh-revelar-grupo">
                    <p class="sh-eyebrow">03 &middot; Alertas</p>
                    <h2 class="sh-display" id="ap-t-alertas">Alertas antes de faltar.</h2>
                    <p class="sh-lead">
                        Quando o saldo chega ao estoque mínimo, o produto entra na lista de reposição, do mais urgente para o menos.
                        Dali dá para repor com uma entrada, sem procurar o produto de novo.
                    </p>
                    <ul class="ap-pontos">
                        <li><i class="bi bi-graph-down-arrow" aria-hidden="true"></i>O gráfico de 14 dias mostra o que entrou e o que saiu.</li>
                        <li><i class="bi bi-bell" aria-hidden="true"></i>A lista de reposição aparece logo no início.</li>
                    </ul>
                </div>

                <div class="ap-demo" aria-hidden="true">
                    <div class="ap-cartao ap-cartao--grafico">
                        <div class="ap-cartao__cab">
                            <span class="ap-cartao__titulo">Café torrado 500 g, nível em 14 dias <em>(exemplo)</em></span>
                        </div>
                        <svg class="ap-nivel" viewBox="0 0 560 210" focusable="false">
                            <g class="ap-nivel__grade">
                                <path d="M30 30 H530 M30 80 H530 M30 130 H530 M30 180 H530"/>
                            </g>
                            <line class="ap-nivel__min sh-cena-fade" x1="30" x2="530" y1="149" y2="149"/>
                            <text class="ap-nivel__txt sh-cena-fade ap-t-min" x="34" y="141">Estoque mínimo</text>
                            <polygon class="ap-nivel__area sh-cena-fade ap-t-area" points="30,43.6 68.5,53.8 107,64 145.4,81 183.9,77.6 222.3,98 260.8,111.6 299.2,121.8 337.7,132 376.2,142.2 414.6,150.7 453.1,159.2 491.5,166 530,172.8 530,180 30,180"/>
                            <polyline class="ap-nivel__linha sh-cena-desenha ap-t-linha" pathLength="1"
                                points="30,43.6 68.5,53.8 107,64 145.4,81 183.9,77.6 222.3,98 260.8,111.6 299.2,121.8 337.7,132 376.2,142.2 414.6,150.7 453.1,159.2 491.5,166 530,172.8"/>
                            <g class="sh-cena-fade ap-t-marca">
                                <circle class="ap-nivel__alvo" cx="406.9" cy="149" r="5.5"/>
                                <text class="ap-nivel__rotulo" x="398" y="112" text-anchor="end">Abaixo do mínimo</text>
                                <path class="ap-nivel__guia" d="M400 118 L406 142"/>
                            </g>
                            <text class="ap-nivel__eixo" x="30" y="200">há 14 dias</text>
                            <text class="ap-nivel__eixo" x="530" y="200" text-anchor="end">hoje</text>
                        </svg>
                    </div>
                    <div class="ap-cartao ap-cartao--lista">
                        <div class="ap-cartao__cab"><span class="ap-cartao__titulo">Requer ação <em>(exemplo)</em></span></div>
                        <ul class="ap-repor">
                            <li class="ap-repor__item sh-cena-sobe ap-r1">
                                <div class="ap-prod__nome"><strong>Sabão em pó 1 kg</strong><span class="sh-mono">0 un. &middot; mínimo 15</span></div>
                                <sh:sparkline valores="22,15,9,4,0" tom="sem" largura="72" altura="24" anima="false" rotulo="Saldo em queda" />
                                <sh:badgeEstoque nivel="SEM_ESTOQUE" />
                                <span class="btn btn-primary btn-sm">Repor</span>
                            </li>
                            <li class="ap-repor__item sh-cena-sobe ap-r2">
                                <div class="ap-prod__nome"><strong>Café torrado 500 g</strong><span class="sh-mono">18 un. &middot; mínimo 20</span></div>
                                <sh:sparkline valores="64,52,41,33,24,18" tom="baixo" largura="72" altura="24" anima="false" rotulo="Saldo em queda" />
                                <sh:badgeEstoque nivel="BAIXO" />
                                <span class="btn btn-primary btn-sm">Repor</span>
                            </li>
                            <li class="ap-repor__item sh-cena-sobe ap-r3">
                                <div class="ap-prod__nome"><strong>Detergente neutro 500 ml</strong><span class="sh-mono">28 un. &middot; mínimo 30</span></div>
                                <sh:sparkline valores="62,55,47,40,33,28" tom="baixo" largura="72" altura="24" anima="false" rotulo="Saldo em queda" />
                                <sh:badgeEstoque nivel="BAIXO" />
                                <span class="btn btn-primary btn-sm">Repor</span>
                            </li>
                        </ul>
                    </div>
                    <p class="ap-fig-legenda">Maquete ilustrativa, com dados fictícios.</p>
                </div>
            </div>
        </div>
    </section>

    <%-- ===================== 04 Atalhos (cena fixa) ===================== --%>
    <section class="ap-secao sh-cena ap-cena ap-cena--atalhos" id="atalhos" aria-labelledby="ap-t-atalhos">
        <div class="sh-cena__palco ap-palco">
            <div class="ap-palco__grade">
                <div class="ap-texto sh-revelar-grupo">
                    <p class="sh-eyebrow">04 &middot; Atalhos</p>
                    <h2 class="sh-display" id="ap-t-atalhos">Tudo à mão, sem largar o teclado.</h2>
                    <p class="sh-lead">
                        Aperte <kbd class="sh-tecla" data-ap-tecla>Ctrl K</kbd> para buscar um produto, ir a uma tela ou registrar uma movimentação.
                        Atalhos de uma tecla cobrem o resto.
                    </p>
                    <dl class="sh-atalhos ap-atalhos">
                        <dt class="sh-cena-fade ap-a1"><kbd class="sh-tecla">/</kbd></dt>
                        <dd class="sh-cena-fade ap-a1">Buscar</dd>
                        <dt class="sh-cena-fade ap-a2"><kbd class="sh-tecla">n</kbd></dt>
                        <dd class="sh-cena-fade ap-a2">Novo produto</dd>
                        <dt class="sh-cena-fade ap-a3"><kbd class="sh-tecla">m</kbd></dt>
                        <dd class="sh-cena-fade ap-a3">Movimentar estoque</dd>
                        <dt class="sh-cena-fade ap-a4"><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">p</kbd></dt>
                        <dd class="sh-cena-fade ap-a4">Ir para Produtos</dd>
                        <dt class="sh-cena-fade ap-a5"><kbd class="sh-tecla">?</kbd></dt>
                        <dd class="sh-cena-fade ap-a5">Ver todos os atalhos</dd>
                    </dl>
                </div>

                <div class="ap-demo" aria-hidden="true">
                    <div class="ap-paleta">
                        <div class="ap-paleta__topo">
                            <i class="bi bi-search"></i>
                            <span class="ap-pilha ap-paleta__texto">
                                <span class="sh-cena-fade ap-q ap-q--vazio">Buscar produto, ir para uma tela ou executar um comando&hellip;</span>
                                <span class="sh-cena-fade ap-q ap-q1">c</span>
                                <span class="sh-cena-fade ap-q ap-q2">ca</span>
                                <span class="sh-cena-fade ap-q ap-q3">caf</span>
                                <span class="sh-cena-fade ap-q ap-q4">café</span>
                            </span>
                            <span class="ap-paleta__esc">Esc</span>
                        </div>
                        <div class="ap-paleta__corpo">
                            <div class="ap-pilha ap-paleta__listas">
                                <div class="sh-cena-fade ap-lista-a">
                                    <p class="ap-paleta__grupo">Ações</p>
                                    <div class="ap-op ap-op--sel"><i class="bi bi-plus-circle"></i>Registrar entrada</div>
                                    <div class="ap-op"><i class="bi bi-dash-circle"></i>Registrar saída</div>
                                    <div class="ap-op"><i class="bi bi-box-seam"></i>Novo produto</div>
                                    <p class="ap-paleta__grupo">Telas</p>
                                    <div class="ap-op"><i class="bi bi-boxes"></i>Produtos</div>
                                    <div class="ap-op"><i class="bi bi-arrow-left-right"></i>Movimentações</div>
                                </div>
                                <div class="sh-cena-fade ap-lista-b">
                                    <p class="ap-paleta__grupo">Produtos</p>
                                    <div class="ap-op ap-op--sel"><i class="bi bi-box-seam"></i><span class="ap-op__nome"><mark>Caf</mark>é torrado 500 g</span><sh:badgeEstoque nivel="BAIXO" /></div>
                                    <div class="ap-op"><i class="bi bi-box-seam"></i><span class="ap-op__nome"><mark>Caf</mark>é solúvel 200 g</span><sh:badgeEstoque nivel="NORMAL" /></div>
                                    <div class="ap-op"><i class="bi bi-box-seam"></i><span class="ap-op__nome"><mark>Caf</mark>eteira 12 xícaras</span><sh:badgeEstoque nivel="NORMAL" /></div>
                                </div>
                            </div>
                        </div>
                        <p class="ap-paleta__rodape"><span><kbd>&uarr;</kbd> <kbd>&darr;</kbd> navegar</span><span><kbd>Enter</kbd> abrir</span><span><kbd>Esc</kbd> fechar</span></p>
                    </div>
                    <p class="ap-fig-legenda">Maquete ilustrativa, com dados fictícios.</p>
                </div>
            </div>
        </div>
    </section>

    <%-- ===================== 05 Confiança (escura) ===================== --%>
    <section class="ap-secao ap-escuro ap-confianca" id="confianca" data-bs-theme="dark" aria-labelledby="ap-t-conf">
        <div class="ap-bloco-centro">
            <p class="sh-eyebrow sh-revelar">05 &middot; Confiança</p>
            <h2 class="sh-display sh-revelar" id="ap-t-conf">Feito para ser confiável.</h2>
            <p class="sh-lead sh-revelar">O que o sistema garante, sem promessa vaga.</p>
        </div>
        <ul class="ap-garantias sh-revelar-grupo">
            <li class="ap-garantia">
                <i class="bi bi-shield-lock" aria-hidden="true"></i>
                <h3>Senhas com BCrypt</h3>
                <p>Nenhuma senha é guardada como texto: o banco recebe só o hash.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-shield-check" aria-hidden="true"></i>
                <h3>Proteção CSRF</h3>
                <p>Todo formulário envia um token, e o servidor recusa pedidos sem ele.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-building-lock" aria-hidden="true"></i>
                <h3>Cada loja vê só o seu</h3>
                <p>Consultas e alterações são filtradas pela loja que entrou.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-slash-circle" aria-hidden="true"></i>
                <h3>Saldo nunca negativo</h3>
                <p>Uma saída maior que o saldo é recusada, e nada é gravado.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-journal-check" aria-hidden="true"></i>
                <h3>Histórico imutável</h3>
                <p>Movimentações não têm edição nem exclusão. Erro se corrige com outra.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-filetype-csv" aria-hidden="true"></i>
                <h3>Exportação em CSV</h3>
                <p>Produtos e movimentações saem em planilha, quando você quiser.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-circle-half" aria-hidden="true"></i>
                <h3>Tema claro e escuro</h3>
                <p>Segue o seu sistema, ou a sua escolha, e lembra dela.</p>
            </li>
            <li class="ap-garantia">
                <i class="bi bi-keyboard" aria-hidden="true"></i>
                <h3>Acessível por teclado</h3>
                <p>Foco visível, rótulo em todo campo e atalhos para as tarefas comuns.</p>
            </li>
        </ul>
    </section>

    <%-- ===================== Chamada final e rodapé ===================== --%>
    <section class="ap-secao ap-final" id="comecar" aria-labelledby="ap-t-final">
        <h2 class="sh-hero-texto ap-final__titulo sh-revelar" id="ap-t-final">Comece pelo primeiro produto.</h2>
        <p class="sh-lead sh-revelar">Crie a conta da sua loja e acompanhe o estoque desde hoje.</p>
        <div class="ap-hero__ctas ap-hero__ctas--centro sh-revelar">
            <a class="btn btn-primary btn-lg" href="${ctx}/cadastro">Criar conta</a>
            <a class="btn btn-outline-secondary btn-lg" href="${ctx}/login">Entrar</a>
        </div>
    </section>
    <footer class="ap-rodape">
        <div class="ap-rodape__in">
            <span class="ap-rodape__marca"><sh:marca tamanho="20" /> StockHub</span>
            <span class="ap-rodape__texto">Sistema de gestão de estoque. Projeto acadêmico da UFSM.</span>
            <nav aria-label="Rodapé">
                <a href="${ctx}/login">Entrar</a>
                <a href="${ctx}/cadastro">Criar conta</a>
                <a href="#inicio">Voltar ao topo</a>
            </nav>
        </div>
    </footer>
    </jsp:body>
</sh:layoutPublico>
