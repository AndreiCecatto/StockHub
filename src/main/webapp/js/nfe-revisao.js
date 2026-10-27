/*
 * StockHub: revisão da importação de NF-e (WEB-INF/pages/nfe-revisao.jsp).
 *
 * Marcas da JSP:
 *   form[data-nfe-revisao]            formulário #nfe-form-confirmar (o botão Confirmar fica na barra fixa, com form="...")
 *   [data-nfe-item="N"]               um item da nota; data-unidade = unidade da nota
 *   input[data-nfe-acao]              os três rádios (VINCULAR, CRIAR, IGNORAR) do item; itens travados não têm rádios
 *   [data-nfe-painel="ACAO"]          painel de campos da ação; só o da ação escolhida aparece e fica habilitado
 *   select[data-nfe-produto]          produto a vincular; as opções têm data-unidade
 *   [data-nfe-criar]                  campos de "criar" (nome, categoria, preço): obrigatórios só quando a ação é CRIAR
 *   [data-nfe-aviso-unidade]          aviso de unidade diferente (o custo do produto não é atualizado)
 *   [data-nfe-aviso-ean], [data-nfe-aviso-ncm]  avisos "será gravado no produto"; li[data-ean], li[data-ncm] e as opções do seletor (data-ean, data-ncm) decidem
 *   [data-nfe-conta="entram|criam|ignoram"], [data-nfe-resumo-curto]   resumo no topo e na barra
 *   [data-nfe-confirmar], [data-nfe-bloqueio]   botão Confirmar e o texto que explica por que está desabilitado
 *
 * A regra da unidade é a do servidor (ImportacaoNfeService.unidadeIgual): igual ignorando maiúsculas e minúsculas.
 * Sem JavaScript todos os painéis ficam visíveis e o servidor usa só os campos da ação escolhida.
 * Todo texto entra por textContent.
 */
