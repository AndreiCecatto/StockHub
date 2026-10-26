/*
 * StockHub: confirmação de exclusão (seção 4.7 de docs/design-system.md). Substitui o js/app.js antigo.
 * Contrato mantido: todo <form data-confirmar-exclusao data-tipo="o produto" data-nome="..." data-aviso="...">
 * abre o modal #sh-modal-exclusao e só é enviado (com o token CSRF que ele já tem) depois do clique em
 * "Excluir". O foco inicial vai para "Cancelar" (a ação segura). Sem JavaScript, o formulário envia direto.
 * Tudo entra por textContent: o nome do item nunca é interpretado como HTML.
 */
(function () {
  'use strict';

  var modalEl = document.getElementById('sh-modal-exclusao');
  if (!modalEl || typeof window.bootstrap === 'undefined') {
    return;
  }

  var modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
  var tituloEl = document.getElementById('sh-modal-exclusao-titulo');
  var tipoEl = document.getElementById('sh-modal-exclusao-tipo');
  var nomeEl = document.getElementById('sh-modal-exclusao-nome');
  var avisoEl = document.getElementById('sh-modal-exclusao-aviso');
  var cancelar = document.getElementById('sh-modal-exclusao-cancelar');
  var confirmar = document.getElementById('sh-modal-exclusao-confirmar');
  var rotuloConfirmar = document.getElementById('sh-modal-exclusao-rotulo');
  var formPendente = null;
  var gatilho = null;

  // "o produto" -> "produto" (para o título "Excluir produto?" e o botão "Excluir produto")
  function semArtigo(tipo) {
    return tipo.replace(/^(o|a|os|as)\s+/i, '');
  }

  document.addEventListener('submit', function (evento) {
    var form = evento.target;
    if (!form.matches || !form.matches('form[data-confirmar-exclusao]')) {
      return;
    }
    evento.preventDefault();
    formPendente = form;
    gatilho = document.activeElement;
    var tipo = form.getAttribute('data-tipo') || 'o item';
    var nucleo = semArtigo(tipo);
    tituloEl.textContent = 'Excluir ' + nucleo + '?';
    tipoEl.textContent = tipo;
    nomeEl.textContent = form.getAttribute('data-nome') || '';
    avisoEl.textContent = form.getAttribute('data-aviso') || '';
    rotuloConfirmar.textContent = 'Excluir ' + nucleo;
    modal.show();
  });

  modalEl.addEventListener('shown.bs.modal', function () {
    if (cancelar) {
      cancelar.focus();
    }
  });

  confirmar.addEventListener('click', function () {
    if (!formPendente) {
      return;
    }
    confirmar.disabled = true; // evita duplo clique
    confirmar.setAttribute('aria-busy', 'true');
    rotuloConfirmar.textContent = 'Excluindo…';
    formPendente.submit();
  });

  modalEl.addEventListener('hidden.bs.modal', function () {
    confirmar.disabled = false;
    confirmar.removeAttribute('aria-busy');
    formPendente = null;
    // devolve o foco ao botão que abriu o modal (acessibilidade por teclado)
    if (gatilho && gatilho.focus) {
      gatilho.focus();
    }
  });
})();
