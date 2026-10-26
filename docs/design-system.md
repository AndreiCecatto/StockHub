# StockHub: identidade visual, experiência e plano de implementação (Fase 2 do redesenho)

Documento para o `designer-ui` (implementação na fase 3) e para o `dev-backend` (pedidos pequenos da seção 9).
Escrito em 26/10/2026, a partir de `docs/mercado-requisitos.md`, `docs/regras-negocio.md`, dos controllers e do front atual.

Legenda usada em todo o texto:

- **[F]** só front-end (JSP, CSS, JS), sem mudar o backend.
- **[B]** exige backend novo (item numerado na seção 9).
- **Descartado:** alternativa considerada e rejeitada, com o motivo.

## 0. Resumo das decisões

1. **Conceito "Balcão":** ferramenta de trabalho que se usa em pé, com uma mão, entre um cliente e outro. O que **falta** aparece antes do que sobra; cada tela serve a uma tarefa; todo registro mostra o **antes → depois** do saldo.
2. **Visual novo:** barra lateral escura "tinta" + área de trabalho clara e plana (bordas, quase sem sombra), cor de marca **índigo** (`#4338CA`), tema **claro e escuro**, fonte **Inter** servida localmente, números tabulares.
3. **Navegação:** barra lateral no desktop (recolhível); **barra inferior** no celular com o botão central **Movimentar**; busca global em `/`; atalhos de teclado desligáveis; sem paleta de comandos na v1.
4. **Bibliotecas:** só o que já existe, **local** em `src/main/webapp/vendor/`: Bootstrap 5.3.8 (CSS + JS), Bootstrap Icons 1.11.3 e a fonte Inter (subset latino). JS próprio em vanilla (~7 arquivos pequenos). **Sem biblioteca de gráficos** na v1.
5. **Interações novas, todas [F]:** busca de produtos ao digitar, seletor de produto com busca na movimentação, tipo de movimento em controle segmentado, motivos pré-definidos que preenchem a observação, prévia "saldo atual → saldo depois", botão **Repor** que abre a movimentação já com produto e tipo escolhidos, toasts, contagem animada, histórico em linha do tempo.
6. **Depende de backend (pequeno):** liberar `/vendor/**` no `SecurityConfig` (obrigatório para o lote 1), filtros e CSV do histórico, histórico no detalhe do produto, CSV de produtos.

## 1. Identidade

### 1.1 Conceito e tom

**Quem usa:** dono ou funcionário de loja pequena, no balcão ou no depósito, muitas vezes no celular, com pressa e pouca paciência para jargão.

**Princípios**

| Princípio | O que significa na tela |
|---|---|
| O problema primeiro | Dashboard abre com "o que precisa de reposição", não com totais. Vermelho e âmbar são **reservados** para problema de estoque, então um olhar rápido encontra só problemas |
| Uma tela, uma tarefa | Movimentar é uma tela de ação (formulário em destaque), histórico é leitura |
| Mostrar o que aconteceu | Toda movimentação confirma "saldo passou de 12 para 7 un."; o histórico mostra antes → depois |
| Ação à mão | O botão mais provável de cada tela é primário e fica visível sem rolar (no celular, na barra inferior) |
| Calma | Nada de decoração que não informe: sem gradientes, sem cartões que "levitam", sem animação gratuita |

**Tom de texto:** direto, verbo no infinitivo nos botões ("Registrar saída", "Salvar produto", "Repor"), frases curtas, sem culpar o usuário. Mensagens de erro dizem o que fazer. Sempre em português com acentos.

**Alternativas descartadas**

- *Painel de SaaS cheio de gráficos:* o backend só entrega totais, não séries no tempo; gráfico agora seria enfeite.
- *ERP denso e cinza:* intimida quem vem de caderno ou planilha.
- *App colorido e "gamificado":* passa a imagem de brinquedo; o projeto é uma ferramenta de trabalho.

### 1.2 Cores

**Decisão:** neutros frios (quase cinza-azulado) + **uma** cor de marca (índigo) + semânticas à parte. A marca **não** é verde, âmbar nem vermelho, para nunca ser confundida com estado de estoque.

**Descartado:** manter o azul `#2563eb` com fundo pastel em gradiente (é o visual atual, genérico, e o azul compete com a cor de "informação"); marca verde (confunde com "Normal").

Contrastes calculados pela fórmula da WCAG 2.x (luminância relativa). Meta: texto ≥ 4,5:1, elementos gráficos e bordas de campo ≥ 3:1.

#### Neutros, marca e foco (prefixo `--sh-`)

| Token | Claro | Escuro | Uso | Contrastes verificados |
|---|---|---|---|---|
| `--sh-bg` | `#F5F6F8` | `#0E1117` | fundo da página | |
| `--sh-surface` | `#FFFFFF` | `#161B24` | cartões, tabelas, campos | |
| `--sh-surface-2` | `#EEF0F4` | `#1D2430` | cabeçalho de tabela, áreas sutis | |
| `--sh-border` | `#D5DAE2` | `#2A3341` | divisores e bordas de cartão (decorativas) | n/a |
| `--sh-border-strong` | `#7C8696` | `#7B8798` | borda de campo, checkbox, medidor | claro 3,68:1 sobre surface e 3,40:1 sobre bg; escuro 4,74:1 sobre surface |
| `--sh-text` | `#161B26` | `#E7EBF1` | texto principal | claro 17,23:1 (surface) e 15,93:1 (bg); escuro 14,43:1 e 15,79:1 |
| `--sh-text-muted` | `#4B5565` | `#A5AFBF` | texto secundário | claro 7,54:1 / 6,61:1 (surface-2); escuro 7,80:1 / 7,04:1 |
| `--sh-text-subtle` | `#5F6B7C` | `#8D98A9` | placeholder, dicas | claro 5,41:1 (surface) e 4,74:1 (surface-2); escuro 5,92:1 e 5,34:1 |
| `--sh-accent` | `#4338CA` | `#5B59E0` | botão primário, fundo | branco sobre ele: claro 7,90:1; escuro 5,32:1 |
| `--sh-accent-hover` | `#3730A3` | `#4F4DD1` | hover do primário | branco: claro 9,93:1; escuro 6,34:1 |
| `--sh-accent-text` | `#4338CA` | `#A9A8F8` | links e ícones de destaque | claro 7,90:1 (surface); escuro 7,93:1 (surface) |
| `--sh-accent-soft` | `#E8E7FB` | `#24244F` | fundo de item ativo, linha destacada | texto de acento sobre ele: claro 6,49:1; escuro 6,70:1 |
| `--sh-focus` | `#4338CA` | `#A9A8F8` | anel de foco (3 px + 2 px de afastamento) | claro 7,90:1 (surface) e 7,31:1 (bg); escuro 7,93:1 e 8,68:1 (mínimo 3:1) |

Barra lateral (escura nos dois temas, é a assinatura visual):

| Token | Claro | Escuro | Contrastes verificados |
|---|---|---|---|
| `--sh-side-bg` | `#161B2E` | `#0A0D13` | |
| `--sh-side-text` | `#C5CCDC` | `#B4BDCD` | 10,60:1 / 10,28:1 |
| `--sh-side-muted` | `#98A2B8` | `#8A94A8` | 6,66:1 / 6,37:1 |
| `--sh-side-active-bg` | `#262E48` | `#1B2231` | branco sobre ele: 13,40:1 / 15,91:1 |
| `--sh-side-marker` | `#A5A4F8` | `#A9A8F8` | barra do item ativo: 7,53:1 / 8,93:1 (mínimo 3:1) |

#### Semânticas (texto sobre fundo tonal, que é como badges e alertas são desenhados)

| Papel | Texto claro / fundo claro | Texto escuro / fundo escuro | Razão claro | Razão escuro |
|---|---|---|---|---|
| Sucesso | `#0E6B3B` / `#E4F6EB` | `#6FDB9D` / `#12301F` | 5,87:1 | 8,37:1 |
| Aviso | `#7A4100` / `#FFF2D9` | `#F6C46A` / `#35260A` | 7,34:1 | 9,09:1 |
| Perigo | `#B42318` / `#FDEAE8` | `#FFA39B` / `#3B1512` | 5,67:1 | 8,42:1 |
| Info | `#0A5BA8` / `#E5F1FC` | `#8DC5FF` / `#0F2744` | 5,96:1 | 8,29:1 |

Tokens: `--sh-success-fg`, `--sh-success-bg`, `--sh-success-border`, e o mesmo para `warning`, `danger`, `info`. As bordas tonais (`#A8DEBF`, `#F2CD8A`, `#F4B4AE`, `#A9CFF2` no claro; `#2C6B47`, `#6B4E17`, `#7A302B`, `#2A5A91` no escuro) são decorativas, o texto e o ícone carregam o significado. Cada fg também passa sobre `--sh-surface` (claro 6,57 a 8,13:1; escuro 9,01 a 10,71:1).

Botões sólidos (texto branco): **Perigo** `#C0261B` claro (5,94:1) e `#D63A2E` escuro (4,66:1); **Sucesso** `#15803D` claro (5,02:1) e `#1A7F45` escuro (5,04:1). Esses sólidos são só para confirmar ação (excluir) e raros.

#### Estados de estoque (o conceito central do produto)

| Estado | Regra (RN-12) | Cor | Ícone (Bootstrap Icons) | Texto do badge | Reforço sem cor |
|---|---|---|---|---|---|
| Normal | saldo > mínimo | Sucesso | `check-circle-fill` | "Normal" | ícone de círculo com visto |
| Baixo | 0 < saldo ≤ mínimo | Aviso | `exclamation-triangle-fill` | "Baixo" | ícone de triângulo |
| Sem estoque | saldo = 0 | Perigo | `x-octagon-fill` | "Sem estoque" | ícone de octógono, linha da tabela com barra lateral de 4 px |

Tokens: `--sh-estoque-normal-*`, `--sh-estoque-baixo-*`, `--sh-estoque-sem-*` (apelidos dos pares de Sucesso, Aviso e Perigo). Três formas diferentes (círculo, triângulo, octógono) + palavra: o estado nunca depende só da cor.

