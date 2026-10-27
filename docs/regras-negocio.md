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
| RN-09 | **Exclusão de categoria.** Só é permitida se **nenhum** produto a usar, ativo ou inativado. Com produtos ativos a mensagem diz quantos a usam; se só restarem inativados, diz que eles são mantidos para preservar o histórico. | evita erro de FK e produto sem categoria || RN-10 | **Nomes únicos por loja.** Não pode haver dois produtos ativos nem duas categorias com o mesmo nome (sem diferenciar maiúsculas e sem espaços nas pontas) na mesma loja. | evita duplicidade na contagem |
| RN-11 | **Faixas de valores.** Nome de 2 a 120 caracteres, descrição até 500, observação até 255, preço maior ou igual a zero com 2 casas (até 9.999.999,99), estoque mínimo maior ou igual a zero. | dados limpos e coerentes com as colunas |
| RN-12 | **Nível de estoque tem uma definição só.** `SEM_ESTOQUE` quando saldo = 0; `BAIXO` quando 0 < saldo ≤ estoque mínimo; `NORMAL` acima disso. Dashboard, filtros e alertas usam esse mesmo cálculo. | o diagnóstico achou duas definições diferentes |
| RN-13 | **Valor do estoque** é a soma de preço × quantidade dos produtos ativos, calculada no banco. | consistente e sem carregar tudo em memória |
| RN-14 | **Usuário.** E-mail normalizado (sem espaços, minúsculo) e único, inclusive entre inativos. Senha de 8 a 72 bytes, sempre com BCrypt. Cadastro duplicado responde com mensagem genérica. | segurança e não revelar quais e-mails existem |

> Nota RN-09: o produto inativado (RN-08) continua apontando para a categoria (chave estrangeira obrigatória), então ele também impede a exclusão. Para liberar a categoria é preciso mover os produtos ativos para outra categoria; os inativados mantêm a categoria original por serem histórico.

> Nota RN-04/RN-08: o motivo "Estoque inicial" (sem diferenciar maiúsculas e espaços) é reservado ao sistema. A movimentação manual com esse motivo é recusada; só o cadastro de produto o grava (`EstoqueService.registrarEstoqueInicial`). No cadastro o estoque inicial vai de 0 a 1.000.000; na edição a quantidade é ignorada.

## Modelo de dados exigido (migration V2)
Aplicar sobre a V1, sem apagar dados:
- `produto`: nova coluna `ativo BOOLEAN NOT NULL DEFAULT TRUE`; `nome VARCHAR(120)`, `descricao VARCHAR(500)`; `preco NUMERIC(9,2)` com `CHECK (preco >= 0)`; índice único parcial em `(usuario, lower(btrim(nome)))` onde `ativo`; índice em `(usuario, ativo)`.
- `categoria`: `nome VARCHAR(120)`, `descricao VARCHAR(500)`; índice único em `(usuario, lower(btrim(nome)))`.
- `movimentacao_estoque`: novas colunas `quantidade_anterior INTEGER NOT NULL` e `quantidade_nova INTEGER NOT NULL` (preencher linhas existentes com um valor coerente antes do `NOT NULL`); `observacao VARCHAR(255)`; `CHECK (quantidade_anterior >= 0 AND quantidade_nova >= 0)`; `CHECK` de quantidade: `> 0` para `ENTRADA`/`SAIDA` e `>= 0` para `AJUSTE`; índice em `(produto, criado_em DESC)`.
- Os nomes reais das colunas de chave estrangeira são os que a V1 já usa. Conferir na V1 antes de escrever.

## Importação de NF-e de compra (RN-15 em diante)

A loja envia o XML de uma NF-e de compra, revisa, e só depois de confirmar os itens entram no estoque. Decisão de arquitetura: AD-10.

