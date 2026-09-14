import os
import json
import urllib.request
import urllib.error
import subprocess

def run_audit():
    api_key = os.environ.get("OPENROUTER_API_KEY", "").strip()
    if not api_key:
        report = "⚠️ **Aviso:** O secret com a chave do OpenRouter (`OPENROUTER`, `OPENROUTER_API_KEY` ou `LLM_API_KEY`) não foi encontrado nos Secrets do GitHub."
    else:
        try:
            # Coleta lista de arquivos relevantes do repositório
            raw_files = subprocess.check_output(["git", "ls-files"]).decode("utf-8", errors="ignore").splitlines()
            files = [f for f in raw_files if not f.startswith(".idea/") and not f.startswith(".gradle/")][:80]
            files_summary = "\n".join(files)

            prompt_user = f"""Analise a estrutura de arquivos e arquitetura deste repositório (Tessera - Aplicativo Android Kotlin / Jetpack Compose):

Arquivos do projeto:
{files_summary}

Gere um relatório de auditoria técnica para a equipe de desenvolvimento contendo:
1. 🏗️ **Arquitetura & Organização**: Avaliação da modularização, separação de camadas e pastas.
2. ⚡ **Boas Práticas & Código**: Recomendações para Kotlin, Jetpack Compose e UI responsiva.
3. 🔒 **Segurança & Dependências**: Boas práticas de credenciais, dependências e build.
4. 🎯 **Checklist de Próximos Passos**: 3 a 5 tarefas acionáveis e prioritárias para implementar."""

            payload = {
                "model": "openrouter/free",
                "messages": [
                    {
                        "role": "system",
                        "content": "Você é um arquiteto e auditor de software sênior especializado em Android, Kotlin e engenharia de software móvel. Responda em português com formatação Markdown clara, profissional e prática."
                    },
                    {
                        "role": "user",
                        "content": prompt_user
                    }
                ]
            }

            req = urllib.request.Request(
                "https://openrouter.ai/api/v1/chat/completions",
                data=json.dumps(payload).encode("utf-8"),
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://github.com/beyonder96/Tessera",
                    "X-Title": "Tessera AI Auditor"
                }
            )

            print("Enviando requisição ao OpenRouter...")
            with urllib.request.urlopen(req, timeout=120) as resp:
                res = json.loads(resp.read().decode("utf-8"))
                report = res["choices"][0]["message"]["content"]
                print("Relatório gerado com sucesso pela IA!")
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8", errors="ignore")
            print(f"Erro HTTP OpenRouter: {e.code} - {err_body}")
            report = f"⚠️ **Erro na requisição ao OpenRouter (HTTP {e.code}):**\n```json\n{err_body}\n```"
        except Exception as e:
            print(f"Erro geral: {e}")
            report = f"⚠️ **Erro ao executar análise da IA:** {e}"

    # Grava no GITHUB_ENV para uso do create-an-issue
    delimiter = "EOF_REPORT_ENV"
    github_env = os.environ.get("GITHUB_ENV")
    if github_env:
        with open(github_env, "a", encoding="utf-8") as f:
            f.write(f"REPORT<<{delimiter}\n{report}\n{delimiter}\n")
    else:
        print("GITHUB_ENV não definido (execução local). Relatório:")
        print(report)

if __name__ == "__main__":
    run_audit()
