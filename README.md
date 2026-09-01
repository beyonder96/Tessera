# Tessera 📱✨

Bem-vindo ao **Tessera**, o seu assistente de estilo de vida completo e hub inteligente!
Acompanhe suas finanças, metas diárias, notícias, saúde e bem-estar em um único lugar.


## 🚀 Novidades da Versão 2.0.45

- **Ajuste Fino no Orçamento Comprometido:** Contabilização de despesas e parcelas futuras com vencimento no mês atual (`dueDate`), contas fixas recorrentes e parcelas de dívidas ativas, mantendo isolamento de faturas cheias de cartão e transferências internas.
- **Filtro Rigoroso no Painel de Parcelados:** Validação estrita de despesas parceladas por subtítulo `Parcela` ou indicador numérico `(X/Y)`, impedindo que lançamentos de extrato contendo datas ou códigos com barra `/` sejam indevidamente listados como parcelas.

## 🚀 Novidades da Versão 2.0.44

- **Classificação e Exclusão em Lote no Extrato Completo:** Modo de seleção múltipla para classificar ou excluir dezenas de transações simultaneamente com estorno automático de saldos e controle inteligente de checkboxes.
- **Extrato Completo Interativo e Edição com 1 Toque:** Toque em qualquer lançamento da lista para abrir o formulário completo de edição (`AddTransactionBottomSheet`), ajustando categoria, valor, datas ou contas.
- **Auto-Categorização Inteligente com IA/Palavras-Chave:** Botão de ação rápida que detecta termos bancários brasileiros (*iFood, Uber, Mercados, Farmácias, Postos, Contas, Assinaturas, Streamings*) e classifica lançamentos pendentes com 1 toque.
- **Chips de Filtro Rápido e Banner de Pendências na Aba Finanças:** Visualização instantânea de lançamentos por categoria ou *"Sem Categoria"*, acompanhado de banner minimalista na tela de Finanças alertando sobre despesas pendentes de classificação no mês.

## 🚀 Novidades da Versão 2.0.43

- **Soma Automática no Modo Mercado (Carrinho):** Correção completa no cálculo e totalizador do Modo Mercado ("NO MERCADO"). Itens adicionados diretamente no carrinho passam a ser contabilizados imediatamente no total, com promoção automática para ativo ao precificar produtos e estabilização de digitação de valores decimais e com vírgula no card interativo.

## 🚀 Novidades da Versão 2.0.42

- **Sistema de Registro de Hidratação com Metas (Aba Nutrição):** Novo módulo completo de consumo de água diária na aba Nutri da Central de Saúde, com cálculo automático de meta diária em ml, barra de progresso em tempo real, botões de registro rápido (+150ml, +250ml, +500ml, +1L e quantidade personalizada), histórico de lançamentos do dia com horário e diálogo intuitivo para personalização de metas.
- **Remoção de Academia da Aba Saúde:** Navegação simplificada e minimalista na Central de Saúde focada nas sub-abas **Geral** e **Nutri**.
- **Categorização Inteligente em Extratos Bancários:** Atribuição e persistência correta de categorias em lançamentos importados de extratos bancários (PDF e CSV) e cálculo preciso do percentual de orçamento comprometido do mês.
- **Alinhamento Visual do Placar de Futebol:** Escudos dos clubes alinhados na mesma linha horizontal do placar, nomes das equipes exibidos por completo e cabeçalho de campeonatos formatado sem cortes.

## 🚀 Novidades da Versão 2.0.41

- **Correção e Aprimoramento na Importação de Extratos em PDF:** Identificação de débitos e créditos com precisão matemática em extratos bancários brasileiros (Itaú, Nubank, Santander, etc.), suporte a detecção de sinais, exclusão estrita de linhas de `SALDO DO DIA`, fechamentos diários e avisos legais, além de extração correta de colunas duplas (valor da transação vs saldo) e novo botão interativo de alternância com 1 toque (`Entrada ⇄` / `Saída ⇄`) no modal de importação.

## 🚀 Novidades da Versão 2.0.40