Medidor de saldo (barra): preenchimento Normal `#15803D`, Baixo `#B25E09`, Sem estoque `#C0261B` sobre trilho `#DDE2EA` no claro (3,86 / 3,59 / 4,57:1 contra o trilho); `#4CC38A`, `#E8A93A`, `#F0645A` sobre trilho `#2A3341` no escuro (5,75 / 6,17 / 4,05:1).

Tipos de movimentação (outra família, para **não** gastar vermelho e âmbar): **Entrada** usa Sucesso e o sinal "+"; **Saída** usa Info e o sinal "−"; **Ajuste** usa neutro (`--sh-text-muted` sobre `--sh-surface-2`) e o sinal "=". Sempre sinal + ícone + palavra.

#### Mapeamento para o Bootstrap

`css/tokens.css` redefine as variáveis do Bootstrap 5.3 em `:root, [data-bs-theme="light"]` e `[data-bs-theme="dark"]`: `--bs-body-bg`, `--bs-body-color`, `--bs-border-color`, `--bs-link-color`, `--bs-primary` **e** `--bs-primary-rgb` (o Bootstrap usa o trio RGB em foco e sombras), `--bs-success-*`, `--bs-warning-*`, `--bs-danger-*`, `--bs-info-*`. `.btn-primary`, `.btn-danger` etc. recebem `--bs-btn-*` próprios. Motivo: o tema escuro nativo do Bootstrap (`data-bs-theme`) já cuida de modal, dropdown, tabela e formulário; só trocamos a paleta.

### 1.3 Tipografia

**Fonte:** **Inter** (variável, eixo de peso 400 a 700), licença SIL OFL 1.1. **Servida localmente**: arquivo `inter-latin-wght-normal.woff2` do pacote `@fontsource-variable/inter` (subset latino, cobre todos os acentos do português e `R$`, em torno de 50 KB; conferir o tamanho real no download), em `vendor/inter/`, com `font-display: swap` e `<link rel="preload" as="font" type="font/woff2" crossorigin>` no `head`. Texto de licença junto (`vendor/inter/OFL.txt`).

Pilha: `"Inter", system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`. Números com `font-variant-numeric: tabular-nums` (colunas de saldo e preço alinham).

**Motivo:** o CSS atual já pede Inter mas **nunca a carrega** (cai em fonte do sistema, resultado muda por máquina). Inter foi desenhada para interface, tem algarismos tabulares e boa leitura em tamanho pequeno.

**Descartado:** Google Fonts por CDN (dependência externa, envia IP do usuário ao Google, quebra a regra "sem CDN"); só fonte do sistema (zero peso, mas aparência e números variam entre Windows, Android e iOS); Roboto/Open Sans (sem vantagem sobre Inter aqui); Inter completa (cerca de 340 KB com alfabetos que não usamos).

**Escala** (raiz 16 px; corpo em 15 px; campos em 16 px para o iOS não dar zoom ao focar):

| Token | Tamanho | Peso | Uso |
|---|---|---|---|
| `--sh-text-sm` | 0,8125rem (13 px) | 500 | badges, legendas, dicas, rodapé de tabela |
| `--sh-text-md` | 0,875rem (14 px) | 400/500 | células de tabela, texto secundário |
| `--sh-text-base` | 0,9375rem (15 px) | 400 | corpo |
| `--sh-text-lg` | 1,0625rem (17 px) | 600 | título de cartão (h2), campos (16 px) |
| `--sh-text-xl` | 1,375rem (22 px) | 700 | título de página (h1) no celular |
| `--sh-text-2xl` | 1,75rem (28 px) | 700 | título de página (h1) no desktop |
| `--sh-text-metric` | 2rem (32 px) | 700 | número grande dos cartões de métrica |

Altura de linha: 1,5 no corpo, 1,25 nos títulos, 1,1 nos números grandes. Nada abaixo de 13 px. Sem `text-transform: uppercase` em rótulos (leitura pior e o CSS atual abusa); rótulo de cartão é 13 px / 500 / `--sh-text-muted`.

### 1.4 Espaçamento, raios, sombras

- **Espaço** (base 4 px): `--sh-space-1` 4, `-2` 8, `-3` 12, `-4` 16, `-5` 20, `-6` 24, `-8` 32, `-10` 40, `-12` 48 px. Gutter da página: 16 px no celular, 24 px no desktop. Largura útil do conteúdo: máx. 1200 px (listas) e 960 px (formulários).
- **Raios:** `--sh-radius-sm` 6 px (campos, botões, badges retangulares), `--sh-radius` 10 px (cartões, tabelas), `--sh-radius-lg` 14 px (modais, painéis flutuantes), `--sh-radius-pill` 999 px (badges, chips).
- **Sombras:** o visual é **plano**: cartões usam borda 1 px, sem sombra. Só elementos que flutuam têm sombra: `--sh-shadow-pop` (`0 8px 24px rgba(16,24,40,.12)`, dropdown, seletor, toast) e `--sh-shadow-modal` (`0 16px 48px rgba(16,24,40,.20)`). No escuro a sombra perde efeito, então a separação vem de `--sh-surface` mais clara que `--sh-bg` e da borda.
- **Descartado:** sombra colorida e cartões que sobem no hover (efeito do CSS atual): decoração sem informação e causa tremor de layout.

### 1.5 Ícones

**Bootstrap Icons 1.11.3** (MIT), fonte `woff2` local, mesma versão que o projeto já usa. Ícone sempre acompanhado de texto, ou com `aria-label` quando for só ícone; os decorativos levam `aria-hidden="true"`. Tamanhos: 16 px inline, 20 px na navegação, 40 px em estados vazios.

Mapa fixo: Início `house-door`; Produtos `box-seam`; Categorias `tags`; Movimentações `arrow-left-right`; Estoque baixo `exclamation-triangle`; Unidades `stack`; Valor `cash-coin`; Entrada `box-arrow-in-down`; Saída `box-arrow-up`; Ajuste `sliders`; Buscar `search`; Filtrar `funnel`; Novo `plus-lg`; Editar `pencil`; Excluir `trash3`; Tema `circle-half`/`sun`/`moon-stars`; Atalhos `keyboard`; Menu `list`; Sair `box-arrow-right`; Senha `eye`/`eye-slash`.

**Descartado:** Font Awesome (a versão grátis é parcial e pesada), Lucide e Phosphor em SVG (ótimos, mas sem build exigiriam montar sprite à mão; a troca não compensa), emoji (renderiza diferente em cada sistema).
**Otimização futura, não agora:** sprite SVG só com os ~35 ícones usados (a fonte inteira tem cerca de 130 KB e fica em cache).

### 1.6 Tema claro, escuro e do sistema

- Três opções no menu do usuário: **Sistema** (padrão), **Claro**, **Escuro**. Salvo em `localStorage` (`sh-tema`), sempre dentro de `try/catch`.
- Para não piscar branco, `js/tema-init.js` (arquivo externo, ~10 linhas, **síncrono no `head`**, antes do CSS) lê a escolha e define `data-bs-theme` em `<html>`. Sem JavaScript, vale `prefers-color-scheme` por CSS.
- `<meta name="color-scheme" content="light dark">` e `color-scheme` no CSS (barras de rolagem e campos nativos acompanham).
- Impressão sempre em tema claro (`css/impressao.css`).
- **Descartado:** só claro (balcão com pouca luz e uso à noite pedem escuro, e o Bootstrap 5.3 já traz o mecanismo); escolha só pelo sistema, sem botão (o usuário não consegue contornar).

## 2. Layout e navegação

### 2.1 Estrutura

```
Desktop (>= 992 px)                         Celular (< 992 px)
+--------+--------------------------+       +------------------------------+
| Lateral| Barra superior           |       | Barra superior (título, busca)|
| 248 px | (migalhas, busca, Novo,  |       +------------------------------+
| escura |  usuário)                |       | Conteúdo                     |
|        +--------------------------+       |                              |
|        | Cabeçalho da página      |       |                              |
|        | Conteúdo (máx. 1200 px)  |       +------------------------------+
+--------+--------------------------+       | Início|Produtos|(+)|Alertas|Mais|
                                            +------------------------------+
```

- **Barra lateral** 248 px; recolhível para 68 px (só ícones; os rótulos continuam no DOM como `visually-hidden`). Preferência em `localStorage`. Em 992 a 1199 px abre recolhida por padrão.
- **Barra superior** 56 px, fixa: migalhas (ou título no celular), campo de busca global, menu do usuário (nome da loja, tema, atalhos, Sair por POST com CSRF).
- **Cabeçalho da página** (`.sh-page-header`): `h1`, uma linha de descrição e as ações da página à direita.
- **Descartado:** barra de navegação só no topo (o menu tem 7 destinos agrupados, não cabe sem virar menu suspenso); sidebar clara como a atual (sem identidade, mistura com o conteúdo); sidebar recolhível por padrão no desktop grande (esconde os rótulos de quem ainda está aprendendo).

### 2.2 Menu

| Grupo | Item | Rota | Observação |
|---|---|---|---|
| (sem título) | Início | `/dashboard` | |
| Operação | Movimentações | `/estoque` | |
| | Produtos | `/produtos?tamanho=25` [F] | o parâmetro `tamanho` já existe (máx. 100); evita a página de 8 itens |
| | Categorias | `/categorias` | |
| Alertas e relatórios | Estoque baixo | `/estoque-baixo` | contagem em badge só com [B] opcional 6 |
| | Unidades | `/unidades` | |
| | Valor do estoque | `/valor-estoque` | |

Saem do menu os itens "Cadastro > Produto/Categoria" (duplicavam as ações das páginas). Criar algo novo passa a ser o botão **Novo** (menu suspenso: Produto, Categoria) na barra superior e o botão primário de cada lista. Item ativo: fundo `--sh-side-active-bg`, barra de 3 px `--sh-side-marker` e `aria-current="page"` (continua pela variável `activePage`).

### 2.3 Celular

