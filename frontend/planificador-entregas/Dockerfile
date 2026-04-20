# ============================================================
# Frontend Dockerfile – DeliveryPlanner | ByStep Solutions
#
# Build-time args (passed via docker-compose build.args or --build-arg):
#   API_URL                   – Backend URL. Leave empty for same-stack nginx proxy.
#   APP_NAME                  – Display name shown in the UI.
#   GOOGLE_CLIENT_ID          – Google OAuth client ID.
#   FIREBASE_*                – Firebase configuration values.
#
# Runtime env var (via -e or docker-compose environment):
#   BACKEND_URL               – nginx proxy target for /api (e.g. backend:8084).
#                               Only used when API_URL is empty (same-stack mode).
#                               Defaults to "backend:8084".
# ============================================================

# ── Stage 1: Build Angular app ───────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

# Build-time args with safe defaults
ARG API_URL=""
ARG APP_NAME="DeliveryPlanner"
ARG GOOGLE_CLIENT_ID=""
ARG FIREBASE_API_KEY=""
ARG FIREBASE_AUTH_DOMAIN=""
ARG FIREBASE_PROJECT_ID=""
ARG FIREBASE_STORAGE_BUCKET=""
ARG FIREBASE_MESSAGING_SENDER_ID=""
ARG FIREBASE_APP_ID=""
ARG FIREBASE_VAPID_KEY=""

COPY package*.json ./
RUN npm ci --legacy-peer-deps

COPY . .

# Substitute placeholders in production environment file and service worker before building
RUN sed -i \
      -e "s|\${API_URL}|${API_URL}|g" \
      -e "s|\${APP_NAME}|${APP_NAME}|g" \
      -e "s|\${GOOGLE_CLIENT_ID}|${GOOGLE_CLIENT_ID}|g" \
      -e "s|\${FIREBASE_API_KEY}|${FIREBASE_API_KEY}|g" \
      -e "s|\${FIREBASE_AUTH_DOMAIN}|${FIREBASE_AUTH_DOMAIN}|g" \
      -e "s|\${FIREBASE_PROJECT_ID}|${FIREBASE_PROJECT_ID}|g" \
      -e "s|\${FIREBASE_STORAGE_BUCKET}|${FIREBASE_STORAGE_BUCKET}|g" \
      -e "s|\${FIREBASE_MESSAGING_SENDER_ID}|${FIREBASE_MESSAGING_SENDER_ID}|g" \
      -e "s|\${FIREBASE_APP_ID}|${FIREBASE_APP_ID}|g" \
      -e "s|\${FIREBASE_VAPID_KEY}|${FIREBASE_VAPID_KEY}|g" \
      src/environments/environment.prod.ts \
      public/firebase-messaging-sw.js

RUN npm run build:prod

# ── Stage 2: Nginx runtime ───────────────────────────────────
FROM nginx:alpine AS runtime

# Install gettext for envsubst (used to inject BACKEND_URL at container start)
RUN apk add --no-cache gettext

# Copy built Angular app
COPY --from=builder /app/dist/planificador-entregas/browser /usr/share/nginx/html

# Angular 19 SSR: if SSR pre-rendering did not generate index.html (e.g. API
# unavailable at build time), fall back to the CSR index so nginx doesn't serve
# the default nginx welcome page.
RUN if [ ! -s /usr/share/nginx/html/index.html ] || \
       grep -q "Welcome to nginx" /usr/share/nginx/html/index.html 2>/dev/null; then \
      cp /usr/share/nginx/html/index.csr.html /usr/share/nginx/html/index.html; \
    fi

# Copy nginx config as a template so BACKEND_URL can be substituted at runtime
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ || exit 1

# nginx official image auto-runs envsubst on /etc/nginx/templates/*.template
# and places the result in /etc/nginx/conf.d/ before starting nginx.
# BACKEND_URL defaults to "backend:8084" (docker-compose same-stack mode).
ENV BACKEND_URL="backend:8084"

CMD ["nginx", "-g", "daemon off;"]
