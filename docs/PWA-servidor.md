# PWA no servidor (precisa domínio + HTTPS)

## Resumo

### DuckDNS (subdomínio gratuito)
- Entrei com a conta GitHub em www.duckdns.org
- Criei o subdomínio gratuito: julibio-todo.duckdns.org
- Apontei o "current ip" para o IP DO VPS: 147.15.78.254
  (atenção: o site preenche com o IP de quem acessa — trocar manualmente!)
- Conferência: `nslookup julibio-todo.duckdns.org` deve responder 147.15.78.254
- A URL https://julibio-todo.duckdns.org só funcionará após o deploy v0.2.0
  + Caddy no ar (ele emite o certificado no primeiro acesso)

### Redes Docker (explícitas nos composes)
- todolist02_dev — interna do ambiente local (db, backend, frontend se falam por hostname)
- todolist02_internal — interna de produção no VPS (mesmo papel)
- web — compartilhada do VPS, criada à mão (sudo docker network create web);
  só o frontend do todolist entra; é onde o Caddy encontra os apps
- Regra: banco e backend NUNCA entram na web — só o container de borda de cada app
- Ver na prática: sudo docker network ls | sudo docker network inspect web

### Caddy (proxy reverso com HTTPS automático)
- Roda SÓ no VPS, como infraestrutura central: /opt/caddy (compose próprio, fora do repositório)
- É o único dono das portas 80/443; encaminha cada domínio para o container certo (rede Docker "web")
- Emite e renova sozinho os certificados Let's Encrypt — zero manutenção
- Setup (uma única vez):
  - `sudo docker network create web` — rede compartilhada entre Caddy e os apps
  - /opt/caddy/Caddyfile — o roteamento: `julibio-todo.duckdns.org { reverse_proxy todolist02-frontend:80 }`
  - /opt/caddy/docker-compose.yml — container caddy:2-alpine nas portas 80/443, certificados em volume
  - `sudo docker compose up -d` (só APÓS o deploy v0.2.0 — antes disso o frontend antigo ainda ocupa a porta 80)
- Fluxo: internet → Caddy (443, TLS) → nginx do frontend → /api → backend → Postgres
- Cada app do repositório NÃO publica porta; apenas entra na rede "web"
- App novo no futuro = compose na rede web + 3 linhas no Caddyfile
  + `sudo docker exec caddy caddy reload --config /etc/caddy/Caddyfile`
- Primeiro acesso ao domínio demora ~30 s (emissão do certificado); depois é instantâneo
- Local (dev): Caddy não existe — localhost já é contexto seguro para o PWA

### Alterações no projeto desde a v0.1.2 (candidatas à v0.2.0)

#### Feitas por mim (infra HTTPS/PWA — nenhuma linha de código de app)
- docker-compose.prod.yml (17 linhas)
  - frontend: REMOVIDO o "ports 80:80" (deixa de falar com a internet)
  - frontend: ADICIONADO networks default + web (Caddy o alcança pela web)
  - seção networks nova: default nomeada "todolist02_internal" (explícita)
    e "web" declarada como external (criada à mão no VPS)
- docker-compose.yml — dev (6 linhas)
  - seção networks nova: default nomeada "todolist02_dev" (só transparência;
    comportamento idêntico)
- .env.example (1 linha)
  - comentário: domínio/HTTPS é responsabilidade do Caddy central (/opt/caddy)
- deploy.yml — SEM mudança líquida (o Caddyfile entrou e saiu do scp na refatoração)
- Caddyfile na raiz — criado e apagado (migrou como conceito para /opt/caddy no VPS)

#### Suas (documentação e anotações)
- docs/SemVer.md e docs/passos.md — modificados
- docs/PWA-servidor.md e docs/commits.md — novos (untracked, entram no próximo add)

#### ⚠️ Verificar antes do commit
- mobile/gradlew.bat — 186 linhas "alteradas": quase certo que é só troca de
  fim de linha (CRLF), não mudança real. Se você não mexeu nele:
  git restore mobile/gradlew.bat
- mobile/JDA2.txt — confirmar se a mudança é intencional



