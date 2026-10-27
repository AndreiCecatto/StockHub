/*
 * StockHub: atalhos de teclado e modal de ajuda (seção 2.6 de docs/design-system.md).
 * Regras (WCAG 2.1.4, atalho de uma tecla):
 *   - atalhos de uma tecla só valem quando o foco NÃO está em campo de texto, select, textarea ou
 *     elemento editável, e nunca com Ctrl, Alt ou Meta (AltGr é aceito: no teclado ABNT2 "/" e "?" usam AltGr);
 *   - o modal de ajuda (tecla ?) tem a chave "Atalhos de teclado ativados"; desligar grava em localStorage.
 * Ctrl+Enter envia o formulário marcado com data-sh-ctrl-enter, mesmo de dentro do texto.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});

  // Telas públicas (login e cadastro) não têm navegação: sem atalhos
  if (document.body.hasAttribute('data-sh-publico')) {
    return;
  }

  var CHAVE = 'sh-atalhos';
  var PREFIXO_IR = 'g';
  var JANELA_IR_MS = 1500;

  var aguardandoIr = false;
  var temporizadorIr = null;

  function ativado() {
    return SH.armazenar ? SH.armazenar.ler(CHAVE) !== 'desligados' : true;
  }

  function contexto() {
    return SH.contexto || '';
  }

  /** Destino das teclas "g" + letra. */
  var DESTINOS = {
    i: '/dashboard',
    p: '/produtos?tamanho=25',
    c: '/categorias',
    b: '/estoque-baixo',
    e: '/estoque',
    r: '/valor-estoque'
  };

  function emCampoDeTexto(alvo) {
    if (!alvo || !alvo.tagName) {
      return false;
    }
    var etiqueta = alvo.tagName.toLowerCase();
    if (etiqueta === 'textarea' || etiqueta === 'select') {
      return true;
    }
    if (etiqueta === 'input') {
      var tipo = (alvo.getAttribute('type') || 'text').toLowerCase();
      // caixa de marcar, botão e semelhantes não recebem texto, então não bloqueiam o atalho
      return ['checkbox', 'radio', 'button', 'submit', 'reset', 'range', 'color', 'file', 'image'].indexOf(tipo) === -1;
    }
    return !!alvo.isContentEditable;
  }

  function modalAberto() {
    return document.body.classList.contains('modal-open');
  }

  function irPara(caminho) {
    window.location.assign(contexto() + caminho);
  }

  function focarBusca() {
    var campo = document.querySelector('[data-sh-busca-lista]');
    if (campo) {
      campo.focus();
      if (campo.select) {
        campo.select();
      }
      return true;
    }
    // nas outras telas, "/" abre a paleta de comandos (js/paleta.js), que busca produtos, telas e ações
    if (SH.paleta && document.getElementById('sh-paleta')) {
      SH.paleta.abrir('');
      return true;
    }
    campo = document.querySelector('[data-sh-busca-global]');
    if (campo && campo.focus) {
      campo.focus();
      return true;
    }
    return false;
  }

  function novoItem() {
    var link =
      document.querySelector('[data-sh-atalho-novo="pagina"]') ||
      document.querySelector('[data-sh-atalho-novo="padrao"]');
    if (link && link.href) {
      window.location.assign(link.href);
    }
  }

  function movimentar() {
    var seletor = document.querySelector('[data-sh-seletor-produto]');
    if (seletor) {
      seletor.focus();
    } else {
      irPara('/estoque#registrar');
    }
  }

  function abrirAjuda() {
    var modalEl = document.getElementById('sh-modal-atalhos');
    if (!modalEl || typeof window.bootstrap === 'undefined') {
      return;
    }
    var abrir = function () {
      window.bootstrap.Modal.getOrCreateInstance(modalEl).show();
    };
    var painel = document.querySelector('.offcanvas.show');
    if (painel) {
      // fecha o painel "Mais" antes, para não empilhar dois fundos escuros
      painel.addEventListener('hidden.bs.offcanvas', abrir, { once: true });
      window.bootstrap.Offcanvas.getOrCreateInstance(painel).hide();
    } else {
      abrir();
    }
  }

  function cancelarIr() {
    aguardandoIr = false;
    window.clearTimeout(temporizadorIr);
  }

  document.addEventListener('keydown', function (evento) {
    if (evento.defaultPrevented) {
      return;
    }

    // Ctrl+Enter: envia o formulário de movimentação (não é atalho de uma tecla, vale também digitando)
    if (evento.key === 'Enter' && (evento.ctrlKey || evento.metaKey)) {
      var origem = evento.target && evento.target.closest ? evento.target.closest('form[data-sh-ctrl-enter]') : null;
      if (origem && typeof origem.requestSubmit === 'function') {
        evento.preventDefault();
        origem.requestSubmit();
      }
      return;
    }

    // Esc no campo de busca: limpa o texto
    if (evento.key === 'Escape') {
      var campo = evento.target;
      if (campo && campo.matches && campo.matches('input[data-sh-limpar-esc]') && campo.value) {
        campo.value = '';
        evento.preventDefault();
      }
      return;
    }

    if (!ativado() || evento.repeat || modalAberto()) {
      return;
    }
    var altGr = evento.getModifierState && evento.getModifierState('AltGraph');
    if ((evento.ctrlKey || evento.altKey || evento.metaKey) && !altGr) {
      return;
    }
    if (emCampoDeTexto(evento.target)) {
      return;
    }

    var tecla = evento.key;
    if (/^[a-zA-Z]$/.test(tecla)) {
      // letra com Shift não é atalho; com Caps Lock ligado a letra maiúscula vale como minúscula
      if (evento.shiftKey) {
        return;
      }
      tecla = tecla.toLowerCase();
    }

    if (aguardandoIr) {
      var destino = DESTINOS[tecla];
      cancelarIr();
      if (destino) {
        evento.preventDefault();
        irPara(destino);
      }
      return;
    }

    switch (tecla) {
      case '/':
        if (focarBusca()) {
          evento.preventDefault();
        }
        break;
      case 'n':
        novoItem();
        break;
      case 'm':
        movimentar();
        break;
      case PREFIXO_IR:
        aguardandoIr = true;
        temporizadorIr = window.setTimeout(cancelarIr, JANELA_IR_MS);
        break;
      case '?':
        evento.preventDefault();
        abrirAjuda();
        break;
      default:
        break;
    }
  });

  // Botões "Atalhos de teclado" dos menus abrem o mesmo modal
  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-abrir-atalhos]') : null;
    if (botao) {
      evento.preventDefault();
      abrirAjuda();
    }
  });

  // Chave "Atalhos de teclado ativados" dentro do modal
  var chave = document.getElementById('sh-atalhos-ativos');
  if (chave) {
    chave.checked = ativado();
    chave.addEventListener('change', function () {
      SH.armazenar.gravar(CHAVE, chave.checked ? 'ligados' : 'desligados');
      if (SH.anunciar) {
        SH.anunciar(chave.checked ? 'Atalhos de teclado ativados.' : 'Atalhos de teclado desativados.');
      }
    });
  }
})();