(function () {
  'use strict';

  var form = document.querySelector('form[data-nfe-revisao]');
  if (!form) {
    return;
  }

  var itens = Array.prototype.slice.call(form.querySelectorAll('[data-nfe-item]'));
  var botao = document.querySelector('[data-nfe-confirmar]');
  var bloqueio = document.querySelector('[data-nfe-bloqueio]');
  var curto = document.querySelector('[data-nfe-resumo-curto]');
  var contas = {};
  Array.prototype.forEach.call(document.querySelectorAll('[data-nfe-conta]'), function (el) {
    contas[el.getAttribute('data-nfe-conta')] = el;
  });

  function acaoDe(item) {
    var marcado = item.querySelector('input[data-nfe-acao]:checked');
    return marcado ? marcado.value : null;
  }

  function mesmaUnidade(a, b) {
    return String(a || '').toLowerCase() === String(b || '').toLowerCase();
  }

  /** Mostra só o painel da ação escolhida, habilita só os campos dele e ajusta o que é obrigatório. */
  function aplicarAcao(item) {
    var acao = acaoDe(item);
    var paineis = item.querySelectorAll('[data-nfe-painel]');
    Array.prototype.forEach.call(paineis, function (painel) {
      var ativo = painel.getAttribute('data-nfe-painel') === acao;
      painel.hidden = !ativo;
      Array.prototype.forEach.call(painel.querySelectorAll('input, select, textarea'), function (campo) {
        campo.disabled = !ativo;
        if (campo.hasAttribute('data-nfe-criar') || campo.hasAttribute('data-nfe-produto')) {
          campo.required = ativo;
          if (!ativo) {
            campo.classList.remove('is-invalid');
            campo.removeAttribute('aria-invalid');
          }
        }
      });
    });
    atualizarUnidade(item, acao);
    atualizarAvisosDoSugerido(item, acao);
  }

  /**
   * Avisos "o código de barras / o NCM será gravado no produto": valem para o produto ESCOLHIDO (não só o sugerido).
   * O aviso aparece quando a ação é vincular, há produto escolhido e esse produto ainda não tem o dado
   * (data-ean e data-ncm das opções do seletor); a flag do servidor (avisoEan/avisoNcm) só decide o estado inicial.
   */
  function atualizarAvisosDoSugerido(item, acao) {
    var select = item.querySelector('select[data-nfe-produto]');
    var opcao = select && select.value !== '' ? select.options[select.selectedIndex] : null;
    var vinculando = acao === 'VINCULAR' && opcao !== null;
    alternarAviso(item, '[data-nfe-aviso-ean]', vinculando && item.getAttribute('data-ean') && !opcao.getAttribute('data-ean'));
    alternarAviso(item, '[data-nfe-aviso-ncm]', vinculando && item.getAttribute('data-ncm') && !opcao.getAttribute('data-ncm'));
  }

  function alternarAviso(item, seletor, mostrar) {
    var aviso = item.querySelector(seletor);
    if (aviso) {
      aviso.hidden = !mostrar;
    }
  }

  function atualizarUnidade(item, acao) {
    var aviso = item.querySelector('[data-nfe-aviso-unidade]');
    var select = item.querySelector('select[data-nfe-produto]');
    if (!aviso || !select) {
      return;
    }
    var opcao = select.options[select.selectedIndex];
    var unidadeProduto = opcao ? opcao.getAttribute('data-unidade') : null;
    var unidadeNota = item.getAttribute('data-unidade');
    var diferente = acao === 'VINCULAR' && select.value !== '' && unidadeProduto !== null && !mesmaUnidade(unidadeProduto, unidadeNota);
    aviso.hidden = !diferente;
    if (diferente) {
      var texto = aviso.querySelector('[data-nfe-aviso-unidade-texto]');
      if (texto) {
        texto.textContent = 'A unidade da nota (' + unidadeNota + ') é diferente da unidade do produto (' + unidadeProduto + '). ';
      }
    }
  }

  function plural(n, singular, pluralTexto) {
    return n + ' ' + (n === 1 ? singular : pluralTexto);
  }

  /** Recalcula o resumo (topo e barra) e liga ou desliga o botão Confirmar. */
  function recalcular() {
    var vinculam = 0;
    var criam = 0;
    var ignoram = 0;
    itens.forEach(function (item) {
      var acao = acaoDe(item);
      if (acao === 'VINCULAR') {
        vinculam++;
      } else if (acao === 'CRIAR') {
        criam++;
      } else {
        ignoram++; // IGNORAR escolhido ou item travado
      }
    });
    var entram = vinculam + criam;
    if (contas.entram) {
      contas.entram.textContent = String(entram);
    }
    if (contas.criam) {
      contas.criam.textContent = String(criam);
    }
    if (contas.ignoram) {
      contas.ignoram.textContent = String(ignoram);
    }
    if (curto) {
      curto.textContent = plural(entram, 'item entra', 'itens entram') + ' · ' + plural(criam, 'produto novo', 'produtos novos') + ' · ' +
        plural(ignoram, 'ignorado', 'ignorados');
    }
    if (botao && !botao.hasAttribute('data-nfe-enviando')) {
      botao.disabled = entram === 0;
    }
    if (bloqueio) {
      bloqueio.hidden = entram !== 0;
    }
  }

  itens.forEach(aplicarAcao);
  recalcular();

  form.addEventListener('change', function (evento) {
    var alvo = evento.target;
    var item = alvo.closest ? alvo.closest('[data-nfe-item]') : null;
    if (!item) {
      return;
    }
    if (alvo.matches('input[data-nfe-acao], select[data-nfe-produto]')) {
      aplicarAcao(item);
      recalcular();
    }
  });

  /* ---------- Envio: o botão da barra mostra "Importando…" (formularios.js só cuida dos botões dentro do formulário) ---------- */
  var rotuloOriginal = null;
  form.addEventListener('submit', function (evento) {
    window.setTimeout(function () {
      if (evento.defaultPrevented || !botao) {
        return;
      }
      var rotulo = botao.querySelector('[data-nfe-confirmar-rotulo]');
      rotuloOriginal = rotulo ? rotulo.textContent : null;
      botao.setAttribute('data-nfe-enviando', 'sim');
      botao.setAttribute('aria-busy', 'true');
      botao.disabled = true;
      if (rotulo) {
        rotulo.textContent = 'Importando…';
      }
    }, 0);
  });

  // Voltar pelo histórico (bfcache) não pode deixar o botão preso
  window.addEventListener('pageshow', function (evento) {
    if (!evento.persisted || !botao) {
      return;
    }
    botao.removeAttribute('data-nfe-enviando');
    botao.removeAttribute('aria-busy');
    var rotulo = botao.querySelector('[data-nfe-confirmar-rotulo]');
    if (rotulo && rotuloOriginal) {
      rotulo.textContent = rotuloOriginal;
    }
    form.removeAttribute('data-sh-enviando-agora');
    recalcular();
  });

  /* ---------- Volta de um erro de formato: leva o foco ao resumo de erros ---------- */
  var resumoErros = document.querySelector('[data-sh-resumo-erros]');
  if (resumoErros && !resumoErros.hidden) {
    resumoErros.focus();
  }
})();
