# StockHub: identidade visual e experiência

Registro das decisões de design do front. **A direção vigente é a seção 12 (v3, "central de operações")**; as seções 1 a 11 ficam como histórico das versões anteriores.
Escrito em 26/10/2026, a partir de `docs/mercado-requisitos.md`, `docs/regras-negocio.md`, dos controllers e do front atual.

Legenda usada em todo o texto:

- **[F]** só front-end (JSP, CSS, JS), sem mudar o backend.
- **[B]** exige backend novo (item numerado na seção 9).
- **Descartado:** alternativa considerada e rejeitada, com o motivo.


> **Motor da apresentação (27/10/2026):** a página `/` agora é conduzida por GSAP + ScrollTrigger (`js/apresentacao-gsap.js`, bibliotecas em `vendor/gsap-3.15.0/`): escreve o `--sh-progresso` das cenas com um pequeno suavizamento e faz a revelação em lote. O visual é o mesmo (diferença medida menor que 0,4% dos pixels contra o kit de CSS). Sem GSAP ou com menos movimento, o kit de CSS continua valendo. As demais telas seguem no kit.
>
> **Atualização (v3):** a seção 12, "v3: central de operações", **substitui** a direção visual da v2 e da v1 (identidade, paleta, tipografia, componentes, movimento). As seções 1 a 11 ficam como histórico.
>
> **Atualização (v2, SUBSTITUÍDA pela v3):** a seção 11, "v2: com personalidade", substituía a identidade visual da v1; foi rejeitada ("muita cara de joguinho") e substituída pela seção 12.

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
| Inter (subset latino, variável) | **5.3.0** do pacote `@fontsource-variable/inter` (fixada no download do lote 1) | SIL OFL 1.1 | `vendor/inter/inter-latin-wght-normal.woff2`, `OFL.txt` | 48 KB (48256 bytes) | seção 1.3 |
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
| **Toast** (`.sh-toast`) | n/a | n/a | não se usa para erro (erro fica inline e persistente) | anunciado pelo contêiner `#sh-toasts` (`aria-live="polite"`), sem `role` em cada toast para o leitor não ler duas vezes; some em 7 s, pausa com mouse ou foco | n/a |
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
- **Como ficou nos lotes 1 e 2 (verificado no Jasper do Tomcat 11, o mesmo do projeto):** os componentes com atributos tipados ou com corpo viraram **tag files** em `WEB-INF/tags/`, usados com `<%@ taglib prefix="sh" tagdir="/WEB-INF/tags" %>`: `layout.tag`, `layoutPublico.tag` (login e, no lote 5, o erro), `cabecalhoPagina.tag` (corpo = botões de ação), `badgeEstoque.tag` (`nivel`), `medidorEstoque.tag` (`saldo`, `minimo`), `estadoVazio.tag`, `paginacao.tag`. Motivo: `jsp:include` só passa texto por `jsp:param` e não aceita marcação de ações; o tag file aceita número, enum e corpo. A trilha de navegação (`breadcrumb.jsp`) virou o fragmento `migalhas` do `layout.tag` (a página passa só os `<li>`). Os trechos fixos (cabeça, barra lateral, barra inferior, painel "Mais", modais, alertas, opções de tema) ficam em `WEB-INF/includes/sh/`, pasta nova para não colidir com os includes antigos até o lote 5.
- **Plano original (mantido para consulta):** `badge-estoque.jsp` (param `nivel`: `${produto.nivelEstoque}`), `medidor-estoque.jsp`, `estado-vazio.jsp`, `breadcrumb.jsp`, `cabecalho-pagina.jsp`. Layout base como **tag file** `WEB-INF/tags/layout.tag` (atributos `titulo`, `ativo`, fragmento `scripts`, `<jsp:doBody/>`), que concentra `head`, barra lateral, superior, inferior, toasts e scripts: acaba a repetição do `head` em cada JSP. Se tag file der problema no Jasper do Spring Boot, o plano B é um par `layout-inicio.jsp` / `layout-fim.jsp` incluído com `jsp:include`.

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
| 992 a 1199 px | barra lateral recolhida (68 px); métricas em 4 colunas (eram 3; com 4 cartões, 3 colunas deixariam um sozinho na linha de baixo) |
| 1200 px ou mais | barra lateral aberta; movimentação em 2 colunas (formulário 380 px fixo + histórico); métricas em 4 colunas |
| 1400 px ou mais | só aumenta a margem lateral; o conteúdo continua com máx. 1200 px |

Testar em 320, 360, 390, 768, 1024, 1440 px, com zoom de 200 % e **reflow a 320 px** sem rolagem horizontal (WCAG 1.4.10).

### 6.2 Regras

- **Alvo de toque:** 44 × 44 px no celular para qualquer controle (botões, chips, linhas de menu, itens do seletor, paginação); 40 px no desktop (mínimo absoluto da WCAG 2.2 é 24 px). Espaço de 8 px entre alvos vizinhos.
- **Foco visível:** `outline: 3px solid var(--sh-focus); outline-offset: 2px` em **tudo** que recebe foco, em `:focus-visible`; nunca `outline: none` sem substituto. Contraste do anel ≥ 3:1 contra os dois fundos (verificado: ≥ 7,3:1 no claro e ≥ 7,9:1 no escuro). **Exceção corrigida no lote 1:** dentro dos painéis escuros (barra lateral e painel de marca do login) o índigo do tema claro daria só 2,2:1, então ali o anel usa `--sh-side-marker` (7,5:1 no claro, 8,9:1 no escuro).
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
| **0** (backend) | **B1**: `/vendor/**` no `permitAll`; teste de que `/vendor/...` abre sem login | login carrega os arquivos de `vendor/` sem sessão | "Libera os arquivos de vendor sem login" |
| **1 Fundação** | baixar Bootstrap 5.3.8, Bootstrap Icons 1.11.3 e Inter para `vendor/` com `VERSOES.md` (versão, licença, URL, SHA-256); `tokens.css` (cores claro e escuro, tipografia, espaço, raios, sombras, movimento, mapeamento `--bs-*`, regra de `prefers-reduced-motion`); `layout.css`; `componentes.css` (botão, campo, badge, medidor, alerta, toast, tabela e cartões, esqueleto, estado vazio, paginação, linha do tempo); `impressao.css`; `tags/layout.tag`, barra lateral, superior, inferior e offcanvas "Mais"; `breadcrumb.jsp`, `cabecalho-pagina.jsp`, `badge-estoque.jsp`, `medidor-estoque.jsp`, `estado-vazio.jsp`, `paginacao.jsp` novo, `modal-exclusao`, modal de atalhos; JS: `tema-init`, `nucleo`, `toast`, `atalhos`, `formularios`, `confirmar-exclusao`; arquivo opcional `css/guia-componentes.html` (público, sem dados) para ver todos os componentes nos dois temas, **removido no lote 5** | guia de componentes correto nos 2 temas; contrastes conferidos; nenhuma tela existente quebrada | "Adiciona a fundação do novo design: tokens, layout e componentes" |
| **2 Entrada e início** | `login.jsp` (cadastro e login, painel de marca, mostrar senha, estados); `dashboard.jsp` (painel de reposição, barra de saúde, cartões com contagem animada, recentes, primeiros passos); `numeros.js` | login e dashboard no layout novo, só `<c:out>`, CSRF intacto, checklist 1 a 10, 17 e 25 | "Redesenha login, cadastro e dashboard" |
| **3 Catálogo** | `produtos.jsp` (filtros, chips, tabela com cartões no celular, ordenação por cabeçalho, paginação unificada); `busca-viva.js`; `produto-form.jsp` (validação inline, contador, zona de perigo, aviso de alterações); `produto-detalhe.jsp` (sem histórico); `categorias.jsp`; `categoria-form.jsp` | CRUD completo funcionando; exclusão com modal; checklist 1 a 14 | "Redesenha produtos e categorias" (pode ser 2 commits: produtos; categorias) |
| **4 Movimentação** | `estoque.jsp` (seletor com busca, segmentado, motivos, prévia, linha do tempo, destaque); `movimentacao.js` (substitui `estoque.js`); links **Repor** e **Movimentar** nas outras telas | registrar entrada, saída e ajuste; erro de saldo insuficiente aparece inline; envio duplo bloqueado; checklist 11, 16 e 24 | "Redesenha a tela de movimentações" |
| **5 Relatórios, erro e limpeza** | `estoque-baixo.jsp`, `unidades.jsp`, `valor-estoque.jsp` (impressão, participação no valor); `error.jsp`; remover `style.css`, includes antigos, `app.js`, `estoque.js`, `guia-componentes.html` e **todo** link de CDN; rodar a checklist completa da seção 6.3 | checklist 1 a 25 passando; `grep` sem CDN, sem `style=`, sem `<script>` inline | "Redesenha relatórios e erro e remove o front antigo" |
| **6** (depende do backend) | com **B2, B3, B4** prontos: filtros do histórico, bloco de histórico no detalhe, botões de exportar CSV; opcionais B5 a B9 | cada recurso testado ponta a ponta | um commit por recurso |

Antes de dar um lote por pronto: `./mvnw test` com o resultado real, conferência manual em 390 e 1440 px, nos dois temas, e passada do axe ou Lighthouse na tela alterada.

### 8.1 Como ficaram os lotes 1 e 2 (fase 3)

