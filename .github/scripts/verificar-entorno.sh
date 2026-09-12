#!/usr/bin/env bash
# Se ejecuta EN EL SERVIDOR (por SSH, desde el workflow de despliegue).
# Comprueba que el .env del stack tenga todo lo que la app necesita ANTES de desplegar.
# Nunca imprime valores: solo nombres de variables y estados.
set -uo pipefail

ENV_FILE="${APP_DIR:-.}/.env"

# Variables sin las que la app NO arranca o queda insegura
REQUERIDAS=(
  DB_HOST DB_USERNAME DB_PASSWORD
  JWT_SECRET GOOGLE_CLIENT_ID CORS_ALLOWED_ORIGINS
  MAIL_HOST MAIL_USERNAME MAIL_PASSWORD
  FIREBASE_SERVICE_ACCOUNT_JSON FIREBASE_STORAGE_BUCKET
  APP_DISPLAY_NAME APP_URL PLATFORM_ADMIN_EMAIL
  DOZZLE_AUTH
)

# Variables opcionales: si faltan, la función queda apagada pero la app arranca
OPCIONALES=(
  WHATSAPP_CREDENTIALS_KEY WHATSAPP_ENABLED WHATSAPP_DRY_RUN WHATSAPP_GRAPH_VERSION
  JWT_EXPIRATION_MS BACKEND_URL DB_PORT DB_NAME
)

echo "### 🧾 Variables del stack (\`$ENV_FILE\`)"
echo ""

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ No existe el archivo \`.env\` en \`$APP_DIR\`. Créalo desde la interfaz de Dokploy (pestaña Environment)."
  exit 1
fi

tiene() { grep -Eq "^[[:space:]]*$1[[:space:]]*=[[:space:]]*[^[:space:]]" "$ENV_FILE"; }

FALTAN=()
echo "| Variable | Obligatoria | Estado |"
echo "|---|---|---|"
for v in "${REQUERIDAS[@]}"; do
  if tiene "$v"; then echo "| \`$v\` | sí | ✅ definida |"; else echo "| \`$v\` | sí | ❌ **falta** |"; FALTAN+=("$v"); fi
done
for v in "${OPCIONALES[@]}"; do
  if tiene "$v"; then echo "| \`$v\` | no | ✅ definida |"; else echo "| \`$v\` | no | ⚪ sin definir (se usa el valor por defecto) |"; fi
done
echo ""

echo "### 🖥️ Estado del servidor"
echo ""
echo "| Recurso | Valor |"
echo "|---|---|"
echo "| Disco (/) | $(df -h / | awk 'NR==2{print $4" libres de "$2" ("$5" usado)"}') |"
echo "| Memoria | $(free -h 2>/dev/null | awk 'NR==2{print $7" disponibles de "$2}') |"
echo "| Docker | $(docker version --format '{{.Server.Version}}' 2>/dev/null || echo 'no disponible') |"
echo "| Servicios del stack | $(docker service ls --filter name=delivery-planner --format '{{.Name}}={{.Replicas}}' 2>/dev/null | paste -sd ' ' -) |"
echo ""

LIBRE_KB=$(df -k / | awk 'NR==2{print $4}')
if [ "${LIBRE_KB:-0}" -lt 2097152 ]; then
  echo "> ⚠️ Quedan menos de 2 GB libres en disco. Considera \`docker system prune -af\` antes de desplegar."
  echo ""
fi

if [ ${#FALTAN[@]} -gt 0 ]; then
  echo "> ❌ **Despliegue detenido.** Faltan variables obligatorias: \`${FALTAN[*]}\`."
  echo "> Agrégalas en Dokploy (Environment del proyecto) y vuelve a lanzar el despliegue."
  exit 1
fi

echo "> ✅ Todo lo necesario está configurado. Se puede desplegar."
