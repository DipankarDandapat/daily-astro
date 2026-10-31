# Daily Astro

Daily horoscope app in 10 Indian languages, built from `New Daily Astro Design .html`.

## Structure

- `Android/` — native Kotlin + Jetpack Compose client.
- `backend/` — Python FastAPI service with versioned REST endpoints, a built-in daily scheduler and SQLite storage.

## Run the backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
#.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

On startup the service fetches today's readings if they are missing, then refreshes every day at `DAILY_ASTRO_INGEST_AT` (default `00:05`, `Asia/Kolkata`). It retries every 30 minutes until all 12 signs × 10 languages for the day are stored, so no external cron is needed. The Android emulator reaches the host backend at `http://10.0.2.2:8000`.

Run the tests (offline; every source is faked):

```bash
cd backend && .venv/bin/pytest -q
```

## Where the data comes from

| Data | Source | Notes |
| --- | --- | --- |
| Daily, weekly, monthly reading text | [FreeHoroscopeAPI](https://freehoroscopeapi.com) `/get-horoscope/{daily,weekly,monthly}` | Free, no key. Attribution required, so the app shows "Powered by FreeHoroscopeAPI". |
| Planetary positions (tropical), Sun/Moon times, Rahu Kaal, panchang | FreeHoroscopeAPI `/planetary-positions`, `/times`, `/panchang` | Calculated with Skyfield + JPL DE421. |
| Energy scores, category guidance, lucky color/number/time, special advice | `backend/app/astro.py` | Computed from that day's real planetary positions: whole-sign houses from the reader's sun sign, planetary dignity and retrogrades. Lucky time is the first daytime planetary hour (hora) of the day's luckiest planet that does not overlap Rahu Kaal. |
| Tomorrow | Computed sky forecast | The text source only publishes the current day, so "Tomorrow" is built from tomorrow's planetary positions and is labelled as a forecast. |
| Translations (hi, bn, te, mr, ta, gu, kn, ml, pa) | `translate.googleapis.com` (gtx) with MyMemory as fallback | Each translated string is cached in `translations` and each reading is stored per language. Set `TRANSLATION_PROVIDERS=none` to serve English only. |

Every outbound call is recorded in `source_fetch_logs`, every ingestion in `ingestion_runs`, and every `/api/*` request in `api_request_logs`. Logs are also written to `backend/logs/daily-astro.log`, and log rows older than 90 days are pruned.

## API (v1)

| Method & path | Purpose |
| --- | --- |
| `GET /api/v1/horoscope?sign=leo&language=hi&day=today` | `day` is `yesterday`, `today` or `tomorrow`. Includes categories with scores and details, energy, lucky color/number/time, affirmation, sky (moon phase, Rahu Kaal, tithi, nakshatra) and attribution. |
| `GET /api/v1/horoscope/today` | Same as above with `day=today` (kept for older clients). |
| `GET /api/v1/horoscope/history?sign=&language=&days=7` | Stored readings for the last N days. |
| `GET /api/v1/horoscope/period?sign=&period=weekly\|monthly&language=` | Weekly/monthly reading. |
| `GET /api/v1/sky?day=today` | Sky summary for the configured city. |
| `GET /api/v1/compatibility?a=aries&b=leo&language=` | Sign match score and summary. |
| `POST /api/v1/users/device` | Password-less device profile (name, language, sign, birth date, notification, theme settings). |
| `GET /api/v1/users/{device_id}/stats` | Day streak, readings read, saved count. |
| `GET/POST /api/v1/users/{device_id}/saved`, `DELETE …/saved/{date}/{sign}` | Saved horoscopes. |
| `DELETE /api/v1/users/{device_id}` | Delete my data. |
| `POST /api/v1/feedback` | Rating and/or message. |
| `POST /api/v1/admin/ingest/daily`, `GET /api/v1/admin/ingest/runs`, `GET /api/v1/admin/logs/{fetches,requests}` | Need the `X-Admin-Key` header matching `DAILY_ASTRO_ADMIN_KEY`. |

Password registration and login (`/users/register`, `/users/login`, `/users/me`) are unchanged. Send `X-Device-Id` with horoscope requests to count reading streaks.

### Configuration

| Variable | Default |
| --- | --- |
| `DAILY_ASTRO_DB` | `backend/daily_astro.db` |
| `DAILY_ASTRO_TZ` | `Asia/Kolkata` |
| `DAILY_ASTRO_LAT`, `DAILY_ASTRO_LNG`, `DAILY_ASTRO_CITY` | New Delhi (used for sunrise, Rahu Kaal, lucky hour, panchang) |
| `DAILY_ASTRO_SCHEDULER` | `1` (set `0` to disable the built-in scheduler) |
| `DAILY_ASTRO_INGEST_AT`, `DAILY_ASTRO_RETRY_MINUTES` | `00:05`, `30` |
| `TRANSLATION_PROVIDERS` | `gtx,mymemory` |
| `DAILY_ASTRO_ADMIN_KEY` | unset (admin endpoints disabled) |

`python -m app.ingest` still runs a one-off ingestion; `backend/scripts/crontab.example` is kept for setups that disable the in-process scheduler.

Zodiac IDs are stable for storage, while names are localized. See `docs/localization.md`.

## Android app

Onboarding (welcome/name → language → sun sign), then four tabs:

- **Home** — greeting, your sign, today's energy bars, lucky color/number/time, daily affirmation, today's sky (moon phase, sunrise/sunset, Rahu Kaal, tithi, nakshatra, retrogrades) and other signs to explore.
- **Horoscope** — Yesterday / Today / Tomorrow, the reading, special advice, expandable Love, Career, Health, Spiritual and Lucky sections with scores, weekly and monthly readings, plus save and share.
- **Profile** — name, sun sign, birth date (suggests your sign), language, notifications, and stats (streak, read, saved).
- **More** — language, notification frequency and time (once, twice or weekly, with quiet hours from 10 PM to 6 AM), appearance (system, light or dark), large text, a home-screen widget, offline cache, history, saved readings, compatibility, share, feedback and rating, privacy, delete my data, and about.

The UI is translated into all 10 languages (`res/values-*/strings.xml`). Strings come from the design file where it has them and are machine translated otherwise, so a native-speaker review is welcome.

```bash
cd Android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```
