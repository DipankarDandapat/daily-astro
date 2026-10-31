import json, logging
from datetime import date, timedelta
from typing import Literal
from fastapi import APIRouter, Header, Query, HTTPException
from app import astro, sources
from app.config import SOURCE_ATTRIBUTION, SOURCE_LINK, DEFAULT_CITY, today
from app.data import SIGNS, LANGUAGES, localized_sign
from app.database import get_horoscope, latest_horoscope_date, horoscope_dates, record_read, list_bookmarks, get_period, save_period
from app.ingest import ingest_sign, store_language, ensure_sky, localize, ingest_periods
from app.schemas import HoroscopeResponse, HistoryItem, PeriodResponse
from app.translate import translate_many

router = APIRouter()
log = logging.getLogger("daily_astro.horoscope")
LANGUAGE_CODES = {l["code"] for l in LANGUAGES}


def resolve(sign: str, language: str):
    item = next((x for x in SIGNS if x[0] == sign.lower()), None)
    if not item:
        raise HTTPException(404, "Unknown zodiac sign")
    if language not in LANGUAGE_CODES:
        raise HTTPException(400, f"Unsupported language '{language}'")
    return item


def row_in_language(day: str, sign_id: str, language: str):
    row = get_horoscope(day, sign_id, language)
    if not row and language != "en" and get_horoscope(day, sign_id, "en"):
        row = store_language(day, sign_id, language)
    return row


def to_response(row, item, language, day_label, device_id=None):
    insights = json.loads(row["insights_json"]) if row.get("insights_json") else None
    response = {"date": row["horoscope_date"], "day": day_label, "kind": "daily", "sign": localized_sign(item, language), "language": language,
                "summary": row["summary"], "special_advice": row["special_advice"], "lucky_color": row["lucky_color"], "lucky_number": row["lucky_number"],
                "source": row["source"], "attribution": SOURCE_ATTRIBUTION, "attribution_url": SOURCE_LINK, "fetched_at": row.get("fetched_at")}
    if insights:
        response.update(categories=insights["categories"], energy=insights["energy"], affirmation=insights["affirmation"],
                        lucky=insights["lucky"], sky=insights["sky"] | {"location": insights.get("location", DEFAULT_CITY)},
                        translated=insights.get("translated", False))
    else:  # rows stored before insights existed
        response["categories"] = [{"key": "lucky", "title": "Lucky Insights", "summary": f"Lucky color: {row['lucky_color']} · Lucky number: {row['lucky_number']}"}]
    if device_id:
        response["saved"] = any(b["horoscope_date"] == row["horoscope_date"] and b["sign_id"] == item[0] for b in list_bookmarks(device_id))
    return response


def daily(item, language, target: date, allow_live: bool):
    sign_id = item[0]
    row = row_in_language(target.isoformat(), sign_id, language)
    if row or not allow_live:
        return row
    try:
        stored = ingest_sign(sign_id)
    except Exception as exc:
        log.warning("live fetch for %s failed: %s", sign_id, exc)
        stored = latest_horoscope_date(sign_id, "en", target.isoformat())  # serve the newest stored reading rather than nothing
    return row_in_language(stored, sign_id, language) if stored else None


def forecast(item, language, target: date):
    try:
        sky = ensure_sky(target)
    except sources.SourceError as exc:
        raise HTTPException(503, f"Tomorrow's sky is unavailable right now: {exc}")
    insights = astro.insights(sky["positions"], sky["times"], sky["panchang"], item[0], target)
    insights["location"] = DEFAULT_CITY
    summary, insights, provider = localize(astro.forecast_summary(sky["positions"], item[0], insights["energy"]), insights, language)
    row = {"horoscope_date": target.isoformat(), "summary": summary, "special_advice": insights["special_advice"], "lucky_color": insights["lucky"]["color"],
           "lucky_number": insights["lucky"]["number"], "source": "computed", "fetched_at": sky["fetched_at"], "insights_json": json.dumps(insights)}
    return row


@router.get("/horoscope", response_model=HoroscopeResponse)
def horoscope(sign: str = Query(...), language: str = Query("en"), day: Literal["yesterday", "today", "tomorrow"] = Query("today"),
              x_device_id: str | None = Header(default=None)):
    item = resolve(sign, language)
    current = today()
    if day == "tomorrow":
        response = to_response(forecast(item, language, current + timedelta(days=1)), item, language, day, x_device_id)
        response["kind"] = "forecast"
        return response
    target = current if day == "today" else current - timedelta(days=1)
    row = daily(item, language, target, allow_live=day == "today")
    if not row:
        if day == "yesterday":
            raise HTTPException(404, "No reading was stored for yesterday.")
        raise HTTPException(503, "Today's horoscope is not available yet. Please try again shortly.")
    if x_device_id:
        record_read(x_device_id, current.isoformat(), row["horoscope_date"], item[0], day, language)
    return to_response(row, item, language, day, x_device_id)


@router.get("/horoscope/today", response_model=HoroscopeResponse)
def today_horoscope(sign: str = Query(...), language: str = Query("en"), x_device_id: str | None = Header(default=None)):
    return horoscope(sign, language, "today", x_device_id)


@router.get("/horoscope/history", response_model=list[HistoryItem])
def history(sign: str = Query(...), language: str = Query("en"), days: int = Query(7, ge=1, le=60)):
    item = resolve(sign, language)
    until = today()
    since = until - timedelta(days=days - 1)
    result = []
    for day in horoscope_dates(item[0], "en", since.isoformat(), until.isoformat()):
        row = row_in_language(day, item[0], language)
        if row:
            result.append({"date": day, "sign": item[0], "summary": row["summary"]})
    return result


@router.get("/horoscope/period", response_model=PeriodResponse)
def period(sign: str = Query(...), period: Literal["weekly", "monthly"] = Query(...), language: str = Query("en")):
    item = resolve(sign, language)
    key = astro.week_key(today()) if period == "weekly" else today().strftime("%Y-%m")
    row = get_period(period, key, item[0], language)
    if not row:
        english = get_period(period, key, item[0], "en")
        if not english:
            ingest_periods(item[0], today())
            english = get_period(period, key, item[0], "en")
        if not english:
            raise HTTPException(503, f"The {period} reading is not available right now.")
        if language != "en":
            (text,), provider = translate_many([english["summary"]], language)
            save_period(period, key, item[0], language, text, {"provider": provider})
            row = get_period(period, key, item[0], language)
        else:
            row = english
    return {"period": period, "period_key": key, "sign": localized_sign(item, language), "language": language, "summary": row["summary"]}
