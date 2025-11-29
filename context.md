# RingMyDevice – working notes

## Goal
- Self-hosted RMD server (forked from fmd-server) with app support for registration/login, camera uploads, and UnifiedPush (Sunup) so web commands (ring, etc.) reach the device.

## Server (rmd-server)
- Run HTTP quick start: `./rmd-server.sh` → http://<LAN-IP>:8080 (DB in `rmddata/db`).
- If `certs/server.crt/key` exist, the script runs HTTPS on 8443; otherwise HTTP on 8080.
- Push endpoint stored in SQLite `rmddata/db/rmd.sqlite` table `rmd_users.push_url`.
- Web portal files: `web/index.html`, `web/logic.js`, `web/style.css`.
  - Added Push status + inline panel to view/save endpoint (POST `/api/v1/push`).
  - “Configured” shown when server returns a non-empty push URL via `/api/v1/push` (POST, IDT=access token, Data=endpoint or blank to query).
- Build check: `GOCACHE=.gocache GOMODCACHE=.gomodcache go build ./...` (for CI/sandbox).

## App (Kotlin/Compose)
- Dependencies include UnifiedPush connector (JitPack repo added in `settings.gradle.kts`).
- PushReceiver listens to UnifiedPush events; on new endpoint it stores locally and attempts to POST to `/api/v1/push`, auto-login if token is missing and “remember password” is enabled.
- Login/Register flows:
  - “Remember password” option; if enabled, auto re-login on 401 and when saving push endpoint.
  - Starts lightweight poller after auth; attempts to register/post push endpoint on login/register/verify.
- Camera capture saves to gallery and uploads to `/api/v1/picture` via POST.
- UI file: `app/src/main/java/com/github/ringmydevice/ui/settings/FmdServerScreen.kt` (push card, auth, verify, etc.).

## Current gaps / TODO
- Server DB shows empty `push_url` for some users; web still warns “UnifiedPush not configured.” Need to ensure push endpoint is posted and persisted:
  - Confirm `/api/v1/push` accepts POST with IDT=access token, Data=<endpoint> (same as fmd-server).
  - Verify app is actually posting the endpoint (check logs/DB); align with fmd-android behavior from https://fmd-foss.org/docs/fmd-android/push/.
- Web portal ring/commands still require valid push URL; once stored, status should show “Configured.”
- Gradle wrapper may need `GRADLE_USER_HOME=./.gradle ./gradlew :app:assembleDebug` in restricted environments.

## Reference
- FMD push doc: https://fmd-foss.org/docs/fmd-android/push/
- FMD behavior: distributor Sunup, push URL shown in app, registered via `/push` on server.
