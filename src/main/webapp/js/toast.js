/*
 * StockHub v3: toasts sérios, com ação opcional "Desfazer".
 *
 * As páginas continuam mostrando a mensagem de sucesso no HTML (funciona sem JS e é escapada na JSP).
 * Com JS, todo elemento com data-sh-toast vira um toast e sai da página. Erros do servidor NÃO viram toast:
 * ficam na página, persistentes (WCAG 2.2.1).
 * O contêiner #sh-toasts tem aria-live="polite" desde o carregamento, então o texto novo é anunciado sem tirar o foco
 * de onde o usuário está. Dura 7 s (10 s com ação), pausa com mouse ou foco e retoma de onde parou, fecha com o botão
 * ou Esc. Máximo de 3 empilhados.
 *
 * API: SH.toast(texto, opcoes?) -> elemento
 *   opcoes.tom      "sucesso" (padrão) | "info" | "aviso" | "erro" (o de erro fica até ser fechado)
 *   opcoes.acao     { rotulo: "Desfazer", aoClicar: function () {...} }
 *   opcoes.desfazer function () {...}   atalho para acao com o rótulo "Desfazer"
 *   opcoes.duracao  milissegundos (0 = não fecha sozinho)
 *   O texto entra por textContent (nunca como HTML).
 * Declarativo (mensagem do servidor): <div data-sh-toast data-sh-toast-tom="info"
 *     data-sh-toast-desfazer="#form-desfazer" data-sh-toast-acao-rotulo="Desfazer"> texto </div>
 *   O seletor aponta para um <form method="post"> (com o token CSRF) que fica FORA do toast; a ação o envia.
 * Botão com data-sh-mostrar-toast="texto" mostra um toast (usado no guia de componentes).
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var DURACAO = 7000;
  var DURACAO_COM_ACAO = 10000;
  var MAXIMO = 3;

  var ICONES = {
    sucesso: 'bi-check-circle-fill',
    info: 'bi-info-circle-fill',
    aviso: 'bi-exclamation-triangle-fill',
    erro: 'bi-x-octagon-fill'
  };

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
    }, 180);
  }

  // O relógio de cada toast guarda quanto falta; pausar e retomar não "reinicia" a contagem
  function agendar(toast, tempo) {
    window.clearTimeout(Number(toast.getAttribute('data-timer')));
    if (!(tempo > 0)) {
      return;
    }
    toast.setAttribute('data-inicio', String(Date.now()));
    toast.setAttribute('data-restante', String(tempo));
    var id = window.setTimeout(function () {
      remover(toast);
    }, tempo);
    toast.setAttribute('data-timer', String(id));
  }

  function pausar(toast) {
    var restante = Number(toast.getAttribute('data-restante')) || 0;
    if (!restante) {
      return;
    }
    window.clearTimeout(Number(toast.getAttribute('data-timer')));
    var gasto = Date.now() - Number(toast.getAttribute('data-inicio'));
    toast.setAttribute('data-restante', String(Math.max(1500, restante - gasto)));
  }

  function retomar(toast) {
    var restante = Number(toast.getAttribute('data-restante')) || 0;
    if (restante) {
      agendar(toast, restante);
    }
  }

  /** Mostra um toast. O texto entra por textContent (nunca como HTML). */
  SH.toast = function (texto, opcoes) {
    var area = areaDeToasts();
    if (!area || !texto) {
      return null;
    }
    var o = opcoes || {};
    var tom = ICONES[o.tom] ? o.tom : 'sucesso';
    var acao = o.acao || (typeof o.desfazer === 'function' ? { rotulo: 'Desfazer', aoClicar: o.desfazer } : null);
    var duracao = typeof o.duracao === 'number' ? o.duracao : tom === 'erro' ? 0 : acao ? DURACAO_COM_ACAO : DURACAO;

    while (area.children.length >= MAXIMO) {
      area.removeChild(area.firstElementChild);
    }

    var toast = document.createElement('div');
    toast.className = 'sh-toast' + (tom === 'sucesso' ? '' : ' sh-toast--' + tom);

    var icone = document.createElement('i');
    icone.className = 'bi ' + ICONES[tom];
    icone.setAttribute('aria-hidden', 'true');

    var corpo = document.createElement('div');
    corpo.className = 'sh-toast__texto';
    corpo.textContent = texto;

    toast.appendChild(icone);
    toast.appendChild(corpo);

    if (acao && typeof acao.aoClicar === 'function') {
      var botaoAcao = document.createElement('button');
      botaoAcao.type = 'button';
      botaoAcao.className = 'sh-toast__acao';
      botaoAcao.textContent = acao.rotulo || 'Desfazer';
      botaoAcao.addEventListener('click', function () {
        remover(toast);
        acao.aoClicar();
      });
      toast.appendChild(botaoAcao);
    }

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
    toast.appendChild(fechar);

    if (duracao > 0) {
      var tempo = document.createElement('span');
      tempo.className = 'sh-toast__tempo';
      tempo.setAttribute('aria-hidden', 'true');
      if (duracao >= 10000) {
        toast.classList.add('sh-toast--longo'); // fio de 10 s (o padrão do CSS é 7 s); sem style inline
      }
      toast.appendChild(tempo);
    }

    toast.addEventListener('mouseenter', function () {
      pausar(toast);
    });
    toast.addEventListener('mouseleave', function () {
      retomar(toast);
    });
    toast.addEventListener('focusin', function () {
      pausar(toast);
    });
    toast.addEventListener('focusout', function () {
      retomar(toast);
    });

    area.appendChild(toast);
    agendar(toast, duracao);
    return toast;
  };

  // Esc fecha o toast mais recente (só se não houver modal aberto, que usa o Esc para fechar)
  document.addEventListener('keydown', function (evento) {
    if (evento.key !== 'Escape' || document.body.classList.contains('modal-open') || document.querySelector('dialog[open]')) {
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

  // Converte as mensagens que o servidor colocou na página
  var avisos = document.querySelectorAll('[data-sh-toast]');
  for (var i = 0; i < avisos.length; i++) {
    var aviso = avisos[i];
    var texto = (aviso.textContent || '').replace(/\s+/g, ' ').trim();
    if (!texto) {
      continue;
    }
    var opcoes = { tom: aviso.getAttribute('data-sh-toast-tom') || 'sucesso' };
    var alvoForm = aviso.getAttribute('data-sh-toast-desfazer');
    if (alvoForm) {
      var form;
      try {
        form = document.querySelector(alvoForm);
      } catch (erro) {
        form = null;
      }
      if (form && form.tagName === 'FORM') {
        opcoes.acao = {
          rotulo: aviso.getAttribute('data-sh-toast-acao-rotulo') || 'Desfazer',
          aoClicar: (function (f) {
            return function () {
              if (f.requestSubmit) {
                f.requestSubmit();
              } else {
                f.submit();
              }
            };
          })(form)
        };
      }
    }
    SH.toast(texto, opcoes);
    if (aviso.parentNode) {
      aviso.parentNode.removeChild(aviso);
    }
  }
})();
