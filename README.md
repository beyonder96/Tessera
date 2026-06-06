# Tessera 📱✨

> Um dashboard inteligente, imersivo e de alta fidelidade visual construído com **Jetpack Compose**, integrando inteligência artificial local e sincronização avançada de saúde.

---

## 📥 Downloads (APKs)

Os pacotes oficiais prontos para instalação estão disponíveis na pasta `.build-outputs/`:

* **[📥 app-release-1.0.1.apk (Versão Oficial)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-release-1.0.1.apk)**  
  *Versão final de produção. Otimizada (~12.2 MB), com minificação de código (R8/ProGuard), remoção automática de logs de depuração/prints de console e compressão extrema de assets (PNG Crunching).*
* **[📥 app-debug-1.0.1.apk (Versão Debug)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-debug-1.0.1.apk)**  
  *Versão para testes contendo ferramentas de depuração ativas.*

---

## 🌟 Principais Funcionalidades

### 🐾 1. Painel Petz (Estética Premium)
* **Ficha Técnica em Glassmorphism:** Dados consolidados do pet com badges industriais monoespaçados (Sexo, RGA, Microchip e Status de Castração).
* **Scroll Imersivo:** Foto de topo fixa que aplica desfoque (blur) dinâmico e progressivo conforme o scroll, sem distorcer ou dar zoom na imagem.
* **Readiness Ring (Vitalidade):** Gráfico circular no estilo *Oura Ring* desenhado sob medida via Canvas com degradês e brilho reativo baseado nas vacinas e peso do pet.
* **Agendador de Rotinas:** Seletor de horário integrado com relógio analógico nativo do Material 3 (`TimePicker`).

### 🩺 2. Módulo de Saúde Inteligente & Leve
* **Sincronização Health Connect Automática:**
  * **Permissão Unificada:** Checagem em segundo plano que ativa o sincronismo silencioso automaticamente se a permissão já foi dada, sem banners repetitivos.
  * **Histórico Inteligente de Peso:** Filtro dinâmico contra duplicidades que registra o peso histórico apenas mediante variação relevante (> 0.05 kg).
  * **Resgate Inteligente de Altura:** Busca estendida em lote de até 5 anos para encontrar o último registro de altura no Health Connect e calcular o **IMC automático** com status colorido.
* **Design Leve com Luzes Ambientais:** Fundo escuro premium decorado com luzes de neon difusas (verde-menta e roxo) e animações de entrada em cascata (*staggered fade-in*) para um visual relaxante.
* **Anel de Passos Neon Glow:** Arco circular luminoso composto por múltiplas camadas de glow e cursor com halo azul brilhante na ponta do progresso diário.

### 🏠 3. Experiência de Uso na Aba Hoje (Home)
* **Performance de Scroll Otimizada:** Parâmetros de animação delegados diretamente à GPU através do `.graphicsLayer`, mantendo a fluidez de renderização em 120 FPS.
* **Glow Respirante Circadiano:** Efeito de pulsação suave em degradê radial que ajusta a tonalidade em tempo real baseado no horário do dia (alvorecer, meio-dia, crepúsculo e noite).
* **Micro-animações do Tempo:** Ícones de saudação que rotacionam (Sol) ou balançam (Lua) dinamicamente.
* **Efeito Mola (Bounce Click):** Feedback físico de clique elástico aplicado a todos os botões e cartões da interface.

### 🤖 4. IA Local Contextual (Tessera AI)
* **Injeção de Contexto de Tabelas Locais:** O prompt da inteligência artificial local é alimentado em tempo real com o estado dos medicamentos agendados (`Medication`) e itens de mercado (`MarketItem`).
* **Fallback Inteligente:** O assistente de IA responde com precisão contextual e lógica reativa sobre as tarefas diárias pendentes, mesmo em modo offline.

---

## 🛠️ Como Compilar e Buildar

### Pré-requisitos
* **Android SDK** (API level 34+)
* **JDK 17**

### Configuração do Ambiente

1. Clone o repositório:
   ```bash
   git clone https://github.com/beyonder96/Tessera.git
   cd Tessera
   ```

2. Crie e preencha as variáveis de ambiente necessárias a partir do modelo:
   ```bash
   cp .env.example .env
   ```

### Comandos do Gradle

* **Gerar versão de depuração (Debug):**
  ```powershell
  .\gradlew.bat assembleDebug
  ```
  *O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.*

* **Gerar versão de produção otimizada (Release):**
  ```powershell
  .\gradlew.bat assembleRelease
  ```
  *O APK será gerado em `app/build/outputs/apk/release/app-release.apk`.*

---

## 📁 Estrutura do Projeto

```text
Tessera/
├── .build-outputs/       # Diretório de APKs compilados e prontos para uso
│   ├── app-debug-1.0.1.apk # Versão de testes com logs ativos
│   └── app-release-1.0.1.apk # Versão final de produção assinada
├── app/                  # Módulo principal Android (Código Kotlin & Jetpack Compose)
│   ├── src/main/java/    # Arquivos de código-fonte
│   └── proguard-rules.pro# Regras de minificação R8/ProGuard
├── assets/               # Imagens e recursos estáticos do repositório
├── gradle/               # Configuração e wrappers do Gradle
├── .env.example          # Modelo de configuração de chaves do app
└── settings.gradle.kts   # Definições gerais do projeto Gradle
```
