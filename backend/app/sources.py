"""Clients for the public astrology data source. Every outbound call is written to source_fetch_logs."""
import logging, time
import httpx
from app.config import SOURCE_BASE_URL, SOURCE_NAME, HTTP_TIMEOUT
from app.database import log_fetch

log = logging.getLogger("daily_astro.sources")


class SourceError(RuntimeError):
    pass


def get_json(url: str, params: dict | None = None, source: str = SOURCE_NAME, retries: int = 2) -> dict:
    last_error = None
    for attempt in range(retries + 1):
        started = time.monotonic()
        status = None
        try:
            response = httpx.get(url, params=params, timeout=HTTP_TIMEOUT, follow_redirects=True, headers={"User-Agent": "DailyAstro/2.0 (+https://github.com/DipankarDandapat/daily-astro)"})
            status = response.status_code
            response.raise_for_status()
            body = response.json()
            log_fetch(source, str(response.url), status, int((time.monotonic() - started) * 1000), True)
            return body
        except (httpx.HTTPError, ValueError) as exc:
            last_error = exc
            full_url = str(httpx.URL(url, params=params))
            log_fetch(source, full_url, status, int((time.monotonic() - started) * 1000), False, str(exc)[:500])
            log.warning("fetch failed (%s/%s) %s: %s", attempt + 1, retries + 1, full_url, exc)
            # 4xx means the request itself is wrong; retrying will not help.
            if status is not None and 400 <= status < 500:
                break
            time.sleep(1.5 * (attempt + 1))
    raise SourceError(f"{source} request failed: {last_error}")


def horoscope_text(sign: str, period: str = "daily") -> dict:
    """Returns {date, period, sign, horoscope} for the current daily/weekly/monthly period."""
    body = get_json(f"{SOURCE_BASE_URL}/get-horoscope/{period}", {"sign": sign})
    data = body.get("data", body)
    if not str(data.get("horoscope", "")).strip():
        raise SourceError(f"{SOURCE_NAME} returned no {period} text for {sign}")
    return data


def planetary_positions(datetime_iso: str, zodiac: str = "tropical") -> dict:
    return get_json(f"{SOURCE_BASE_URL}/planetary-positions", {"datetime": datetime_iso, "zodiac": zodiac})


def sun_moon_times(lat: float, lng: float, day: str) -> dict:
    return get_json(f"{SOURCE_BASE_URL}/times", {"lat": lat, "lng": lng, "date": day, "timeformat": "24", "include": "moon_details"})


def panchang(lat: float, lng: float, day: str) -> dict:
    return get_json(f"{SOURCE_BASE_URL}/panchang", {"lat": lat, "lng": lng, "date": day})
