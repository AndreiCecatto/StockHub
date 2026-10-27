/*
 * StockHub v3: apresentação pública. Só o que o kit de rolagem (js/rolagem.js) não cobre:
 *  1. Barra superior: ganha a linha de baixo (.is-rolou) quando a página sai do topo.
 *  2. Menu de âncoras: marca com aria-current="location" a seção que ocupa a tela.
 *  3. Rótulo da tecla da paleta: "⌘ K" no Mac, "Ctrl K" nos demais (texto das maquetes e do parágrafo).
 * Tudo com IntersectionObserver (sem ouvinte de scroll), sem tocar na rolagem. Sem JavaScript a página inteira funciona.
 */
(function () {
  'use strict';

  var barra = document.querySelector('[data-ap-barra]');

  /* 1. Linha da barra: sentinela no topo da página */
  if (barra && 'IntersectionObserver' in window) {
    var sentinela = document.createElement('div');
    sentinela.setAttribute('aria-hidden', 'true');
    sentinela.className = 'visually-hidden';
    document.body.insertBefore(sentinela, document.body.firstChild);
    new window.IntersectionObserver(function (entradas) {
      barra.classList.toggle('is-rolou', !entradas[0].isIntersecting);
    }).observe(sentinela);
  }

  /* 2. Seção atual no menu */
  var links = document.querySelectorAll('.ap-barra__nav a[href^="#"]');
  if (links.length && 'IntersectionObserver' in window) {
    var porId = {};
    var secoes = [];
    for (var i = 0; i < links.length; i++) {
      var id = links[i].getAttribute('href').slice(1);
      var secao = document.getElementById(id);
      if (secao) {
        porId[id] = links[i];
        secoes.push(secao);
      }
    }
    var atual = null;
    var marcar = function (id) {
      if (id === atual) {
        return;
      }
      atual = id;
      for (var chave in porId) {
        if (Object.prototype.hasOwnProperty.call(porId, chave)) {
          if (chave === id) {
            porId[chave].setAttribute('aria-current', 'location');
          } else {
            porId[chave].removeAttribute('aria-current');
          }
        }
      }
    };
    // faixa fina no meio da viewport: a seção que cruza o meio da tela é a atual
    var obs = new window.IntersectionObserver(function (entradas) {
      for (var e = 0; e < entradas.length; e++) {
        if (entradas[e].isIntersecting) {
          marcar(entradas[e].target.id);
        }
      }
    }, { rootMargin: '-50% 0px -50% 0px', threshold: 0 });
    for (var s = 0; s < secoes.length; s++) {
      obs.observe(secoes[s]);
    }
    // fora das seções do menu (hero, chamada final): nenhuma marcada
    var fora = new window.IntersectionObserver(function (entradas) {
      for (var f = 0; f < entradas.length; f++) {
        if (entradas[f].isIntersecting) {
          marcar(null);
        }
      }
    }, { rootMargin: '-50% 0px -50% 0px', threshold: 0 });
    var extras = document.querySelectorAll('#inicio, #comecar');
    for (var x = 0; x < extras.length; x++) {
      fora.observe(extras[x]);
    }
  }

  /* 4. Âncoras do menu: URL limpa e rolagem própria (suave, interrompível), que faz a cena da seção tocar sozinha.
   * O salto nativo do link deixava a cena parada no começo (progresso 0) e escrevia #secao na URL. */
  var raiz = document.documentElement;
  var reduzido = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var TOPO = 64; // altura da barra fixa (--ap-topo)

  // abriu (ou recarregou) com #secao: tira o hash sem mexer na rolagem nem no histórico
  if (window.location.hash && window.history && window.history.replaceState) {
    window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
  }

  var rodando = null;
  var parar = function () {
    if (rodando) {
      window.cancelAnimationFrame(rodando);
      rodando = null;
    }
  };
  ['wheel', 'touchstart', 'keydown', 'pointerdown'].forEach(function (nome) {
    window.addEventListener(nome, parar, { passive: true });
  });

  var suave = function (t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; };
  var saida = function (t) { return 1 - Math.pow(1 - t, 3); };

  // anima a rolagem de "de" até "para" em "ms" ms; chama "fim" ao chegar (não chama se o usuário interromper)
  var rolarAte = function (para, ms, curva, fim) {
    parar();
    var de = window.pageYOffset;
    var t0 = null;
    var passo = function (agora) {
      if (t0 === null) {
        t0 = agora;
      }
      var k = Math.min((agora - t0) / ms, 1);
      window.scrollTo({ top: de + (para - de) * curva(k), behavior: 'instant' });
      if (k < 1) {
        rodando = window.requestAnimationFrame(passo);
      } else {
        rodando = null;
        if (fim) {
          fim();
        }
      }
    };
    rodando = window.requestAnimationFrame(passo);
  };

  // logo (link para a própria página): sobe ao topo com a URL limpa, em vez de recarregar e voltar à rolagem antiga
  document.addEventListener('click', function (ev) {
    var l = ev.target.closest ? ev.target.closest('a.ap-barra__marca') : null;
    if (!l || ev.defaultPrevented || ev.button !== 0 || ev.metaKey || ev.ctrlKey || ev.shiftKey || ev.altKey) {
      return;
    }
    if (l.pathname !== window.location.pathname) {
      return;
    }
    ev.preventDefault();
    if (reduzido) {
      window.scrollTo({ top: 0, behavior: 'instant' });
    } else {
      rolarAte(0, Math.min(1500, 450 + window.pageYOffset * 0.12), suave);
    }
  });

  document.addEventListener('click', function (ev) {
    var a = ev.target.closest ? ev.target.closest('a[href^="#"]') : null;
    if (!a || ev.defaultPrevented || ev.button !== 0 || ev.metaKey || ev.ctrlKey || ev.shiftKey || ev.altKey) {
      return;
    }
    var id = a.getAttribute('href').slice(1);
    var alvo = id ? document.getElementById(id) : document.body;
    if (!alvo) {
      return;
    }
    ev.preventDefault();
    var y = id ? Math.max(0, alvo.getBoundingClientRect().top + window.pageYOffset - TOPO) : 0;
    var chegou = function () {
      if (id && alvo.id) {
        if (!alvo.hasAttribute('tabindex')) {
          alvo.setAttribute('tabindex', '-1');
        }
        alvo.focus({ preventScroll: true });
      }
    };
    if (reduzido) {
      window.scrollTo({ top: y, behavior: 'instant' });
      chegou();
      return;
    }
    var dist = Math.abs(y - window.pageYOffset);
    var ms = Math.min(1500, 450 + dist * 0.12);
    // cena fixa (telas largas com GSAP): depois de chegar, avança até o fim da cena para o movimento tocar
    var cena = alvo.querySelector ? (alvo.classList.contains('sh-cena') ? alvo : alvo.querySelector('.sh-cena')) : null;
    var largo = window.matchMedia('(min-width: 992px) and (min-height: 600px)').matches;
    var extra = 0;
    if (cena && largo && raiz.classList.contains('ap-gsap')) {
      extra = Math.max(0, (cena.offsetHeight - (window.innerHeight - TOPO)));
    }
    rolarAte(y, ms, suave, function () {
      chegou();
      if (extra) {
        rolarAte(y + extra, Math.min(4000, 1800 + extra), saida);
      }
    });
  });

  /* 3. Ctrl K ou ⌘ K */
  var mac = /Mac|iPhone|iPad|iPod/.test(navigator.platform || navigator.userAgent || '');
  if (mac) {
    var teclas = document.querySelectorAll('[data-ap-tecla]');
    for (var t = 0; t < teclas.length; t++) {
      teclas[t].textContent = '⌘ K';
    }
  }
})();