**Convivência com o legado (decisão):** as telas migradas (`login.jsp`, `dashboard.jsp`) usam **só** o layout novo e **nenhum** arquivo antigo; as não migradas continuam intactas, com `style.css`, os includes antigos (`sidebar`, `navbar`, `menu`, `alertas`, `scripts`, `paginacao`, `modal-exclusao`), `app.js`, `estoque.js` e o Bootstrap por CDN. Os dois mundos não se misturam na mesma página (o CSS novo nunca é carregado numa tela legada e vice-versa), então nada do legado muda de aparência nem de comportamento. Navegar entre uma tela nova e uma antiga troca de layout; isso some no lote 5.

**Arquivos novos:** `vendor/` (com `VERSOES.md`), `css/tokens.css`, `layout.css`, `componentes.css`, `impressao.css`, `guia-componentes.html`, `js/tema-init.js`, `nucleo.js`, `toast.js`, `atalhos.js`, `formularios.js`, `confirmar-exclusao.js`, `numeros.js`, `img/favicon.svg`, `WEB-INF/tags/*.tag`, `WEB-INF/includes/sh/*.jsp`.

**Detalhes que o plano deixava em aberto:**

- **Tema:** o `tokens.css` tem os valores do tema escuro em dois blocos idênticos, `[data-bs-theme="dark"]` (com JS) e `@media (prefers-color-scheme: dark)` com `:root:not([data-bs-theme])` (sem JS); um script conferiu que os dois são iguais. As variáveis `--bs-*` apontam para `--sh-*` (um único bloco), então tema do Bootstrap e tema do projeto nunca divergem.
- **Barra lateral:** `tema-init.js` já define `data-sh-lateral` (`aberta` ou `recolhida`) antes de pintar: preferência salva, ou recolhida abaixo de 1200 px quando não há preferência. Sem JS a barra fica aberta.
- **Barra de saúde:** além de `.sh-w-N` (degraus de 5, medidor e participação no valor), existe `.sh-seg-N` (de 1 em 1 %) para a barra do dashboard; a JSP calcula os percentuais de modo que a soma feche em 100.
- **Atalhos:** `m` fora de `/estoque` vai para `/estoque#registrar` (o lote 4 passa a focar o seletor de produto); `n` usa o primeiro elemento `[data-sh-atalho-novo="pagina"]` e, se não houver, o item "Produto" do menu Novo; nas telas públicas os atalhos ficam desligados. O teclado ABNT2 usa AltGr para `/` e `?`, por isso AltGr é aceito.
- **Mensagens do servidor:** `includes/sh/alertas.jsp` não mostra o alerta `erro` quando a tela também recebe `errosCampos` (o erro aparece junto ao campo e no resumo que a própria tela monta). A regra dos 72 bytes da senha chega como `errosCampos['senhaDentroDoLimite']` (nome da propriedade do `CadastroForm`) e a tela de login a mostra no campo de senha.
- **Paginação:** o seletor "Itens por página" fica para o lote 3, porque pertence ao formulário de filtros de `/produtos` (ele precisa reenviar os filtros).
- **Guia de componentes** (`/css/guia-componentes.html`): fica em `/css/**`, que o `SecurityConfig` já libera sem login; não tem dado nenhum. Remover no lote 5.

### 8.2 Como ficou o lote 5 (relatórios, erro e limpeza)

**Telas migradas** (todas no `layout.tag`, só `<c:out>`, sem `style=` nem `<script>` inline, sem CDN; rotas, parâmetros `pagina` e atributos de model inalterados): `estoque-baixo.jsp`, `unidades.jsp`, `valor-estoque.jsp` e `error.jsp`. Reaproveitam `sh:cabecalhoPagina`, `sh:paginacao`, `sh:badgeEstoque`, `sh:medidorEstoque` e `sh:estadoVazio`. Estilos próprios em `css/relatorios.css` (prefixos `sh-rel` e `sh-erro`) e `js/relatorios.js` (só liga o botão **Imprimir**).

- **Estoque baixo:** tabela que vira cartões no celular, com Saldo / mínimo + medidor, coluna "Para o mínimo" (`estoqueMinimo − quantidade`; mostra "Sem saldo" ou "No mínimo" quando não há falta), badge, **Repor** (`/estoque?movProduto=ID&movTipo=ENTRADA`) e Ver. Vazio positivo com tom de sucesso. Impressão: botão **Imprimir lista**; o `impressao.css` já esconde menus e botões e a coluna em branco **Comprar** (`.sh-so-impressao`) só aparece no papel.
- **Unidades e Valor:** alternância Unidades | Valor, cartão de métrica, tabela. Em Valor, a coluna **Participação no valor** calcula `produto.valorTotal / resumo.valorTotalEstoque` na JSP (divisão por zero protegida), com barra em degraus `.sh-w-N` e o percentual também em texto.
- **Exportar CSV:** reaproveita `/produtos/exportar.csv`. Unidades e Valor exportam todos os produtos em ordem de nome. Estoque baixo tem um menu com dois links (`nivelEstoque=semEstoque` e `nivelEstoque=baixo`), porque o filtro aceita um nível por vez e a lista junta os dois. Sem backend novo.
- **Erro:** usa `layoutPublico` (sem menu, sem login, sem ler `usuarioLogado`), com botão de tema, código grande e texto fixo por status (400, 403, 404, 405 e genérico). Não mostra o caminho pedido (`path`), a mensagem nem stack trace. Botão **Voltar ao início**; em 403 também **Entrar**. Para carregar o `relatorios.css` no `<head>`, o `layoutPublico.tag` ganhou o fragmento opcional `scripts` (igual ao do `layout.tag`).
- **Removidos (sem nenhuma referência, conferido por grep):** `WEB-INF/includes/{alertas,menu,modal-exclusao,navbar,paginacao,scripts,sidebar}.jsp`, `css/style.css`, `js/app.js`, `js/estoque.js`, `css/guia-componentes.html`. Era o único lugar com CDN: não resta nenhum link externo em `src/main/webapp` fora de `vendor/`.
- **Renomeado:** `loteProdutosColunaOrdenavel.tag` virou `colunaOrdenavel.tag` (uso em `produtos.jsp` atualizado). Os `css/lote-*.css` ficaram com o nome atual (renomear exige tocar em várias JSPs; sem ganho).
- **Outros:** BOM UTF-8 removido de `dashboard.jsp`, `estoque-baixo.jsp`, `unidades.jsp`, `valor-estoque.jsp` e do include antigo (apagado). `js/formularios.js` passou a documentar `form="sh-sem-formulario"` (seletor de produto da movimentação), sem mudar comportamento.

**Divergências em relação ao mapa de telas (7.9 a 7.12):** os dois botões do dashboard para Unidades e Valor continuam como cartões de métrica; o cartão de "Total de unidades" e o de "Valor total" nas telas de relatório não são links. Em `error.jsp` o botão de tema é o compacto do login. A página de erro não sabe se há sessão: em 403 o botão **Entrar** aparece sempre.

## 9. Pedidos ao backend

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
- **Tamanho da fonte Inter:** medido no download do lote 1, 48256 bytes (registrado em `vendor/VERSOES.md`, junto com os SHA-256 reais).
- **Busca ao digitar** troca HTML do servidor: depende de a JSP marcar `data-regiao-viva` e de a resposta ter o mesmo trecho; sem JS tudo continua funcionando por envio normal.
- **Motivos pré-definidos** só preenchem texto livre (v1). O motivo estruturado (enum com migration) continua sendo v2, como em `mercado-requisitos.md`.
- **Exclusão sem desfazer:** por desenho do backend (exclusão definitiva ou inativação). O modal precisa ser claro; não há toast de "Desfazer".
- **Não verificado em navegador:** esta fase é só documento. As razões de contraste são matemáticas; a validação visual real (axe, Lighthouse, NVDA) é parte de cada lote.

---

## 11. v2: com personalidade (SUBSTITUÍDA pela v3, seção 12; mantida como histórico)

Escrita em 26/10/2026, depois do retorno de uso da v1: *"ficou com muita cara de que não foi uma pessoa que fez"*, *"ao invés de dar reload na página seria legal ter uma animação"* e *"quero todo um design interativo"*. A v1 (seções 1 a 10) continua aqui como histórico. Esta seção **substitui** os trechos marcados abaixo; o que não aparece na lista continua valendo (estados de estoque, acessibilidade, regras de segurança no front, pontos de quebra, mapa de telas).

**O que a v2 substitui da v1**

| v1 | v2 |
|---|---|
| 1.1 "Calma: sem decoração que não informe, sem cartões que levitam" | Decoração com função: identidade, orientação e prazer de uso. Continua sem gradiente e sem sombra gratuita |
| 1.2 índigo `#4338CA`, neutros frios, preto azulado no escuro | Verde-musgo, papel creme, grafite quente (tabela abaixo). Os **nomes** `--sh-*` não mudaram |
| 1.3 só Inter | Fraunces para títulos e números grandes, Inter na interface |
| 1.5 ícones de biblioteca em todo botão | Ícones só onde ajudam a escanear (menu, estados de estoque, ações de lista). Botões de texto não têm ícone |
| 0.3 / 2.6 busca global em `/`, "sem paleta de comandos na v1" | Paleta de comandos (Ctrl+K), e `/` passa a abrir a paleta |
| 5 (movimento) "nada de animação gratuita" | Sistema de movimento com tokens, transição entre páginas, entrada escalonada (seção 11.5) |
| 4.5 cartões de métrica iguais, em 4 caixas | Números grandes sem caixa, separados por pontilhado |

### 11.1 Conceito: "caderneta de balcão"

O StockHub é a caderneta de quem toca uma loja pequena, só que viva: ela avisa, conta, reage. Tudo na tela tem que parecer feito por uma pessoa que conhece balcão: papel, tinta, etiqueta de gôndola, caixa de papelão.

