# Arquitetura do StockHub

Cada decisão tem o motivo e as alternativas descartadas. Quando uma decisão mudar, atualize este arquivo no mesmo commit.

## AD-01 — Spring Boot 4.1.2 e Java 17
- **Decisão:** parent `spring-boot-starter-parent` 4.1.2, Java 17 como mínimo.
- **Motivo:** 4.1.2 é o patch estável mais recente da linha 4.1 no Maven Central. A documentação oficial (requisitos de sistema) diz que exige Java 17 no mínimo e roda até o Java 27.
- **Descartadas:** 4.2.0-RC1 (candidata, não estável) e 3.5.x (a linha 3 sairá de suporte antes da 4).

## AD-02 — Monólito em camadas, pacote por camada
- **Decisão:** `controller` → `service` → `repository` → banco; `model` (entidades), `dto`, `exception`, `config`. O controller nunca acessa repository.
- **Motivo:** o domínio é pequeno (Produto, Categoria, Usuario, MovimentacaoEstoque). As camadas já existem, a equipe as conhece, e a regra "regra de negócio só no service" fica fácil de revisar.
- **Descartadas:** pacote por funcionalidade e arquitetura hexagonal (cerimônia demais para 4 entidades); API REST com SPA (o requisito do projeto é JSP).

## AD-03 — Onde cada responsabilidade fica
| Responsabilidade | Onde | Observação |
|---|---|---|
| Formato da entrada (obrigatório, tamanho, faixa) | Bean Validation nos DTOs (`@Valid` + `BindingResult` no controller) | o formulário volta com erros por campo |
| Regras de negócio e consistência | `service` (`@Transactional`) | ver `regras-negocio.md` |
| Erros de negócio | `exception` (`RegraNegocioException`, `RecursoNaoEncontradoException`) + `@ControllerAdvice` | nunca deixar virar 500 com stack trace |
| Segurança | `config/SecurityConfig` (Spring Security, BCrypt, CSRF, logout por POST) | escopo por usuário no service |
| Integridade final | restrições e índices do banco (Flyway) | última defesa, não a única |

## AD-04 — DTOs só na entrada; entidades na saída
- **Decisão:** formulários usam DTOs (`ProdutoForm`, `CategoriaForm`, `CadastroForm`, `MovimentacaoForm`). As JSPs de leitura continuam recebendo entidades.
- **Motivo:** o diagnóstico mostrou que usar a entidade como formulário permite mass assignment (o `id` e o dono vindos do POST). Na saída, a entidade é só leitura e criar DTO de resposta para cada tela só duplicaria código.
- **Descartadas:** DTO para tudo e MapStruct (peso desnecessário); entidade como formulário (inseguro).
- **Compatibilidade:** os DTOs mantêm os nomes de campo e o atributo de model usados hoje nas JSPs.

## AD-05 — Banco: Flyway manda, Hibernate só valida
- **Decisão:** `spring.jpa.hibernate.ddl-auto=validate`; todo ajuste de schema é uma migration `V<n>__descricao.sql` em `db/migration`. Migration aplicada nunca é editada, só se cria a próxima.
- **Motivo:** schema reproduzível e versionado, e o `validate` falha na subida se entidade e banco divergirem.
- **Descartadas:** `ddl-auto=update` (muda o schema sem rastro).

## AD-06 — Concorrência: bloqueio pessimista na movimentação
- **Decisão:** `EstoqueService.movimentar` carrega o produto com `PESSIMISTIC_WRITE` dentro da transação.
- **Motivo:** o diagnóstico apontou lost update. Num estoque a contagem tem de estar certa, e o bloqueio por linha é simples e correto para este volume.
- **Descartadas:** `@Version` com retry (mais código no controller para ganho de desempenho que o projeto não precisa).

## AD-07 — Exclusão lógica de produto
- **Decisão:** produto com histórico é inativado (`ativo=false`), nunca apagado. Detalhes em `regras-negocio.md` (RN-08).
- **Motivo:** o histórico de movimentações é a trilha de auditoria; apagar o produto destruiria ou bloquearia isso.

