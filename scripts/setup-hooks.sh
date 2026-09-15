#!/usr/bin/env bash
# Configura o Git para utilizar a pasta .githooks compartilhada no repositório
echo "Configurando hooks do Git em .githooks..."
chmod +x .githooks/*
git config core.hooksPath .githooks
echo "Hooks configurados com sucesso! O pre-commit protegerá o repositório contra vazamento de chaves."