- **Parcelamento de Fatura no Cartão de Crédito:** Nova funcionalidade de parcelar faturas de cartão de crédito com suporte a entrada opcional (debitada de conta bancária selecionada), definição flexível do valor por parcela ou taxa de juros (% a.m.), quitação da fatura atual e agendamento automático das parcelas futuras.
- **Sincronização em Tempo Real no Widget de Música & Letras:** Tempo de reprodução contínuo em milissegundos, scrubber/slider dinâmico, destaque da estrofe atual e auto-scroll suave acompanhando as letras sincronizadas ao vivo.
- **Sincronização Contínua de Finanças com a Web:** Atualização instantânea do link web compartilhado a cada lançamento, edição ou exclusão no app, e recarga em segundo plano ao retornar para a aba do navegador.

## 🚀 Novidades da Versão 2.0.39

- **Reformulação Visual do Compartilhamento Web de Finanças:** Card do disponível para gastar harmonizado para temas claro e escuro, tipografia editorial moderna, layout responsivo com botões de 36x36px e indicadores visuais de receitas e despesas.

## 🚀 Novidades da Versão 2.0.38

- **Central de Módulos — Joias Cósmicas Minimalistas:** Redesign ultra-minimalista com remoção de contêineres e cards escuros pesados. As joias agora flutuam soltas no centro da tela com o dobro do tamanho (320dp), geometria cristalina pura sem ícones internos, halo luminoso, partículas orbitais e desfoque aprofundado de 48dp na tela de fundo.
- **Correção do Compartilhamento Financeiro (Disponível para Gastar):** Sincronização em tempo real do valor livre no mês (`spendable_balance`) com o Supabase e novo card de prévia detalhado no modal de compartilhamento do app e na página web.
- **Ajustes de Insets e Menu Flutuante na Bíblia Sagrada:** Adicionado `statusBarsPadding()` na barra superior para proteger contra sobreposição com a câmera/notch e barra de notificações. O menu flutuante contextual YouVersion agora fica suspenso elegantemente acima da barra de navegação inferior.
- **Redesign Minimalista em Linha Única do Metrô:** Cards de monitoramento de metrô e CPTM simplificados em uma única linha horizontal com o badge colorido do número da linha, operadora, status ao vivo e horário exato de atualização no formato `HH:mm`.

## 🚀 Novidades da Versão 2.0.37

- **Central de Módulos — Joias do Infinito Cósmicas (Marvel Style):** Novo Hub em carrossel horizontal interativo com 9 Joias geométricas desenhadas em Canvas, física de partículas orbitais, feixes de luz rotativos, levitação gravitacional suave e telemetria ao vivo para cada módulo.
- **Importador Automático de Extratos em PDF:** Leitura e extração local de extratos bancários em PDF com detecção automática de datas, descrições, valores em reais, tipo (receita/despesa), categorização inteligente e seleção de conta.
- **Menu Flutuante iOS Premium Glass Capsule:** Barra inferior de navegação reformulada para o padrão iOS Frosted Glass Capsule (58dp) com borda especular superior de reflexo de luz, pílulas ativas fluidas com física Spring e micro-ponto luminoso.
- **Central da Bíblia Sagrada Completa (BIBLIAAPI v2 & Design YouVersion):** Leitor editorial moderno com seletor de livros/capítulos com busca instantânea, seletor de versões (NVT, NVI, ARA, ACF), marcação de versículos em 5 cores pastéis YouVersion, cópia de citações formatadas e compartilhamento nativo.
- **Correção Solar no Widget de Clima:** Reposicionamento do arco solar para o quadrante direito do card, garantindo 100% de clareza visual e sem sobreposição com os dígitos de temperatura.

## 🚀 Novidades da Versão 2.0.36

- **Integração Wger Workout Manager & Catálogo de Academia:** Catálogo de musculação integrado à API Wger (`wger.de/api/v2`) com busca instantânea, carrossel de grupos musculares (*Peito, Costas, Pernas, Ombros, Braços, Abdômen, Panturrilhas*), fotos de execução, músculos primários/secundários, equipamentos e instruções detalhadas de postura com suporte a funcionamento 100% offline.
- **Tessera Smart Audio Hub & Dashboard de Contexto:** Novo card reativo de mídia na Home escutando ativamente o `MediaSessionManager` do Android (Tidal, Spotify, YouTube Music, reprodutores locais) com controles de playback, barra de progresso linear e painel expansível de dossiê da faixa (letras com anotações editoriais do Genius, ficha técnica de estúdio via MusicBrainz e vídeos complementares do YouTube).
- **Monitoramento ao Vivo de Trens & Metrô (ARTESP / CPTM):** Scraping em tempo real do portal CCM da ARTESP via Jsoup e integração à API oficial da CPTM com cache de 60s, fim dos falsos status normais e suporte a chave de desenvolvedor nas configurações.