## AD-08 — Testes
- **Decisão:** testes unitários de service com Mockito, `@WebMvcTest` com `spring-security-test` nos controllers, e um teste de integração com o banco local para o que depende de SQL.
- **Motivo:** as regras de estoque estão no service, então é lá que a cobertura vale mais.

## AD-09 — Busca tolerante a erro de digitação feita no código, sem extensão do banco
- **Decisão:** `GET /api/busca` devolve primeiro os produtos que contêm o texto (como antes). Só quando nenhum contém, completa com os de nome parecido, marcados com `aproximado: true`. A regra é `util/BuscaDifusa` (distância de edição com inversão de letras, por palavra, ignorando caixa e acento). O seletor de produto da tela de movimentações, que já recebe a lista inteira, usa uma cópia da regra em `js/busca-difusa.js`.
- **Motivo:** com troca de letras ("cenata" por "caneta") o `pg_trgm` do PostgreSQL erra, e usá-lo exigiria ativar uma extensão no banco de quem clonar o projeto. O código é puro, fácil de testar e portátil.
- **Limites:** o servidor examina no máximo 5.000 nomes por consulta (teto de segurança). Se algum catálogo passar disso, o próximo passo é um índice `pg_trgm` como pré-filtro. Misturar exatos com parecidos foi descartado: "cane" passava a achar "caderno" e só atrapalhava.
- **Não usa IA externa:** nenhum dado do estoque sai do servidor.

## AD-10 — Importação de NF-e: XML seguro, revisão antes de gravar, transação única
- **Decisão:** o fluxo é *enviar, revisar, confirmar*. O envio valida o XML e o guarda em `importacao_nfe` (PENDENTE, 30 min); a revisão calcula sugestões a partir desse XML; a confirmação o **relê**, revalida e grava nota, fornecedor, produtos novos e ENTRADAs numa única `@Transactional` (RN-15 a RN-31). Código em `nfe` (parser e modelo da nota, sem Spring), `service/ImportacaoNfeService`, `controller/NfeController`, DTOs `ConfirmacaoNfeForm`, `DecisaoItemNfeForm` e `RevisaoNfe`.
- **Parser seguro:** DOM com `disallow-doctype-decl=true` (qualquer DOCTYPE é recusado, então não existe entidade externa nem recursiva: XXE e "billion laughs" não acontecem), `FEATURE_SECURE_PROCESSING`, entidades externas e DTD externo desligados, sem XInclude, `ACCESS_EXTERNAL_DTD/SCHEMA=""`, entrada por `Reader` (nunca resolve URL), sem XSLT. Tamanho conferido em bytes antes de interpretar; UTF-8 estrito; números por `BigDecimal` com formato e faixa; textos sem caracteres de controle e truncados; erros genéricos sem eco do arquivo.
- **Upload e CSRF:** o CSRF continua ligado e o token **não vai na URL** (vazaria em log e histórico). O front envia o arquivo por `fetch` com `FormData` e o cabeçalho `X-CSRF-TOKEN`, mais `X-Requested-With: XMLHttpRequest` e `Accept: application/json` (resposta JSON); sem o token o POST é 403. Existe também um caminho para formulário comum (redireciona com flash), mas ele só funciona se o container ler o `_csrf` do corpo multipart; o caminho suportado é o `fetch`.
- **Abuso:** 20 envios por loja a cada 10 minutos (`NfeLimitador`, janela deslizante em memória: zera ao reiniciar e não vale entre várias instâncias; para escalar, mover para o banco) e no máximo 5 pendentes por loja.
- **Não valida assinatura nem consulta a SEFAZ** (exige certificado digital A1/A3): a revisão sempre mostra esse aviso e o dono confirma o destinatário. Consulta à SEFAZ fica como evolução possível.
- **Base genérica:** `parceiro` (fornecedor/cliente) e `movimentacao_estoque.origem_tipo/origem_id` servem às futuras vendas e OS; basta uma migration que amplie o CHECK de origem. `produto` ganhou custo, unidade, EAN e NCM.
- **Descartadas:** confiar nos dados reenviados pelo navegador na confirmação (adulterável); gravar a nota já no envio (itens entrariam sem revisão); StAX/SAX (o DOM com 2 MB e 990 itens é suficiente e mais simples de validar); conversão automática de unidade e rateio de frete nesta versão (regra de negócio ainda não decidida).

