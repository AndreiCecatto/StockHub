/*
 * StockHub v2: vida nas telas de entrada (login e cadastro).
 *  - o mascote fecha os olhos enquanto a senha está sendo digitada escondida, e espia quando ela está visível;
 *  - ao digitar o e-mail ou o nome, ele olha para baixo, na direção do texto;
 *  - ao enviar, ele pula.
 * Tudo é decoração: sem JavaScript o formulário funciona igual. Respeita prefers-reduced-motion.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var form = document.querySelector('.sh-auth form');
  var mascotes = document.querySelectorAll('.sh-auth .sh-mascote');
  if (!form || !mascotes.length) {
    return;
  }

  var senha = document.getElementById('senha');
  var estadoOriginal = mascotes[0].getAttribute('data-estado');

  function marcar(nome, valor) {
    for (var i = 0; i < mascotes.length; i++) {
      if (valor === null) {
        mascotes[i].removeAttribute(nome);
      } else {
        mascotes[i].setAttribute(nome, valor);
      }
    }
  }

  function olhar(px, py) {
    for (var i = 0; i < mascotes.length; i++) {
      mascotes[i].style.setProperty('--mc-px', String(px));
      mascotes[i].style.setProperty('--mc-py', String(py));
    }
  }

  function atualizarOlhosDaSenha() {
    if (!senha) {
      return;
    }
    var ativo = document.activeElement;
    var focada = ativo === senha || (ativo && ativo.hasAttribute && ativo.hasAttribute('data-sh-senha-alternar'));
    var visivel = senha.type === 'text';
    if (focada && !visivel) {
      marcar('data-olhos', 'fechados');
    } else if (focada && visivel) {
      marcar('data-olhos', null);
      marcar('data-estado', 'atenta'); // espiando
      olhar(-2, 1.5);
      return;
    } else {
      marcar('data-olhos', null);
    }
    marcar('data-estado', estadoOriginal);
  }

  if (senha) {
    senha.addEventListener('focus', atualizarOlhosDaSenha);
    senha.addEventListener('blur', function () {
      window.setTimeout(atualizarOlhosDaSenha, 0);
    });
    document.addEventListener('click', function (evento) {
      if (evento.target.closest && evento.target.closest('[data-sh-senha-alternar]')) {
        window.setTimeout(atualizarOlhosDaSenha, 0);
      }
    });
    var botao = document.querySelector('[data-sh-senha-alternar]');
    if (botao) {
      botao.addEventListener('blur', function () {
        window.setTimeout(atualizarOlhosDaSenha, 0);
      });
    }
  }

  // Olhar para o texto enquanto digita
  var campos = form.querySelectorAll('#nome, #email');
  for (var c = 0; c < campos.length; c++) {
    campos[c].addEventListener('input', function (evento) {
      var tam = evento.target.value.length;
      var px = Math.max(-3, Math.min(3, (tam / 24) * 6 - 3));
      olhar(px.toFixed(2), 2.4);
    });
  }

  form.addEventListener('submit', function () {
    if (SH.pularMascote) {
      for (var i = 0; i < mascotes.length; i++) {
        SH.pularMascote(mascotes[i]);
      }
    }
  });
})();
