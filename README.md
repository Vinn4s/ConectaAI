# ConectaAI

ConectaAI é um sistema de atendimento automatizado com IA, integrado ao WhatsApp, com backend próprio e interface web para interação e testes.

O projeto foi desenvolvido como uma solução prática para simular o atendimento de uma empresa, permitindo que o usuário envie mensagens, consulte informações e receba respostas geradas a partir de uma API de inteligência artificial.

## Objetivo do projeto

O objetivo do ConectaAI é demonstrar uma arquitetura simples e funcional para atendimento automatizado, conectando:

- Um backend em Spring Boot
- Uma interface web em Next.js
- Um bot de WhatsApp usando Baileys
- Uma API de IA para geração de respostas

Este projeto também serve como estudo prático de integração entre aplicações, consumo de APIs, organização em monorepo e uso seguro de variáveis de ambiente.

## Estrutura do monorepo

```txt
apps/
  backend/    API principal em Spring Boot
  frontend/   Interface web em Next.js
  whatsapp/   Serviço de integração com WhatsApp

Tecnologias utilizadas
Backend
Java 21
Spring Boot
Maven
API Groq/IA
REST API
Frontend
Next.js
React
TypeScript
CSS
WhatsApp
Node.js
Baileys
Axios
QR Code via terminal

Como funciona

O fluxo principal do sistema é:

Usuário → WhatsApp Bot → Backend Spring Boot → API de IA → Backend → WhatsApp

Também é possível interagir pelo frontend:

Usuário → Interface Web → Backend Spring Boot → API de IA → Interface Web

Pré-requisitos

Antes de rodar o projeto, tenha instalado:

Java 21
Node.js
npm
Git

O Maven Wrapper já está incluído em apps/backend, então não é obrigatório ter o Maven instalado globalmente.

Variáveis de ambiente

Cada aplicação possui um arquivo .env.example com as variáveis esperadas, sem credenciais reais:

apps/backend/.env.example
apps/frontend/.env.example
apps/whatsapp/.env.example

Arquivos .env, .env.local ou equivalentes com valores reais não devem ser versionados.

Configuração do backend

Arquivo de exemplo:

SERVER_PORT=8081
GROQ_API_KEY=your_groq_api_key_here

No Windows PowerShell, você pode configurar as variáveis assim:

[Environment]::SetEnvironmentVariable("GROQ_API_KEY", "sua_chave_groq", "User")
[Environment]::SetEnvironmentVariable("SERVER_PORT", "8081", "User")

Depois de configurar, feche e reabra o terminal para as variáveis serem reconhecidas.

Para rodar o backend:

cd apps/backend
./mvnw spring-boot:run

No Windows, se necessário:

cd apps/backend
.\mvnw.cmd spring-boot:run

A API ficará disponível em:

http://localhost:8081

Configuração do frontend

Arquivo de exemplo:

NEXT_PUBLIC_BACKEND_URL=http://localhost:8081

Para rodar:

cd apps/frontend
npm install
npm run dev

O frontend ficará disponível em:

http://localhost:3000

Configuração do WhatsApp

Arquivo de exemplo:

BACKEND_URL=http://localhost:8081

Para rodar:

cd apps/whatsapp
npm install
npm start

Ao iniciar, o terminal exibirá um QR Code. Escaneie o código com o WhatsApp para conectar o bot.

Ordem recomendada para desenvolvimento
Inicie o backend em apps/backend.
Inicie o frontend em apps/frontend.
Inicie o serviço do WhatsApp em apps/whatsapp.
Escaneie o QR Code exibido no terminal.
Teste o fluxo de mensagens.
Segurança

Este projeto utiliza variáveis de ambiente para proteger chaves de API e configurações sensíveis.

O .gitignore da raiz ignora arquivos locais e artefatos gerados, incluindo:

.env, .env.local e variações
node_modules
target
.next
dist e build
auth_info_baileys
logs e arquivos temporários

Nunca envie chaves reais de API, tokens, sessões do WhatsApp ou credenciais para o GitHub.

Status do projeto

Projeto em desenvolvimento.

Funcionalidades atuais:

Backend Spring Boot funcional
Frontend Next.js integrado ao backend
Bot de WhatsApp conectado ao backend
Integração com API de IA
Organização em monorepo
Variáveis de ambiente separadas por aplicação

Melhorias futuras planejadas:

Dashboard administrativo
Histórico de conversas
Autenticação de usuários
Banco de dados
Deploy em ambiente cloud
Melhorias no fluxo de atendimento
Autor

Desenvolvido por Vinnícius Yuri.

Projeto criado como parte dos estudos e desenvolvimento prático em programação fullstack, APIs, automação e inteligência artificial.