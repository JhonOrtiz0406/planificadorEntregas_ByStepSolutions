import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { Observable } from 'rxjs';
import {
  WhatsAppService, WhatsAppOverview, WhatsAppMessageView, WhatsAppEventView, apiErrorMessage
} from '../../../core/services/whatsapp.service';

/**
 * Módulo WhatsApp de UNA organización: estado de conexión, credenciales (solo
 * PLATFORM_ADMIN), eventos a notificar, plantillas, prueba e historial.
 */
@Component({
  selector: 'app-org-whatsapp-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSlideToggleModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatDividerModule, DatePipe],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title style="display:flex;align-items:center;gap:8px">
          <mat-icon style="color:#16a34a">chat</mat-icon> Notificaciones por WhatsApp
          <span [class]="'chip ' + statusClass()">{{ statusLabel() }}</span>
        </mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @let ov = overview();
        @if (loading()) {
          <div style="display:flex;justify-content:center;padding:24px"><mat-spinner diameter="32"></mat-spinner></div>
        } @else if (ov) {

          <!-- Estado -->
          <div class="grid">
            <div><span class="lbl">Número conectado</span>
              <span>{{ ov.config?.displayPhoneNumber || '—' }}</span></div>
            <div><span class="lbl">Nombre verificado</span>
              <span>{{ ov.config?.verifiedName || '—' }}</span></div>
            <div><span class="lbl">Calidad del número</span>
              <span>{{ qualityLabel(ov.config?.qualityRating) }}</span></div>
            <div><span class="lbl">Este mes</span>
              <span>{{ count('SENT') }} enviados · {{ count('FAILED') }} fallidos · {{ count('SKIPPED') }} omitidos</span></div>
          </div>
          @if (ov.config?.lastError) {
            <div class="alert">{{ ov.config?.lastError }}</div>
          }

          <!-- Solo PLATFORM_ADMIN: activación y credenciales de Meta -->
          @if (isPlatformAdmin) {
            <mat-divider style="margin:16px 0"></mat-divider>
            <mat-slide-toggle [checked]="!!ov.config?.enabled" (change)="toggleModule($event.checked)" [disabled]="busy()">
              Módulo WhatsApp activo para esta organización
            </mat-slide-toggle>

            <h4 class="section">Credenciales de Meta</h4>
            <form [formGroup]="credForm" (ngSubmit)="saveCredentials()" class="cred-form">
              <mat-form-field appearance="outline">
                <mat-label>WABA ID</mat-label>
                <input matInput formControlName="wabaId" autocomplete="off">
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Phone Number ID</mat-label>
                <input matInput formControlName="phoneNumberId" autocomplete="off">
              </mat-form-field>
              <mat-form-field appearance="outline" class="wide">
                <mat-label>Access token (System User)</mat-label>
                <input matInput formControlName="accessToken" type="password" autocomplete="new-password"
                       [placeholder]="ov.config?.hasAccessToken ? 'Guardado — escribe solo si lo vas a cambiar' : 'Pega el token permanente'">
              </mat-form-field>
              <mat-form-field appearance="outline" class="wide">
                <mat-label>Pie de los mensajes (opcional)</mat-label>
                <input matInput formControlName="supportContactText" maxlength="60"
                       placeholder="¿Dudas? Escríbenos al 300 123 4567">
                <mat-hint align="end">{{ credForm.value.supportContactText?.length || 0 }}/60</mat-hint>
              </mat-form-field>
              <div class="actions wide">
                <button mat-raised-button color="primary" type="submit" [disabled]="credForm.invalid || busy()">
                  <mat-icon>save</mat-icon> Guardar y verificar
                </button>
                <button mat-stroked-button type="button" (click)="verify()" [disabled]="busy() || !ov.config?.hasAccessToken">
                  <mat-icon>verified</mat-icon> Verificar conexión
                </button>
                <button mat-stroked-button type="button" (click)="syncTemplates()"
                        [disabled]="busy() || ov.config?.status !== 'CONNECTED'">
                  <mat-icon>sync</mat-icon> Sincronizar plantillas
                </button>
              </div>
            </form>
          }

          <!-- Eventos y plantillas -->
          <mat-divider style="margin:16px 0"></mat-divider>
          <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px">
            <h4 class="section" style="margin:0">Qué se le notifica al cliente</h4>
            <button mat-button type="button" (click)="refreshTemplates()"
                    [disabled]="busy() || ov.config?.status !== 'CONNECTED'">
              <mat-icon>refresh</mat-icon> Actualizar estado de plantillas
            </button>
          </div>
          <div class="events">
            @for (ev of ov.events; track ev.key) {
              <div class="event">
                <mat-slide-toggle [checked]="ev.enabled" (change)="toggleEvent(ev, $event.checked)" [disabled]="busy()">
                  {{ ev.label }}
                </mat-slide-toggle>
                <span [class]="'tpl ' + templateClass(ev.templateStatus)"
                      [title]="ev.rejectionReason || ev.templateName">
                  {{ templateLabel(ev.templateStatus) }}
                </span>
              </div>
            }
          </div>

          <!-- Prueba -->
          <mat-divider style="margin:16px 0"></mat-divider>
          <h4 class="section">Enviar mensaje de prueba</h4>
          <div style="display:flex;gap:8px;align-items:flex-start;flex-wrap:wrap">
            <mat-form-field appearance="outline" style="flex:1;min-width:200px">
              <mat-label>Celular de prueba</mat-label>
              <input matInput [(ngModel)]="testPhone" [ngModelOptions]="{standalone: true}" type="tel"
                     placeholder="300 123 4567">
            </mat-form-field>
            <button mat-stroked-button type="button" (click)="sendTest()"
                    [disabled]="busy() || !testPhone || ov.config?.status !== 'CONNECTED'" style="margin-top:8px">
              <mat-icon>send</mat-icon> Enviar prueba
            </button>
          </div>

          <!-- Historial -->
          <mat-divider style="margin:16px 0"></mat-divider>
          <h4 class="section">Últimos mensajes</h4>
          @if (messages().length === 0) {
            <p style="color:var(--text-secondary);margin:0">Aún no hay mensajes.</p>
          } @else {
            <div style="overflow-x:auto">
              <table class="msgs">
                <thead><tr><th>Fecha</th><th>Evento</th><th>Destino</th><th>Estado</th><th>Detalle</th></tr></thead>
                <tbody>
                  @for (m of messages(); track m.id) {
                    <tr>
                      <td>{{ m.createdAt | date:'d/MM/yy HH:mm' }}</td>
                      <td>{{ eventLabel(m.eventKey) }}</td>
                      <td>{{ m.toPhone }}</td>
                      <td><span [class]="'st st-' + m.status">{{ messageStatusLabel(m.status) }}</span></td>
                      <td class="detail">{{ m.errorMessage || skipLabel(m.skipReason) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(200px,1fr)); gap:12px; }
    .grid > div { display:flex; flex-direction:column; gap:2px; }
    .lbl { font-size:.75rem; color:var(--text-secondary); text-transform:uppercase; letter-spacing:.03em; }
    .section { margin:12px 0 8px; color:var(--text-primary); }
    .alert { margin-top:12px; padding:10px 12px; border-radius:8px; background:#fff7ed; border:1px solid #fdba74;
             color:#92400e; font-size:.85rem; }
    .cred-form { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:4px 12px; }
    .cred-form .wide { grid-column:1 / -1; }
    .actions { display:flex; gap:8px; flex-wrap:wrap; }
    .events { display:flex; flex-direction:column; gap:10px; margin-top:8px; }
    .event { display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap; }
    .chip, .tpl, .st { font-size:.72rem; padding:2px 8px; border-radius:999px; font-weight:600; white-space:nowrap; }
    .ok { background:#dcfce7; color:#166534; }
    .warn { background:#fef9c3; color:#854d0e; }
    .bad { background:#fee2e2; color:#991b1b; }
    .off { background:#e5e7eb; color:#374151; }
    .msgs { width:100%; border-collapse:collapse; font-size:.82rem; }
    .msgs th, .msgs td { text-align:left; padding:6px 8px; border-bottom:1px solid rgba(0,0,0,.08); }
    .msgs .detail { color:var(--text-secondary); max-width:320px; }
    .st-SENT { background:#dcfce7; color:#166534; }
    .st-PENDING, .st-SENDING { background:#fef9c3; color:#854d0e; }
    .st-FAILED { background:#fee2e2; color:#991b1b; }
    .st-SKIPPED { background:#e5e7eb; color:#374151; }
  `]
})
export class OrgWhatsAppCardComponent implements OnInit {
  @Input({ required: true }) orgId!: string;
  @Input() isPlatformAdmin = false;

  private fb = inject(FormBuilder);
  private waService = inject(WhatsAppService);
  private snackBar = inject(MatSnackBar);

  loading = signal(true);
  busy = signal(false);
  overview = signal<WhatsAppOverview | null>(null);
  messages = signal<WhatsAppMessageView[]>([]);
  testPhone = '';

  credForm = this.fb.group({
    wabaId: ['', [Validators.required, Validators.pattern(/^\d{5,30}$/)]],
    phoneNumberId: ['', [Validators.required, Validators.pattern(/^\d{5,30}$/)]],
    accessToken: [''],
    supportContactText: ['', [Validators.maxLength(60)]]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.waService.getOverview(this.orgId).subscribe({
      next: (ov) => {
        this.overview.set(ov);
        this.loading.set(false);
        this.credForm.patchValue({
          wabaId: ov.config?.wabaId ?? '',
          phoneNumberId: ov.config?.phoneNumberId ?? '',
          accessToken: '',
          supportContactText: ov.config?.supportContactText ?? ''
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cargar WhatsApp'), 'Cerrar', { duration: 4000 });
      }
    });
    this.waService.getMessages(this.orgId, 20).subscribe({
      next: (list) => this.messages.set(list ?? []),
      error: () => this.messages.set([])
    });
  }

  // ── Acciones ────────────────────────────────────────────────────────────

  toggleModule(enabled: boolean): void {
    this.run(this.waService.setEnabled(this.orgId, enabled),
      enabled ? 'Módulo WhatsApp activado' : 'Módulo WhatsApp desactivado');
  }

  saveCredentials(): void {
    if (this.credForm.invalid) return;
    const v = this.credForm.value;
    this.run(this.waService.saveCredentials(this.orgId, {
      wabaId: (v.wabaId ?? '').trim(),
      phoneNumberId: (v.phoneNumberId ?? '').trim(),
      accessToken: (v.accessToken ?? '').trim() || undefined,
      supportContactText: (v.supportContactText ?? '').trim() || undefined
    }), 'Credenciales guardadas y verificadas');
  }

  verify(): void {
    this.run(this.waService.verify(this.orgId), 'Verificación completada');
  }

  syncTemplates(): void {
    this.run(this.waService.syncTemplates(this.orgId),
      'Plantillas enviadas a Meta. La aprobación puede tardar de minutos a 24 horas.');
  }

  refreshTemplates(): void {
    this.run(this.waService.refreshTemplates(this.orgId), 'Estado de plantillas actualizado');
  }

  toggleEvent(ev: WhatsAppEventView, enabled: boolean): void {
    this.run(this.waService.updateEvents(this.orgId, { [ev.key]: enabled }),
      enabled ? `"${ev.label}" activado` : `"${ev.label}" desactivado`);
  }

  sendTest(): void {
    const phone = this.testPhone.trim();
    if (!phone) return;
    this.busy.set(true);
    this.waService.sendTest(this.orgId, phone).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.snackBar.open(r.success ? 'Mensaje de prueba enviado' : `No se envió: ${r.errorMessage}`,
          'Cerrar', { duration: 5000 });
        this.load();
      },
      error: (err) => {
        this.busy.set(false);
        this.snackBar.open(apiErrorMessage(err, 'Error al enviar la prueba'), 'Cerrar', { duration: 5000 });
      }
    });
  }

  private run(obs: Observable<unknown>, okMessage: string): void {
    this.busy.set(true);
    obs.subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open(okMessage, 'Cerrar', { duration: 3500 });
        this.load();
      },
      error: (err) => {
        this.busy.set(false);
        this.snackBar.open(apiErrorMessage(err, 'Ocurrió un error'), 'Cerrar', { duration: 5000 });
        this.load();
      }
    });
  }

  // ── Presentación ────────────────────────────────────────────────────────

  count(status: string): number {
    return this.overview()?.monthCounts?.[status] ?? 0;
  }

  statusLabel(): string {
    const c = this.overview()?.config;
    if (!c || !c.enabled) return 'No activo';
    return ({ CONNECTED: 'Conectado', PENDING: 'Pendiente', ERROR: 'Error' } as Record<string, string>)[c.status] ?? c.status;
  }

  statusClass(): string {
    const c = this.overview()?.config;
    if (!c || !c.enabled) return 'off';
    return c.status === 'CONNECTED' ? 'ok' : c.status === 'ERROR' ? 'bad' : 'warn';
  }

  qualityLabel(q?: string): string {
    if (!q) return '—';
    return ({ GREEN: 'Alta', YELLOW: 'Media', RED: 'Baja' } as Record<string, string>)[q.toUpperCase()] ?? q;
  }

  templateLabel(s: string): string {
    return ({
      APPROVED: 'Aprobada', PENDING: 'En revisión', REJECTED: 'Rechazada', PAUSED: 'Pausada',
      DISABLED: 'Deshabilitada', NOT_CREATED: 'Sin crear', ERROR: 'Error al crear'
    } as Record<string, string>)[(s || '').toUpperCase()] ?? s;
  }

  templateClass(s: string): string {
    const v = (s || '').toUpperCase();
    if (v === 'APPROVED') return 'ok';
    if (v === 'PENDING' || v === 'IN_APPEAL') return 'warn';
    if (v === 'NOT_CREATED') return 'off';
    return 'bad';
  }

  eventLabel(key: string): string {
    if (key === 'TEST') return 'Prueba';
    return this.overview()?.events.find(e => e.key === key)?.label ?? key;
  }

  messageStatusLabel(s: string): string {
    return ({ SENT: 'Enviado', PENDING: 'En cola', SENDING: 'Enviando', FAILED: 'Fallido', SKIPPED: 'Omitido' } as Record<string, string>)[s] ?? s;
  }

  skipLabel(reason?: string): string {
    if (!reason) return '';
    return ({
      CLIENT_NOT_ACCEPTED: 'El cliente no autorizó notificaciones',
      INVALID_PHONE: 'Celular del cliente inválido',
      NOT_CONNECTED: 'WhatsApp no estaba conectado',
      TEMPLATE_NOT_APPROVED: 'La plantilla aún no está aprobada por Meta',
      INVALID_PARAMS: 'Datos incompletos para la plantilla',
      MODULE_DISABLED: 'Módulo desactivado'
    } as Record<string, string>)[reason] ?? reason;
  }
}
