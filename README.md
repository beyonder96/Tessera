# Tessera 📱✨

Bem-vindo ao **Tessera**, o seu assistente de estilo de vida completo e hub inteligente! 
Acompanhe suas finanças, transporte público (SPTrans), metas diárias, notícias e bem-estar em um único lugar.

## 🚀 Novidades da Versão 1.6.9
- **Finanças Inteligentes:** Correção no cálculo de orçamentos comprometidos e restabelecimento imediato de limites de cartões de crédito.
- **Transporte Público Real (SPTrans):** Integração 100% oficial com a API Olho Vivo. O app agora usa o seu GPS para encontrar o ponto de ônibus mais próximo da sua localização e traz a previsão exata de chegada do ônibus.
- **Integração de Notícias:** Now Briefing animado e funcional na tela inicial.

---

## 📥 Download e Instalação

Você pode baixar e instalar o aplicativo diretamente no seu celular clicando no botão abaixo:

[![Download APK](https://img.shields.io/badge/Download_APK-Versão_1.6.9-brightgreen?style=for-the-badge&logo=android)](https://github.com/aistudio/tessera/releases/latest/download/app-release.apk)
*(Nota: Substitua o link acima pelo link de release do seu repositório GitHub caso faça upload online)*

### 🛡️ Sobre Segurança e o Google Play Protect
Ao instalar o APK diretamente fora da Google Play Store, o **Google Play Protect** pode emitir um aviso padrão de "Aplicativo Desconhecido". 
Para validar a segurança e instalar sem problemas:

1. Baixe o arquivo `.apk` pelo link acima (ou transfira do Android Studio para o seu celular).
2. Ao abrir o arquivo, o sistema pedirá permissão para **"Instalar de fontes desconhecidas"**. Autorize o seu navegador ou gerenciador de arquivos.
3. Se o **Play Protect** bloquear a instalação exibindo uma tela vermelha ou aviso, clique em **"Mais detalhes"** (More details) e em seguida clique em **"Instalar assim mesmo"** (Install anyway).
4. O Tessera foi assinado digitalmente e todas as suas permissões (GPS, Saúde) são solicitadas dinamicamente apenas quando você tenta utilizá-las, respeitando todas as políticas de segurança modernas do Android 14+.

---

## 🛠️ Como compilar você mesmo (Build Local)
Se você tem o código fonte aberto no seu Android Studio:
1. Vá no menu superior em `Build > Generate Signed Bundle / APK...`
2. Selecione `APK` e utilize a chave de assinatura de release (ou a debug padrão).
3. Transfira o arquivo gerado na pasta `/app/build/outputs/apk/` para o seu celular.

## 🔗 APIs Integradas
- **SPTrans (Olho Vivo):** Requer chave de acesso configurada em `SPTransApi.kt`.
- **NewsAPI:** Requer chave de acesso configurada na view de notícias.
- **Health Connect:** Integração nativa de saúde (passos, sono, peso).
