#!/bin/bash
set -e

# ─────────────────────────────────────────────────────────────────────────────
# init-ssl.sh  — run ONCE on the server to obtain the Let's Encrypt certificate
#
# Prerequisites:
#   1. DNS A-record:  n-balance.site  →  <your-server-IP>
#   2. docker compose is installed and .env has DOMAIN=n-balance.site
#   3. Ports 80 and 443 are open in the DigitalOcean firewall
#
# Usage:
#   chmod +x init-ssl.sh
#   ./init-ssl.sh
# ─────────────────────────────────────────────────────────────────────────────

DOMAIN=$(grep '^DOMAIN=' .env 2>/dev/null | cut -d= -f2 | tr -d '[:space:]')

if [ -z "$DOMAIN" ]; then
    echo "ERROR: DOMAIN not set in .env"
    echo "Add this line to .env:  DOMAIN=n-balance.site"
    exit 1
fi

EMAIL="admin@${DOMAIN}"
CERT_DIR="./nginx/certbot/conf/live/${DOMAIN}"

echo "============================================"
echo " NeuroBalance HTTPS Setup"
echo " Domain : ${DOMAIN}"
echo " Email  : ${EMAIL}"
echo "============================================"
echo ""

# ── Step 1: temporary self-signed cert so nginx can start ────────────────────
echo "[1/4] Creating temporary self-signed certificate..."
mkdir -p "${CERT_DIR}"
openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout "${CERT_DIR}/privkey.pem" \
    -out    "${CERT_DIR}/fullchain.pem" \
    -subj   "/CN=${DOMAIN}" 2>/dev/null
cp "${CERT_DIR}/fullchain.pem" "${CERT_DIR}/chain.pem"
echo "Done."

# ── Step 2: start nginx only (other services not needed for challenge) ────────
echo ""
echo "[2/4] Starting nginx..."
docker compose --profile production up -d nginx
echo "Waiting 5s for nginx to be ready..."
sleep 5

# ── Step 3: obtain real certificate via webroot challenge ─────────────────────
echo ""
echo "[3/4] Obtaining Let's Encrypt certificate..."
docker compose --profile production run --rm certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "${EMAIL}" \
    --agree-tos \
    --no-eff-email \
    -d "${DOMAIN}"

# ── Step 4: reload nginx with the real certificate ────────────────────────────
echo ""
echo "[4/4] Reloading nginx with real certificate..."
docker compose exec nginx nginx -s reload

echo ""
echo "============================================"
echo " HTTPS setup complete!"
echo " API: https://${DOMAIN}/api/v1/"
echo "============================================"
echo ""
echo "Now start all services:"
echo "  docker compose --profile production up -d"
echo ""
echo "Certificate auto-renews every 12h via the certbot container."
