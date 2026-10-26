/*
 * StockHub: formulários (seção 4.3 de docs/design-system.md).
 *
 * Marcas que as JSPs usam (todas opcionais; sem JS o formulário funciona com a validação nativa):
 *   form[data-sh-validar]            validação inline em português + resumo de erros + bloqueio de envio duplo
 *   form[data-sh-bloquear]           só o bloqueio de envio duplo
 *   form[data-sh-avisar-alteracoes]  avisa antes de sair da página com alterações não salvas
 *   [data-sh-resumo-erros]           contêiner do resumo (role="alert", tabindex="-1", hidden)
 *   [data-sh-senha-alternar]         botão mostrar/ocultar senha (aria-controls aponta o campo)
 *   [data-sh-regras-senha]           lista de regras da senha (data-campo = id do campo)
 *   [data-sh-contador="id"]          contador de caracteres do campo (usa o maxlength)
 *   data-sh-enviando="Entrando…"     texto do botão de envio enquanto o formulário é enviado
 *   form="sh-sem-formulario"         (em um input) campo SEM dono: o id não existe de propósito, então o campo não entra
 *                                    no formulário nem na validação deste arquivo. O seletor de produto com busca da
 *                                    movimentação (js/combobox-produto.js) usa isso; o valor real segue pelo select.
 *
 * Os atributos nativos (required, min, max, minlength...) continuam no HTML. Com JS o formulário ganha
 * novalidate e as mensagens aparecem ao SAIR do campo e ao enviar. Mensagens de erro entram por textContent.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});

  /* ---------- Utilidades ---------- */
  function escaparId(id) {
    return window.CSS && window.CSS.escape ? window.CSS.escape(id) : String(id).replace(/["\\]/g, '\\$&');
  }

  function rotuloDo(campo) {
    var rotulo = campo.id ? document.querySelector('label[for="' + escaparId(campo.id) + '"]') : null;
    var texto = rotulo ? rotulo.textContent : campo.getAttribute('name') || 'este campo';
    return texto.replace(/\(obrigat[óo]rio\)/i, '').replace(/\s+/g, ' ').trim();
  }

  function ehCampoValidavel(el) {
    if (!el || !el.tagName || el.disabled) {
      return false;
    }
    var etiqueta = el.tagName.toLowerCase();
    if (etiqueta !== 'input' && etiqueta !== 'select' && etiqueta !== 'textarea') {
      return false;
    }
    var tipo = (el.getAttribute('type') || '').toLowerCase();
    return ['hidden', 'submit', 'button', 'reset', 'image', 'radio'].indexOf(tipo) === -1;
  }

  function idDoErro(campo) {
    return 'erro-' + (campo.id || campo.getAttribute('name'));
  }

  function acrescentarDescricao(campo, id) {
    var atual = (campo.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean);
    if (atual.indexOf(id) === -1) {
      atual.push(id);
    }
    campo.setAttribute('aria-describedby', atual.join(' '));
  }

  function removerDescricao(campo, id) {
    var restante = (campo.getAttribute('aria-describedby') || '')
      .split(/\s+/)
      .filter(function (parte) {
        return parte && parte !== id;
      });
    if (restante.length) {
      campo.setAttribute('aria-describedby', restante.join(' '));
    } else {
      campo.removeAttribute('aria-describedby');
    }
  }

  /* ---------- Mensagens em português ---------- */
  function mensagemDe(campo) {
    var v = campo.validity;
    if (v.valid) {
      return '';
    }
    var nome = rotuloDo(campo);
    var tipo = (campo.getAttribute('type') || '').toLowerCase();
    if (v.valueMissing) {
      var personalizada = campo.getAttribute('data-sh-msg-obrigatorio');
      if (personalizada) {
        return personalizada;
      }
      if (tipo === 'checkbox') {
        return 'Marque a opção "' + nome + '".';
      }
      if (campo.tagName.toLowerCase() === 'select') {
        return 'Escolha uma opção em "' + nome + '".';
      }
      return 'Preencha o campo "' + nome + '".';
    }
    if (v.typeMismatch) {
      return tipo === 'email' ? 'Informe um e-mail válido, como voce@email.com.' : 'Informe um valor válido em "' + nome + '".';
    }
    if (v.tooShort) {
      return 'Use pelo menos ' + campo.minLength + ' caracteres em "' + nome + '".';
    }
    if (v.tooLong) {
      return 'Use no máximo ' + campo.maxLength + ' caracteres em "' + nome + '".';
    }
    if (v.rangeUnderflow) {
      return 'O valor mínimo em "' + nome + '" é ' + campo.min + '.';
    }
    if (v.rangeOverflow) {
      return 'O valor máximo em "' + nome + '" é ' + campo.max + '.';
    }
    if (v.stepMismatch) {
      return 'Informe um valor válido em "' + nome + '".';
    }
    if (v.badInput) {
      return 'Digite apenas números em "' + nome + '".';
    }
    if (v.patternMismatch) {
      return campo.getAttribute('data-sh-msg-formato') || 'O formato de "' + nome + '" não é válido.';
    }
    return campo.validationMessage || 'Confira o campo "' + nome + '".';
  }

  /* ---------- Mostrar e limpar erro de um campo ---------- */
  function mostrarErro(campo, texto) {
    var id = idDoErro(campo);
    var el = document.getElementById(id);
    if (!el) {
      el = document.createElement('div');
      el.id = id;
      el.className = 'sh-erro-campo';
      var icone = document.createElement('i');
      icone.className = 'bi bi-exclamation-circle';
      icone.setAttribute('aria-hidden', 'true');
      var span = document.createElement('span');
      span.className = 'sh-erro-campo__texto';
      el.appendChild(icone);
      el.appendChild(span);
      var ancora = campo.closest('.sh-senha, .input-group') || campo;
      ancora.parentNode.insertBefore(el, ancora.nextSibling);
    }
    var destino = el.querySelector('.sh-erro-campo__texto') || el;
    destino.textContent = texto;
    el.hidden = false;
    campo.classList.add('is-invalid');
    campo.setAttribute('aria-invalid', 'true');
    acrescentarDescricao(campo, id);
  }

  function limparErro(campo) {
    var id = idDoErro(campo);
    var el = document.getElementById(id);
    if (el) {
      el.hidden = true;
    }
    campo.classList.remove('is-invalid');
    campo.removeAttribute('aria-invalid');
    removerDescricao(campo, id);
  }

  function validarCampo(campo) {
    var texto = mensagemDe(campo);
    if (texto) {
      mostrarErro(campo, texto);
      return false;
    }
    limparErro(campo);
    return true;
  }

  /* ---------- Resumo de erros ---------- */
  function mostrarResumo(form, invalidos) {
    var resumo = form.querySelector('[data-sh-resumo-erros]');
    if (!resumo) {
      return;
    }
    var titulo = resumo.querySelector('.sh-resumo-erros__titulo');
    var lista = resumo.querySelector('ul');
    if (!titulo || !lista) {
      return;
    }
    titulo.textContent =
      invalidos.length === 1 ? 'Corrija 1 campo para continuar:' : 'Corrija ' + invalidos.length + ' campos para continuar:';
    while (lista.firstChild) {
      lista.removeChild(lista.firstChild);
    }
    invalidos.forEach(function (campo) {
      var item = document.createElement('li');
      var link = document.createElement('a');
      link.href = '#' + campo.id;
      link.setAttribute('data-sh-ir-campo', campo.id);
      link.textContent = mensagemDe(campo);
      item.appendChild(link);
      lista.appendChild(item);
    });
    resumo.hidden = false;
  }

  function esconderResumo(form) {
    var resumo = form.querySelector('[data-sh-resumo-erros]');
    if (resumo) {
      resumo.hidden = true;
    }
  }

  document.addEventListener('click', function (evento) {
    var link = evento.target.closest ? evento.target.closest('[data-sh-ir-campo]') : null;
    if (!link) {
      return;
    }
    var campo = document.getElementById(link.getAttribute('data-sh-ir-campo'));
    if (campo) {
      evento.preventDefault();
      campo.focus();
    }
  });

  /* ---------- Bloqueio de envio duplo ---------- */
  function botoesDeEnvio(form) {
    return form.querySelectorAll('button[type="submit"], button:not([type])');
  }

  function bloquear(form) {
    var botoes = botoesDeEnvio(form);
    for (var i = 0; i < botoes.length; i++) {
      var botao = botoes[i];
      if (botao.getAttribute('data-sh-guardado') === 'sim') {
        continue;
      }
      var original = [];
      for (var j = 0; j < botao.childNodes.length; j++) {
        original.push(botao.childNodes[j].cloneNode(true));
      }
      botao._shOriginal = original;
      botao.setAttribute('data-sh-guardado', 'sim');
      var texto = form.getAttribute('data-sh-enviando') || botao.getAttribute('data-sh-enviando') || 'Salvando…';
      while (botao.firstChild) {
        botao.removeChild(botao.firstChild);
      }
      var giro = document.createElement('span');
      giro.className = 'sh-spinner';
      giro.setAttribute('aria-hidden', 'true');
      var rotulo = document.createElement('span');
      rotulo.textContent = texto;
      botao.appendChild(giro);
      botao.appendChild(rotulo);
      botao.setAttribute('aria-busy', 'true');
      botao.disabled = true;
    }
  }

  function desbloquear(form) {
    form.removeAttribute('data-sh-enviando-agora');
    var botoes = botoesDeEnvio(form);
    for (var i = 0; i < botoes.length; i++) {
      var botao = botoes[i];
      if (botao.getAttribute('data-sh-guardado') !== 'sim') {
        continue;
      }
      while (botao.firstChild) {
        botao.removeChild(botao.firstChild);
      }
      (botao._shOriginal || []).forEach(function (no) {
        botao.appendChild(no);
      });
      botao.removeAttribute('data-sh-guardado');
      botao.removeAttribute('aria-busy');
      botao.disabled = false;
    }
  }

  // Voltar pelo histórico do navegador (bfcache) não pode deixar o botão preso em "Salvando…"
  window.addEventListener('pageshow', function (evento) {
    if (!evento.persisted) {
      return;
    }
    var formularios = document.querySelectorAll('form[data-sh-enviando-agora]');
    for (var i = 0; i < formularios.length; i++) {
      desbloquear(formularios[i]);
    }
  });

  /* ---------- Envio ---------- */
  document.addEventListener('submit', function (evento) {
    var form = evento.target;
    if (!form || !form.matches || !form.matches('form[data-sh-validar], form[data-sh-bloquear]')) {
      return;
    }
    if (form.matches('[data-confirmar-exclusao]')) {
      return; // o modal de confirmação cuida deste
    }

    if (form.hasAttribute('data-sh-validar')) {
      var invalidos = [];
      for (var i = 0; i < form.elements.length; i++) {
        var campo = form.elements[i];
        if (ehCampoValidavel(campo) && !validarCampo(campo)) {
          invalidos.push(campo);
        }
      }
      if (invalidos.length) {
        evento.preventDefault();
        mostrarResumo(form, invalidos);
        invalidos[0].focus();
        return;
      }
      esconderResumo(form);
    }

    if (form.hasAttribute('data-sh-enviando-agora')) {
      evento.preventDefault(); // segundo clique ou segundo Enter: ignora
      return;
    }
    form.setAttribute('data-sh-enviando-agora', 'sim');
    // Depois do ciclo do evento: se outro código cancelou o envio, o botão volta ao normal
    window.setTimeout(function () {
      if (evento.defaultPrevented) {
        desbloquear(form);
      } else {
        bloquear(form);
      }
    }, 0);
  });

  /* ---------- Validação ao sair do campo e ao corrigir ---------- */
  document.addEventListener('focusout', function (evento) {
    var campo = evento.target;
    if (!ehCampoValidavel(campo) || !campo.form || !campo.form.hasAttribute('data-sh-validar')) {
      return;
    }
    if (!document.hasFocus()) {
      return; // o foco saiu da janela, não do campo
    }
    // Ir do campo de senha para o botão "mostrar senha" não é "sair do campo"
    var grupoSenha = campo.closest('.sh-senha');
    if (grupoSenha && evento.relatedTarget && grupoSenha.contains(evento.relatedTarget)) {
      return;
    }
    validarCampo(campo);
  });

  function aoDigitar(evento) {
    var campo = evento.target;
    if (!ehCampoValidavel(campo) || !campo.form || !campo.form.hasAttribute('data-sh-validar')) {
      return;
    }
    // Só reavalia quem já mostra erro: não incomoda enquanto a pessoa ainda digita
    if (campo.getAttribute('aria-invalid') === 'true') {
      validarCampo(campo);
    }
  }
  document.addEventListener('input', aoDigitar);
  document.addEventListener('change', aoDigitar);

  /* ---------- Mostrar e ocultar senha ---------- */
  document.addEventListener('click', function (evento) {
    var botao = evento.target.closest ? evento.target.closest('[data-sh-senha-alternar]') : null;
    if (!botao) {
      return;
    }
    var campo = document.getElementById(botao.getAttribute('aria-controls'));
    if (!campo) {
      return;
    }
    var mostrar = campo.type === 'password';
    campo.type = mostrar ? 'text' : 'password';
    botao.setAttribute('aria-pressed', mostrar ? 'true' : 'false');
    var icone = botao.querySelector('.bi');
    if (icone) {
      icone.classList.toggle('bi-eye', !mostrar);
      icone.classList.toggle('bi-eye-slash', mostrar);
    }
  });

  /* ---------- Regras da senha (cadastro) ---------- */
  function bytesUtf8(texto) {
    return window.TextEncoder ? new window.TextEncoder().encode(texto).length : texto.length;
  }

  var regras = document.querySelector('[data-sh-regras-senha]');
  if (regras) {
    var campoSenha = document.getElementById(regras.getAttribute('data-campo'));
    var atualizarRegras = function () {
      var valor = campoSenha ? campoSenha.value : '';
      var estado = {
        minimo: valor.length >= 8,
        maximo: valor.length > 0 && bytesUtf8(valor) <= 72
      };
      var itens = regras.querySelectorAll('li[data-regra]');
      for (var i = 0; i < itens.length; i++) {
        var ok = !!estado[itens[i].getAttribute('data-regra')];
        itens[i].classList.toggle('is-ok', ok);
        var dica = itens[i].querySelector('.sh-regras__estado');
        if (dica) {
          dica.textContent = ok ? '(atendida)' : '(pendente)';
        }
      }
    };
    if (campoSenha) {
      campoSenha.addEventListener('input', atualizarRegras);
      atualizarRegras();
    }
  }

  /* ---------- Contador de caracteres ---------- */
  var contados = document.querySelectorAll('[data-sh-contador]');
  Array.prototype.forEach.call(contados, function (campo) {
    var saida = document.getElementById(campo.getAttribute('data-sh-contador'));
    var max = parseInt(campo.getAttribute('maxlength'), 10);
    if (!saida || !max) {
      return;
    }
    var atualizar = function () {
      var usados = campo.value.length;
      var restam = max - usados;
      var perto = restam <= Math.max(10, Math.round(max * 0.05));
      // perto do limite o texto muda ("faltam 12"): a informação não depende só da cor
      saida.textContent = perto ? (restam <= 0 ? 'Limite de ' + max + ' atingido' : 'faltam ' + restam) : usados + '/' + max;
      saida.classList.toggle('is-perto', perto);
    };
    campo.addEventListener('input', atualizar);
    atualizar();
  });

  /* ---------- Alterações não salvas ---------- */
  var vigiados = document.querySelectorAll('form[data-sh-avisar-alteracoes]');
  if (vigiados.length) {
    var fotografar = function (form) {
      var partes = [];
      new window.FormData(form).forEach(function (valor, nome) {
        partes.push(nome + '=' + (typeof valor === 'string' ? valor : valor.name));
      });
      return partes.join('&');
    };
    var fotos = [];
    for (var v = 0; v < vigiados.length; v++) {
      fotos.push(fotografar(vigiados[v]));
    }
    window.addEventListener('beforeunload', function (evento) {
      for (var i = 0; i < vigiados.length; i++) {
        if (!vigiados[i].hasAttribute('data-sh-enviando-agora') && fotografar(vigiados[i]) !== fotos[i]) {
          evento.preventDefault();
          evento.returnValue = '';
          return;
        }
      }
    });
  }

  /* ---------- Início ---------- */
  var formularios = document.querySelectorAll('form[data-sh-validar]');
  for (var f = 0; f < formularios.length; f++) {
    formularios[f].noValidate = true; // as mensagens nativas em inglês saem; entram as nossas
  }

  // Erro vindo do servidor: leva o foco ao resumo (ou ao alerta de erro), para o leitor de tela ler
  // e o teclado seguir daí
  var resumoDoServidor = document.querySelector('[data-sh-resumo-erros]:not([hidden]), .sh-alert--danger[role="alert"]');
  if (resumoDoServidor && resumoDoServidor.focus) {
    resumoDoServidor.focus();
  }

  SH.formularios = { validarCampo: validarCampo, bloquear: bloquear, desbloquear: desbloquear };
})();
