/*
 * StockHub: gráficos da tela /analises. SVG próprio, sem biblioteca.
 *
 * Cada <figure data-grafico="..." data-tabela="id"> é desenhada a partir de uma TABELA da própria página (a
 * alternativa em texto para leitor de tela e para quem está sem JavaScript). Os números crus ficam em data-v nas
 * células; o texto da célula é o formatado em pt-BR e só aparece na dica. O rótulo de cada linha é a primeira
 * célula (data-r guarda o valor cru: aaaa-mm-dd ou aaaa-mm).
 *
 * Forma pelo trabalho do dado:
 *   barras     ranking (barras horizontais, uma por linha; data-limite/data-maximo para a cobertura)
 *   composicao parte do todo: faixa 100% (as 5 maiores + "Outras") e, abaixo, o ranking com a parte de cada uma
 *   linha      uma série no tempo: linha suave com área; data-dias preenche os dias sem saída com zero
 *   linhas     duas séries no tempo sobrepostas (compras x saídas por mês), com legenda e rótulo direto
 *   pareto     curva ABC: barras da parte de cada produto + linha da soma acumulada, no mesmo eixo de 0 a 100%
 * Atributos: data-formato (moeda | inteiro | dias), data-coluna / data-colunas (índices das células de valor),
 * data-classe (índice da classe, no pareto), data-ordenar="desc", data-rotulo-acessivel.
 *
 * Cores e temas vêm do CSS (classes an-*): trocar o tema não exige redesenhar. Os textos vindos da tabela entram
 * sempre por textContent. A animação de desenho roda só na primeira vez e é desligada por prefers-reduced-motion
 * (no CSS). Nos gráficos no tempo há um único ponto de tabulação: setas, Home, End e Esc.
 */
