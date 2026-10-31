import logging, os
from logging.handlers import RotatingFileHandler
from pathlib import Path
from app.config import SCHEDULER_ENABLED
from app.database import init_db

LOG_DIR = Path(os.getenv("DAILY_ASTRO_LOG_DIR", Path(__file__).resolve().parents[1] / "logs"))

def configure_logging():
    root = logging.getLogger("daily_astro")
    if root.handlers:
        return
    root.setLevel(logging.INFO)
    fmt = logging.Formatter("%(asctime)s %(levelname)s %(name)s: %(message)s")
    console = logging.StreamHandler(); console.setFormatter(fmt); root.addHandler(console)
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    file = RotatingFileHandler(LOG_DIR / "daily-astro.log", maxBytes=2_000_000, backupCount=5, encoding="utf-8")
    file.setFormatter(fmt); root.addHandler(file)

def initialize():
    configure_logging()
    init_db()

def start_background():
    if SCHEDULER_ENABLED:
        from app import scheduler
        scheduler.start()