**Barra inferior** (64 px + área segura do aparelho), 5 destinos: **Início**, **Produtos**, **Movimentar** (central, destacado, é a ação mais frequente), **Alertas** (`/estoque-baixo`), **Mais** (abre o offcanvas do Bootstrap com Categorias, Unidades, Valor do estoque, tema, atalhos e Sair).

**Motivo:** o polegar alcança a parte de baixo; Movimentar é a tarefa diária. **Descartado:** só o menu hambúrguer atual (esconde tudo atrás de dois toques); abas fixas com 7 itens (alvos pequenos).

Em formulários a barra inferior some e dá lugar à **barra de ações fixa** (Cancelar / Salvar), para não competir.

### 2.4 Migalhas e títulos

Só em páginas de profundidade 2 ou mais: Início › Produtos › *nome do produto* › Editar. `<nav aria-label="Trilha de navegação">` com `ol.breadcrumb`, último item com `aria-current="page"`. Nome do produto sempre via `<c:out>` e truncado com reticências em CSS. No celular a trilha vira só **‹ Produtos** (link para o nível acima). Páginas de lista mostram só o título.

### 2.5 Ações primárias sempre à mão

| Onde | Ação primária | Como fica visível |
|---|---|---|
| Qualquer tela, desktop | Botão **Novo** (suspenso) na barra superior | fixa no topo |
| Qualquer tela, celular | **Movimentar** no centro da barra inferior | fixa embaixo |
| Lista | botão primário no cabeçalho da página (Novo produto, Nova categoria) | |
| Linha de produto | **Movimentar** (e **Repor** quando Baixo ou Sem estoque) | em cada linha ou cartão |
| Formulário | Salvar / Cancelar na barra fixa inferior (`.sh-form-actions`) | `position: sticky; bottom: 0` |

### 2.6 Atalhos de teclado

Implementação em `js/atalhos.js`. Regras (WCAG 2.1.4): atalhos de **uma tecla** só valem quando o foco **não** está em campo de texto, `select`, `textarea` ou elemento editável, e nunca com Ctrl/Alt/Meta; o modal de ajuda tem a chave "Atalhos de teclado ativados" (desligar grava em `localStorage`).

| Tecla | Efeito |
|---|---|
| `/` | foca a busca (em `/produtos` e `/categorias`, o campo da lista; nas outras, a busca global) |
| `n` | novo produto (em `/categorias`, nova categoria) |
| `m` | abre Movimentações e foca o seletor de produto |
| `g` depois `i` / `p` / `c` / `b` / `e` / `r` | ir para Início / Produtos / Categorias / Estoque baixo / Movimentações / Valor do estoque |
| `?` | abre a ajuda de atalhos |
| `Esc` | fecha seletor, modal ou menu; no campo de busca, limpa |
| `Ctrl` + `Enter` | envia o formulário de movimentação, mesmo de dentro da observação |

**Paleta de comandos (Ctrl+K): descartada na v1.** O sistema tem cerca de 10 destinos e 3 ações, já cobertos por menu e atalhos `g`. O ganho real de uma paleta seria **saltar para um produto pelo nome**, e isso exige um endpoint de busca em JSON (não existe; o produto só é listado em `/produtos` com 8 a 100 itens e, na movimentação, todos de uma vez). Reavaliar na v2 junto com esse endpoint.

## 3. Bibliotecas

**Regras:** sem npm, sem build; tudo **local** em `src/main/webapp/vendor/`; nunca CDN. Como os arquivos são do mesmo domínio, não se usa SRI; a integridade fica registrada por **SHA-256** em `vendor/VERSOES.md` (conferido contra o pacote publicado no npm no momento do download).

### 3.1 Conjunto proposto

| Biblioteca | Versão | Licença | Arquivos | Peso aprox. | Por quê |
|---|---|---|---|---|---|
| Bootstrap | **5.3.8** (a mesma de hoje) | MIT | `vendor/bootstrap-5.3.8/css/bootstrap.min.css`, `js/bootstrap.bundle.min.js` | CSS ~230 KB, JS ~80 KB (gzip ~30 KB) | Já é a base; grade, formulários, modal, offcanvas, dropdown, toast e tema escuro nativo, todos com acessibilidade testada (foco preso no modal, Esc, `aria-*`). Trocar custaria reescrever todas as telas por nada |
| Bootstrap Icons | **1.11.3** (a mesma de hoje) | MIT | `vendor/bootstrap-icons-1.11.3/bootstrap-icons.min.css`, `fonts/bootstrap-icons.woff2` (e `.woff`, mantido como veio) | ~130 KB (woff2) | Mesma família do Bootstrap, cobre o mapa de ícones |
| Inter (subset latino, variável) | **5.x** do pacote `@fontsource-variable/inter` (fixar a exata no download) | SIL OFL 1.1 | `vendor/inter/inter-latin-wght-normal.woff2`, `OFL.txt` | ~50 KB | seção 1.3 |
| JS e CSS próprios | n/a | do projeto | `css/` e `js/` | ~25 KB de JS e ~25 KB de CSS sem minificar | seção 3.3 |

Como baixar na fase 3 (sem npm): uma vez, com `Invoke-WebRequest` a partir do jsDelivr/GitHub, gravar `Get-FileHash -Algorithm SHA256` de cada arquivo em `vendor/VERSOES.md` (nome, versão exata, licença, URL de origem, SHA-256). Remover o comentário final `//# sourceMappingURL` dos arquivos do Bootstrap só se o 404 do mapa incomodar (registrar a alteração e o hash do arquivo alterado).

**Pré-requisito [B1]:** hoje o `SecurityConfig` só libera `/css/**`, `/js/**`, `/img/**`, `/images/**` sem login. `/vendor/**` precisa entrar no `permitAll` (uma linha), senão a tela de login fica sem estilo. Alternativa sem backend, se preferirem: colocar tudo em `css/vendor/` e `js/vendor/` (com as fontes dentro de `css/vendor/...`).

**Observação de implantação (já existia):** os recursos de `src/main/webapp` só são servidos com `mvnw spring-boot:run` ou empacotando como WAR; num JAR executável nem as JSPs funcionam. Não muda com este redesenho.

### 3.2 Bibliotecas avaliadas e descartadas

| Alternativa | Decisão | Motivo |
|---|---|---|
| React / Vue / Svelte | descartado | exigem build e API JSON; o projeto é JSP renderizado no servidor e o requisito é manter assim |
| **htmx** | descartado (era a opção séria) | casa bem com HTML do servidor e roda sem backend novo (`hx-select`). Mas só temos **uma** interação que busca HTML do servidor (a busca ao digitar), que cabe em ~40 linhas de `fetch` + `DOMParser`. Adicionar a lib (~14 KB) e uma segunda linguagem de atributos pesa mais que o ganho. Reavaliar se passarem de 3 interações assim |
| Alpine.js | descartado | o build padrão usa `eval` e impede uma futura política CSP sem `unsafe-eval`; nossas interações são poucas e focadas |
| Tailwind | descartado | pede build (ou o script de desenvolvimento, que não é para produção) e conflita com as classes do Bootstrap |
| Tom Select / Select2 / Choices | descartado | o único seletor com busca é o de produto; escrevo-o seguindo o padrão ARIA *combobox* (~150 linhas), sem jQuery e sem CSS alheio. Risco assumido: se o componente crescer, trocar por Tom Select |
| jQuery, DataTables | descartado | paginação e ordenação já são do servidor |
| SweetAlert | descartado | o modal do Bootstrap já cobre confirmações, com foco gerenciado |
| Biblioteca de gráficos (Chart.js, uPlot, ApexCharts) | **descartado na v1** | o backend só entrega totais. A distribuição Normal / Baixo / Sem estoque sai de `resumo` e vira uma barra em HTML+CSS acessível, com a legenda em texto. Gráfico só se justifica quando existir série no tempo (movimentação por período, v2); aí preferir **Chart.js 4** (MIT, ~200 KB, mais documentado) ou **uPlot** (~50 KB) e registrar versão e hash |
| Dropzone, datepicker, máscaras | descartado | `<input type="date">` e `inputmode` nativos bastam |

### 3.3 JavaScript próprio (vanilla, sem build)

Arquivos pequenos em `js/`, carregados com `defer`, **sem `<script>` inline** e **sem atributo `style` inline** (deixa o caminho livre para um cabeçalho CSP `script-src 'self'`, ver [B9]). Dados vêm em atributos `data-*` escapados com `<c:out>`. **JS nunca monta HTML com dados do usuário:** usa `textContent` e `createElement`. A única exceção é a troca de resultados da busca ao digitar, que importa HTML do **próprio servidor** (já escapado pelas JSPs) via `DOMParser`.

| Arquivo | Função |
|---|---|
| `tema-init.js` | aplica o tema antes de pintar (no `head`, síncrono) |
| `nucleo.js` | tema (trocar), barra lateral recolhível, `prefers-reduced-motion`, utilitários |
| `toast.js` | transforma o alerta do servidor em toast (seção 4) |
| `atalhos.js` | atalhos e modal de ajuda |
| `formularios.js` | validação inline, contador de caracteres, bloqueio de envio duplo, mostrar/ocultar senha, aviso de alterações não salvas |
| `confirmar-exclusao.js` | o `app.js` atual, mantido (modal de exclusão) |
| `busca-viva.js` | busca ao digitar nas listas |
| `movimentacao.js` | seletor de produto, tipo segmentado, motivos, prévia de saldo, link direto (`movProduto`, `movTipo`); substitui `estoque.js` |
| `numeros.js` | contagem animada e destaque da linha nova |

## 4. Componentes e estados

Classes próprias com prefixo `sh-`, sobre o Bootstrap. Os arquivos CSS (seção 8, lote 1): `tokens.css`, `layout.css`, `componentes.css`, `impressao.css`.

### 4.1 Matriz de estados

"n/a" = não se aplica. Foco é sempre o anel `--sh-focus` (3 px, afastamento 2 px, `outline`, nunca removido); só mudo o que está na tabela.