## 🚀 Novidades da Versão 2.0.35

- **Reformulação do Menu Flutuante (Liquid Glass Capsule):** Nova barra de navegação translúcida com curvatura acrílica, cápsulas ativas com física de mola elástica, micro-traço de LED nos atalhos, divisor vertical e botão âncora com rotação de 90° para a Central de Módulos.
- **Animações e Status de Transporte Refinados:** Animação procedural `AnimatedMetroTrain` restabelecida na base dos cartões de metrô com layout compacto de status ao lado da operadora e horário em **cilindro acrílico translúcido**, além de nova animação exclusiva `AnimatedBusDrive` para linhas da SPTrans e espaçamento inferior ampliado para rolagem desobstruída.
- **Desduplicação Nativa de Passos no Health Connect:** Implementada agregação oficial do Google Health Connect (`AggregateRequest` com `StepsRecord.COUNT_TOTAL`), eliminando contagens duplicadas entre múltiplos apps de saúde (Google Fit, Samsung Health, sensor nativo) e consolidando histórico diário no fuso local.
- **Diagnóstico e Script SQL para Compartilhamento Web (Supabase):** Disponibilização do schema SQL e instruções de ativação em 1 clique para tempo real nas listas de compras e resumos financeiros.

## 🚀 Novidades da Versão 2.0.34

- **Nova Aba Nutri & Scanner por Câmera:** Introduzida a nova aba Nutri dentro do módulo de Saúde, com integração às APIs Open Food Facts e Edamam, leitor de código de barras físico em tempo real com permissão de câmera, metas de calorias, 4 barras de macronutrientes e registro de refeições.
- **Redesign Enxuto do Transporte SP:** Interface de transporte simplificada, veloz e elegante com métricas em tempo real de Metrô & CPTM e chegadas de ônibus SPTrans com modal de busca ágil.
- **Central de Módulos Bento Grid Moderna:** Reformulação completa do Hub geral de módulos com Bento Grid dinâmico, telemetria em tempo real (saldo financeiro, calorias do dia, rotinas ativas) e microinterações de toque amortecidas.

## 🚀 Novidades da Versão 2.0.33

- **Animação Aerodinâmica de Trens & Filtros de Transporte:** Novo visual moderno e elegante em Canvas para as linhas de trem/metrô, status e horários organizados e chips rápidos de busca de ônibus (Ida, Volta, Noturnas N).
- **Módulos em Bento Grid Minimalista:** Redesenho completo do painel de módulos na Home com grade de 8px, proporções refinadas, subtítulos contextuais funcionais e microinterações de toque.
- **Preenchimento Automático de Passos (Google Health / Connect):** Consulta e preenchimento automático da contagem de passos do dia ao abrir o modal de registro ou alternar datas, com selo de confirmação do Health Connect.
- **Correção de Compartilhamento Web & Modo Claro na Tela de Bloqueio:** Resolução do erro de "Resumo indisponível" no Supabase e suporte total ao modo claro na tela de bloqueio com tipografia sans-serif de alto contraste.

## 🚀 Novidades da Versão 2.0.32

- **Seleção Dupla de Fotos para Pets (Modo Claro & Escuro):** Agora é possível escolher fotos separadas para cada pet (diurna e noturna), adaptando-se instantaneamente à mudança de tema do aplicativo ou sistema.
- **Reformulação Total de Todos os Widgets Externos (Glance):** Widgets da tela inicial do Android redesenhados do zero com formato ultra-compacto (4x1 minimalista), paletas neutras adaptativas e exibição dos **escudos oficiais dos clubes** no widget de futebol.
- **Widget de Clima com Sol em Arco Real:** O Sol agora nasce, atinge o zênite e se põe calculando a posição angular exata conforme os horários reais da API Open-Meteo, com renderização de alto contraste visível tanto no modo claro quanto escuro.
- **Formulário de Finanças no Modo Claro:** Cores e contrastes reformulados com tokens dinâmicos, garantindo total legibilidade dos campos, rótulos e seletores no tema claro.
- **Compartilhamento Web Resiliente (Mercado & Finanças):** Roteamento universal na web (`/market/:id`, `/finance/:id`, query params e hashes), suporte a fallback offline e schema SQL pronto para Supabase.

## 🚀 Novidades da Versão 2.0.31

