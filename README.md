# Tessera 📱✨

> Um dashboard inteligente, imersivo e de alta fidelidade visual construído com **Jetpack Compose**, integrando inteligência artificial local e sincronização avançada de saúde.

---

## 📥 Downloads (APKs)

Os pacotes oficiais prontos para instalação estão disponíveis na pasta `.build-outputs/`:

* **[📥 app-release-1.6.0.apk (Versão Oficial)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-release-1.6.0.apk)**  
  *Versão final de produção. Otimizada (~12.9 MB), com minificação de código (R8/ProGuard), remoção automática de logs de depuração/prints de console e compressão extrema de assets (PNG Crunching).*
* **[📥 app-debug-1.6.0.apk (Versão Debug)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-debug-1.6.0.apk)**  
  *Versão para testes contendo ferramentas de depuração ativas.*

---

## 🚀 Novidades da Versão 1.6.0 ✨
* 🚌 **Meus Ônibus (SPTrans)**: Integração nativa com a API Olho Vivo da SPTrans utilizando autenticação por Cookies (`CookieJar`). Os cartões interativos na aba de Transporte agora exibem suas linhas salvas com previsão de chegada em tempo real na sua parada.
* 🚇 **Metrô e CPTM Redesenhados**: Nova interface integrada de Transportes consolidando os alertas operacionais do Metrô (ARTESP) com painéis horizontais que exibem ícones de atenção baseados na cor real de cada linha.
* 👁️‍🗨️ **Privacidade Financeira Ocular**: O `FinanceWidget` na aba "Hoje" ganhou um botão flutuante de olho (Ocultar/Exibir Valores) que transforma todo o balanço financeiro em asteriscos (`R$ ****`) a um toque, com persistência na memória.
* 🧩 **Edição Modular da Home (Hoje)**: Inclusão do painel de Configurações de Widgets, que permite ligar, desligar e reordenar fisicamente os módulos (Saúde, Metas, Finanças, Pets) da tela inicial ao gosto do usuário.
* 🤖 **Amplo Suporte à IA Local (Gemma)**: O mecanismo autônomo `LocalLLMManager` agora varre as pastas de "Download" e diretórios nativos Android automaticamente para acoplar qualquer modelo `.bin` do Gemma (como `gemma-2b-it-cpu-int4.bin`), tornando o *sideloading* extremamente fácil.
* 🏷️ **Versão 1.6.0**: Incremento oficial do build (`versionCode` 14) e geração de APKs na pasta `.build-outputs/`.

---

## 🚀 Novidades da Versão 1.5.0 ✨
* 🚇 **Painel de Transporte & Roteirizador Integrado**: Novo módulo de Mobilidade Urbana com suporte a dois estados principais:
  * *Estado A (Painel Inicial)*: Painel de status global das linhas de metrô e trem e barra de busca de destinos "Para onde vamos?" estilo Google Maps com autocomplete inteligente.
  * *Estado B (Rota Ativa)*: Divisão de tela de rota conforme o modal. Para ônibus, exibe o Google Maps interativo com a posição do veículo em tempo real e painel com detalhes da viagem e ETA. Para metrô e trem, exibe uma linha do tempo vertical contínua com nós gradientes luminosos e vibrantes (glow), indicando baldeações e progresso em tempo real baseado no GPS.
* ⚽ **Placar de Futebol em Formato de Pílula**:
  * O antigo cartão volumoso de placar de futebol foi substituído por uma **pílula discreta glassmorphic** na tela inicial ("Hoje") que rotaciona os confrontos a cada 6 segundos.
  * Ao clicar na pílula, um pop-up (`Dialog`) detalhado é exibido permitindo alternar de time, consultar próximos confrontos e recarregar os dados.
  * Lógica de mock de futebol refatorada para obter e exibir datas e placares dinâmicos correspondentes aos jogos reais de ontem e próximos dias de forma inteligente.
* 🏷️ **Versão 1.5.0**: Incremento oficial do build (`versionCode` 13) e geração dos APKs correspondentes na pasta `.build-outputs/`.

---

