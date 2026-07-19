É o padrão **Conventional Commits**: `tipo(escopo): descrição`. Os tipos principais:

| Tipo | Quando usar | Exemplo no seu projeto |
|---|---|---|
| **feat** | Funcionalidade nova (mapeia para MINOR no SemVer) | `feat: prazo/lembrete nas tarefas` |
| **fix** | Correção de bug (mapeia para PATCH) | `fix: filtro de tags ignorava acentos` |
| **docs** | Só documentação | `docs: checklist manual no VERSIONING.md` |
| **style** | Formatação sem mudar lógica (indentação, ponto-e-vírgula) | `style: prettier no App.jsx` |
| **refactor** | Reorganização de código sem mudar comportamento | `refactor: extrai api.js do App.jsx` |
| **perf** | Melhoria de desempenho | `perf: índice na coluna user_id` |
| **test** | Adição/ajuste de testes | `test: rotas de auth com supertest` |
| **build** | Sistema de build, dependências, Dockerfiles | `build: node 22 → 24 nas imagens` |
| **ci** | Workflows/pipeline | `ci: testes antes do build no deploy.yml` |
| **chore** | Manutenção que não toca código de produção nem se encaixa acima | `chore: título com SemVer` |
| **revert** | Desfaz um commit anterior | `revert: feat de lembretes` |

Complementos úteis da convenção:

- **Escopo** opcional entre parênteses: `feat(backend): ...`, `fix(mobile): ...` — útil no seu monorepo com frontend/backend/mobile.
- **`!` = breaking change** (mapeia para MAJOR): `feat(api)!: token no header em vez de cookie`.
- Descrição no **imperativo, minúscula, sem ponto final**: "adiciona X", não "Adicionado X.".

O elo bonito com o que você montou: os prefixos conversam com o SemVer — `fix` pede PATCH, `feat` pede MINOR, `!` pede MAJOR. Em projetos maiores, ferramentas (semantic-release) leem os commits e calculam a próxima versão sozinhas — seus commits bem rotulados hoje são o que tornaria isso plugável amanhã.