| Componente | Vazio | Carregando | Erro | Sucesso | Desabilitado |
|---|---|---|---|---|---|
| **Botão** (`.btn`, `.btn-primary`, `-outline-secondary`, `-danger`, `.btn-ghost`) | n/a | ao enviar: vira "Salvando…" com spinner pequeno, `aria-busy="true"`, desabilitado | n/a (o erro aparece no formulário) | n/a (o sucesso vem no toast) | opacidade 0,55, `cursor: not-allowed`, `disabled`; o motivo aparece em texto ao lado quando não óbvio |
| **Campo** (`.form-control`, `.form-select`, `.sh-field`) | placeholder em `--sh-text-subtle` | n/a | borda `--sh-danger-fg` 2 px + ícone `exclamation-circle` + mensagem abaixo (`.invalid-feedback`, `aria-describedby`, `aria-invalid="true"`) | borda neutra; sem "verde de aprovado" (poluição) | fundo `--sh-surface-2`, texto `--sh-text-subtle`; somente leitura mostra o valor sem borda e sem aparência de campo |
| **Seletor de produto** (combobox) | "Nenhum produto com esse nome" + link "Cadastrar produto"; sem produtos cadastrados: estado vazio com CTA | n/a (lista local) | erro do servidor (`errosCampos['produtoId']`) no campo | cartão "Produto selecionado" com saldo e badge | `disabled` se não há produtos |
| **Controle segmentado** de tipo (3 rádios) | n/a | n/a | mensagem sob o grupo (`errosCampos['tipo']`) | opção marcada com fundo de acento e ícone de visto | opção cinza |
| **Chips de motivo** | n/a (sempre há motivos) | n/a | n/a | chip marcado (`aria-pressed="true"`) e texto aparece na observação | cinza quando o tipo não admite o motivo |
| **Tabela / lista** (`.sh-table`) | estado vazio com ícone, frase e botão (por tela, seção 7) | esqueleto de 5 linhas durante a busca ao digitar | linha de aviso "Não foi possível atualizar a lista" + botão Tentar de novo; falha de rede cai no envio normal do formulário | linha nova destacada (seção 5) | n/a |
| **Cartão de métrica** (`.sh-metric`) | "0" com frase explicando ("Cadastre o primeiro produto") | esqueleto da barra do número | n/a | número com contagem animada | n/a |
| **Badge de estoque** (`.sh-badge--normal/--baixo/--sem`) | n/a | n/a | n/a | n/a (ver 1.2) | n/a |
| **Medidor de saldo** (`.sh-meter`) | mínimo = 0: só o número, sem barra | n/a | n/a | enche de 0 ao valor ao carregar (400 ms) | n/a |
| **Alerta inline** (`.sh-alert--success/warning/danger/info`) | n/a | n/a | `role="alert"`, ícone + título + texto | `role="status"` | n/a |
| **Toast** (`.sh-toast`) | n/a | n/a | não se usa para erro (erro fica inline e persistente) | `role="status"`, some em 7 s, pausa com mouse ou foco | n/a |
| **Modal de confirmação** | n/a | botão Confirmar vira "Excluindo…" e fica desabilitado | falha: a página recarrega com o alerta do servidor | n/a | Confirmar desabilitado após o clique (evita duplo envio) |
| **Paginação** | oculta se só há 1 página | n/a | n/a | n/a | Anterior na página 1 e Próxima na última, `aria-disabled="true"` e fora da ordem de tabulação |
| **Filtros e busca** | "Nenhum resultado para 'x'" + botão "Limpar filtros" | opacidade 0,6 na lista + esqueleto | mensagem sob o campo | contador "N produtos" atualizado em região `aria-live="polite"` | n/a |
| **Linha do tempo** (histórico) | "Nenhuma movimentação ainda. Registre a primeira." + botão | esqueleto de 4 itens | n/a | item novo destacado | n/a |
| **Barra de ações do formulário** (`.sh-form-actions`) | n/a | Salvar em carregamento | resumo de erros acima do formulário | n/a | Salvar fica habilitado de propósito (o servidor valida); só desabilita após enviar |
| **Navegação** (lateral e inferior) | n/a | n/a | n/a | item atual marcado | n/a |

### 4.2 Botões

Tamanhos: `sm` 32 px (só desktop, dentro de tabelas), padrão 40 px, `lg` 48 px; **no celular todos têm no mínimo 44 px de altura**. Variações: **primário** (índigo, 1 por região), **secundário** (contorno neutro), **perigo** (sólido só dentro de modal; fora dele, contorno `.btn-outline-danger`), **fantasma** (`.btn-ghost`, ações discretas e ícone). Texto com verbo; botão só com ícone leva `aria-label` com o nome do item ("Excluir Camiseta azul"). Perigo nunca é o botão mais destacado da tela.

### 4.3 Formulários e validação inline

- Rótulo sempre visível acima do campo (nada de rótulo só em placeholder); obrigatório marcado com "(obrigatório)" no texto, não só com asterisco; dica abaixo em `--sh-text-muted`.
- Os atributos nativos (`required`, `min`, `max`, `minlength`) **continuam** (funciona sem JS). Com JS, o formulário ganha `novalidate` e `formularios.js` mostra mensagens em português **ao sair do campo** (e não a cada tecla), e ao enviar: foca o primeiro campo inválido e exibe um **resumo de erros** no topo (`role="alert"`, `tabindex="-1"`, lista de links para cada campo).
- Erros que vêm do servidor (`errosCampos`, `erro`) usam o mesmo desenho e os mesmos `id` (`erro-nome`, ...) e `aria-describedby` que as JSPs de hoje.
- Contador de caracteres em descrição (500) e observação (255): "230/255"; perto do limite o texto passa a "faltam 12" (não só cor). Sem `aria-live` a cada tecla.
- Números: `inputmode="numeric"` (quantidades) e `inputmode="decimal"` (preço, com prefixo "R$"); `type="number"` e os nomes de campo ficam como estão.
- **Envio duplicado bloqueado:** ao enviar, o botão desabilita e mostra "Salvando…". Importante na movimentação, onde um duplo clique registraria duas entradas. Ao voltar pelo histórico do navegador (`pageshow`), reabilita.
- Aviso de alterações não salvas ao sair de formulário de produto ou categoria (`beforeunload`, só se algo mudou).
- Senha: botão mostrar/ocultar (`type="button"`, `aria-pressed`); no cadastro, a regra "8 a 72 bytes" aparece como lista que marca o que já foi atendido (texto + ícone).

### 4.4 Tabelas responsivas

- **Desktop e tablet (≥ 768 px):** tabela real, cabeçalho em `--sh-surface-2`, linha de 52 px, numéricos alinhados à direita com `tabular-nums`, cabeçalhos ordenáveis são links (com `aria-sort` e ícone de seta) usando os parâmetros `ordenacao` e `direcao` que já existem; coluna "Situação" com badge.
- **Celular (< 768 px):** a **mesma** marcação vira **cartões** (`.sh-table--cards`): cada `td` mostra seu rótulo via `data-label` (texto fixo escrito na JSP). Como `display: block` faz alguns leitores de tela perderem a semântica de tabela, a tabela leva `role="table"`, `rowgroup`, `row`, `columnheader`, `cell` explícitos. **Verificar com NVDA** (item do checklist).
- **Descartado:** rolagem horizontal da tabela no celular (esconde a coluna de ação e o saldo); manter duas marcações (uma de tabela, outra de cartão), que seria mais robusta para leitor de tela mas duplica toda JSP.
- Linha com **Sem estoque** ganha barra lateral de 4 px `--sh-danger-fg`; **Baixo**, `--sh-warning-fg`. O badge continua obrigatório.

### 4.5 Cartões de métrica

`.sh-metric`: rótulo (13 px, muted), número (`--sh-text-metric`, tabular), frase de apoio, ícone num círculo tonal. Sem faixa colorida no topo. O cartão inteiro é **um** link (área de toque grande), com foco visível; nada de elevação no hover, só o fundo passa a `--sh-surface-2`. Contagem animada (seção 5). O cartão de problema ("Precisam de reposição") só ganha cor de aviso/perigo se N > 0; com 0 mostra tom de sucesso e o texto "Tudo em dia".

### 4.6 Alertas e toasts

- **Fluxo:** as páginas continuam com `alertas.jsp` renderizando `mensagem` e `erro` **no HTML** (funciona sem JS e é escapado com `<c:out>`). Com JS, `toast.js` converte as de **sucesso** em toast e remove o alerta inline; as de **erro** ficam inline, no topo do conteúdo, persistentes (WCAG 2.2.1: não some sozinho).
- Toast: canto inferior direito no desktop; no celular, **acima** da barra inferior, largura total menos o gutter. Contêiner `aria-live="polite"` presente desde o carregamento. Fecha com botão "Fechar" ou `Esc`. Dura 7 s, pausa ao passar o mouse ou focar. Máximo de 3 empilhados.
- Texto da movimentação: "Movimentação registrada: o saldo passou de X para Y un." (mensagem que o controller já envia).
- **Descartado:** toast para erro (some antes de ser lido); "Desfazer" no toast de exclusão (não há como desfazer: exclusão ou inativação já foi gravada, e movimentação é imutável por RN-06).

### 4.7 Modais e confirmações

Bootstrap Modal (foco preso, `Esc`, devolve o foco ao gatilho, já feito em `app.js`). Mantém o contrato atual: `form[data-confirmar-exclusao]` com `data-tipo`, `data-nome`, `data-aviso`. Mudanças: título "Excluir produto?", botão **Excluir produto** (perigo), "Cancelar" com foco inicial (`autofocus` no cancelar, a ação segura). O texto de aviso fica visível (ex.: "Se o produto tiver histórico, ele será inativado e o histórico preservado"). O modal de ajuda de atalhos é outro modal, mesmo padrão.

### 4.8 Paginação e filtros

