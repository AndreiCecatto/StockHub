# StockHub

<p align="center">
  <h1 align="center">StockHub</h1>
  <p align="center">
    <strong>StockHub</strong> é um sistema web para gerenciamento de estoque desenvolvido com Java e Spring Boot.
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL">
</p>

---

##  Sobre o projeto

O **StockHub** é uma aplicação web desenvolvida para gerenciamento de produtos e categorias de um estoque.

O projeto foi desenvolvido durante a graduação em Sistemas para Internet, com o objetivo de aplicar na prática conceitos de desenvolvimento web, programação orientada a objetos, arquitetura em camadas, operações CRUD, persistência de dados e integração com banco de dados.

A aplicação foi estruturada utilizando diferentes camadas para separar as responsabilidades e facilitar a organização e manutenção do código.

---

##  Funcionalidades

### 📦 Produtos
- Cadastro de produtos
- Listagem de produtos
- Edição de produtos
- Exclusão de produtos
- Controle de quantidade em estoque
- Definição de estoque mínimo
- Associação com categorias
- Informações de preço e descrição

### 🏷️ Categorias
- Cadastro de categorias
- Listagem de categorias
- Edição de categorias
- Exclusão de categorias
- Descrição da categoria
- Imagem da categoria

---

##  Tecnologias

| Categoria | Tecnologias |
| :--- | :--- |
| **Linguagem** | Java 17 |
| **Backend** | Spring Boot, Spring Data JPA, Hibernate |
| **Frontend** | JSP, JSTL, HTML, CSS, Bootstrap 5, JavaScript |
| **Banco de Dados** | PostgreSQL, SQL |
| **Build** | Maven |
| **Versionamento** | Git, GitHub |
| **Servidor** | Tomcat embutido (Spring Boot) |

<br>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Data JPA">
  <img src="https://img.shields.io/badge/Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white" alt="Hibernate">
  <img src="https://img.shields.io/badge/JSP-323330?style=flat-square" alt="JSP">
  <img src="https://img.shields.io/badge/HTML5-E34F26?style=flat-square&logo=html5&logoColor=white" alt="HTML5">
  <img src="https://img.shields.io/badge/CSS3-1572B6?style=flat-square&logo=css3&logoColor=white" alt="CSS3">
  <img src="https://img.shields.io/badge/Bootstrap_5-7952B3?style=flat-square&logo=bootstrap&logoColor=white" alt="Bootstrap">
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black" alt="JavaScript">
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white" alt="Git">
</p>

---

##  Arquitetura

O projeto utiliza uma arquitetura baseada na separação de responsabilidades entre diferentes camadas.

**Fluxo da aplicação:**  
`Interface` ➔ `Controller` ➔ `Service` ➔ `Repository` ➔ `PostgreSQL`

- **Controller:** Responsável por receber as requisições da aplicação e controlar o fluxo das operações.
- **Service:** Responsável pela lógica de negócio e pela comunicação entre os Controllers e os Repositories.
- **Repository:** Responsável pelo acesso e persistência dos dados utilizando Spring Data JPA.
- **Model:** Contém as entidades utilizadas pela aplicação e representa os dados persistidos no banco de dados.
- **Config:** Contém configurações utilizadas pela aplicação.

### 📂 Estrutura do projeto

```text
src/
├── main/
│   ├── java/org/example/stockhubspring/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── StockHubSpringApplication.java
│   ├── resources/
│   │   ├── application.properties
│   │   └── application-example.properties
│   └── webapp/
│       ├── css/
│       └── WEB-INF/
│           ├── includes/
│           └── pages/
└── test/java/org/example/stockhubspring/
pom.xml
README.md
```

---

##  Banco de Dados

O **StockHub** utiliza PostgreSQL para armazenamento e persistência dos dados.

### Principais entidades
- **Usuario:** Usuários do sistema
- **Categoria:** Categorias utilizadas para organizar os produtos
- **Produto:** Produtos e informações relacionadas ao estoque

Os produtos possuem relacionamento com suas respectivas categorias.  
O projeto utiliza JPA e Hibernate para realizar o mapeamento entre as entidades Java e o banco de dados.

---

##  Requisitos

| Requisito | Versão |
| :--- | :--- |
| **JDK** | 17 (não use o Java 8/JRE) |
| **Maven** | Opcional (o projeto inclui o wrapper `mvnw`) |
| **PostgreSQL** | 14+ |
| **Git** | Versão atual |

---

## 🔧 Instalação

### 1. Clone o repositório
```bash
git clone https://github.com/AndreiCecatto/StockHub.git
cd StockHub
```

### 2. Configure o JDK 17
Se a variável `JAVA_HOME` estiver vazia, aponte para um JDK 17. Exemplo usando o JDK embutido do IntelliJ (PowerShell):

```powershell
$env:JAVA_HOME="C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"
```

### 3. Configure o banco de dados
Crie um banco PostgreSQL para o projeto:

```sql
CREATE DATABASE stockhub;
```

As credenciais **nunca** ficam no `application.properties`. Informe-as de uma das formas abaixo:

**Opção A: variáveis de ambiente** (PowerShell)
```powershell
$env:DB_USERNAME="seu_usuario"
$env:DB_PASSWORD="sua_senha"
```

**Opção B: arquivo `application-local.properties`**
Copie `src/main/resources/application-example.properties` para `src/main/resources/application-local.properties` e preencha usuário e senha reais (`spring.datasource.username` e `spring.datasource.password`). Esse arquivo é carregado automaticamente (`spring.config.import`) e está no `.gitignore`, portanto **não é versionado**.

### 4. Execute a aplicação
O projeto roda com o Tomcat embutido do Spring Boot (não é necessário WildFly nem outro servidor):

```bash
./mvnw spring-boot:run
```

No Windows:
```cmd
mvnw.cmd spring-boot:run
```

### 5. Acesse a aplicação
```text
http://localhost:8080
```

### Testes e build
```bash
./mvnw test
./mvnw clean package
```
---

##  Conceitos aplicados

Durante o desenvolvimento do StockHub foram aplicados conceitos de:

- Programação Orientada a Objetos
- Arquitetura MVC e Arquitetura em camadas
- Operações CRUD
- Controllers, Services e Repositories
- JPA, Hibernate e Persistência de dados
- Relacionamento entre entidades
- Integração com PostgreSQL
- Desenvolvimento de interfaces com JSP
- Desenvolvimento web com Spring Boot
- Controle de versão com Git

---

##  Objetivo

O StockHub foi desenvolvido para colocar em prática conhecimentos adquiridos durante a graduação em Sistemas para Internet na Universidade Federal de Santa Maria (UFSM), principalmente nas áreas de:

- Desenvolvimento de software
- Desenvolvimento web
- Programação Orientada a Objetos
- Banco de dados e persistência
- Arquitetura de aplicações

---

## 📊 Status

<p align="center">
  <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-yellow?style=for-the-badge" alt="Status">
</p>

O projeto encontra-se em desenvolvimento e pode receber novas funcionalidades, melhorias de interface e aprimoramentos na arquitetura.

---

## 👤 Autor

**Andrei Cecatto**  
Estudante de Sistemas para Internet na Universidade Federal de Santa Maria (UFSM).

<p align="center">
  <a href="https://github.com/AndreiCecatto">
    <img src="https://img.shields.io/badge/GitHub-AndreiCecatto-181717?style=flat-square&logo=github&logoColor=white" alt="GitHub">
  </a>
</p>

<p align="center">
  Desenvolvido por <strong>Andrei Cecatto</strong>
</p>
