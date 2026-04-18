import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from './core/services/auth.service';
import { NotificationService } from './core/services/notification.service';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule,
    MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule,
    MatSidenavModule, MatListModule, MatDividerModule, MatSnackBarModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  appName = environment.appName;
  sidenavOpen = false;

  get displayName(): string {
    const user = this.authService.currentUser();
    return user?.organizationName ?? this.appName;
  }

  get brandIcon(): string {
    const user = this.authService.currentUser();
    return user?.orgIconName || 'local_shipping';
  }

  ngOnInit(): void {
    this.notificationService.notification$.subscribe(notification => {
      if (notification?.notification) {
        this.snackBar.open(
          `${notification.notification.title}: ${notification.notification.body}`,
          'Ver',
          { duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
        );
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  toggleSidenav(): void {
    this.sidenavOpen = !this.sidenavOpen;
  }

  getRoleLabel(role?: string): string {
    const labels: Record<string, string> = {
      'PLATFORM_ADMIN': 'Administrador de Plataforma',
      'ORG_ADMIN': 'Administrador',
      'ORG_WORKER': 'Colaborador'
    };
    return role ? (labels[role] ?? role) : '';
  }

  get navItems() {
    const user = this.authService.currentUser();
    const items: { label: string; icon: string; route: string }[] = [];
    if (!user) return items;

    if (user.role === 'PLATFORM_ADMIN') {
      items.push({ label: 'Organizaciones', icon: 'business', route: '/organizations' });
    } else {
      items.push({ label: 'Calendario', icon: 'calendar_today', route: '/dashboard' });
      items.push({ label: 'Pedidos', icon: 'inventory_2', route: '/orders' });
      if (user.role === 'ORG_ADMIN') {
        const orgRoute = user.organizationId ? `/organizations/${user.organizationId}` : '/organizations';
        items.push({ label: 'Mi Equipo', icon: 'group', route: orgRoute });
      }
    }
    return items;
  }
}