- **Paginação** (`paginacao.jsp` unificado, inclusive para `/produtos`, que hoje tem uma cópia própria): "Mostrando 26–50 de 140" + janela de 5 páginas + Anterior/Próxima. Aceita o parâmetro `extra` com a query dos filtros (em `/produtos`, o atributo `parametrosFiltros` que o controller já monta, mais `direcao`). No celular: "‹ Anterior · 2 de 6 · Próxima ›". Seletor "Itens por página" (10, 25, 50, 100) que envia `tamanho`.
- **Filtros de `/produtos`** (`.sh-filters`): campo de busca (largura total), **chips de nível** como grupo de links ou rádios (Todos / Normal / Baixo / Sem estoque, valor `ok`, `baixo`, `semEstoque` do parâmetro `nivelEstoque`), seletor de categoria. Ordenar passa para o **cabeçalho da tabela** (some o "Ordenar" e "Ordem" separados). Filtros ativos viram **chips removíveis** ("Categoria: Bebidas ✕") e há "Limpar filtros".
- **Busca ao digitar [F]** (`busca-viva.js`): em formulário com `data-busca-viva`, 300 ms após parar de digitar faz `fetch` (GET, mesma URL e parâmetros), troca só `[data-regiao-viva]` pelo trecho equivalente da resposta, atualiza o endereço com `history.replaceState` e anuncia "N produtos encontrados" em região `aria-live="polite"`. Sem JS ou em falha, o formulário envia normalmente (Enter e botão **Buscar** continuam existindo).
  **Descartado:** filtrar só as linhas já carregadas (a lista tem 8 a 100 por página, filtrar a página engana o usuário).

### 4.9 Seletor de produto com busca (movimentação)

Resolve o problema do `<select>` com todos os produtos (achado em `mercado-requisitos.md`). **Progressivo:** o `<select name="produtoId">` continua na JSP, dentro de `<noscript>` e como fonte de dados; o JS o esconde e monta o componente acima dele.

- **Dados [F]:** cada `<option>` ganha `data-nome`, `data-saldo` e `data-minimo` (`produto.nome`, `produto.quantidade`, `produto.estoqueMinimo`, via `<c:out>`). A categoria **não** entra: `listarParaSelecao` não faz fetch da categoria e `open-in-view` está desligado, acessá-la na JSP daria erro de carregamento tardio (ver [B7]).
- **Padrão ARIA combobox** (APG): `input` com `role="combobox"`, `aria-expanded`, `aria-controls`, `aria-autocomplete="list"`, `aria-activedescendant`; lista `role="listbox"`, itens `role="option"` com `aria-selected`. Teclas: ↓ ↑ navegam, Enter escolhe, Esc fecha (e, fechado, limpa), Home/End vão ao primeiro/último, digitar filtra.
- **Busca:** ignora maiúsculas e acentos (`normalize('NFD')`), casa todas as palavras digitadas em qualquer ordem; mostra até 50 resultados e, se houver mais, "Refine a busca para ver os outros N". Trecho encontrado em `<mark>` criado por nós de texto (nunca `innerHTML`).
- **Item:** nome em negrito, "Saldo 12 un." e o badge de estoque. Alvo de toque de 44 px; no celular a lista abre larga, logo abaixo do campo.
- **Selecionado:** o campo vira um cartão "Produto" com nome, saldo atual, mínimo, badge, link "Ver produto" e botão "Trocar".
- **Link direto [F]:** `/estoque?movProduto=ID&movTipo=ENTRADA|SAIDA|AJUSTE` pré-seleciona produto e tipo. O JS lê a query, valida (`ID` numérico existente nas opções, tipo na lista fixa) e só então muda o DOM; o servidor ignora os parâmetros. É o que faz o botão **Repor** funcionar sem backend. Nomes `mov*` escolhidos para não colidir com os filtros do histórico propostos em [B2].

### 4.10 Tipo e motivos pré-definidos

- **Tipo:** três rádios com `name="tipo"` e valores `ENTRADA`, `SAIDA`, `AJUSTE` apresentados como controle segmentado (cada um com ícone, rótulo e dica de uma linha: "soma ao saldo", "subtrai do saldo", "define o saldo final"). Substitui o `<select>`. Os rótulos de quantidade ("Quantidade" / "Contagem final"), `min`/`max` e a obrigatoriedade da observação no ajuste (RN-05) seguem a lógica de `estoque.js`.
- **Quantidade:** campo grande com botões − e + de 44 px.
- **Motivos [F]:** chips acima da observação; clicar **preenche** `observacao` com o texto do chip (usuário pode complementar; limite 255; trocar de chip substitui só se o texto ainda for o do chip anterior). Não existe campo novo nem mudança de backend. O motivo "Estoque inicial" é reservado ao sistema (nota de RN-04/RN-08) e **nunca** aparece.

| Tipo | Chips |
|---|---|
| Entrada | Compra de fornecedor · Devolução de cliente · Reposição |
| Saída | Venda · Perda · Quebra · Vencimento · Furto ou extravio |
| Ajuste (obrigatório) | Correção de contagem · Inventário · Erro de lançamento |

- **Prévia de saldo:** abaixo da quantidade, "Saldo: **12** → **7** un." com o badge do novo nível. Saída maior que o saldo mostra aviso "Saída maior que o saldo (12 un.): o sistema vai recusar", **sem bloquear o envio**: o saldo da página pode estar desatualizado (outra pessoa pode ter dado entrada), a decisão é do servidor (RN-02 e RN-07). Ajuste mostra a diferença ("−5 em relação ao saldo atual").

### 4.11 Histórico em linha do tempo

`.sh-timeline`: grupos por dia ("26/10/2026" como `h3`), e dentro de cada um itens com horário (`<time datetime>`), ícone do tipo num círculo (cores da seção 1.2), nome do produto (link para o detalhe), **delta** ("+12", "−5", "= 40 contagem") em `tabular-nums`, "12 → 7" (antes → depois) e a observação como motivo. Sem coluna de usuário (o usuário é a própria loja). O agrupamento por dia é feito na JSP comparando `fn:substring(dt, 0, 10)` com o item anterior, como já se extrai a data hoje.
**Motivo:** a tabela de 6 colunas some no celular; a linha do tempo conta "o que aconteceu" em ordem e ocupa menos largura.

### 4.12 Outros

- **Estado vazio** (`estado-vazio.jsp`): ícone 40 px, título curto, uma frase, botão de ação. Um por tela (seção 7).
- **Esqueleto** (`.sh-skeleton`): barras cinza `--sh-surface-2` com brilho de 1,2 s; sem movimento se o usuário pediu menos animação.
- **Medidor de saldo** (`.sh-meter`): barra cheia até `quantidade / (2 × mínimo)` (máx. 100 %), marcador fixo no mínimo (50 %); mínimo 0 mostra só o número. A largura sai por **classes de degrau** (`.sh-w-0`, `.sh-w-5`, ..., `.sh-w-100`, arredondando a múltiplos de 5), sem `style` inline. `role="img"` com `aria-label="Saldo 3 de mínimo 10"`.
- **Barra de saúde do estoque** (dashboard): três segmentos Normal / Baixo / Sem estoque (`resumo.totalProdutos − resumo.produtosEstoqueBaixo`; `resumo.produtosEstoqueBaixo − resumo.produtosSemEstoque`; `resumo.produtosSemEstoque`), com legenda em texto e contagem. A barra é decorativa (`aria-hidden`), a legenda é a informação.
- **Includes novos:** `badge-estoque.jsp` (param `nivel`: `${produto.nivelEstoque}`), `medidor-estoque.jsp`, `estado-vazio.jsp`, `breadcrumb.jsp`, `cabecalho-pagina.jsp`. Layout base como **tag file** `WEB-INF/tags/layout.tag` (atributos `titulo`, `ativo`, fragmento `scripts`, `<jsp:doBody/>`), que concentra `head`, barra lateral, superior, inferior, toasts e scripts: acaba a repetição do `head` em cada JSP. Se tag file der problema no Jasper do Spring Boot, o plano B é um par `layout-inicio.jsp` / `layout-fim.jsp` incluído com `jsp:include`.

## 5. Movimento

**Princípio:** animação só para explicar uma mudança ("isto acabou de entrar", "isto foi salvo", "o número é este"). Anima só `opacity`, `transform` e cores. Nada de elevação ao passar o mouse.

Tokens: `--sh-dur-fast: 120ms` (hover, foco, cor), `--sh-dur: 200ms` (painéis, toasts), `--sh-dur-slow: 320ms` (offcanvas, barra lateral), `--sh-ease-out: cubic-bezier(.2,.8,.2,1)` (entradas), `--sh-ease-in: cubic-bezier(.4,0,1,1)` (saídas), `--sh-ease: cubic-bezier(.4,0,.2,1)` (demais).

| Onde | Efeito | Duração e easing |
|---|---|---|
| Toast | entra subindo 8 px com fade; sai só com fade | 200 ms ease-out / 150 ms ease-in |
| Linha ou item **recém-criado** | fundo `--sh-accent-soft` que desbota para transparente + barra de 3 px de acento | 2,4 s ease-out; a barra fica até recarregar |
| Contagem animada dos números do dashboard e detalhe | do 0 ao valor; moeda em formato pt-BR | 700 ms ease-out, **só na primeira exibição** da página |
| Medidor de saldo | largura de 0 ao valor | 400 ms ease-out |
| Prévia "12 → 7" | cruzamento de opacidade do número e transição de cor do badge | 120 ms e 200 ms |
| Busca ao digitar | lista a 60 % de opacidade enquanto carrega, volta a 100 % ao trocar | 120 ms |
| Barra lateral recolhível, offcanvas, modal | largura / `transform` / fade nativos | 320 ms ease; fade do modal do Bootstrap |
| Esqueleto | brilho horizontal contínuo | 1,2 s linear, infinito |

**Destaque da linha alterada, sem backend [F]:** depois de movimentar, o controller redireciona para `/estoque` e o item mais recente é o **primeiro** do histórico (página 1, ordem decrescente). A JSP marca `data-destacar-primeiro` na lista **só** quando existe `mensagem` (flash de sucesso), e `numeros.js` destaca o primeiro item. Para produtos e categorias o destaque exige saber qual linha mudou: [B5], opcional.

**`prefers-reduced-motion: reduce` (obrigatório):**

