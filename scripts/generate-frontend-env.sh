#!/usr/bin/env bash
# ============================================================
# generate-frontend-env.sh
# Reads .env from repo root and generates environment.ts
# for local Angular development.
#
# Usage:
#   ./scripts/generate-frontend-env.sh
#
# Run once before `npm start` or `npm test` in the frontend.
# The generated environment.ts is NOT committed to git.
# ============================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
OUT_FILE="$ROOT_DIR/frontend/planificador-entregas/src/environments/environment.ts"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: $ENV_FILE not found."
  echo "Copy .env.example to .env and fill in real values."
  exit 1
fi

# Load .env (skip comments and blank lines)
# shellcheck disable=SC2046
export $(grep -v '^\s*#' "$ENV_FILE" | grep -v '^\s*$' | xargs)

cat > "$OUT_FILE" <<EOF
// ============================================================
// LOCAL DEVELOPMENT ENVIRONMENT — AUTO-GENERATED
// Do NOT edit manually. Run scripts/generate-frontend-env.sh
// to regenerate from .env.
// This file is listed in .gitignore after the first run.
// ============================================================
export const environment = {
  production: false,
  apiUrl: 'http://localhost:${SERVER_PORT:-8084}/api',
  appName: '${APP_NAME:-DeliveryPlanner}',
  companyName: 'ByStep Solutions S.A.S.',
  companyWebsite: 'https://www.bystepsolutions.tech/',
  googleClientId: '${GOOGLE_CLIENT_ID:-}',
  firebase: {
    apiKey: '${FIREBASE_API_KEY:-}',
    authDomain: '${FIREBASE_AUTH_DOMAIN:-}',
    projectId: '${FIREBASE_PROJECT_ID:-}',
    storageBucket: '${FIREBASE_STORAGE_BUCKET:-}',
    messagingSenderId: '${FIREBASE_MESSAGING_SENDER_ID:-}',
    appId: '${FIREBASE_APP_ID:-}',
    vapidKey: '${FIREBASE_VAPID_KEY:-}'
  }
};
EOF

echo "Generated: $OUT_FILE"

# Prevent git from tracking local changes to the generated file
if git -C "$ROOT_DIR" rev-parse --is-inside-work-tree &>/dev/null; then
  git -C "$ROOT_DIR" update-index --skip-worktree \
    frontend/planificador-entregas/src/environments/environment.ts \
    2>/dev/null || true
fi

echo "Done. Run 'npm start' inside frontend/planificador-entregas/"
