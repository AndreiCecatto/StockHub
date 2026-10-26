/*
 * StockHub: toasts (seção 4.6 de docs/design-system.md).
 * As páginas continuam mostrando a mensagem de sucesso no HTML (funciona sem JS e é escapada na JSP).
 * Com JS, todo elemento com data-sh-toast vira um toast e sai da página. Erros NÃO viram toast:
 * ficam na página, persistentes (WCAG 2.2.1).
 * O contêiner #sh-toasts tem aria-live="polite" desde o carregamento, então o texto novo é anunciado
 * sem tirar o foco de onde o usuário está. Dura 7 s, pausa com mouse ou foco, fecha com o botão ou Esc.
 * Máximo de 3 empilhados.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var DURACAO = 7000;
  var MAXIMO = 3;

  function areaDeToasts() {
    return document.getElementById('sh-toasts');
  }

  function remover(toast) {
    if (!toast || !toast.parentNode || toast.getAttribute('data-saindo') === 'sim') {
      return;
    }
    toast.setAttribute('data-saindo', 'sim');
    window.clearTimeout(Number(toast.getAttribute('data-timer')));
    if (SH.reduzMovimento && SH.reduzMovimento()) {
      toast.parentNode.removeChild(toast);
      return;
    }
    toast.classList.add('is-saindo');
    window.setTimeout(function () {
      if (toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
    }, 160);
  }

  function agendar(toast, tempo) {
    window.clearTimeout(Number(toast.getAttribute('data-timer')));
    var id = window.setTimeout(function () {
      remover(toast);
    }, tempo);
    toast.setAttribute('data-timer', String(id));
  }

  /** Mostra um toast de sucesso. O texto entra por textContent (nunca como HTML). */
  SH.toast = function (texto) {
    var area = areaDeToasts();
    if (!area || !texto) {
      return null;
    }
    while (area.children.length >= MAXIMO) {
      area.removeChild(area.firstElementChild);
    }

    var toast = document.createElement('div');
    toast.className = 'sh-toast';

    var icone = document.createElement('i');
    icone.className = 'bi bi-check-circle-fill';
    icone.setAttribute('aria-hidden', 'true');

    var corpo = document.createElement('div');
    corpo.className = 'sh-toast__texto';
    corpo.textContent = texto;

    var fechar = document.createElement('button');
    fechar.type = 'button';
    fechar.className = 'btn btn-ghost btn-sm';
    fechar.setAttribute('aria-label', 'Fechar mensagem');
    var x = document.createElement('i');
    x.className = 'bi bi-x-lg';
    x.setAttribute('aria-hidden', 'true');
    fechar.appendChild(x);
    fechar.addEventListener('click', function () {
      remover(toast);
    });

    toast.appendChild(icone);
    toast.appendChild(corpo);
    toast.appendChild(fechar);

    // pausa o relógio com o mouse ou o foco em cima do toast
    toast.addEventListener('mouseenter', function () {
      window.clearTimeout(Number(toast.getAttribute('data-timer')));
    });
    toast.addEventListener('mouseleave', function () {
      agendar(toast, DURACAO / 2);
    });
    toast.addEventListener('focusin', function () {
      window.clearTimeout(Number(toast.getAttribute('data-timer')));
    });
    toast.addEventListener('focusout', function () {
      agendar(toast, DURACAO / 2);
    });

    area.appendChild(toast);
    agendar(toast, DURACAO);
    return toast;
  };

  // Esc fecha o toast mais recente (só se não houver modal aberto, que usa o Esc para fechar)
  document.addEventListener('keydown', function (evento) {
    if (evento.key !== 'Escape' || document.body.classList.contains('modal-open')) {
      return;
    }
    var area = areaDeToasts();
    if (area && area.lastElementChild) {
      remover(area.lastElementChild);
    }
  });

  // Botão com data-sh-mostrar-toast="texto" mostra um toast (usado no guia de componentes)
  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-mostrar-toast]') : null;
    if (botao) {
      SH.toast(botao.getAttribute('data-sh-mostrar-toast'));
    }
  });

  // Converte as mensagens de sucesso que o servidor colocou na página
  var avisos = document.querySelectorAll('[data-sh-toast]');
  for (var i = 0; i < avisos.length; i++) {
    var texto = (avisos[i].textContent || '').replace(/\s+/g, ' ').trim();
    if (texto) {
      SH.toast(texto);
      if (avisos[i].parentNode) {
        avisos[i].parentNode.removeChild(avisos[i]);
      }
    }
  }
})();