1. CSS global em `tokens.css`: zera `animation-duration`, `transition-duration` e `scroll-behavior` (`0.01ms`, `animation-iteration-count: 1`) para todos os elementos.
2. `nucleo.js` expõe `reduzMovimento()` (`matchMedia`). Com ele ligado: a contagem mostra o valor final direto; o destaque de linha vira **estático** (mesmo fundo e barra, sem desbotar); o medidor aparece cheio; o toast aparece sem deslizar.
3. Esqueleto sem brilho (cinza fixo); spinner substituído por texto "Carregando…" com ícone estático.
4. O Bootstrap 5.3 já respeita a preferência nas transições dele (`$enable-reduced-motion`); a regra 1 cobre o restante.
5. Nenhuma informação é dada **só** por movimento (o toast e o texto dizem o que houve).

## 6. Acessibilidade e responsividade

### 6.1 Pontos de quebra (os do Bootstrap)

| Largura | Comportamento |
|---|---|
| < 576 px | uma coluna; barra inferior; tabelas em cartões; ações empilhadas |
| 576 a 767 px | idem, grades de 2 colunas em métricas |
| 768 a 991 px | tabelas voltam a ser tabelas; ainda sem barra lateral (barra inferior + offcanvas) |
| 992 a 1199 px | barra lateral recolhida (68 px); métricas em 3 colunas |
| 1200 px ou mais | barra lateral aberta; movimentação em 2 colunas (formulário 380 px fixo + histórico); métricas em 4 colunas |
| 1400 px ou mais | só aumenta a margem lateral; o conteúdo continua com máx. 1200 px |

Testar em 320, 360, 390, 768, 1024, 1440 px, com zoom de 200 % e **reflow a 320 px** sem rolagem horizontal (WCAG 1.4.10).

### 6.2 Regras

- **Alvo de toque:** 44 × 44 px no celular para qualquer controle (botões, chips, linhas de menu, itens do seletor, paginação); 40 px no desktop (mínimo absoluto da WCAG 2.2 é 24 px). Espaço de 8 px entre alvos vizinhos.
- **Foco visível:** `outline: 3px solid var(--sh-focus); outline-offset: 2px` em **tudo** que recebe foco, em `:focus-visible`; nunca `outline: none` sem substituto. Contraste do anel ≥ 3:1 contra os dois fundos (verificado: ≥ 7,3:1 no claro e ≥ 7,9:1 no escuro).
- **Ordem de tabulação** = ordem visual: link "Pular para o conteúdo" (primeiro elemento, aparece ao focar) → barra lateral → barra superior → conteúdo → barra de ações do formulário. No celular: barra superior → conteúdo → barra inferior. Nenhum `tabindex` positivo.
- **Marcos:** `header`, `nav aria-label="Menu principal"`, `main id="conteudo"`, `nav aria-label="Trilha de navegação"`; um `h1` por página, sem pular níveis.
- **Regiões vivas:** contêiner de toasts `aria-live="polite"`; resumo de erros `role="alert"`; contador de resultados `aria-live="polite"`; atualização do seletor de produto anunciada ("3 produtos encontrados").
- **Não depender só de cor:** estado de estoque = ícone + palavra + cor; tipo de movimentação = sinal + ícone + palavra; erro de campo = ícone + texto + borda; item ativo do menu = barra + `aria-current`.
- **Contraste:** texto ≥ 4,5:1 e bordas de controle ≥ 3:1 nos dois temas (valores calculados na seção 1.2). Texto desabilitado é isento, mas mantém ≥ 3:1 sempre que possível.
- **Formulários:** `label` em todo campo, `autocomplete` correto (`username`, `current-password`, `new-password`, `organization`), mensagens ligadas por `aria-describedby`, `aria-invalid` nos inválidos.
- **Imagens:** só decorativas (`aria-hidden`); se houver imagem informativa, `alt` descritivo.
- **Idioma:** `<html lang="pt-BR">`, datas `dd/MM/aaaa HH:mm`, moeda "R$ 1.234,56".
- **Zoom e reflow:** sem `maximum-scale`; texto em `rem`; nada de largura fixa em px nos contêineres de conteúdo.

### 6.3 Checklist verificável (cada item é testável antes de fechar um lote)

| # | Item | Como verificar |
|---|---|---|
| 1 | Todas as páginas navegáveis só com teclado, sem armadilha | percorrer com Tab e Shift+Tab, abrir e fechar modal, offcanvas e seletor |
| 2 | Foco sempre visível, anel ≥ 3:1 | Tab em cada tipo de controle, nos 2 temas |
| 3 | Link "Pular para o conteúdo" funciona | Tab na primeira vez, Enter, foco vai ao `main` |
| 4 | Ordem de tabulação igual à ordem visual | percorrer; conferir que a barra lateral não vem depois do conteúdo |
| 5 | Contraste de texto ≥ 4,5:1 e de bordas de controle ≥ 3:1, claro e escuro | axe DevTools / Lighthouse; conferir os tokens da seção 1.2 |
| 6 | Estado de estoque identificável em tons de cinza | emular "achromatopsia" no DevTools; badge continua distinguível |
| 7 | Cada campo tem `label` associado; erros ligados por `aria-describedby` | axe + inspeção |
| 8 | Um `h1` por página, hierarquia sem saltos | extensão HeadingsMap ou inspeção |
| 9 | Toast anunciado por leitor de tela sem roubar o foco | NVDA + Firefox/Chrome: aparece o texto, o foco não muda |
| 10 | Erro de validação foca o primeiro campo inválido e lista o resumo | enviar formulário vazio |
| 11 | Seletor de produto opera por teclado (↓ ↑ Enter Esc) e anuncia opções | NVDA ou Narrador |
| 12 | Tabela em cartões continua sendo lida como tabela | NVDA em 360 px: navegação de tabela (`Ctrl+Alt+setas`) lê cabeçalho e célula |
| 13 | Alvos de toque ≥ 44 px no celular | emulação 390 px, medir botões, chips, itens de menu e de paginação |
| 14 | Sem rolagem horizontal a 320 px e a 400 % de zoom | DevTools 320 px; zoom do navegador |
| 15 | `prefers-reduced-motion` respeitado | ativar no DevTools (Rendering): sem contagem, desbotar, brilho nem deslizar |
| 16 | Atalhos de uma tecla não disparam ao digitar e podem ser desligados | digitar "n" no campo de busca; desligar no modal `?` |
| 17 | Tema escuro completo, sem texto ilegível, sem branco piscando ao carregar | alternar o tema; recarregar com tema escuro salvo |
| 18 | `<c:out>` em todo dado exibido; nenhum `innerHTML` com dado do usuário | `grep` por `\${` fora de `c:out`/`c:if`/atributos fixos nas JSPs; `grep innerHTML js/` |
| 19 | Todo `form method="post"` tem o token CSRF | `grep` de `method="post"` x `_csrf.parameterName` |
| 20 | Rotas e `name` dos campos iguais aos atuais | comparar com a seção 7; rodar `./mvnw test` |
| 21 | Sem recursos de CDN | `grep` por `cdn.` e `https://` em `webapp/` |
| 22 | Sem `<script>` nem `style=` inline | `grep` em `webapp/WEB-INF` |
| 23 | Impressão limpa (sem menu nem botões) em estoque baixo, unidades e valor | Ctrl+P |
| 24 | Envio duplo bloqueado | clicar duas vezes em "Registrar movimentação": só um registro |
| 25 | Textos em português com acentos, `lang="pt-BR"` | inspeção |

## 7. Mapa de telas

Rotas e `name` de campos **não mudam**. Toda tela inclui: `usuarioLogado` (do `GlobalModelAdvice`, usado só para `nome` da loja) e os flashes `mensagem` e `erro`. Os atributos abaixo são os que os controllers fornecem hoje.

### 7.1 Login e cadastro (`login.jsp`)
- **Rotas:** `GET /login`, `GET /cadastro`, `POST /login` (`email`, `senha`, tratado pelo Spring Security), `POST /cadastro` (`nome`, `email`, `senha`), `POST /logout`.
- **Objetivo:** entrar ou criar a loja em poucos segundos.
- **Conteúdo:** em ≥ 992 px, duas colunas: painel de marca (tinta, logotipo, frase "Saiba o que tem, o que falta e o que saiu." e 3 benefícios curtos) + formulário; no celular só o formulário com o logotipo no topo. Botão de tema no canto.
- **Ações:** Entrar / Criar minha loja; mostrar senha; link para o outro modo.
- **Estados:** erro de login com mensagem única e genérica (não revela se o e-mail existe), `role="alert"`; sucesso ("Você saiu", "Cadastro concluído") em alerta `role="status"`; enviando: botão "Entrando…" desabilitado; erros por campo no cadastro; senha nunca volta preenchida.
- **Dados:** `erro`, `sucesso`, `modoCadastro`, `errosCampos` (`nome`, `email`, `senha`), `nome`, `email`, `_csrf`.
- **Só front [F].**

### 7.2 Dashboard (`dashboard.jsp`, rotas `/` e `/dashboard`)
- **Objetivo:** dizer em 5 segundos o que exige ação hoje.
- **Conteúdo, nesta ordem:**
  1. cabeçalho "Início" + botões **Registrar movimentação** (primário) e **Novo produto**;
  2. **painel "Precisam de reposição"** (destaque): número `resumo.produtosEstoqueBaixo` com "X sem estoque" (`resumo.produtosSemEstoque`), lista de até 6 de `produtosEstoqueBaixo` com nome, saldo/mínimo, medidor, badge e botão **Repor** (`/estoque?movProduto=ID&movTipo=ENTRADA`), link "Ver todos";
  3. **barra de saúde do estoque** (Normal / Baixo / Sem estoque, cálculo na 4.12);
  4. cartões: Produtos (`resumo.totalProdutos`), Unidades (`resumo.quantidadeTotalItens`), Valor do estoque (`resumo.valorTotalEstoque`), Categorias (`resumo.totalCategorias`), cada um um link para a lista correspondente;
  5. **Produtos recentes** (`produtosRecentes`, 6): nome, categoria, preço, badge.
