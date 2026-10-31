import copy, json, logging, threading
from concurrent.futures import ThreadPoolExecutor
from datetime import date, timedelta
from app import astro, sources
from app.config import APP_TZ, DEFAULT_CITY, DEFAULT_LAT, DEFAULT_LNG, SOURCE_NAME, today
from app.data import LANGUAGES, SIGNS
from app.database import (init_db, save_horoscope, get_horoscope, save_sky, get_sky, save_period, get_period,
                          start_run, finish_run, count_horoscopes)
from app.translate import translate_many

log = logging.getLogger("daily_astro.ingest")
LOCATION = f"{DEFAULT_LAT:.4f},{DEFAULT_LNG:.4f}"
WORKERS = 4  # the source answers in ~10s per call; a few parallel requests keep a full run to about a minute
_run_lock = threading.Lock()


def fetch_sign(sign: str) -> dict:
    data = sources.horoscope_text(sign, "daily")
    return {"description": str(data["horoscope"]).strip(), "source_date": data.get("date"), "raw": data}


def ensure_sky(day: date, refresh: bool = False) -> dict:
    """Live sky for a date: tropical positions at local noon (required) plus Sun/Moon times and panchang (best effort)."""
    cached = None if refresh else get_sky(day.isoformat(), LOCATION)
    if cached and cached["times"] and cached["panchang"]:
        return cached
    positions = cached["positions"] if cached else sources.planetary_positions(astro.noon(day, APP_TZ), "tropical")
    times = panchang = None
    try: times = sources.sun_moon_times(DEFAULT_LAT, DEFAULT_LNG, day.isoformat())
    except sources.SourceError as exc: log.warning("times unavailable for %s: %s", day, exc)
    try: panchang = sources.panchang(DEFAULT_LAT, DEFAULT_LNG, day.isoformat())
    except sources.SourceError as exc: log.warning("panchang unavailable for %s: %s", day, exc)
    save_sky(day.isoformat(), LOCATION, positions, times, panchang)
    return get_sky(day.isoformat(), LOCATION)


def build_row(day: date, sign_id: str, description: str, raw: dict) -> tuple[tuple, dict]:
    sky = ensure_sky(day)
    insights = astro.insights(sky["positions"], sky["times"], sky["panchang"], sign_id, day)
    insights["location"] = DEFAULT_CITY
    row = (day.isoformat(), sign_id, "en", SOURCE_NAME, description, insights["special_advice"],
           insights["lucky"]["color"], insights["lucky"]["number"], json.dumps(raw, ensure_ascii=False))
    return row, insights


def localize(summary: str, insights: dict, language: str) -> tuple[str, dict, str]:
    """Translates every user-facing string of an English reading. Returns (summary, insights, provider)."""
    out = copy.deepcopy(insights)
    texts = [summary, out["special_advice"], out["affirmation"], out["lucky"]["color"]]
    for category in out["categories"]:
        texts.append(category["title"])
        for part in category["details"]:
            texts += [part["label"], part["text"]]
    translated, provider = translate_many(texts, language)
    it = iter(translated)
    summary, out["special_advice"], out["affirmation"], out["lucky"]["color"] = next(it), next(it), next(it), next(it)
    for category in out["categories"]:
        category["title"] = next(it)
        for part in category["details"]:
            part["label"], part["text"] = next(it), next(it)
        if category["key"] != "lucky":
            category["summary"] = " ".join(p["text"] for p in category["details"])
    out["translated"] = provider not in ("none", "source")
    return summary, out, provider


def store_language(day: str, sign_id: str, language: str) -> dict | None:
    english = get_horoscope(day, sign_id, "en")
    if not english or not english.get("insights_json"):
        return None
    summary, insights, provider = localize(english["summary"], json.loads(english["insights_json"]), language)
    save_horoscope((day, sign_id, language, english["source"], summary, insights["special_advice"], insights["lucky"]["color"],
                    english["lucky_number"], english["raw_json"]), insights, provider)
    return get_horoscope(day, sign_id, language)


