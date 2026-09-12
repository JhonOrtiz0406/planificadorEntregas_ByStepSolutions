#!/usr/bin/env bash
# Se ejecuta EN EL SERVIDOR (por SSH). Publica la nueva versión en Docker Swarm.
# Variables que recibe: APP_DIR, STACK, VERSION, GHCR_TOKEN
set -euo pipefail

echo "→ Autenticando en GHCR (si falla, se continúa: el paquete es público)"
echo "$GHCR_TOKEN" | docker login ghcr.io -u jhonortiz0406 --password-stdin || \
  echo "  WARN: docker login falló (¿token vencido?), se continúa"

echo "→ Descargando imágenes $VERSION"
docker pull "ghcr.io/jhonortiz0406/delivery-planner-backend:$VERSION"
docker pull "ghcr.io/jhonortiz0406/delivery-planner-frontend:$VERSION"

echo "→ Cargando variables del stack"
cat > /tmp/load_env.py << 'PYEOF'
import re, sys
with open(sys.argv[1]) as f:
    content = f.read()
pattern = r'^([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)(?=\n[A-Z_]|\Z)'
for m in re.finditer(pattern, content.strip() + '\n', re.MULTILINE | re.DOTALL):
    key = m.group(1)
    val = m.group(2).strip().rstrip('\n')
    escaped = val.replace("'", "'\"'\"'")
    print(f"export {key}='{escaped}'")
PYEOF
set +u
eval "$(python3 /tmp/load_env.py "$APP_DIR/.env")"
set -u

# La versión y la fecha quedan visibles en /actuator/info
export APP_VERSION="$VERSION"
export DEPLOYED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

echo "→ Desplegando stack $STACK (rolling, con rollback automático)"
docker stack deploy \
  --with-registry-auth \
  --compose-file "$APP_DIR/docker-stack.yml" \
  --detach=false \
  "$STACK"

echo "→ Estado de los servicios"
docker service ls --filter "name=$STACK" --format '   {{.Name}}  {{.Replicas}}  {{.Image}}'
