/*
 * StockHub: busca tolerante a erro de digitação (cópia da regra de util/BuscaDifusa.java; ao mudar uma, mude a outra).
 * Uma palavra digitada casa com uma palavra do nome quando é o começo dela, ou, com 3 letras ou mais, fica a poucas
 * edições de distância dela ou de um começo dela (troca, falta, sobra ou inversão de letras). Todas as palavras
 * digitadas precisam casar. Ignora caixa e acento.
 *
 * API: SH.buscaDifusa.pontuar(termo, nome) -> 0 (não casa) ou nota de 0 a 1.
 * Usada pelo seletor de produto (js/combobox-produto.js), só quando nenhum nome contém o texto digitado.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var MARCAS = /[̀-ͯ]/g;

  function palavras(texto) {
    return String(texto || '')
      .normalize('NFD')
      .replace(MARCAS, '')
      .toLowerCase()
      .split(/[^a-z0-9]+/)
      .filter(Boolean);
  }

  /** Distância de edição com inversão de letras vizinhas (optimal string alignment). */
  function distancia(a, b) {
    var d = [];
    var i;
    var j;
    for (i = 0; i <= a.length; i++) {
      d[i] = [i];
    }
    for (j = 0; j <= b.length; j++) {
      d[0][j] = j;
    }
    for (i = 1; i <= a.length; i++) {
      for (j = 1; j <= b.length; j++) {
        var custo = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
        d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + custo);
        if (i > 1 && j > 1 && a.charAt(i - 1) === b.charAt(j - 2) && a.charAt(i - 2) === b.charAt(j - 1)) {
          d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
        }
      }
    }
    return d[a.length][b.length];
  }

  function notaDaPalavra(digitada, palavra) {
    if (palavra === digitada) {
      return 1;
    }
    if (palavra.indexOf(digitada) === 0) {
      return 0.95;
    }
    var tamanho = digitada.length;
    if (tamanho < 3) {
      return 0;
    }
    var permitido = tamanho <= 4 ? 1 : 2;
    var menor = distancia(digitada, palavra);
    for (var corte = Math.max(1, tamanho - 1); corte <= Math.min(palavra.length, tamanho + 1); corte++) {
      menor = Math.min(menor, distancia(digitada, palavra.substring(0, corte)));
    }
    return menor <= permitido ? 1 - menor / (tamanho + 1) : 0;
  }

  function pontuar(termo, nome) {
    var digitadas = palavras(termo);
    var doNome = palavras(nome);
    if (!digitadas.length || !doNome.length) {
      return 0;
    }
    var soma = 0;
    for (var i = 0; i < digitadas.length; i++) {
      var melhor = 0;
      for (var j = 0; j < doNome.length; j++) {
        melhor = Math.max(melhor, notaDaPalavra(digitadas[i], doNome[j]));
      }
      if (melhor === 0) {
        return 0;
      }
      soma += melhor;
    }
    return soma / digitadas.length;
  }

  SH.buscaDifusa = { pontuar: pontuar };
})();
