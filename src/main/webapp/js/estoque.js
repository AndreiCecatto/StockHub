/*
 * Tela de movimentacoes: validacao e rotulos conforme o tipo escolhido (RN-05).
 *  - ENTRADA / SAIDA: quantidade de 1 ate 1.000.000.
 *  - AJUSTE: informa a contagem final (aceita 0) e a observacao passa a ser obrigatoria.
 * A validacao definitiva continua no servidor; aqui e so ajuda ao usuario.
 */
(function () {
    'use strict';

    var tipo = document.getElementById('tipo');
    var quantidade = document.getElementById('quantidade');
    var observacao = document.getElementById('observacao');
    var rotuloQuantidade = document.getElementById('rotulo-quantidade');
    var dicaQuantidade = document.getElementById('dica-quantidade');
    var marcaObrigatoria = document.getElementById('obs-obrigatoria');
    if (!tipo || !quantidade || !observacao) {
        return;
    }

    function atualizar() {
        var ajuste = tipo.value === 'AJUSTE';
        quantidade.min = ajuste ? '0' : '1';
        quantidade.max = ajuste ? '2000000000' : '1000000';
        observacao.required = ajuste;
        if (rotuloQuantidade) {
            rotuloQuantidade.textContent = ajuste ? 'Contagem final' : 'Quantidade';
        }
        if (dicaQuantidade) {
            dicaQuantidade.textContent = ajuste
                ? 'Saldo que o produto deve ter depois do ajuste (pode ser 0).'
                : 'Mínimo 1 unidade.';
        }
        if (marcaObrigatoria) {
            marcaObrigatoria.hidden = !ajuste;
        }
    }

    tipo.addEventListener('change', atualizar);
    atualizar();
})();