- **Modo Claro em Finanças, Desejos e Transporte:** Adequação rigorosa ao Design System minimalista em todas as telas com superfícies dinâmicas, alto contraste e suporte perfeito ao tema Claro.
- **Deploy do Portal Web & Correção dos Links:** O compartilhamento de Mercado e Finanças agora aponta diretamente para o portal oficial no Firebase Hosting (`https://tessera-35c54.web.app`), com roteamento SPA completo sem erros 404.
- **Fuso Horário dos Jogos de Futebol:** Conversão precisa de horários UTC para o fuso local do dispositivo.
- **Tabela do Brasileirão:** Temporada atual com fallback dinâmico.
- **Lua e Estrelas no Widget de Clima:** Renderização em alta definição com contraste WCAG AA no Modo Claro.
- **Navegador de Módulos Redesenhado:** Grid minimalista de 2 colunas exibindo apenas o ícone e o nome de cada módulo.

## 🚀 Novidades da Versão 2.0.30

- **Conexão Web em Tempo Real (Supabase):** Compartilhamento online de listas de compras e do dashboard financeiro via links públicos e QR Codes seguros, com visualização reativa instantânea no navegador (mobile e desktop) sem necessidade de instalação do app.
- **Correção da SPTrans (Olho Vivo):** Injeção automática de header `Content-Length: 0` e autenticação com token de produção resiliente.
- **Clima com Ciclo Noturno Realista:** Leitura do parâmetro `is_day` com partículas de estrelas cintilantes (`NIGHT_STARS`) e gradiente térmico noturno azul celestial.
- **Tabela Completa do Brasileirão Série A:** Integração da `TheSportsDbApi` coexistindo com a `ApiFootballData` para exibição dinâmica de todos os 20 clubes com zonas de classificação.
- **Novo Widget de Placar no Android (Glance):** Placar minimalista e adaptativo na tela inicial do dispositivo com status ao vivo.
- **Organização Geral do Repositório:** Limpeza de arquivos temporários, centralização de scripts em `scripts/` e novo `.gitignore` abrangente.

## 🚀 Novidades da Versão 2.0.29

- **Refatoração Completa do Modo Claro (Light Mode):** Todos os componentes, modais, formulários, gráficos e cartões térmicos (`ThermalUI`) foram reestruturados para alternar perfeitamente entre os temas Claro e Escuro, garantindo conformidade total com as diretrizes de minimalismo rigoroso, superfícies neutras de camadas suaves e alto contraste (WCAG AA).
- **Nova API da Bíblia (100% em Português):** Integração com API pública com tradução em português (João Ferreira de Almeida) e fallback offline de versículos do dia.
- **Placar Esportivo com Escudos de Times (TheSportsDB):** Cobertura com logos em alta resolução para todos os clubes do futebol brasileiro (incluindo suporte nativo ao Flamengo) e tabela de classificação dinâmica.
- **Correções de Integridade:** Correção de alarmes de reset de benefícios, estorno de limites em parcelamentos cancelados e checkout do mercado.

## 🚀 Novidades da Versão 2.0.28

- **UI & Pop-ups (Overlay):** O pop-up do Global Overlay Service foi modernizado com bordas arredondadas (60f), tema escuro minimalista, feedback sonoro nativo e roteamento inteligente direto para a aba correspondente ao clicar em "Registrar Agora".
- **Placar Esportivo (Football Scoreboard):** Substituído o renderizador de imagens (`SubcomposeAsyncImage` por `AsyncImage`) para corrigir a falha silenciosa na exibição dos escudos dos times que às vezes apresentava apenas siglas ("FC") ao invés dos logos reais. Incluído fallback inteligente e log de erros para monitoramento.

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

[![Baixar APK Debug](https://img.shields.io/badge/Download-APK_Debug-green?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-debug-2.0.45.apk)
[![Baixar APK Release](https://img.shields.io/badge/Download-APK_Release-blue?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-release-2.0.45.apk)

---
Tessera. Seu guia, em toda a linha do tempo.

### Versão 2.0.14
- **Modo Claro Corrigido:** Resolvido o bug onde as telas Petz, Transporte, Desejos e Inicial não acompanhavam a alteração para tema claro (Frosty UI).
- **Lava Aura:** Adicionado efeito especial de lâmpada de lava na logo, na foto de perfil e no ícone do metrô.
- **Widgets Inteligentes:** Os anéis de progresso das métricas da página inicial agora recarregam dinamicamente todas as vezes que o aplicativo retorna à tela.