## 🚀 Novidades da Versão 1.4.1 ✨
* 🛍️ **Redesign da Lista de Desejos**: Interface 100% repensada com painel superior glassmorphism exibindo contagem geral de desejos em aberto e segmentação horizontal rolável de itens por categorias estruturadas.
* 🔗 **Link de Compra Obrigatório**: Inclusão de um link de compra (`buyUrl`) persistente no Room para cada meta de desejo, agindo como o destino principal ao clicar nos botões de ação e compra.
* 📆 **Remoção de Prazos**: Limpeza visual e funcional completa do controle e exibição de prazos, simplificando os formulários e layouts do módulo.
* 🎨 **Alternância Cards vs Lista Accordion**: Permite alternar dinamicamente a exibição. No modo Lista, os títulos dos desejos possuem cores dinâmicas baseadas na sua prioridade (Urgente = Vermelho, Moderado = Dourado, Leve = Verde-teal) e exibem seus detalhes (imagem do produto, aporte e compra direta) por meio de um efeito sanfona (accordion) suave.
* 🔍 **Busca de Imagens Pexels Integrada**: Novo modal de pesquisa de imagens diretamente no aplicativo com grade responsiva, seleção de miniatura e suporte a fallback offline inteligente mapeado por termos comuns em português e inglês.
* 🏷️ **Versão 1.4.1**: Incremento oficial do build (`versionCode` 12) e geração dos respectivos APKs na pasta de saídas.

---

## 🚀 Novidades da Versão 1.4.0 ✨
* 💸 **Finanças Inteligente & Minimalista**: Redesenho completo com o card *Ralo-X do Adiantamento* em Liquid Glass e barra neon em degradê progressivo, card reativo de *Atenção ao fluxo* para vencimentos de contas nas próximas 48h, botões rápidos de ação com setas diagonais nativas (rotacionadas a 45 graus) e seção minimalista de *Próximos Débitos*. Remoção do score anterior e gráficos poluídos, mantendo contas e cartões colapsados por padrão.
* 🏠 **Edifício Cilíndrico & Gatos em Órbita**: Nova arquitetura futurista para a tela de Apartamento. Lajes cilíndricas onduladas com pilares finos que surgem de baixo do terreno com efeito de mola elástica super fluido (`elasticOut`). Modelagem de gatos de cerâmica preta brilhante em órbitas espirais e senoidais ao redor do prédio com auras que ativam o brilho amarelo-âmbar pulsante de cada andar.
* 🎛️ **Controle por Gesto em Fio de Ouro**: Substituição do slider clássico por um fio dourado brilhante correndo rente à borda inferior do card de vidro denso, controlado por arraste e toque horizontal na tela com tipografia extra-leve Montserrat.
* 🏷️ **Versão 1.4.0**: Incremento oficial do build (`versionCode` 11) e geração do lote de APKs correspondente.

---

## 🚀 Novidades da Versão 1.3.9 ✨
* 🚇 **Integração da API Pública de Status Metroferroviário**: Conexão à API da ARTESP para buscar em tempo real a situação operacional das linhas de metrô e trem da região metropolitana de São Paulo.
* 🕒 **Configuração de Horários de Alerta**: Nova seção no painel de configurações que possibilita definir múltiplos horários específicos para que a verificação de status metroferroviário seja feita de forma automática.
* 🎛️ **Seleção de Linhas Monitoradas**: Checklist customizado para selecionar exatamente quais linhas (Metrô, CPTM, ViaQuatro, ViaMobilidade, etc.) devem ser exibidas no relatório operacional.
* 🔮 **Pop-up Glassmorphic na HomeScreen**: Diálogo de vidro translúcido responsivo (com desfoque de fundo reativo ao nível global de glassmorphism do app) exibido automaticamente nos horários agendados ou por meio do novo botão de consulta manual no cabeçalho.
* 🏷️ **Versão 1.3.9**: Incremento oficial do build (`versionCode` 10) e geração dos APKs 1.3.9 correspondentes.

---