- **Papel e tinta:** neutros quentes. Claro é papel creme com textura quase invisível; escuro é grafite quente (sem preto azulado).
- **Capa:** a barra lateral é a capa verde-musgo da caderneta, com costura pontilhada, e o marcador de página (terracota) mostra onde você está.
- **Etiqueta de gôndola:** os selos de estado (Normal, Baixo, Sem estoque) têm forma de etiqueta de preço, com furinho.
- **Assimetria controlada:** cantos em "folha" (o de baixo à esquerda é mais seco: `--sh-radius-folha`), o cartão da saúde começa 28 px abaixo da lista, mancha irregular atrás do mascote, sublinhado à mão.
- **Voz:** frases curtas, de gente. "Nada acabando por aqui.", "Boa tarde, Mercearia Teste", "Ai, ai... falta coisa.", datas relativas ("há 2 h").

**Alternativas descartadas:** (a) "painel de controle" frio com gráficos densos: é o visual que o usuário apontou como genérico. (b) Neo-brutalismo (bordas grossas, sombras duras, cores berrantes): tem personalidade, mas cansa em uso diário, pesa em tabelas e briga com os estados vermelho/âmbar. (c) Ilustração por toda parte: o mascote só aparece onde ele diz algo (saudação, vazio, erro, entrada).

### 11.2 Paleta (valores no `css/tokens.css`)

Marca: **verde-musgo** `#3E5A2F` (botão, link, foco no claro). **Terracota** `#A34622` (claro) / `#E58B63` (escuro) é só **decoração e destaque** (rabisco, marcador de página, bochecha do mascote, inicial do usuário). **Nunca** indica estado de estoque: estoque continua verde (normal), âmbar (baixo), vermelho (sem estoque), sempre com ícone + palavra + cor.

Risco assumido: o verde-musgo da marca é parente do verde de "Normal". Mitigação: o musgo é amarelado e escuro (botões, links), o verde de estado é azulado e só aparece dentro de selo, medidor e anel, sempre com ícone e texto.

| Papel (claro) | Valor | Grafite (escuro) | Valor |
|---|---|---|---|
| fundo | `#F3EDDF` | fundo | `#1A1612` |
| superfície | `#FBF8F0` | superfície | `#241F19` |
| superfície 2 | `#EAE2D0` | superfície 2 | `#2E2820` |
| texto | `#261F16` | texto | `#F0E8D8` |
| acento (botão) | `#3E5A2F` | acento (botão) | `#4F7040` |
| acento como texto | `#3E5A2F` | acento como texto | `#A9C98F` |
| barra lateral | `#26301C` | barra lateral | `#120F0B` |

**Contrastes calculados (WCAG 2.x, fórmula de luminância relativa; mínimo 4,5:1 para texto e 3:1 para elementos gráficos):**

| Par | Claro | Escuro |
|---|---|---|
| Texto sobre fundo | 13,9:1 | 14,8:1 |
| Texto sobre superfície | 15,3:1 | 13,4:1 |
| Texto secundário sobre superfície | 8,3:1 | 8,5:1 |
| Texto discreto sobre superfície 2 | 5,2:1 | 5,4:1 |
| Botão primário (texto branco) | 7,8:1 | 5,6:1 |
| Link / acento como texto sobre superfície | 7,3:1 | 8,9:1 |
| Terracota como texto sobre superfície | 5,7:1 | 6,4:1 |
| Borda de campo (elemento gráfico, mín. 3) | 4,0:1 | 4,4:1 |
| Barra lateral: texto / texto discreto | 10,0 / 6,1:1 | 11,2 / 6,5:1 |
| Selo Normal | 5,6:1 | 8,3:1 |
| Selo Baixo | 6,9:1 | 8,5:1 |
| Selo Sem estoque | 5,6:1 | 8,3:1 |
| Selo Saída (azul) | 6,0:1 | 8,0:1 |
| Botão de perigo (texto branco) | 5,9:1 | 5,2:1 |
| Medidor normal / baixo / sem sobre o trilho (mín. 3) | 3,1 / 3,4 / 3,6:1 | 5,2 / 5,6 / 3,7:1 |

O script que gera os três blocos de cor do `tokens.css` a partir de uma só tabela e calcula todos os pares foi usado na verificação; o claro "baixo" do medidor foi escurecido de `#B25E09` para `#A25406` porque o par com o trilho dava 2,8:1. Foram conferidos também com o axe (sem violações) nas telas renderizadas.

### 11.3 Tipografia

**Fraunces** (variável, eixos de peso e `SOFT`, subset latino, 62 KB, OFL; registro e integridade SHA-512 em `vendor/VERSOES.md`) para títulos de página e de seção, saudação e números grandes. Inter continua na interface, em tabelas e formulários, com números tabulares.

Por quê: serifa suave e levemente "cheia", com `SOFT` em 100 as pontas ficam arredondadas, como letra de etiqueta de balcão; contrasta bem com Inter e funciona em 48 px e em 18 px. **Descartadas:** Bricolage Grotesque (boa, mas é sans e parece com muita coisa recente; a serifa dá o ar de caderneta), Instrument Serif (elegante demais, só tem peso regular, ruim em números grandes).

Escala nova: `--sh-text-hero` (saudação), `--sh-text-numero` (números do início). A classe `.sh-display` aplica a fonte de display.

### 11.4 Mascote e ilustrações

A **caixinha** é um SVG inline desenhado à mão, sem biblioteca (`WEB-INF/tags/mascote.tag`): caixa de papelão aberta, abas que mudam de posição, etiqueta colada, rosto simples. Traço levemente irregular de propósito (curvas Bézier com pequenos desvios, `stroke-linejoin: round`).

| Estado | Quando | Expressão |
|---|---|---|
| contente | tudo acima do mínimo; login; vazio positivo | abas para o alto, sorriso, acena e solta confete ao clicar |
| atenta | só há estoque baixo (nada acabou) | abas de orelha, sobrancelha levantada, olha para cima |
| preocupada | há produto sem estoque; erro de login; gráfico com falha | abas caídas, sobrancelhas aflitas, gota de suor |
| dormindo | loja nova sem produto; "nada aqui" | olhos fechados, "zzz" subindo |

Troca de estado sem recarregar: `data-estado` no `<svg>`. Vida: respira, pisca, gota escorre; pupilas seguem o cursor (`data-sh-olhar`); no login, fecha os olhos na senha e espia quando ela está visível. Tudo isso é CSS/JS pequeno (`css/identidade.css`, `js/humano.js`, `js/entrar.js`) e desliga com `prefers-reduced-motion`.

Ilustrações de estado vazio (`WEB-INF/tags/estadoVazio.tag`, atributo `ilustracao`): `caixa` (mascote dormindo), `lupa` (mascote atenta com lupa, "nada encontrado"), `etiquetas` (etiquetas balançando num barbante), `ok` (mascote contente com visto riscado). O 404 usa o mesmo tag do mascote (`<sh:mascote estado="preocupada">`); a migração da tela de erro fica para o lote seguinte.

Outros detalhes de artesanato: sublinhado de rabisco (`.sh-rabisco`, máscara SVG que "desenha" ao entrar), textura de papel por SVG de ruído de 180 px em data URI (cerca de 600 bytes, sem imagem), pauta de caderno na capa do login, costura pontilhada na lateral, link com sublinhado ondulado terracota.

### 11.5 Movimento e navegação

**Tokens** (`tokens.css`): durações `--sh-dur-instante` 90 ms, `-fast` 140, `--sh-dur` 220, `-slow` 360, `-lenta` 640; easings `--sh-ease-out`, `--sh-ease-in`, `--sh-ease`, `--sh-ease-mola` (leve overshoot); `--sh-stagger` 45 ms. Só `transform`, `opacity` e traço de SVG se animam (60 fps, sem layout shift). Todo movimento some com `prefers-reduced-motion`.

**Transição entre páginas, sem sensação de recarga.** `@view-transition { navigation: auto }` (View Transitions entre documentos) em `css/layout.css`:

- peças fixas com nome (`lateral`, `topo`, `inferior`): a foto nova aparece na hora por cima da antiga, **sem piscar**;
- `conteudo` (o `<main>`): fade e deslize curto; **direção**: entrando num nível mais fundo (lista para detalhe) vem da direita, subindo de nível ou botão voltar vem da esquerda, entre telas do menu só fade. A direção sai de `js/navegacao-init.js` (script síncrono no `<head>`, tipos de transição `para-frente`, `para-tras`, `mesmo-nivel`);
- o marcador de página do menu tem nome (`marcador`) e **desliza** de um item ao outro;
- `<link rel="expect" blocking="render">` segura a primeira pintura até o `<main>` existir.

**Suporte e plano B.** Chrome e Edge 126+ e Safari recente fazem a transição; Firefox ainda não. O `<html>` ganha `data-sh-vt` quando há suporte. Sem ele, entra o plano B: o conteúdo faz fade ao sair (130 ms, `js/navegacao.js`, nunca em Ctrl/Cmd+clique, nova aba, download ou hash) e sobe ao entrar (`.sh-main`). Com `prefers-reduced-motion`, nada anima.

**Pré-carga.** `js/navegacao.js` faz `<link rel="prefetch">` ao passar o mouse (65 ms de intenção), tocar ou focar links internos GET. Nunca: `/logout`, `/login`, `/cadastro`, `/excluir`, `/api`, `.csv`, `exportar`, `?acao=`, `data-sh-sem-prefetch`, outra origem, "economia de dados"; no máximo 8 por visita.

