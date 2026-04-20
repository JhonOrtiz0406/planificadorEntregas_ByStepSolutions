importScripts('https://www.gstatic.com/firebasejs/11.6.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.6.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: '${FIREBASE_API_KEY}',
  authDomain: '${FIREBASE_AUTH_DOMAIN}',
  projectId: '${FIREBASE_PROJECT_ID}',
  storageBucket: '${FIREBASE_STORAGE_BUCKET}',
  messagingSenderId: '${FIREBASE_MESSAGING_SENDER_ID}',
  appId: '${FIREBASE_APP_ID}'
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const { title, body, icon } = payload.notification || {};
  self.registration.showNotification(title || 'Notificación', {
    body: body || '',
    icon: icon || '/favicon.ico'
  });
});
