import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-no-access',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatInputModule, MatFormFieldModule,
    MatIconModule, MatProgressSpinnerModule, MatSnackBarModule
  ],
  template: `
    <div class="container">
      <mat-card class="card">
        <mat-card-header>
          <mat-icon mat-card-avatar color="warn">block</mat-icon>
          <mat-card-title>Sin acceso a la plataforma</mat-card-title>
          <mat-card-subtitle>Tu cuenta no pertenece a ninguna organización activa</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          @if (!submitted()) {
            <p class="info-text">
              Si crees que esto es un error, completa el formulario y nos pondremos en contacto contigo.
            </p>

            <form [formGroup]="form" (ngSubmit)="send()" class="contact-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nombre completo</mat-label>
                <input matInput formControlName="name" placeholder="Tu nombre">
                <mat-error>Requerido</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Correo electrónico</mat-label>
                <input matInput formControlName="email" type="email" placeholder="tu@correo.com">
                <mat-error>Correo inválido</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Celular / WhatsApp</mat-label>
                <input matInput formControlName="phone" placeholder="+57 300 000 0000">
                <mat-error>Requerido</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Organización (opcional)</mat-label>
                <input matInput formControlName="organizationName" placeholder="Nombre de tu organización">
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Mensaje adicional (opcional)</mat-label>
                <textarea matInput formControlName="message" rows="3"
                          placeholder="Cuéntanos tu situación..."></textarea>
              </mat-form-field>

              <div class="actions">
                <button mat-raised-button color="primary" type="submit"
                        [disabled]="form.invalid || loading()">
                  @if (loading()) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    Enviar solicitud de soporte
                  }
                </button>
                <button mat-button type="button" (click)="goLogin()">
                  Volver al inicio de sesión
                </button>
              </div>
            </form>
          } @else {
            <div class="success-msg">
              <mat-icon class="success-icon">check_circle</mat-icon>
              <h3>¡Solicitud enviada!</h3>
              <p>El equipo de soporte revisará tu caso y te contactará al correo <strong>{{ form.value.email }}</strong>.</p>
              <button mat-raised-button color="primary" (click)="goLogin()">
                Volver al inicio de sesión
              </button>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container {
      display: flex; align-items: center; justify-content: center;
      min-height: 100vh; background: #f5f5f5; padding: 16px;
    }
    .card { width: 100%; max-width: 500px; }
    .info-text { color: #555; margin: 16px 0 8px; line-height: 1.5; }
    .contact-form { display: flex; flex-direction: column; gap: 4px; margin-top: 16px; }
    .full-width { width: 100%; }
    .actions { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
    .success-msg { text-align: center; padding: 24px 0; }
    .success-icon { font-size: 56px; height: 56px; width: 56px; color: #4caf50; }
    h3 { color: #1e1b4b; margin: 16px 0 8px; }
  `]
})
export class NoAccessComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);

  loading = signal(false);
  submitted = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    organizationName: [''],
    message: ['']
  });

  ngOnInit(): void {
    const stored = this.authService.getNoAccessUser();
    if (stored) {
      this.form.patchValue({ name: stored.name, email: stored.email });
    }
  }

  send(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    const val = this.form.value;
    this.authService.contactSupport({
      name: val.name!,
      email: val.email!,
      phone: val.phone!,
      organizationName: val.organizationName || undefined,
      message: val.message || undefined
    }).subscribe({
      next: () => {
        this.authService.clearNoAccessUser();
        this.submitted.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error al enviar la solicitud. Inténtalo de nuevo.', 'Cerrar', { duration: 4000 });
        this.loading.set(false);
      }
    });
  }

  goLogin(): void {
    this.authService.clearNoAccessUser();
    this.router.navigate(['/auth/login']);
  }
}
