<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Modal de confirmacao de exclusao. O js/app.js intercepta todo form com
    data-confirmar-exclusao, preenche o nome do item e so envia o form (que ja leva o token CSRF)
    depois do clique em "Excluir".
--%>
<div class="modal fade" id="modalExclusao" tabindex="-1" aria-labelledby="modalExclusaoTitulo" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title fs-5" id="modalExclusaoTitulo">Confirmar exclusão</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <p class="mb-2">Deseja excluir <span id="modalExclusaoTipo">o item</span>
                    <strong id="modalExclusaoNome"></strong>?</p>
                <p class="mb-0 text-secondary small" id="modalExclusaoAviso"></p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="button" class="btn btn-danger" id="modalExclusaoConfirmar">
                    <i class="bi bi-trash" aria-hidden="true"></i> Excluir
                </button>
            </div>
        </div>
    </div>
</div>
