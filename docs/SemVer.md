O **SemVer** (abreviação de *Semantic Versioning* ou Versionamento Semântico) é um conjunto universal de regras usado no desenvolvimento de software para atribuir números de versão. O objetivo é comunicar o impacto das atualizações de código de forma clara, indicando se uma nova versão introduz funções novas, corrige bugs ou quebra a compatibilidade com versões anteriores.

A estrutura segue sempre o formato numérico **`vMAJOR.MINOR.PATCH`** (ou `vMAIOR.MENOR.CORREÇÃO`).

## Como a numeração funciona

Cada parte do número tem um significado estrito e dita quando a versão deve ser incrementada:

* **MAJOR (Maior — ex: v`2`.0.0):** Incrementado quando são feitas **mudanças incompatíveis** na API. Isso sinaliza que a atualização "quebra" o funcionamento anterior; quem usa o software ou biblioteca precisará adaptar o próprio código para não ter problemas.
* **MINOR (Menor — ex: v1.`3`.0):** Incrementado quando **novas funcionalidades** são adicionadas de maneira retrocompatível. O software ganha recursos novos, mas tudo o que já funcionava antes continua funcionando perfeitamente.
* **PATCH (Correção — ex: v1.2.`4`):** Incrementado quando são feitas **correções de bugs** retrocompatíveis. Não há funções novas nem quebras de compatibilidade, apenas consertos internos.

## Regras de "Zerar" (Reset)

O SemVer funciona como um odômetro com regras de dependência entre os números:

* Se você incrementa o **MAJOR** (de `1.4.2` para `2.0.0`), os números MINOR e PATCH obrigatoriamente voltam para zero.
* Se você incrementa o **MINOR** (de `1.4.2` para `1.5.0`), o PATCH obrigatoriamente volta para zero.

## Metadados Adicionais

O padrão também permite adicionar sufixos ao final do PATCH para indicar versões de pré-lançamento ou metadados de compilação (*build*). Eles são separados por um hífen ou sinal de adição:

* **Pré-lançamentos:** `v1.0.0-alpha.1`, `v2.3.4-beta`, `v3.0.0-rc.1` (Release Candidate). Indicam que a versão ainda não é estável.
* **Metadados:** `v1.0.0+20130313144700` (geralmente ignorados ao verificar se uma versão é mais recente que a outra).

Esse sistema é fundamental para gerenciadores de pacotes modernos (como NPM, Pip, Cargo, etc.), pois permite configurar atualizações automáticas seguras (ex: "aceitar qualquer atualização de PATCH e MINOR, mas travar o MAJOR para evitar que o sistema quebre").
