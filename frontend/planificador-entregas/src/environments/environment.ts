// ============================================================
// LOCAL DEVELOPMENT ENVIRONMENT
//
// DO NOT put real secrets here — this file is tracked in git.
// Run `scripts/generate-frontend-env.sh` (or .bat) to populate
// from your local .env file before running `npm start`.
// ============================================================
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8084/api',
  appName: 'DeliveryPlanner',
  companyName: 'ByStep Solutions S.A.S.',
  companyWebsite: 'https://www.bystepsolutions.tech/',
  googleClientId: '',
  firebase: {
    apiKey: '',
    authDomain: '',
    projectId: '',
    storageBucket: '',
    messagingSenderId: '',
    appId: '',
    vapidKey: ''
  }
};
