import json, os
from datetime import date
import httpx
from app.data import SIGNS
from app.database import init_db, save_horoscope

HOROSCOPE_SOURCE_URL = os.getenv("HOROSCOPE_SOURCE_URL", "https://freehoroscopeapi.com/api/v1/get-horoscope/daily")

def fetch_sign(sign: str) -> dict:
    response = httpx.get(HOROSCOPE_SOURCE_URL, params={"sign": sign.title(), "day": "TODAY"}, timeout=20, follow_redirects=True)
    response.raise_for_status()
    body = response.json().get("data", response.json())
    return {"description": body.get("horoscope", ""), "mood": "Focused", "color": "Red", "lucky_number": "7", "source_date": body.get("date")}

def ingest_daily(language: str = "en") -> dict:
    init_db(); today = date.today().isoformat(); stored = []
    for sign_id, sign_name, date_range, symbol in SIGNS:
        payload = fetch_sign(sign_id)
        description = str(payload.get("description", "")).strip()
        if not description: raise RuntimeError(f"Source returned no description for {sign_id}")
        save_horoscope((today, sign_id, language, "freehoroscopeapi", description, f"Focus on self-reflection and communication today. {payload.get('mood', '')}".strip(), str(payload.get("color", "Red")).strip(), int(str(payload.get("lucky_number", "7")).strip() or 7), json.dumps(payload)))
        stored.append(sign_id)
    return {"date": today, "language": language, "source": "freehoroscopeapi", "stored": stored}

if __name__ == "__main__":
    print(ingest_daily())
