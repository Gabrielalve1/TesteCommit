# SocksLite Pro — workspace de análise estática

Este diretório prepara um fluxo reproduzível para examinar um APK obtido legalmente e com autorização para análise. O objetivo é documentar evidências técnicas e orientar uma implementação independente — não contornar autenticação, obter credenciais ou acessar servidores sem autorização.

## Estado atual
- [x] Workspace e roteiro de análise criados no GitHub.
- [x] Script de triagem estática incluído.
- [ ] APK obtido e hash registrado.
- [ ] JADX/apktool executados sobre a amostra.
- [ ] Evidências verificadas e relatório preenchido.
- [ ] Testes dinâmicos autorizados, se necessários.

**Transparência:** ainda não foi feita uma descompilação real do SocksLite Pro, pois nenhum APK foi disponibilizado neste ambiente. Nenhuma conclusão sobre classes, endpoints ou implementação interna está confirmada.

## Ferramentas
- Java 17+
- JADX (jadx)
- Apktool (opcional)
- Python 3.10+

## Executar
Linux/macOS: python3 analyze_apk.py /caminho/aplicativo.apk --out ./out

Windows PowerShell: py .\analyze_apk.py C:\caminho\aplicativo.apk --out .\out

O script gera REPORT.md, results.json, logs e a saída do JADX. URLs e nomes encontrados são apenas candidatos e precisam ser revisados manualmente.

## Escopo responsável
Inclui manifesto, permissões, bibliotecas, classes, strings e documentação de fluxos observáveis. Não inclui extração de segredos, bypass de autenticação/certificate pinning ou sondagem de infraestrutura de terceiros. Para testes dinâmicos, use ambiente controlado e serviços próprios ou autorizados.

Consulte ANALYSIS_PLAN.md e REPORT_TEMPLATE.md.