### AD-10 (revisão após auditoria de segurança)
- **Revisão barata:** consultas em lote (EAN e memória do fornecedor), orçamento de tempo dentro do laço de nomes e limites de uso (loja, IP, global e 3 leituras simultâneas) também em revisar e confirmar. Guardar o resultado da revisão no envio foi descartado (ocuparia mais espaço no banco e exigiria invalidar com mudanças no catálogo); o limite mais o lote bastam para o volume.
- **XML efêmero:** a coluna `xml` passou a aceitar NULL (V5); encerrada a importação, o XML é zerado. O SHA-256 gravado no envio é conferido a cada leitura.
- **Confirmação em dois passos:** planejar (validar tudo) e gravar; produtos travados em ordem crescente de id. A nota ganhou a UNIQUE de negócio (loja, fornecedor, modelo, série, número).
- **Descartado:** reescrever o parceiro existente com os dados do XML (um arquivo externo não pode alterar cadastro); aceitar campos de formato fixo truncados.
- **Limite conhecido:** os contadores de uso são em memória (não valem entre instâncias); o IP vem de `remoteAddr`.

## AD-11 — Tipo de loja como interruptor de módulos (menu), lido do banco a cada página
- **Decisão:** `tipo_loja` e `subtipo` ficam em `usuario` (V6, padrão ESTOQUE) e só controlam a visibilidade dos módulos (enum `Modulo`: ESTOQUE, VENDAS, OS). O `GlobalModelAdvice` publica `tipoLoja` e o mapa `modulos` em toda requisição, lendo só o tipo do banco (`findTipoLojaByCodigo`), não da sessão, para a troca valer na hora. Os JSPs usam `<sh:modulo nome="VENDAS">` (tags/modulo.tag). A combinação tipo/subtipo é validada no service (RN-44).
- **Descartado:** guardar o tipo no `UsuarioPrincipal` (ficaria desatualizado até novo login); CHECK de combinação no banco (a lista muda com o produto, e a regra mora no service); tela de Configurações (ainda não existe: por ora só o endpoint `POST /conta/tipo-loja`).

## AD-12 — Análises de estoque: agregar no banco, desenhar a partir da tabela
- **Decisão:** `AnaliseController` (`GET /analises`) chama `AnaliseEstoqueService.montar(loja, periodo)`, que devolve um `PainelAnalises` (DTOs de saída em `dto`). Somas, contagens e agrupamentos ficam em consultas JPQL/nativas de `ProdutoRepository` e `MovimentacaoEstoqueRepository`, todas com `usuario_codigo`; o service só completa dias/meses vazios, calcula cobertura e curva ABC e recorta listas (RN-45 a RN-52). O valor de saída e o "caixa" usam o `custo_unitario` gravado em cada movimentação (V7), por isso `EstoqueService.movimentar` ganhou uma sobrecarga com custo.
- **Gráficos:** SVG próprio em `js/analises.js`, sem biblioteca. Cada gráfico é desenhado a partir de uma `<table>` da própria JSP (`data-tabela`, números crus em `data-v`): a tabela é a alternativa para leitor de tela e para quem está sem JavaScript, e os textos entram por `textContent`. Cores em tokens `--an-*` (`css/analises.css`), validados com o validador da skill dataviz nos dois temas; alerta sempre com ícone e texto.
- **Descartado:** biblioteca de gráficos (peso e tema próprio); entregar JSON e desenhar só no cliente (sem alternativa sem JS e dois formatos de dado); eixo duplo na curva ABC (trocada por uma barra única dividida em A, B e C); histórico do valor do estoque no tempo (exigiria guardar fotos diárias do saldo; fica para depois).
- **Limite conhecido:** a cobertura e os parados carregam todos os produtos candidatos da loja em memória antes de recortar (catálogo pequeno); se crescer muito, mover o recorte para a consulta.