**Limites, com honestidade.** A próxima página **continua sendo carregada do servidor**: o ganho é de continuidade visual, não de velocidade. A pré-carga só reduz a espera se o servidor deixar a resposta ser guardada; o Spring Security manda `Cache-Control: no-store` em páginas autenticadas por padrão, o que a anula (pedido ao backend, seção 9 abaixo). Uma página muito alta pode mostrar um corte na foto antiga. Efeitos de entrada que rodam durante a transição são curtos de propósito para não somar movimento.

**Entrada e feedback.** `.sh-escalona` (filhos entram em sequência com atraso de 45 ms, até 8), linhas de tabela entram em sequência, contadores animados (`data-contar`, reexecutáveis por `SH.animarNumeros(raiz)`), barras que preenchem (`.sh-enche`, medidor, anel, gráfico), botão primário que afunda ao apertar, botão secundário com marca-texto passando no hover, toasts em forma de bilhete com o visto "estalando", esqueletos de carregamento (`.sh-esqueleto`).

### 11.6 Paleta de comandos

`Ctrl+K` / `Cmd+K` em qualquer tela, o botão da barra superior (parece um campo de busca) e a tecla `/`. `<dialog>` modal nativo (camada superior, fundo inerte, Esc), foco preso entre o campo e "Esc", foco devolvido a quem abriu. ARIA: campo `role="combobox"` com `aria-controls`, `aria-expanded`, `aria-activedescendant`; lista `role="listbox"` com grupos (`role="group"` rotulado); resultado anunciado em região `aria-live="polite"`.

Conteúdo: **Ações** (Registrar entrada, Registrar saída, Novo produto, Nova categoria, Alternar tema, Atalhos de teclado), **Ir para** (as sete telas) e **Produtos** via `GET /api/busca?q=termo&limite=8` (contrato: `[{id, nome, categoria, saldo, minimo, nivel, url}]`). Busca sem acento, com destaque do trecho. Debounce de 180 ms, cancelamento da busca anterior, cache por termo, tempo máximo de 6 s. **Degrada:** se o endpoint não existir, der erro ou responder algo que não seja JSON, a paleta mostra um aviso discreto no lugar dos produtos e mantém ações, telas e a saída "Ver “termo” na lista de produtos" (`/produtos?busca=`). Dados entram só por `textContent`; `url` só é aceita se for caminho da própria aplicação.

### 11.7 Início (dashboard)

Ordem: saudação com mascote, "o que fazer hoje", saúde do estoque, gráfico de 14 dias, números, recentes.

- **Saudação por horário** ("Bom dia/Boa tarde/Boa noite", feita no navegador por `js/humano.js`; o servidor escreve "Olá" como plano B) e data de hoje por extenso. O mascote reage ao estado do estoque (tabela 11.4) e ao clique.
- **O que fazer hoje:** os itens a repor, do mais urgente ao menos, cada um com **reposição rápida**: campo de quantidade (sugestão = leva o saldo a 2x o mínimo) e botão Repor, que faz o mesmo `POST /estoque/movimentar` (ENTRADA) da tela de movimentações, com token CSRF e observação "Reposição rápida pelo início". Sem nada a repor: mascote contente, texto "Nada para repor." e uma **celebração discreta** (confete de papel, uma vez por aba, só com movimento permitido).
- **Saúde do estoque:** anel SVG (três fatias com vão) com centro que muda ao passar o mouse ou focar uma linha da legenda; cada linha é link para a lista filtrada (`nivelEstoque=ok|baixo|semEstoque`). O anel é decorativo (`aria-hidden`); a informação está em texto.
- **Gráfico de 14 dias** (`js/inicio.js`, SVG sem biblioteca, dados de `GET /api/dashboard/movimentos?dias=14`): entradas sobem da linha do meio e saídas descem (a posição diz o tipo, a cor reforça); ajustes viram um ponto na linha. Estados: esqueleto, erro com "Tentar de novo" (mascote preocupada), vazio (mascote dormindo e botão para registrar entrada), pronto. Tooltip por mouse, toque e teclado (um tab stop, setas, Home, End, Esc), `aria-label` completo em cada dia ("segunda, 26 de outubro: 12 entradas, 5 saídas, 0 ajustes"), resumo em texto e **tabela equivalente** em `<details>`. Redesenha ao mudar a largura, sem repetir a animação.
- **Números** sem caixa, em Fraunces, separados por pontilhado; cada número é link. **Chegou por último:** lista simples.

### 11.8 Arquivos da v2

CSS: `tokens.css` (reescrito), `layout.css` (reescrito), `identidade.css` (novo: botões, selos, cartões, mascote, rabisco, esqueleto, toast, vazio, escalonamento), `paleta.css`, `inicio.css`, `entrar.css` (novos). JS: `navegacao-init.js`, `navegacao.js`, `paleta.js`, `humano.js`, `inicio.js`, `entrar.js` (novos); `numeros.js` e `atalhos.js` (ajustados). Tags: `mascote.tag` (novo), `layout.tag`, `estadoVazio.tag`. Includes: `head.jsp`, `lateral.jsp`, `inferior.jsp`, `paleta.jsp` (novo), `modal-atalhos.jsp`. Páginas: `dashboard.jsp`, `login.jsp`. Fonte: `vendor/fraunces/`.

### 11.9 Pedidos ao backend (v2)

1. `GET /api/busca` e `GET /api/dashboard/movimentos` com o contrato acima, autenticados e filtrados pela loja do usuário. **Confirmar o significado de `entradas` e `saidas`:** a tela as chama só de "entradas" e "saídas" (sem "unidades"); se forem somas de quantidade, dá para trocar o texto por "unidades".
2. Cabeçalho de cache nas páginas GET autenticadas (por exemplo `Cache-Control: private, max-age=10`) para a pré-carga ter efeito; hoje o padrão do Spring Security (`no-store`) a anula.
3. Data de cadastro (`criadoEm`) em `Produto` para "Chegou por último" mostrar "há 2 h" (`<time data-relativo datetime="...">` já é suportado por `js/humano.js`).

### 11.10 Verificação desta entrega

Renderizado em mini-Tomcat avulso (porta alta) com Edge headless: início em computador e celular, claro e escuro; login e cadastro; paleta aberta com busca; tooltip do gráfico; estados vazio, erro e esqueleto. axe sem violações em todas essas telas. Telas da v1 (produtos, detalhe, formulário, categorias) renderizadas com a nova fundação, sem erro de JS e sem violações no axe. Transição entre documentos confirmada (`pagereveal` com `viewTransition` e tipos `mesmo-nivel` e `para-tras`); com `prefers-reduced-motion` ela desliga. **Não verificado:** Firefox e Safari (plano B só revisado no código), leitor de tela de verdade, e a pré-carga contra o servidor real.

---

## 12. v3: central de operações

Escrita em 26/10/2026, depois da rejeição da v2: *"muita cara de joguinho"*. O pedido: *"uma aparência mais profissional, mas algo diferente e moderno, com animações sérias mas agradáveis aos olhos; algo revolucionário mas com aparência de coisa séria"*, com referência a *"algo tipo Apple, ou aqueles sites que tu rola para baixo e vai mudando"*. A **v2 ("caderneta de balcão": mascote, rabiscos, textura de papel, Fraunces, voz brincalhona) está SUBSTITUÍDA por esta seção** e fica nas seções 1 a 11 só como histórico. O que a v3 não menciona continua valendo (estados de estoque com ícone + texto + cor, acessibilidade, regras de segurança no front, pontos de quebra, estrutura das telas).

**O que a v3 substitui**

| v2 | v3 |
|---|---|
| "Caderneta de balcão", voz brincalhona, mascote, rabiscos, confete, balão de fala | Voz neutra, direta e profissional. Nada de personagem. A marca é um quadrado de acento com três barras de nível (`<sh:marca>`) |
| Papel creme, verde-musgo, terracota, textura de papel, pauta | Branco e cinza suave no claro; grafite em camadas no escuro; **um** acento cobalto sóbrio. Sem textura, sem gradiente decorativo |
| Fraunces nos títulos | Só Inter, com hierarquia forte (títulos pesados, tracking apertado) e **Geist Mono** para valores, códigos e atalhos |
| Cantos "folha" (um canto seco), botão "tecla", marca-texto, selo de gôndola | Raios de 6 a 10 px, bordas hairline, selos retangulares de 6 px |
| Toast "bilhete", entrada com mola e rotação | Toast sério com ação **Desfazer**, entrada com fade + 8 px, sem overshoot |
| Barra lateral verde "capa" com costura | Barra lateral na cor do fundo, recolhível, item atual como cartão com filete de acento |
| Paleta de comandos como botão de busca discreto | **Barra de comando** central na barra superior ("Buscar ou executar um comando…  Ctrl K") |
| Movimento 90/140/220/360/640 ms com "mola" | Durações 120/180/240/400 ms, só desaceleração |

### 12.1 Conceito: central de operações com acabamento de produto

- **Sério e preciso.** A interface é uma central de operações: o que importa é o saldo, o que falta e o que mudou. Cada tela tem uma tarefa; o estado do estoque nunca depende só da cor.
- **Acabamento de produto Apple.** Muito espaço, tipografia grande e pesada nos títulos, cinza no texto secundário, bordas de um pixel, sombras só no que flutua, vidro fosco só onde agrega (barra superior, barra inferior do celular, barra de ações do formulário e a barra de comando) e sempre com cor sólida de reserva.
- **Rolagem narrativa onde cabe.** A apresentação pública e o início usam o **kit de rolagem** (12.5): revelações, cenas fixas com progresso, números e linhas que se desenham conforme a página rola. As telas de trabalho (listas, formulários, movimentação) **não** usam cenas: aí vale densidade e previsibilidade.
- **Movimento sério.** Só desaceleração, sem overshoot, sem bounce; entradas com fade + deslocamento de 4 a 8 px; tudo em `transform` e `opacity`.