## 🚀 Novidades da Versão 1.3.8 ✨
* 🎨 **Glassmorphism Global e Inteligente (3 Níveis)**: Criação de um CompositionLocal reativo que propaga o nível de Glassmorphism selecionado globalmente no app. O modificador `PremiumGlassModifier` agora é dinâmico e suporta três estilos de vidro: **Frosted (Concept)** (jateado denso, branco fosco), **Clear (iOS)** (super translúcido, bordas reflexivas cristalinas) e **Blur (One UI)** (equilibrado, blur denso e cor integrada).
* 🌌 **Seletor de Vidros em Configurações**: Adição de um seletor visual na tela de configurações com base na imagem de referência, contendo cards interativos em formato de vidro com previews ao vivo.
* 🔄 **Atualizações de Plano de Fundo em Tempo Real**: Correção no fluxo de atualização do plano de fundo na `SettingsScreen` e `MainActivity`. Agora, ao trocar a imagem na galeria ou clicar em "Restaurar", o plano de fundo é atualizado de forma instantânea em todas as abas sem precisar fechar o app.
* 🏗️ **Apartamento 3D Isométrico Futurista**: Redesenho completo do Canvas do apartamento para renderizar uma simulação de edifício tridimensional em perspectiva isométrica (duas faces texturizadas e sombreadas). Inclusão de partículas com Z-Order pseudo-3D (orbitando atrás e na frente do prédio), anéis holográficos orbitais pontilhados na base, guindaste laser interativo de construção com faíscas neon e telemetria monospace de alta tecnologia em tempo real.
* 🏷️ **Versão 1.3.8**: Incremento oficial do build (`versionCode` 9) e geração do novo lote de APKs 1.3.8.

---

## 🚀 Novidades da Versão 1.3.7 ✨
* 🎨 **Aba Hoje Minimalista & Liquid Glass**: Simplificação do menu flutuante (BottomNavBar) para exatamente 4 botões (remoção do botão de câmera). Remoção da seção de insights contextuais e adição de efeito de desfoque dinâmico (blur) no plano de fundo durante a rolagem. Aplicação global do visual *Liquid Glass* (iOS 27) no painel.
* 🧘 **Redesenho Completo do Foco (Aba Focus)**: Implementação do carrossel horizontal de modos ("✨ Now for you") contendo *Focus Timer*, *Quick Nap* e *Breathing*. Adição da escala horizontal deslizante de minutos (`TimeRuler`), card de soundscape ("Ocean") e modo de foco ativo imersivo (tela cheia com plano de fundo do mar) com suporte a visualizações comum e minimalista (alternados por toque) e exercício de respiração guiado.
* 🏷️ **Versão 1.3.7**: Incremento oficial do build (`versionCode` 8) e atualização dos arquivos binários finais.

---

## 🚀 Novidades da Versão 1.3.6 ✨
* 📦 **Artefatos de distribuição atualizados**: README revisado e APKs disponibilizados na versão 1.3.6 para download direto.
* 🔧 **Versionamento alinhado**: a configuração do app foi atualizada para a versão 1.3.6 e o código interno de versão foi incrementado.

---

## 🚀 Novidades da Versão 1.3.5 ✨
* 📚 **Catálogo Completo de Recursos**: Mapeamento minucioso e documentação de todas as funcionalidades de saúde, finanças, hábitos, pets, compras e segurança.
* 🏷️ **Versão 1.3.5**: Incremento oficial do build (`versionCode` 7) e atualização dos arquivos binários finais.

---

## 🚀 Novidades da Versão 1.3.4 ✨
* 📈 **Insights Relevantes Dinâmicos**: Substituição do placeholder genérico por insights reais gerados a partir do banco de dados do usuário (estatísticas de caminhada ativa calculadas com base em passos reais, quantidade de medicamentos pendentes e taxa de conclusão de rituais). Botões de **Confirm** e **Edit** 100% funcionais.
* 🌌 **Chat Tessera com Fundo Preto**: O fundo do assistente de IA foi alterado para preto puro (`Color.Black`) para máximo contraste e visual imersivo.
* 🔮 **Futuristic Core Orb**: Novo orbe no chat composto por um núcleo centralizado com gradiente cobre/laranja e prateado/branco (estilo vidro 3D), 3 órbitas finas elípticas com elétrons girando ao redor e anéis de pulso de frequência emitidos quando a IA está processando.
* ⚡ **Destaque de Texto via Regex**: Parser robusto de palavras-chave (`rhythm`, `ritmo`, `predictable biology`, `biology`, etc.) que destaca os termos em itálico e negrito, inserindo um ícone de relâmpago (`⚡ `) antes de qualquer menção de ritmo. Atualizado nas respostas offline simuladas da IA.

---