| ID | Regra | Motivo |
|---|---|---|
| RN-15 | **Só NF-e de compra.** Aceita apenas NF-e **modelo 55**, **layout 4.00**, raiz `nfeProc` ou `NFe`, `finNFe=1` (normal) e `tpNF=1` (saída do emitente = compra para a loja). NFC-e (65), devolução, complementar, ajuste e `tpNF=0` são recusadas com mensagem em português. | só a nota normal de saída do fornecedor representa uma compra |
| RN-16 | **Chave de acesso.** 44 dígitos, dígito verificador (módulo 11) correto, igual ao `Id` (`"NFe"+chave`) e, se houver `protNFe/infProt/chNFe`, igual a ele; o modelo e o CNPJ do emitente embutidos na chave também precisam bater com o corpo da nota. | detecta arquivo adulterado ou montado errado |
| RN-17 | **Idempotência.** A mesma chave nunca entra duas vezes na mesma loja ("esta nota já foi importada em dd/MM/aaaa HH:mm"), no envio, na revisão e na confirmação. O banco garante com `UNIQUE (usuario_codigo, chave_acesso)` (corrida entre duas confirmações vira a mesma mensagem). A mesma chave pode existir em lojas diferentes. | evita dar entrada em dobro |
| RN-18 | **Sem verificação de autenticidade.** A assinatura digital **não** é validada e a SEFAZ **não** é consultada (exigiria certificado digital). A revisão mostra sempre esse aviso (`avisoVerificacao`), indica se o arquivo traz assinatura/protocolo, e o sistema nunca diz que a nota é "autêntica". | honestidade sobre o que foi conferido |
| RN-19 | **Destinatário.** A revisão sempre mostra o destinatário e exige que o dono marque a confirmação "esta nota é minha" (`confirmouDestinatario`). Se `usuario.cnpj` estiver preenchido e diferir do `dest/CNPJ`, há aviso (não bloqueia). `usuario.cnpj` é opcional, só dígitos, com dígito verificador válido. | o dono é a última barreira contra nota de outra empresa |
| RN-20 | **CFOP.** Guarda-se o CFOP original e o de entrada derivado (5xxx para 1xxx, 6xxx para 2xxx, 7xxx para 3xxx, mesmos 3 últimos dígitos). CFOPs que normalmente não são mercadoria para revenda (5910, 5949, 5915, 5916, 5920, 5921, 5927, e os mesmos sufixos em 6/7) geram aviso; o dono decide. | informação fiscal preservada sem decidir pelo dono |
| RN-21 | **Quantidade.** O saldo do sistema é inteiro. `qCom` com parte decimal (ex.: 2,5) vira situação `FRACIONADO`: **não** entra no estoque e a revisão avisa para lançar manualmente (12.0000 é inteiro e vale 12). Quantidade menor ou igual a zero ou acima de 1.000.000 (limite de ENTRADA, RN-05) deixa o item **recusado** (só pode ser ignorado). | respeita RN-05 e a coluna inteira |
| RN-22 | **Unidade.** Se `uCom` for diferente da unidade do produto vinculado há aviso e **não há conversão automática**; nesse caso o custo do produto **não** é atualizado (custo por caixa não pode virar custo por unidade). Fator de conversão fica para depois. | evita custo errado |
| RN-23 | **Custo.** Custo unitário = (`vProd` menos `vDesc` do item) / `qCom`, 4 casas, HALF_UP. **Limite documentado:** não há rateio de frete, seguro, outras despesas nem impostos. Ao importar, `produto.custo` recebe o último custo; `ean` e `ncm` do produto só são preenchidos se estiverem vazios. O preço de venda continua em `produto.preco`. | custo simples e previsível nesta versão |
| RN-24 | **Vínculo item para produto.** O serviço sugere (o dono pode trocar): 1º por EAN igual a `produto.ean` de produto ativo da loja; 2º pela memória do fornecedor (mesmo parceiro e mesmo `cProd` em nota anterior com produto ainda ativo); 3º pelo nome (`BuscaDifusa.pontuar` >= 0,8, nos dois sentidos, até 5.000 nomes ativos e no máximo 3 s por revisão; passou disso o restante vira "novo produto"); senão sugere criar. Cada item recebe, na confirmação, uma decisão: `VINCULAR(produtoId)`, `CRIAR(nome, categoriaId, preco)` ou `IGNORAR`. | acelera a conferência sem tirar o controle do dono |
| RN-25 | **Produto novo.** Nasce com saldo 0 e depois recebe a ENTRADA (o livro-razão fecha: 0 para N). Respeita RN-10 (nome único entre ativos) e RN-11, é da loja da sessão, o preço vem do dono (maior que zero, até 9.999.999,99, 2 casas; a tela sugere o custo) e a categoria precisa ser da loja. `produtoId` ou `categoriaId` de outra loja (ou inexistente) recebem a mesma mensagem de "não encontrado". | dono=loja e nada de vazar existência |
| RN-26 | **Movimentação da nota.** Cada item importado gera uma ENTRADA por `EstoqueService.movimentar` com `origem_tipo=NFE_COMPRA` e `origem_id` = id da nota; observação "NF-e (número) de (fornecedor)", cortada em 255. O motivo reservado "Estoque inicial" não é usado. | rastreabilidade (RN-06) |
| RN-27 | **Tudo ou nada.** A confirmação roda numa **única transação**: se qualquer item falhar (ex.: saldo passaria de 2.000.000.000), nada é gravado e a mensagem diz qual item. Se nenhum item entrar no estoque, a confirmação é recusada (senão a chave ficaria "gasta" sem entrada). | consistência |
| RN-28 | **Fornecedor (parceiro).** O emitente vira `parceiro` da loja (único por loja e documento, `eh_fornecedor=true`); nome, fantasia e UF só entram na criação (ver RN-38). `parceiro` é genérico: cliente entra no futuro. | base para vendas/OS |
| RN-29 | **A confirmação usa o XML guardado.** `importacao_nfe` guarda o XML (até 2 MB, zerado ao encerrar: RN-32); a confirmação o relê, confere o SHA-256 (RN-33) e revalida, e **nunca** usa dados da nota reenviados pelo navegador, só as decisões por item (`acao`, `produtoId`, `nome`, `categoriaId`, `preco`), validadas campo a campo. | o navegador não é confiável |
| RN-30 | **Pendências.** A importação fica `PENDENTE` por 30 minutos e depois expira; no máximo 5 pendentes por loja (a mais antiga é cancelada ao passar disso); `CONFIRMADA` e `CANCELADA` são finais. O token é um UUID aleatório que só vale para a loja que o criou (outro token responde 404). | limita o que fica guardado |
| RN-31 | **Limites de entrada e abuso.** Arquivo de até 2 MB (conferido em bytes antes de ler o conteúdo), só UTF-8, até 990 itens, limites de uso com HTTP 429 (RN-34). O XML nunca é exibido nem registrado em log, e as mensagens de erro do parser são genéricas ("o arquivo enviado não é uma NF-e válida"). | proteção contra abuso e vazamento |

