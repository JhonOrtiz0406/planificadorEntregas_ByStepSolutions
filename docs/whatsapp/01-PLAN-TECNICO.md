# WhatsApp multi-tenant y aislamiento por organización — Diseño implementado

> ByStep Solutions · Delivery Planner · Rama `feature/whatsapp-multitenant` · Septiembre 2026
> Principio: **cada organización es su propio mundo**. Sus datos, su equipo, su número de WhatsApp y su cola de mensajes nunca se cruzan con los de otra, aunque sean de la misma categoría (Joyería 1 ≠ Joyería 2).

---

## 1. Qué cambió

| Tema | Antes | Ahora |
|---|---|---|
| Número de WhatsApp | Uno global en variables de entorno | **Uno por organización**, con su token guardado cifrado (AES-256-GCM) |
| Envío | Síncrono dentro del request, sin timeouts, errores perdidos | **Cola por organización** (outbox en Postgres). Envío **inmediato** en segundo plano + reintentos |
| Si falla el token de una org | Afectaba a todas | Solo esa organización queda en `ERROR`; las demás siguen enviando |
| Eventos | Pedidos (nombres de plantilla sin gestionar) | Pedidos, arreglos de joyería y abonos, con catálogo de plantillas que se crea solo en la cuenta de cada org |
| Webhook de WhatsApp | Stub sin uso que además escribía el token en logs | **Eliminado** (endpoint, permiso público y variables) |
| Crear organización | Nombre, categoría, email del admin | + **nombres, apellidos, celular personal del admin** y **celular de la organización** (el que se registra en Meta) |
| Miembros | Inhabilitar/eliminar actuaba sobre el usuario global | Actúa sobre la **membresía en esa organización** |
| Token (JWT) de un miembro removido | Seguía sirviendo hasta expirar | Deja de servir en ≤ 30 s |

---

## 2. Aislamiento entre organizaciones (auditoría y correcciones)

Se revisaron todos los endpoints. Pedidos, arreglos, calendario y abonos ya filtraban por la organización del token. Se encontraron y corrigieron estos huecos:

| # | Hueco | Riesgo | Corrección |
|---|---|---|---|
| 1 | `GET /api/organizations/{id}` no validaba la org para `ORG_ADMIN` | Un admin podía ver los datos de otra organización cambiando el id | Solo su organización (PLATFORM_ADMIN ve todas) |
| 2 | Inhabilitar / habilitar / eliminar miembro no validaba que el usuario perteneciera a la org del path | Un admin podía inhabilitar o **borrar** empleados de otra organización | El usuario debe ser miembro de esa organización; si no → 404 |
| 3 | Inhabilitar un miembro lo inhabilitaba en **todas** sus organizaciones | Una org afectaba el acceso del usuario a otra | Estado por membresía (`user_organizations.is_active`) |
| 4 | Borrar un abono no validaba que perteneciera al pedido/arreglo | Se podían borrar abonos de otra organización | El abono debe ser del pedido/arreglo, que a su vez debe ser de la org |
| 5 | Borrar foto eliminaba del storage **cualquier URL** enviada | Se podían borrar fotos de otra organización | Solo se borra si la foto pertenece a ese pedido/arreglo |
| 6 | Fotos se guardaban en carpetas compartidas (`orders/`) | Mezcla de archivos entre orgs | Nuevas fotos en `orgs/{orgId}/{orders\|arreglos}/…` (las existentes siguen funcionando) |
| 7 | JWT seguía válido tras sacar/inhabilitar a alguien | Acceso residual por horas | Filtro verifica usuario activo + membresía activa + mismo rol (caché 30 s). Si no → 401 y el frontend cierra sesión |
| 8 | La lista de miembros devolvía `googleId` y token FCM | Exposición innecesaria | Respuesta reducida a lo que usa la pantalla |
| 9 | Deshabilitar una org apagaba usuarios que también pertenecen a otra | Una org afectaba a otra | Solo se apagan las membresías de esa org; el usuario conserva las demás |

Base de datos: las tablas nuevas de WhatsApp tienen RLS con denegación total a `anon`/`authenticated` y **sin grants**: solo el backend las usa.

---

## 3. Arquitectura del envío (desacoplado por organización)