## 🚀 Novidades da Versão 1.3.0 ✨
* 📱 **Refatoração Visual Oura Style (Aba Hoje)**: Implementada uma imagem de fundo de paisagem com desfoque e sobreposição escura em todo o app. Redesenhada a barra superior de forma minimalista com saudação personalizada e foto de perfil à direita. O arco central `HeroMetric` foi recriado para seguir o estilo estrito da Oura (ticks, gradientes, ícone circular centralizado e legendas).
* 🤖 **IA Dinâmica & Fallback Real**: Centralização do ciclo de vida da IA Gemma 4 local dentro do `TesseraViewModel` (evitando recarregamento pesado de memória), integração com a UI e inclusão de um algoritmo de fallback local de alta fidelidade para gerar insights contextuais de saúde, finanças, compras e pets diretamente do banco Room quando a IA estiver offline.
* 💎 **Vidro Líquido (Glassmorphism)**: Substituição dos painéis antigos na Home por uma pilha vertical de cards de insights decorados com o `PremiumGlassModifier` (vidro fosco premium, desfoque e bordas delicadas).
* 🏠 **Visual Global Coeso**: Refatoração completa da tela do apartamento (`ApartmentScreen.kt`) para adotar a imagem de fundo translúcida global, cards em glassmorphism e tipografia sem serifa (`FontFamily.SansSerif`).

---

## 🚀 Novidades da Versão 1.1.0 ✨
* 🌀 **Animações de Scroll Premium**: Adicionamos efeitos dinâmicos de fade-in e fade-out na aba Hoje baseados no posicionamento vertical real de cada widget durante a rolagem.
* ⚡ **Vibe Minimalista/Premium**: Substituímos os antigos emojis por ícones vetoriais refinados e discretos do Material Design, salvando dados textuais e recuperando-os dinamicamente.
* 🩺 **Notificações Exatas e Interativas**: Correção e upgrade do fluxo de alarmes para medicamentos usando alarmes exatos (`setExactAndAllowWhileIdle`), persistência após boot do dispositivo (`BootReceiver`), solicitação de permissão de notificações ativa ao iniciar o app, e botão de ação rápida **"Marcar como tomado"** diretamente no banner pop-up.
* 🗑️ **Exclusão de Medicamentos**: Adicionamos ícones de lixeira e caixa de diálogo (`AlertDialog`) com confirmação de segurança para exclusão direta na aba Saúde.
* 🩺 **Notificações Inteligentes**: Removemos os parênteses vazios indesejados nas mensagens de alerta de medicamentos quando a dosagem está em branco.
* 📱 **Correção de Widgets Android (Glance)**: Corrigimos o travamento infinito de "Carregando..." dos widgets na tela inicial, movendo todas as buscas ao banco de dados Room para a thread de I/O assíncrona (`Dispatchers.IO`).

---

## 🌟 Principais Funcionalidades

### 🤖 1. IA Local & Chat Contextual (Tessera AI)
* **Assistência Local Avançada**: Ciclo de vida integrado da IA local Gemma (2B) gerenciado no ViewModel com carregamento assíncrono inteligente.
* **Algoritmo de Fallback Local**: Caso a IA esteja offline ou não suportada no aparelho, um motor heurístico avançado gera respostas contextuais com base nas tabelas do banco de dados local.
* **Injeção de Contexto de Room**: Dados reais de patrimônio, rituais, lista de compras e status de medicamentos são injetados de forma segura e transparente no prompt para respostas hiper-personalizadas.
* **Orbe Futurista 3D (Core Orb)**: Interface de carregamento do assistente projetada sob medida no Canvas contendo um orbe central cobre/laranja estilo vidro translúcido, cercado por 3 anéis orbitais com elétrons brilhantes ativos e ondas senoidais reativas de ressonância emitidas quando a IA está "pensando".
* **Destaque Dinâmico via Regex**: Parser inteligente que encontra conceitos biológicos e financeiros (como `rhythm`, `ritmo`, `predictable biology`, etc.), aplicando estilo itálico, negrito e adicionando um marcador neon em formato de raio (`⚡`).
* **Reveal Efeito de Digitação**: Animação suave e cadenciada palavra-a-palavra que melhora a legibilidade.
* **Atalhos Rápidos**: Menu de sugestões de perguntas baseadas no contexto de sono, foco, finanças e remédios do usuário.

