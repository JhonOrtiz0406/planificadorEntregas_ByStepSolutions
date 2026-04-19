import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'select-org',
    loadComponent: () => import('./features/auth/org-selection/org-selection.component').then(m => m.OrgSelectionComponent)
  },
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'invite/:token',
        loadComponent: () => import('./features/auth/invite-accept/invite-accept.component').then(m => m.InviteAcceptComponent)
      },
      {
        path: 'no-access',
        loadComponent: () => import('./features/auth/no-access/no-access.component').then(m => m.NoAccessComponent)
      }
    ]
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard('ORG_ADMIN', 'ORG_EMPLOYEE', 'ORG_DELIVERY')],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard, roleGuard('ORG_ADMIN', 'ORG_EMPLOYEE', 'ORG_DELIVERY')],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/orders/order-list/order-list.component').then(m => m.OrderListComponent)
      },
      {
        path: 'new',
        canActivate: [roleGuard('ORG_ADMIN', 'ORG_EMPLOYEE')],
        loadComponent: () => import('./features/orders/order-form/order-form.component').then(m => m.OrderFormComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent)
      },
      {
        path: ':id/edit',
        canActivate: [roleGuard('ORG_ADMIN')],
        loadComponent: () => import('./features/orders/order-form/order-form.component').then(m => m.OrderFormComponent)
      }
    ]
  },
  {
    path: 'organizations',
    canActivate: [authGuard, roleGuard('PLATFORM_ADMIN', 'ORG_ADMIN')],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/organizations/org-list/org-list.component').then(m => m.OrgListComponent)
      },
      {
        path: 'new',
        canActivate: [roleGuard('PLATFORM_ADMIN')],
        loadComponent: () => import('./features/organizations/org-form/org-form.component').then(m => m.OrgFormComponent)
      },
      {
        path: ':id',
        loadComponent: () => import('./features/organizations/org-detail/org-detail.component').then(m => m.OrgDetailComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/dashboard' }
];
