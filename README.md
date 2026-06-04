# Tessera

> A stylish dashboard built with Jetpack Compose.

## 📱 Download

O APK de debug mais recente está disponível em:

```
.build-outputs/app-debug.apk
```

Você pode baixar diretamente pelo GitHub:
[📥 app-debug.apk](https://github.com/beyonder96/Tessera/blob/main/.build-outputs/app-debug.apk)

## 🛠️ Tecnologias

- **Android** com [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Gradle (Kotlin DSL)** para build
- **Gemini API** (server-side) via `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`

## 🚀 Como buildar

```bash
# Clone o repositório
git clone https://github.com/beyonder96/Tessera.git
cd Tessera

# Configure as variáveis de ambiente
cp .env.example .env
# Edite o .env com suas chaves

# Build de debug
./gradlew assembleDebug

# O APK será gerado em:
# app/build/outputs/apk/debug/app-debug.apk
```

## 📁 Estrutura

```
Tessera/
├── .build-outputs/       # APK gerado pelo CI/GitHub Actions
│   └── app-debug.apk
├── app/                  # Código-fonte do app Android
├── assets/               # Recursos estáticos
├── gradle/               # Configurações do Gradle
├── .env.example          # Exemplo de variáveis de ambiente
└── metadata.json         # Metadados do projeto
```

## ⚙️ Variáveis de Ambiente

Consulte o arquivo [`.env.example`](.env.example) para ver as variáveis necessárias.

---

> **Status:** em transição 🚧
