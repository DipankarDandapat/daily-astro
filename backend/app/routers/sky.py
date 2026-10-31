from datetime import timedelta
from typing import Literal
from fastapi import APIRouter, HTTPException, Query
from app import astro, sources
from app.config import DEFAULT_CITY, today
from app.data import SIGNS, LANGUAGES, localized_sign
from app.ingest import ensure_sky
from app.schemas import Sky, CompatibilityResponse
from app.translate import translate_many

router = APIRouter()


@router.get("/sky", response_model=Sky)
def sky(day: Literal["today", "tomorrow"] = Query("today")):
    target = today() + timedelta(days=1 if day == "tomorrow" else 0)
    try:
        snapshot = ensure_sky(target)
    except sources.SourceError as exc:
        raise HTTPException(503, f"Sky data unavailable: {exc}")
    return astro.sky_summary(snapshot["positions"], snapshot["times"], snapshot["panchang"]) | {"location": DEFAULT_CITY}


@router.get("/compatibility", response_model=CompatibilityResponse)
def compatibility(a: str = Query(...), b: str = Query(...), language: str = Query("en")):
    items = {x[0]: x for x in SIGNS}
    if a.lower() not in items or b.lower() not in items:
        raise HTTPException(404, "Unknown zodiac sign")
    if language not in {l["code"] for l in LANGUAGES}:
        raise HTTPException(400, f"Unsupported language '{language}'")
    match = astro.compatibility(a.lower(), b.lower())
    (summary,), _ = translate_many([match["summary"]], language)
    return {"a": localized_sign(items[a.lower()], language), "b": localized_sign(items[b.lower()], language), "score": match["score"],
            "summary": summary, "elements": match["elements"], "language": language}