## Passos

Código pronto — quatro mudanças: `Caddyfile` novo (proxy com TLS automático lendo o domínio do `.env`), o compose de produção ganhou o serviço **caddy** (portas 80/443, certificados em volume persistente) e o frontend ficou só interno, o `.env.example` documenta a variável `DOMAIN`, e o `deploy.yml` passou a copiar o Caddyfile para o servidor. As portas 443 já estavam abertas na security list e no iptables desde o setup — sorte de principiante planejada.

Sua parte, em três passos:

**1. Criar o subdomínio DuckDNS** (~2 min): acesse `www.duckdns.org`, entre com a conta GitHub, digite um nome no campo "sub domain" (ex.: `julibio-todo`) → **add domain** → na linha criada, preencha o IP `147.15.78.254` → **update ip**.

**2. Registrar o domínio no VPS** (troque pelo nome que criou):

```bash
ssh -i ~/.ssh/oracle_vps ubuntu@147.15.78.254 "echo 'DOMAIN=julibio-todo.duckdns.org' >> /opt/todolist02/.env"
```

**3. Release** — pela sua regra de versionamento, capacidade nova = **MINOR**: `v0.2.0` (e pré-1.0 pode exigir env nova sem drama, como previsto):

```bash
cd /mnt/c/Projetos/00-claudecode-todolist02+CI-CD
git add .
git commit -m "feat: HTTPS via Caddy/DuckDNS — habilita PWA"
git push
git tag v0.2.0 && git push origin v0.2.0
```

Após o deploy, o endereço do app passa a ser **`https://julibio-todo.duckdns.org`** (o Caddy emite o certificado no primeiro acesso, ~30 s; o HTTP redireciona sozinho). Aí o teste do objetivo: abrir no Chrome e procurar o ícone de **instalar** na barra de endereço — com HTTPS válido, o PWA finalmente se oferece para instalar, no desktop e no celular. Me diga o nome do subdomínio quando criar, que eu confiro o DNS antes do deploy se quiser.

## Detalhando o que o Claude fez

