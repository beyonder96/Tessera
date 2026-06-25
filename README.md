# Tessera - Assistente de Vida Inteligente

![Tessera](https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1200&auto=format&fit=crop)

O **Tessera** é um aplicativo Android revolucionário que unifica a gestão da sua vida em um único lugar. Com design ultra-moderno baseado em *Premium Glassmorphism*, transições fluidas e widgets inteligentes, ele organiza suas finanças, compras de mercado, transporte público, rotina dos seus pets, e muito mais.

Tudo isso acompanhado da **Tessera AI**, uma inteligência artificial local nativa (powered by Gemma 2B) ou em nuvem (Gemini 1.5 Flash), desenhada para conversar, auxiliar e cruzar dados locais do seu dia a dia.

---

## 🚀 Novidades da Versão 1.6.7

A versão **1.6.7** traz grandes atualizações para a experiência do usuário e novas integrações inteligentes:

*   **⚽ Futebol Ao Vivo (Sportmonks API):** O widget de futebol foi totalmente reformulado! Agora ele busca dados dinâmicos da nova API da Sportmonks, gerando automaticamente abas para os times que estão jogando no momento (ao invés de times fixos).
*   **⏰ Lembretes Inteligentes:** Nova seção nas Configurações para programar notificações diárias que lembram você de registrar seus passos e suas horas de sono, mantendo a aba de Saúde sempre atualizada!
*   **🛒 Aba Mercado (One-Handed UX):** Interface redesenhada para uso com apenas uma mão enquanto empurra o carrinho.
*   **🚇 Transporte Mais Inteligente:** Foco nas Linhas Favoritas configuradas, evitando falhas com APIs de terceiros.
*   **🤖 Tessera Chat AI:** Melhorias no carregamento do modelo local Gemma 2B e integração com Gemini na nuvem.
*   **💸 Finanças Minimalistas:** Layout focado no essencial para praticidade no dia a dia.

---

## 📥 Download

Baixe a versão mais recente do Tessera diretamente aqui:

[![Download Release APK](https://img.shields.io/badge/Download-Release_APK_(1.6.7)-4CAF50?style=for-the-badge&logo=android)](.build-outputs/app-release-1.6.7.apk)

[![Download Debug APK](https://img.shields.io/badge/Download-Debug_APK_(1.6.7)-FF9800?style=for-the-badge&logo=android)](.build-outputs/app-debug-1.6.7.apk)

---

## 🛠 Funcionalidades Principais

*   **📱 Widgets Dinâmicos:** Seis widgets (Resumo Diário, Finanças, Pets, Mercado, Saúde, e Metas) nativos na sua tela inicial, todos totalmente independentes e com atualização em segundo plano livre de telas de carregamento infinito.
*   **🎨 Premium Glassmorphism:** O aplicativo inteiro compartilha uma estética em vidro translúcido responsivo, com transições "Liquid Glass" ao rolar o topo das abas.
*   **🐶 Gestão de Pets:** Controle a ração, hidratação, despesas, lembretes de banho e vacinas do seu amigo de quatro patas.
*   **🛒 Supermercado & Planejamento:** Planeje compras em casa e transfira os itens com um clique para "No Mercado", checando os produtos enquanto pega das prateleiras.
*   **🚌 Mobilidade Urbana:** Acompanhe o status do metrô e trem, e preveja a chegada do seu ônibus no ponto exato.
*   **🌟 Metas & Desejos:** Defina o que você quer comprar, adicione fotos impressionantes diretamente da API do Pexels integrada, e defina uma meta de economia.
*   **🧠 IA Integrada:** O chat conversa de forma fluida, e possui a capacidade (com a sua permissão) de cruzar os dados guardados em todas as outras telas.

---

## 💻 Como Compilar (APK)

O Tessera é projetado com Jetpack Compose moderno. Para gerar o APK no seu computador local:

1. Clone o repositório.
2. Abra o projeto no **Android Studio**.
3. Sincronize o Gradle.
4. Execute `./gradlew assembleRelease` para gerar o APK de Produção, ou `./gradlew assembleDebug` para testar.
5. Os arquivos estarão na pasta `.build-outputs/`:
    *   `app-debug-1.6.7.apk`
    *   `app-release-1.6.7.apk`

*Observação: A geração direta dentro da nuvem (IDE remota) pode exigir a configuração prévia do `ANDROID_HOME` com os SDKs compilados.*

---

**Criado por Kenned com o Google Gemini.**
