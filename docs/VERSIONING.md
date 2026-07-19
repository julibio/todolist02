# Regras de versionamento — todolist02

Adotamos **SemVer** (`vMAJOR.MINOR.PATCH`), com a tag git como fonte única da verdade.
A tag dispara o deploy e nomeia as imagens — código, artefato e produção ficam amarrados.

## Quando incrementar o quê

| Parte | Incrementa quando... | Exemplo |
|---|---|---|
| **PATCH** (`v0.1.1` → `v0.1.2`) | Correção de bug, ajuste de texto/estilo, sem mudar comportamento esperado | corrigir o filtro de tags |
| **MINOR** (`v0.1.2` → `v0.2.0`) | Funcionalidade nova compatível com o que existe | adicionar lembretes/prazos |
| **MAJOR** (`v0.2.0` → `v1.0.0`) | Quebra de compatibilidade: API muda contrato, schema do banco exige migração destrutiva, .env ganha variável obrigatória | trocar formato do login |

Regras auxiliares: `v1.0.0` marca o primeiro release "estável" (antes disso, MINOR pode quebrar);
nunca reutilizar nem mover tag já publicada — errou, publica a próxima.

## Ritual de release (sempre igual)

### Testes manuais (obrigatórios enquanto não há testes automatizados)

**Antes da tag — no ambiente local** (`docker compose -f docker-compose.dev.yml up --build`):

- [ ] Registro de usuário novo funciona
- [ ] Login com credencial correta entra; com senha errada é recusado
- [ ] Criar tarefa (com categoria e tag) aparece na lista
- [ ] Concluir, editar e excluir tarefa funcionam
- [ ] Filtros por categoria e por tag retornam o esperado
- [ ] `docker compose restart` → dados continuam lá (persistência)
- [ ] O que a versão MUDOU se comporta como descrito no changelog

**Depois do deploy — smoke test em produção** (`http://147.15.78.254`, com Ctrl+Shift+R):

- [ ] Página carrega e mostra a mudança da versão
- [ ] Login com usuário existente funciona
- [ ] Criar uma tarefa e recarregar a página — ela persiste

> Falhou algo em produção? Rollback imediato (seção abaixo), investigar com calma depois.
> Estes checklists serão substituídos por testes automatizados no ci.yml/deploy.yml (previsto para a próxima versão).

```bash
# 1. código pronto e CHECKLIST LOCAL acima completo
# 2. atualizar "version" nos package.json (backend e frontend) — cosmético, mas coerente
git commit -am "feat: <o que mudou>"
git push
# 3. tag = versão (push cirúrgico: só a tag nova)
git tag vX.Y.Z
git push origin vX.Y.Z             # dispara build + deploy
# 4. SMOKE TEST em produção (checklist acima) — falhou? rollback
# 5. release com changelog (vitrine e histórico)
gh release create vX.Y.Z --notes "- mudança 1
- mudança 2"
```

Cada release publica no GHCR: `:vX.Y.Z` (imutável), `:<sha>` (imutável, auditoria) e move o `:latest`.

## Como rodar QUALQUER versão (inclusive antiga)

**Opção A — imagens prontas (recomendado; sem build):**

```bash
git clone https://github.com/julibio/todolist02.git && cd todolist02
cp .env.example .env               # preencher; escolher a versão:
# IMAGE_TAG=v0.1.1  (ou latest, ou um SHA curto)
docker compose -f docker-compose.prod.yml up -d
```

**Opção B — build do código-fonte daquele momento:**

```bash
git clone https://github.com/julibio/todolist02.git && cd todolist02
git checkout v0.1.1                # o código exato daquela versão
docker compose -f docker-compose.dev.yml up --build
```

**Rollback em produção** (no VPS): editar `IMAGE_TAG` no `/opt/todolist02/.env`
para a versão desejada e `docker compose -f docker-compose.prod.yml pull && up -d`.

⚠️ Banco de dados: voltar a imagem NÃO volta o schema. Enquanto o `init.sql` for a única
migração, ok; quando houver migrações incrementais, rollback de MAJOR exige plano próprio.

## Estado atual

- `v0.1.0` — tag histórica (deploy falhou; ignorar)
- `v0.1.1` — primeira versão em produção (VPS Oracle, 19/07/2026)
