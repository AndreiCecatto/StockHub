/*
 * StockHub: gráficos da tela /analises. SVG próprio, sem biblioteca.
 *
 * Cada <figure data-grafico="..." data-tabela="id"> é desenhada a partir de uma TABELA da própria página (a
 * alternativa em texto para leitor de tela e para quem está sem JavaScript). Os números crus ficam em data-v nas
 * células; o texto da célula é o formatado em pt-BR e só aparece na dica. O rótulo de cada linha é a primeira
 * célula (data-r guarda o valor cru: aaaa-mm-dd ou aaaa-mm).
 *
 * Tipos: barras (horizontais, um rótulo por linha), colunas (uma série ao longo do tempo), duplas (duas séries por
 * mês, com legenda) e pilha (uma barra dividida nas classes A, B e C, com legenda).
 * Atributos: data-formato (moeda | inteiro | dias), data-coluna (índice da célula do valor), data-colunas (duas, nas
 * duplas), data-classe (índice da célula da classe, na pilha), data-limite e data-maximo (barras de cobertura),
 * data-rotulo-acessivel.
 *
 * Cores e temas vêm do CSS (classes an-*): trocar o tema não exige redesenhar. Os textos vindos da tabela entram
 * sempre por textContent. Sem animação.
 */
(function () {
  'use strict';

  var NS = 'http://www.w3.org/2000/svg';
  var MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
  var fmtMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  var fmtInt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
  var fmtDec1 = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  var fmtCompacto = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });

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
    return fmtInt.format(n);
  }

  /** Valor curto para os números do eixo: 1,5 mil, 2 mi. */
  function compacto(n, formato) {
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
        curto: rotuloCurto(celulas[0].getAttribute('data-r'), texto),
        valores: colunas.map(function (c) { return num(celulas[c].getAttribute('data-v')); }),
        classe: colunaClasse >= 0 ? celulas[colunaClasse].getAttribute('data-c') : null,
        alerta: tr.hasAttribute('data-alerta'),
        extras: extras
      });
    });
    return {
      linhas: linhas,
      series: colunas.map(function (c) { return cabecalho[c]; })
    };
  }

  /* ---------- Dica (tooltip) ---------- */
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
   * Liga a dica aos alvos [data-i] do svg. `mostrar(i)` devolve {titulo, linhas, extras, marcas: [elementos]}.
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

  /* ---------- Barras horizontais ---------- */
  function desenharBarras(fig, area, dados, largura) {
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

    var marcas = [];
    dados.linhas.forEach(function (linha, i) {
      var y = i * alturaLinha;
      var v = linha.valores[0];
      var texto = completo(v, formato);
      var rotuloValor = linha.alerta ? 'Acaba em breve · ' + texto : texto;
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
          'class': 'an-marca ' + (linha.alerta ? 'an-m-alerta' : 'an-m1')
        });
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
      var v = linha.valores[0];
      var texto = completo(v, formato);
      var partes = [linha.nome + ': ' + texto];
      if (linha.alerta) { partes.push('acaba em breve'); }
      alvo(svg, i, 0, i * alturaLinha, largura, alturaLinha, partes.concat(linha.extras).join('. '), focavel);
    });
    ligarDica(fig, svg, function (i) {
      var linha = dados.linhas[i];
      return {
        titulo: linha.nome,
        linhas: [{ cor: linha.alerta ? 'var(--an-alerta)' : 'var(--an-s1)', valor: completo(linha.valores[0], formato), rotulo: nome }],
        extras: (linha.alerta ? ['Acaba em breve'] : []).concat(linha.extras),
        marcas: marcas[i] ? [marcas[i]] : []
      };
    });
    area.appendChild(svg);
  }

  /* ---------- Colunas (uma série) e duplas (duas séries) ---------- */
  function desenharColunas(fig, area, dados, largura) {
    var formato = fig.getAttribute('data-formato');
    var nSeries = dados.series.length;
    var n = dados.linhas.length;
    var altura = largura < 480 ? 190 : 230;
    var margem = { e: 46, d: 4, t: 10, b: 26 };
    var internoL = Math.max(10, largura - margem.e - margem.d);
    var internoA = altura - margem.t - margem.b;
    var maior = 0;
    dados.linhas.forEach(function (l) { l.valores.forEach(function (v) { if (v > maior) { maior = v; } }); });
    var ex = eixo(maior, formato === 'inteiro');
    var svg = criarSvg(fig, largura, altura);

    // Grade e números do eixo
    ex.marcas.forEach(function (v) {
      var y = margem.t + internoA - v / ex.topo * internoA;
      svg.appendChild(svgEl('line', {
        x1: margem.e, x2: largura - margem.d, y1: y, y2: y, 'class': v === 0 ? 'an-base' : 'an-grade'
      }));
      svg.appendChild(svgEl('text', {
        x: margem.e - 8, y: y + 4, 'text-anchor': 'end', 'class': 'an-t'
      }, compacto(v, formato)));
    });

    var slot = internoL / n;
    var folga = slot >= 8 ? 2 : (slot >= 3.5 ? 1 : 0);
    var larguraGrupo = nSeries === 1 ? slot - folga : Math.min(slot * 0.72, 2 * 24 + 2);
    var bw = nSeries === 1 ? Math.max(1, Math.min(24, larguraGrupo))
      : Math.max(2, Math.min(24, (larguraGrupo - 2) / 2));
    var focavel = n <= 12;
    var marcasPorLinha = [];

    dados.linhas.forEach(function (linha, i) {
      var centro = margem.e + i * slot + slot / 2;
      var inicio = centro - (nSeries * bw + (nSeries - 1) * 2) / 2;
      var daLinha = [];
      linha.valores.forEach(function (v, s) {
        var h = v / ex.topo * internoA;
        if (v > 0) { h = Math.max(1, h); }
        if (h <= 0) { return; }
        var x = inicio + s * (bw + 2);
        var y = margem.t + internoA - h;
        var marca = bw >= 6 ? svgEl('path', { d: caminhoBarra(x, y, bw, h, 'v') })
          : svgEl('rect', { x: x, y: y, width: bw, height: h });
        marca.setAttribute('class', 'an-marca an-m' + (s + 1));
        svg.appendChild(marca);
        daLinha.push(marca);
      });
      marcasPorLinha.push(daLinha);
    });

    // Rótulos do eixo X: só os que cabem, sem sobrepor
    var passo = Math.max(1, Math.ceil(46 / slot));
    dados.linhas.forEach(function (linha, i) {
      if (i % passo !== 0) { return; }
      svg.appendChild(svgEl('text', {
        x: margem.e + i * slot + slot / 2, y: altura - 6, 'text-anchor': 'middle', 'class': 'an-t'
      }, linha.curto));
    });

    dados.linhas.forEach(function (linha, i) {
      var partes = [linha.nome].concat(linha.valores.map(function (v, s) {
        return dados.series[s] + ': ' + completo(v, formato);
      })).concat(linha.extras);
      alvo(svg, i, margem.e + i * slot, margem.t, slot, internoA + margem.b, partes.join('. '), focavel);
    });
    ligarDica(fig, svg, function (i) {
      var linha = dados.linhas[i];
      return {
        titulo: linha.nome,
        linhas: linha.valores.map(function (v, s) {
          return { cor: 'var(--an-s' + (s + 1) + ')', valor: completo(v, formato), rotulo: dados.series[s] };
        }),
        extras: linha.extras,
        marcas: marcasPorLinha[i]
      };
    });

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

  /* ---------- Pilha (curva ABC) ---------- */
  function desenharPilha(fig, area, dados, largura) {
    var classes = ['A', 'B', 'C'];
    var total = { A: { n: 0, pct: 0 }, B: { n: 0, pct: 0 }, C: { n: 0, pct: 0 } };
    dados.linhas.forEach(function (l) {
      if (total[l.classe]) { total[l.classe].n += 1; total[l.classe].pct += l.valores[0]; }
    });
    var soma = classes.reduce(function (a, c) { return a + total[c].pct; }, 0) || 1;
    var altura = 30;
    var svg = criarSvg(fig, largura, altura);
    var folga = 2;
    var visiveis = classes.filter(function (c) { return total[c].n > 0; });
    var util = largura - folga * (visiveis.length - 1);
    var x = 0;
    var marcas = {};
    visiveis.forEach(function (c) {
      var w = Math.max(4, total[c].pct / soma * util);
      var marca = svgEl('rect', { x: x, y: 0, width: w, height: altura, rx: 4, 'class': 'an-marca an-m' + c });
      svg.appendChild(marca);
      marcas[c] = marca;
      x += w + folga;
    });
    function texto(c) {
      var t = total[c];
      var pct = fmtInt.format(Math.round(t.pct / soma * 100));
      return 'Classe ' + c + ': ' + fmtInt.format(t.n) + (t.n === 1 ? ' produto' : ' produtos') + ', ' + pct + '% do valor';
    }
    x = 0;
    visiveis.forEach(function (c, k) {
      var w = Math.max(4, total[c].pct / soma * util);
      alvo(svg, k, x, 0, w + folga, altura, texto(c), true);
      x += w + folga;
    });
    ligarDica(fig, svg, function (k) {
      var c = visiveis[k];
      return {
        titulo: 'Classe ' + c,
        linhas: [{ cor: 'var(--an-' + c.toLowerCase() + ')', valor: fmtInt.format(Math.round(total[c].pct / soma * 100)) + '%', rotulo: 'do valor que saiu' }],
        extras: [fmtInt.format(total[c].n) + (total[c].n === 1 ? ' produto' : ' produtos')],
        marcas: [marcas[c]]
      };
    });
    area.appendChild(svg);

    var legenda = htmlEl('ul', 'an-leg an-leg--abaixo');
    visiveis.forEach(function (c) {
      var li = htmlEl('li');
      li.appendChild(htmlEl('span', 'an-leg__cor an-leg__cor--' + c));
      li.appendChild(htmlEl('span', null, texto(c)));
      legenda.appendChild(li);
    });
    area.appendChild(legenda);
  }

  /* ---------- Montagem ---------- */
  var DESENHOS = { barras: desenharBarras, colunas: desenharColunas, duplas: desenharColunas, pilha: desenharPilha };

  function desenhar(fig) {
    var area = fig.querySelector('.an-fig__area');
    var dados = fig._anDados;
    var largura = Math.floor(area.clientWidth);
    var desenho = DESENHOS[fig.getAttribute('data-grafico')];
    if (!desenho || !dados || largura < 40) { return false; }
    var dica = fig.querySelector('.an-dica');
    if (dica) { dica.remove(); }
    area.textContent = '';
    desenho(fig, area, dados, largura);
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
