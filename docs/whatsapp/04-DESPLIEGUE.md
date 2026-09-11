# Despliegue — rama `feature/whatsapp-multitenant`

> Ojo: hacer merge a `main` **despliega a producción** (workflow `deploy.yml`) y Flyway ejecuta la migración V14 contra la BD de producción al arrancar. Sigue el orden.

## 1. Antes del merge

1. Sube la rama y abre el PR:
   ```bash
   git push -u origin feature/whatsapp-multitenant
   ```
   El workflow **CI** compila backend (con tests) y frontend en la rama. No hagas merge si no está en verde.
2. Prueba local (recomendado):
   ```bash
   cd backend && ./gradlew build          # compila + tests
   cd ../frontend/planificador-entregas && npm ci && npm run build
   ```
   Con `WHATSAPP_DRY_RUN=true` puedes probar toda la pantalla de WhatsApp sin tocar Meta (simula número conectado y plantillas aprobadas).
3. Respaldo de la BD en Supabase (Database → Backups) antes del merge.

## 2. Variables de entorno en Dokploy (backend)

| Variable | Valor | Obligatoria |
|---|---|---|
| `WHATSAPP_CREDENTIALS_KEY` | Generar **una sola vez**: `openssl rand -base64 32`. Guárdala también en tu gestor de contraseñas: si se pierde hay que volver a pegar los tokens de todas las organizaciones. | Sí, para guardar credenciales |
| `WHATSAPP_ENABLED` | `true` (en `false` apaga todos los envíos: kill switch) | No |
| `WHATSAPP_DRY_RUN` | `false` en producción | No |
| `WHATSAPP_GRAPH_VERSION` | `v23.0` (cámbiala cuando Meta la deprecie) | No |

Elimina las viejas: `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_TEST_MODE`, `WHATSAPP_LANGUAGE`.

Si falta `WHATSAPP_CREDENTIALS_KEY` la app **arranca igual**; solo falla (con mensaje claro) al guardar credenciales de WhatsApp.

## 3. Merge y verificación

1. Merge del PR → GitHub Actions construye y despliega.
2. Revisa logs del backend: Flyway debe decir `Migrating schema "public" to version "14 - org admin contact memberships whatsapp"` y la app debe arrancar sin errores de `validate` de Hibernate.
3. Prueba de humo (5 min):
   - Entrar como PLATFORM_ADMIN → crear organización de prueba con los datos nuevos del administrador.
   - Entrar como admin de la joyería → crear un pedido y un arreglo, registrar un abono, borrar una foto. Todo debe funcionar igual que antes.
   - Inhabilitar a un empleado de prueba → en máximo 30 s su sesión debe cerrarse.
4. Configurar WhatsApp de la joyería con `02-RUNBOOK-META.md`.

## 4. Qué pasa con los datos existentes

- Organizaciones existentes: los campos nuevos del administrador quedan vacíos. Complétalos desde la tarjeta **Administrador y contacto** del detalle de la organización.
- Pedidos y arreglos existentes: quedan con “Notificar por WhatsApp” = sí.
- Membresías: los usuarios hoy inhabilitados quedan con su membresía inhabilitada (mismo comportamiento de antes).
- Fotos existentes: siguen donde están; solo las nuevas van a `orgs/{orgId}/…`.
- Ninguna organización envía WhatsApp hasta que la actives y conectes su número.

## 5. Rollback

- Código: revertir el merge en `main` (se redespliega la versión anterior).
- BD: V14 es aditiva; la versión anterior del backend ignora las columnas/tablas nuevas, así que **no hace falta revertir la migración**. Si aun así quieres limpiarla, no borres la fila de `flyway_schema_history` sin revisar primero.
