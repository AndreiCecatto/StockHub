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

  /* 3. Ctrl K ou ⌘ K */
  var mac = /Mac|iPhone|iPad|iPod/.test(navigator.platform || navigator.userAgent || '');
  if (mac) {
    var teclas = document.querySelectorAll('[data-ap-tecla]');
    for (var t = 0; t < teclas.length; t++) {
      teclas[t].textContent = '⌘ K';
    }
  }
})();
