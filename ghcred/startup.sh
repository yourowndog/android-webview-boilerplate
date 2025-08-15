#!/data/data/com.termux/files/usr/bin/bash

# === CONFIGURATION ===
APP_ID=1786337
PEM_PATH="./private-key.pem"
INSTALLATION_ID_FILE="./installation_id.txt"

# === Create a short-lived JWT for GitHub App ===
create_jwt() {
  local now=$(date +%s)
  local exp=$((now + 540))
  local header='{"alg":"RS256","typ":"JWT"}'
  local payload="{\"iat\":$now,\"exp\":$exp,\"iss\":$APP_ID}"

  local b64_header=$(echo -n "$header" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  local b64_payload=$(echo -n "$payload" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  local data="$b64_header.$b64_payload"

  local signature=$(echo -n "$data" | openssl dgst -sha256 -sign "$PEM_PATH" | openssl base64 -A | tr '+/' '-_' | tr -d '=')
  echo "$data.$signature"
}

# === Get installation ID (only needed once per app install) ===
get_installation_id() {
  jwt=$1
  curl -s -H "Authorization: Bearer $jwt" \
       -H "Accept: application/vnd.github+json" \
       https://api.github.com/app/installations \
       | jq '.[0].id' > "$INSTALLATION_ID_FILE"
}

# === Mint access token ===
mint_token() {
  jwt=$1
  installation_id=$(cat "$INSTALLATION_ID_FILE")
  curl -s -X POST \
    -H "Authorization: Bearer $jwt" \
    -H "Accept: application/vnd.github+json" \
    https://api.github.com/app/installations/$installation_id/access_tokens \
    | jq -r .token
}

# === MAIN ===
echo "[+] Creating JWT..."
jwt=$(create_jwt)

if [ ! -f "$INSTALLATION_ID_FILE" ]; then
  echo "[+] Getting installation ID from GitHub..."
  get_installation_id "$jwt"
fi

echo "[+] Minting short-lived GitHub token..."
TOKEN=$(mint_token "$jwt")

if [[ -z "$TOKEN" ]]; then
  echo "[!] ERROR: Failed to mint token"
  exit 1
fi

export TOKEN
echo "[✓] TOKEN exported to \$TOKEN"
