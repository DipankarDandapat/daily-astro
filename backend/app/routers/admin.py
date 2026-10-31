import os
from fastapi import APIRouter, Header, HTTPException
from app.ingest import ingest_daily

router = APIRouter()

@router.post("/ingest/daily")
def ingest(x_admin_key: str | None = Header(default=None)):
    expected = os.getenv("DAILY_ASTRO_ADMIN_KEY")
    if not expected or x_admin_key != expected:
        raise HTTPException(401, "Valid X-Admin-Key required")
    try:
        return ingest_daily()
    except Exception as exc:
        raise HTTPException(502, f"Horoscope source ingestion failed: {exc}")
