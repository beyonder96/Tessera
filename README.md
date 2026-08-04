# Tessera 📱✨

Bem-vindo ao **Tessera**, o seu assistente de estilo de vida completo e hub inteligente!
Acompanhe suas finanças, metas diárias, notícias, saúde e bem-estar em um único lugar.


## 🚀 Novidades da Versão 2.0.12

- **Integração Otimizada com API-Football (v3):** O widget de placar agora utiliza identificadores de times e limites precisos da api-sports.io, garantindo apenas requisições cirúrgicas e protegendo o limite diário de requisições. 
- **Thermal UI & Minimalismo Visual:** Redimensionamento suave de títulos para maior elegância no "TESSERA" e barra de saudações. Nova paleta de luz no widget do clima inspirada num efeito incandescente direcional sobre fundo profundo preto (#000000).

## 🚀 Novidades da Versão 2.0.9

- **Novo Placar de Futebol e Widget de Clima (Estilo Termostato):** Interface em estilo arena luxury com gradiente térmico em camadas, agulha procedural e mostrador com tipografia em gradiente.
- **Novos Temas Globais (Preto Profundo & Branco Total):** Implementado suporte completo aos temas Preto Profundo (OLED #000000) e Branco Total (Pure White #FFFFFF), garantindo contraste e legibilidade ideais. Removido o suporte a imagens de fundo globalmente para maior velocidade e fluidez.
- **Mercado Inteligente 2.0:** Interface de mercado completamente renovada com filtros por categoria (Hortifrúti, Açougue, Laticínios, Bebidas etc.), pílulas de sugestão rápida de itens essenciais e checkout dinâmico.

## 🚀 Novidades da Versão 2.0.0

- **Novo Design da Lista de Desejos:** Interface premium com fundo preto, glassmorphism e extração automática de fotos pelo link de compra (Web Scraping de OpenGraph Tags)! Adicionado barra de pesquisa e botão de compartilhamento.
- **Finanças Reformuladas:** Painel de cartões restaurado com design minimalista. Novo recurso de Extrato Completo (PDF/CSV) com visual de extrato bancário. Nova "Central de Assinaturas" premium.
- **Transportes:** Lógica do status dos trens/metrô aprimorada.
- **Notificações:** Alertas importantes agora pulam em tela cheia (Full Screen Intent) utilizando AlarmManager nativo.
- **Limpeza de Código:** O antigo "Tessera Chat" foi desativado e removido do app, deixando a tela Daily mais enxuta.

## 🚀 Novidades da Versão 1.9.6

- **Widgets Transparentes (Glassmorphism):** Atualizamos o design dos widgets da tela inicial (Finanças, Saúde, Pets, Mercado, Metas e Diário). Eles agora possuem um fundo translúcido moderno. Em launchers compatíveis, o fundo aplica um sofisticado efeito de vidro fosco (blur) com o papel de parede, elevando a estética da sua tela inicial.

## 🚀 Novidades da Versão 1.9.4

- **Reforço de Segurança e Privacidade:** Removemos credenciais sensíveis e senhas de keystore do código-fonte, movendo-as para arquivos de configuração protegidos.
- **Protocolo HTTPS Obrigatório:** Todas as chamadas de rede externas agora utilizam HTTPS criptografado. O tráfego em texto claro (HTTP) foi completamente bloqueado no nível do sistema para garantir a integridade dos seus dados.
- **Sincronização Inteligente do BuildConfig:** O Gradle agora detecta mudanças no seu arquivo `.env` automaticamente, eliminando a necessidade de "Clean Project" manual ao atualizar chaves de API.
- **Limpeza de Permissões:** Removemos permissões obsoletas de armazenamento externo, tornando o app mais leve e respeitando a privacidade do Android moderno.
- **Logs de Produção Protegidos:** Substituímos o rastreamento de pilhas de erro (`printStackTrace`) por logs estruturados e seguros.

## 🚀 Novidades da Versão 1.9.1

- **API de Futebol 100% Brasileira:** Trocamos a plataforma de dados esportivos (Sportmonks para API-Football) para suportar cobertura completa e gratuita do Brasileirão, Copa do Brasil e times sul-americanos!
- **Upgrade da Tessera AI (Gemini 2.5):** O motor de inteligência artificial do chat foi atualizado do `gemini-1.5-flash` para o novíssimo `gemini-2.5-flash`, compatível com a nova estrutura avançada de chaves da Google, entregando respostas mais rápidas e inteligentes!
- **Deploy do Mercado Web:** A funcionalidade de compartilhar a lista do mercado por link/QR Code foi ajustada para buscar o domínio dinâmico do Firebase automaticamente (`.env`), eliminando o erro de site não encontrado.

## 🚀 Novidades da Versão 1.9.0

- **UI Minimalista (Bottom Sheets):** As antigas janelas modais de Adicionar Transação, Transferências e Gerenciamento de Cartões foram completamente substituídas por Bottom Sheets modernos e minimalistas (que deslizam suavemente da parte inferior da tela, estilo iOS). Muito mais clean e menos intrusivo!
- **Integração do Cartão de Benefícios:** Agora é totalmente possível usar o seu Vale-Refeição/Alimentação diretamente na tela de Lançamentos de despesas e receitas.
- **Transferências Funcionais:** A função de Transferência, que estava ausente, agora funciona perfeitamente, permitindo movimentar valores entre suas Contas de forma rápida e registrando o histórico de entradas e saídas.
- **Formatação Numérica (BR):** Corrigimos a ausência dos separadores de milhares nas transações recentes. Valores como 1600,00 agora aparecem elegantes como 1.600,00.

## 🚀 Novidades da Versão 1.8.7

- **Tessera AI Chat Minimalista (Estilo Premium):** Nossa assistente virtual ganhou uma cara nova! Abandonamos as antigas "bolhas de bate-papo" para dar espaço a um visual estilo iOS, muito mais limpo, imersivo e sem bordas. Adicionamos uma nova Tela de Boas-Vindas com cards interativos de atalhos e ajustamos a tipografia para ser impactante. Além disso, agora há um atalho no topo para limpar facilmente o histórico da sessão.
- **Novo Gerenciador de Vale-Refeição:** A área de VR na aba de Finanças foi inteiramente reestruturada. Agora ela funciona como um gerenciador completo (nos mesmos moldes dos cartões de crédito), permitindo visualizar o saldo dinâmico atual, titular do cartão e um extrato completo com histórico de entradas e saídas persistidos em banco de dados!

## 🚀 Novidades da Versão 1.8.2

- **Ajustes de UI (Daily Briefing):** Removemos o widget "Golden Sun" e a seta de voltar da tela de Briefing para uma interface mais limpa. A aba de IA foi movida da navegação inferior para o topo da tela do Daily Briefing como uma elegante barra de pesquisa. O acesso às configurações e o perfil do usuário retornaram ao cabeçalho.
- **Meu Apê (Construção & Reformas):** A animação Lottie do progresso (que ficava em carregamento infinito) foi substituída por um ícone estático premium iluminado. O gráfico inferior foi ajustado para não ser cortado na tela. A data estimada de término, antes fixa, agora pode ser editada ao tocar nela!

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

[![Baixar APK Debug](https://img.shields.io/badge/Download-APK_Debug-green?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-debug-2.0.16.apk)
[![Baixar APK Release](https://img.shields.io/badge/Download-APK_Release-blue?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-release-2.0.16.apk)

---
Tessera. Seu guia, em toda a linha do tempo.

### Versão 2.0.14
- **Modo Claro Corrigido:** Resolvido o bug onde as telas Petz, Transporte, Desejos e Inicial não acompanhavam a alteração para tema claro (Frosty UI).
- **Lava Aura:** Adicionado efeito especial de lâmpada de lava na logo, na foto de perfil e no ícone do metrô.
- **Widgets Inteligentes:** Os anéis de progresso das métricas da página inicial agora recarregam dinamicamente todas as vezes que o aplicativo retorna à tela.
