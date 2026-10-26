# StockHub: mercado e requisitos (Fase 1 do redesenho)

Documento para o dono do projeto e para o `designer-ui` e o `dev-backend` na próxima fase.
Legenda usada em todo o texto:

- **[Fonte]** afirmação confirmada por uma fonte listada na seção 1.
- **[Inferência]** conhecimento geral ou conclusão minha, sem fonte específica.
- **[Código]** verificado lendo o código do repositório.

## 1. Fontes e limites

A pesquisa foi feita pelo coordenador, sem acesso à web neste documento. Nada aqui foi acrescentado além do que ela trouxe.

### Fontes consultadas

| Tema | Fonte | Tipo |
|---|---|---|
| Bling: reserva de estoque, estoque mínimo, tipos de movimentação | [5 funcionalidades](https://blog.bling.com.br/funcionalidades-bling-que-voce-precisa-conhecer/), [Movimentação de estoque](https://blog.bling.com.br/?p=21628) | blog do fornecedor |
| Bling: relatórios (opinião de usuário) | [b2bstack](https://www.b2bstack.com.br/product/bling-erp/avaliacoes) | avaliação isolada |
| Omie: Curva ABC, estoque em data específica | [Curva ABC](https://ajuda.omie.com.br/pt-BR/articles/5833962-curva-abc-como-emitir-entender-e-analisar), [Relatórios de Curva ABC](https://ajuda.omie.com.br/pt-BR/articles/17217628-conhecendo-os-relatorios-de-curva-abc-de-produtos) | ajuda oficial |
| Omie: PEPS para perecíveis, inventário rotativo | [Blog Omie](https://www.omie.com.br/blog/gestao-de-estoque-para-pme-aprenda-a-aplicar/index.md) | blog do fornecedor |
| Odoo: lote/série, regras mín/máx, código de barras, relatórios | [guia Jupical](https://timesheet-jupical.odoo.com/blog/our-blog-1/how-to-optimize-your-inventory-management-with-odoo-111), [guia Ecosire](https://ecosire.com/guides/automation/inventory-automation) | guias de terceiros |
| Dor: estoque fantasma | [Contmatic](https://simplifique.contmatic.com.br/blogs/estoque-fantasma-como-a-falta-de-controle-faz-perder-vendas) | blog de fornecedor |
| Dor: causas de divergência (processo manual, perdas sem apontamento, conferência) | [TCC UFPE](https://repositorio.ufpe.br/bitstream/123456789/65333/5/TCC%20FELIPE%20JESS%c3%89%20DE%20ALMEIDA.pdf) | trabalho acadêmico, caso específico |
| Dor: cliente migra ao não achar o produto | [Sebrae (varejo)](https://sebrae.com.br/Sebrae/Portal%20Sebrae/UFs/RJ/Click%20Empreendedor/GESTÃO%20DE%20ESTOQUES%20NO%20VAREJO.pdf) | material do Sebrae citando pesquisa americana |
| Dor: causas da ruptura | [Unicruz](https://home.unicruz.edu.br/wp-content/uploads/2017/11/Carine-Lais-Secretti.pdf), [Tray](https://tray.com.br/escola/o-que-e-quebra-de-estoque/) | trabalho acadêmico e blog |
| Dor: contagem manual e devoluções mal codificadas | [Uniube](https://dspace.uniube.br/jspui/bitstream/123456789/2804/1/projetointegradorelatorio3.pdf) | trabalho acadêmico |
| Dor: compra em excesso e falta de conferência na entrada | [Linear Sistemas](https://www.linearsistemas.com.br/?p=1724) | blog de fornecedor |
| Recursos essenciais em software para pequenas empresas | [EngineerBabu](https://engineerbabu.com/blog/features-of-an-inventory-management-software/), [ControlHub](https://www.controlhub.com/blog/small-business-inventory-system), [Sortly](https://www.sortly.com/?p=2699) | blogs de fornecedores |
| Tiny/Olist | [integração Olist e Tiny](https://olist.com/blog/pt/olist/olist-news/integracao-olist-e-tiny-2) | blog, sem detalhe de estoque |

### Lacunas e limites

- **Tiny ERP:** não foi achada documentação oficial sobre lote, validade ou inventário. O Tiny não entra em nenhuma comparação deste documento.
- **Omie:** ponto de pedido e estoque mínimo **não foram confirmados**. Curva ABC e estoque em data específica foram.
- **Bling:** não foram achadas telas de inventário nem a lista completa de relatórios. A crítica à personalização de relatórios é uma opinião isolada.
- **Odoo:** as fontes são guias de terceiros, não a documentação oficial. Os números de "99% de acurácia" desses guias são marketing sem fonte e foram descartados.
- **Blogs de fornecedores** (Bling, Omie, Contmatic, Linear, Tray e os guias de software) têm interesse em vender; servem como **indicativo**, não como prova neutra.
- **Estudos de dor** (UFPE, Uniube, Unicruz) são casos específicos, não retrato do mercado inteiro. O número de 31% do Sebrae vem de uma pesquisa americana citada de segunda mão.
- Não há dado de mercado confiável sobre quanto cada dor custa. Não usar percentuais em apresentação sem citar a fonte e o contexto.

## 2. Dores comuns de quem controla estoque

| Dor | O que a pesquisa diz | Fonte |
|---|---|---|
| Estoque divergente do real ("estoque fantasma") | Diferença entre o que o sistema mostra e o físico; gera venda perdida | [Contmatic](https://simplifique.contmatic.com.br/blogs/estoque-fantasma-como-a-falta-de-controle-faz-perder-vendas) |
| Causas da divergência | Falta de padronização em processos manuais, ausência de apontamento de perdas, falhas de conferência | [TCC UFPE](https://repositorio.ufpe.br/bitstream/123456789/65333/5/TCC%20FELIPE%20JESS%c3%89%20DE%20ALMEIDA.pdf) |
| Perdas não registradas | Vencido, quebrado, extraviado ou furtado que nunca é baixado | Contmatic e UFPE (acima) |
| Ruptura (cliente não acha o produto) | Sebrae cita pesquisa americana: 31% dos clientes vão à concorrência. Causas discutidas: treinamento de repositor, furto, armazenagem | [Sebrae](https://sebrae.com.br/Sebrae/Portal%20Sebrae/UFs/RJ/Click%20Empreendedor/GESTÃO%20DE%20ESTOQUES%20NO%20VAREJO.pdf), [Unicruz](https://home.unicruz.edu.br/wp-content/uploads/2017/11/Carine-Lais-Secretti.pdf), [Tray](https://tray.com.br/escola/o-que-e-quebra-de-estoque/) |
| Erro de contagem e de lançamento manual | Erros de digitação e omissão na contagem manual; devoluções mal codificadas dão registro impreciso | [Uniube](https://dspace.uniube.br/jspui/bitstream/123456789/2804/1/projetointegradorelatorio3.pdf) |
| Inventário manual | Contagem geral e manual é fonte de erro. O Omie recomenda inventário rotativo (contar partes do estoque de tempos em tempos) | Uniube; [Blog Omie](https://www.omie.com.br/blog/gestao-de-estoque-para-pme-aprenda-a-aplicar/index.md) |
| Excesso de compra e falta de conferência na entrada | Compra acima da necessidade ocupa espaço e aumenta o risco de vencimento; sem conferência na entrada não se acha a diferença | [Linear Sistemas](https://www.linearsistemas.com.br/?p=1724) |
| Falta de rastreabilidade e validade | Odoo oferece lote/série e rastreabilidade; Omie recomenda PEPS para perecíveis | guias Odoo e Blog Omie |
| Compras atrasadas | **[Inferência]** é consequência direta de não ter alerta de mínimo. A pesquisa trouxe só o alerta como recurso (Bling e guias de software), não um estudo sobre atraso de compra | Bling, guias de software |

## 3. Recursos para ser usável numa empresa real

| Recurso | Evidência | Fonte |
|---|---|---|
| Busca rápida e filtros | **[Inferência]** padrão de qualquer lista grande; a pesquisa não trouxe fonte específica | n/d |
| Ações em lote | **[Inferência]**, sem fonte na pesquisa | n/d |
| Histórico de movimentações (entrada, saída, transferência, devolução, ajuste) | Bling lista esses tipos; Odoo tem "Product Moves" | Bling, guias Odoo |
| Alerta de estoque mínimo / ponto de pedido | Bling (aviso de reposição e mínimo por produto); Odoo (regras mín/máx que geram compra automática); guias de software | Bling, Odoo, EngineerBabu, ControlHub, Sortly |
| Relatórios | Bling tem gerador (personalização criticada por um usuário); Odoo: Stock Valuation, Inventory at Date, Product Moves, Forecasted Inventory | b2bstack, guias Odoo |
| Exportação CSV | "Must-have" em guias de software | guias de software |
| Controle de acesso por perfil | Quem altera estoque, quem vê relatórios, gerente só da própria loja | guias de software |
| Trilha de auditoria | "Must-have" em guias de software | guias de software |
| Inventário / contagem | Bling: ajuste de inventário; Odoo: contagem cíclica no app; Omie: inventário rotativo | Bling, Odoo, Blog Omie |
| Código de barras | Odoo: recebimento, separação e contagem; guias de software citam mobile e leitura | guias Odoo e de software |
| Lote e validade | Odoo (lotes e séries); Omie recomenda PEPS para perecíveis | guias Odoo, Blog Omie |
| Reserva de estoque | Bling: reserva para pedidos em aberto, evita vender o mesmo item duas vezes | Blog Bling |
| Curva ABC | Omie tem relatório de Curva ABC de produtos | Ajuda Omie |
| Estoque em data específica | Omie (quantidade e valor numa data); Odoo ("Inventory at Date") | Ajuda Omie, guias Odoo |
| Facilidade para não técnicos; multi-local | Citados em guias de software | guias de software |

## 4. Cobertura atual do StockHub

Baseada na leitura de `src/main/java`, `src/main/webapp`, `src/main/resources/db/migration` (V1 e V2) e `docs/regras-negocio.md`. A pasta `db/` na raiz não existe; as migrations estão em `src/main/resources/db/migration`.

Esforço: P = pequeno (horas), M = médio (1 a 2 dias), G = grande (vários dias).

| Recurso | Situação | Onde está no código / o que falta | Esforço |
|---|---|---|---|
| Busca rápida | Parcial | `ProdutoSpecs.comBusca`: nome, nome da categoria ou id exato; formulário em `produtos.jsp`. Falta busca por código/SKU (o produto não tem esses campos) e busca ao digitar. Busca existe também em `categorias.jsp` | P (busca ao digitar: front) |
| Filtros | Parcial | Produtos: categoria e nível (`ProdutoFiltro`, `ProdutoSpecs.comNivel`), ordenação por 5 campos, paginação. Histórico (`estoque.jsp`, `EstoqueController`): **sem nenhum filtro** (só paginação de 50) | M (filtros do histórico: backend) |
| Ações em lote | Falta | `produtos.jsp` só tem ações por produto (ver, editar, excluir). Sem seleção múltipla nem endpoint de lote | M a G |
| Histórico de movimentações | Tem | `MovimentacaoEstoque` guarda tipo, quantidade, saldo anterior e novo, observação e usuário (RN-06); tela `estoque.jsp`; imutável. Falta histórico **por produto** (`produto-detalhe.jsp` não mostra movimentações; o repositório só lista por loja) | P a M (por produto: backend pequeno) |
| Tipos de movimentação | Parcial | `TipoMovimentacao`: ENTRADA, SAIDA, AJUSTE. Sem transferência, devolução nem motivo estruturado (perda, quebra, vencimento); motivo é texto livre em `observacao` | M (enum de motivo: backend + migration) |
| Alerta de estoque mínimo | Tem | `estoque_minimo` no produto; `NivelEstoque` (RN-12: SEM_ESTOQUE, BAIXO, NORMAL); `/estoque-baixo` (`estoque-baixo.jsp`), painel do dashboard, contagem em `ResumoEstoque`. Sem aviso externo (e-mail) nem sugestão de compra | Tem; sugestão de compra: M |
| Relatórios | Parcial | Telas `unidades.jsp` e `valor-estoque.jsp` (total de itens e valor, listados por nome), `estoque-baixo.jsp`, dashboard. Sem relatório de movimentação por período, sem valor por categoria | M |
| Exportação CSV | Falta | Nenhum endpoint de exportação nem botão. O `grep` por `csv` e `export` em `src/main` não achou nada | M (backend) |
| Controle de acesso por perfil | Falta | `UsuarioPrincipal` devolve só `ROLE_USER`; `SecurityConfig` só distingue logado de não logado. Cada `Usuario` é uma loja (RN-01), não uma pessoa com papel. Isolamento por loja existe e é sólido | G (modelo novo: usuário da loja com papéis) |
| Trilha de auditoria | Parcial | Movimentações são imutáveis e guardam usuário e saldos (RN-06), com índice por produto. **Não** há auditoria de edição de produto (preço, nome, estoque mínimo) nem de exclusão/inativação | M |
| Inventário / contagem | Parcial | `AJUSTE` recebe a contagem final e exige motivo (RN-05); registra saldo anterior e novo, o que mostra a divergência. Falta tela de contagem (várias linhas de uma vez), diferença esperado x contado e inventário rotativo | M a G |
| Código de barras | Falta | `Produto` não tem campo de código; sem leitura por câmera/leitor. Busca por id numérico existe | M (campo + índice único: backend e migration) |
| Lote e validade | Falta | Nenhum campo `lote` ou `validade` em `Produto`/`MovimentacaoEstoque`. O saldo é um número único por produto | G |
| Reserva de estoque | Falta | Só existe o saldo (`quantidade`); sem pedidos, sem saldo reservado/disponível | G (e fora do escopo: não há módulo de vendas) |
| Curva ABC | Falta | Não há consumo/saída por período agregado nem relatório. Os dados existem (histórico de SAIDA com preço do produto), falta a consulta | M |
| Estoque em data específica | Falta (dados permitem) | Cada movimentação guarda `quantidadeNova` e `criadoEm`, então o saldo numa data é calculável (última movimentação do produto até a data). Falta consulta e tela | M |
| Dashboard / resumo | Tem | `DashboardService.buscarResumo` (`ResumoEstoque`): total de produtos, categorias, estoque baixo, sem estoque, unidades, valor total (RN-13) | n/a |
| Isolamento entre lojas, saldo nunca negativo, concorrência | Tem | RN-01, RN-02, RN-07 (bloqueio pessimista em `EstoqueService.registrar`) | n/a |

Observações de achados no código que afetam a próxima fase:

- `estoque.jsp` carrega **todos** os produtos da loja num `<select>` (`produtoService.listarParaSelecao`). Em catálogo grande fica pesado e difícil de usar; [Inferência] uma busca no campo resolve sem backend novo.
- A listagem de produtos usa tamanho de página 8 por padrão (`ProdutoFiltro.TAMANHO_PADRAO`); máximo 100.
- As telas de dashboard, `unidades` e `valor-estoque` repetem a mesma lista de produtos por nome (`DashboardController.listarPorNome`).

## 5. Priorização e recomendação

### Priorização por impacto

| Impacto | Recurso | Justificativa curta |
|---|---|---|
| Alto | Filtros e período no histórico | Conferir divergência exige achar a movimentação. Hoje só há paginação. Ataca "estoque fantasma" e "falta de conferência" |
| Alto | Motivo estruturado de saída/ajuste (perda, quebra, vencimento, furto, correção) | Ataca "perdas não registradas", dor central na pesquisa. Hoje o motivo é texto livre |
| Alto | Histórico por produto na tela de detalhe | Rastrear um item é o uso mais comum da trilha; os dados já existem |
| Alto | Exportação CSV | Citada como essencial; permite o usuário levar dados para planilha |
| Médio | Telas de relatório melhores (valor por categoria, movimentação por período) | Os dados existem; hoje há só listas |
| Médio | Inventário/contagem em lote | Reduz erro de contagem manual; maior esforço |
| Médio | Estoque em data específica | Citado em Omie e Odoo; dados já permitem |
| Médio | Código de barras/SKU como campo e busca | Útil, mas leitura por câmera é trabalho extra |
| Médio | Auditoria de edição de produto | Completa a trilha; só movimentação é auditada |
| Médio | Curva ABC | Relatório clássico (Omie), agrega valor de análise |
| Baixo (neste projeto) | Ações em lote | **[Inferência]** conveniência; sem fonte na pesquisa |
| Baixo (neste projeto) | Controle de acesso por perfil | Citado nos guias, mas o modelo atual é "um usuário = uma loja"; mudar é grande e arriscado |
| Baixo (neste projeto) | Lote e validade, reserva de estoque | Fonte confirma que grandes sistemas têm, mas o custo é alto e reserva pressupõe pedidos, que o StockHub não tem |

### Recomendação

**Entra na v1 (cada item marcado com quem faz):**

| Item | Quem faz |
|---|---|
| Redesenho visual e de usabilidade das telas existentes (dashboard, produtos, detalhe, estoque, categorias, login), responsividade, acessibilidade | designer-ui (só front) |
| Busca ao digitar e escolha de produto com busca no formulário de movimentação (em vez do `<select>` gigante) | designer-ui (só front; usa dados já enviados) |
| Seletor de motivos pré-definidos (perda, quebra, vencimento, furto, correção de contagem) que **preenche o texto da observação** | designer-ui (só front, sem mudar backend); versão estruturada fica para depois |
| Destaque visual de estoque baixo e sem estoque, atalhos do dashboard para `/estoque-baixo` e para os produtos filtrados por nível (filtro `nivelEstoque` já existe) | designer-ui (só front) |
| Impressão de listas por CSS (`@media print`) como alternativa leve de relatório | designer-ui (só front) |
| Filtros no histórico (produto, tipo, período) | dev-backend (repositório, service e controller) |
| Histórico por produto na tela de detalhe | dev-backend (consulta por produto) + designer-ui (bloco na tela) |
| Exportação CSV (produtos e histórico) | dev-backend (endpoint) + designer-ui (botão) |

Critério: a v1 usa o que o backend já entrega (resumo, paginação, filtros de produto, saldo anterior/novo) e só pede backend novo onde não há como resolver no front: filtros do histórico, histórico por produto e CSV. Cada item de backend é pequeno ou médio, sem migration.

**Fica para depois (v2), se sobrar tempo:**

- Motivo estruturado como enum com migration (substitui o texto livre).
- Relatórios: valor por categoria, movimentação por período, estoque em data específica, Curva ABC.
- Tela de inventário/contagem em várias linhas, com diferença esperado x contado.
- SKU/código de barras como campo único por loja, com busca.
- Auditoria de edição de produto.
- Ações em lote.

**Não vale a pena num projeto acadêmico:**

- Lote e validade (PEPS, rastreabilidade): muda o modelo de saldo, que hoje é um número por produto.
- Reserva de estoque: exige pedidos/vendas, fora do escopo.
- Controle de acesso por perfil: o modelo atual (um `Usuario` por loja, `ROLE_USER` único) teria de ser refeito; o risco de quebrar o isolamento por loja (RN-01) é maior que o ganho.
- Leitura de código de barras por câmera, app mobile, multi-local, transferências e integrações com ERP/marketplace.
- Pedido de compra automático (regras mín/máx do Odoo).

### Divisão entre front-end e backend

| Só front-end (designer-ui, sem mudar backend) | Exige backend novo (dev-backend) |
|---|---|
| Layout, componentes, responsividade, acessibilidade de todas as telas | Filtros e período no histórico de movimentações |
| Busca ao digitar (filtro na lista já carregada ou envio automático do formulário que já existe) | Histórico por produto |
| Seletor de motivos que preenche a observação | Exportação CSV |
| Atalhos do dashboard para listas já filtradas por nível | Motivo estruturado, SKU/código, auditoria de edição (v2, com migration) |
| Impressão por CSS | Relatórios novos: Curva ABC, estoque em data, valor por categoria (v2) |

## Incertezas que continuam

- Não sabemos o que o Tiny faz de lote, validade e inventário.
- Não confirmamos estoque mínimo/ponto de pedido do Omie.
- Os guias do Odoo não são a documentação oficial.
- As prioridades de impacto da seção 5 combinam a pesquisa com o que cabe no prazo; a ordem entre itens "médio" é julgamento meu, não resultado de pesquisa. Cabe ao dono do projeto validar com usuários reais, se houver.
- "Ações em lote", "busca rápida" e "filtros" não têm fonte específica na pesquisa; estão marcados como inferência.