### 12.2 Paleta e contrastes (valores em `css/tokens.css`)

Os nomes `--sh-*` não mudaram; só os valores. Os três blocos de cor do `tokens.css` (claro, escuro, escuro do sistema sem JavaScript) são gerados da mesma tabela. Tokens novos ficam à parte (12.7).

**O acento: cobalto sóbrio (`#1D55C4` no claro; `#2D68D6` como botão e `#8DB3FF` como texto no escuro).** Por quê: azul é a cor da confiança institucional e de produto (links e controles do sistema que o usuário citou como referência), é fria e contida, e se distingue dos estados de estoque (verde, âmbar, vermelho). Matiz em torno de 219°: azul puro, **não** índigo nem roxo, e baixa saturação no escuro (nada de azul-neon: o escuro usa `#8DB3FF`, um azul lavado). O "Saída" da movimentação, que era azul, passou a **ciano-petróleo** (`--sh-info-*`) para não se confundir com a cor de ação. Rejeitados: musgo e terracota (v2), índigo (v1), preto sem acento (sem sinal de ação clara), teal (confunde com o verde de "Normal").

| Papel | Claro | Escuro |
|---|---|---|
| `--sh-bg` (fundo da página e da barra lateral) | `#F5F5F7` | `#101113` |
| `--sh-surface` (cartões, campos, tabelas) | `#FFFFFF` | `#17181B` |
| `--sh-surface-2` (cabeçalho de grupo, hover, trilho) | `#EEEFF2` | `#202125` |
| `--sh-surface-3` (derivado, 18% mais perto da borda) | derivado | derivado |
| `--sh-border` (hairline decorativa) | `#E2E4E8` | `#2B2D32` |
| `--sh-border-strong` (borda de campo, 3:1) | `#80868F` | `#6F747D` |
| `--sh-text` / `-muted` / `-subtle` | `#1D1D1F` / `#4D5158` / `#62666E` | `#ECEDEF` / `#B0B4BB` / `#8C9199` |
| `--sh-accent` / `-hover` / `-text` / `-soft` / `--sh-focus` | `#1D55C4` / `#173F94` / `#1D55C4` / `#E8EFFC` / `#1D55C4` | `#2D68D6` / `#2559BD` / `#8DB3FF` / `#16233E` / `#8DB3FF` |
| Normal (texto / fundo / borda) | `#146B3C` / `#E5F4EA` / `#A9D6B8` | `#6FD39B` / `#10271A` / `#2A6B47` |
| Baixo | `#7A4100` / `#FCEFCF` / `#E8C679` | `#F0BE62` / `#2D2209` / `#7A5A1C` |
| Sem estoque e erro | `#B3261E` / `#FCE9E7` / `#EDA7A1` | `#FF9E94` / `#341513` / `#85332C` |
| Saída e informação | `#0B6478` / `#E1F2F6` / `#9CCBD6` | `#7CCDE4` / `#0E2830` / `#2A6575` |
| Medidor Normal / Baixo / Sem | `#1F8A4F` / `#B45F08` / `#C42B1F` (trilho `#DCDFE4`) | `#4CC38A` / `#E8A93A` / `#F0645A` (trilho `#2B2D32`) |

O escuro é **grafite em camadas** (fundo, superfície, superfície elevada em passos de luminosidade, sem azul no cinza). Estados de estoque: sempre **ícone + texto + cor** (`<sh:badgeEstoque>`: círculo com visto, triângulo, octógono).

Contrastes calculados (WCAG 2.x, luminância relativa; script de apoio fora do repositório; todos passam):


**Tema claro** (mínimo 4,5:1 para texto; 3:1 para bordas de campo, foco e medidores)

| Par | Cores | Razão | Mín. | Resultado |
|---|---|---|---|---|
| texto / bg | `#1D1D1F` sobre `#F5F5F7` | 15.46:1 | 4.5 | passa |
| texto / surface | `#1D1D1F` sobre `#FFFFFF` | 16.83:1 | 4.5 | passa |
| texto / surface2 | `#1D1D1F` sobre `#EEEFF2` | 14.64:1 | 4.5 | passa |
| texto secundário / bg | `#4D5158` sobre `#F5F5F7` | 7.32:1 | 4.5 | passa |
| texto secundário / surface | `#4D5158` sobre `#FFFFFF` | 7.97:1 | 4.5 | passa |
| texto secundário / surface2 | `#4D5158` sobre `#EEEFF2` | 6.94:1 | 4.5 | passa |
| texto terciário e placeholder / bg | `#62666E` sobre `#F5F5F7` | 5.29:1 | 4.5 | passa |
| texto terciário e placeholder / surface | `#62666E` sobre `#FFFFFF` | 5.76:1 | 4.5 | passa |
| texto terciário e placeholder / surface2 | `#62666E` sobre `#EEEFF2` | 5.01:1 | 4.5 | passa |
| borda de campo / surface (3:1) | `#80868F` sobre `#FFFFFF` | 3.67:1 | 3 | passa |
| borda de campo / bg (3:1) | `#80868F` sobre `#F5F5F7` | 3.37:1 | 3 | passa |
| texto branco / botão primário | `#FFFFFF` sobre `#1D55C4` | 6.67:1 | 4.5 | passa |
| texto branco / primário hover | `#FFFFFF` sobre `#173F94` | 9.65:1 | 4.5 | passa |
| link de acento / surface | `#1D55C4` sobre `#FFFFFF` | 6.67:1 | 4.5 | passa |
| link de acento / bg | `#1D55C4` sobre `#F5F5F7` | 6.13:1 | 4.5 | passa |
| link de acento / accent-soft | `#1D55C4` sobre `#E8EFFC` | 5.77:1 | 4.5 | passa |
| anel de foco / surface (3:1) | `#1D55C4` sobre `#FFFFFF` | 6.67:1 | 3 | passa |
| anel de foco / bg (3:1) | `#1D55C4` sobre `#F5F5F7` | 6.13:1 | 3 | passa |
| Normal/sucesso: texto / fundo tonal | `#146B3C` sobre `#E5F4EA` | 5.77:1 | 4.5 | passa |
| Normal/sucesso: texto / surface | `#146B3C` sobre `#FFFFFF` | 6.57:1 | 4.5 | passa |
| Baixo/aviso: texto / fundo tonal | `#7A4100` sobre `#FCEFCF` | 7.12:1 | 4.5 | passa |
| Baixo/aviso: texto / surface | `#7A4100` sobre `#FFFFFF` | 8.13:1 | 4.5 | passa |
| Sem estoque/erro: texto / fundo tonal | `#B3261E` sobre `#FCE9E7` | 5.59:1 | 4.5 | passa |
| Sem estoque/erro: texto / surface | `#B3261E` sobre `#FFFFFF` | 6.54:1 | 4.5 | passa |
| Saída/info: texto / fundo tonal | `#0B6478` sobre `#E1F2F6` | 5.87:1 | 4.5 | passa |
| Saída/info: texto / surface | `#0B6478` sobre `#FFFFFF` | 6.76:1 | 4.5 | passa |
| branco / sucesso sólido | `#FFFFFF` sobre `#15803D` | 5.02:1 | 4.5 | passa |
| branco / perigo sólido | `#FFFFFF` sobre `#C0261B` | 5.94:1 | 4.5 | passa |
| medidor Normal / trilho (3:1) | `#1F8A4F` sobre `#DCDFE4` | 3.27:1 | 3 | passa |
| medidor Baixo / trilho (3:1) | `#B45F08` sobre `#DCDFE4` | 3.43:1 | 3 | passa |
| medidor Sem estoque / trilho (3:1) | `#C42B1F` sobre `#DCDFE4` | 4.23:1 | 3 | passa |

**Tema escuro** (mínimo 4,5:1 para texto; 3:1 para bordas de campo, foco e medidores)

