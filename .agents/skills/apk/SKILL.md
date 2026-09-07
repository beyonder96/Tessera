---
name: apk
description: Gera APKs debug e release, incrementa versão, atualiza README, faz commit, cria tag e publica Release Oficial no GitHub com os APKs como assets.
---

# Comando /apk - Release e Empacotamento do Tessera

Este fluxo automatiza 100% o ciclo de lançamento de uma nova versão do aplicativo Android do Tessera, incluindo build dos APKs, versionamento, documentação e publicação oficial no GitHub Releases.

## Etapas Obrigatórias de Execução

### 1. Identificar e Incrementar Versão
1. Leia `app/build.gradle.kts` e localize:
   - `versionCode` (ex: `110`)
   - `versionName` (ex: `"2.0.57"`)
2. Calcule a próxima versão incrementando apenas o patch:
   - `versionCode` = `versionCode + 1` (ex: `111`)
   - `versionName` = patch + 1 (ex: `"2.0.58"`)
3. Atualize o arquivo `app/build.gradle.kts` com os novos valores.

### 2. Compilar APKs
Execute o comando de build no terminal:
```bash
./gradlew assembleDebug assembleRelease
```
*Observação: As tasks `copyDebugApk` e `copyReleaseApk` configuradas no Gradle copiarão automaticamente os APKs gerados para `.build-outputs/app-debug-<versao>.apk` e `.build-outputs/app-release-<versao>.apk`.*

### 3. Atualizar README.md
1. Adicione a seção `## 🚀 Novidades da Versão <versao>` no topo das notas de versão em `README.md` resumindo as alterações e melhorias da release.
2. Atualize os links de download no rodapé de `README.md`:
   ```markdown
   [![Baixar APK Debug](https://img.shields.io/badge/Download-APK_Debug-green?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-debug-<versao>.apk)
   [![Baixar APK Release](https://img.shields.io/badge/Download-APK_Release-blue?style=for-the-badge&logo=android)](https://github.com/beyonder96/Tessera/raw/main/.build-outputs/app-release-<versao>.apk)
   ```

### 4. Git Commit e Tag
1. Adicione todas as mudanças e remova os APKs da versão anterior:
   ```bash
   git add .
   git commit -m "chore(release): v<versao>"
   git tag -a "v<versao>" -m "Release v<versao>"
   ```
2. Envie o commit e a tag para o GitHub:
   ```bash
   git push origin main --tags
   ```

### 5. Publicar Release Oficial no GitHub (Latest)
Execute o script auxiliar para registrar a Release na aba Releases do GitHub com o selo verde **Latest** e fazer o upload dos APKs:
```bash
python3 .agents/skills/apk/scripts/publish_release.py --version "v<versao>"
```

### 6. Relatório Final
Apresente ao usuário:
- Versão gerada (ex: `v2.0.58`);
- Link direto da release no GitHub (`https://github.com/beyonder96/Tessera/releases/tag/v<versao>`);
- Links diretos para download dos APKs (Debug e Release).
