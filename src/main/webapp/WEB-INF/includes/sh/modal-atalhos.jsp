<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Modal de ajuda dos atalhos de teclado (tecla ?). A chave "Atalhos de teclado ativados" é tratada por
    js/atalhos.js (grava em localStorage). Atalhos de uma tecla só valem fora de campos de texto.
--%>
<div class="modal fade" id="sh-modal-atalhos" tabindex="-1" aria-labelledby="sh-modal-atalhos-titulo" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="modal-title" id="sh-modal-atalhos-titulo">Atalhos de teclado</h2>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body">
                <dl class="sh-atalhos">
                    <dt><kbd class="sh-tecla">Ctrl</kbd> + <kbd class="sh-tecla">K</kbd> (no Mac, <kbd class="sh-tecla">&#8984;</kbd> + <kbd class="sh-tecla">K</kbd>)</dt>
                    <dd>Paleta de comandos: buscar produto, ir para uma tela ou fazer uma ação (vale até digitando)</dd>
                    <dt><kbd class="sh-tecla">/</kbd></dt>
                    <dd>Buscar (na lista, no campo da lista; nas outras telas, abre a paleta de comandos)</dd>
                    <dt><kbd class="sh-tecla">n</kbd></dt>
                    <dd>Novo produto (em Categorias, nova categoria)</dd>
                    <dt><kbd class="sh-tecla">m</kbd></dt>
                    <dd>Movimentar estoque</dd>
                    <dt><kbd class="sh-tecla">E</kbd> / <kbd class="sh-tecla">S</kbd> / <kbd class="sh-tecla">A</kbd></dt>
                    <dd>Em Movimentações, escolher o tipo: Entrada, Saída ou Ajuste</dd>
                    <dt><kbd class="sh-tecla">F</kbd></dt>
                    <dd>Em Movimentações, filtrar o livro-razão</dd>
                    <dt><kbd class="sh-tecla">J</kbd> / <kbd class="sh-tecla">K</kbd> e <kbd class="sh-tecla">Enter</kbd></dt>
                    <dd>Nas listas, descer e subir entre as linhas; Enter abre o item</dd>
                    <dt><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">i</kbd></dt>
                    <dd>Ir para Início</dd>
                    <dt><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">p</kbd></dt>
                    <dd>Ir para Produtos</dd>
                    <dt><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">c</kbd></dt>
                    <dd>Ir para Categorias</dd>
                    <dt><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">b</kbd></dt>
                    <dd>Ir para Estoque baixo</dd>
                    <dt><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">e</kbd></dt>
                    <dd>Ir para Movimentações</dd>
                    <dt><kbd class="sh-tecla">g</kbd> depois <kbd class="sh-tecla">r</kbd></dt>
                    <dd>Ir para Valor do estoque</dd>
                    <dt><kbd class="sh-tecla">?</kbd></dt>
                    <dd>Abrir esta ajuda</dd>
                    <dt><kbd class="sh-tecla">Esc</kbd></dt>
                    <dd>Fechar menu, janela ou aviso; no campo de busca, limpar o texto</dd>
                    <dt><kbd class="sh-tecla">Ctrl</kbd> + <kbd class="sh-tecla">Enter</kbd></dt>
                    <dd>Registrar a movimentação, mesmo de dentro da observação</dd>
                </dl>
                <hr>
                <div class="form-check form-switch mb-0">
                    <input class="form-check-input" type="checkbox" role="switch" id="sh-atalhos-ativos" checked>
                    <label class="form-check-label" for="sh-atalhos-ativos">Atalhos de teclado ativados</label>
                </div>
                <p class="form-text mb-0">Os atalhos de uma tecla não funcionam enquanto você digita em um campo.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" data-bs-dismiss="modal">Fechar</button>
            </div>
        </div>
    </div>
</div>
