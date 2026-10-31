"""In-process daily scheduler: keeps today's readings stored without an external cron.

Runs once on startup when today is missing, every day at DAILY_ASTRO_INGEST_AT, and retries every
DAILY_ASTRO_RETRY_MINUTES while the run failed or the source has not rolled over to the new day yet.
"""
import logging, threading
from datetime import datetime, timedelta
from app.config import APP_TZ, INGEST_AT, LOG_RETENTION_DAYS, RETRY_MINUTES, now, today
from app.database import prune_logs
from app.ingest import ingest_daily, is_complete

log = logging.getLogger("daily_astro.scheduler")
_stop = threading.Event()
_thread: threading.Thread | None = None
status = {"last_attempt": None, "last_result": None, "next_run": None}


def _next_daily_run(after: datetime) -> datetime:
    hour, minute = (int(x) for x in INGEST_AT.split(":"))
    run = after.replace(hour=hour, minute=minute, second=0, microsecond=0)
    return run if run > after else run + timedelta(days=1)


def _attempt(trigger: str):
    status["last_attempt"] = now().isoformat()
    try:
        result = ingest_daily(trigger=trigger)
        status["last_result"] = {k: result[k] for k in ("status", "date", "target_date", "translated")} | {"stored": len(result["stored"])}
    except Exception as exc:
        log.exception("scheduled ingest failed")
        status["last_result"] = {"status": "failed", "error": str(exc)[:300]}


def _loop():
    next_run = _next_daily_run(now())
    last_try = None
    last_prune = None
    if not is_complete(today()):
        _attempt("startup"); last_try = now()
    while not _stop.wait(60):
        current = now()
        status["next_run"] = next_run.isoformat()
        if current >= next_run:
            _attempt("schedule"); last_try = current
            next_run = _next_daily_run(current)
        elif not is_complete(today()) and (last_try is None or current - last_try >= timedelta(minutes=RETRY_MINUTES)):
            _attempt("retry"); last_try = current
        if last_prune != current.date():
            prune_logs(LOG_RETENTION_DAYS); last_prune = current.date()


def start():
    global _thread
    if _thread and _thread.is_alive():
        return
    _stop.clear()
    _thread = threading.Thread(target=_loop, name="daily-astro-scheduler", daemon=True)
    _thread.start()
    log.info("scheduler started; daily ingest at %s %s", INGEST_AT, APP_TZ)


def stop():
    _stop.set()
