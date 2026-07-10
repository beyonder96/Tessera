# Tessera 📱✨

Bem-vindo ao **Tessera**, o seu assistente de estilo de vida completo e hub inteligente!
Acompanhe suas finanças, metas diárias, notícias, saúde e bem-estar em um único lugar.

## 🚀 Novidades da Versão 1.8.0

- **Tessera Chat (Gemini SDK):** Integração nativa de IA usando a novíssima biblioteca do Gemini para Android. Agora a Tessera tem uma aba própria de chat acessível direto pela navegação principal, pronta para te ajudar nas tarefas e planejamento diário! O chat conta com efeito *Glassmorphism* super elegante.
- **Futebol Profissional:** O acompanhamento de jogos foi inteiramente reformulado! O velho widget tipo "pílula" foi substituído pelo espetacular `DetailedMatchWidget` na tela principal de Daily, trazendo todos os eventos (gols, cartões amarelos e vermelhos, pênaltis perdidos e alterações) e estatísticas detalhadas com ícones personalizados.
- **Notícias de Alta Performance (RSS):** Substituímos o uso engessado e custoso da News API por um feed direto via RSS ultrarrápido (`com.prof18.rssparser:rssparser:6.0.10`). Acompanhe o G1, BBC e muito mais diretamente da sua tela, tudo rodando em altíssima velocidade em *background*.

## 🚀 Novidades da Versão 1.7.9

- **Independência das Listas de Mercado e Planejamento:** A aba de Planejamento e No Mercado agora operam com itens separados no banco de dados. Itens adicionados em "No Mercado" já entram marcados por padrão.
- **Integração de Deleção Inteligente:** Ao finalizar a compra, os itens marcados na aba de mercado são excluídos do banco, e o app agora automaticamente apaga os itens com o mesmo nome na aba de Planejamento!
- **Autocompletar na API de Futebol:** Na tela de configurações, a caixa de adição de times ganhou autocompletar dinâmico direto com base nos dados da API, evitando nomes incorretos.
- **Jogos da Rodada:** Configurado o fluxo base para suporte à opção de listar os próximos confrontos diretos da rodada atual.

## 🚀 Novidades da Versão 1.7.6

- **Design Clássico Restaurado na Aba Hoje:** Retornamos ao layout clássico da aba **Hoje**, reativando a linha superior de métricas (`TopMetricsRow`) e o carrossel horizontal (`HorizontalPager`) com os widgets em tamanho real (Finanças, Saúde, Foco, Petz e Mercado).
- **Remoção do Arco de Atividades:** Opcionalmente, removemos o arco central de progresso de atividades (`HeroMetric`), mantendo uma tela extremamente limpa focada nos widgets superiores e inferiores.

## 🚀 Novidades da Versão 1.7.5

- **Restauração de Widgets Antigos:** A pedido dos usuários, trouxemos de volta os widgets clássicos na aba **Hoje** (Placar de futebol `FootballScoreboardPill` e letreiro de notícias `basicMarquee`) e na tela diária (Widget do X/Twitter `XTimelineWidget` e o feed expandido de notícias `NewsExpandedSection`).
- **O Mega Redesign da Aba Hoje (1.7.4):** Substituímos o painel antigo por uma Grid Premium de widgets quadrados com efeito Glassmorphism, mantendo consistência e layout de alta performance. Adicionamos um novo Widget Climático Premium usando dados em tempo real da API Open-Meteo com transições fluidas e microanimações!
- **Zenith Hub (Foco & Rotinas):** Transformamos as páginas de metas no "Zenith Hub". Adicionamos suporte ao `HorizontalPager` permitindo transições por swipe! Implementamos um layout inspirado no Routinery.
- **Finanças 100% Sincronizadas:** Corrigido o bug histórico que impedia o campo "Disponível para Gastar" de zerar. Agora ele engloba também faturas de cartão corretamente para cálculos precisos, e introduzimos o Invoice Hub para gerir faturas de crédito com riqueza de detalhes!
- **Conexão do Spotify Estabilizada:** Chega do erro `AUTHENTICATION_SERVICE_UNAVAILABLE`! Refizemos toda a autenticação usando CustomTabs e redirecionamento nativo, deixando o app completamente independente do SDK instável.
- **Glance Widgets (Android):** Aplicamos cores mais premium em cada um dos widgets do Android para combinar com a interface do aplicativo!

### 📥 Links para Download
Os APKs desta versão encontram-se na pasta `.build-outputs/`:
- **Debug:** `app-debug-1.8.1.apk`
- **Release:** `app-release-1.8.1.apk`

---
Tessera. Seu guia, em toda a linha do tempo.
