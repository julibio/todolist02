Fazendo um versionamento, seguindo o padrão SemVer no projeto todolist02.
Trocar nome na aba web: seria alterar "To Do List v2" no index.html (decidi não fazer)
Trocar nome no conteúdo web: troquei 2x "To Do List v2" por "To Do List v2 (SemVer)", no App.jsx
Trocar nome no PWA: troquei o "To Do List v2" por "To Do List v2 (SemVer)", no vite.config.js
Alterar a versão nos arquivos packge.json do backend e do frontend para 0.1.2

Vamos gerar a o controle de versão da 0.1.1 para a 0.1.2 da minha máquina até o servidor, passo a passo.

0. Alteras os arquivos no projeto  
- a metodologia CI/CD implica em testes automatizados
- nesse projeto, o ci.yml só builda

1. Conferir as mudanças  
cd /mnt/c/Projetos/00-claudecode-todolist02+CI-CD  
git status  
git diff --stat  

2. Alinhar o "version" nos package.json → "0.1.2"  
- no arquivo do frontend e 
- no arquivo do backend

3. Testar localmente (opcional se os testes automáticos estivessem rodando)  
- no windows, em c:\Projetos\00-claudecode-todolist02+CI-CD\, rodar:  
wsl --cd c:\Projetos\00-claudecode-todolist02+CI-CD\ docker compose up -d --build
- ou, do wsl, em /mnt/c/Projetos/00-claudecode-todolist02+CI-CD, rodar:  
docker compose up -d --build
- abra http://localhost:8081 (ou 5174, conforme seu compose dev) e veja o novo título
- checklist de 7 itens 
  - registro, 
  - login certo/errado, 
  - CRUD, 
  - filtros, 
  - persistência no restart, e 
  - conferir que o título "(SemVer)" aparece nas duas telas — que é "o que a versão mudou"). 
- derrubar com down ao final  
- no windows, em c:\Projetos\00-claudecode-todolist02+CI-CD\, rodar:  
wsl --cd c:\Projetos\00-claudecode-todolist02+CI-CD\ docker compose down
- ou, do wsl, em /mnt/c/Projetos/00-claudecode-todolist02+CI-CD, rodar:  
docker compose down

4. Coloca os arquivos alterados na área de embarque (stage), depois transfere para a repositório local e finalmente envia para o repositório rem  
git add .  
git commit -m "chore: título com identificação SemVer (App.jsx e manifest PWA)"  
git push  

5. A versão nasce aqui:  
git tag v0.1.2  
git push origin v0.1.2  
- A primeira linha rotula o commit atual como v0.1.2 (local); a segunda publica só essa tag no GitHub — e é ela que acorda o workflow Deploy: build das duas imagens → GHCR (:v0.1.2, :sha e :latest) → SSH no VPS → compose pull && up -d.  

6. Trilha de auditoria do seu versionamento  



- `gh run list 10` → **quando** cada versão foi ao ar (e se o processo passou);
- `git tag` → **qual código** cada versão contém;
- `gh release list` → **o que mudou** (changelog humano);
- GHCR → **o artefato executável** de cada uma, pronto para pull.  

E as linhas `CI` intercaladas mostram a outra metade do fluxo: pushes na main que foram validados sem gerar release — a separação limpa entre "integrar código" e "publicar versão".  

Para detalhar a versão: `gh release create v0.1.2 --notes "Título identifica a versão SemVer no app (telas de login e principal) e no manifest PWA. Docs: regras de versionamento e checklists de teste manual."`  

Completando o mapa — cada vista com seu endereço no site e o comando equivalente:

| Vista | No navegador | No terminal |
|---|---|---|
| **Quando foi ao ar** (runs) | `github.com/julibio/todolist02/actions` | `gh run list --limit 10` |
| **Qual código** (tags) | `github.com/julibio/todolist02/tags` | `git tag` (local) · `git ls-remote --tags origin` (remoto) |
| **O que mudou** (releases) | `github.com/julibio/todolist02/releases` | `gh release list` · `gh release view v0.1.2` |
| **Artefato executável** (GHCR) | `github.com/julibio?tab=packages` (→ `todolist02-backend`/`-frontend`) | `docker pull ghcr.io/julibio/todolist02-backend:v0.1.2` |

E duas de bônus que amarram as pontas:

| Vista | No navegador | No terminal |
|---|---|---|
| **O que roda em produção** | — | `ssh -i ~/.ssh/oracle_vps ubuntu@147.15.78.254 "grep IMAGE_TAG /opt/todolist02/.env && sudo docker ps --format '{{.Names}}: {{.Image}}'"` |
| **Código de uma versão específica** | `github.com/julibio/todolist02/tree/v0.1.1` | `git checkout v0.1.1` (e `git checkout main` para voltar) |

Com essas seis, você responde qualquer pergunta de versionamento do projeto: o que existe, quando subiu, o que mudou, onde está o binário, o que está no ar e como era o código em qualquer ponto da história.






