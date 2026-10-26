<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Modal de confirmação de exclusão do novo design. Contrato mantido: js/confirmar-exclusao.js intercepta todo
    <form data-confirmar-exclusao data-tipo="o produto" data-nome="..." data-aviso="..."> e só envia o
    formulário (que já leva o token CSRF) depois do clique em "Excluir". Foco inicial em "Cancelar".
    Os textos abaixo são preenchidos pelo JS com textContent.
--%>
<div class="modal fade" id="sh-modal-exclusao" tabindex="-1" aria-labelledby="sh-modal-exclusao-titulo" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title" id="sh-modal-exclusao-titulo">Excluir item?</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <p class="mb-2">Deseja excluir <span id="sh-modal-exclusao-tipo">o item</span>
                    <strong id="sh-modal-exclusao-nome"></strong>?</p>
                <p class="mb-0 sh-muted" id="sh-modal-exclusao-aviso"></p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal"
                    id="sh-modal-exclusao-cancelar">Cancelar</button>
                <button type="button" class="btn btn-danger" id="sh-modal-exclusao-confirmar">
                    <i class="bi bi-trash3" aria-hidden="true"></i>
                    <span id="sh-modal-exclusao-rotulo">Excluir</span>
                </button>
            </div>
        </div>
    </div>
</div>