```
Pedido/Arreglo/Abono (caso de uso)
      │  ClientNotificationUseCase.notify(orgId, evento, …)   ← nunca lanza error al caso de uso
      ▼
whatsapp_messages (PENDING, con organization_id)             ← cola por organización
      │  WhatsAppDispatcher: dispara al instante + barrido cada 15 s
      ▼
WhatsAppDispatchUseCase  → toma lote (FOR UPDATE SKIP LOCKED)
      │  por cada mensaje carga la config de SU organización
      ▼
Meta Cloud API  (número + token de esa organización)
```

Reglas antes de encolar (si alguna falla, queda `SKIPPED` con motivo visible):
1. La org tiene el módulo activo (`enabled`) → si no, no se registra nada.
2. El evento aplica a la categoría (arreglos solo JEWELRY) y la org lo tiene activado.
3. No se envió antes (`idempotency_key`, ej. `ORDER:{id}:STATUS:READY_TO_DELIVER`).
4. El cliente autorizó (checkbox en el pedido/arreglo), celular válido, WhatsApp `CONNECTED` y plantilla `APPROVED`.

Reintentos: 1 min → 5 min → 30 min; al 4.º intento queda `FAILED`. Mensajes que quedaron `SENDING` por un reinicio vuelven a la cola a los 10 min.

**Inmediato:** las notificaciones al cliente salen en segundos después de guardar. El único proceso programado es el recordatorio **interno** al equipo (push), que ahora corre a las 8:00 a.m. hora Colombia (antes 3:00 a.m. por UTC).

---

## 4. Eventos y plantillas (catálogo en código: `NotificationEvent`)

| Evento | Plantilla | Aplica |
|---|---|---|
| Pedido registrado | `dp_pedido_creado_v1` | todas |
| Cambio de fecha | `dp_pedido_cambio_fecha_v1` | todas |
| Pedido en proceso (`IN_PREPARATION`) | `dp_pedido_en_proceso_v1` | todas (apagado por defecto en Joyería) |
| Pedido listo | `dp_pedido_listo_v1` | todas |
| Pedido entregado | `dp_pedido_entregado_v1` | todas |
| Abono a pedido | `dp_abono_pedido_v1` | todas |
| Arreglo recibido | `dp_arreglo_recibido_v1` | Joyería |
| Arreglo listo | `dp_arreglo_listo_v1` | Joyería |
| Arreglo entregado | `dp_arreglo_entregado_v1` | Joyería |
| Abono a arreglo | `dp_abono_arreglo_v1` | Joyería |

Todas `UTILITY`, idioma `es`, sin tono promocional. “Sincronizar plantillas” las crea en el WABA de la organización; el pie de mensaje (“¿Dudas? Escríbenos al …”) es propio de cada org. Agregar un evento = nueva constante en `NotificationEvent` + disparo en el caso de uso.

---

## 5. Endpoints nuevos

`/api/organizations/{id}/whatsapp` (PLATFORM_ADMIN cualquier org; ORG_ADMIN solo la suya):

| Método | Ruta | Quién | Para qué |
|---|---|---|---|
| GET | `` | admin org / plataforma | Estado, eventos, plantillas, conteo del mes (sin token) |
| GET | `/messages?limit=20` | admin org / plataforma | Historial reciente (teléfonos enmascarados) |
| PUT | `/events` | admin org / plataforma | Activar/desactivar eventos |
| POST | `/test` | admin org / plataforma | Enviar prueba (10/hora por org) |
| POST | `/templates/refresh` | admin org / plataforma | Consultar estado de aprobación en Meta |
| PUT | `/credentials` | plataforma | WABA ID, Phone Number ID, token, pie de mensaje → guarda y verifica |
| POST | `/verify` | plataforma | Re-verificar el número |
| PATCH | `/enabled` | plataforma | Activar el módulo (add-on que se cobra) |
| POST | `/templates/sync` | plataforma | Crear plantillas faltantes en Meta |

---

## 6. Qué NO incluye esta versión (a propósito)

- **Webhook** (estados ✓✓ entregado/leído, respuestas de clientes, opt-out por “STOP”): se quitó el stub. El historial muestra “Enviado” cuando Meta acepta el mensaje. Si se necesita ✓✓ o leer respuestas, se agrega un webhook nuevo con firma HMAC.
- Registro automático con **Embedded Signup** (requiere que ByStep sea Tech Provider en Meta). Hoy las credenciales se pegan a mano (ver `02-RUNBOOK-META.md`); el modelo de datos ya soporta el flujo automático.
- WhatsApp al equipo interno: siguen con notificaciones push (inmediatas). El celular personal del admin queda guardado para usarlo en una siguiente fase.
