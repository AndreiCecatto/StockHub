/*
 * StockHub v2: navegação mais fluida (carregado com defer).
 *  1. Pré-carga ao passar o mouse (ou tocar) nos links internos: <link rel="prefetch"> (ou fetch de baixa prioridade).
 *     Só mesma origem, só links GET comuns, e NUNCA: sair, excluir, exportar CSV, /api, links com ?acao=, links com
 *     data-sh-sem-prefetch, nem com "economia de dados" ligada. No máximo 8 páginas por visita.
 *     Honestidade: a pré-carga só ajuda se o servidor deixar a resposta ser guardada. O Spring Security manda
 *     "Cache-Control: no-store" em páginas autenticadas por padrão, o que anula o ganho (a página é buscada de novo
 *     ao clicar). Sem esse cabeçalho a mudança de página fica mais rápida; com ele, tudo continua funcionando.
 *  2. Plano B para navegadores sem View Transitions entre documentos (Firefox): ao clicar num link interno comum, o
 *     conteúdo faz fade de ~130 ms antes de navegar (a entrada tem a animação CSS .sh-main). Respeita
 *     prefers-reduced-motion (sem animação) e nunca intercepta Ctrl/Cmd/Shift/Alt, botão do meio, nova aba, download.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var raiz = document.documentElement;
  var dadosEconomicos = !!(navigator.connection && navigator.connection.saveData);
  var reduz = function () {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  };

  /* ---------- Quais links são "navegação comum" ---------- */
  function linkComum(a) {
    if (!a || !a.href || a.tagName !== 'A') {
      return false;
    }
    if (a.hasAttribute('download') || a.hasAttribute('data-bs-toggle') || a.hasAttribute('data-sh-sem-transicao')) {
      return false;
    }
    var alvo = a.getAttribute('target');
    if (alvo && alvo !== '_self') {
      return false;
    }
    if (a.protocol !== 'http:' && a.protocol !== 'https:') {
      return false;
    }
    if (a.origin !== window.location.origin) {
      return false;
    }
    // só mudança de hash na mesma página: o navegador resolve
    if (a.pathname === window.location.pathname && a.search === window.location.search) {
      return false;
    }
    return true;
  }

  function seguroParaPrefetch(a) {
    if (!linkComum(a) || a.hasAttribute('data-sh-sem-prefetch')) {
      return false;
    }
    var caminho = a.pathname.toLowerCase();
    if (/\/(logout|login|cadastro|excluir|api)(\/|$)/.test(caminho) || /\.(csv|pdf|xlsx?|zip)$/.test(caminho)) {
      return false;
    }
    if (/exportar/.test(caminho) || /[?&]acao=/i.test(a.search)) {
      return false;
    }
    return true;
  }

  /* ---------- Pré-carga ---------- */
  var jaPedidos = {};
  var totalPedidos = 0;
  var suportaPrefetch = (function () {
    try {
      var l = document.createElement('link');
      return !!(l.relList && l.relList.supports && l.relList.supports('prefetch'));
    } catch (erro) {
      return false;
    }
  })();

  function prefetch(a) {
    var url = a.href.split('#')[0];
    if (jaPedidos[url] || totalPedidos >= 8 || dadosEconomicos) {
      return;
    }
    jaPedidos[url] = true;
    totalPedidos++;
    if (suportaPrefetch) {
      var l = document.createElement('link');
      l.rel = 'prefetch';
      l.href = url;
      l.as = 'document';
      document.head.appendChild(l);
    } else if (window.fetch) {
      window.fetch(url, { credentials: 'same-origin', priority: 'low' }).catch(function () {
        // pré-carga é só um palpite: falhar não muda nada
      });
    }
  }

  var temporizador = null;
  var alvoPendente = null;

  function intencao(evento) {
    var a = evento.target.closest ? evento.target.closest('a[href]') : null;
    if (!a || !seguroParaPrefetch(a)) {
      return;
    }
    alvoPendente = a;
    window.clearTimeout(temporizador);
    // mouse: espera 65 ms (passagem de raspão não conta); toque e teclado: na hora
    var espera = evento.type === 'mouseover' ? 65 : 0;
    temporizador = window.setTimeout(function () {
      if (alvoPendente === a) {
        prefetch(a);
      }
    }, espera);
  }

  function desistir(evento) {
    var a = evento.target.closest ? evento.target.closest('a[href]') : null;
    if (a && a === alvoPendente) {
      window.clearTimeout(temporizador);
      alvoPendente = null;
    }
  }

  document.addEventListener('mouseover', intencao, { passive: true });
  document.addEventListener('mouseout', desistir, { passive: true });
  document.addEventListener('focusin', intencao);
  document.addEventListener('touchstart', intencao, { passive: true });

  /* ---------- Plano B: fade ao sair, só sem View Transitions ---------- */
  document.addEventListener('click', function (evento) {
    if (raiz.hasAttribute('data-sh-vt') || reduz() || evento.defaultPrevented) {
      return;
    }
    if (evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
      return;
    }
    var a = evento.target.closest ? evento.target.closest('a[href]') : null;
    if (!a || !linkComum(a)) {
      return;
    }
    evento.preventDefault();
    raiz.classList.add('sh-saindo');
    var destino = a.href;
    window.setTimeout(function () {
      window.location.assign(destino);
    }, 130);
    // se a navegação for cancelada (por exemplo, "sair sem salvar?"), a página volta ao normal
    window.setTimeout(function () {
      raiz.classList.remove('sh-saindo');
    }, 2500);
  });

  // Voltou pelo cache do navegador (botão voltar): tira o estado de saída
  window.addEventListener('pageshow', function (evento) {
    if (evento.persisted) {
      raiz.classList.remove('sh-saindo');
    }
  });
})();
