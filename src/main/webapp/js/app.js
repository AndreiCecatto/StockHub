/*
 * Scripts comuns do StockHub.
 * Confirmacao de exclusao: todo <form data-confirmar-exclusao> abre o modal do Bootstrap
 * (#modalExclusao) com o nome do item. O form so e enviado (com o token CSRF que ele ja tem)
 * depois do clique em "Excluir". Sem JavaScript, o form envia direto (comportamento padrao).
 */
(function () {
    'use strict';

    var modalEl = document.getElementById('modalExclusao');
    if (!modalEl || typeof bootstrap === 'undefined') {
        return;
    }

    var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    var tipoEl = document.getElementById('modalExclusaoTipo');
    var nomeEl = document.getElementById('modalExclusaoNome');
    var avisoEl = document.getElementById('modalExclusaoAviso');
    var confirmar = document.getElementById('modalExclusaoConfirmar');
    var formPendente = null;
    var gatilho = null;

    document.addEventListener('submit', function (evento) {
        var form = evento.target;
        if (!form.matches || !form.matches('form[data-confirmar-exclusao]')) {
            return;
        }
        evento.preventDefault();
        formPendente = form;
        gatilho = document.activeElement;
        // textContent: o nome do item nunca e interpretado como HTML
        tipoEl.textContent = form.getAttribute('data-tipo') || 'o item';
        nomeEl.textContent = form.getAttribute('data-nome') || '';
        avisoEl.textContent = form.getAttribute('data-aviso') || '';
        modal.show();
    });

    confirmar.addEventListener('click', function () {
        if (formPendente) {
            confirmar.disabled = true; // evita duplo clique
            formPendente.submit();
        }
    });

    modalEl.addEventListener('hidden.bs.modal', function () {
        confirmar.disabled = false;
        formPendente = null;
        // devolve o foco ao botao que abriu o modal (acessibilidade por teclado)
        if (gatilho && gatilho.focus) {
            gatilho.focus();
        }
    });
})();