### 📱 2. Aba Hoje (Painel Principal Circadiano)
* **Design Oura Style Premium**: Interface translúcida sobreposta a um fundo fotográfico com efeito de desfoque atmosférico. Barra superior minimalista contendo saudação dinâmica baseada no horário do dia (Bom dia, Boa tarde, Boa noite, Boa madrugada) e avatar do usuário com borda dourada.
* **Arco Oura central (HeroMetric)**: Canvas circular calibrado que exibe graficamente o índice de prontidão do usuário por meio de múltiplos "ticks", gradiente progressivo de progresso e ícone centralizador.
* **Glow Respirante Circadiano**: Gradiente radial pulsante que altera de cor dinamicamente para refletir o período do dia (alvorecer, meio-dia, crepúsculo e noite).
* **Widgets Glassmorphism**: Cards informativos de alta fidelidade visual que usam o modificador de vidro líquido premium com bordas e desfoque nativos.
* **Scroll Inteligente de GPU**: Animações de entrada e posicionamento calculadas diretamente pela placa de vídeo via modificador `.graphicsLayer` garantindo estabilidade a 120 FPS.
* **Módulo Acalmar a Mente**: Seletor de exercícios de meditação/respiração guiados com player e contagem regressiva reativa.

### 🩺 3. Módulo de Saúde & Integração Health Connect
* **Google Health Connect Integrado**: Fluxo que detecta silenciosamente e sincroniza dados em segundo plano.
* **Recuperação Retroativa de Altura**: Busca em lote no histórico de até 5 anos para obter a última altura registrada no sistema e realizar o cálculo em tempo real do **IMC** (Índice de Massa Corporal), com cartões de classificação coloridos.
* **Registrador Inteligente de Peso**: Mecanismo de redução de duplicados que armazena um novo registro somente sob variação maior que 0.05 kg. Histórico visualizado em um gráfico linear de evolução contendo linha horizontal da meta estipulada pelo usuário.
* **Anel de Passos Neon Glow**: Arco luminoso com múltiplas camadas de glow e cursor com halo azul que se ajusta à meta de passos.
* **Mapeador de Sono**: Registro da qualidade e duração do último período de descanso, associado a um gráfico de barras com a evolução histórica de noites dormidas.
* **Gerenciador de Medicamentos**: Lista de medicamentos cadastrados com dosagem, recorrência e marcação de tomada com confirmação rápida.
* **Alarmes Exatos & Boot Recovery**: Agenda lembretes e notificações exatas no Android (`setExactAndAllowWhileIdle`) persistentes a reinicializações de sistema via receiver de boot (`BootReceiver`).

### 💸 4. Controle Financeiro & Contabilidade Local
* **Painel Patrimonial consolidado**: Resumo dinâmico de receitas, despesas, saldo líquido e patrimônio de todas as contas cadastradas.
* **Pontuação Financeira (Financial Score Ring)**: Gráfico de anel que mede a saúde financeira do usuário relacionando percentualmente a renda ativa contra os gastos correntes.
* **Gráficos de Evolução Suave**: Gráfico de linha que renderiza o fluxo histórico e a evolução das finanças.
* **Gráfico de Pizza das Categorias**: Análise gráfica que separa despesas por finalidade (alimentação, lazer, transporte, saúde, etc.).
* **Carrossel de Cartões de Crédito**: Carrossel 3D exibindo cartões com bandeiras, limites, saldos parciais e designs personalizados baseados em cores hexadecimais.
* **Gestão de Contas Bancárias**: Listagem de saldo individual de contas correntes e poupança.
* **Transações Recorrentes**: Agendamento de receitas e despesas futuras automáticas.
* **Diálogos de Ajuste**: Fluxos modais para inclusão de transações com calendário nativo, cadastro de novos cartões/contas bancárias e conciliação de saldos.

### 🎯 5. Foco, Hábitos (Rituais) & Rotinas Chronos
* **Hábitos Diários (Rituais)**: Cadastro de tarefas com streaks (sequência de dias consecutivos cumpridos) e interruptor rápido de finalização com feedback tátil.
* **Metas de Compras (Desejos)**: Criação de metas financeiras para itens de desejo, com barra de progresso do valor economizado, adição rápida de depósitos e controle de edição.
* **Módulo Chronos**: Sistema de rotinas complexas estruturadas (ex: Rotina da Manhã) divididas em etapas sequenciais com ícones, títulos e durações estimadas.
  * *Player de Rotinas:* Reprodutor interativo que gerencia a transição de etapas, tempo restante com cronômetro interno regressivo ativo, suporte a pause/play e encerramento com animação festiva.
