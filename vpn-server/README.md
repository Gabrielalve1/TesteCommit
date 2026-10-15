# VPN própria gratuita (WireGuard)

Este diretório contém um instalador mínimo para configurar um servidor WireGuard em uma VPS Ubuntu/Debian que você controla.

## Importante
- O script **não cria uma VPS**. Ele instala o VPN em uma máquina já criada e acessível por SSH.
- GitHub Pages, GitHub Actions e funções serverless não são servidores VPN: não mantêm um túnel UDP persistente nem encaminham o tráfego da forma necessária.
- A hospedagem pode ter plano gratuito, mas depende da disponibilidade, cadastro e regras do provedor. Não há garantia de uma VPS gratuita permanente.
- Não reutiliza credenciais, servidores ou configurações privadas do SocksLite Pro. É uma implementação própria baseada no protocolo aberto WireGuard.
- O tráfego de navegação passará pela VPS; use apenas uma máquina sua e proteja os arquivos de configuração, pois contêm chaves privadas.

## Instalação
1. Crie uma VPS Ubuntu 22.04/24.04 em um provedor que permita uso de VPN e tenha IP público.
2. No painel do provedor, permita tráfego de entrada **UDP 51820** (ou a porta escolhida).
3. Conecte por SSH e envie `install-wireguard.sh` para a VPS.
4. Execute `sudo bash install-wireguard.sh`.
5. O perfil será salvo em `/etc/wireguard/clients/android.conf` (ou nome escolhido).
6. Importe o perfil no app WireGuard oficial para testar. Se o app clone estiver usando outro protocolo, esse perfil não será diretamente compatível sem implementar o cliente WireGuard nele.
7. Teste com um site de consulta de IP antes/depois da conexão e confira `sudo wg show` no servidor.

## Limitações desta etapa
Este repositório é apenas a automação de instalação. Ainda não há VPS provisionada, nem APK reconstruído, nem teste de navegador confirmado.