- **Ações:** Registrar movimentação, Novo produto, Repor, abrir produto.
- **Estados:** loja nova (`resumo.totalProdutos == 0`): painel de **primeiros passos** em 3 etapas (1 Criar categoria, 2 Cadastrar produto, 3 Registrar entrada), marcando o que já foi feito (`totalCategorias > 0`, `totalProdutos > 0`); sem alertas: "Tudo em dia" com ícone de visto; carregando: não há (renderização no servidor).
- **Dados:** `resumo` (`totalProdutos`, `totalCategorias`, `produtosEstoqueBaixo` que inclui o sem estoque, `produtosSemEstoque`, `quantidadeTotalItens`, `valorTotalEstoque`), `produtosRecentes`, `produtosEstoqueBaixo` (6 de menor saldo; cada `Produto` traz `categoria.nome` e `nivelEstoque`).
- **Só front [F].**

### 7.3 Produtos (`produtos.jsp`, `GET /produtos`)
- **Objetivo:** achar um produto, ver o estado do saldo e agir (movimentar, editar).
- **Conteúdo:** cabeçalho com **Novo produto**; filtros (busca, chips de nível, categoria, "Itens por página"); contador "N produtos"; **tabela** com Produto (nome + descrição curta), Categoria, Preço, Saldo (número + medidor), Situação (badge), Ações; paginação.
- **Ações por linha:** **Movimentar** (ou **Repor** se Baixo ou Sem estoque), Editar, Excluir (ícone `trash3`, `.btn-ghost` perigo, com `aria-label`; confirma no modal). Clicar no nome abre o detalhe.
- **Estados:** vazio sem produtos ("Cadastre seu primeiro produto" + botão); vazio com filtros ("Nenhum produto encontrado" + Limpar filtros); busca viva carregando (esqueleto); sucesso por toast (`mensagem`: excluído ou inativado); ordenação por cabeçalho com `aria-sort`.
- **Dados:** `produtos`, `categorias` (para o seletor), `busca`, `categoriaId`, `nivelEstoque`, `ordenacao` (`id`, `nome`, `preco`, `quantidade`, `categoria`), `direcao`, `direcaoInversa`, `paginaAtual`, `totalPaginas`, `totalProdutosFiltro`, `tamanho`, `parametrosFiltros`. Formulário de exclusão: `POST /produtos/{id}/excluir` com CSRF.
- **[F]:** tudo acima, inclusive busca ao digitar.
- **[B] depois:** botão **Exportar CSV** respeitando os filtros ([B4]).

### 7.4 Formulário de produto (`produto-form.jsp`)
- **Rotas:** `GET /produtos/novo`, `GET /produtos/{id}/editar`, `POST /produtos` (`id`, `nome`, `descricao`, `categoriaId`, `preco`, `quantidade`, `estoqueMinimo`).
- **Objetivo:** cadastrar ou corrigir dados sem confusão sobre o saldo.
- **Conteúdo:** migalhas; 2 seções (Informações; Estoque e preço); no **cadastro**, "Estoque inicial" (fica registrado como entrada, RN-04); na **edição**, bloco informativo "Saldo atual: 12 un." com link **Movimentar** (RN-03) e um `input type="hidden" name="quantidade"` com o valor atual, para manter o contrato; "Estoque mínimo" com dica "Abaixo disso o produto aparece como Baixo". Se não há categoria, aviso com link para criar uma. Na edição, "Zona de perigo" com **Excluir produto** (formulário separado, `data-confirmar-exclusao`, CSRF).
- **Estados:** erros por campo e resumo (`errosCampos`: `nome`, `descricao`, `categoriaId`, `preco`, `quantidade`, `estoqueMinimo`), salvando, alterações não salvas.
- **Dados:** `produto` (`ProdutoForm`), `categorias`, `erro`, `errosCampos`; `edicao` é calculado na própria JSP. **[F].**

### 7.5 Detalhe do produto (`produto-detalhe.jsp`, `GET /produtos/{id}`)
- **Objetivo:** ver tudo de um item e agir.
- **Conteúdo:** migalhas; título, categoria, badge; ações **Movimentar** (primário) / **Repor**, Editar, Excluir; cartões Saldo (com medidor e "mínimo N"), Preço, Valor em estoque (`produto.valorTotal`), Estoque mínimo; descrição ("Sem descrição cadastrada" se vazia); **Histórico do produto** (linha do tempo, últimas 10).
- **Estados:** produto inexistente ou de outra loja: 404 (já tratado pelo servidor).
- **Dados:** `produto` (`id`, `nome`, `descricao`, `preco`, `quantidade`, `estoqueMinimo`, `categoria.nome`, `valorTotal`, `nivelEstoque`).
- **[F]:** tudo, **exceto** o histórico.
- **[B3]:** atributo `movimentacoesProduto` (lista das últimas movimentações do produto). A seção só é renderizada se o atributo existir (`movimentacoesProduto != null`), assim não mostra "vazio" enganoso antes de o backend entregar. Com ele, também o link "Ver histórico completo" (`/estoque?produto=ID` com [B2]).

### 7.6 Categorias (`categorias.jsp`, `GET /categorias?busca=`)
- **Objetivo:** organizar o catálogo.
- **Conteúdo:** cabeçalho com **Nova categoria**; busca (também ao digitar); tabela (cartões no celular): Nome, Descrição, Nº de produtos (`totalProdutos`) com link **Ver produtos** (`/produtos?categoriaId=ID`, filtro que já existe), Ações Editar e Excluir (modal; o aviso de RN-09 continua no `data-aviso`).
- **Estados:** vazio ("Crie categorias para agrupar seus produtos" + botão); erro de exclusão por flash `erro` (inline, persistente, com a contagem de produtos que a regra RN-09 devolve); sucesso por toast.
- **Dados:** `categorias` (`id`, `nome`, `descricao`, `totalProdutos`), `busca`. Exclusão: `POST /categorias/{id}/excluir` com CSRF. **[F].**

### 7.7 Formulário de categoria (`categoria-form.jsp`)
- **Rotas:** `GET /categorias/novo`, `GET /categorias/{id}/editar`, `POST /categorias` (`id`, `nome`, `descricao`).
- **Conteúdo:** migalhas, 2 campos com contador na descrição, barra de ações fixa. **Dados:** `categoria` (`CategoriaForm`), `erro`, `errosCampos` (`nome`, `descricao`). **[F].**

### 7.8 Movimentações (`estoque.jsp`, `GET /estoque`)
- **Objetivo:** registrar o que entrou, saiu ou foi corrigido, e conferir o histórico.
- **Conteúdo:** à esquerda (≥ 1200 px; no celular, em cima) o **painel "Registrar movimentação"**: seletor de produto com busca (4.9), tipo segmentado, quantidade com − e +, **prévia de saldo**, chips de motivo, observação com contador, botão **Registrar** (com `Ctrl+Enter`). À direita (embaixo no celular) o **histórico em linha do tempo**; contador `totalMovimentacoes`; paginação.
- **Ações:** registrar; abrir o produto pelo histórico; mudar de página.
- **Estados:** sem produtos ("Cadastre um produto para movimentar" + botão); histórico vazio ("Nenhuma movimentação ainda"); erro de regra (`erro`, ex.: saldo insuficiente): alerta inline no topo do painel e o formulário volta com o que foi digitado; erro de campo (`errosCampos`: `produtoId`, `tipo`, `quantidade`, `observacao`); sucesso por toast com "saldo passou de X para Y" e o primeiro item do histórico destacado; enviando: botão bloqueado.
- **Dados:** `produtos` (para o seletor: `id`, `nome`, `quantidade`, `estoqueMinimo`), `movimentacoes` (`criadoEm`, `produto.nome`, `produto.categoria.nome`, `tipo`, `quantidade`, `quantidadeAnterior`, `quantidadeNova`, `observacao`), `paginaAtual`, `totalPaginas`, `totalMovimentacoes`, `tipos`, `movimentacaoForm`, `erro`, `errosCampos`. `POST /estoque/movimentar` com CSRF.
- **[F]:** formulário completo, motivos, prévia, link direto, linha do tempo, destaque.
- **[B2]:** filtros do histórico (produto, tipo, de, até) e **[B4]** exportar CSV do histórico. Desenho já previsto: barra de filtros compacta acima da linha do tempo, sem alterar o painel de registro; parâmetros propostos `produto`, `tipo`, `de`, `ate`.

### 7.9 Estoque baixo (`estoque-baixo.jsp`, `GET /estoque-baixo`)
- **Objetivo:** a lista de compras do dono.
- **Conteúdo:** cabeçalho com contador e botão **Imprimir lista**; tabela (cartões no celular): Produto, Categoria, Saldo / Mínimo com medidor, "Faltam N para o mínimo" (`estoqueMinimo − quantidade`), badge, ação **Repor** (abre movimentação de Entrada) e Ver. Primeiro os Sem estoque (a ordem já vem do serviço, menor saldo primeiro).
- **Estados:** vazio positivo ("Nenhum produto abaixo do mínimo. Tudo em dia.", ícone de visto, tom de sucesso); impressão em `@media print`: só a tabela, com coluna em branco "Comprar" para anotar.
- **Dados:** `produtos`, `paginaAtual`, `totalPaginas`, `totalProdutosFiltro`. **[F].**

### 7.10 Unidades (`unidades.jsp`, `GET /unidades`)
- **Objetivo:** quanto há em estoque, por produto.
- **Conteúdo:** alternância Unidades | Valor (links entre as duas telas de relatório), cartão "Total de unidades" (`resumo.quantidadeTotalItens`), tabela: Produto, Categoria, Quantidade, Mínimo, Situação, Ver; botão **Imprimir**; paginação (100 por página).
- **Dados:** `resumo`, `produtos`, `paginaAtual`, `totalPaginas`, `totalProdutosFiltro`. **[F].**
- **[B] depois (v2):** valor por categoria e outros relatórios do `mercado-requisitos.md`.

