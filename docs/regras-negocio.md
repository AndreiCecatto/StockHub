# Regras de negócio do StockHub

Todas as regras vivem na camada de serviço, nunca no controller nem na JSP. O banco repete as que forem restrições de integridade, como última defesa.

Cada `Usuario` representa uma loja, com seus próprios produtos, categorias e movimentações.

## Regras

| ID | Regra | Motivo |
|---|---|---|
| RN-01 | **Isolamento por loja.** Toda consulta e alteração é filtrada pelo usuário logado. Registro de outra loja responde "não encontrado". | impede acesso cruzado (IDOR) e o `id` do formulário nunca é confiável |
| RN-02 | **Saldo nunca negativo.** Saída maior que o saldo é recusada e nada é gravado. | contagem falsa é o pior erro de um estoque |
| RN-03 | **Só se altera o saldo por movimentação.** O saldo muda apenas por `EstoqueService.movimentar`, com tipo `ENTRADA`, `SAIDA` ou `AJUSTE`. O formulário de edição do produto não altera `quantidade`. | auditabilidade: todo saldo tem origem registrada |
| RN-04 | **Estoque inicial é uma entrada.** Ao criar produto com quantidade maior que zero, o sistema grava uma movimentação `ENTRADA` com motivo "Estoque inicial". | o saldo inicial também fica na trilha |
| RN-05 | **Valores por tipo.** `ENTRADA` e `SAIDA` exigem quantidade maior que zero. `AJUSTE` informa a contagem final (maior ou igual a zero) e exige motivo preenchido. A quantidade é limitada para não estourar o `int` (máximo 1.000.000 por movimentação e saldo de até 2.000.000.000). | evita overflow e ajuste sem justificativa |
| RN-06 | **Movimentação é imutável e auditável.** Guarda produto, usuário, tipo, quantidade, `quantidadeAnterior`, `quantidadeNova`, observação e data de criação. Não existe edição nem exclusão. Erro se corrige com outra movimentação. | trilha de auditoria confiável |
| RN-07 | **Atomicidade e concorrência.** A movimentação lê o produto com bloqueio pessimista, calcula e grava saldo e movimentação na mesma transação. | evita lost update com dois usuários mexendo ao mesmo tempo |
| RN-08 | **Exclusão de produto.** Produto sem movimentação além do estoque inicial pode ser excluído de vez. Com histórico é **inativado** (`ativo=false`): some das listagens e do dashboard, não aceita movimentação, e o histórico permanece. | preserva a auditoria sem bloquear o usuário |
| RN-09 | **Exclusão de categoria.** Só é permitida se não tiver produtos ativos. Caso contrário a mensagem diz quantos produtos a usam. | evita erro de FK e produto sem categoria |
| RN-10 | **Nomes únicos por loja.** Não pode haver dois produtos ativos nem duas categorias com o mesmo nome (sem diferenciar maiúsculas e sem espaços nas pontas) na mesma loja. | evita duplicidade na contagem |
| RN-11 | **Faixas de valores.** Nome de 2 a 120 caracteres, descrição até 500, observação até 255, preço maior ou igual a zero com 2 casas (até 9.999.999,99), estoque mínimo maior ou igual a zero. | dados limpos e coerentes com as colunas |
| RN-12 | **Nível de estoque tem uma definição só.** `SEM_ESTOQUE` quando saldo = 0; `BAIXO` quando 0 < saldo ≤ estoque mínimo; `NORMAL` acima disso. Dashboard, filtros e alertas usam esse mesmo cálculo. | o diagnóstico achou duas definições diferentes |
| RN-13 | **Valor do estoque** é a soma de preço × quantidade dos produtos ativos, calculada no banco. | consistente e sem carregar tudo em memória |
| RN-14 | **Usuário.** E-mail normalizado (sem espaços, minúsculo) e único, inclusive entre inativos. Senha de 8 a 72 bytes, sempre com BCrypt. Cadastro duplicado responde com mensagem genérica. | segurança e não revelar quais e-mails existem |

## Modelo de dados exigido (migration V2)
Aplicar sobre a V1, sem apagar dados:
- `produto`: nova coluna `ativo BOOLEAN NOT NULL DEFAULT TRUE`; `nome VARCHAR(120)`, `descricao VARCHAR(500)`; `preco NUMERIC(9,2)` com `CHECK (preco >= 0)`; índice único parcial em `(usuario, lower(btrim(nome)))` onde `ativo`; índice em `(usuario, ativo)`.
- `categoria`: `nome VARCHAR(120)`, `descricao VARCHAR(500)`; índice único em `(usuario, lower(btrim(nome)))`.
- `movimentacao_estoque`: novas colunas `quantidade_anterior INTEGER NOT NULL` e `quantidade_nova INTEGER NOT NULL` (preencher linhas existentes com um valor coerente antes do `NOT NULL`); `observacao VARCHAR(255)`; `CHECK (quantidade_anterior >= 0 AND quantidade_nova >= 0)`; `CHECK` de quantidade: `> 0` para `ENTRADA`/`SAIDA` e `>= 0` para `AJUSTE`; índice em `(produto, criado_em DESC)`.
- Os nomes reais das colunas de chave estrangeira são os que a V1 já usa. Conferir na V1 antes de escrever.
