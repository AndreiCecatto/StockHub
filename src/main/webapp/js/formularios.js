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

/*
 * StockHub: componentes de formulário próprios (seção 15 de docs/design-system.md).
 * Melhoria progressiva sobre o HTML da página: o select nativo continua no DOM (é ele que vai no POST e que o resto
 * do código lê). Aqui nascem:
 *   - lista suspensa própria (.sh-sel) sobre todo <select class="form-select"> (busca com mais de 8 opções);
 *     opte por sair com data-sh-nativo; seletores com busca própria (data-sh-combo) ficam como estão;
 *   - chips de ramo ([data-sh-ramo]) que filtram pelo tipo de loja escolhido;
 *   - seletor de quantidade (- / +) nos campos numéricos inteiros;
 *   - formatação de valores com duas casas (step="0.01") ao sair do campo.
 */
(function () {
  'use strict';

  var SH = (window.SH = window.SH || {});
  var contador = 0;

  function el(tag, classe, atributos) {
    var n = document.createElement(tag);
    if (classe) n.className = classe;
    if (atributos) Object.keys(atributos).forEach(function (k) { n.setAttribute(k, atributos[k]); });
    return n;
  }

  function normalizar(t) {
    t = String(t || '').toLowerCase();
    return t.normalize ? t.normalize('NFD').replace(/[̀-ͯ]/g, '') : t;
  }

  function disparar(campo, tipo) {
    var ev;
    if (typeof Event === 'function') ev = new Event(tipo, { bubbles: true });
    else { ev = document.createEvent('Event'); ev.initEvent(tipo, true, false); }
    campo.dispatchEvent(ev);
  }

  /* Faz um setter do select (value, selectedIndex) avisar o componente quando outro script o altera */
  function espiarPropriedade(campo, nome, aoMudar) {
    var desc = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, nome);
    if (!desc || !desc.set) return;
    Object.defineProperty(campo, nome, {
      configurable: true,
      get: function () { return desc.get.call(campo); },
      set: function (v) { desc.set.call(campo, v); aoMudar(); }
    });
  }

  function rotuloDoCampo(campo) {
    if (!campo.id) return null;
    var r = document.querySelector('label[for="' + (window.CSS && CSS.escape ? CSS.escape(campo.id) : campo.id) + '"]');
    if (r && !r.id) r.id = 'sh-rot-' + (++contador);
    return r;
  }

  /* ====================== Lista suspensa própria ====================== */
  var painelAberto = null;

  function montarSelect(sel) {
    if (sel.getAttribute('data-sh-sel') || sel.multiple || sel.size > 1 || sel.hasAttribute('data-sh-nativo') ||
        sel.closest('[data-sh-combo]') || sel.classList.contains('visually-hidden')) return;
    sel.setAttribute('data-sh-sel', '1');
    var id = ++contador;
    var rotulo = rotuloDoCampo(sel);
    var raiz = el('div', 'sh-sel');
    sel.parentNode.insertBefore(raiz, sel);
    raiz.appendChild(sel);

    var valorEl = el('span', 'sh-sel__valor', { id: 'sh-selv-' + id });
    var botao = el('button', 'sh-sel__botao', {
      type: 'button', role: 'combobox', 'aria-haspopup': 'listbox', 'aria-expanded': 'false', 'aria-controls': 'sh-sell-' + id
    });
    botao.appendChild(valorEl);
    botao.appendChild(el('span', 'sh-sel__seta', { 'aria-hidden': 'true' }));
    botao.setAttribute('aria-labelledby', (rotulo ? rotulo.id + ' ' : '') + valorEl.id);
    raiz.insertBefore(botao, sel);

    sel.setAttribute('tabindex', '-1');
    sel.setAttribute('aria-hidden', 'true');
    sel.focus = function () { botao.focus(); };
    sel.addEventListener('focus', function () { botao.focus(); });

    var painel = el('div', 'sh-sel__painel');
    painel.hidden = true;
    var lista = el('ul', 'sh-sel__lista', { role: 'listbox', id: 'sh-sell-' + id });
    if (rotulo) lista.setAttribute('aria-labelledby', rotulo.id);
    painel.appendChild(lista);
    var busca = null;
    var buscaEl = null;
    var opcoes = [];     // { li, op, texto }
    var ativa = -1;
    var digitado = '';
    var tempoDigitado = 0;

    function grupoOculto(g) {
      return g.hidden || (g.disabled && g.hasAttribute('data-tipos'));
    }

    function visivel(op) {
      if (op.hidden) return false;
      var g = op.parentNode;
      return !(g && g.tagName === 'OPTGROUP' && grupoOculto(g));
    }

    function atualizarBotao() {
      var op = sel.options[sel.selectedIndex];
      var texto = op ? op.textContent.trim() : '';
      valorEl.textContent = texto || ' ';
      valorEl.classList.toggle('sh-sel__valor--vazio', !op || op.value === '');
      botao.disabled = sel.disabled;
    }

    function sincronizarAtributos() {
      var inv = sel.classList.contains('is-invalid') || sel.getAttribute('aria-invalid') === 'true';
      if (inv) botao.setAttribute('aria-invalid', 'true'); else botao.removeAttribute('aria-invalid');
      var d = sel.getAttribute('aria-describedby');
      if (d) botao.setAttribute('aria-describedby', d); else botao.removeAttribute('aria-describedby');
      if (sel.required) botao.setAttribute('aria-required', 'true'); else botao.removeAttribute('aria-required');
      botao.disabled = sel.disabled;
    }

    function construirLista() {
      lista.textContent = '';
      opcoes = [];
      function novaOpcao(op, pai) {
        var li = el('li', 'sh-sel__opcao', { role: 'option', id: 'sh-selo-' + id + '-' + opcoes.length });
        li.textContent = op.textContent.trim();
        if (op.disabled) li.setAttribute('aria-disabled', 'true');
        li.setAttribute('aria-selected', op.selected ? 'true' : 'false');
        pai.appendChild(li);
        opcoes.push({ li: li, op: op, texto: normalizar(op.textContent) });
      }
      Array.prototype.forEach.call(sel.children, function (filho) {
        if (filho.tagName === 'OPTGROUP') {
          if (grupoOculto(filho)) return;
          var gli = el('li', null, { role: 'presentation' });
          var gt = el('div', 'sh-sel__grupo', { id: 'sh-selg-' + id + '-' + opcoes.length });
          gt.textContent = filho.label;
          var grupoUl = el('ul', null, { role: 'group', 'aria-labelledby': gt.id, style: 'list-style:none;margin:0;padding:0' });
          gli.appendChild(gt);
          gli.appendChild(grupoUl);
          lista.appendChild(gli);
          Array.prototype.forEach.call(filho.children, function (op) { if (!op.hidden) novaOpcao(op, grupoUl); });
        } else if (filho.tagName === 'OPTION' && !filho.hidden) {
          novaOpcao(filho, lista);
        }
      });
      var vazio = el('div', 'sh-sel__nada', { 'data-nada': '' });
      vazio.textContent = 'Nada encontrado.';
      vazio.hidden = true;
      painel.appendChild(vazio);
      if (opcoes.length > 8 && !busca) {
        busca = el('div', 'sh-sel__busca');
        buscaEl = el('input', null, {
          type: 'text', role: 'combobox', 'aria-autocomplete': 'list', 'aria-expanded': 'true', 'aria-controls': lista.id,
          'aria-label': 'Buscar na lista', placeholder: 'Buscar', autocomplete: 'off', spellcheck: 'false', form: 'sh-sem-formulario'
        });
        busca.appendChild(buscaEl);
        painel.insertBefore(busca, lista);
        buscaEl.addEventListener('input', filtrar);
        buscaEl.addEventListener('keydown', teclasAberto);
      }
    }

    function filtrar() {
      var q = normalizar(buscaEl ? buscaEl.value : '').trim();
      var algum = false;
      opcoes.forEach(function (o) {
        var ok = !q || o.texto.indexOf(q) !== -1;
        o.li.hidden = !ok;
        if (ok) algum = true;
      });
      Array.prototype.forEach.call(lista.querySelectorAll('[role=group]'), function (g) {
        g.parentNode.hidden = !g.querySelector('[role=option]:not([hidden])');
      });
      painel.querySelector('[data-nada]').hidden = algum;
      var primeira = -1;
      opcoes.some(function (o, i) { if (!o.li.hidden && !o.li.getAttribute('aria-disabled')) { primeira = i; return true; } return false; });
      var sele = -1;
      opcoes.some(function (o, i) { if (o.op.selected && !o.li.hidden) { sele = i; return true; } return false; });
      definirAtiva(q ? primeira : (sele >= 0 ? sele : primeira), true);
    }

    function habilitada(i) {
      return i >= 0 && i < opcoes.length && !opcoes[i].li.hidden && !opcoes[i].li.getAttribute('aria-disabled');
    }

    function definirAtiva(i, rolar) {
      if (ativa >= 0 && opcoes[ativa]) opcoes[ativa].li.removeAttribute('data-ativa');
      ativa = i;
      var alvo = buscaEl && !painel.hidden ? buscaEl : botao;
      if (i >= 0 && opcoes[i]) {
        opcoes[i].li.setAttribute('data-ativa', '');
        alvo.setAttribute('aria-activedescendant', opcoes[i].li.id);
        if (rolar !== false) opcoes[i].li.scrollIntoView({ block: 'nearest' });
      } else {
        botao.removeAttribute('aria-activedescendant');
        if (buscaEl) buscaEl.removeAttribute('aria-activedescendant');
      }
    }

    function mover(passo, inicio) {
      var i = ativa;
      var n = opcoes.length;
      if (inicio === 'fim') { i = n; passo = -1; } else if (inicio === 'ini') { i = -1; passo = 1; }
      for (var c = 0; c < n; c++) {
        i += passo;
        if (i < 0 || i >= n) break;
        if (habilitada(i)) { definirAtiva(i); return; }
      }
    }

    function escolher(i) {
      if (!habilitada(i)) return;
      var mudou = sel.selectedIndex !== opcoes[i].op.index;
      sel.selectedIndex = opcoes[i].op.index;
      fechar(true);
      if (mudou) { disparar(sel, 'input'); disparar(sel, 'change'); }
    }

    function posicionar() {
      var r = botao.getBoundingClientRect();
      var abaixo = window.innerHeight - r.bottom - 12;
      var acima = r.top - 12;
      var altura = Math.min(340, Math.max(abaixo, acima));
      var emCima = abaixo < 220 && acima > abaixo;
      painel.classList.toggle('sh-sel__painel--acima', emCima);
      painel.style.left = Math.max(8, r.left) + 'px';
      painel.style.width = Math.min(r.width, window.innerWidth - 16) + 'px';
      painel.style.maxHeight = altura + 'px';
      if (emCima) { painel.style.top = 'auto'; painel.style.bottom = (window.innerHeight - r.top + 6) + 'px'; }
      else { painel.style.bottom = 'auto'; painel.style.top = (r.bottom + 6) + 'px'; }
    }

    function abrir() {
      if (sel.disabled || !painel.hidden) return;
      if (painelAberto) painelAberto.fechar(false);
      construirLista();
      if (buscaEl) buscaEl.value = '';
      painel.hidden = false;
      document.body.appendChild(painel);
      botao.setAttribute('aria-expanded', 'true');
      posicionar();
      filtrar();
      painelAberto = { fechar: fechar };
      document.addEventListener('pointerdown', foraDoPainel, true);
      window.addEventListener('resize', aoRedimensionar);
      window.addEventListener('scroll', aoRolar, true);
      if (buscaEl) buscaEl.focus({ preventScroll: true });
    }

    function fechar(devolverFoco) {
      if (painel.hidden) return;
      painel.hidden = true;
      botao.setAttribute('aria-expanded', 'false');
      definirAtiva(-1, false);
      document.removeEventListener('pointerdown', foraDoPainel, true);
      window.removeEventListener('resize', aoRedimensionar);
      window.removeEventListener('scroll', aoRolar, true);
      painelAberto = null;
      if (devolverFoco !== false) botao.focus({ preventScroll: true });
    }

    function foraDoPainel(e) {
      if (!painel.contains(e.target) && !botao.contains(e.target)) fechar(false);
    }
    function aoRedimensionar() { posicionar(); }
    function aoRolar(e) { if (!painel.contains(e.target)) fechar(false); }

    function teclasAberto(e) {
      var k = e.key;
      if (k === 'ArrowDown') { e.preventDefault(); mover(1); }
      else if (k === 'ArrowUp') { e.preventDefault(); mover(-1); }
      else if (k === 'Home' && !buscaEl) { e.preventDefault(); mover(1, 'ini'); }
      else if (k === 'End' && !buscaEl) { e.preventDefault(); mover(-1, 'fim'); }
      else if (k === 'Enter' || (k === ' ' && !buscaEl)) { e.preventDefault(); escolher(ativa); }
      else if (k === 'Escape') { e.preventDefault(); e.stopPropagation(); fechar(true); }
      else if (k === 'Tab') { fechar(false); }
      else if (!buscaEl && k.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) { tipoAntecipado(k, true); }
    }

    function tipoAntecipado(letra, aberto) {
      var agora = Date.now();
      digitado = agora - tempoDigitado > 700 ? letra : digitado + letra;
      tempoDigitado = agora;
      var q = normalizar(digitado);
      if (aberto) {
        for (var i = 0; i < opcoes.length; i++) {
          if (habilitada(i) && opcoes[i].texto.indexOf(q) === 0) { definirAtiva(i); return; }
        }
      } else {
        var cand = Array.prototype.filter.call(sel.options, function (o) { return !o.disabled && visivel(o); });
        for (var j = 0; j < cand.length; j++) {
          if (normalizar(cand[j].textContent).indexOf(q) === 0) {
            if (sel.selectedIndex !== cand[j].index) {
              sel.selectedIndex = cand[j].index;
              disparar(sel, 'input'); disparar(sel, 'change');
            }
            return;
          }
        }
      }
    }

    botao.addEventListener('click', function () { if (painel.hidden) abrir(); else fechar(true); });
    botao.addEventListener('keydown', function (e) {
      if (!painel.hidden) { teclasAberto(e); return; }
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].indexOf(e.key) !== -1) { e.preventDefault(); abrir(); }
      else if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) { tipoAntecipado(e.key, false); }
    });
    function indiceDe(alvo) {
      var li = alvo.closest ? alvo.closest('[role=option]') : null;
      if (!li) return -1;
      for (var i = 0; i < opcoes.length; i++) if (opcoes[i].li === li) return i;
      return -1;
    }
    lista.addEventListener('mousemove', function (e) {
      var i = indiceDe(e.target);
      if (i >= 0 && i !== ativa && habilitada(i)) definirAtiva(i, false);
    });
    lista.addEventListener('click', function (e) { escolher(indiceDe(e.target)); });
    painel.addEventListener('mousedown', function (e) { if (e.target !== buscaEl) e.preventDefault(); });

    sel.addEventListener('change', atualizarBotao);
    espiarPropriedade(sel, 'value', atualizarBotao);
    espiarPropriedade(sel, 'selectedIndex', atualizarBotao);
    var obs = new MutationObserver(function () { sincronizarAtributos(); atualizarBotao(); });
    obs.observe(sel, { childList: true, subtree: true, attributes: true, attributeFilter: ['class', 'aria-invalid', 'aria-describedby', 'disabled', 'required', 'selected'] });
    if (sel.form) sel.form.addEventListener('reset', function () { setTimeout(atualizarBotao, 0); });
    sincronizarAtributos();
    atualizarBotao();
  }

  /* ====================== Chips de ramo ====================== */
  function montarChips(bloco) {
    var sel = bloco.querySelector('select[name="subtipo"]');
    if (!sel || sel.getAttribute('data-sh-chips')) return;
    sel.setAttribute('data-sh-chips', '1');
    sel.setAttribute('data-sh-sel', '1'); // fora da lista suspensa
    var rotulo = rotuloDoCampo(sel);
    var grupo = el('div', 'sh-chips', { role: 'radiogroup' });
    if (rotulo) grupo.setAttribute('aria-labelledby', rotulo.id);
    var chips = [];
    var descValor = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value');

    function criarChip(op, pai) {
      var c = el('button', 'sh-chip', { type: 'button', role: 'radio', 'aria-checked': 'false', tabindex: '-1' });
      c.textContent = op.value === '' ? 'Prefiro não informar' : op.textContent.trim();
      c.addEventListener('click', function () { escolher(op.value, true); });
      pai.appendChild(c);
      chips.push({ c: c, op: op });
    }

    var vazio = Array.prototype.filter.call(sel.children, function (f) { return f.tagName === 'OPTION'; })[0];
    if (vazio) {
      var topo = el('div', 'sh-chips__lista');
      criarChip(vazio, topo);
      grupo.appendChild(topo);
    }
    var grupos = [];
    Array.prototype.forEach.call(sel.querySelectorAll('optgroup'), function (og) {
      var g = el('div', 'sh-chips__grupo');
      var t = el('div', 'sh-chips__titulo');
      t.textContent = og.label;
      var l = el('div', 'sh-chips__lista');
      g.appendChild(t);
      g.appendChild(l);
      Array.prototype.forEach.call(og.children, function (op) { criarChip(op, l); });
      grupo.appendChild(g);
      grupos.push({ g: g, og: og, t: t });
    });

    sel.parentNode.insertBefore(grupo, sel);
    sel.style.cssText = 'position:absolute;width:1px;height:1px;opacity:0;pointer-events:none;overflow:hidden';
    sel.setAttribute('tabindex', '-1');
    sel.setAttribute('aria-hidden', 'true');

    function escolher(valor, foco) {
      var mudou = descValor.get.call(sel) !== valor;
      descValor.set.call(sel, valor);
      sincronizar();
      if (foco) {
        var alvo = chips.filter(function (x) { return x.op.value === valor; })[0];
        if (alvo) alvo.c.focus();
      }
      if (mudou) { disparar(sel, 'input'); disparar(sel, 'change'); }
    }

    function sincronizar() {
      var atual = descValor.get.call(sel);
      var visiveis = 0;
      grupos.forEach(function (x) {
        var oculto = x.og.hidden || x.og.disabled;
        x.g.hidden = oculto;
        if (!oculto) visiveis++;
      });
      grupos.forEach(function (x) { x.t.hidden = visiveis < 2; });
      chips.forEach(function (x) {
        var marcado = x.op.value === atual && !x.c.closest('[hidden]');
        x.c.setAttribute('aria-checked', marcado ? 'true' : 'false');
        x.c.tabIndex = -1;
      });
      var alvoTab = chips.filter(function (x) { return x.c.getAttribute('aria-checked') === 'true'; })[0] || chips[0];
      if (alvoTab) alvoTab.c.tabIndex = 0;
    }

    grupo.addEventListener('keydown', function (e) {
      var teclas = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 };
      if (!teclas[e.key]) return;
      var vis = chips.filter(function (x) { return !x.c.closest('[hidden]'); });
      var i = vis.map(function (x) { return x.c; }).indexOf(document.activeElement);
      if (i < 0) return;
      e.preventDefault();
      escolher(vis[(i + teclas[e.key] + vis.length) % vis.length].op.value, true);
    });

    espiarPropriedade(sel, 'value', sincronizar);
    sel.addEventListener('focus', function () {
      var a = chips.filter(function (x) { return x.c.tabIndex === 0; })[0];
      if (a) a.c.focus();
    });
    var tipos = bloco.closest('[data-sh-tipos]');
    if (tipos) {
      Array.prototype.forEach.call(tipos.querySelectorAll('input[name="tipoLoja"]'), function (r) {
        r.addEventListener('change', function () { setTimeout(sincronizar, 0); });
      });
    }
    new MutationObserver(sincronizar).observe(sel, { subtree: true, attributes: true, attributeFilter: ['hidden', 'disabled'] });
    sincronizar();
    setTimeout(sincronizar, 0);
  }

  /* ====================== Quantidade e valores ====================== */
  function montarPasso(inp) {
    if (inp.getAttribute('data-sh-stepper') || inp.readOnly || inp.hasAttribute('data-sh-sem-stepper') || inp.type !== 'number' ||
        inp.closest('.v3e-qtd, .sh-stepper, .input-group, [class*="in-repor"]')) return;
    var passo = inp.getAttribute('step');
    if (passo && passo !== '1') return;
    inp.setAttribute('data-sh-stepper', '1');
    var raiz = el('div', 'sh-stepper');
    var menos = el('button', 'sh-stepper__botao', { type: 'button', 'aria-label': 'Diminuir 1', tabindex: '-1' });
    var mais = el('button', 'sh-stepper__botao', { type: 'button', 'aria-label': 'Aumentar 1', tabindex: '-1' });
    menos.innerHTML = '<i class="bi bi-dash-lg" aria-hidden="true"></i>';
    mais.innerHTML = '<i class="bi bi-plus-lg" aria-hidden="true"></i>';
    inp.parentNode.insertBefore(raiz, inp);
    raiz.appendChild(menos);
    raiz.appendChild(inp);
    raiz.appendChild(mais);

    function lim() {
      return {
        v: parseInt(inp.value, 10),
        mn: inp.min !== '' ? parseInt(inp.min, 10) : -Infinity,
        mx: inp.max !== '' ? parseInt(inp.max, 10) : Infinity
      };
    }
    function estado() {
      var l = lim();
      menos.disabled = inp.disabled || (!isNaN(l.v) && l.v <= l.mn);
      mais.disabled = inp.disabled || (!isNaN(l.v) && l.v >= l.mx);
    }
    function mudar(d) {
      var l = lim();
      var v = l.v;
      if (isNaN(v)) v = isFinite(l.mn) ? l.mn - d : 0;
      v = Math.min(l.mx, Math.max(l.mn, v + d));
      inp.value = String(v);
      disparar(inp, 'input');
      disparar(inp, 'change');
      estado();
    }
    menos.addEventListener('click', function () { mudar(-1); });
    mais.addEventListener('click', function () { mudar(1); });
    inp.addEventListener('input', estado);
    inp.addEventListener('change', estado);
    estado();
  }

  function formatarValor(e) {
    var inp = e.target;
    if (!inp || inp.tagName !== 'INPUT' || inp.type !== 'number') return;
    if (inp.getAttribute('step') !== '0.01' && !inp.hasAttribute('data-sh-moeda')) return;
    var v = parseFloat(inp.value);
    if (!isNaN(v) && inp.validity && !inp.validity.badInput) inp.value = v.toFixed(2);
  }

  function iniciar(raiz) {
    raiz = raiz || document;
    Array.prototype.forEach.call(raiz.querySelectorAll('[data-sh-ramo]'), montarChips);
    Array.prototype.forEach.call(raiz.querySelectorAll('select.form-select'), montarSelect);
    Array.prototype.forEach.call(raiz.querySelectorAll('input[type="number"].form-control'), montarPasso);
  }

  SH.iniciarFormularios = iniciar;
  document.addEventListener('focusout', formatarValor);
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { iniciar(); });
  else iniciar();
})();
