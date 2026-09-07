#!/usr/bin/env python3
import os
import sys
import json
import argparse
import urllib.parse
import urllib.request

def get_github_token():
    # 1. Try env vars
    token = os.environ.get('GITHUB_TOKEN') or os.environ.get('GH_TOKEN')
    if token:
        return token

    # 2. Try ~/.git-credentials
    cred_path = os.path.expanduser('~/.git-credentials')
    if os.path.exists(cred_path):
        with open(cred_path, 'r') as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                parsed = urllib.parse.urlparse(line)
                if 'github.com' in parsed.netloc or 'github.com' in parsed.path:
                    if parsed.password:
                        return parsed.password
                    if parsed.username and len(parsed.username) > 30:
                        return parsed.username
    return None

def extract_latest_notes_from_readme(readme_path, version_tag):
    clean_version = version_tag.lstrip('v')
    target_header = f"## 🚀 Novidades da Versão {clean_version}"
    if not os.path.exists(readme_path):
        return f"Release {version_tag} do aplicativo Tessera."

    with open(readme_path, 'r', encoding='utf-8') as f:
        content = f.read()

    if target_header in content:
        parts = content.split(target_header, 1)[1]
        notes = parts.split("## 🚀", 1)[0].split("### 📥", 1)[0].strip()
        return f"## 🚀 Novidades da Versão {clean_version}\n\n{notes}"

    return f"Release {version_tag} do aplicativo Tessera."

def main():
    parser = argparse.ArgumentParser(description="Publicar release oficial do Tessera no GitHub")
    parser.add_argument("--version", required=True, help="Tag da versão (ex: v2.0.58)")
    parser.add_argument("--title", help="Título da release")
    parser.add_argument("--repo", default="beyonder96/Tessera", help="Repositório no formato owner/repo")
    args = parser.parse_args()

    version_tag = args.version if args.version.startswith('v') else f"v{args.version}"
    clean_version = version_tag.lstrip('v')
    title = args.title or f"{version_tag} - Release Oficial"

    token = get_github_token()
    if not token:
        print("Erro: Nenhum token do GitHub encontrado em ~/.git-credentials ou GITHUB_TOKEN.")
        sys.exit(1)

    headers = {
        'Authorization': f'Bearer {token}',
        'Accept': 'application/vnd.github+json',
        'User-Agent': 'Tessera-Release-Automation'
    }

    readme_path = os.path.join(os.getcwd(), "README.md")
    notes_body = extract_latest_notes_from_readme(readme_path, version_tag)
    notes_body += f"\n\n### 📥 Downloads\n- **`app-release-{clean_version}.apk`**: Versão de produção recomendada para instalação.\n- **`app-debug-{clean_version}.apk`**: Versão de desenvolvimento e depuração.\n"

    # 1. Create or Update Release
    release_url = f"https://api.github.com/repos/{args.repo}/releases"
    payload = {
        "tag_name": version_tag,
        "target_commitish": "main",
        "name": title,
        "body": notes_body,
        "draft": False,
        "prerelease": False,
        "make_latest": "true"
    }

    print(f"Criando/atualizando release {version_tag} no GitHub...")
    req = urllib.request.Request(
        release_url,
        data=json.dumps(payload).encode('utf-8'),
        headers={**headers, 'Content-Type': 'application/json'}
    )

    try:
        with urllib.request.urlopen(req) as resp:
            release_data = json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        if e.code == 422:
            print(f"Release {version_tag} já existe. Buscando informações existentes...")
            get_req = urllib.request.Request(
                f"https://api.github.com/repos/{args.repo}/releases/tags/{version_tag}",
                headers=headers
            )
            with urllib.request.urlopen(get_req) as resp:
                release_data = json.loads(resp.read().decode('utf-8'))
        else:
            print("Erro ao criar release:", e.code, e.read().decode('utf-8'))
            sys.exit(1)

    release_id = release_data.get('id')
    html_url = release_data.get('html_url')
    upload_url_template = release_data.get('upload_url', '')
    upload_url_base = upload_url_template.split('{')[0]

    print(f"Release pronta: {html_url} (ID: {release_id})")

    # 2. Upload APK assets
    outputs_dir = os.path.join(os.getcwd(), ".build-outputs")
    assets = [
        os.path.join(outputs_dir, f"app-release-{clean_version}.apk"),
        os.path.join(outputs_dir, f"app-debug-{clean_version}.apk")
    ]

    for asset_path in assets:
        if not os.path.exists(asset_path):
            print(f"Aviso: Arquivo {asset_path} não encontrado!")
            continue

        asset_name = os.path.basename(asset_path)
        asset_size = os.path.getsize(asset_path)
        print(f"Enviando asset {asset_name} ({asset_size / (1024*1024):.1f} MB)...")

        upload_req = urllib.request.Request(
            f"{upload_url_base}?name={asset_name}",
            data=open(asset_path, 'rb').read(),
            headers={
                **headers,
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Length': str(asset_size)
            }
        )

        try:
            with urllib.request.urlopen(upload_req) as resp:
                asset_resp = json.loads(resp.read().decode('utf-8'))
                print(f"✓ {asset_name} enviado: {asset_resp.get('browser_download_url')}")
        except urllib.error.HTTPError as e:
            print(f"Aviso ao enviar {asset_name}:", e.code, e.read().decode('utf-8'))

    print(f"\nSucesso! A versão {version_tag} está publicada como Latest no GitHub:")
    print(html_url)

if __name__ == '__main__':
    main()
