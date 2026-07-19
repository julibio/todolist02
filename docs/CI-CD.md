# CI/CD do Todolist02 — passo a passo

Guia completo do ciclo: **código → GitHub → testes automáticos (CI) → imagens Docker publicadas → deploy no servidor (CD)**.
Tudo que dava para deixar pronto sem o servidor **já está criado no projeto**. As etapas 1 a 3 funcionam hoje; as etapas 4 e 5 ficam prontas para o dia em que o servidor existir.

## Visão geral do fluxo

```
 Sua máquina                GitHub                              Servidor (futuro)
┌─────────────┐   push   ┌──────────────────────────┐  SSH   ┌──────────────────┐
│ git commit  │ ───────► │ CI (ci.yml)              │ ─────► │ docker compose   │
│ git push    │          │  • backend + Postgres    │        │  pull + up -d    │
│ git tag v*  │          │  • build do frontend     │        │                  │
└─────────────┘          │  • build das imagens     │        │  db + backend +  │
                         ├──────────────────────────┤        │  frontend (nginx)│
                         │ Deploy (deploy.yml)      │        └──────────────────┘
                         │  • publica imagens GHCR  │
                         │  • deploy via SSH        │
                         └──────────────────────────┘
```

## Decisões e porquês

| Decisão | Escolha | Por quê |
|---|---|---|
| Plataforma de CI/CD | **GitHub Actions** | Você já tem SSH do GitHub configurado no WSL; grátis para repositório público (e 2.000 min/mês no privado); zero infraestrutura própria |
| Registry de imagens | **GHCR** (GitHub Container Registry) | Integrado ao Actions — publica com o `GITHUB_TOKEN`, sem cadastrar senha de Docker Hub |
| Estratégia de deploy | **VPS/servidor Linux + Docker Compose via SSH** | É exatamente o stack que você já usa no WSL; sem a complexidade de Kubernetes, que não se justifica aqui |
| Gatilho de deploy | **Tag de versão `v*`** (ou manual) | Push na `main` roda só o CI; publicar versão é um ato deliberado (`git tag v1.0.0`) — evita deploy acidental |
| Frontend em produção | **nginx servindo build estático** | O `Dockerfile` atual roda `vite dev` (servidor de desenvolvimento) — inaceitável em produção. O `Dockerfile.prod` compila e serve estático, com proxy de `/api` para o backend |

## O que já foi preparado

| Arquivo | Papel |
|---|---|
| `.gitignore` | Exclui `node_modules`, builds do Android, e o `.env` (segredos nunca vão para o git) |
| `.github/workflows/ci.yml` | CI: valida backend (com Postgres real), frontend e Dockerfiles a cada push/PR |
| `.github/workflows/deploy.yml` | CD: publica imagens no GHCR e (quando ativado) faz deploy via SSH |
| `docker-compose.prod.yml` | Compose de produção: usa imagens do GHCR, segredos via `.env`, banco sem porta exposta |
| `frontend/Dockerfile.prod` + `frontend/nginx.conf` | Build estático + nginx com proxy `/api` e cache correto para PWA |
| `.env.example` | Modelo do `.env` que viverá **no servidor** |
| `backend/src/server.js` → `GET /api/health` | Endpoint de saúde: usado pelo CI, pelo healthcheck do Docker e por monitoramento futuro |
| `backend/package-lock.json` | Gerado — necessário para `npm ci` (instalação reprodutível no CI e no Docker) |
| `backend/Dockerfile` | Atualizado para `npm ci --omit=dev` (build reprodutível, sem dependências de dev) |

O `docker-compose.yml` original continua sendo o ambiente de **desenvolvimento** — nada mudou no seu fluxo local.

---

## Etapa 1 — Versionamento (fazer agora)

CI/CD começa com o código no GitHub. Rode os comandos **no WSL** (onde seu SSH do GitHub já está configurado), um de cada vez:

```bash
cd /mnt/c/Projetos/00-claudecode-todolist02
git init -b main
git add .
git status        # confira: node_modules e builds NÃO devem aparecer
git commit -m "Projeto todolist02 com pipeline CI/CD"
```