Quatro arquivos, todos em `C:\Projetos\00-claudecode-todolist02+CI-CD\`:

**1. `Caddyfile` (novo, na raiz)** — 3 linhas de configuração: o site `{$DOMAIN}` (valor vem do `.env` do servidor) com `reverse_proxy frontend:80`. É o Caddy quem cuida do certificado Let's Encrypt e da renovação, sem config extra.

**2. `docker-compose.prod.yml`** — duas mudanças no bloco de serviços:
- No `frontend`: **removi** o `ports: "80:80"` — ele deixa de falar com a internet e vira serviço interno (o comentário no lugar explica).
- **Adicionei** o serviço `caddy` (imagem `caddy:2-alpine`): publica 80 e 443, recebe `DOMAIN` do `.env`, monta o `Caddyfile` e dois volumes novos (`caddy_data`/`caddy_config`, declarados no fim do arquivo) para os certificados sobreviverem a deploys.

**3. `.env.example`** — acrescentei ao final a variável `DOMAIN=` com comentário explicando (modelo para o `.env` real do VPS, que você alimenta no passo 2).

**4. `.github/workflows/deploy.yml`** — uma linha: o `source` do passo de `scp` ganhou o `Caddyfile` (`"docker-compose.prod.yml,database/init.sql,Caddyfile"`), para o arquivo chegar a `/opt/todolist02/` a cada deploy.

Confira você mesmo com `git diff` (mostra 2, 3 e 4 linha a linha) e `git status` (acusa o `Caddyfile` como novo). O fluxo de tráfego resultante: internet → Caddy (443, TLS) → nginx do frontend (interno) → `/api` → backend → Postgres — uma camada a mais na frente, nada mudou dali para dentro.

## O potencial do Caddy

Ele roteia por **SNI/Host**: as portas 80/443 são dele sozinho, e cada requisição vai para o app certo conforme o domínio pedido, com **um certificado automático por domínio**. No Caddyfile, cada app são 3 linhas:

```
todo.seudominio.duckdns.org {
    reverse_proxy todolist-frontend:80
}
blog.seudominio.duckdns.org {
    reverse_proxy ghost:2368
}
api-teste.seudominio.duckdns.org {
    reverse_proxy outra-api:5000
}
```

Mas há uma implicação de arquitetura no seu caso: hoje o Caddy vive **dentro** do compose do todolist. Para servir várias aplicações, ele deve ser promovido a **infraestrutura compartilhada do VPS**: um compose próprio (ex.: `/opt/caddy/`) dono das portas 80/443, e uma rede Docker externa (ex.: `web`) que cada app declara ao subir — os apps não publicam porta nenhuma, só entram na rede para o Caddy alcançá-los pelo nome do container. Adicionar um app novo vira: subir o compose dele na rede `web` + 3 linhas no Caddyfile + `docker exec caddy caddy reload`.

Detalhes práticos do seu contexto: o DuckDNS dá **até 5 subdomínios grátis** por conta, todos podendo apontar para o mesmo IP (para mais, um domínio próprio ~R$40/ano resolve com subdomínios ilimitados via wildcard). E o limitante real será a RAM do 1 GB — apps pequenos em Alpine cabem vários, mas cada Postgres próprio come ~80 MB; compartilhar uma instância de banco entre os apps de teste é o truque para caber mais.

## refatorando para "Caddy central + rede web" 

Muda pouco (mover o serviço para um compose próprio e ajustar o do todolist).

Refatorando para o desenho definitivo — Caddy central, apps plugáveis. Primeiro as mudanças no repositório:O `rm` do arquivo é com você (minha permissão de escrita aqui não apaga arquivos): `rm /mnt/c/Projetos/00-claudecode-todolist02+CI-CD/Caddyfile` — ele migra para o VPS como infraestrutura.

Repositório pronto: compose sem Caddy e com o frontend na rede externa `web`, deploy.yml sem o Caddyfile, `.env.example` com nota explicando onde o domínio vive agora. A sequência completa, **na ordem** (importa, por causa da porta 80 e da rede):

**1. DuckDNS** (se ainda não fez): criar o subdomínio em duckdns.org apontando para `147.15.78.254`. Nos comandos abaixo uso `julibio-todo.duckdns.org` — troque pelo seu.

**2. Infra central no VPS** (rede + Caddy, uma única vez):

```bash
ssh -i ~/.ssh/oracle_vps ubuntu@147.15.78.254 'sudo docker network create web; sudo mkdir -p /opt/caddy && sudo chown ubuntu:ubuntu /opt/caddy
cat > /opt/caddy/Caddyfile <<EOF
julibio-todo.duckdns.org {
    reverse_proxy todolist02-frontend:80
}
EOF
cat > /opt/caddy/docker-compose.yml <<EOF
services:
  caddy:
    image: caddy:2-alpine
    container_name: caddy
    restart: unless-stopped
    ports: ["80:80", "443:443"]
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks: [web]
volumes:
  caddy_data:
  caddy_config:
networks:
  web:
    external: true
EOF'
```

**3. Release do todolist** (libera a porta 80 e entra na rede `web`):

```bash
cd /mnt/c/Projetos/00-claudecode-todolist02+CI-CD
git add . && git commit -m "feat: frontend na rede web do Caddy central — habilita HTTPS/PWA"
git push
git tag v0.2.0 && git push origin v0.2.0
gh run watch
```

**4. Subir o Caddy** (só depois do deploy verde — antes disso a 80 ainda está ocupada pelo frontend antigo):

```bash
ssh -i ~/.ssh/oracle_vps ubuntu@147.15.78.254 "cd /opt/caddy && sudo docker compose up -d"
```

**5. Teste**: `https://julibio-todo.duckdns.org` (primeiro acesso pode levar ~30 s enquanto o certificado é emitido). Cadeado verde → procure o ícone de **instalar app** na barra do Chrome — é o PWA finalmente elegível.

Daqui em diante, app novo no VPS = compose dele na rede `web` (sem publicar portas) + 3 linhas no `/opt/caddy/Caddyfile` + `sudo docker exec caddy caddy reload --config /etc/caddy/Caddyfile`.



