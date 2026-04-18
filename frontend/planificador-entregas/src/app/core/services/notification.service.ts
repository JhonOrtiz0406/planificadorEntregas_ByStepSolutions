import { Injectable, inject } from '@angular/core';
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, Messaging } from 'firebase/messaging';
import { environment } from '../../../environments/environment';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private messaging: Messaging | null = null;
  notification$ = new BehaviorSubject<any>(null);

  constructor() {
    this.initFirebase();
  }

  private initFirebase(): void {
    try {
      const app = initializeApp(environment.firebase);
      this.messaging = getMessaging(app);
      this.setupMessageListener();
    } catch (error) {
      console.error('Firebase initialization error:', error);
    }
  }

  async requestPermissionAndGetToken(): Promise<string | null> {
    if (!this.messaging) return null;
    try {
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        const token = await getToken(this.messaging, { vapidKey: environment.firebase.vapidKey });
        return token;
      }
    } catch (error) {
      console.error('FCM token error:', error);
    }
    return null;
  }

  private setupMessageListener(): void {
    if (!this.messaging) return;
    onMessage(this.messaging, payload => {
      console.log('FCM message received:', payload);
      this.notification$.next(payload);
      if (payload.notification) {
        this.showBrowserNotification(
          payload.notification.title || 'Notification',
          payload.notification.body || ''
        );
      }
    });
  }

  private showBrowserNotification(title: string, body: string): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, { body, icon: '/assets/images/logo.png' });
    }
  }
}
