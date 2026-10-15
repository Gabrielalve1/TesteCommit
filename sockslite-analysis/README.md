# SocksLite Pro — análise estática

Este workspace documenta uma inspeção estática inicial do XAPK versão 50 fornecido pelo usuário. Ele inclui um painel HTML navegável, relatório técnico, plano de análise e script de triagem.

## Acessar o painel

Após habilitar GitHub Pages com a fonte **GitHub Actions** e concluir o deploy, o painel estará disponível em:

https://gabrielalve1.github.io/TesteCommit/

Se o repositório usar um domínio personalizado ou configurações diferentes, consulte Settings → Pages e a URL informada pelo workflow.

## O que já foi feito

- [x] XAPK recebido e estrutura do contêiner inspecionada.
- [x] SHA-256 calculado e registrado no relatório.
- [x] Manifesto do contêiner, splits e permissões listados.
- [x] Strings indicativas e nomes de classes registrados como pistas, não como prova de comportamento.
- [x] Dashboard HTML com busca de permissões, abas e checklist salvo localmente.
- [x] Workflow de deploy GitHub Pages preparado.
- [ ] Descompilação completa via JADX.
- [ ] Análise de fluxo de rede e validação das referências.
- [ ] Testes dinâmicos autorizados em sandbox.

## Relatórios

- `INITIAL_STATIC_REPORT.md` — achados e limitações.
- `ANALYSIS_PLAN.md` — plano de trabalho.
- `REPORT_TEMPLATE.md` — modelo para evidências.
- `analyze_apk.py` — script de triagem para uso local.

## Executar a triagem local

Requisitos: Python 3.10+, Java 17+ e JADX instalado.

```bash
python3 analyze_apk.py /caminho/para/aplicativo.apk --out ./out
```

O script de triagem não substitui revisão manual ou execução controlada. Para esta amostra, a estrutura do XAPK foi inspecionada, mas a descompilação legível com JADX ainda não foi concluída neste ambiente.

## Limites

Este projeto **não é um clone funcional do SocksLite Pro nem um cliente VPN**. Uma VPN utilizável requer implementação própria, servidores autorizados e configuração operacional. A análise não contorna autenticação, não extrai credenciais e não tenta acessar infraestrutura privada.
