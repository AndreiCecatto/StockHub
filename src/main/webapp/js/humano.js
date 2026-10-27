/*
 * StockHub v2: voz humana e vida no mascote (carregado com defer em todas as telas).
 *  - [data-sh-saudacao]  troca o texto por "Bom dia", "Boa tarde" ou "Boa noite" conforme a hora do aparelho;
 *  - [data-sh-data-hoje] escreve a data de hoje por extenso ("segunda-feira, 26 de outubro");
 *  - time[data-relativo] mostra datas relativas ("há 2 h", "ontem") a partir do atributo datetime, e atualiza a cada minuto;
 *  - [data-sh-mascote-btn]  o mascote reage ao clique (pula, acena) e o balão de fala [data-sh-fala] troca de frase;
 *  - [data-sh-olhar]        as pupilas do mascote seguem o cursor (só com movimento permitido);
 *  - [data-sh-festa]        confete de papel discreto (uma vez por visita ao painel, só com movimento permitido).
 * O texto que a JSP escreve é o plano B (sem JavaScript continua correto, só menos personalizado).
 * Tudo por textContent; nunca monta HTML a partir de dado.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var reduz = function () {
    return SH.reduzMovimento ? SH.reduzMovimento() : false;
  };

  /* ---------- Saudação e data ---------- */
  SH.saudacao = function (agora) {
    var hora = (agora || new Date()).getHours();
    if (hora >= 5 && hora < 12) {
      return 'Bom dia';
    }
    if (hora >= 12 && hora < 18) {
      return 'Boa tarde';
    }
    return 'Boa noite';
  };

  var saudacoes = document.querySelectorAll('[data-sh-saudacao]');
  for (var i = 0; i < saudacoes.length; i++) {
    saudacoes[i].textContent = SH.saudacao();
  }

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

  /* ---------- Mascote: reação ao clique ---------- */
  var FRASES = {
    contente: ['Conferi tudo hoje!', 'Prateleira em dia.', 'Hoje o balcão está tranquilo.', 'Bom trabalho!'],
    atenta: ['De olho no estoque.', 'Esse aqui está no fim.', 'Daqui eu vigio.', 'Vamos repor?'],
    preocupada: ['Ai, ai... falta coisa.', 'Dá para repor hoje?', 'Tem prateleira vazia!', 'Me ajuda a repor?'],
    dormindo: ['Zzz... acorda quando tiver produto.', 'Só mais cinco minutinhos...', 'Zzz...']
  };

  function pular(mascote) {
    if (!mascote || reduz()) {
      return;
    }
    mascote.classList.remove('is-pula');
    // reinicia a animação mesmo com cliques seguidos
    void mascote.getBoundingClientRect();
    mascote.classList.add('is-pula');
    window.setTimeout(function () {
      mascote.classList.remove('is-pula');
    }, 700);
  }

  function acenar(mascote) {
    if (!mascote || reduz()) {
      return;
    }
    mascote.classList.add('is-festa');
    window.setTimeout(function () {
      mascote.classList.remove('is-festa');
    }, 1600);
  }

  SH.confete = function (area) {
    if (!area || reduz()) {
      return;
    }
    area.textContent = '';
    for (var n = 0; n < 12; n++) {
      var papel = document.createElement('span');
      papel.className = 'sh-confete';
      area.appendChild(papel);
    }
    window.setTimeout(function () {
      area.textContent = '';
    }, 1900);
  };

  SH.pularMascote = pular;

  var indiceFala = {};

  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-mascote-btn]') : null;
    if (!botao) {
      return;
    }
    var mascote = botao.querySelector('.sh-mascote');
    pular(mascote);
    var estado = mascote ? mascote.getAttribute('data-estado') : 'contente';
    var bloco = botao.parentNode;
    var fala = bloco ? bloco.querySelector('[data-sh-fala]') : null;
    if (fala && FRASES[estado]) {
      indiceFala[estado] = ((indiceFala[estado] || 0) + 1) % FRASES[estado].length;
      fala.classList.remove('is-trocando');
      void fala.getBoundingClientRect();
      fala.textContent = FRASES[estado][indiceFala[estado]];
      fala.classList.add('is-trocando');
    }
    if (estado === 'contente') {
      acenar(mascote);
      SH.confete(bloco ? bloco.querySelector('[data-sh-festa]') : null);
    }
  });

  /* ---------- Mascote: olhar o cursor ---------- */
  var olhadores = document.querySelectorAll('[data-sh-olhar] .sh-mascote, .sh-mascote[data-sh-olhar]');
  if (olhadores.length && !reduz() && window.requestAnimationFrame) {
    var pendente = false;
    var ultimo = null;
    var mover = function () {
      pendente = false;
      if (!ultimo) {
        return;
      }
      for (var k = 0; k < olhadores.length; k++) {
        var caixa = olhadores[k].getBoundingClientRect();
        var cx = caixa.left + caixa.width / 2;
        var cy = caixa.top + caixa.height * 0.62;
        var dx = ultimo.x - cx;
        var dy = ultimo.y - cy;
        var dist = Math.sqrt(dx * dx + dy * dy) || 1;
        var forca = Math.min(1, dist / 220);
        olhadores[k].style.setProperty('--mc-px', ((dx / dist) * 3.2 * forca).toFixed(2));
        olhadores[k].style.setProperty('--mc-py', ((dy / dist) * 2.6 * forca).toFixed(2));
      }
    };
    document.addEventListener(
      'pointermove',
      function (evento) {
        if (evento.pointerType === 'touch') {
          return;
        }
        ultimo = { x: evento.clientX, y: evento.clientY };
        if (!pendente) {
          pendente = true;
          window.requestAnimationFrame(mover);
        }
      },
      { passive: true }
    );
  }

  /* ---------- Celebração automática (nada a repor): uma vez por aba ---------- */
  var festa = document.querySelector('[data-sh-festa-auto]');
  if (festa && !reduz()) {
    var jaFez = SH.armazenar ? SH.armazenar.ler('sh-festa-vista') : null;
    var viuNaAba = false;
    try {
      viuNaAba = window.sessionStorage.getItem('sh-festa-vista') === '1';
    } catch (erro) {
      viuNaAba = false;
    }
    if (!viuNaAba && jaFez !== 'sempre-ignorar') {
      window.setTimeout(function () {
        var mascote = festa.parentNode.querySelector('.sh-mascote');
        pular(mascote);
        acenar(mascote);
        SH.confete(festa);
      }, 900);
      try {
        window.sessionStorage.setItem('sh-festa-vista', '1');
      } catch (erro2) {
        // sem sessionStorage: comemora de novo na próxima visita, sem problema
      }
    }
  }
})();