### 7.11 Valor do estoque (`valor-estoque.jsp`, `GET /valor-estoque`)
- **Conteúdo:** cartão "Valor total" (`resumo.valorTotalEstoque`); tabela: Produto, Categoria, Preço, Quantidade, Total (`produto.valorTotal`) e **participação no valor** (barra em degraus `.sh-w-*` + percentual calculado na JSP com `valorTotal / resumo.valorTotalEstoque`, protegendo divisão por zero); botão **Imprimir**.
- **Dados:** `resumo`, `produtos`, `paginaAtual`, `totalPaginas`, `totalProdutosFiltro`. **[F].**

### 7.12 Erro (`error.jsp`)
- Página independente (sem barra lateral; funciona sem login). Tokens novos, código do erro grande, título e frase amigáveis por status (já existem: 400, 403, 404, 405 e genérico), **nunca** stack trace, botão **Voltar ao início** (e **Entrar** quando o status for 403 sem sessão). Botão de tema. **Dados:** `status`, `path`. **[F].**

### 7.13 Resumo do que é só front e do que pede backend

| Só front [F] | Exige backend [B] |
|---|---|
| Todo o visual, layout, tema, componentes, responsividade, acessibilidade | **B1** liberar `/vendor/**` no `SecurityConfig` (pré-requisito do lote 1) |
| Busca ao digitar, chips de nível, ordenação por cabeçalho, tamanho de página | **B2** filtros do histórico: `produto`, `tipo`, `de`, `ate` em `GET /estoque` |
| Seletor de produto com busca, tipo segmentado, motivos, prévia de saldo | **B3** histórico por produto em `GET /produtos/{id}` (`movimentacoesProduto`) |
| Botão **Repor** e link direto `?movProduto&movTipo` | **B4** exportação CSV de produtos e do histórico |
| Barra de saúde do estoque, primeiros passos, destaque do item novo no histórico | Opcionais: **B5** `idDestaque` no flash; **B6** contagem de alertas global; **B7** categoria carregada em `listarParaSelecao`; **B8** tamanho padrão 25 em `/produtos`; **B9** cabeçalho CSP |
| Impressão por CSS | |

## 8. Escopo da implementação (fase 3), em lotes

Estratégia de convivência: arquivos **novos** (`css/tokens.css`, `layout.css`, `componentes.css`, `impressao.css`, `tags/layout.tag`, includes e JS novos) ao lado dos antigos. As telas migram uma a uma para o layout novo; as ainda não migradas continuam com `style.css` e os includes velhos. Cada lote deixa o sistema funcionando, passa em `./mvnw test` e vira **um commit por assunto** (mensagem em português no imperativo). O legado só é apagado no lote 5.

| Lote | Conteúdo | Pronto quando | Commit sugerido |
|---|---|---|---|
| **0** (`dev-backend`) | **B1**: `/vendor/**` no `permitAll`; teste de que `/vendor/...` abre sem login | login carrega os arquivos de `vendor/` sem sessão | "Libera os arquivos de vendor sem login" |
| **1 Fundação** | baixar Bootstrap 5.3.8, Bootstrap Icons 1.11.3 e Inter para `vendor/` com `VERSOES.md` (versão, licença, URL, SHA-256); `tokens.css` (cores claro e escuro, tipografia, espaço, raios, sombras, movimento, mapeamento `--bs-*`, regra de `prefers-reduced-motion`); `layout.css`; `componentes.css` (botão, campo, badge, medidor, alerta, toast, tabela e cartões, esqueleto, estado vazio, paginação, linha do tempo); `impressao.css`; `tags/layout.tag`, barra lateral, superior, inferior e offcanvas "Mais"; `breadcrumb.jsp`, `cabecalho-pagina.jsp`, `badge-estoque.jsp`, `medidor-estoque.jsp`, `estado-vazio.jsp`, `paginacao.jsp` novo, `modal-exclusao`, modal de atalhos; JS: `tema-init`, `nucleo`, `toast`, `atalhos`, `formularios`, `confirmar-exclusao`; arquivo opcional `css/guia-componentes.html` (público, sem dados) para ver todos os componentes nos dois temas, **removido no lote 5** | guia de componentes correto nos 2 temas; contrastes conferidos; nenhuma tela existente quebrada | "Adiciona a fundação do novo design: tokens, layout e componentes" |
| **2 Entrada e início** | `login.jsp` (cadastro e login, painel de marca, mostrar senha, estados); `dashboard.jsp` (painel de reposição, barra de saúde, cartões com contagem animada, recentes, primeiros passos); `numeros.js` | login e dashboard no layout novo, só `<c:out>`, CSRF intacto, checklist 1 a 10, 17 e 25 | "Redesenha login, cadastro e dashboard" |
| **3 Catálogo** | `produtos.jsp` (filtros, chips, tabela com cartões no celular, ordenação por cabeçalho, paginação unificada); `busca-viva.js`; `produto-form.jsp` (validação inline, contador, zona de perigo, aviso de alterações); `produto-detalhe.jsp` (sem histórico); `categorias.jsp`; `categoria-form.jsp` | CRUD completo funcionando; exclusão com modal; checklist 1 a 14 | "Redesenha produtos e categorias" (pode ser 2 commits: produtos; categorias) |
| **4 Movimentação** | `estoque.jsp` (seletor com busca, segmentado, motivos, prévia, linha do tempo, destaque); `movimentacao.js` (substitui `estoque.js`); links **Repor** e **Movimentar** nas outras telas | registrar entrada, saída e ajuste; erro de saldo insuficiente aparece inline; envio duplo bloqueado; checklist 11, 16 e 24 | "Redesenha a tela de movimentações" |
| **5 Relatórios, erro e limpeza** | `estoque-baixo.jsp`, `unidades.jsp`, `valor-estoque.jsp` (impressão, participação no valor); `error.jsp`; remover `style.css`, includes antigos, `app.js`, `estoque.js`, `guia-componentes.html` e **todo** link de CDN; rodar a checklist completa da seção 6.3 | checklist 1 a 25 passando; `grep` sem CDN, sem `style=`, sem `<script>` inline | "Redesenha relatórios e erro e remove o front antigo" |
| **6** (depende do backend) | com **B2, B3, B4** prontos: filtros do histórico, bloco de histórico no detalhe, botões de exportar CSV; opcionais B5 a B9 | cada recurso testado ponta a ponta | um commit por recurso |

Antes de dar um lote por pronto: `./mvnw test` com o resultado real, conferência manual em 390 e 1440 px, nos dois temas, e passada do axe ou Lighthouse na tela alterada.

## 9. Pedidos ao `dev-backend`

Nenhum deles muda regra de negócio nem modelo; todos mantêm o isolamento por loja (RN-01).

| ID | Pedido | Prioridade | Para o lote |
|---|---|---|---|
| **B1** | Em `SecurityConfig`, incluir `/vendor/**` nas rotas públicas (junto de `/css/**`, `/js/**`). Alternativa: front usa `css/vendor` e `js/vendor` | obrigatório | 0 |
| **B2** | `GET /estoque` aceitar `produto` (id), `tipo` (`ENTRADA`/`SAIDA`/`AJUSTE`), `de` e `ate` (datas `aaaa-MM-dd`, `ate` inclusivo), sempre filtrando pela loja; manter paginação e devolver os mesmos atributos mais os valores recebidos para repor nos campos | alta | 6 |
| **B3** | `GET /produtos/{id}` expor `movimentacoesProduto` (últimas 10 movimentações do produto, da loja, com produto e categoria já carregados por causa de `open-in-view` desligado) | alta | 6 |
| **B4** | Exportar CSV: `GET /produtos/exportar.csv` (respeitando `busca`, `categoriaId`, `nivelEstoque`) e `GET /estoque/exportar.csv` (com os filtros de B2). UTF-8 com BOM, separador `;`, decimal com vírgula (abre certo no Excel brasileiro), `Content-Disposition: attachment`, escapar `=`, `+`, `-` e `@` no início de células de texto (injeção de fórmula) | alta | 6 |
| B5 | Flash `idDestaque` ao salvar produto ou categoria, para destacar a linha alterada | opcional | 6 |
| B6 | Atributo global `alertasEstoque` (em `GlobalModelAdvice`, a contagem de produtos Baixo + Sem estoque) para o badge no menu | opcional | 6 |
| B7 | `listarParaSelecao` de produtos com `@EntityGraph("categoria")`, para mostrar a categoria no seletor da movimentação | opcional | 6 |
| B8 | Tamanho padrão de `/produtos` em 25 (hoje 8) | opcional | 6 |
| B9 | Cabeçalho `Content-Security-Policy` (`default-src 'self'; img-src 'self' data:`) depois que o front estiver sem script e estilo inline (lote 5) | opcional, recomendado | 6 |

## 10. Ressalvas e riscos

- **Contraste:** todos os pares de cor da seção 1.2 foram calculados com a fórmula da WCAG, nos dois temas. Falta só a conferência visual com ferramenta (axe, Lighthouse) em cada lote; as bordas decorativas dos badges (`--sh-*-border`) não entram na conta.
- **Tag file (`layout.tag`):** não foi testado no Jasper do projeto; o plano B (par de includes) está descrito na 4.12.
- **Seletor de produto próprio:** é o componente mais complexo do front; o padrão ARIA é conhecido, mas exige teste real com NVDA. Se travar, a alternativa é Tom Select (MIT) servido local.
- **Tabela em cartões:** a restauração da semântica por `role` precisa de teste com leitor de tela (checklist 12); se falhar, usar duas marcações.
- **Tamanho da fonte Inter** (~50 KB) é estimativa do pacote Fontsource; registrar o valor real em `VERSOES.md`.
- **Busca ao digitar** troca HTML do servidor: depende de a JSP marcar `data-regiao-viva` e de a resposta ter o mesmo trecho; sem JS tudo continua funcionando por envio normal.
- **Motivos pré-definidos** só preenchem texto livre (v1). O motivo estruturado (enum com migration) continua sendo v2, como em `mercado-requisitos.md`.
- **Exclusão sem desfazer:** por desenho do backend (exclusão definitiva ou inativação). O modal precisa ser claro; não há toast de "Desfazer".
- **Não verificado em navegador:** esta fase é só documento. As razões de contraste são matemáticas; a validação visual real (axe, Lighthouse, NVDA) é parte de cada lote.
