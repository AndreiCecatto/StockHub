/*
 * StockHub: telas de Clientes. Sem biblioteca. Sem este arquivo as telas funcionam do mesmo jeito (só sem máscara).
 *
 *   [data-mascara="documento"]  CPF (000.000.000-00) ou CNPJ (00.000.000/0000-00) conforme a quantidade de dígitos;
 *                               confere os dígitos verificadores e avisa na validação inline (js/formularios.js)
 *   [data-mascara="telefone"]   (00) 0000-0000 ou (00) 00000-0000
 *   [data-mascara="cep"]        00000-000
 *   form[data-cliente-filtros]  aplica os filtros ao mudar situação, OS ou ordem (a busca continua pelo botão ou Enter)
 *
 * O servidor refaz toda a validação e guarda só os dígitos; a máscara é conforto.
 */
(function () {
  'use strict';

  function digitos(texto) {
    return String(texto || '').replace(/\D/g, '');
  }

  function mascararDocumento(d) {
    d = d.slice(0, 14);
    if (d.length <= 11) {
      return d
        .replace(/^(\d{3})(\d)/, '$1.$2')
        .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
        .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3-$4');
    }
    return d
      .replace(/^(\d{2})(\d)/, '$1.$2')
      .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3/$4')
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, '$1.$2.$3/$4-$5');
  }

  function mascararTelefone(d) {
    d = d.slice(0, 11);
    if (d.length <= 2) {
      return d ? '(' + d : '';
    }
    if (d.length <= 6) {
      return '(' + d.slice(0, 2) + ') ' + d.slice(2);
    }
    if (d.length <= 10) {
      return '(' + d.slice(0, 2) + ') ' + d.slice(2, 6) + '-' + d.slice(6);
    }
    return '(' + d.slice(0, 2) + ') ' + d.slice(2, 7) + '-' + d.slice(7);
  }

  function mascararCep(d) {
    d = d.slice(0, 8);
    return d.length > 5 ? d.slice(0, 5) + '-' + d.slice(5) : d;
  }

  var MASCARAS = { documento: mascararDocumento, telefone: mascararTelefone, cep: mascararCep };

  var PESOS = {
    cpf1: [10, 9, 8, 7, 6, 5, 4, 3, 2],
    cpf2: [11, 10, 9, 8, 7, 6, 5, 4, 3, 2],
    cnpj1: [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2],
    cnpj2: [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
  };

  function digitoVerificador(d, pesos) {
    var soma = 0;
    for (var i = 0; i < pesos.length; i++) {
      soma += Number(d.charAt(i)) * pesos[i];
    }
    var resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  }

  function documentoValido(d) {
    if (/^(\d)\1+$/.test(d)) {
      return false;
    }
    if (d.length === 11) {
      return digitoVerificador(d, PESOS.cpf1) === Number(d.charAt(9)) && digitoVerificador(d, PESOS.cpf2) === Number(d.charAt(10));
    }
    if (d.length === 14) {
      return digitoVerificador(d, PESOS.cnpj1) === Number(d.charAt(12)) && digitoVerificador(d, PESOS.cnpj2) === Number(d.charAt(13));
    }
    return false;
  }

  function conferirDocumento(campo) {
    var d = digitos(campo.value);
    if (d === '' || campo.readOnly) {
      campo.setCustomValidity('');
    } else if (d.length !== 11 && d.length !== 14) {
      campo.setCustomValidity('O CPF tem 11 números e o CNPJ tem 14. Confira ou deixe em branco.');
    } else if (!documentoValido(d)) {
      campo.setCustomValidity('Este ' + (d.length === 11 ? 'CPF' : 'CNPJ') + ' não é válido. Confira os números.');
    } else {
      campo.setCustomValidity('');
    }
  }

  function ligar(campo) {
    var tipo = campo.getAttribute('data-mascara');
    var mascarar = MASCARAS[tipo];
    if (!mascarar || campo.readOnly) {
      return;
    }
    campo.value = mascarar(digitos(campo.value));
    if (tipo === 'documento') {
      conferirDocumento(campo);
    }
    campo.addEventListener('input', function () {
      campo.value = mascarar(digitos(campo.value));
      if (tipo === 'documento') {
        conferirDocumento(campo);
      }
    });
  }

  function iniciar() {
    Array.prototype.forEach.call(document.querySelectorAll('[data-mascara]'), ligar);

    // Documento já gravado (campo só de leitura) também aparece com pontuação
    Array.prototype.forEach.call(document.querySelectorAll('[data-mascara="documento"][readonly]'), function (campo) {
      campo.value = mascararDocumento(digitos(campo.value));
    });

    // Tipo de pessoa acompanha o documento digitado (o servidor confere de novo)
    var doc = document.getElementById('documento');
    if (doc && !doc.readOnly) {
      doc.addEventListener('input', function () {
        var d = digitos(doc.value);
        var alvo = d.length === 11 ? 'tipo-fisica' : d.length === 14 ? 'tipo-juridica' : '';
        var radio = alvo ? document.getElementById(alvo) : null;
        if (radio && !radio.checked) {
          radio.checked = true;
        }
      });
    }

    var filtros = document.querySelector('form[data-cliente-filtros]');
    if (filtros) {
      filtros.addEventListener('change', function (evento) {
        if (evento.target && evento.target.name !== 'busca') {
          filtros.submit();
        }
      });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
  } else {
    iniciar();
  }
})();
