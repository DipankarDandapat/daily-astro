import os
from datetime import date, datetime
from zoneinfo import ZoneInfo

# Calendar day boundaries follow the audience's timezone, not the server's.
APP_TZ = ZoneInfo(os.getenv("DAILY_ASTRO_TZ", "Asia/Kolkata"))
# Sun/Moon times, Rahu Kaal and lucky hours are computed for this place.
DEFAULT_LAT = float(os.getenv("DAILY_ASTRO_LAT", "28.6139"))
DEFAULT_LNG = float(os.getenv("DAILY_ASTRO_LNG", "77.2090"))
DEFAULT_CITY = os.getenv("DAILY_ASTRO_CITY", "New Delhi")

SOURCE_BASE_URL = os.getenv("HOROSCOPE_SOURCE_BASE_URL", "https://freehoroscopeapi.com/api/v1").rstrip("/")
SOURCE_NAME = "freehoroscopeapi"
SOURCE_ATTRIBUTION = "Powered by FreeHoroscopeAPI"
SOURCE_LINK = "https://freehoroscopeapi.com"
HTTP_TIMEOUT = float(os.getenv("HOROSCOPE_SOURCE_TIMEOUT", "20"))

# gtx = translate.googleapis.com free endpoint, mymemory = api.mymemory.translated.net, none = English only.
TRANSLATION_PROVIDERS = [p.strip() for p in os.getenv("TRANSLATION_PROVIDERS", "gtx,mymemory").split(",") if p.strip()]

SCHEDULER_ENABLED = os.getenv("DAILY_ASTRO_SCHEDULER", "1") == "1"
INGEST_AT = os.getenv("DAILY_ASTRO_INGEST_AT", "00:05")
RETRY_MINUTES = int(os.getenv("DAILY_ASTRO_RETRY_MINUTES", "30"))
LOG_RETENTION_DAYS = int(os.getenv("DAILY_ASTRO_LOG_RETENTION_DAYS", "90"))


def now() -> datetime:
    return datetime.now(APP_TZ)


def today() -> date:
    return now().date()
