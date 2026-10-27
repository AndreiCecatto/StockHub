/*
 * StockHub v3: kit de rolagem, parte JavaScript (carregado com defer em todas as telas).
 * O contrato completo está em docs/design-system.md ("v3: central de operações") e css/rolagem.css.
 *
 * O que faz:
 *  - Modo CSS (Chrome/Edge 115+, Safari 26+): quase nada. As animações são do CSS (animation-timeline). Aqui só roda o
 *    acompanhamento DISCRETO das cenas que pedem etapas (data-sh-cena-passos="N"), que dá data-sh-passo e o evento
 *    sh:cena-passo.
 *  - Modo JS (Firefox): html.sh-rolagem-js (ligada por js/tema-init.js). IntersectionObserver marca .is-visivel, uma vez,
 *    em .sh-revelar, nos filhos de .sh-revelar-grupo e em .sh-desenhar; um ouvinte de scroll PASSIVO (com
 *    requestAnimationFrame) calcula --sh-progresso nas cenas .sh-cena e --sh-progresso-pagina no <html>.
 *  - Menos movimento: não faz nada (o CSS já mostra tudo no estado final).
 * NUNCA impede, atrasa ou redireciona a rolagem: não usa wheel, touchmove nem preventDefault.
 *
 * API: SH.rolagem.modo ('css' | 'js'), SH.rolagem.progresso(elementoCena) -> número de 0 a 1,
 *      SH.rolagem.atualizar() (recalcula; útil depois de inserir conteúdo novo na página).
 * Eventos (no elemento .sh-cena, com bolha): "sh:cena-passo" com detail { passo, total, progresso }.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var raiz = document.documentElement;
  var temTimeline = !!(window.CSS && window.CSS.supports && window.CSS.supports('animation-timeline: view()'));
  var reduz = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function limitar(n) {
    return Math.max(0, Math.min(1, n));
  }

  /** Progresso (0 a 1) de uma cena fixa: de quando o palco gruda até quando solta. */
  function progresso(cena) {
    var caixa = cena.getBoundingClientRect();
    var estilo = window.getComputedStyle(cena);
    var topo = parseFloat(estilo.getPropertyValue('--sh-cena-topo')) || 0;
    var palco = cena.firstElementChild;
    var alturaPalco = palco ? palco.getBoundingClientRect().height : window.innerHeight - topo;
    var curso = caixa.height - alturaPalco;
    if (curso <= 0) {
      return caixa.top <= topo ? 1 : 0;
    }
    return limitar((topo - caixa.top) / curso);
  }

  SH.rolagem = {
    modo: temTimeline ? 'css' : 'js',
    progresso: progresso,
    atualizar: function () {}
  };

  raiz.setAttribute('data-sh-rolagem', 'pronta');

  if (reduz) {
    return;
  }

  var modoJs = !temTimeline && 'IntersectionObserver' in window;
  if (modoJs) {
    raiz.classList.add('sh-rolagem-js');
  }

  /* ---------- Modo JS: revelar, desenhar e crescer ao entrar na viewport ---------- */
  var observador = null;

  function observar(raizBusca) {
    if (!modoJs) {
      return;
    }
    if (!observador) {
      observador = new window.IntersectionObserver(
        function (entradas) {
          for (var i = 0; i < entradas.length; i++) {
            if (entradas[i].isIntersecting) {
              entradas[i].target.classList.add('is-visivel');
              observador.unobserve(entradas[i].target);
            }
          }
        },
        { threshold: 0.12, rootMargin: '0px 0px -6% 0px' }
      );
    }
    var alvos = (raizBusca || document).querySelectorAll('.sh-revelar, .sh-revelar-grupo > *, .sh-desenhar');
    for (var j = 0; j < alvos.length; j++) {
      if (!alvos[j].classList.contains('is-visivel') && !alvos[j].hasAttribute('data-sh-observado')) {
        alvos[j].setAttribute('data-sh-observado', '');
        observador.observe(alvos[j]);
      }
    }
  }

  /* ---------- Cenas e progresso da página (ouvinte passivo de scroll) ---------- */
  var cenas = [];
  var passos = {};
  var barraPagina = null;
  var pendente = false;

  function coletar() {
    cenas = Array.prototype.slice.call(document.querySelectorAll('.sh-cena'));
    barraPagina = document.querySelector('.sh-progresso-pagina');
  }

  function quadro() {
    pendente = false;
    var altura = window.innerHeight || 1;
    for (var i = 0; i < cenas.length; i++) {
      var cena = cenas[i];
      var caixa = cena.getBoundingClientRect();
      // longe da viewport: não calcula
      if (caixa.bottom < -altura || caixa.top > altura * 2) {
        continue;
      }
      var p = progresso(cena);
      if (modoJs) {
        cena.style.setProperty('--sh-progresso', p.toFixed(4));
      }
      var total = parseInt(cena.getAttribute('data-sh-cena-passos'), 10);
      if (total > 0) {
        var passo = Math.min(total - 1, Math.floor(p * total));
        if (cena.getAttribute('data-sh-passo') !== String(passo)) {
          cena.setAttribute('data-sh-passo', String(passo));
          cena.dispatchEvent(new window.CustomEvent('sh:cena-passo', { bubbles: true, detail: { passo: passo, total: total, progresso: p } }));
        }
      }
    }
    if (modoJs && barraPagina) {
      var max = raiz.scrollHeight - altura;
      raiz.style.setProperty('--sh-progresso-pagina', max > 0 ? limitar(window.pageYOffset / max).toFixed(4) : '0');
    }
  }

  function agendar() {
    if (!pendente && window.requestAnimationFrame) {
      pendente = true;
      window.requestAnimationFrame(quadro);
    }
  }

  function iniciar() {
    coletar();
    observar(document);
    var precisaDeScroll = cenas.length > 0 && (modoJs || document.querySelector('[data-sh-cena-passos]'));
    if ((precisaDeScroll || (modoJs && barraPagina)) && !SH.rolagem.ouvindo) {
      SH.rolagem.ouvindo = true;
      window.addEventListener('scroll', agendar, { passive: true });
      window.addEventListener('resize', agendar, { passive: true });
    }
    agendar();
  }

  SH.rolagem.atualizar = iniciar;
  iniciar();
})();
