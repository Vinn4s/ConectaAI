# ConectaAI

Monorepo com os três apps do ConectaAI:

- `apps/backend`: API Spring Boot
- `apps/frontend`: interface web Next.js
- `apps/whatsapp`: bot WhatsApp com Baileys

## Pré-requisitos

- Java 21
- Maven Wrapper, já incluído em `apps/backend`
- Node.js e npm

## Variáveis de ambiente

Cada app tem um `.env.example` com os nomes esperados, sem credenciais reais:

- `apps/backend/.env.example`
- `apps/frontend/.env.example`
- `apps/whatsapp/.env.example`

Não versione arquivos `.env`, `.env.local` ou equivalentes com valores reais.

### Backend

Variáveis:

```bash
SERVER_PORT=8081
GROQ_API_KEY=your_groq_api_key_here
```

Para rodar localmente:

```bash
cd apps/backend
export SERVER_PORT=8081
export GROQ_API_KEY=sua_chave_groq_local
./mvnw spring-boot:run
```

A API ficará disponível em `http://localhost:8081`.

### Frontend

Variáveis:

```bash
NEXT_PUBLIC_BACKEND_URL=http://localhost:8081
```

Para rodar localmente:

```bash
cd apps/frontend
npm install
cp .env.example .env.local
npm run dev
```

O frontend ficará disponível em `http://localhost:3000`.

### WhatsApp

Variáveis:

```bash
BACKEND_URL=http://localhost:8081
```

Para rodar localmente:

```bash
cd apps/whatsapp
npm install
export BACKEND_URL=http://localhost:8081
npm start
```

Ao iniciar, o app gera um QR Code no terminal para conectar o WhatsApp.

## Ordem recomendada para desenvolvimento

1. Inicie o backend em `apps/backend`.
2. Inicie o frontend em `apps/frontend`.
3. Inicie o WhatsApp em `apps/whatsapp` e escaneie o QR Code.

## Arquivos que não devem ir para o Git

O `.gitignore` da raiz ignora arquivos locais e artefatos gerados, incluindo:

- `.env`, `.env.local` e variações locais
- `node_modules`
- `target`
- `.next`
- `dist` e `build`
- `auth_info_baileys`
- logs e arquivos temporários comuns
