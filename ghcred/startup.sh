#!/usr/bin/env bash
set -euo pipefail

# === CONFIGURATION ===
# These can be overridden via environment variables before calling the script
APP_ID="${APP_ID:-1786337}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PEM_PATH="${PEM_PATH:-$SCRIPT_DIR/private-key.pem}"
INSTALLATION_ID_FILE="${INSTALLATION_ID_FILE:-$SCRIPT_DIR/installation_id.txt}"

# Optional DEBUG flag: DEBUG=1 bash startup.sh
if [[ "${DEBUG:-0}" == 1 ]]; then
  set -x
fi

# === Create a short-lived JWT for GitHub App ===
create_jwt() {
  local now exp header payload b64_header b64_payload unsigned signature
  now=$(date +%s)
  exp=$((now + 600))
  header='{"alg":"RS256","typ":"JWT"}'
  payload=$(printf '{"iat":%s,"exp":%s,"iss":%s}' "$now" "$exp" "$APP_ID")
  b64_header=$(printf '%s' "$header" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  b64_payload=$(printf '%s' "$payload" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  unsigned="$b64_header.$b64_payload"
  signature=$(printf '%s' "$unsigned" | openssl dgst -sha256 -sign "$PEM_PATH" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  printf '%s.%s' "$unsigned" "$signature"
}

# === Get installation ID (only needed once per app install) ===
get_installation_id() {
  local jwt="$1"
  curl -s \
       -H "Authorization: Bearer $jwt" \
       -H "Accept: application/vnd.github+json" \
       -H "User-Agent: ghcred" \
       https://api.github.com/app/installations \
       | jq -r '.[0].id' > "$INSTALLATION_ID_FILE"
}

# === Mint access token ===
mint_token() {
  local jwt="$1"
  local installation_id
  installation_id=$(cat "$INSTALLATION_ID_FILE")
  curl -s -X POST \
    -H "Authorization: Bearer $jwt" \
    -H "Accept: application/vnd.github+json" \
    -H "User-Agent: ghcred" \
    https://api.github.com/app/installations/$installation_id/access_tokens \
    | jq -r .token
}

# === MAIN ===
if ! openssl rsa -check -in "$PEM_PATH" >/dev/null 2>&1; then
  echo "[!] Invalid or unreadable private key at $PEM_PATH" >&2
  exit 1
fi

echo "[+] Creating JWT..."
jwt=$(create_jwt)
if [[ -z "$jwt" ]]; then
  echo "[!] Failed to create JWT" >&2
  exit 1
fi
if [[ "${DEBUG:-0}" == 1 ]]; then
  echo "JWT=$jwt"
fi
echo "[✓] JWT created"

if [[ ! -s "$INSTALLATION_ID_FILE" ]]; then
  echo "[+] Getting installation ID from GitHub..."
  get_installation_id "$jwt" || {
    echo "[!] Failed to fetch installation ID" >&2
    exit 1
  }
fi

echo "[+] Minting short-lived GitHub token..."
TOKEN=$(mint_token "$jwt")
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "[!] ERROR: Failed to mint token" >&2
  exit 1
fi

export TOKEN
export JWT="$jwt"
echo "[✓] TOKEN exported to \$TOKEN"
