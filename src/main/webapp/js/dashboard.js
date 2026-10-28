/*
 * StockHub: gráfico de 14 dias do início (dashboard). Desenhado aqui em SVG (sem biblioteca) com os dados de
 * GET /api/dashboard/movimentos?dias=14 -> [{"dia":"aaaa-MM-dd","entradas":int,"saidas":int,"ajustes":int}].
 *   Entradas sobem da linha do meio, saídas descem (a posição diz o tipo, não só a cor).
 *   Estados: carregando (esqueleto), erro (com "Tentar de novo"), vazio (nenhum movimento) e pronto.
 *   Tooltip por mouse, toque E teclado (setas, Home, End; um só tab stop). Tabela equivalente em <details>.
 *   Se o endpoint não existir ou falhar, só o gráfico mostra o erro; o resto da tela segue normal.
 * Nunca monta HTML a partir de dado: createElementNS/createElement e textContent.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var NS = 'http://www.w3.org/2000/svg';

  /* ---------- 2. Gráfico de 14 dias ---------- */
  var raiz = document.querySelector('[data-sh-grafico]');
  if (!raiz) {
    return;
  }

  var url = raiz.getAttribute('data-url');
  var area = raiz.querySelector('[data-grafico-area]');
  var quadro = raiz.querySelector('[data-grafico-quadro]');
  var dica = raiz.querySelector('[data-grafico-dica]');
  var resumo = raiz.querySelector('[data-grafico-resumo]');
  var tabela = raiz.querySelector('[data-grafico-tabela]');
  var linhas = raiz.querySelector('[data-grafico-linhas]');
  var estados = {
    carregando: raiz.querySelector('[data-grafico-estado="carregando"]'),
    erro: raiz.querySelector('[data-grafico-estado="erro"]'),
    vazio: raiz.querySelector('[data-grafico-estado="vazio"]')
  };
  var textoErro = raiz.querySelector('[data-grafico-erro-texto]');
  var DIAS = 14;
  var dados = null; // [{data: Date, chave, entradas, saidas, ajustes}]
  var diaAtivo = -1;
  var elementosDia = [];

  var MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
  var SEMANA = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado'];

  function mostrar(qual) {
    for (var chave in estados) {
      if (Object.prototype.hasOwnProperty.call(estados, chave) && estados[chave]) {
        estados[chave].hidden = chave !== qual;
      }
    }
    quadro.hidden = qual !== 'pronto';
    tabela.hidden = qual !== 'pronto';
    esconderDica();
  }

  function el(nome, atributos, classe) {
    var e = document.createElementNS(NS, nome);
    if (classe) {
      e.setAttribute('class', classe);
    }
    for (var a in atributos) {
      if (Object.prototype.hasOwnProperty.call(atributos, a)) {
        e.setAttribute(a, String(atributos[a]));
      }
    }
    return e;
  }

  function dataDe(texto) {
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(texto));
    if (!m) {
      return null;
    }
    return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  }

  function chaveDe(d) {
    var mm = d.getMonth() + 1;
    var dd = d.getDate();
    return d.getFullYear() + '-' + (mm < 10 ? '0' : '') + mm + '-' + (dd < 10 ? '0' : '') + dd;
  }

  function inteiro(v) {
    var n = Number(v);
    return isFinite(n) && n > 0 ? Math.round(n) : 0;
  }

  function plural(n, um, varios) {
    return n + ' ' + (n === 1 ? um : varios);
  }

  /** Monta os 14 dias seguidos terminando em hoje, preenchendo com zero os dias que a API não trouxe. */
  function normalizar(lista) {
    if (!Array.isArray(lista)) {
      throw new Error('formato');
    }
    var mapa = {};
    for (var i = 0; i < lista.length; i++) {
      var item = lista[i];
      if (!item || typeof item.dia !== 'string') {
        continue;
      }
      var d = dataDe(item.dia);
      if (!d) {
        continue;
      }
      var chave = chaveDe(d);
      var atual = mapa[chave] || { entradas: 0, saidas: 0, ajustes: 0 };
      atual.entradas += inteiro(item.entradas);
      atual.saidas += inteiro(item.saidas);
      atual.ajustes += inteiro(item.ajustes);
      mapa[chave] = atual;
    }
    var hoje = new Date();
    hoje = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
    var saida = [];
    for (var k = DIAS - 1; k >= 0; k--) {
      var dia = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - k);
      var chaveDia = chaveDe(dia);
      var v = mapa[chaveDia] || { entradas: 0, saidas: 0, ajustes: 0 };
      saida.push({ data: dia, chave: chaveDia, entradas: v.entradas, saidas: v.saidas, ajustes: v.ajustes, hoje: k === 0 });
    }
    return saida;
  }

  function rotuloDia(d, ehHoje, ontem) {
    var base = d.getDate() + ' de ' + MESES[d.getMonth()];
    if (ehHoje) {
      return 'hoje, ' + base;
    }
    if (ontem) {
      return 'ontem, ' + base;
    }
    return SEMANA[d.getDay()] + ', ' + base;
  }

  function rotuloAria(item, indice) {
    var partes = [
      plural(item.entradas, 'entrada', 'entradas'),
      plural(item.saidas, 'saída', 'saídas'),
      plural(item.ajustes, 'ajuste', 'ajustes')
    ];
    return rotuloDia(item.data, item.hoje, indice === DIAS - 2) + ': ' + partes.join(', ');
  }

  function escalaMaxima(maior) {
    if (maior <= 4) {
      return 4;
    }
    var passos = [5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000, 10000];
    for (var i = 0; i < passos.length; i++) {
      if (maior <= passos[i]) {
        return passos[i];
      }
    }
    var pot = Math.pow(10, Math.floor(Math.log(maior) / Math.LN10));
    return Math.ceil(maior / pot) * pot;
  }

  /* ---------- Desenho ---------- */
  function desenhar(semAnimar) {
    if (!dados) {
      return;
    }
    var largura = Math.max(280, Math.floor(quadro.getBoundingClientRect().width || area.getBoundingClientRect().width));
    var estreito = largura < 480;
    var altura = estreito ? 230 : 270;
    var m = { e: 40, d: 6, t: 22, b: estreito ? 30 : 40 };
    var pw = largura - m.e - m.d;
    var ph = altura - m.t - m.b;
    var meio = m.t + ph / 2;
    var banda = pw / DIAS;
    var barra = Math.min(banda * 0.62, 30);

    var maior = 0;
    for (var i = 0; i < dados.length; i++) {
      maior = Math.max(maior, dados[i].entradas, dados[i].saidas);
    }
    var teto = escalaMaxima(maior);
    var porUnidade = ph / 2 / teto;

    var svg = el('svg', { viewBox: '0 0 ' + largura + ' ' + altura, width: largura, height: altura, focusable: 'false' }, 'db-grafico__svg sh-desenhar' + (semAnimar ? ' is-visivel' : ''));
    // gradientes suaves das barras (o desenho em si usa fill:url(#...) no CSS)
    var defs = el('defs', {});
    var gE = el('linearGradient', { id: 'db-grad-entrada', x1: 0, y1: 0, x2: 0, y2: 1 });
    gE.appendChild(el('stop', { offset: '0%' }, 'db-grad-e0'));
    gE.appendChild(el('stop', { offset: '100%' }, 'db-grad-e1'));
    var gS = el('linearGradient', { id: 'db-grad-saida', x1: 0, y1: 1, x2: 0, y2: 0 });
    gS.appendChild(el('stop', { offset: '0%' }, 'db-grad-s0'));
    gS.appendChild(el('stop', { offset: '100%' }, 'db-grad-s1'));
    defs.appendChild(gE);
    defs.appendChild(gS);
    svg.appendChild(defs);
    svg.setAttribute('role', 'group');
    svg.setAttribute('aria-label', 'Gráfico de entradas e saídas dos últimos 14 dias. Use as setas para percorrer os dias.');

    // grade e eixo Y (acima do meio: entradas; abaixo: saídas)
    var marcas = [teto, teto / 2, 0, teto / 2, teto];
    for (var g = 0; g < marcas.length; g++) {
      var y = g === 2 ? meio : (g < 2 ? meio - marcas[g] * porUnidade : meio + marcas[g] * porUnidade);
      if (g !== 2) {
        svg.appendChild(el('line', { x1: m.e, x2: largura - m.d, y1: y, y2: y }, 'db-grafico__grade'));
      }
      var ehInteiro = marcas[g] === Math.floor(marcas[g]);
      if (ehInteiro) {
        var rotulo = el('text', { x: m.e - 8, y: y + 4, 'text-anchor': 'end', 'aria-hidden': 'true' }, 'db-grafico__eixo');
        rotulo.textContent = String(marcas[g]);
        svg.appendChild(rotulo);
      }
    }

    // destaque de hoje (última banda)
    svg.appendChild(el('rect', { x: m.e + banda * (DIAS - 1) + 1, y: m.t - 6, width: banda - 2, height: ph + 12, rx: 6 }, 'db-grafico__hoje'));

    svg.appendChild(el('line', { x1: m.e, x2: largura - m.d, y1: meio, y2: meio }, 'db-grafico__base'));

    elementosDia = [];
    for (var d = 0; d < dados.length; d++) {
      var item = dados[d];
      var cx = m.e + banda * d + banda / 2;
      var gDia = el('g', { tabindex: d === (diaAtivo >= 0 ? diaAtivo : DIAS - 1) ? '0' : '-1', role: 'img' }, 'db-grafico__dia');
      gDia.setAttribute('aria-label', rotuloAria(item, d));
      gDia.setAttribute('data-indice', String(d));
      gDia.appendChild(el('rect', { x: m.e + banda * d + 1, y: m.t - 6, width: banda - 2, height: ph + 12, rx: 6 }, 'db-grafico__hit'));

      gDia.appendChild(el('line', { x1: cx, x2: cx, y1: m.t - 6, y2: m.t + ph + 6 }, 'db-grafico__cruz'));

      var hEntrada = item.entradas * porUnidade;
      var hSaida = item.saidas * porUnidade;
      if (item.entradas > 0) {
        var rE = el('rect', { x: cx - barra / 2, y: meio - Math.max(hEntrada, 2), width: barra, height: Math.max(hEntrada, 2), rx: Math.min(7, barra / 2) }, 'db-grafico__barra db-grafico__barra--entrada sh-barra');
        rE.style.setProperty('--sh-i', String(d));
        gDia.appendChild(rE);
      }
      if (item.saidas > 0) {
        var rS = el('rect', { x: cx - barra / 2, y: meio, width: barra, height: Math.max(hSaida, 2), rx: Math.min(7, barra / 2) }, 'db-grafico__barra db-grafico__barra--saida sh-barra');
        rS.style.setProperty('--sh-i', String(d));
        gDia.appendChild(rS);
      }
      if (item.ajustes > 0) {
        gDia.appendChild(el('circle', { cx: cx, cy: meio, r: 4 }, 'db-grafico__ajuste'));
      }

      // rótulos do eixo X: número do dia; no começo do mês, também o mês
      var mostrarNumero = !estreito || d % 2 === (DIAS - 1) % 2;
      if (mostrarNumero) {
        var tx = el('text', { x: cx, y: altura - m.b + 14, 'text-anchor': 'middle', 'aria-hidden': 'true' }, 'db-grafico__eixo' + (item.hoje ? ' db-grafico__eixo--forte' : ''));
        tx.textContent = String(item.data.getDate());
        gDia.appendChild(tx);
        if (!estreito) {
          var sub = el('text', { x: cx, y: altura - m.b + 28, 'text-anchor': 'middle', 'aria-hidden': 'true' }, 'db-grafico__eixo');
          sub.textContent = item.hoje ? 'hoje' : SEMANA[item.data.getDay()].slice(0, 3);
          gDia.appendChild(sub);
        }
      }

      (function (indice, grupo) {
        grupo.addEventListener('pointerenter', function (ev) {
          if (ev.pointerType !== 'touch') {
            mostrarDica(indice, false);
          }
        });
        grupo.addEventListener('pointerleave', function (ev) {
          if (ev.pointerType !== 'touch' && document.activeElement !== grupo) {
            esconderDica();
          }
        });
        grupo.addEventListener('focus', function () {
          mostrarDica(indice, true);
        });
        grupo.addEventListener('blur', function () {
          esconderDica();
        });
        grupo.addEventListener('click', function () {
          // toque: mostra a dica do dia tocado
          mostrarDica(indice, false);
        });
      })(d, gDia);

      svg.appendChild(gDia);
      elementosDia.push(gDia);
    }

    quadro.textContent = '';
    quadro.appendChild(svg);
    if (!semAnimar && SH.rolagem && SH.rolagem.atualizar) {
      SH.rolagem.atualizar(); // o plano B (sem animation-timeline) passa a observar o gráfico novo
    }
    quadro.__geo = { m: m, banda: banda, altura: altura, largura: largura, meio: meio, porUnidade: porUnidade };
  }

  /* ---------- Dica (tooltip) ---------- */
  function esconderDica() {
    if (dica) {
      dica.hidden = true;
    }
    for (var i = 0; i < elementosDia.length; i++) {
      elementosDia[i].classList.remove('is-ativo');
    }
  }

  function linhaDica(classe, texto) {
    var linha = document.createElement('div');
    linha.className = 'db-grafico__dica-linha';
    var marca = document.createElement('span');
    marca.className = 'db-grafico__dica-marca ' + classe;
    linha.appendChild(marca);
    var t = document.createElement('span');
    t.textContent = texto;
    linha.appendChild(t);
    return linha;
  }

  function mostrarDica(indice) {
    if (!dados || !dados[indice] || !quadro.__geo) {
      return;
    }
    var item = dados[indice];
    var geo = quadro.__geo;
    for (var i = 0; i < elementosDia.length; i++) {
      elementosDia[i].classList.toggle('is-ativo', i === indice);
    }
    dica.textContent = '';
    var titulo = document.createElement('strong');
    titulo.textContent = rotuloDia(item.data, item.hoje, indice === DIAS - 2);
    dica.appendChild(titulo);
    dica.appendChild(linhaDica('db-grafico__dica-marca--entrada', plural(item.entradas, 'entrada', 'entradas')));
    dica.appendChild(linhaDica('db-grafico__dica-marca--saida', plural(item.saidas, 'saída', 'saídas')));
    if (item.ajustes > 0) {
      dica.appendChild(linhaDica('db-grafico__dica-marca--ajuste', plural(item.ajustes, 'ajuste', 'ajustes')));
    }
    dica.hidden = false;

    var cx = geo.m.e + geo.banda * indice + geo.banda / 2;
    var larguraDica = dica.offsetWidth;
    var esquerda = cx - larguraDica / 2;
    var limite = quadro.getBoundingClientRect().width - larguraDica;
    esquerda = Math.max(0, Math.min(limite, esquerda));
    var topo = Math.max(0, geo.meio - 90);
    dica.style.setProperty('transform', 'translate(' + Math.round(esquerda) + 'px, ' + Math.round(topo) + 'px)');
  }

  /* ---------- Teclado: setas entre os dias (um tab stop só) ---------- */
  quadro.addEventListener('keydown', function (evento) {
    var alvo = evento.target;
    if (!alvo || !alvo.getAttribute || alvo.getAttribute('data-indice') === null) {
      return;
    }
    var atual = Number(alvo.getAttribute('data-indice'));
    var novo = atual;
    if (evento.key === 'ArrowRight') {
      novo = Math.min(DIAS - 1, atual + 1);
    } else if (evento.key === 'ArrowLeft') {
      novo = Math.max(0, atual - 1);
    } else if (evento.key === 'Home') {
      novo = 0;
    } else if (evento.key === 'End') {
      novo = DIAS - 1;
    } else if (evento.key === 'Escape') {
      esconderDica();
      return;
    } else {
      return;
    }
    evento.preventDefault();
    elementosDia[atual].setAttribute('tabindex', '-1');
    elementosDia[novo].setAttribute('tabindex', '0');
    diaAtivo = novo;
    elementosDia[novo].focus();
  });

  // Toque fora do gráfico esconde a dica
  document.addEventListener('pointerdown', function (evento) {
    if (!quadro.contains(evento.target)) {
      esconderDica();
    }
  });

  /* ---------- Resumo e tabela ---------- */
  function preencherResumo() {
    var totalE = 0;
    var totalS = 0;
    for (var i = 0; i < dados.length; i++) {
      totalE += dados[i].entradas;
      totalS += dados[i].saidas;
    }
    resumo.textContent = '';
    resumo.appendChild(document.createTextNode('Nos últimos 14 dias: '));
    var e = document.createElement('strong');
    e.setAttribute('data-contar', String(totalE));
    e.setAttribute('data-formato', 'inteiro');
    e.textContent = String(totalE);
    resumo.appendChild(e);
    resumo.appendChild(document.createTextNode(totalE === 1 ? ' entrada e ' : ' entradas e '));
    var s = document.createElement('strong');
    s.setAttribute('data-contar', String(totalS));
    s.setAttribute('data-formato', 'inteiro');
    s.textContent = String(totalS);
    resumo.appendChild(s);
    resumo.appendChild(document.createTextNode(totalS === 1 ? ' saída.' : ' saídas.'));
    if (SH.animarNumeros) {
      SH.animarNumeros(resumo);
    }
  }

  function preencherTabela() {
    linhas.textContent = '';
    for (var i = dados.length - 1; i >= 0; i--) {
      var item = dados[i];
      var tr = document.createElement('tr');
      var th = document.createElement('th');
      th.scope = 'row';
      th.textContent = rotuloDia(item.data, item.hoje, i === DIAS - 2);
      tr.appendChild(th);
      var valores = [item.entradas, item.saidas, item.ajustes];
      for (var v = 0; v < valores.length; v++) {
        var td = document.createElement('td');
        td.className = 'text-end';
        td.textContent = String(valores[v]);
        tr.appendChild(td);
      }
      linhas.appendChild(tr);
    }
  }

  /* ---------- Carregamento ---------- */
  var requisicao = 0;

  function carregar() {
    var minha = ++requisicao;
    mostrar('carregando');
    resumo.textContent = 'Carregando os movimentos…';
    if (!window.fetch || !url) {
      falhar('Este navegador não consegue buscar os dados do gráfico.');
      return;
    }
    var controle = window.AbortController ? new window.AbortController() : null;
    var tempo = window.setTimeout(function () {
      if (controle) {
        controle.abort();
      }
    }, 8000);
    window
      .fetch(url, {
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
        signal: controle ? controle.signal : undefined
      })
      .then(function (resposta) {
        if (resposta.redirected && /\/login/.test(resposta.url)) {
          var sessao = new Error('sessao');
          sessao.sessao = true;
          throw sessao;
        }
        var tipo = resposta.headers.get('content-type') || '';
        if (!resposta.ok || tipo.indexOf('json') < 0) {
          throw new Error('http ' + resposta.status);
        }
        return resposta.json();
      })
      .then(function (lista) {
        window.clearTimeout(tempo);
        if (minha !== requisicao) {
          return;
        }
        dados = normalizar(lista);
        var total = 0;
        for (var i = 0; i < dados.length; i++) {
          total += dados[i].entradas + dados[i].saidas + dados[i].ajustes;
        }
        if (total === 0) {
          resumo.textContent = 'Sem movimentos nos últimos 14 dias.';
          mostrar('vazio');
          if (SH.anunciar) {
            SH.anunciar('Gráfico carregado: nenhum movimento nos últimos 14 dias.');
          }
          return;
        }
        diaAtivo = -1;
        mostrar('pronto');
        desenhar();
        preencherResumo();
        preencherTabela();
        if (SH.anunciar) {
          SH.anunciar('Gráfico de 14 dias carregado.');
        }
      })
      .catch(function (erro) {
        window.clearTimeout(tempo);
        if (minha !== requisicao) {
          return;
        }
        if (erro && erro.sessao) {
          falhar('Sua sessão pode ter expirado. Entre de novo para ver o gráfico.');
        } else if (erro && erro.name === 'AbortError') {
          falhar('A busca demorou demais. Tente de novo.');
        } else {
          falhar('O resto da tela continua valendo. Tente de novo em instantes.');
        }
      });
  }

  function falhar(texto) {
    dados = null;
    if (textoErro) {
      textoErro.textContent = texto;
    }
    resumo.textContent = 'Não foi possível carregar o gráfico.';
    mostrar('erro');
  }

  raiz.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-grafico-tentar]') : null;
    if (botao) {
      carregar();
    }
  });

  // Redesenha ao mudar a largura (rotação do celular, recolher a barra lateral)
  var agendado = false;
  var larguraAnterior = 0;
  function aoRedimensionar() {
    if (agendado || !dados || quadro.hidden) {
      return;
    }
    var atual = Math.floor(quadro.getBoundingClientRect().width);
    if (atual === larguraAnterior) {
      return;
    }
    agendado = true;
    window.requestAnimationFrame(function () {
      agendado = false;
      larguraAnterior = Math.floor(quadro.getBoundingClientRect().width);
      var tinhaFoco = quadro.contains(document.activeElement);
      desenhar(true);
      if (tinhaFoco && elementosDia[diaAtivo >= 0 ? diaAtivo : DIAS - 1]) {
        elementosDia[diaAtivo >= 0 ? diaAtivo : DIAS - 1].focus();
      }
    });
  }
  if (window.ResizeObserver) {
    new window.ResizeObserver(aoRedimensionar).observe(area);
  } else {
    window.addEventListener('resize', aoRedimensionar);
  }

  carregar();
})();
