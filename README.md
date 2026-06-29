# Tessera 📱✨

Bem-vindo ao **Tessera**, o seu assistente de estilo de vida completo e hub inteligente! 
Acompanhe suas finanças, transporte público (SPTrans), metas diárias, notícias e bem-estar em um único lugar.

## 🚀 Novidades da Versão 1.7.3
- **Correções na SPTrans:** O ponto de ônibus mais próximo agora usa a localização real forçada (`getCurrentLocation`). Linhas sem paradas da API Olho Vivo não desaparecem mais da aba "Meus Ônibus". Corrigido bug de parsing.
- **Foco e Rotinas:** Zenith Hub adicionado. Novo cronômetro circular e microanimações incorporadas.

---

## 📥 Download e Instalação

Você pode baixar e instalar a nova versão do aplicativo diretamente no seu celular clicando nos botões abaixo (dependendo da sua necessidade):

[![Download Versão RELEASE](https://img.shields.io/badge/Download_RELEASE-Versão_1.7.3-brightgreen?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/releases/latest/download/app-release-1.7.3.apk)
*(Recomendado para o dia a dia, mais rápido e otimizado)*

[![Download Versão DEBUG](https://img.shields.io/badge/Download_DEBUG-Versão_1.7.3-blue?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/releases/latest/download/app-debug-1.7.3.apk)
*(Recomendado para testes, exibe mais logs em caso de erros)*

### 🛡️ Play Protect e Segurança (Aplicativo Desconhecido)
Como este aplicativo é distribuído fora da Google Play Store e nós, como desenvolvedores, ainda não pagamos a licença de desenvolvedor oficial do Google Play, a ferramenta **Google Play Protect** exibirá um aviso de que "O Desenvolvedor não é reconhecido" e pode bloquear a instalação. **Isso é completamente normal para aplicativos baixados fora da loja.**

**O que fazer para instalar:**
1. Ao tentar instalar o arquivo `.apk`, o Play Protect abrirá uma tela de aviso vermelha ou branca dizendo "Aplicativo desconhecido bloqueado".
2. Clique no botão **"Mais Detalhes"** (ou "More details") que aparece nessa mensagem de aviso.
3. Em seguida, clique na opção **"Instalar assim mesmo"** (ou "Install anyway") que aparecerá na tela.
4. (Opcional) Se o seu celular pedir para enviar o app para verificação do Google, você pode clicar em "Não enviar".
5. O aplicativo será instalado com sucesso! Nossas permissões (como o Health Connect) são 100% locais e seguras.

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
