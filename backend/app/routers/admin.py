import os
from fastapi import APIRouter, Depends, Header, HTTPException, Query
from app import scheduler
from app.database import recent
from app.ingest import ingest_daily

router = APIRouter()

def require_admin(x_admin_key: str | None = Header(default=None)):
    expected = os.getenv("DAILY_ASTRO_ADMIN_KEY")
    if not expected or x_admin_key != expected:
        raise HTTPException(401, "Valid X-Admin-Key required")

@router.post("/ingest/daily", dependencies=[Depends(require_admin)])
def ingest(languages: str | None = Query(None, description="Comma-separated language codes to translate; default all")):
    try:
        return ingest_daily(trigger="admin", languages=languages.split(",") if languages is not None else None)
    except Exception as exc:
        raise HTTPException(502, f"Horoscope source ingestion failed: {exc}")

@router.get("/ingest/runs", dependencies=[Depends(require_admin)])
def runs(limit: int = Query(20, le=200)):
    return {"scheduler": scheduler.status, "runs": recent("ingestion_runs", limit)}

@router.get("/logs/fetches", dependencies=[Depends(require_admin)])
def fetch_logs(limit: int = Query(50, le=500)):
    return recent("source_fetch_logs", limit)

@router.get("/logs/requests", dependencies=[Depends(require_admin)])
def request_logs(limit: int = Query(50, le=500)):
    return recent("api_request_logs", limit)
