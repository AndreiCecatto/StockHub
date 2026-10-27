/*
 * StockHub v3: navegação por teclado em listas e tabelas (utilitário genérico e OPCIONAL).
 *
 * Contrato:
 *  - Marque o contêiner com data-sh-lista-teclado (uma <table>, <ul> ou qualquer bloco).
 *  - Linhas: em tabela, as <tr> do <tbody>; nos demais casos, os elementos com data-sh-linha.
 *  - j = próxima linha, k = linha anterior, Enter = abre a linha com foco. Esc tira o foco da linha.
 *    Enter aciona o link marcado com data-sh-linha-link (ou, na falta, o primeiro <a href> da linha) por um clique de
 *    verdade; por isso um link com data-sh-peek abre o painel lateral em vez de navegar.
 *  - A linha com foco recebe a classe is-foco (estilo em css/componentes.css) e tabindex="-1"; o foco do navegador
 *    vai para ela, então o leitor de tela anuncia o conteúdo da linha.
 *  - Segue as regras dos atalhos do sistema (js/atalhos.js): nunca age digitando em campo, nem com Ctrl, Alt ou Meta,
 *    nem com modal ou painel aberto, e respeita a chave "Atalhos de teclado ativados".
 * Evento (no contêiner, com bolha): sh:linha-foco, detail { linha, indice, total }.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var listas = document.querySelectorAll('[data-sh-lista-teclado]');
  if (!listas.length || document.body.hasAttribute('data-sh-publico')) {
    return;
  }

  function atalhosAtivos() {
    return SH.armazenar ? SH.armazenar.ler('sh-atalhos') !== 'desligados' : true;
  }

  function emCampo(alvo) {
    if (!alvo || !alvo.tagName) {
      return false;
    }
    var t = alvo.tagName.toLowerCase();
    if (t === 'textarea' || t === 'select') {
      return true;
    }
    if (t === 'input') {
      var tipo = (alvo.getAttribute('type') || 'text').toLowerCase();
      return ['checkbox', 'radio', 'button', 'submit', 'reset', 'range', 'color', 'file', 'image'].indexOf(tipo) === -1;
    }
    return !!alvo.isContentEditable;
  }

  function linhasDe(lista) {
    var itens = lista.tagName === 'TABLE' || lista.querySelector('tbody')
      ? lista.querySelectorAll('tbody > tr')
      : lista.querySelectorAll('[data-sh-linha]');
    var visiveis = [];
    for (var i = 0; i < itens.length; i++) {
      if (itens[i].offsetParent !== null || itens[i].getClientRects().length) {
        visiveis.push(itens[i]);
      }
    }
    return visiveis;
  }

  function listaDaLinha(linha) {
    return linha && linha.closest ? linha.closest('[data-sh-lista-teclado]') : null;
  }

  function limparFoco(lista) {
    var marcadas = lista.querySelectorAll('.is-foco');
    for (var i = 0; i < marcadas.length; i++) {
      marcadas[i].classList.remove('is-foco');
    }
  }

  function focar(lista, linha, indice, total) {
    limparFoco(lista);
    linha.classList.add('is-foco');
    linha.setAttribute('tabindex', '-1');
    linha.setAttribute('data-sh-linha', '');
    linha.focus({ preventScroll: true });
    linha.scrollIntoView({ block: 'nearest', behavior: 'auto' });
    lista.dispatchEvent(new window.CustomEvent('sh:linha-foco', { bubbles: true, detail: { linha: linha, indice: indice, total: total } }));
  }

  function listaAtiva() {
    var ativo = document.activeElement;
    var daLinha = listaDaLinha(ativo);
    if (daLinha) {
      return daLinha;
    }
    return listas[0];
  }

  document.addEventListener('keydown', function (evento) {
    if (evento.defaultPrevented || evento.ctrlKey || evento.altKey || evento.metaKey) {
      return;
    }
    var tecla = evento.key;
    if (tecla !== 'j' && tecla !== 'k' && tecla !== 'Enter' && tecla !== 'Escape') {
      return;
    }
    if (emCampo(evento.target) || !atalhosAtivos()) {
      return;
    }
    if (document.querySelector('dialog[open]') || document.body.classList.contains('modal-open')) {
      return;
    }
    var lista = listaAtiva();
    var linhas = linhasDe(lista);
    if (!linhas.length) {
      return;
    }
    var atual = -1;
    for (var i = 0; i < linhas.length; i++) {
      if (linhas[i] === document.activeElement || linhas[i].contains(document.activeElement) && linhas[i].classList.contains('is-foco')) {
        atual = i;
        break;
      }
    }
    if (atual < 0) {
      var marcada = lista.querySelector('.is-foco');
      atual = marcada ? linhas.indexOf(marcada) : -1;
    }

    if (tecla === 'j' || tecla === 'k') {
      evento.preventDefault();
      var destino = tecla === 'j' ? Math.min(linhas.length - 1, atual + 1) : Math.max(0, atual < 0 ? 0 : atual - 1);
      focar(lista, linhas[destino], destino, linhas.length);
      return;
    }

    if (tecla === 'Escape') {
      if (atual >= 0) {
        limparFoco(lista);
        linhas[atual].blur();
      }
      return;
    }

    // Enter: só quando a linha tem o foco (não rouba o Enter de botões e links focados dentro dela)
    if (tecla === 'Enter' && atual >= 0 && document.activeElement === linhas[atual]) {
      var link = linhas[atual].querySelector('[data-sh-linha-link]') || linhas[atual].querySelector('a[href]');
      if (link) {
        evento.preventDefault();
        link.click();
      }
    }
  });
})();