| Par | Cores | Razão | Mín. | Resultado |
|---|---|---|---|---|
| texto / bg | `#ECEDEF` sobre `#101113` | 16.13:1 | 4.5 | passa |
| texto / surface | `#ECEDEF` sobre `#17181B` | 15.15:1 | 4.5 | passa |
| texto / surface2 | `#ECEDEF` sobre `#202125` | 13.73:1 | 4.5 | passa |
| texto secundário / bg | `#B0B4BB` sobre `#101113` | 9.08:1 | 4.5 | passa |
| texto secundário / surface | `#B0B4BB` sobre `#17181B` | 8.53:1 | 4.5 | passa |
| texto secundário / surface2 | `#B0B4BB` sobre `#202125` | 7.73:1 | 4.5 | passa |
| texto terciário e placeholder / bg | `#8C9199` sobre `#101113` | 5.96:1 | 4.5 | passa |
| texto terciário e placeholder / surface | `#8C9199` sobre `#17181B` | 5.60:1 | 4.5 | passa |
| texto terciário e placeholder / surface2 | `#8C9199` sobre `#202125` | 5.07:1 | 4.5 | passa |
| borda de campo / surface (3:1) | `#6F747D` sobre `#17181B` | 3.78:1 | 3 | passa |
| borda de campo / bg (3:1) | `#6F747D` sobre `#101113` | 4.02:1 | 3 | passa |
| texto branco / botão primário | `#FFFFFF` sobre `#2D68D6` | 5.17:1 | 4.5 | passa |
| texto branco / primário hover | `#FFFFFF` sobre `#2559BD` | 6.48:1 | 4.5 | passa |
| link de acento / surface | `#8DB3FF` sobre `#17181B` | 8.47:1 | 4.5 | passa |
| link de acento / bg | `#8DB3FF` sobre `#101113` | 9.02:1 | 4.5 | passa |
| link de acento / accent-soft | `#8DB3FF` sobre `#16233E` | 7.46:1 | 4.5 | passa |
| anel de foco / surface (3:1) | `#8DB3FF` sobre `#17181B` | 8.47:1 | 3 | passa |
| anel de foco / bg (3:1) | `#8DB3FF` sobre `#101113` | 9.02:1 | 3 | passa |
| Normal/sucesso: texto / fundo tonal | `#6FD39B` sobre `#10271A` | 8.64:1 | 4.5 | passa |
| Normal/sucesso: texto / surface | `#6FD39B` sobre `#17181B` | 9.69:1 | 4.5 | passa |
| Baixo/aviso: texto / fundo tonal | `#F0BE62` sobre `#2D2209` | 9.11:1 | 4.5 | passa |
| Baixo/aviso: texto / surface | `#F0BE62` sobre `#17181B` | 10.35:1 | 4.5 | passa |
| Sem estoque/erro: texto / fundo tonal | `#FF9E94` sobre `#341513` | 8.37:1 | 4.5 | passa |
| Sem estoque/erro: texto / surface | `#FF9E94` sobre `#17181B` | 8.94:1 | 4.5 | passa |
| Saída/info: texto / fundo tonal | `#7CCDE4` sobre `#0E2830` | 8.58:1 | 4.5 | passa |
| Saída/info: texto / surface | `#7CCDE4` sobre `#17181B` | 9.90:1 | 4.5 | passa |
| branco / sucesso sólido | `#FFFFFF` sobre `#1A7F45` | 5.04:1 | 4.5 | passa |
| branco / perigo sólido | `#FFFFFF` sobre `#CC3226` | 5.18:1 | 4.5 | passa |
| medidor Normal / trilho (3:1) | `#4CC38A` sobre `#2B2D32` | 6.22:1 | 3 | passa |
| medidor Baixo / trilho (3:1) | `#E8A93A` sobre `#2B2D32` | 6.67:1 | 3 | passa |
| medidor Sem estoque / trilho (3:1) | `#F0645A` sobre `#2B2D32` | 4.38:1 | 3 | passa |

### 12.3 Tipografia

- **Inter** variável local (já existia) para a interface e os títulos; **Geist Mono** variável local (`vendor/geist-mono/`, OFL 1.1, registrada em `vendor/VERSOES.md`) **só** para valores, códigos, SKUs e atalhos (`.sh-mono`, `code.sh-codigo`, teclas). Fraunces foi removida.
- **Hierarquia forte:** títulos grandes e pesados (650 a 750) com tracking apertado (`-0,022em` a `-0,045em`), texto secundário em cinza (`--sh-text-muted`, `--sh-text-subtle`), corpo 15 px, sobretítulo em caixa-alta com tracking aberto (`.sh-eyebrow`).
- **Números tabulares** (`font-variant-numeric: tabular-nums`) em tabelas e métricas (`.sh-num`, `.sh-col-num`); a monoespaçada usa zero cortado.
- **Escala fluida** (`clamp`), tokens `--sh-fs-*`:

| Token | Valor | Uso |
|---|---|---|
| `--sh-fs-pagina` | `clamp(1.625rem, 1.35rem + 0.9vw, 2.25rem)` | título de página (`.sh-page-header__titulo`, `.sh-titulo-pagina`) |
| `--sh-fs-secao` | `clamp(1.25rem, 1.1rem + 0.5vw, 1.5rem)` | título de seção (`.sh-titulo-secao-v3`) |
| `--sh-fs-display` | `clamp(2.25rem, 1.4rem + 3.4vw, 4rem)` | título de bloco (`.sh-display`) |
| `--sh-fs-hero` | `clamp(2.75rem, 1.2rem + 6.2vw, 6rem)` | chamada da apresentação (`.sh-hero-texto`) |
| `--sh-fs-hero-xl` | `clamp(3.25rem, 0.8rem + 10vw, 9rem)` | número ou palavra gigante (`.sh-hero-xl`) |
| `--sh-fs-numero` / `-numero-xl` | `clamp(2rem, ..., 3.25rem)` / `clamp(2.75rem, ..., 6.5rem)` | números de destaque |
| `--sh-fs-lead` | `clamp(1.0625rem, ..., 1.375rem)` | texto de apoio (`.sh-lead`) |
| `--sh-track-hero / display / titulo / corpo / rotulo` | `-0.045em / -0.035em / -0.022em / -0.006em / 0.07em` | espaçamento entre letras |

Os nomes antigos (`--sh-text-hero`, `--sh-text-numero`, `--sh-font-display`) seguem existindo, apontando para valores neutros, para o início atual não quebrar.

### 12.4 Acabamento, estrutura e movimento

- **Bordas e raios:** hairline de 1 px; raios `--sh-radius-xs` (4), `--sh-radius-sm` (6), `--sh-radius` (8) e `--sh-radius-lg` (10). Sombras em camadas **só** para sobreposições (`--sh-shadow-pop` em menus e toasts, `--sh-shadow-modal` em diálogos e no painel peek, `--sh-shadow-1` quase invisível em controles). Véu `--sh-scrim`.
- **Vidro fosco** (`backdrop-filter`) só na barra superior, barra inferior do celular, barra de ações do formulário e paleta; `@supports` com cor sólida de reserva (`--sh-glass`).
- **Foco:** anel de 2 px com 2 px de afastamento, na cor `--sh-focus`, em tudo (contraste mínimo 6:1 no claro, 8,4:1 no escuro).
- **Layout do aplicativo:** barra lateral enxuta de 232 px (recolhível para 64 px, só ícones, com `title`), na cor do fundo; item atual vira cartão com filete de acento que **desliza** entre páginas. Barra superior fixa (56 px): migalhas à esquerda, **barra de comando central** (botão que abre a paleta; `Ctrl K` em fonte mono, `⌘ K` no Mac), ações à direita. Conteúdo em `.sh-container` (1200 px; `largura="form"` 960; `largura="larga"` 1440) e grade `.sh-grade` de 12 colunas (`.sh-col-3` a `.sh-col-9` e `.sh-col-12` a partir de 992 px; `.sh-col-md-6` a partir de 768 px). Celular: barra inferior de vidro com o botão central **Movimentar**.
- **Transição entre páginas** (View Transitions, mantida): shell fixo, conteúdo com fade curto + deslocamento de poucos pixels (8 px na vertical; 14 px ao entrar num nível mais fundo), marcador do menu deslizando, pré-carga, plano B no Firefox.
- **Tokens de movimento** (`css/tokens.css`):

| Token | Valor | Uso |
|---|---|---|
| `--sh-t-1` | 120 ms | hover, cor, saída rápida |
| `--sh-t-2` | 180 ms | menus, linhas de tabela, saída de painéis |
| `--sh-t-3` | 240 ms | entrada de conteúdo, toasts, painel, barra lateral |
| `--sh-t-4` | 400 ms | barras que crescem, revelações, contagens |
| `--sh-ease-out` | `cubic-bezier(.2,.8,.2,1)` | padrão (desaceleração) |
| `--sh-ease-out-forte` | `cubic-bezier(.16,1,.3,1)` | entradas grandes, painel, traçados |
| `--sh-ease-in` / `--sh-ease` | `cubic-bezier(.4,0,1,1)` / `cubic-bezier(.4,0,.2,1)` | saídas / trocas de cor |
| `--sh-stagger` | 40 ms | atraso entre irmãos; `--sh-dist-entrada`: 6 px |

  Nomes antigos (`--sh-dur-fast`, `--sh-dur`, `--sh-dur-slow`, `--sh-dur-lenta`, `--sh-ease-mola`) continuam, com os valores novos (a "mola" virou desaceleração simples). **Regras:** sem overshoot, sem bounce, sem confete, sem olhos piscando; entradas = fade + `translateY` de 4 a 8 px; barras crescem em `scale`; números contam **uma** vez; esqueletos sutis (faixa de luz em `transform`); diálogos com leve escala (`.98` a `1`); só `transform` e `opacity` (**única exceção:** linhas SVG usam `stroke-dashoffset` para se desenhar). `prefers-reduced-motion`: sem animação, sem transição entre páginas, estados finais visíveis. Arquivo: `css/movimento.css`.

### 12.5 Kit de rolagem (para a apresentação e o início)

Arquivos: `css/rolagem.css`, `js/rolagem.js`, `js/numeros.js`, `js/tema-init.js` (decide o modo). **Princípios:** nunca sequestra a rolagem nativa (nenhum ouvinte de `wheel` ou `touchmove`, nenhum `preventDefault`, nenhum `scroll-snap` obrigatório; a "altura" de uma cena é altura real e a página rola normalmente); modo CSS com `animation-timeline: view()` / `scroll()` dentro de `@supports`; plano B (Firefox e outros) com `IntersectionObserver` e um ouvinte de scroll **passivo**; `prefers-reduced-motion` = sem animação (tudo no estado final, cenas viram blocos comuns); sem JavaScript e sem suporte, **nada fica escondido**; impressão mostra tudo.

