# Runbook — Conectar el WhatsApp de una organización (Meta Cloud API)

> Para: Anthony (PLATFORM_ADMIN de Delivery Planner) · Modelo: **WABA en el portfolio del cliente, el cliente le paga a Meta**
> Tiempo estimado: 1–2 horas con el dueño del negocio al lado (o en videollamada) + espera de aprobación de plantillas.
> Repetible para cada organización nueva. Al final hay un checklist.

---

## 0. Antes de la cita — pídele al cliente

- [ ] El **celular con la SIM** del número (para recibir el código por SMS o llamada).
- [ ] Una cuenta personal de Facebook del dueño (será admin del portfolio).
- [ ] NIT / razón social, dirección, correo del negocio, web o Instagram.
- [ ] Logo cuadrado (mín. 640×640).
- [ ] **Tarjeta de crédito/débito** a nombre del negocio (Meta cobra por plantilla entregada; se puede facturar en COP).
- [ ] Confirmar que el **celular de la organización** registrado al crearla en Delivery Planner es el mismo que se va a conectar (si no, corrígelo en la tarjeta “Administrador y contacto”).
- [ ] Texto de contacto para el pie de los mensajes, ej: “¿Dudas? Escríbenos al 300 123 4567” (máx. 60 caracteres) — **otro número** donde sí los atiendan por chat.

Déjale claro al cliente, por escrito:
1. Ese número **dejará de funcionar en la app de WhatsApp** del celular. Solo enviará notificaciones automáticas.
2. Esta versión **no recibe respuestas**: si un cliente contesta, la joyería no lo verá. Por eso el pie del mensaje debe decir a qué otro número escribir.
3. Meta le cobrará directamente a su tarjeta por cada mensaje entregado (categoría “utilidad”).

---

## 1. Liberar el número de la app de WhatsApp

1. (Opcional) Exportar los chats importantes.
2. En el celular: **WhatsApp → Ajustes → Cuenta → Eliminar cuenta** → confirmar con el número. *(Desinstalar la app NO basta.)*
3. Esperar unos minutos antes del paso 4.

---

## 2. Business Portfolio de la joyería

1. Entrar a **business.facebook.com** con la cuenta del dueño.
2. Si ya tienen portfolio (por la página de Facebook/Instagram), usar ese. Si no: **Crear portfolio** con el nombre legal del negocio.
3. **Configuración → Personas → Agregar**: tu correo de ByStep como **Administrador** (para que puedas operar sin tener la sesión del dueño). Al terminar puedes bajarte a acceso parcial.
4. (Recomendado, no bloqueante) **Centro de seguridad → Verificación del negocio**. Sube el límite de mensajes; sin verificar arranca en 250 clientes distintos por 24h, que para una joyería es suficiente.

---

## 3. App de Meta dentro del portfolio del cliente

1. **developers.facebook.com → Mis apps → Crear app**.
2. Caso de uso: **“Conectarte con clientes a través de WhatsApp”**.
3. Nombre: `Delivery Planner - <Nombre org>`. Portfolio: **el de la joyería** (¡no el de ByStep!).
4. Meta crea un número y WABA de prueba: ignóralos.
5. **Configuración de la app → Básica**: agregar URL de política de privacidad (`https://www.bystepsolutions.tech/privacidad` — confirma que exista) y categoría.

---

## 4. Agregar el número real

1. En la app: **WhatsApp → Configuración de la API → Agregar número de teléfono** (o desde WhatsApp Manager).
2. Crear/seleccionar el **WhatsApp Business Account (WABA)** del negocio.
3. Perfil: **Nombre para mostrar** = nombre comercial de la joyería (debe coincidir con el negocio; Meta lo revisa), categoría “Compras y ventas minoristas”, descripción.
4. Número con indicativo `+57` y verificar por **SMS o llamada**.
5. Anota:
   - `WABA ID` (WhatsApp Manager → Configuración de la cuenta)
   - `Phone Number ID` (Configuración de la API, debajo del número)

### 4.1 Registrar el número para Cloud API (PIN de 2 pasos)
Si en WhatsApp Manager el número aparece como pendiente/no conectado, registrarlo (puedes hacerlo luego desde Delivery Planner, o con curl):

```bash
curl -X POST "https://graph.facebook.com/v25.0/<PHONE_NUMBER_ID>/register" \
  -H "Authorization: Bearer <TOKEN_DEL_PASO_6>" \
  -H "Content-Type: application/json" \
  -d '{"messaging_product":"whatsapp","pin":"<6 DÍGITOS>"}'
```
Guarda el PIN en tu gestor de contraseñas. Lo vas a necesitar si algún día se migra el número.

---

## 5. Método de pago

**WhatsApp Manager → Configuración de facturación (o Business Settings → Pagos)** → agregar la tarjeta del negocio, moneda **COP**.
Sin método de pago Meta rechaza los mensajes iniciados por el negocio (error `131042`).

---

## 6. System User y token permanente

