# Tessera - Assistente de Vida Inteligente

![Tessera](https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1200&auto=format&fit=crop)

O **Tessera** é um aplicativo Android revolucionário que unifica a gestão da sua vida em um único lugar. Com design ultra-moderno baseado em *Premium Glassmorphism*, transições fluidas e widgets inteligentes, ele organiza suas finanças, compras de mercado, transporte público, rotina dos seus pets, e muito mais.

Tudo isso acompanhado da **Tessera AI**, uma inteligência artificial local nativa (powered by Gemma 2B) ou em nuvem (Gemini 1.5 Flash), desenhada para conversar, auxiliar e cruzar dados locais do seu dia a dia.

---

## 🚀 Novidades da Versão 1.6.5

A versão **1.6.5** foca na melhoria da experiência de uso com apenas uma mão e na estabilidade das APIs públicas:

*   **🛒 Aba Mercado (One-Handed UX):** Interface redesenhada! Campo de busca agora flutua acima do teclado, cartões de lista muito maiores (toque em qualquer lugar do item para marcá-lo), e botão "Finalizar" ancorado na base da tela. Tudo para ser usado com uma única mão enquanto você empurra o carrinho do supermercado!
*   **🚇 Transporte Mais Inteligente:** Como a API pública do Metrô SP/ViaQuatro foi desativada pelas concessionárias (Erro 404), o Tessera agora foca *exclusivamente* nas **Linhas Favoritas** que você selecionou nas Configurações, garantindo que você não tenha telas quebradas! As previsões de ônibus (SPTrans) também receberam melhorias de estabilidade e simulação em caso de queda do servidor.
*   **🤖 Tessera Chat AI:** Instruções claras para carregar a Inteligência Artificial **Gemma 2B Localmente** (totalmente offline e privada!). Se a IA não for encontrada na pasta do app (`Android/data/com.example/files/`), ela indicará exatamente como proceder, ou fará o *fallback* silencioso para o Gemini na nuvem, desde que a chave API esteja configurada.
*   **💸 Finanças Minimalistas:** Tela focada no essencial ("O quanto posso gastar"), separando despesas fixas, recorrentes e gerais. A seção de Contas e Cartões agora permanece sempre visível para mais praticidade.

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
    *   `app-debug-1.6.5.apk`
    *   `app-release-1.6.5.apk`

*Observação: A geração direta dentro da nuvem (IDE remota) pode exigir a configuração prévia do `ANDROID_HOME` com os SDKs compilados.*

---

**Criado por Kenned com o Google Gemini.**