* **Módulo Focus (Pomodoro)**: Cronômetro clássico com tempos de foco e descanso customizáveis.
  * *Som Ambiente Integrado:* Reprodução de sons de relaxamento nativos (chuva, cafeteria, ondas e ruído branco) usando `AudioTrack` para melhor isolamento acústico.
  * *Categorias Customizadas:* Organização de sessões de foco por atividade cadastradas e persistidas localmente.

### 🐾 6. Perfil & Vitalidade Petz
* **Identificação Premium**: Ficha técnica do pet em estilo Glassmorphism monoespaçado contendo sexo, idade calculada dinamicamente com base na data de nascimento, RGA (registro geral do animal) com máscara de 7 dígitos e número do microchip de 15 dígitos.
* **Readiness Ring Animal**: Canvas interativo que mede e exibe o grau de vitalidade do pet de forma visual com gradiente brilhante baseado no status das vacinas e peso ideal.
* **Evolução de Peso do Pet**: Gráfico de histórico de pesagem para acompanhamento veterinário.
* **Timeline de Compromissos**: Histórico e lista linear de lembretes e tarefas voltados ao pet.
* **Agendador de Rotinas do Pet**: Cadastro de horários com o relógio circular analógico `TimePicker` do Material Design 3.
* **Diálogos Sanitários**: Gerenciamento detalhado de medicamentos, datas de vacinas aplicadas e próximas doses pendentes.

### 🛒 7. Módulo de Mercado & Compras
* **Modo Planejamento**: Lista de itens a comprar com quantidade, unidades de medida customizadas (kg, g, unidade, L, ml) e histórico de compras anteriores.
* **Modo Compras (Carrinho)**: Interface otimizada para o momento de ir ao mercado. Permite riscar itens e colocá-los no carrinho, digitar ou ajustar o preço real do produto em tempo real e visualizar o somatório total estimado de forma imediata na barra superior.
* **Finalização de Compra**: Fluxo de checkout que limpa o carrinho e transfere itens comprados para a aba de histórico.

### 🏠 8. Meu Apartamento (Aba Obras)
* **Controle de Evolução**: Painel indicando a porcentagem de conclusão de reformas ou montagem do apartamento, controlado por meio de um slider com persistência em cache SharedPreferences de forma simples e intuitiva.

### ⚙️ 9. Configurações Globais & Segurança
* **Personalização de Fundo**: Troca da imagem de fundo global do app selecionando qualquer arquivo de foto diretamente da galeria de fotos do usuário via `PickVisualMedia`, com opção de restaurar o papel de parede clássico.
* **Segurança Biométrica**: Opção de exigir desbloqueio biométrico (sensor de impressão digital ou reconhecimento facial do Android) na inicialização do aplicativo para proteger dados financeiros e de saúde.
* **Estatísticas e Tamanho do Banco**: Informações detalhadas do banco Room local (conexão ativa, quantidade de tabelas/linhas registradas e tamanho exato ocupado em KB).
* **Seed de Demonstração**: Carrega o aplicativo instantaneamente com dados mockados em todas as telas para visualização completa de gráficos e tabelas de exemplo.
* **Exportação & Importação de Backups**: Permite extrair o banco de dados local `.db` completo para compartilhamento ou backup seguro. O fluxo de restauração limpa arquivos temporários do SQLite (removendo arquivos de log WAL `.db-wal` e `.db-shm` antes da cópia) para prevenir qualquer tipo de corrupção ou perda de dados.

---

## 🛠️ Como Compilar e Buildar

### Pré-requisitos
* **Android SDK** (API level 36, minor level 1)
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
│   ├── app-debug-1.6.0.apk # Versão de testes com logs ativos
│   └── app-release-1.6.0.apk # Versão final de produção assinada
├── app/                  # Módulo principal Android (Código Kotlin & Jetpack Compose)
│   ├── src/main/java/    # Arquivos de código-fonte
│   └── proguard-rules.pro# Regras de minificação R8/ProGuard
├── assets/               # Imagens e recursos estáticos do repositório
├── gradle/               # Configuração e wrappers do Gradle
├── .env.example          # Modelo de configuração de chaves do app
└── settings.gradle.kts   # Definições gerais do projeto Gradle
```
