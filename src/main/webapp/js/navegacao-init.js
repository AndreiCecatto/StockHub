/*
 * StockHub v2: início da transição entre páginas. Carregado de forma SÍNCRONA no <head> (minúsculo), porque o evento
 * "pagereveal" dispara antes da primeira pintura e um script adiado perderia a hora.
 *
 * O que faz:
 *  1. Marca o <html> com data-sh-vt quando o navegador faz sozinho a transição entre documentos
 *     (Chrome/Edge 126+, Safari recente; Firefox ainda não). Sem a marca, entra o plano B de css/layout.css e
 *     js/navegacao.js (fade ao sair e subida ao entrar).
 *  2. Diz ao navegador a DIREÇÃO da navegação: "para-frente" (entrando num nível mais fundo, ou botão avançar),
 *     "para-tras" (subindo de nível, ou botão voltar) ou "mesmo-nivel" (entre as telas do menu, só fade).
 *     O CSS usa isso em html:active-view-transition-type(...).
 * Não lê nem grava dado nenhum; só compara endereços.
 */
(function () {
  'use strict';

  var raiz = document.documentElement;
  var reduz = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var suporta = 'onpagereveal' in window && typeof window.CSS !== 'undefined' && window.CSS.supports &&
    window.CSS.supports('selector(:active-view-transition-type(a))');

  if (suporta && !reduz) {
    raiz.setAttribute('data-sh-vt', '');
  }

  function profundidade(endereco) {
    var caminho = '';
    try {
      caminho = new URL(endereco, window.location.href).pathname;
    } catch (erro) {
      return 0;
    }
    var partes = caminho.split('/').filter(function (p) {
      return p !== '';
    });
    // "/" e "/dashboard" são o mesmo nível: a raiz do menu
    return partes.length;
  }

  function tipoDeNavegacao(de, para, tipo) {
    if (!de || !para) {
      return 'mesmo-nivel';
    }
    if (tipo === 'traverse') {
      var indiceDe = de.index;
      var indicePara = para.index;
      if (typeof indiceDe === 'number' && typeof indicePara === 'number' && indiceDe !== indicePara) {
        return indicePara < indiceDe ? 'para-tras' : 'para-frente';
      }
    }
    var a = profundidade(de.url);
    var b = profundidade(para.url);
    if (b > a) {
      return 'para-frente';
    }
    if (b < a) {
      return 'para-tras';
    }
    return 'mesmo-nivel';
  }

  function marcar(evento, de, para, tipo) {
    var transicao = evento.viewTransition;
    if (!transicao || !transicao.types || typeof transicao.types.add !== 'function') {
      return;
    }
    try {
      transicao.types.add(tipoDeNavegacao(de, para, tipo));
    } catch (erro) {
      // sem tipos: fica a animação padrão (fade)
    }
  }

  // Na página que está saindo
  window.addEventListener('pageswap', function (evento) {
    var ativacao = evento.activation;
    if (!ativacao) {
      return;
    }
    marcar(evento, ativacao.from, ativacao.entry, ativacao.navigationType);
  });

  // Na página que está chegando
  window.addEventListener('pagereveal', function (evento) {
    var nav = window.navigation;
    var ativacao = nav && nav.activation;
    if (!ativacao) {
      return;
    }
    marcar(evento, ativacao.from, ativacao.entry, ativacao.navigationType);
  });
})();