1. **business.facebook.com → Configuración → Usuarios → Usuarios del sistema → Agregar**
   - Nombre: `deliveryplanner-api` · Rol: **Administrador**.
2. **Asignar activos** a ese system user:
   - La **app** `Delivery Planner - <org>` → Control total.
   - El **WABA** → Control total.
3. **Generar token**:
   - App: `Delivery Planner - <org>`
   - Caducidad: **Nunca**
   - Permisos: `whatsapp_business_messaging`, `whatsapp_business_management`
4. Copiarlo **directo** al formulario de Delivery Planner (paso 8) o a tu gestor de contraseñas. Nunca por WhatsApp ni correo.

> Por qué no un token de usuario: expira en 24h/60 días. El del system user no expira y no depende de la cuenta personal del dueño.

---

## 7. Webhook

**No se necesita en esta versión.** Delivery Planner solo envía; no escucha eventos de Meta. No configures URL de devolución de llamada.

---

## 8. En Delivery Planner (PLATFORM_ADMIN)

Organizaciones → `<org>` → pestaña **WhatsApp**:

1. Pegar `WABA ID`, `Phone Number ID`, `Access token` y el texto del pie → **Guardar y verificar**.
2. Debe quedar **Conectado** mostrando el número, el nombre verificado y la calidad. Si sale un aviso de que el número no coincide con el celular de la organización, corrige uno de los dos.
3. **Sincronizar plantillas** → se crean las plantillas `dp_*` en el WABA del cliente. Aprobación: de minutos a 24h. Se ve el estado en la tabla.
4. Cuando estén `APPROVED`: activar los eventos que el cliente quiere.
5. **Enviar mensaje de prueba** a tu celular y al del dueño.
6. Activar el switch **Módulo WhatsApp activo** (esto es lo que se factura como add-on).

---

## 9. Perfil de WhatsApp (5 min, se nota mucho)

WhatsApp Manager → Números de teléfono → Perfil: logo, descripción, dirección, horario, web/Instagram.

---

## 10. Checklist por organización

| # | Paso | ✔ |
|---|---|---|
| 1 | Número liberado de la app (cuenta eliminada) | ☐ |
| 2 | Portfolio del cliente creado + ByStep como admin | ☐ |
| 3 | App `Delivery Planner - <org>` en el portfolio del cliente | ☐ |
| 4 | Número agregado, verificado y registrado (PIN guardado) | ☐ |
| 5 | Nombre para mostrar aprobado | ☐ |
| 6 | Tarjeta del cliente configurada (COP) | ☐ |
| 7 | System user + token permanente con los 2 permisos | ☐ |
| 8 | (No aplica en esta versión: sin webhook) | — |
| 9 | Credenciales en Delivery Planner, “Verificar” OK | ☐ |
| 10 | Plantillas sincronizadas y aprobadas | ☐ |
| 11 | Prueba recibida en tu celular | ☐ |
| 12 | Módulo activado + add-on agregado a la factura del cliente | ☐ |

---

## 11. Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| “El número ya está registrado en WhatsApp” | No se eliminó la cuenta en la app | Paso 1 y esperar unos minutos |
| Error `190` en Delivery Planner | Token revocado o system user borrado | Regenerar token (paso 6) y actualizarlo |
| Error `131042` | Sin método de pago | Paso 5 |
| Error `132001` | Plantilla no existe / no aprobada en ese idioma | Sincronizar plantillas y esperar aprobación |
| Error `131026` | El destinatario no tiene WhatsApp o bloqueó el número | Nada que hacer; queda en historial |
| Plantilla `REJECTED` o reclasificada a MARKETING | Texto con tono promocional | Ajustar texto en el catálogo (sin promociones) y re-sincronizar con nuevo sufijo `_v2` |
| El historial dice “Omitido” | Ver el motivo: plantilla sin aprobar, cliente sin autorización, celular inválido o WhatsApp desconectado | Corregir el motivo; los siguientes eventos saldrán normal |

---

## 12. Cuando llegues a varios clientes: Tech Provider

Cuando hacer este runbook a mano duela (≈ cliente #4–5), el siguiente paso es inscribir a ByStep como **Tech Provider** en Meta (verificación del negocio de ByStep + App Review de `whatsapp_business_messaging` y `whatsapp_business_management` con Advanced Access) e implementar **Embedded Signup v4** (la v2 se depreca el 15-oct-2026, no la uses). El cliente conecta su número solo desde un botón en Delivery Planner, y el backend guarda los mismos campos que hoy se pegan a mano. Como Tech Provider arrancas con 10 clientes nuevos por semana y subes a 200 al completar las verificaciones. Esto también habilita **Coexistencia** (el cliente sigue usando la app en el celular con el mismo número).

Legal: el token le permite a ByStep enviar mensajes en nombre del cliente y procesar teléfonos de sus clientes finales (Ley 1581 de 2012, habeas data). Incluye una cláusula de encargo de tratamiento de datos en el contrato del add-on. Llévalo a la cita con el abogado.
