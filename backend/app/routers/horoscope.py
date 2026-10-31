from datetime import date
from fastapi import APIRouter, Query, HTTPException
from app.data import SIGNS, localized_sign
from app.database import get_horoscope
from app.schemas import HoroscopeResponse

router = APIRouter()

@router.get("/horoscope/today", response_model=HoroscopeResponse)
def today(sign: str = Query(...), language: str = Query("en")):
    item = next((x for x in SIGNS if x[0] == sign.lower()), None)
    if not item:
        raise HTTPException(404, "Unknown zodiac sign")
    cached = get_horoscope(date.today().isoformat(), item[0], language)
    if not cached:
        raise HTTPException(503, "Today's horoscope has not been ingested yet. Run POST /api/v1/admin/ingest/daily.")
    return {"date": cached["horoscope_date"], "sign": localized_sign(item, language), "language": language, "summary": cached["summary"], "special_advice": cached["special_advice"], "lucky_color": cached["lucky_color"], "lucky_number": cached["lucky_number"], "source": cached["source"], "categories": [
        {"key":"love","title":"Love & Relationship","summary":"Be open with your partner and let thoughtful communication lead."},
        {"key":"career","title":"Career & Finance","summary":"Take initiative while staying mindful of your spending habits."},
        {"key":"health","title":"Health & Wellness","summary":"Keep a balanced diet and stay hydrated today."},
        {"key":"spiritual","title":"Spiritual Growth","summary":"Meditation or journaling can clarify your thoughts."},
        {"key":"lucky","title":"Lucky Insights","summary":f"Lucky color: {cached['lucky_color']} · Lucky number: {cached['lucky_number']}"},
    ]}
