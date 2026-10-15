# SocksLite Pro — relatório inicial de análise estática

**Status:** triagem estática inicial concluída sobre o XAPK fornecido pelo usuário.  
**Data da análise:** 2026-10-15  
**Versão declarada:** 50 (version_code=50)  
**Pacote:** com.sockslitepro.net

## Integridade e conteúdo do pacote

- Arquivo recebido: Sockslite+Pro+-+VPN_50_APKPure.xapk
- Tamanho: 91.723.623 bytes
- SHA-256 calculado localmente: 85a3419168d9cd18fa6eafd79fcb86c0f5caf00d8dac6e45dc70c5d5710b4b49
- O XAPK é um contêiner ZIP e inclui:
  - com.sockslitepro.net.apk (base, 77.054.883 bytes)
  - config.arm64_v8a.apk (14.050.893 bytes)
  - config.en.apk
  - config.mdpi.apk
  - manifest.json e ícone
- O manifesto do XAPK declara min_sdk_version=32 e target_sdk_version=36.
- O APK base contém 9 arquivos DEX, incluindo um DEX em assets/.ucvyxzft.dex. O nome incomum sugere que vale investigar carregamento dinâmico/empacotamento, mas **não prova** por si só ofuscação maliciosa ou comportamento malicioso.
- Não foi encontrado arquivo .so diretamente na listagem do APK base. Isso não exclui código nativo em outros splits ou carregamento dinâmico.

## Permissões declaradas no manifesto do XAPK

Entre as permissões declaradas estão:

- Rede: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE
- Estado do telefone: READ_PHONE_STATE
- Serviço em primeiro plano: FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE
- Execução/energia: WAKE_LOCK, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- Notificações e vibração: POST_NOTIFICATIONS, VIBRATE
- Armazenamento legado: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
- Biometria: USE_BIOMETRIC, USE_FINGERPRINT
- Cobrança/licença e publicidade: com.android.vending.BILLING, CHECK_LICENSE, com.google.android.gms.permission.AD_ID
- Recebimento de mensagens do Firebase: com.google.android.c2dm.permission.RECEIVE
- Solicitação de exclusão de pacotes: REQUEST_DELETE_PACKAGES

A presença de uma permissão não demonstra que ela seja usada em todos os fluxos nem que exista abuso; é necessário relacioná-la ao código e ao comportamento observado.

## Componentes e funcionalidades sugeridos por nomes de classes/strings

A triagem de strings dos DEX encontrou referências a:

- com.sockslitepro.net.MainActivity
- com.sockslitepro.net.activity.ServersActivity
- com.sockslitepro.net.activity.ImportFileActivity
- com.sockslitepro.net.activity.GoogleSignInActivity
- com.sockslitepro.net.service.SocksVpnService
- com.sockslitepro.net.service.NetSpeedService
- com.sockslitepro.net.service.tunnel.vpn.TProxyService
- com.trilead.ssh2 e identificadores de protocolo SSH
- com.efvpn.tunnel.EFVpnNative
- Ações de serviço CONNECT, DISCONNECT e RECONNECT

Esses nomes são indícios fortes de que o app implementa fluxo VPN/túnel e suporte SSH. A extração de strings, isoladamente, não revela o protocolo completo nem a lógica operacional.

## Endpoints encontrados como strings candidatas

Strings que citam o domínio do próprio produto:

- https://locale.sockslite.com/geoip
- https://metrics.sockslite.com

A existência dessas strings não prova quando são chamadas, quais dados são enviados, ou se todos os caminhos são ativos. Isso exigiria descompilação legível e/ou observação dinâmica autorizada.

Também foram encontradas URLs associadas a SDKs de anúncios, métricas e diagnóstico, incluindo domínios de Google/AdMob, AppLovin, InMobi, Vungle, Unity mediation, Pangle e Sentry. Como muitos desses endereços vêm de bibliotecas de terceiros, não devem ser confundidos automaticamente com endpoints próprios do SocksLite Pro.

## Bibliotecas/tecnologias sugeridas por strings

- SSH: referências a com.trilead.ssh2, algoritmos ssh-rsa, ssh-ed25519 e outros identificadores de protocolo.
- Rede HTTP/WebSocket: strings compatíveis com OkHttp sombreado sob com.applovin.shadow.okhttp3; parte pode pertencer ao SDK de anúncios.
- Firebase/Google Play Services e várias integrações de monetização/publicidade.
- Sentry/Crashlytics e endpoints de métricas/diagnóstico.

## Limitações desta etapa

- Esta etapa foi uma inspeção de estrutura ZIP, metadados do XAPK e strings imprimíveis dos DEX.
- O ambiente não tem JADX, apktool ou Android SDK build-tools disponíveis, então **ainda não foi produzida uma descompilação Java/Kotlin legível nem uma árvore completa de componentes do AndroidManifest.xml**.
- Não foi executado o app, nem feita captura de tráfego, teste de segurança, análise de comportamento em runtime ou verificação antimalware independente.
- O resultado não é uma certificação de segurança. SHA-256 serve para identificar exatamente o arquivo analisado, não para atestar que ele seja seguro.

## Próximos passos recomendados

1. Descompilar o APK base com JADX em ambiente isolado e revisar as classes próprias do pacote.
2. Decodificar o AndroidManifest.xml binário para confirmar activities, services, receivers, providers e permissões efetivas.
3. Mapear chamadas de rede a partir do código de primeira parte e separar claramente chamadas dos SDKs de terceiros.
4. Se houver autorização, executar em emulador/sandbox com dados de teste e observar conexões de rede sem usar credenciais reais.
5. Documentar conclusões com evidências reproduzíveis, mantendo hipóteses separadas de fatos confirmados.

## Uso responsável

Este relatório documenta apenas inspeção estática inicial. Não inclui credenciais, chaves, dados de usuários ou instruções para acessar servidores privados. Para criar um app próprio inspirado em funcionalidades gerais de VPN/SSH, implemente um cliente independente e use servidores e configurações autorizados, sem copiar código proprietário nem contornar autenticação.
