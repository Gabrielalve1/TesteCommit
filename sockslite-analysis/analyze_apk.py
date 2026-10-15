#!/usr/bin/env python3
"""Static APK triage. Requires JADX; Apktool is optional."""
import argparse, hashlib, json, os, re, shutil, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

URL_RE = re.compile(r"(?i)\b(?:https?://|wss?://)[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+")
DOMAIN_RE = re.compile(r"(?i)\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+(?:com|net|org|io|app|dev|co|br|info|xyz|cloud|me|gg|tv|site|online)\b")
PERM_RE = re.compile(r'android:name="([^"]+)"')
TERMS = ["okhttp", "retrofit", "ktor", "volley", "websocket", "socket", "ssh", "tls", "ssl", "vpnservice", "proxy", "wireguard", "openvpn", "trustmanager", "certificatepinning", "javax.net.ssl"]

def run(cmd, log):
    try:
        p = subprocess.run(cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=1800)
        log.write_text(p.stdout, encoding="utf-8", errors="replace")
        return p.returncode
    except FileNotFoundError:
        log.write_text("Comando não encontrado: " + cmd[0], encoding="utf-8")
        return 127
    except subprocess.TimeoutExpired:
        log.write_text("Tempo limite de 30 minutos excedido.", encoding="utf-8")
        return 124

def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def listing(items):
    return "\n".join("- " + str(x) for x in items) or "- Nenhum resultado encontrado automaticamente."

def main():
    ap = argparse.ArgumentParser(description="Triagem estática de APK")
    ap.add_argument("apk", help="APK que você tem autorização para analisar")
    ap.add_argument("--out", default="analysis-out")
    args = ap.parse_args()
    apk = Path(args.apk).expanduser().resolve()
    out = Path(args.out).expanduser().resolve()
    if not apk.is_file() or apk.suffix.lower() != ".apk":
        print("Informe um arquivo .apk existente.", file=sys.stderr)
        return 2
    jadx = shutil.which("jadx")
    if not jadx:
        print("JADX não encontrado no PATH. Instale JADX e tente novamente.", file=sys.stderr)
        return 3
    out.mkdir(parents=True, exist_ok=True)
    meta = {"sample": apk.name, "size_bytes": apk.stat().st_size, "sha256": sha256(apk), "analyzed_at_utc": datetime.now(timezone.utc).isoformat(), "tools": {}, "permissions": [], "url_candidates": [], "domain_candidates": [], "network_indicators": {}, "limitations": []}
    src = out / "jadx"
    rc = run([jadx, "-d", str(src), "--deobf", str(apk)], out / "jadx.log")
    meta["tools"]["jadx"] = {"path": jadx, "exit_code": rc}
    apktool = shutil.which("apktool")
    decoded = out / "apktool"
    if apktool:
        rc2 = run([apktool, "d", "-f", str(apk), "-o", str(decoded)], out / "apktool.log")
        meta["tools"]["apktool"] = {"path": apktool, "exit_code": rc2}
    else:
        decoded = None
        meta["tools"]["apktool"] = "not installed"
    urls, domains, perms = set(), set(), set()
    indicators = {term: [] for term in TERMS}
    text_files = []
    for root, _, names in os.walk(src):
        for name in names:
            if name.endswith((".java", ".kt", ".xml", ".txt")):
                p = Path(root) / name
                try:
                    content = p.read_text(encoding="utf-8", errors="ignore")
                except OSError:
                    continue
                text_files.append((p, content))
                urls.update(u[:500] for u in URL_RE.findall(content))
                domains.update(d.lower() for d in DOMAIN_RE.findall(content))
                lower = content.lower()
                for term in TERMS:
                    if term in lower:
                        indicators[term].append(str(p.relative_to(out)))
    manifests = ([decoded / "AndroidManifest.xml"] if decoded else [])
    manifests += [p for p, _ in text_files if p.name == "AndroidManifest.xml"]
    for manifest in manifests:
        if manifest.exists():
            perms.update(PERM_RE.findall(manifest.read_text(encoding="utf-8", errors="ignore")))
    meta["permissions"] = sorted(perms)
    meta["url_candidates"] = sorted(urls)
    meta["domain_candidates"] = sorted(domains)
    meta["network_indicators"] = {k: sorted(set(v))[:100] for k, v in indicators.items() if v}
    meta["limitations"] = ["Strings/URLs são candidatos e não provam uso efetivo.", "Ofuscação, código nativo, strings cifradas e lógica remota podem não aparecer.", "Nenhuma sondagem de servidores, extração de credenciais ou tentativa de contornar proteções foi feita."]
    (out / "results.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False), encoding="utf-8")
    report = "# Relatório de triagem estática\n\n"
    report += "## Amostra\n- Arquivo: " + meta["sample"] + "\n"
    report += "- Tamanho: " + str(meta["size_bytes"]) + " bytes\n"
    report += "- SHA-256: " + meta["sha256"] + "\n"
    report += "- Data UTC: " + meta["analyzed_at_utc"] + "\n\n"
    report += "## Permissões\n" + listing(meta["permissions"]) + "\n\n"
    report += "## URLs candidatas\n" + listing(meta["url_candidates"]) + "\n\n"
    report += "## Domínios candidatos\n" + listing(meta["domain_candidates"]) + "\n\n"
    report += "## Indicadores de rede/protocolo\n"
    for term, paths in meta["network_indicators"].items():
        report += "\n### " + term + "\n" + listing(paths) + "\n"
    report += "\n## Limitações\n" + listing(meta["limitations"]) + "\n"
    (out / "REPORT.md").write_text(report, encoding="utf-8")
    print("Relatório criado em:", out / "REPORT.md")
    print("SHA-256:", meta["sha256"])
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
