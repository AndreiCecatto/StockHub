<div align="center">

# StockHub

**Gestão de estoque para lojas, com histórico imutável de movimentações e alertas de reposição.**

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?logo=postgresql&logoColor=white)
![Testes](https://img.shields.io/badge/testes-953_passando-2EA44F)
![Licença MIT](https://img.shields.io/badge/licença-MIT-blue)

[Funcionalidades](#funcionalidades) · [Como rodar](#como-rodar) · [Arquitetura](#arquitetura) · [Documentação](#documentação)

<img src="docs/img/demo.gif" alt="Demonstração do StockHub" width="880">

</div>

## Sobre

Cada conta do StockHub é uma loja, com seus próprios produtos, categorias e movimentações. A regra central do sistema é simples: **o saldo de um produto só muda por uma movimentação** (entrada, saída ou ajuste). Cada movimentação guarda o saldo de antes e o de depois e não pode ser editada nem apagada, então o histórico funciona como trilha de auditoria.

Projeto acadêmico de Sistemas para Internet, da Universidade Federal de Santa Maria (UFSM).

## Telas

<table>
  <tr>
    <td width="33%"><img src="docs/img/dashboard.png" alt="Visão geral"></td>
    <td width="33%"><img src="docs/img/movimentacoes.png" alt="Movimentações"></td>
    <td width="33%"><img src="docs/img/produtos.png" alt="Produtos"></td>
  </tr>
  <tr>
    <td align="center"><sub><b>Visão geral</b><br>indicadores e reposição</sub></td>
    <td align="center"><sub><b>Movimentações</b><br>lançamento e livro-razão</sub></td>
    <td align="center"><sub><b>Produtos</b><br>busca, filtros e ajuste rápido</sub></td>
  </tr>
</table>

<details>
<summary>Ver mais telas (detalhe, tema escuro, página inicial e celular)</summary>
<br>
<table>
  <tr>
    <td width="50%"><img src="docs/img/produto-peek.png" alt="Painel de detalhe do produto"></td>
    <td width="50%"><img src="docs/img/dashboard-escuro.png" alt="Tema escuro"></td>
  </tr>
  <tr>
    <td align="center"><sub><b>Detalhe sem sair da lista</b></sub></td>
    <td align="center"><sub><b>Tema escuro</b></sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/img/apresentacao.png" alt="Página inicial pública"></td>
    <td width="50%" align="center"><img src="docs/img/dashboard-celular.png" alt="Celular" width="200"> <img src="docs/img/produtos-celular.png" alt="Produtos no celular" width="200"></td>
  </tr>
  <tr>
    <td align="center"><sub><b>Página inicial pública</b></sub></td>
    <td align="center"><sub><b>Versão para celular</b></sub></td>
  </tr>
</table>
</details>

## Funcionalidades

| Área | O que faz |
| :--- | :--- |
| **Contas** | Cadastro e login de lojas; cada loja só enxerga os próprios dados. |
| **Produtos** | Cadastro, edição, busca ao digitar, filtros por categoria e situação, ajuste rápido de saldo com desfazer, painel de detalhe. |
| **Categorias** | Organização por loja; uma categoria em uso não pode ser excluída. |
| **Movimentações** | Entrada, saída e ajuste com observação; livro-razão filtrável por produto, tipo e período. |
| **Painel e relatórios** | Indicadores, produtos a repor, saúde do estoque, gráfico de 14 dias e relatórios de estoque baixo, unidades e valor. |
| **Compras (NF-e)** | Importação do XML da nota de compra, com revisão item a item (vincular, criar ou ignorar), custo com frete rateado, pagamento e parcelas, limites contra abuso e parser seguro. A assinatura e a SEFAZ não são consultadas, e a tela avisa. |
| **Análises** | Dinheiro parado no estoque, o que mais sai, compras e saídas por mês, produtos que vão acabar, parados e curva ABC, com gráficos próprios. |
| **Tipo de loja e equipe** | Cada loja escolhe o tipo (só estoque, comércio, oficina ou misto); a equipe tem papéis (dono, gerente, operador, vendedor, mecânico). |
| **Estorno e reserva** | Desfazer uma movimentação gera o movimento contrário; reservas separam peças sem baixar o saldo. |
| **Ordens de serviço** | Para oficinas: orçamento, aprovação com reserva de peças, conclusão com baixa e impressão. |
| **Exportação** | CSV de produtos (todos, filtrados ou só os marcados) e do histórico. |
| **Usabilidade** | Paleta de comandos (`Ctrl+K`), atalhos de teclado (`?`), tema claro, escuro ou do sistema, navegação por teclado e respeito à preferência de menos movimento. |

### Regras que o sistema garante

- O saldo nunca fica negativo, mesmo com várias pessoas movimentando o mesmo produto ao mesmo tempo (bloqueio pessimista no banco).
- O saldo só muda por movimentação, e a movimentação é imutável.
- Produtos com histórico são inativados em vez de apagados.
- Cada loja acessa apenas os próprios dados, inclusive nos arquivos CSV.

As 90 regras completas estão em [docs/regras-negocio.md](docs/regras-negocio.md).

## Tecnologias

| Camada | Tecnologias |
| :--- | :--- |
| Backend | Java 17, Spring Boot 4.1, Spring MVC, Spring Security, Spring Data JPA, Bean Validation |
| Frontend | JSP e JSTL, Bootstrap 5, JavaScript e CSS próprios, GSAP na página inicial; sem etapa de build e sem CDN |
| Banco | PostgreSQL 14+ com migrations Flyway |
| Qualidade | JUnit 5, Mockito, Spring Security Test (953 testes) |
| Build | Maven, com wrapper incluso |

## Arquitetura

O fluxo é `Controller → Service → Repository → PostgreSQL`:

- **Controller** recebe a requisição e escolhe a resposta, sem acessar repositórios.
- **Service** concentra as regras de negócio e as transações.
- **Repository** faz o acesso a dados com Spring Data JPA.
- **DTOs na entrada, entidades na saída**, para que formulários não preencham campos internos.
- **Flyway manda, Hibernate só valida**: o schema é versionado em migrations, e a aplicação não sobe se o modelo e o banco divergirem.

<details>
<summary>Estrutura de pastas</summary>

```text
src/main/java/org/example/stockhubspring/
  config/ controller/ dto/ exception/ model/ repository/ service/ util/
src/main/resources/
  application.properties            configuração, sem credenciais
  db/migration/                     migrations Flyway
src/main/webapp/
  WEB-INF/pages/ tags/ includes/    telas JSP
  css/ js/ img/ vendor/             arquivos estáticos e bibliotecas
docs/                               arquitetura, regras, design e mercado
```
</details>

## Como rodar

**Requisitos:** JDK 17 ou superior, PostgreSQL 14+ e Git. O Maven já vem no projeto.

**1. Clone e entre na pasta**

```bash
git clone https://github.com/AndreiCecatto/StockHub.git
cd StockHub
```

**2. Crie o banco.** As tabelas são criadas sozinhas na primeira execução.

```sql
CREATE DATABASE stockhub;
```

**3. Informe usuário e senha do banco.** Eles nunca ficam no código. Escolha uma opção:

<details open>
<summary>Opção A: variáveis de ambiente</summary>

```bash
# Linux e macOS
export DB_USERNAME=seu_usuario
export DB_PASSWORD=sua_senha
```

```powershell
# Windows (PowerShell)
$env:DB_USERNAME="seu_usuario"
$env:DB_PASSWORD="sua_senha"
```
</details>

<details>
<summary>Opção B: arquivo local (ignorado pelo Git)</summary>

Crie `src/main/resources/application-local.properties`:

```properties
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```
</details>

**4. Execute**

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Abra http://localhost:8080 e crie uma conta em **Criar conta** (senha com pelo menos 8 caracteres). Se o `JAVA_HOME` apontar para um Java anterior ao 17, aponte-o para um JDK 17 ou superior antes de rodar.

<details>
<summary>Configurações opcionais</summary>

| Variável | Para que serve |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | outro banco ou host (padrão `jdbc:postgresql://localhost:5432/stockhub`) |
| `SERVER_PORT` | outra porta (padrão 8080) |
| `COOKIE_SECURE=true` | obrigatório em produção com HTTPS |

Para gerar o pacote executável: `./mvnw clean package`.
</details>

### Testes

```bash
./mvnw test
```

São 953 testes: regras de serviço (Mockito), controllers (`@WebMvcTest` e Spring Security Test) e integração contra o PostgreSQL configurado acima. Cada teste de integração desfaz o que gravou, então não deixa dados no banco.

## Segurança

Login com Spring Security e BCrypt, proteção CSRF em formulários e chamadas JSON, isolamento por loja em todas as consultas, saída escapada nas telas, cookie de sessão `HttpOnly` e `SameSite`, CSV protegido contra injeção de fórmulas e credenciais fora do código.

## Documentação

| Documento | Conteúdo |
| :--- | :--- |
| [Arquitetura](docs/arquitetura.md) | decisões técnicas e alternativas descartadas |
| [Regras de negócio](docs/regras-negocio.md) | as regras RN-01 a RN-90 |
| [Design e front-end](docs/design-system.md) | identidade visual, componentes, movimento e acessibilidade |
| [Mercado e requisitos](docs/mercado-requisitos.md) | levantamento que orientou os recursos |

## Licença e autor

Distribuído sob a licença MIT (veja [LICENSE](LICENSE)). As bibliotecas do front mantêm as próprias licenças, listadas em [VERSOES.md](src/main/webapp/vendor/VERSOES.md). O GSAP, usado na animação da página inicial, **não** é MIT: segue a [licença Standard "no charge"](https://gsap.com/standard-license), gratuita inclusive para uso comercial, mas que proíbe criar ferramentas visuais de animação concorrentes.

**Andrei Cecatto**, estudante de Sistemas para Internet na UFSM · [@AndreiCecatto](https://github.com/AndreiCecatto)