(function () {
  'use strict';

  var NS = 'http://www.w3.org/2000/svg';
  var MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  var fmtMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  var fmtInt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
  var fmtDec1 = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  var fmtCompacto = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
  var contador = 0;

  /* ---------- Utilidades ---------- */
  function num(v) {
    var n = parseFloat(String(v == null ? '' : v).replace(',', '.'));
    return isNaN(n) ? 0 : n;
  }

  function svgEl(tag, attrs, texto) {
    var e = document.createElementNS(NS, tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (k) { e.setAttribute(k, attrs[k]); });
    }
    if (texto != null) {
      e.textContent = texto;
    }
    return e;
  }

  function htmlEl(tag, classe, texto) {
    var e = document.createElement(tag);
    if (classe) { e.className = classe; }
    if (texto != null) { e.textContent = texto; }
    return e;
  }

  function completo(n, formato) {
    if (formato === 'moeda') { return fmtMoeda.format(n); }
    if (formato === 'dias') { return fmtDec1.format(n) + ' dias'; }
    if (formato === 'pct') { return fmtDec1.format(n) + '%'; }
    return fmtInt.format(n);
  }

  /** Valor curto para os números do eixo: 1,5 mil, 2 mi. */
  function compacto(n, formato) {
    if (formato === 'pct') { return fmtInt.format(n) + '%'; }
    var prefixo = formato === 'moeda' ? 'R$ ' : '';
    var abs = Math.abs(n);
    if (abs >= 1e6) { return prefixo + fmtCompacto.format(n / 1e6) + ' mi'; }
    if (abs >= 1e3) { return prefixo + fmtCompacto.format(n / 1e3) + ' mil'; }
    return prefixo + fmtCompacto.format(n);
  }

  /** Eixo com números redondos: no máximo 5 marcas, de zero ao primeiro múltiplo do passo acima do maior valor. */
  function eixo(max, inteiro) {
    if (!(max > 0)) { return { topo: 1, marcas: [0, 1] }; }
    var bruto = max / 4;
    var mag = Math.pow(10, Math.floor(Math.log(bruto) / Math.LN10));
    var r = bruto / mag;
    var passo = (r <= 1 ? 1 : r <= 2 ? 2 : r <= 2.5 ? 2.5 : r <= 5 ? 5 : 10) * mag;
    if (inteiro && passo < 1) { passo = 1; }
    var topo = Math.ceil(max / passo - 1e-9) * passo;
    var marcas = [];
    for (var v = 0; v <= topo + passo / 1000; v += passo) { marcas.push(v); }
    return { topo: topo, marcas: marcas };
  }

  /** "2026-06-03" vira 03/06; "2026-06" vira jun/26; outro texto passa como está. */
  function rotuloCurto(cru, texto) {
    var d = /^(\d{4})-(\d{2})-(\d{2})$/.exec(cru || '');
    if (d) { return d[3] + '/' + d[2]; }
    var m = /^(\d{4})-(\d{2})$/.exec(cru || '');
    if (m) { return MESES[parseInt(m[2], 10) - 1] + '/' + m[1].slice(2); }
    return texto;
  }

  function cortar(texto, max) {
    return texto.length > max ? texto.slice(0, Math.max(1, max - 1)) + '…' : texto;
  }

  function isoDe(d) {
    function dois(n) { return (n < 10 ? '0' : '') + n; }
    return d.getUTCFullYear() + '-' + dois(d.getUTCMonth() + 1) + '-' + dois(d.getUTCDate());
  }

  function parteTexto(v, total) {
    var p = total > 0 ? v / total * 100 : 0;
    return (p > 0 && p < 1 ? '<1' : fmtInt.format(Math.round(p))) + '%';
  }

  /** Curva suave que não passa de um ponto ao outro além dos valores (monótona: nunca fica abaixo de zero). */
  function curva(p) {
    var n = p.length;
    if (n === 0) { return ''; }
    if (n === 1) { return 'M' + p[0].x + ',' + p[0].y; }
    var dx = [], m = [], t = [];
    var i;
    for (i = 0; i < n - 1; i++) {
      dx[i] = p[i + 1].x - p[i].x;
      m[i] = (p[i + 1].y - p[i].y) / (dx[i] || 1);
    }
    t[0] = m[0];
    t[n - 1] = m[n - 2];
    for (i = 1; i < n - 1; i++) { t[i] = m[i - 1] * m[i] <= 0 ? 0 : (m[i - 1] + m[i]) / 2; }
    for (i = 0; i < n - 1; i++) {
      if (m[i] === 0) { t[i] = 0; t[i + 1] = 0; continue; }
      var a = t[i] / m[i], b = t[i + 1] / m[i], s = a * a + b * b;
      if (s > 9) { var k = 3 / Math.sqrt(s); t[i] = k * a * m[i]; t[i + 1] = k * b * m[i]; }
    }
    var d = 'M' + p[0].x + ',' + p[0].y;
    for (i = 0; i < n - 1; i++) {
      var h = dx[i] / 3;
      d += 'C' + (p[i].x + h) + ',' + (p[i].y + t[i] * h) + ' ' + (p[i + 1].x - h) + ',' + (p[i + 1].y - t[i + 1] * h)
        + ' ' + p[i + 1].x + ',' + p[i + 1].y;
    }
    return d;
  }

  /* ---------- Leitura da tabela ---------- */
  function lerTabela(fig) {
    var tabela = document.getElementById(fig.getAttribute('data-tabela'));
    if (!tabela) { return null; }
    var cabecalho = Array.prototype.map.call(tabela.querySelectorAll('thead th'), function (th) {
      return th.textContent.trim();
    });
    var colunas = (fig.getAttribute('data-colunas') || fig.getAttribute('data-coluna') || '1')
      .split(',').map(function (s) { return parseInt(s, 10); });
    var colunaClasse = fig.hasAttribute('data-classe') ? parseInt(fig.getAttribute('data-classe'), 10) : -1;
    var linhas = [];
    Array.prototype.forEach.call(tabela.querySelectorAll('tbody tr'), function (tr) {
      var celulas = tr.children;
      var texto = celulas[0].textContent.trim();
      var extras = [];
      for (var i = 1; i < celulas.length; i++) {
        if (colunas.indexOf(i) === -1 && i !== colunaClasse) {
          extras.push(cabecalho[i] + ': ' + celulas[i].textContent.trim().replace(/\s+/g, ' '));
        }
      }
      linhas.push({
        nome: texto,
        cru: celulas[0].getAttribute('data-r'),
        curto: rotuloCurto(celulas[0].getAttribute('data-r'), texto),
        valores: colunas.map(function (c) { return num(celulas[c].getAttribute('data-v')); }),
        classe: colunaClasse >= 0 ? celulas[colunaClasse].getAttribute('data-c') : null,
        alerta: tr.hasAttribute('data-alerta'),
        extras: extras
      });
    });
    if (fig.getAttribute('data-ordenar') === 'desc') {
      linhas.sort(function (a, b) { return b.valores[0] - a.valores[0]; });
    }
    if (fig.hasAttribute('data-dias')) { linhas = preencherDias(linhas, parseInt(fig.getAttribute('data-dias'), 10)); }
    return {
      linhas: linhas,
      series: colunas.map(function (c) { return cabecalho[c]; })
    };
  }

  /** Dias sem saída viram zero, para a linha mostrar o ritmo de verdade (e não ligar dois dias distantes). */
  function preencherDias(linhas, dias) {
    var porDia = {};
    linhas.forEach(function (l) { if (l.cru) { porDia[l.cru] = l; } });
    var hoje = new Date();
    var fim = Date.UTC(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
    var ultimo = linhas.length ? linhas[linhas.length - 1].cru : null;
    if (ultimo && /^\d{4}-\d{2}-\d{2}$/.test(ultimo)) {
      var u = Date.UTC(+ultimo.slice(0, 4), +ultimo.slice(5, 7) - 1, +ultimo.slice(8, 10));
      if (u > fim) { fim = u; }
    }
    var saida = [];
    for (var k = dias - 1; k >= 0; k--) {
      var iso = isoDe(new Date(fim - k * 86400000));
      saida.push(porDia[iso] || {
        nome: iso.slice(8, 10) + '/' + iso.slice(5, 7) + '/' + iso.slice(0, 4), cru: iso,
        curto: iso.slice(8, 10) + '/' + iso.slice(5, 7), valores: [0], classe: null, alerta: false, extras: []
      });
    }
    return saida;
  }

  /* ---------- Dica (tooltip em vidro) ---------- */
  function criarDica(fig) {
    var dica = htmlEl('div', 'an-dica');
    dica.hidden = true;
    dica.setAttribute('aria-hidden', 'true'); // a dica só repete o que está na tabela e no rótulo de cada marca
    fig.appendChild(dica);
    return dica;
  }

  function montarDica(dica, titulo, linhas, extras) {
    dica.textContent = '';
    dica.appendChild(htmlEl('p', 'an-dica__titulo', titulo));
    linhas.forEach(function (l) {
      var p = htmlEl('p', 'an-dica__linha');
      var chave = htmlEl('span', 'an-dica__chave');
      chave.style.background = l.cor;
      p.appendChild(chave);
      p.appendChild(htmlEl('span', 'an-dica__valor', l.valor));
      p.appendChild(htmlEl('span', 'an-dica__rotulo', l.rotulo));
      dica.appendChild(p);
    });
    (extras || []).forEach(function (e) { dica.appendChild(htmlEl('p', 'an-dica__extra', e)); });
  }

  function posicionarDica(fig, dica, x, y) {
    dica.hidden = false;
    var largura = fig.clientWidth;
    var w = dica.offsetWidth;
    var h = dica.offsetHeight;
    var esquerda = Math.max(0, Math.min(x + 14, largura - w));
    if (x + 14 + w > largura) { esquerda = Math.max(0, x - 14 - w); }
    var topo = y - h - 12;
    if (topo < 0) { topo = y + 18; }
    dica.style.left = esquerda + 'px';
    dica.style.top = topo + 'px';
  }

  function esconderDica(fig, dica) {
    dica.hidden = true;
    fig.classList.remove('an-ativo');
    Array.prototype.forEach.call(fig.querySelectorAll('.an-sel'), function (e) { e.classList.remove('an-sel'); });
  }

  /**
   * Liga a dica aos alvos [data-i] do svg (barras). `mostrar(i)` devolve {titulo, linhas, extras, marcas}.
   * Ponteiro e foco por teclado mostram o mesmo conteúdo.
   */
  function ligarDica(fig, svg, mostrar) {
    var dica = criarDica(fig);
    function abrir(alvo, x, y) {
      var info = mostrar(parseInt(alvo.getAttribute('data-i'), 10));
      montarDica(dica, info.titulo, info.linhas, info.extras);
      fig.classList.add('an-ativo');
      Array.prototype.forEach.call(fig.querySelectorAll('.an-sel'), function (e) { e.classList.remove('an-sel'); });
      info.marcas.forEach(function (m) { m.classList.add('an-sel'); });
      posicionarDica(fig, dica, x, y);
    }
    function alvoDe(e) {
      return e.target.closest ? e.target.closest('[data-i]') : null;
    }
    svg.addEventListener('pointermove', function (e) {
      var alvo = alvoDe(e);
      if (!alvo) { esconderDica(fig, dica); return; }
      var caixa = fig.getBoundingClientRect();
      abrir(alvo, e.clientX - caixa.left, e.clientY - caixa.top);
    });
    svg.addEventListener('pointerleave', function () { esconderDica(fig, dica); });
    svg.addEventListener('focusin', function (e) {
      var alvo = alvoDe(e);
      if (!alvo) { return; }
      var a = alvo.getBoundingClientRect();
      var caixa = fig.getBoundingClientRect();
      abrir(alvo, a.left - caixa.left + a.width / 2, a.top - caixa.top);
    });
    svg.addEventListener('focusout', function () { esconderDica(fig, dica); });
    svg.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { esconderDica(fig, dica); }
    });
  }

  function criarSvg(fig, largura, altura) {
    var svg = svgEl('svg', {
      viewBox: '0 0 ' + largura + ' ' + altura,
      focusable: 'false'
    });
    svg.setAttribute('role', 'group');
    svg.setAttribute('aria-label', (fig.getAttribute('data-rotulo-acessivel') || 'Gráfico')
      + '. Os mesmos dados estão na tabela abaixo.');
    if (!fig._anAnimou) { svg.setAttribute('class', 'an-anim'); }
    return svg;
  }

  /** Alvo transparente maior que a marca (também recebe foco quando são poucos). */
  function alvo(svg, i, x, y, w, h, rotulo, focavel) {
    var r = svgEl('rect', { x: x, y: y, width: w, height: h, 'class': 'an-alvo', 'data-i': i });
    r.setAttribute('role', 'img');
    r.setAttribute('aria-label', rotulo);
    if (focavel) { r.setAttribute('tabindex', '0'); }
    svg.appendChild(r);
    return r;
  }

  /** Barra com a ponta de dados arredondada (4 px) e a base reta. dir: 'h' (cresce para a direita) ou 'v' (para cima). */
  function caminhoBarra(x, y, w, h, dir) {
    var r = Math.min(4, (dir === 'h' ? h : w) / 2, dir === 'h' ? w : h);
    if (r < 1) { return 'M' + x + ',' + y + 'h' + w + 'v' + h + 'h' + (-w) + 'Z'; }
    if (dir === 'h') {
      return 'M' + x + ',' + y + 'H' + (x + w - r) + 'A' + r + ',' + r + ' 0 0 1 ' + (x + w) + ',' + (y + r)
        + 'V' + (y + h - r) + 'A' + r + ',' + r + ' 0 0 1 ' + (x + w - r) + ',' + (y + h) + 'H' + x + 'Z';
    }
    return 'M' + x + ',' + (y + h) + 'V' + (y + r) + 'A' + r + ',' + r + ' 0 0 1 ' + (x + r) + ',' + y
      + 'H' + (x + w - r) + 'A' + r + ',' + r + ' 0 0 1 ' + (x + w) + ',' + (y + r) + 'V' + (y + h) + 'Z';
  }

  function atraso(el, i, passo) {
    el.style.animationDelay = Math.min(i * passo, 600) + 'ms';
  }

  /* ---------- Barras horizontais (ranking) ---------- */
  /** opcoes.parte: soma para mostrar a parte de cada uma; opcoes.ranking: classes de cor por posição (composição). */
  function desenharBarras(fig, area, dados, largura, opcoes) {
    opcoes = opcoes || {};
    var formato = fig.getAttribute('data-formato');
    var limite = fig.hasAttribute('data-limite') ? num(fig.getAttribute('data-limite')) : null;
    var teto = fig.hasAttribute('data-maximo') ? num(fig.getAttribute('data-maximo')) : 0;
    var maior = Math.max.apply(null, dados.linhas.map(function (l) { return l.valores[0]; }).concat([0]));
    var escala = teto > 0 ? teto : (maior > 0 ? maior : 1);
    var alturaLinha = 46;
    var rodape = limite != null ? 22 : 0;
    var altura = dados.linhas.length * alturaLinha + rodape;
    var svg = criarSvg(fig, largura, altura);
    var focavel = dados.linhas.length <= 24;
    var nome = dados.series[0];

    function classeCor(linha, i) {
      if (linha.alerta) { return 'an-m-alerta'; }
      if (opcoes.ranking) { return 'an-r' + (i < 5 ? i + 1 : 0); }
      return 'an-m1';
    }
    function textoValor(linha, v) {
      var t = completo(v, formato);
      if (opcoes.parte) { t += ' · ' + parteTexto(v, opcoes.parte); }
      return linha.alerta ? 'Acaba em breve · ' + t : t;
    }

    var marcas = [];
    dados.linhas.forEach(function (linha, i) {
      var y = i * alturaLinha;
      var v = linha.valores[0];
      var rotuloValor = textoValor(linha, v);
      var larguraValor = rotuloValor.length * 7.4 + 4;
      svg.appendChild(svgEl('text', {
        x: 0, y: y + 15, 'class': 'an-t an-t--nome'
      }, cortar(linha.nome, Math.max(8, Math.floor((largura - larguraValor - 12) / 7))))).appendChild(
        svgEl('title', null, linha.nome));
      svg.appendChild(svgEl('text', {
        x: largura, y: y + 15, 'text-anchor': 'end',
        'class': 'an-t an-t--valor' + (linha.alerta ? ' an-t--alerta' : '')
      }, rotuloValor));
      var w = v > 0 ? Math.max(3, Math.min(largura, v / escala * largura)) : 0;
      var marca = null;
      if (w > 0) {
        marca = svgEl('path', {
          d: caminhoBarra(0, y + 24, w, 9, 'h'),
          'class': 'an-marca an-cresce-h ' + classeCor(linha, i)
        });
        atraso(marca, i, 50);
        svg.appendChild(marca);
      }
      marcas.push(marca);
    });
    svg.appendChild(svgEl('line', { x1: 0.5, x2: 0.5, y1: 20, y2: dados.linhas.length * alturaLinha - 6, 'class': 'an-base' }));
    if (limite != null) {
      var xl = Math.min(largura - 1, limite / escala * largura);
      // A linha só atravessa a zona das barras (não passa por cima dos nomes)
      dados.linhas.forEach(function (linha, i) {
        svg.appendChild(svgEl('line', {
          x1: xl, x2: xl, y1: i * alturaLinha + 20, y2: i * alturaLinha + 37, 'class': 'an-limite'
        }));
      });
      var ancora = xl > largura - 70 ? 'end' : 'start';
      svg.appendChild(svgEl('text', {
        x: ancora === 'end' ? xl - 6 : xl + 6, y: dados.linhas.length * alturaLinha + 14, 'text-anchor': ancora,
        'class': 'an-t'
      }, limite + ' dias'));
    }
    dados.linhas.forEach(function (linha, i) {
      var partes = [linha.nome + ': ' + textoValor(linha, linha.valores[0]).replace(' · ', ', ')];
      alvo(svg, i, 0, i * alturaLinha, largura, alturaLinha, partes.concat(linha.extras).join('. '), focavel);
    });
    ligarDica(fig, svg, function (i) {
      var linha = dados.linhas[i];
      var cor = linha.alerta ? 'var(--an-alerta)' : (opcoes.ranking ? 'var(--an-r' + (i < 5 ? i + 1 : 0) + ')' : 'var(--an-s1)');
      var extras = (linha.alerta ? ['Acaba em breve'] : []).concat(linha.extras);
      if (opcoes.parte) { extras.unshift(parteTexto(linha.valores[0], opcoes.parte) + ' do total'); }
      return {
        titulo: linha.nome,
        linhas: [{ cor: cor, valor: completo(linha.valores[0], formato), rotulo: nome }],
        extras: extras,
        marcas: marcas[i] ? [marcas[i]] : []
      };
    });
    area.appendChild(svg);
  }

  /* ---------- Composição: faixa 100% + ranking com a parte de cada uma ---------- */
  function desenharComposicao(fig, area, dados, largura) {
    var linhas = dados.linhas.filter(function (l) { return l.valores[0] > 0; });
    var total = linhas.reduce(function (a, l) { return a + l.valores[0]; }, 0);
    if (total <= 0) { return desenharBarras(fig, area, dados, largura); }
    var formato = fig.getAttribute('data-formato');

    var altura = 16;
    var faixa = svgEl('svg', { viewBox: '0 0 ' + largura + ' ' + altura, focusable: 'false', 'class': 'an-faixa' + (fig._anAnimou ? '' : ' an-anim') });
    faixa.setAttribute('aria-hidden', 'true');
    var grupos = linhas.slice(0, 5).map(function (l, i) { return { nome: l.nome, v: l.valores[0], cls: 'an-r' + (i + 1) }; });
    var resto = linhas.slice(5);
    if (resto.length) {
      grupos.push({
        nome: resto.length === 1 ? resto[0].nome : 'Outras (' + resto.length + ')',
        v: resto.reduce(function (a, l) { return a + l.valores[0]; }, 0), cls: 'an-r0'
      });
    }
    var gap = 2;
    var util = largura - gap * (grupos.length - 1);
    var x = 0;
    grupos.forEach(function (g, i) {
      var w = Math.max(3, g.v / total * util);
      var r = svgEl('rect', { x: x, y: 0, width: w, height: altura, rx: 4, 'class': 'an-marca an-surge ' + g.cls });
      atraso(r, i, 70);
      r.appendChild(svgEl('title', null, g.nome + ': ' + completo(g.v, formato) + ' (' + parteTexto(g.v, total) + ')'));
      faixa.appendChild(r);
      x += w + gap;
    });
    area.appendChild(faixa);
    var espaco = htmlEl('div', 'an-faixa-espaco');
    area.appendChild(espaco);
    var bloco = htmlEl('div');
    area.appendChild(bloco);
    desenharBarras(fig, bloco, { linhas: linhas, series: dados.series }, largura, { parte: total, ranking: true });
  }

  /* ---------- Varredura por teclado e ponteiro (gráficos no tempo) ---------- */
  /**
   * Um único alvo (rect) cobre o gráfico: o ponteiro escolhe o ponto mais próximo no eixo X; no teclado, setas,
   * Home, End e Esc. `ativar(i)` desenha o destaque e devolve o texto para leitor de tela.
   */
  function varrer(fig, svg, sobre, n, xDe, ativar, ocultar) {
    var atual = -1;
    function ir(i) {
      atual = Math.max(0, Math.min(n - 1, i));
      sobre.setAttribute('aria-label', ativar(atual));
    }
    function sair() { atual = -1; ocultar(); }
    sobre.addEventListener('pointermove', function (e) {
      var caixa = svg.getBoundingClientRect();
      var escala = caixa.width ? svg.viewBox.baseVal.width / caixa.width : 1;
      var px = (e.clientX - caixa.left) * escala;
      var melhor = 0, dist = Infinity;
      for (var i = 0; i < n; i++) {
        var d = Math.abs(xDe(i) - px);
        if (d < dist) { dist = d; melhor = i; }
      }
      ir(melhor);
    });
    sobre.addEventListener('pointerleave', function () {
      if (document.activeElement !== sobre) { sair(); }
    });
    sobre.addEventListener('focus', function () { ir(atual < 0 ? n - 1 : atual); });
    sobre.addEventListener('blur', sair);
    sobre.addEventListener('keydown', function (e) {
      var k = e.key;
      if (k === 'ArrowRight' || k === 'ArrowUp') { ir(atual < 0 ? n - 1 : atual + 1); }
      else if (k === 'ArrowLeft' || k === 'ArrowDown') { ir(atual < 0 ? n - 1 : atual - 1); }
      else if (k === 'Home') { ir(0); }
      else if (k === 'End') { ir(n - 1); }
      else if (k === 'Escape') { sair(); return; }
      else { return; }
      e.preventDefault();
    });
  }

  /** Grade, números do eixo Y e retorna a função de escala. */
  function grade(svg, m, largura, internoA, ex, formato) {
    ex.marcas.forEach(function (v) {
      var y = m.t + internoA - v / ex.topo * internoA;
      svg.appendChild(svgEl('line', {
        x1: m.e, x2: largura - m.d, y1: y, y2: y, 'class': v === 0 ? 'an-base' : 'an-grade'
      }));
      svg.appendChild(svgEl('text', {
        x: m.e - 8, y: y + 4, 'text-anchor': 'end', 'class': 'an-t'
      }, compacto(v, formato)));
    });
  }

  function rotulosX(svg, dados, xDe, largura, altura) {
    var n = dados.linhas.length;
    var slot = n > 1 ? (xDe(n - 1) - xDe(0)) / (n - 1) : 60;
    var passo = Math.max(1, Math.ceil(54 / slot));
    // o último rótulo sempre entra; o anterior sai se encostar nele
    for (var i = n - 1; i >= 0; i -= passo) {
      svg.appendChild(svgEl('text', {
        x: xDe(i), y: altura - 6, 'text-anchor': i === 0 ? 'start' : (i === n - 1 ? 'end' : 'middle'), 'class': 'an-t'
      }, dados.linhas[i].curto));
    }
  }

  /* ---------- Linha (uma série) e linhas (duas séries sobrepostas) ---------- */
  function desenharLinhas(fig, area, dados, largura) {
    var formato = fig.getAttribute('data-formato');
    var nSeries = dados.series.length;
    var n = dados.linhas.length;
    var altura = largura < 480 ? 210 : 260;
    var margem = { e: formato === 'moeda' ? 62 : 40, d: 14, t: 18, b: 26 };
    var internoL = Math.max(10, largura - margem.e - margem.d);
    var internoA = altura - margem.t - margem.b;
    var maior = 0;
    dados.linhas.forEach(function (l) { l.valores.forEach(function (v) { if (v > maior) { maior = v; } }); });
    var ex = eixo(maior, formato === 'inteiro');
    var svg = criarSvg(fig, largura, altura);
    var id = 'an-g' + (++contador);

    function xDe(i) { return n === 1 ? margem.e + internoL / 2 : margem.e + i * internoL / (n - 1); }
    function yDe(v) { return margem.t + internoA - v / ex.topo * internoA; }

    if (nSeries === 1) {
      var defs = svgEl('defs');
      var gr = svgEl('linearGradient', { id: id, x1: 0, x2: 0, y1: 0, y2: 1 });
      gr.appendChild(svgEl('stop', { offset: '0%', 'class': 'an-gr-topo' }));
      gr.appendChild(svgEl('stop', { offset: '100%', 'class': 'an-gr-base' }));
      defs.appendChild(gr);
      svg.appendChild(defs);
    }
    grade(svg, margem, largura, internoA, ex, formato);
    rotulosX(svg, dados, xDe, largura, altura);

    var pontos = [];
    for (var s = 0; s < nSeries; s++) {
      pontos.push(dados.linhas.map(function (l, i) { return { x: xDe(i), y: yDe(l.valores[s]) }; }));
    }
    if (nSeries === 1 && n > 1) {
      var base = yDe(0);
      var p0 = pontos[0];
      var areaPath = svgEl('path', {
        d: curva(p0) + 'L' + p0[n - 1].x + ',' + base + 'L' + p0[0].x + ',' + base + 'Z',
        fill: 'url(#' + id + ')', 'class': 'an-area an-surge'
      });
      svg.appendChild(areaPath);
    }
    pontos.forEach(function (p, s) {
      if (n === 1) { return; }
      var linha = svgEl('path', { d: curva(p), pathLength: 1, 'class': 'an-linha an-desenha an-l' + (s + 1) });
      linha.style.animationDelay = (s * 150) + 'ms';
      svg.appendChild(linha);
    });

    // Marcadores fixos só quando são poucos pontos (senão viram poeira)
    if (n <= 14) {
      pontos.forEach(function (p, s) {
        p.forEach(function (pt) {
          svg.appendChild(svgEl('circle', { cx: pt.x, cy: pt.y, r: 4, 'class': 'an-ponto an-surge an-p' + (s + 1) }));
        });
      });
    }

    // Rótulo direto: o pico (uma série) ou o nome de cada série no fim da linha
    if (nSeries === 1) {
      var imax = 0;
      dados.linhas.forEach(function (l, i) { if (l.valores[0] > dados.linhas[imax].valores[0]) { imax = i; } });
      if (dados.linhas[imax].valores[0] > 0) {
        var pk = pontos[0][imax];
        svg.appendChild(svgEl('circle', { cx: pk.x, cy: pk.y, r: 4, 'class': 'an-ponto an-surge an-p1' }));
        var tx = compacto(dados.linhas[imax].valores[0], formato) + (fig.getAttribute('data-unidade') ? ' ' + fig.getAttribute('data-unidade') : '') + ' · ' + dados.linhas[imax].curto;
        var ancora = pk.x < margem.e + 70 ? 'start' : (pk.x > largura - 90 ? 'end' : 'middle');
        svg.appendChild(svgEl('text', {
          x: pk.x, y: Math.max(11, pk.y - 10), 'text-anchor': ancora, 'class': 'an-t an-t--pico an-surge'
        }, 'Pico: ' + tx));
      }
    } else {
      var fim = pontos.map(function (p) { return p[n - 1]; });
      var acima = fim[0].y <= fim[1].y ? 0 : 1;
      dados.series.forEach(function (nome, s) {
        var pt = fim[s];
        var sobe = s === acima;
        var rot = svgEl('text', {
          x: Math.min(pt.x, largura - 2), y: sobe ? pt.y - 11 : pt.y + 20, 'text-anchor': 'end', 'class': 'an-t an-t--serie an-surge'
        }, nome);
        svg.appendChild(rot);
      });
    }

    // Destaque ativo: guia vertical + um círculo por série
    var guia = svgEl('line', { y1: margem.t, y2: margem.t + internoA, 'class': 'an-guia', visibility: 'hidden' });
    svg.appendChild(guia);
    var ativos = dados.series.map(function (nome, s) {
      var c = svgEl('circle', { r: 6, 'class': 'an-ponto-ativo an-p' + (s + 1), visibility: 'hidden' });
      svg.appendChild(c);
      return c;
    });
    var sobre = svgEl('rect', {
      x: margem.e - 6, y: margem.t - 6, width: internoL + 12, height: internoA + margem.b + 6,
      'class': 'an-alvo an-alvo--varre', tabindex: 0, role: 'img'
    });
    sobre.setAttribute('aria-label', (fig.getAttribute('data-rotulo-acessivel') || 'Gráfico')
      + '. Use as setas para percorrer os pontos.');
    svg.appendChild(sobre);

    var dica = criarDica(fig);
    function ativar(i) {
      var l = dados.linhas[i];
      var cx = xDe(i);
      guia.setAttribute('x1', cx);
      guia.setAttribute('x2', cx);
      guia.setAttribute('visibility', 'visible');
      var maisAlto = Infinity;
      ativos.forEach(function (c, s) {
        c.setAttribute('cx', cx);
        c.setAttribute('cy', yDe(l.valores[s]));
        c.setAttribute('visibility', 'visible');
        maisAlto = Math.min(maisAlto, yDe(l.valores[s]));
      });
      montarDica(dica, l.nome, l.valores.map(function (v, s) {
        return { cor: 'var(--an-s' + (s + 1) + ')', valor: completo(v, formato) + (fig.getAttribute('data-unidade') ? ' ' + fig.getAttribute('data-unidade') : ''), rotulo: nSeries > 1 ? dados.series[s] : '' };
      }), l.extras);
      var cs = svg.getBoundingClientRect();
      var cf = fig.getBoundingClientRect();
      fig.classList.add('an-ativo');
      posicionarDica(fig, dica, cs.left - cf.left + cx, cs.top - cf.top + maisAlto);
      return l.nome + '. ' + l.valores.map(function (v, s) { return dados.series[s] + ': ' + completo(v, formato); }).join('. ')
        + (l.extras.length ? '. ' + l.extras.join('. ') : '');
    }
    function ocultar() {
      guia.setAttribute('visibility', 'hidden');
      ativos.forEach(function (c) { c.setAttribute('visibility', 'hidden'); });
      esconderDica(fig, dica);
    }
    varrer(fig, svg, sobre, n, xDe, ativar, ocultar);

    if (nSeries > 1) {
      var legenda = htmlEl('ul', 'an-leg');
      dados.series.forEach(function (nome, s) {
        var li = htmlEl('li');
        li.appendChild(htmlEl('span', 'an-leg__cor an-leg__cor--' + (s + 1)));
        li.appendChild(htmlEl('span', null, nome));
        legenda.appendChild(li);
      });
      area.appendChild(legenda);
    }
    area.appendChild(svg);
  }

  /* ---------- Pareto (curva ABC): barras da parte de cada produto + linha da soma acumulada ---------- */
  function desenharPareto(fig, area, dados, largura) {
    var n = dados.linhas.length;
    var altura = largura < 480 ? 250 : 300;
    var margem = { e: 42, d: 10, t: 16, b: 40 };
    var internoL = Math.max(10, largura - margem.e - margem.d);
    var internoA = altura - margem.t - margem.b;
    var ex = { topo: 100, marcas: [0, 25, 50, 75, 100] };
    var svg = criarSvg(fig, largura, altura);
    var slot = internoL / n;
    function xDe(i) { return margem.e + i * slot + slot / 2; }
    function yDe(v) { return margem.t + internoA - Math.min(v, 100) / 100 * internoA; }

    grade(svg, margem, largura, internoA, ex, 'pct');

    // Linhas de referência: 80% (fim da classe A) e 95% (fim da B)
    [80, 95].forEach(function (r) {
      svg.appendChild(svgEl('line', { x1: margem.e, x2: largura - margem.d, y1: yDe(r), y2: yDe(r), 'class': 'an-limite an-limite--ref' }));
    });
    svg.appendChild(svgEl('text', { x: margem.e + 6, y: yDe(80) - 5, 'class': 'an-t an-t--ref' }, '80%: fim da classe A'));
    svg.appendChild(svgEl('text', { x: margem.e + 6, y: yDe(95) - 5, 'class': 'an-t an-t--ref' }, '95%: fim da classe B'));

    var bw = Math.max(1, Math.min(28, slot - (slot >= 4 ? 2 : 0)));
    var barras = [];
    dados.linhas.forEach(function (l, i) {
      var h = Math.max(1, l.valores[0] / 100 * internoA);
      var x = xDe(i) - bw / 2;
      var y = margem.t + internoA - h;
      var b = bw >= 6 ? svgEl('path', { d: caminhoBarra(x, y, bw, h, 'v') }) : svgEl('rect', { x: x, y: y, width: bw, height: h });
      b.setAttribute('class', 'an-marca an-cresce-v an-m' + (l.classe || 'C'));
      atraso(b, i, Math.max(4, 500 / n));
      svg.appendChild(b);
      barras.push(b);
    });
    var pts = dados.linhas.map(function (l, i) { return { x: xDe(i), y: yDe(l.valores[1]) }; });
    var linha = svgEl('path', { d: curva(pts), pathLength: 1, 'class': 'an-linha an-desenha an-l2' });
    linha.style.animationDelay = '200ms';
    svg.appendChild(linha);

    // Eixo X: posição do produto (1 é o que mais pesa)
    var passo = Math.max(1, Math.ceil(30 / slot));
    var marcas = [];
    for (var i = 0; i < n; i += passo) { marcas.push(i); }
    if (marcas[marcas.length - 1] !== n - 1 && n - 1 - marcas[marcas.length - 1] >= passo * 0.6) { marcas.push(n - 1); }
    marcas.forEach(function (i) {
      svg.appendChild(svgEl('text', { x: xDe(i), y: altura - 22, 'text-anchor': 'middle', 'class': 'an-t' }, String(i + 1)));
    });
    svg.appendChild(svgEl('text', {
      x: margem.e + internoL / 2, y: altura - 5, 'text-anchor': 'middle', 'class': 'an-t an-t--ref'
    }, 'Produtos, do que mais pesa ao que menos pesa'));

    var guia = svgEl('line', { y1: margem.t, y2: margem.t + internoA, 'class': 'an-guia', visibility: 'hidden' });
    svg.appendChild(guia);
    var ponto = svgEl('circle', { r: 6, 'class': 'an-ponto-ativo an-p2', visibility: 'hidden' });
    svg.appendChild(ponto);
    var sobre = svgEl('rect', {
      x: margem.e, y: margem.t - 6, width: internoL, height: internoA + 6,
      'class': 'an-alvo an-alvo--varre', tabindex: 0, role: 'img'
    });
    sobre.setAttribute('aria-label', 'Parte de cada produto no valor que saiu e soma acumulada. Use as setas para percorrer os produtos.');
    svg.appendChild(sobre);

    var dica = criarDica(fig);
    function ativar(i) {
      var l = dados.linhas[i];
      guia.setAttribute('x1', xDe(i)); guia.setAttribute('x2', xDe(i)); guia.setAttribute('visibility', 'visible');
      ponto.setAttribute('cx', xDe(i)); ponto.setAttribute('cy', yDe(l.valores[1])); ponto.setAttribute('visibility', 'visible');
      Array.prototype.forEach.call(svg.querySelectorAll('.an-sel'), function (e) { e.classList.remove('an-sel'); });
      barras[i].classList.add('an-sel');
      fig.classList.add('an-ativo');
      var cl = (l.classe || 'C');
      montarDica(dica, (i + 1) + '. ' + l.nome, [
        { cor: 'var(--an-' + cl.toLowerCase() + ')', valor: completo(l.valores[0], 'pct'), rotulo: 'do valor que saiu' },
        { cor: 'var(--an-s2)', valor: completo(l.valores[1], 'pct'), rotulo: 'soma até aqui' }
      ], ['Classe ' + cl].concat(l.extras));
      var cs = svg.getBoundingClientRect();
      var cf = fig.getBoundingClientRect();
      posicionarDica(fig, dica, cs.left - cf.left + xDe(i), cs.top - cf.top + yDe(l.valores[1]));
      return (i + 1) + '. ' + l.nome + '. Classe ' + cl + '. ' + completo(l.valores[0], 'pct') + ' do valor, soma até aqui '
        + completo(l.valores[1], 'pct') + '. ' + l.extras.join('. ');
    }
    function ocultar() {
      guia.setAttribute('visibility', 'hidden');
      ponto.setAttribute('visibility', 'hidden');
      esconderDica(fig, dica);
    }
    varrer(fig, svg, sobre, n, xDe, ativar, ocultar);
    area.appendChild(svg);

    // Legenda: as três classes (com contagem) e a linha da soma
    var total = { A: { n: 0, pct: 0 }, B: { n: 0, pct: 0 }, C: { n: 0, pct: 0 } };
    dados.linhas.forEach(function (l) { if (total[l.classe]) { total[l.classe].n += 1; total[l.classe].pct += l.valores[0]; } });
    var legenda = htmlEl('ul', 'an-leg an-leg--abaixo');
    ['A', 'B', 'C'].forEach(function (c) {
      if (!total[c].n) { return; }
      var li = htmlEl('li');
      li.appendChild(htmlEl('span', 'an-leg__cor an-leg__cor--' + c));
      li.appendChild(htmlEl('span', null, 'Classe ' + c + ': ' + fmtInt.format(total[c].n)
        + (total[c].n === 1 ? ' produto, ' : ' produtos, ') + fmtInt.format(Math.round(total[c].pct)) + '% do valor'));
      legenda.appendChild(li);
    });
    var lc = htmlEl('li');
    lc.appendChild(htmlEl('span', 'an-leg__cor an-leg__cor--linha'));
    lc.appendChild(htmlEl('span', null, 'Linha: soma acumulada'));
    legenda.appendChild(lc);
    area.appendChild(legenda);
  }

  /* ---------- Montagem ---------- */
  var DESENHOS = {
    barras: desenharBarras, composicao: desenharComposicao,
    linha: desenharLinhas, linhas: desenharLinhas, pareto: desenharPareto
  };

  function desenhar(fig) {
    var area = fig.querySelector('.an-fig__area');
    var dados = fig._anDados;
    var largura = Math.floor(area.clientWidth);
    var desenho = DESENHOS[fig.getAttribute('data-grafico')];
    if (!desenho || !dados || largura < 40) { return false; }
    var dica = fig.querySelector('.an-dica');
    if (dica) { dica.remove(); }
    fig.classList.remove('an-ativo');
    area.textContent = '';
    desenho(fig, area, dados, largura);
    fig._anAnimou = true; // redesenhar ao mudar a largura não repete a animação
    fig._anLargura = largura;
    return true;
  }

  function iniciar() {
    var figuras = document.querySelectorAll('.an-fig[data-grafico]');
    Array.prototype.forEach.call(figuras, function (fig) {
      var dados = lerTabela(fig);
      if (!dados || dados.linhas.length === 0) { return; }
      fig._anDados = dados;
      if (!desenhar(fig)) { return; }
      // O gráfico entrou: a tabela vira opcional (continua a um clique, e lida por leitor de tela)
      var detalhes = document.getElementById(fig.getAttribute('data-tabela'));
      detalhes = detalhes && detalhes.closest('details');
      if (detalhes) { detalhes.open = false; }
      if (window.ResizeObserver) {
        var agendado = false;
        new ResizeObserver(function () {
          var largura = Math.floor(fig.querySelector('.an-fig__area').clientWidth);
          if (agendado || largura === fig._anLargura) { return; }
          agendado = true;
          window.requestAnimationFrame(function () { agendado = false; desenhar(fig); });
        }).observe(fig.querySelector('.an-fig__area'));
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
  } else {
    iniciar();
  }
})();
