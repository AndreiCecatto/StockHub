/*
 * StockHub v3: datas e horários legíveis (carregado com defer em todas as telas).
 *  - [data-sh-data-hoje] escreve a data de hoje por extenso ("segunda-feira, 26 de outubro");
 *  - time[data-relativo] mostra datas relativas ("há 2 h", "ontem") a partir do atributo datetime, e atualiza a cada minuto.
 * O texto que a JSP escreve é o plano B (sem JavaScript continua correto, só menos refinado).
 * API: SH.relativo(quando) -> "há 2 h". Tudo por textContent; nunca monta HTML a partir de dado.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});

  /* ---------- Data de hoje ---------- */
  var datas = document.querySelectorAll('[data-sh-data-hoje]');
  if (datas.length && window.Intl && window.Intl.DateTimeFormat) {
    var texto = new window.Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
    for (var d = 0; d < datas.length; d++) {
      datas[d].textContent = texto;
    }
  }

  /* ---------- Datas relativas ---------- */
  var relativo = window.Intl && window.Intl.RelativeTimeFormat
    ? new window.Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto', style: 'short' })
    : null;

  /** "há 2 h", "ontem", "há 3 dias". Aceita Date ou texto ISO. */
  SH.relativo = function (quando, agora) {
    var alvo = quando instanceof Date ? quando : new Date(quando);
    if (isNaN(alvo.getTime())) {
      return '';
    }
    var base = agora || new Date();
    var segundos = Math.round((alvo.getTime() - base.getTime()) / 1000);
    var abs = Math.abs(segundos);
    if (!relativo) {
      return alvo.toLocaleDateString('pt-BR');
    }
    if (abs < 45) {
      return 'agora há pouco';
    }
    if (abs < 3600) {
      return relativo.format(Math.round(segundos / 60), 'minute');
    }
    if (abs < 86400) {
      return relativo.format(Math.round(segundos / 3600), 'hour');
    }
    if (abs < 86400 * 7) {
      return relativo.format(Math.round(segundos / 86400), 'day');
    }
    return alvo.toLocaleDateString('pt-BR', { day: 'numeric', month: 'short' });
  };

  function atualizarRelativos() {
    var itens = document.querySelectorAll('time[data-relativo]');
    for (var t = 0; t < itens.length; t++) {
      var valor = itens[t].getAttribute('datetime');
      var dito = valor ? SH.relativo(valor) : '';
      if (dito) {
        if (!itens[t].getAttribute('title')) {
          itens[t].setAttribute('title', new Date(valor).toLocaleString('pt-BR'));
        }
        itens[t].textContent = dito;
      }
    }
  }

  if (document.querySelector('time[data-relativo]')) {
    atualizarRelativos();
    window.setInterval(atualizarRelativos, 60000);
  }
})();
