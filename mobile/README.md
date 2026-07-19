# Todolist Mobile (Android)

App Android nativo (Kotlin + Jetpack Compose) que consome a API do backend
`todolist02` rodando no computador via rede local.

## Funcionalidades

- Login e cadastro (JWT, sessão persistida no aparelho)
- Listar, criar, editar, concluir e excluir tarefas
- Categorias e tags (criar, excluir, associar às tarefas)
- Filtros por categoria e tag
- URL do servidor configurável na tela de login (toque em "Servidor: ...")

## Pré-requisitos

1. Backend rodando no computador (o Docker agora roda no WSL2, sem Docker Desktop):
   ```powershell
   wsl --cd C:\Projetos\00-claudecode-todolist02 docker compose up -d
   ```
2. Celular e computador na **mesma rede Wi-Fi**.
3. Porta 3002 liberada no firewall do Windows (executar como administrador):
   ```powershell
   New-NetFirewallRule -DisplayName "Todolist02 backend" -Direction Inbound -LocalPort 3002 -Protocol TCP -Action Allow
   ```
4. Se mesmo assim o celular não conectar: com o WSL em *mirrored networking*, conexões vindas da rede também passam pelo firewall do Hyper-V. Liberar (como administrador):
   ```powershell
   New-NetFirewallHyperVRule -Name "Todolist02-backend" -DisplayName "Todolist02 backend (WSL)" -Direction Inbound -LocalPorts 3002 -Action Allow -VMCreatorId '{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}'
   ```

## Compilar

Abra a pasta `mobile/` no Android Studio e rode, ou via linha de comando:

```powershell
cd C:\Projetos\00-claudecode-todolist02\mobile
.\gradlew.bat :app:assembleDebug
```

O APK fica em `app\build\outputs\apk\debug\app-debug.apk`.

## Instalar no celular

- **Via USB (depuração USB ativada):**
  ```powershell
  adb install app\build\outputs\apk\debug\app-debug.apk
  ```
- **Sem cabo:** copie o `app-debug.apk` para o celular (WhatsApp, Drive,
  e-mail etc.) e abra o arquivo. Permita "instalar apps de fontes desconhecidas".

## Configuração do servidor

O app vem pré-configurado com `http://192.168.15.15:3002`. Se o IP do
computador mudar (verifique com `ipconfig`), toque em **"Servidor: ..."** na
tela de login e ajuste a URL. O valor fica salvo no aparelho.