> Limites conhecidos: sem conversão de unidade, sem rateio de frete/impostos, sem cancelamento/estorno da nota importada (se errou, corrija com movimentações de ajuste), item fracionado só por lançamento manual, notas sem nenhum item inteiro não podem ser confirmadas.

## Modelo de dados (migrations V3 e V4)
- **V3:** `produto` ganha `custo NUMERIC(14,4)`, `unidade VARCHAR(6)`, `ean` (8/12/13/14 dígitos), `ncm` (8 dígitos); nova tabela `parceiro`; `movimentacao_estoque` ganha `origem_tipo`/`origem_id` (os dois juntos ou nenhum; hoje só `NFE_COMPRA`, VENDA e OS entram por nova migration); `usuario` ganha `cnpj`. Só acrescenta.
- **V4:** `nota_fiscal` (UNIQUE loja+chave), `nota_fiscal_item` e `importacao_nfe`. Só acrescenta.

### Endurecimento da importação (correções da auditoria de segurança, migration V5)

| ID | Regra | Motivo |
|---|---|---|
| RN-32 | **O XML não fica guardado para sempre.** `importacao_nfe.xml` aceita NULL e é zerado quando a importação é confirmada, cancelada ou vence (a limpeza roda a cada envio; nenhuma linha é apagada). Enviar de novo o mesmo arquivo enquanto há pendente válida igual (mesma chave e mesmo SHA-256) devolve a mesma importação; mesma chave com conteúdo diferente cancela a antiga. | menos dado sensível parado e sem cópias repetidas |
| RN-33 | **Integridade do XML guardado.** Na revisão e na confirmação o SHA-256 do XML guardado é conferido com o gravado no envio; diferente (ou ausente) recusa a operação. | detecta alteração do que foi guardado |
| RN-34 | **Limites de uso (429).** Envio: 20 por loja a cada 10 min. Revisão e confirmação (reprocessam o XML): 60 por loja a cada 10 min. Por IP: 120 chamadas a cada 10 min. Global: 600 a cada 10 min. No máximo 3 leituras de XML ao mesmo tempo. Contadores de quem parou de chamar saem do mapa. O IP é o `remoteAddr` da conexão (atrás de proxy reverso é preciso configurar o repasse do IP real). | DoS por reprocessamento do XML de 2 MB |
| RN-35 | **Consultas em lote na revisão.** Os EANs e a memória do fornecedor são buscados em UMA consulta cada; o tempo de 3 s da comparação por nome é conferido dentro do próprio laço de nomes. | revisão de nota grande não vira centenas de consultas |
| RN-36 | **Parser mais fechado.** Aninhamento máximo de 32 níveis (`jdk.xml.maxElementDepth`), no máximo 100.000 elementos, `StackOverflowError` vira a mensagem genérica. Campos de formato fixo (`chNFe`, CFOP, CNPJ/CPF, NCM, UF, série) são lidos sem truncar e recusados se o tamanho não for o esperado; `uCom` é posto em maiúsculas antes de qualquer corte e recusado se passar de 6 caracteres. | evita aceitar "valor cortado" |
| RN-37 | **Chave x corpo, e unicidade de negócio.** Além do modelo e do CNPJ (RN-16), UF (`cUF`), série e número (`nNF`) da chave precisam bater com `ide`. O banco tem `UNIQUE (usuario_codigo, parceiro_id, modelo, serie, numero)` em `nota_fiscal`: a mesma nota do mesmo fornecedor não entra duas vezes mesmo com outra chave. | idempotência também por número da nota |
| RN-38 | **Emitente.** O CNPJ/CPF do emitente precisa ter dígito verificador válido. Um XML **nunca** reescreve o cadastro de um parceiro já existente (nome, fantasia e UF só são gravados na criação). | sem sobrescrita de cadastro por arquivo externo |
| RN-39 | **Confirmação em dois passos.** Primeiro valida TODAS as decisões (produto, categoria, nome, preço, nome repetido na própria nota); só depois grava. Os produtos que recebem entrada são travados em ordem crescente de id (sem deadlock entre duas confirmações). Falha de bloqueio (`PessimisticLockingFailureException`) responde "tente de novo", sem erro técnico. | nada pela metade e sem deadlock |
| RN-40 | **Datas.** `dhEmi` mais de 1 dia no futuro é recusada; nota com mais de 12 meses gera aviso. | nota futura é adulterada; nota velha pode já estar lançada |
| RN-41 | **Avisos de gravação.** A revisão avisa que o custo é atualizado e que EAN/NCM serão gravados nos produtos que ainda não os têm (por item, no produto sugerido). Se a loja tem CNPJ e o destinatário diverge, há aviso forte, mas a confirmação continua sendo a mesma caixa `confirmouDestinatario`, que é sempre obrigatória. | o dono vê o que será alterado |
| RN-42 | **Log.** A chave aparece nos logs só pelos 8 últimos dígitos; nunca o XML. | menos dado fiscal em log |