Crie o repositório no GitHub (pode ser pelo site — botão **New repository**, sem README/gitignore para não conflitar — ou pela CLI `gh repo create`). Depois:

```bash
git remote add origin git@github.com:SEU_USUARIO/todolist02.git
git push -u origin main
```

**O que acontece:** ao fazer o push, o GitHub detecta os workflows em `.github/workflows/` e o **CI já roda sozinho** — veja na aba **Actions** do repositório.

## Etapa 2 — Entender o CI (já funciona)

O `ci.yml` roda a cada push na `main` e a cada Pull Request, com 3 jobs em paralelo:

1. **Backend (teste de fumaça)** — sobe um Postgres 16 real (igual ao de produção), carrega o `database/init.sql`, instala as dependências com `npm ci`, inicia o servidor e verifica se `GET /api/health` responde `{"status":"ok"}`. Isso prova que servidor + conexão com banco + schema funcionam.
2. **Frontend (build)** — `npm ci` + `vite build`. Se o build passa, o código compila.
3. **Docker (validação)** — constrói as duas imagens de produção sem publicar. Garante que um Dockerfile quebrado não chega à `main`.

**Fluxo recomendado de trabalho:** crie branches para mudanças (`git checkout -b minha-mudanca`), abra Pull Request, espere o CI ficar verde, faça o merge. Para forçar isso, ative em **Settings → Branches → Add branch ruleset**: exigir PR e exigir status checks (os 3 jobs) antes de mergear na `main`.

> O projeto ainda não tem testes automatizados de verdade (unitários/integração). O teste de fumaça já pega muita coisa, mas o slot está pronto: quando criar testes (ex.: `node --test` + supertest), basta adicionar `npm test` como um passo no job do backend.

## Etapa 3 — Publicar imagens no GHCR (já funciona)

O `deploy.yml` dispara quando você cria uma **tag de versão**:

```bash
git tag v1.0.0
git push origin v1.0.0
```

O job `build-push` então:
- constrói `todolist02-backend` e `todolist02-frontend` (este com o `Dockerfile.prod`);
- publica no GHCR com **3 tags cada**: `latest` (sempre a última), o **SHA curto do commit** (imutável — é o que permite rollback exato) e a **versão** (`v1.0.0`);
- as imagens aparecem em `github.com/SEU_USUARIO?tab=packages`.

O job `deploy` (a segunda metade) **não roda ainda** — ele está condicionado à variável `DEPLOY_ENABLED`, que você só vai criar na Etapa 5. Ou seja: pode criar tags à vontade desde já, só publica imagens.

> **Visibilidade das imagens:** por padrão os pacotes GHCR são privados. Privado funciona, mas o servidor precisará de um token para fazer `docker pull` (explicado na Etapa 4, passo 5).

## Etapa 4 — Preparar o servidor (quando ele existir)

Serve qualquer Linux com Docker: VPS (Hetzner, DigitalOcean, Oracle Cloud free tier...) ou uma máquina sua ligada 24/7. Requisitos mínimos p/ este projeto: 1 vCPU, 1–2 GB RAM.

1. **Instalar o Docker Engine** (mesmo procedimento que você fez no WSL):
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER   # sair e entrar de novo para valer
   ```

2. **Criar o diretório da aplicação:**
   ```bash
   sudo mkdir -p /opt/todolist02
   sudo chown $USER:$USER /opt/todolist02
   ```

3. **Criar o `.env` de produção** em `/opt/todolist02/.env`, usando o `.env.example` do repositório como modelo. Gere o segredo do JWT com `openssl rand -hex 32` e use uma senha forte para o banco. **Este arquivo só existe no servidor.**

4. **Criar um par de chaves SSH exclusivo para o deploy** (na sua máquina, no WSL):
   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/todolist02-deploy -C "github-actions-deploy" -N ""
   ssh-copy-id -i ~/.ssh/todolist02-deploy.pub usuario@IP_DO_SERVIDOR
   ```
   A chave **privada** (`~/.ssh/todolist02-deploy`) vai virar um segredo no GitHub na Etapa 5. Usar uma chave dedicada (e não a sua pessoal) permite revogá-la sem afetar mais nada.