| Peça | Marcação | O que faz |
|---|---|---|
| **Revelar** | `.sh-revelar` no elemento, ou `.sh-revelar-grupo` no pai (**todos os filhos** são revelados, escalonados pela ordem). Variantes: `.sh-revelar--esquerda`, `--direita`, `--escala`, `--fade` | Fade + deslocamento (`--sh-revelar-dist`, 12 px) ao entrar na viewport. Modo CSS: acompanha a entrada (rolar para trás desfaz); plano B: uma vez (`.is-visivel`). Escalonamento: `--sh-i` (0, 1, 2...; automático pelos filhos de um grupo) |
| **Cena fixa** | `<section class="sh-cena"><div class="sh-cena__palco">...</div></section>`. Altura da cena: `--sh-cena-altura` (padrão `300vh`); abaixo de um cabeçalho fixo: `--sh-cena-topo` | O palco fica preso (`position: sticky`) enquanto a página rola pela cena; **`--sh-progresso`** (número de 0 a 1) vai de 0 (palco encosta no topo) a 1 (palco solta), herdado pelos filhos |
| **Efeitos da cena** | `.sh-cena-fade`, `.sh-cena-sobe`, `.sh-cena-escala`, `.sh-cena-desenha` (linha SVG com `pathLength="1"`), `.sh-cena-cresce` (barra em X) e `.sh-cena-cresce--v` (em Y) | Reagem ao `--sh-progresso`. Janela de cada elemento: `--sh-de` e `--sh-ate` (0 a 1, na CSS da página); `.sh-cena-fade` também aceita `--sh-saida` (some de novo); ajustes: `--sh-y0`, `--sh-s0`, `--sh-rampa` (inclinação do fade). Dentro dos efeitos: `--sh-t` (avanço 0 a 1 na janela), `--sh-in`, `--sh-out`; a página pode usá-los em `calc()` para criar efeitos próprios (só `transform` e `opacity`) |
| **Etapas discretas** | `data-sh-cena-passos="N"` na cena | O JS escreve `data-sh-passo="0..N-1"` e dispara o evento `sh:cena-passo` (`detail`: `passo`, `total`, `progresso`). Funciona nos dois modos |
| **Contador** | `<span data-contar="12480">12.480</span>` (+ `data-formato="inteiro | decimal | moeda | percentual"`, `data-contar-duracao="1400"`) | Conta de 0 ao valor **uma vez**, quando entra na viewport (já visível: na hora). O texto escrito pelo servidor é o valor final (sem JS ou com menos movimento nada muda). `SH.animarNumeros(raiz)` para conteúdo novo |
| **Desenhar** | contêiner `.sh-desenhar` (um `<svg>` ou `<div>`) com filhos `.sh-traco` (linha: **`pathLength="1"`**), `.sh-barra` (sobe em Y) e `.sh-barra--h` (cresce em X) | Linhas e barras se desenham conforme a rolagem (modo CSS) ou ao entrar na viewport (plano B). Escalonamento por `--sh-i` (automático pela ordem dos irmãos do mesmo tipo). `<sh:sparkline rolagem="true">` e `<sh:miniBarras rolagem="true">` já saem com essas classes |
| **Progresso da página** | `<div class="sh-progresso-pagina" aria-hidden="true"></div>` | Fio de 2 px no topo que cresce com a rolagem |
| **Profundidade** | `.sh-flutua` (distância em `--sh-flutua`, 24 px) | Desloca devagar em relação à página (só modo CSS; no plano B fica parado) |

API JS: `SH.rolagem.modo` (`'css'` ou `'js'`), `SH.rolagem.progresso(cena)` (0 a 1), `SH.rolagem.atualizar()` (depois de inserir conteúdo). Observações: (1) em animações dirigidas pela rolagem, o elemento observado precisa ter caixa CSS; para SVG, a classe `.sh-desenhar` vai no `<svg>` (ou num `div` em volta), nunca na linha; (2) no modo CSS, o progresso da cena usa a faixa `contain` (confirmada: 0 quando o palco gruda, 1 quando solta, mesmo com cena de `300vh`); (3) o `--sh-progresso` do plano B é escrito por CSSOM (`style.setProperty`), sem atributo `style` no HTML.

### 12.6 Componentes base da v3

- **Botões:** primário (acento, relevo interno de 1 px), secundário (`btn-outline-secondary`, superfície com borda), fantasma (`btn-ghost`), perigo; 36 px no desktop e 44 px no celular; pressionar = `scale(.985)`.
- **Campos:** 36/44 px, borda de 3:1, foco com anel de 2 px e borda de acento; erro com borda, ícone e texto.
- **Selos de estoque:** retângulo de 6 px, borda tonal, ícone + texto. `<sh:badgeEstoque>`. **Medidor:** 6 px, marcador no mínimo (`<sh:medidorEstoque>`), cresce ao aparecer.
- **Tabelas compactas:** células de 46 px, cabeçalho em cinza, números tabulares, hover suave, barra lateral de 3 px em baixo/sem estoque, modo cartão no celular (`sh-table--cards`). Valores e códigos: `.sh-col-mono` ou `td.sh-mono`.
- **Linha com foco e atalhos (opcional):** `data-sh-lista-teclado` no contêiner (`js/lista-teclado.js`). `j` próxima, `k` anterior, `Enter` abre (clica no `[data-sh-linha-link]` ou no primeiro link; com `data-sh-peek` abre o painel), `Esc` solta. Linha com foco: classe `.is-foco`. Respeita o desligamento dos atalhos, ignora campos de texto e diálogos abertos. Evento `sh:linha-foco`.
- **Sparkline e mini-barras (SVG no servidor, sem JS):** `<sh:sparkline valores="3,5,4,8" rotulo="..." tom="normal|baixo|sem" area="true" largura="120" altura="32" anima="true" rolagem="false" classe="sh-spark--bloco">` e `<sh:miniBarras valores="..." destaque="ultimo|maior|nenhum" ...>`. `valores` = números separados por vírgula (ponto decimal), do mais antigo ao mais novo; `role="img"` com `aria-label` (`rotulo`; sem ele a tag descreve os pontos). Cor por `currentColor` (`sh-acento`, `sh-tom--*`). A linha se desenha e as barras sobem uma vez ao aparecer; `rolagem="true"` liga ao kit.
- **Painel lateral "peek"** (`includes/sh/peek.jsp`, incluído pelo layout; `js/peek.js`; `css/peek.css`):
  - **Gatilho:** `<a href="/produtos/5" data-sh-peek data-sh-peek-titulo="Nome">` (ou qualquer elemento com `data-sh-peek-url="/produtos/5"`). Clique simples abre; Ctrl/Cmd/Shift/meio, nova aba e outra origem navegam normalmente; sem JS o link abre a página inteira.
  - **Conteúdo:** GET (cabeçalhos `Accept: text/html` e `X-SH-Peek: 1`) e extração da região **`[data-peek]`** da resposta; título do painel: `data-sh-peek-titulo` do gatilho, ou `data-peek-titulo` da região, ou "Detalhes". **A página completa precisa conter o elemento `data-peek`.** HTML recebido: `script`, `style`, `link`, `meta`, `base`, `iframe`, `object`, `embed`, `noscript`, atributos `on*` e URLs `javascript:` são removidos antes de inserir.
  - **Acessibilidade:** `<dialog>` modal com `aria-labelledby`, foco preso (Tab e Shift+Tab), Esc fecha, clique no fundo fecha, o foco volta ao gatilho, `aria-busy` enquanto carrega, anúncio ao terminar, estado de erro com "Tentar de novo" e link para a página completa. Sem deslize com menos movimento.
  - **API:** `SH.peek.abrir(url, { titulo, gatilho })`, `SH.peek.fechar()`, `SH.peek.aberto()`. **Eventos** (no `<dialog id="sh-peek">`, com bolha): `sh:peek-abrir`, `sh:peek-carregado` (`detail`: `url`, `corpo`), `sh:peek-erro`, `sh:peek-fechar`.
- **Toasts sérios:** `SH.toast(texto, opcoes)`; `opcoes.tom` (`sucesso`, `info`, `aviso`, `erro`; erro não fecha sozinho), `opcoes.desfazer = function` (ou `opcoes.acao = { rotulo, aoClicar }`), `opcoes.duracao`. Fio de tempo que pausa com mouse ou foco; 7 s (10 s com ação); Esc fecha. Mensagem do servidor: `<div data-sh-toast data-sh-toast-tom="info" data-sh-toast-desfazer="#form-desfazer">` onde o seletor aponta para um `<form method="post">` com CSRF **fora** do toast.
- **Estados vazio, erro e carregando:** `<sh:estadoVazio>` (mesma API: `ilustracao` = `caixa`, `lupa`, `etiquetas`, `ok`, novo `erro`; ou `icone`/`tom`; `tom="erro"` usa `role="alert"`), com ilustrações geométricas de traço fino que se desenham uma vez. Carregando: `.sh-esqueleto` (+ `.sh-esq-linha`, `.sh-esq-cartao`, dentro de `.sh-carregando`).
- **Barra de comando (paleta):** mesmo comportamento e API (`GET /api/busca`, atalhos); vidro fosco, marca do trecho encontrado em sublinhado de acento.

### 12.7 Tokens novos e legados

