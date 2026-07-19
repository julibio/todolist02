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

4. x
git add .


