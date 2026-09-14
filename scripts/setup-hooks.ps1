# Configura o Git para utilizar a pasta .githooks compartilhada no repositório
Write-Host "Configurando hooks do Git em .githooks..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -eq 0) {
    Write-Host "Hooks configurados com sucesso! O pre-commit protegerá o repositório contra vazamento de chaves." -ForegroundColor Green
} else {
    Write-Host "Erro ao configurar core.hooksPath no Git." -ForegroundColor Red
}