Novos: `--sh-surface-3`, `--sh-font-mono`, `--sh-fs-*`, `--sh-track-*`, `--sh-lh-*`, `--sh-t-1` a `-4`, `--sh-ease-out-forte`, `--sh-dist-entrada`, `--sh-shadow-1`, `--sh-scrim`, `--sh-glass`, `--sh-glass-borda`, `--sh-radius-xs`, `--sh-space-16/24`, `--sh-lateral-w`, `--sh-topbar-h`, e os do kit (`--sh-revelar-dist`, `--sh-rampa`, `--sh-progresso`, `--sh-cena-*`). **Legados v2** (`--sh-terra*`, `--sh-kraft*`, `--sh-tinta`, `--sh-papel`, `--sh-pauta`, `--sh-marca-texto`, `--sh-radius-folha`) existem só como valores neutros para telas antigas; **nada novo deve usá-los** e eles saem junto com a migração do início.

### 12.8 Mapa de telas (atualizado)

| Tela | Estado nesta entrega |
|---|---|
| Login e cadastro | **v3 pronta** (`login.jsp`, `entrar.css`, `entrar.js`): palco escuro com linhas de gráfico abstratas (nível de estoque em "dente de serra" e a linha do mínimo) que se desenham devagar, formulário limpo, aviso de Caps Lock |
| Apresentação pública (`/`) | **v3 pronta** (`apresentacao.jsp`, `apresentacao.css`, `apresentacao.js`), narrativa de rolagem com o kit (12.5) |
| Início (`dashboard.jsp`) | **v3 pronta** ("Visão geral": KPIs, reposição rápida, saúde do estoque e gráfico de 14 dias) |
| Produtos, formulário, detalhe, categorias, movimentações, estoque baixo, unidades, valor do estoque, erro | **v3 prontas** (painel peek, lista por teclado, ajuste rápido de saldo, livro-razão) |

### 12.9 Arquivos da v3

**Criados:** `css/movimento.css`, `css/rolagem.css`, `css/peek.css`; `js/rolagem.js`, `js/peek.js`, `js/lista-teclado.js`, `js/tempo.js` (substitui `humano.js`); `WEB-INF/tags/marca.tag`, `sparkline.tag`, `miniBarras.tag`; `WEB-INF/includes/sh/peek.jsp`; `vendor/geist-mono/`. **Reescritos:** `css/tokens.css`, `css/layout.css`, `css/paleta.css`, `css/entrar.css`, `js/entrar.js`, `js/toast.js`, `js/numeros.js`, `js/tema-init.js`, `tags/layout.tag`, `tags/estadoVazio.tag`, `includes/sh/head.jsp`, `includes/sh/lateral.jsp`, `pages/login.jsp`, `img/favicon.svg`; `css/componentes.css` (v1 restilizado + componentes novos). **Ajustados:** `tags/layoutPublico.tag` (atributo `classe`), `includes/sh/paleta.jsp`, `pages/dashboard.jsp` e `css/inicio.css` (só a remoção do mascote). **Removidos:** `css/identidade.css`, `js/humano.js`, `tags/mascote.tag`, `vendor/fraunces/`.

### 12.10 Verificação desta entrega

Renderizado em mini-Tomcat avulso (portas altas) com Edge headless (Playwright): login e cadastro (com e sem erros) em computador e celular, claro e escuro; página de teste com todos os componentes e o kit; as demais telas (produtos, formulário, detalhe, categorias, movimentações, estoque baixo, unidades, valor do estoque, erros 404 e 500, início em três estados) em computador claro e celular escuro. **axe: 0 violações** em todas (a única que apareceu foi um campo sem rótulo na página de teste, não no sistema). Sem rolagem horizontal, sem erro de JS, sem recurso 404. Kit: no modo CSS, `--sh-progresso` medido em 0, 0,15, 0,5, 0,85 e 1 ao longo de uma cena de `300vh`, com as etapas e os fades corretos; o plano B foi exercitado desligando o `@supports` do CSS e a detecção em JS (Chrome simulando Firefox): revelar, cena, desenhar e progresso funcionam. Peek: abre por teclado (`j`, `k`, `Enter`), foco preso, Esc, foco devolvido, `script` e `onerror` removidos, erro sem região. **Não verificado:** Firefox e Safari de verdade, leitor de tela real, pré-carga contra o servidor real e desempenho em aparelho modesto.

## 13. Glossário das telas de NF-e (linguagem simples)

Nas telas de compra usamos nomes comuns; o termo fiscal fica só em dica (`title`) ou em "Detalhes fiscais" recolhido.

| Termo na tela | Termo fiscal |
|---|---|
| Fornecedor | emitente |
| Comprador | destinatário |
| Número da nota | número da NF-e (série e modelo ficam em "Detalhes fiscais") |
| Data da compra | data de emissão |
| Código da nota | chave de acesso |
| Total da nota | valor total da NF-e |
| Código de barras | EAN/GTIN |
| Classificação fiscal | NCM |
| Tipo da operação | CFOP / natureza da operação |
| Situação do item | status da importação do item |

- CFOP vira texto ("Compra para revenda", "Devolução", "Remessa para conserto") pela tag `sh:cfop` (`WEB-INF/tags/cfop.tag`), o único lugar do mapeamento; o número aparece pequeno e no `title`. Código desconhecido sai como "Operação código NNNN".
- Frete, desconto, seguro e outras despesas mantêm esses nomes. Seguro e outras despesas só aparecem quando a nota os trouxer.
- "Frete e outras despesas" (por item) é a parte que coube ao item no rateio e já está no custo. "Como foi pago" lista as formas de pagamento (Boleto, Pix, Cartão de crédito...) e "Parcelas" lista número, vencimento e valor; ambas são só para consulta (`sh:cobrancaNfe`).
- Nota que não é compra normal (complementar, ajuste, devolução) é recusada no envio com a frase "Esta é uma nota de ...", sem termo fiscal solto.
- Impostos e demais campos fiscais ficam em seção recolhida (`<details class="nfe-detalhes">`).
- Desfazer movimentação (5b): "Desfazer" (estorno), "Desfeita por um estorno", "Estorno da movimentação #n". Reserva: "Em estoque", "Reservado" e "Disponível" (disponível = em estoque menos reservado). "Clientes" só aparece no menu das lojas com Vendas ou Ordens de serviço.

## 14. Refino v4: acabamento "produto" (27/10/2026)

Refino de acabamento sobre a v3, sem mudar rotas, regras de negócio nem estrutura das telas. **Tudo nasce em `css/tokens.css`** (um lugar só); as telas herdam.

| Token | Antes | Agora | Efeito |
|---|---|---|---|
| `--sh-font` | Inter primeiro | `-apple-system, BlinkMacSystemFont, "SF Pro Text", "Inter", system-ui, ...` | SF em aparelhos Apple, Inter local nos demais; sem CDN |
| `--sh-font-mono` | Geist Mono | **mesma pilha da interface** (algarismos tabulares via `font-variant-numeric`) | `.sh-mono` e valores grandes deixam de parecer "máquina de escrever" |
| `--sh-font-codigo` (novo) | n/a | Geist Mono | só `code.sh-codigo` |
| `--sh-radius-xs/sm/radius/lg` | 4 / 6 / 8 / 10 px | **6 / 8 / 12 / 16 px** | campos e botões 8, cartões e tabelas 12, painéis, modais e peek 16 (o mapeamento do Bootstrap segue os tokens) |
| `--sh-track-rotulo` | 0,07em | **0** | rótulos pequenos em caixa normal, sem tracking |
| `--sh-shadow-card` (novo) | n/a | `0 1px 2px` + `0 4px 16px -6px` a 4 a 6% | sombra quase invisível em cartões, métricas e tabelas; no escuro vale `0` (a borda separa) |
| `--sh-shadow-1` | 6% | 5% | controles secundários |

Padrões novos (bloco "Refino v4" no fim de `css/componentes.css`):

- **Sem caixa-alta:** `text-transform: uppercase` saiu de todos os rótulos (eyebrows, grupos do menu, cabeçalhos do livro-razão, passos); hierarquia vem de tamanho, peso e cor.
- **Superfícies:** `.sh-card`, `.sh-metric`, `.sh-table-wrap` e os painéis das telas usam `--sh-shadow-card` + borda de 1 px; o hover de cartão clicável só muda o fundo, nunca levita.
- **Cabeçalho de página:** mais respiro (32 px abaixo), título 680 com tracking de -0,03em, descrição limitada a 62ch.
- **Tabelas:** cabeçalho com mais ar, divisores de linha a 70% (hairline mais leve).
- **Links de texto:** sublinhado de 1 px com `text-underline-offset` e transição de cor.
- **Barra de rolagem fina** (`scrollbar-width: thin`) onde há suporte.
- **Mantidos:** vidro com `backdrop-filter` só em barra superior, barra inferior, barra de ações e paleta; acento único (cobalto); foco de 2 px com afastamento; `prefers-reduced-motion` zera animações e transições; contraste AA (nenhuma cor mudou).

**Correção:** o botão "Desfazer" agora aparece na linha que `js/v3-estoque.js` insere logo depois de lançar um movimento. A página informa o papel em `data-v3e-pode-desfazer` (`EstoqueController` envia `podeDesfazer`); o JS só mostra o botão quando o papel pode e o saldo mudou (mesma regra de `EstoqueService.estornaveis`, que continua sendo a decisão final no servidor).

**Verificação:** Edge + Playwright contra a aplicação real (porta 8082), 1440 e 390 px, claro e escuro, em `/`, `/login`, `/cadastro`, início, produtos, movimentações, análises, NF-e, equipe, categorias, estoque baixo, unidades e valor do estoque: sem rolagem horizontal e sem erro de console. **Não verificado:** telas de OS e clientes (a loja demo não tem esses módulos e criar outra conta exige aprovação), Safari e Firefox.