5. **Se as imagens GHCR forem privadas** — logar o Docker do servidor no GHCR uma única vez:
   - No GitHub: **Settings (do seu perfil) → Developer settings → Personal access tokens → Tokens (classic) → Generate new token**, só com o escopo `read:packages`.
   - No servidor: `docker login ghcr.io -u SEU_USUARIO` (cole o token como senha — fica salvo).

6. **Firewall do servidor:** liberar as portas **22** (SSH), **80** (frontend) e **3002** (API para o app Android). A porta do Postgres não é exposta.

## Etapa 5 — Ativar o CD (5 minutos, depois da Etapa 4)

No repositório do GitHub, em **Settings → Secrets and variables → Actions**:

1. Aba **Secrets** → New repository secret (4 segredos):

   | Nome | Valor |
   |---|---|
   | `SSH_HOST` | IP ou domínio do servidor |
   | `SSH_USER` | usuário Linux do servidor |
   | `SSH_KEY` | conteúdo da chave **privada** (`cat ~/.ssh/todolist02-deploy`) |
   | `SSH_PORT` | `22` (ou a porta que você configurar) |

2. Aba **Variables** → New repository variable: `DEPLOY_ENABLED` = `true`.
   *(É o interruptor geral: apagar a variável desliga o deploy sem tocar em mais nada.)*

3. (Opcional, recomendado) **Settings → Environments → New environment** chamado `production` → marque **Required reviewers** com você mesmo. O deploy então **pausa e pede sua aprovação** antes de tocar no servidor.

Pronto. A partir daqui, o `deploy.yml` completo funciona: copia `docker-compose.prod.yml` + `database/init.sql` para `/opt/todolist02/` via SCP e roda `docker compose pull && up -d` via SSH.

## Etapa 6 — O ciclo no dia a dia

```bash
git checkout -b nova-funcionalidade   # 1. branch
# ...edita o código...
git commit -am "Descrição da mudança"
git push -u origin nova-funcionalidade # 2. abre PR no GitHub → CI roda
# 3. CI verde → merge na main (CI roda de novo na main)
git checkout main && git pull
git tag v1.1.0                         # 4. decide publicar
git push origin v1.1.0                 # 5. build → GHCR → deploy automático
```

**Rollback** (voltar uma versão): no servidor, edite `/opt/todolist02/.env` e troque `IMAGE_TAG=latest` pela tag anterior (ex.: `IMAGE_TAG=v1.0.0` ou o SHA curto), depois `docker compose -f docker-compose.prod.yml up -d`. Como cada release publica tag imutável, voltar é instantâneo — não precisa rebuildar nada.

**App Android:** o APK aponta para `http://192.168.15.15:3002`. Com o servidor no ar, basta tocar em "Servidor: ..." na tela de login e trocar para `http://IP_DO_SERVIDOR:3002` — ou recompilar o APK com a URL nova como padrão.

## Melhorias futuras (em ordem de prioridade sugerida)

1. **HTTPS + domínio** — colocar um [Caddy](https://caddyserver.com) na frente (container extra no compose): TLS automático via Let's Encrypt com ~5 linhas de config. Necessário para o PWA instalar fora do localhost.
2. **Testes de verdade** — `node --test` + supertest no backend (as rotas de auth e tasks são fáceis de testar); depois plugar `npm test` no CI.
3. **Backup do banco** — cron no servidor: `docker exec todolist02-db pg_dump -U todouser todolist02 | gzip > backup-$(date +%F).sql.gz`.
4. **Migrações de schema** — o `init.sql` só roda com volume vazio; para evoluir o schema em produção use `node-pg-migrate` (as migrações rodam no início do deploy).
5. **CI do APK Android** — job opcional com `gradle assembleDebug` publicando o APK como artefato do workflow.
6. **Monitoramento** — um UptimeRobot (grátis) apontando para `/api/health` já avisa se o site cair.