def ingest_sign(sign_id: str) -> str:
    """Live-fetches one sign (used when a request arrives before the daily job ran). Returns the source date stored."""
    payload = fetch_sign(sign_id)
    day = date.fromisoformat(payload.get("source_date") or today().isoformat())
    row, insights = build_row(day, sign_id, payload["description"], payload.get("raw", payload))
    save_horoscope(row, insights, "source")
    return day.isoformat()


def ingest_periods(sign_id: str, day: date):
    for period, key in (("weekly", astro.week_key(day)), ("monthly", day.strftime("%Y-%m"))):
        if get_period(period, key, sign_id, "en"):
            continue
        try:
            data = sources.horoscope_text(sign_id, period)
            save_period(period, key, sign_id, "en", str(data["horoscope"]).strip(), data)
        except sources.SourceError as exc:
            log.warning("%s reading for %s unavailable: %s", period, sign_id, exc)


def ingest_daily(language: str = "en", trigger: str = "manual", languages: list[str] | None = None, periods: bool = True) -> dict:
    """Fetches today's reading for all 12 signs, computes insights from the live sky, stores English and every translation."""
    init_db()
    target = today()
    with _run_lock:
        run_id = start_run(trigger, target.isoformat())
        stored, translated, failures, source_date = [], 0, [], None
        try:
            ensure_sky(target)
            def one(sign_id):
                payload = fetch_sign(sign_id)
                description = str(payload.get("description", "")).strip()
                if not description:
                    raise RuntimeError(f"Source returned no description for {sign_id}")
                day = payload.get("source_date") or target.isoformat()
                row, insights = build_row(date.fromisoformat(day), sign_id, description, payload.get("raw", payload))
                save_horoscope(row, insights, "source")
                if periods:
                    ingest_periods(sign_id, target)
                return day
            with ThreadPoolExecutor(WORKERS) as pool:
                futures = {sign_id: pool.submit(one, sign_id) for sign_id, *_ in SIGNS}
            for sign_id, future in futures.items():
                try:
                    source_date = future.result()
                    stored.append(sign_id)
                except Exception as exc:
                    log.warning("ingest failed for %s: %s", sign_id, exc)
                    failures.append(f"{sign_id}: {exc}")
            if not stored:
                raise RuntimeError("; ".join(failures) or "nothing stored")
            try: ensure_sky(target + timedelta(days=1))
            except Exception as exc: log.warning("tomorrow's sky unavailable: %s", exc)
            wanted = languages if languages is not None else [l["code"] for l in LANGUAGES if l["code"] != "en"]
            if language != "en" and language not in wanted:
                wanted.append(language)
            def translate_language(code):
                rows = [store_language(source_date, sign_id, code) for sign_id in stored]
                return sum(1 for row in rows if row and row["translation_provider"] != "none")
            with ThreadPoolExecutor(3) as pool:
                translated = sum(pool.map(translate_language, wanted))
            status = "ok" if not failures else "partial"
            finish_run(run_id, status, len(stored), translated, source_date, "; ".join(failures) or None)
        except Exception as exc:
            finish_run(run_id, "failed", len(stored), translated, source_date, str(exc)[:1000])
            raise
    log.info("ingest %s: %s signs, %s translations, source date %s", status, len(stored), translated, source_date)
    return {"date": source_date, "target_date": target.isoformat(), "language": language, "source": SOURCE_NAME, "stored": stored,
            "translated": translated, "failures": failures, "run_id": run_id, "status": status}


def is_complete(day: date) -> bool:
    return count_horoscopes(day.isoformat(), "en") >= len(SIGNS) and count_horoscopes(day.isoformat()) >= len(SIGNS) * len(LANGUAGES)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    print(json.dumps(ingest_daily(trigger="cli"), indent=2, ensure_ascii=False))
