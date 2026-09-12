# Delivery Planner — ByStep Solutions

[![CI](https://github.com/JhonOrtiz0406/planificadorEntregas_ByStepSolutions/actions/workflows/ci.yml/badge.svg)](https://github.com/JhonOrtiz0406/planificadorEntregas_ByStepSolutions/actions/workflows/ci.yml)
[![Deploy a producción](https://github.com/JhonOrtiz0406/planificadorEntregas_ByStepSolutions/actions/workflows/deploy.yml/badge.svg)](https://github.com/JhonOrtiz0406/planificadorEntregas_ByStepSolutions/actions/workflows/deploy.yml)

SaaS B2B para gestión de pedidos y entregas, multi-organización (joyería, lavandería, celulares, general).

| | |
|---|---|
| App | https://app.delivery-planner.bystepsolutions.tech |
| API | https://api.delivery-planner.bystepsolutions.tech |
| Logs | https://logs.delivery-planner.bystepsolutions.tech |

## Estructura

| Carpeta | Qué es |
|---|---|
| `backend/` | Spring Boot 3.4 (MVC + JPA + Flyway), Clean Architecture: `domain/model`, `domain/usecase`, `infrastructure/*`, `applications/app-service` |
| `frontend/planificador-entregas/` | Angular 19 + Angular Material |
| `docker-stack.yml` | Stack de Docker Swarm que corre en el VPS (Dokploy + Traefik) |
| `.github/workflows/` | CI (cada rama) y despliegue a producción (merge a `main`) |
| `docs/` | Documentación funcional y técnica |

## Desarrollo

```bash
# Backend
cd backend && ./gradlew build            # compila y prueba
# Frontend
cd frontend/planificador-entregas && npm ci && npm start
```

## Despliegue

Merge a `main` → se despliega solo. El flujo, las variables y cómo leer los resultados están en
**[docs/cicd/README.md](docs/cicd/README.md)**.

## Documentación

- [CI/CD y observabilidad](docs/cicd/README.md)
- [WhatsApp: diseño multi-tenant](docs/whatsapp/01-PLAN-TECNICO.md)
- [WhatsApp: conectar el número de un cliente](docs/whatsapp/02-RUNBOOK-META.md)
- [WhatsApp: guía de despliegue](docs/whatsapp/04-DESPLIEGUE.md)
