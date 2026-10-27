# Plano de evolução do StockHub

Status: **proposta, nada implementado**. Cada etapa vira commits pequenos, com `./mvnw test` verde antes de seguir.

## Etapa 0. Pendências de decisão
- Enviar ao GitHub os 4 commits da NF-e (precisa de aprovação).
- Limpar os dados de teste da loja demo (nota 90001, pendentes 90002 e 90077, produtos criados por elas).

## Etapa 1. Linguagem simples nas telas de NF-e (pequena)
- Trocar termos fiscais por nomes comuns: Fornecedor, Comprador, Número da nota, Data da compra, Código da nota, Total da nota, Frete, Seguro, Outras despesas, Desconto, Código de barras, Classificação fiscal, Tipo da operação, Situação da nota.
- CFOP vira texto ("Compra para revenda", "Devolução", "Remessa para conserto") com o número como detalhe.
- Impostos numa seção recolhida; termo técnico só em dica ao passar o mouse.
- Só JSP, tags e texto; sem mudança de regra. Atualizar `docs/design-system.md` com o glossário.

## Etapa 2. Tipo de loja e subtipos
Modelo: `tipo_loja` (grande) + `subtipo` (ramo), guardados na conta (Flyway V6, valor padrão `ESTOQUE`). Define só quais módulos e menus aparecem, nunca as regras; mudar de tipo depois não perde dados.

| Tipo | Subtipos (a validar) | Módulos ligados |
|---|---|---|
| Só controle de estoque | almoxarifado, depósito, estoque de escritório | produtos, movimentações, NF-e, análises |
| Comércio | mercado, papelaria, material de construção, farmácia, vestuário, informática, outro | acima + vendas, clientes, contas a receber |
| Oficina e serviços | mecânica, moto, elétrica/eletrônica, assistência técnica (celular/informática), refrigeração, outro | acima + ordens de serviço, peças usadas, mão de obra, equipamento do cliente |
| Misto (comércio + serviço) | à escolha | tudo |

- Escolha no cadastro da conta (cartões com descrição simples) e alterável em Configurações.
- Subtipo também sugere unidades, categorias iniciais e campos úteis (ex.: placa/modelo na oficina de veículos). A validar com pesquisa antes de implementar.
- Menu, paleta de comandos e página inicial leem o tipo para mostrar só o que vale.

## Etapa 3. Fase 4: análises de estoque com gráficos
- Valor do estoque no tempo, "caixa" (compras x saídas), saídas por dia, produto e categoria.
- Cobertura em dias, produtos parados, curva ABC, alertas por regras.
- Gráficos SVG próprios, sem biblioteca; seguir a skill `dataviz`; acessíveis, com tema claro e escuro.
- Consultas agregadas no banco; isolamento por loja; testes de integração.

## Etapa 4. Complementos da NF-e
- Rateio de frete, seguro, outras despesas e desconto no custo do produto.
- Dados de pagamento e vencimentos (base para contas a pagar).
- Notas de devolução e complementares; evento de cancelamento.
- Avisos de item com flags estruturadas; `ean` e `ncm` na lista de produtos.
- Verificar `_csrf` do envio sem JavaScript em servidor real.

## Etapa 5. Base para vendas e OS
- Separar **Loja** de **Usuário** (vendedor, mecânico) com papéis e permissões.
- Cada movimentação aponta para o documento de origem (compra, venda, OS, devolução, perda, inventário).
- Reserva de estoque: em estoque, reservado e disponível; baixa ao fechar a OS.
- Custo e preço gravados no movimento (lucro histórico estável).
- Cancelamento por estorno (movimento contrário); a imutabilidade continua.
- Clientes (reaproveitar `Parceiro`), vendas, contas a receber.

## Etapa 6. Ordens de serviço (só tipos Oficina e Misto)
- Fluxo: orçamento, aprovação, execução, entrega.
- Peças (baixam estoque), mão de obra, equipamento ou veículo do cliente, garantia.
- Impressão da OS; histórico por cliente.

## Etapa 7. Fechamento de cada ciclo
- README: testes (470 para o número atual), funcionalidade NF-e, RN-01 a RN-42.
- Atualizar `docs/regras-negocio.md`, `docs/arquitetura.md` e a memória do projeto.
- Revisão com `revisor-codigo` antes de cada envio; `git push` só com aprovação.

## Pendências antigas
Senha `1234` do postgres, descrição e tópicos do repositório, e-mail hotmail nos commits, pasta `StockHub-backup-contexto`, git stash antigo, testes de Firefox, Safari e leitor de tela.

## Economia de tokens
- Agentes recebem tarefas fechadas e devolvem relatório curto.
- Suíte completa só no fim de cada etapa; entre elas, só os testes afetados.
- Leitura de arquivos por trecho; sem reler o que já foi lido.
