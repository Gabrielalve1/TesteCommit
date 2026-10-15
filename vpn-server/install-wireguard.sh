#!/usr/bin/env bash
set -euo pipefail

# Minimal WireGuard server bootstrap for a fresh Ubuntu 22.04/24.04 VPS.
# Run as root: sudo bash install-wireguard.sh
# This configures one client profile and prints a QR code if qrencode is installed.

if [[ "${EUID}" -ne 0 ]]; then echo "Execute como root: sudo bash $0"; exit 1; fi
if ! command -v apt-get >/dev/null 2>&1; then echo "Este script exige Ubuntu/Debian com apt."; exit 1; fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y wireguard qrencode iptables curl

PUB_IP="$(curl -4fsS --max-time 8 https://api.ipify.org || true)"
if [[ -z "$PUB_IP" ]]; then
  read -rp "IP público da VPS: " PUB_IP
fi
read -rp "Porta UDP [51820]: " PORT
PORT="${PORT:-51820}"
read -rp "Nome do primeiro cliente [android]: " CLIENT
CLIENT="${CLIENT:-android}"

WAN_IF="$(ip -o -4 route show to default | awk '{print $5; exit}')"
if [[ -z "$WAN_IF" ]]; then echo "Não consegui detectar a interface de internet."; exit 1; fi

install -d -m 700 /etc/wireguard /etc/wireguard/clients
umask 077
wg genkey | tee /etc/wireguard/server_private.key | wg pubkey > /etc/wireguard/server_public.key
wg genkey | tee "/etc/wireguard/clients/${CLIENT}_private.key" | wg pubkey > "/etc/wireguard/clients/${CLIENT}_public.key"
wg genpsk > "/etc/wireguard/clients/${CLIENT}_preshared.key"

SERVER_PRIV="$(cat /etc/wireguard/server_private.key)"
SERVER_PUB="$(cat /etc/wireguard/server_public.key)"
CLIENT_PRIV="$(cat "/etc/wireguard/clients/${CLIENT}_private.key")"
CLIENT_PUB="$(cat "/etc/wireguard/clients/${CLIENT}_public.key")"
CLIENT_PSK="$(cat "/etc/wireguard/clients/${CLIENT}_preshared.key")"

cat > /etc/sysctl.d/99-wireguard-forward.conf <<'SYSCTL'
net.ipv4.ip_forward=1
net.ipv6.conf.all.forwarding=1
SYSCTL
sysctl --system >/dev/null

cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
Address = 10.66.66.1/24
ListenPort = ${PORT}
PrivateKey = ${SERVER_PRIV}
PostUp = iptables -t nat -A POSTROUTING -o ${WAN_IF} -j MASQUERADE; iptables -A FORWARD -i wg0 -j ACCEPT; iptables -A FORWARD -o wg0 -j ACCEPT
PostDown = iptables -t nat -D POSTROUTING -o ${WAN_IF} -j MASQUERADE; iptables -D FORWARD -i wg0 -j ACCEPT; iptables -D FORWARD -o wg0 -j ACCEPT

[Peer]
PublicKey = ${CLIENT_PUB}
PresharedKey = ${CLIENT_PSK}
AllowedIPs = 10.66.66.2/32
EOF
chmod 600 /etc/wireguard/wg0.conf

cat > "/etc/wireguard/clients/${CLIENT}.conf" <<EOF
[Interface]
PrivateKey = ${CLIENT_PRIV}
Address = 10.66.66.2/32
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = ${SERVER_PUB}
PresharedKey = ${CLIENT_PSK}
Endpoint = ${PUB_IP}:${PORT}
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
EOF
chmod 600 "/etc/wireguard/clients/${CLIENT}.conf"

systemctl enable --now wg-quick@wg0
echo
echo "WireGuard iniciado. Perfil do cliente: /etc/wireguard/clients/${CLIENT}.conf"
echo "IMPORTANTE: libere UDP ${PORT} no firewall da VPS/provedor."
echo "Para mostrar QR no terminal: qrencode -t ansiutf8 < /etc/wireguard/clients/${CLIENT}.conf"
echo "Para verificar: wg show"
