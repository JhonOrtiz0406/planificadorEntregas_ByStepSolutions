@echo off
REM ============================================================
REM generate-frontend-env.bat
REM Reads .env from repo root and generates environment.ts
REM for local Angular development (Windows version).
REM
REM Usage:
REM   scripts\generate-frontend-env.bat
REM
REM Run once before `npm start` or `npm test` in the frontend.
REM ============================================================

setlocal EnableDelayedExpansion

set "ROOT_DIR=%~dp0.."
set "ENV_FILE=%ROOT_DIR%\.env"
set "OUT_FILE=%ROOT_DIR%\frontend\planificador-entregas\src\environments\environment.ts"

if not exist "%ENV_FILE%" (
  echo ERROR: %ENV_FILE% not found.
  echo Copy .env.example to .env and fill in real values.
  exit /b 1
)

REM Load .env variables (skip comments and empty lines)
for /f "usebackq tokens=1,* delims==" %%A in (`findstr /v "^#" "%ENV_FILE%" ^| findstr /v "^$"`) do (
  set "%%A=%%B"
)

REM Defaults for optional vars
if "!SERVER_PORT!"=="" set "SERVER_PORT=8084"
if "!APP_NAME!"=="" set "APP_NAME=DeliveryPlanner"

REM Write environment.ts
(
  echo // ============================================================
  echo // LOCAL DEVELOPMENT ENVIRONMENT — AUTO-GENERATED
  echo // Do NOT edit manually. Run scripts\generate-frontend-env.bat
  echo // to regenerate from .env.
  echo // ============================================================
  echo export const environment = {
  echo   production: false,
  echo   apiUrl: 'http://localhost:!SERVER_PORT!/api',
  echo   appName: '!APP_NAME!',
  echo   companyName: 'ByStep Solutions S.A.S.',
  echo   companyWebsite: 'https://www.bystepsolutions.tech/',
  echo   googleClientId: '!GOOGLE_CLIENT_ID!',
  echo   firebase: {
  echo     apiKey: '!FIREBASE_API_KEY!',
  echo     authDomain: '!FIREBASE_AUTH_DOMAIN!',
  echo     projectId: '!FIREBASE_PROJECT_ID!',
  echo     storageBucket: '!FIREBASE_STORAGE_BUCKET!',
  echo     messagingSenderId: '!FIREBASE_MESSAGING_SENDER_ID!',
  echo     appId: '!FIREBASE_APP_ID!',
  echo     vapidKey: '!FIREBASE_VAPID_KEY!'
  echo   }
  echo };
) > "%OUT_FILE%"

echo Generated: %OUT_FILE%
endlocal
