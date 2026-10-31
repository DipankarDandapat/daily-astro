# Daily Astro

Daily horoscope app based on the supplied PRD/UI reference.

## Structure

- `Android/` — native Kotlin + Jetpack Compose client.
- `backend/` — Python FastAPI service with versioned REST endpoints.

## Run the backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
#.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

The Android emulator can reach the host backend at `http://10.0.2.2:8000`.

## Authentication

Register with `POST /api/v1/users/register` using `{ "device_id": "...", "name": "...", "password": "at-least-8-chars" }`. Log in with `POST /api/v1/users/login` using `{ "identifier": "device-id-or-user-id", "password": "..." }`; the response contains a signed bearer token. Use that token with `GET /api/v1/users/me`.

## Daily internet ingestion

The service stores data in `backend/daily_astro.db`. Run the daily ingestion job with:

```bash
export DAILY_ASTRO_ADMIN_KEY="replace-me"
python -m app.ingest
```

For a running server, set the same key and call `POST /api/v1/admin/ingest/daily` with the `X-Admin-Key` header. The job fetches today's reading for all 12 signs from the configurable internet source, then upserts the date/sign/language rows into SQLite. Schedule the command once daily with cron or a hosted scheduler. The API only serves the stored daily row and returns `503` until that day's ingestion has completed. See `backend/scripts/ingest_daily.sh` and `backend/scripts/crontab.example` for the scheduler entrypoint.

The source URL can be changed with `HOROSCOPE_SOURCE_URL`. The default source is the public `freehoroscopeapi.com` daily JSON endpoint. Its response is normalized into the app's own database schema, so the Android app never depends directly on the external provider.

Zodiac IDs are stable for storage, while names are localized. The Android app requests `/api/v1/zodiac-signs?language=hi` after Hindi is selected, so `aries` displays as `मेष`; Tamil, Bengali, Telugu, and the other supported languages use their own localized names. See `docs/localization.md` for the contract.

## Product flow

Welcome/name → language → zodiac sign → today's horoscope. The client holds onboarding choices in the ViewModel and loads language-specific horoscope content from the API. The visual system follows the reference: deep navy, bright cobalt CTA, white cards, rounded surfaces, and warm astrology accents.
```commandline
cd Android
.\gradlew assembleDebug

adb install app/build/outputs/apk/debug/app-debug.apk
```