/*
 * StockHub: marca o <html> com a classe "nfe-js" quando há JavaScript (síncrono, no head, como tema-init.js).
 * css/nfe.css usa a classe para: mostrar a dica "arraste o arquivo" e esconder os painéis de ação que não estão
 * escolhidos na revisão da nota. Sem JavaScript, todos os painéis ficam visíveis e o formulário continua enviando.
 */
document.documentElement.classList.add('nfe-js');
