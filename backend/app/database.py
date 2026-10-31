import json, os, sqlite3
from pathlib import Path
from contextlib import contextmanager

DB_PATH = Path(os.getenv("DAILY_ASTRO_DB", Path(__file__).resolve().parents[1] / "daily_astro.db"))

@contextmanager
def connection():
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()

def _add_columns(db, table, columns):
    existing = {row["name"] for row in db.execute(f"PRAGMA table_info({table})")}
    for name, ddl in columns.items():
        if name not in existing:
            db.execute(f"ALTER TABLE {table} ADD COLUMN {name} {ddl}")

def init_db():
    with connection() as db:
        db.execute("PRAGMA journal_mode=WAL")
        db.executescript("""
        CREATE TABLE IF NOT EXISTS users (
          user_id TEXT PRIMARY KEY, device_id TEXT UNIQUE NOT NULL, name TEXT NOT NULL,
          password_hash TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS preferences (
          device_id TEXT PRIMARY KEY, language TEXT, sign TEXT,
          FOREIGN KEY(device_id) REFERENCES users(device_id)
        );
        CREATE TABLE IF NOT EXISTS sessions (
          token TEXT PRIMARY KEY, user_id TEXT NOT NULL, expires_at INTEGER NOT NULL,
          FOREIGN KEY(user_id) REFERENCES users(user_id)
        );
        CREATE TABLE IF NOT EXISTS daily_horoscopes (
          horoscope_date TEXT NOT NULL, sign_id TEXT NOT NULL, language TEXT NOT NULL,
          source TEXT NOT NULL, summary TEXT NOT NULL, special_advice TEXT NOT NULL,
          lucky_color TEXT NOT NULL, lucky_number INTEGER NOT NULL, raw_json TEXT NOT NULL,
          PRIMARY KEY(horoscope_date, sign_id, language)
        );
        CREATE TABLE IF NOT EXISTS period_horoscopes (
          period TEXT NOT NULL, period_key TEXT NOT NULL, sign_id TEXT NOT NULL, language TEXT NOT NULL,
          summary TEXT NOT NULL, raw_json TEXT NOT NULL, fetched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY(period, period_key, sign_id, language)
        );
        CREATE TABLE IF NOT EXISTS sky_snapshots (
          sky_date TEXT NOT NULL, location TEXT NOT NULL, positions_json TEXT NOT NULL,
          times_json TEXT, panchang_json TEXT, fetched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY(sky_date, location)
        );
        CREATE TABLE IF NOT EXISTS translations (
          language TEXT NOT NULL, source_hash TEXT NOT NULL, text TEXT NOT NULL, provider TEXT NOT NULL,
          created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(language, source_hash)
        );
        CREATE TABLE IF NOT EXISTS reading_events (
          id INTEGER PRIMARY KEY AUTOINCREMENT, device_id TEXT NOT NULL, read_date TEXT NOT NULL,
          horoscope_date TEXT NOT NULL, sign_id TEXT NOT NULL, day TEXT NOT NULL, language TEXT NOT NULL,
          created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE INDEX IF NOT EXISTS idx_reading_device ON reading_events(device_id, read_date);
        CREATE TABLE IF NOT EXISTS saved_horoscopes (
          device_id TEXT NOT NULL, horoscope_date TEXT NOT NULL, sign_id TEXT NOT NULL, language TEXT NOT NULL,
          saved_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(device_id, horoscope_date, sign_id)
        );
        CREATE TABLE IF NOT EXISTS feedback (
          id INTEGER PRIMARY KEY AUTOINCREMENT, device_id TEXT, rating INTEGER, message TEXT,
          created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS ingestion_runs (
          id INTEGER PRIMARY KEY AUTOINCREMENT, trigger TEXT NOT NULL, target_date TEXT NOT NULL,
          source_date TEXT, status TEXT NOT NULL, stored INTEGER NOT NULL DEFAULT 0,
          translated INTEGER NOT NULL DEFAULT 0, error TEXT,
          started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, finished_at TEXT
        );
        CREATE TABLE IF NOT EXISTS source_fetch_logs (
          id INTEGER PRIMARY KEY AUTOINCREMENT, source TEXT NOT NULL, url TEXT NOT NULL, status_code INTEGER,
          duration_ms INTEGER NOT NULL, ok INTEGER NOT NULL, error TEXT,
          created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS api_request_logs (
          id INTEGER PRIMARY KEY AUTOINCREMENT, method TEXT NOT NULL, path TEXT NOT NULL, query TEXT,
          status_code INTEGER NOT NULL, duration_ms INTEGER NOT NULL, device_id TEXT, app_version TEXT,
          created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        """)
        _add_columns(db, "daily_horoscopes", {"insights_json": "TEXT", "fetched_at": "TEXT", "translation_provider": "TEXT"})
        _add_columns(db, "preferences", {"dob": "TEXT", "notif_enabled": "INTEGER", "notif_frequency": "TEXT", "notif_time": "TEXT", "theme": "TEXT", "large_text": "INTEGER", "updated_at": "TEXT"})

def upsert_user(user_id, device_id, name, password_hash=None):
    with connection() as db:
        db.execute("INSERT INTO users(user_id,device_id,name,password_hash) VALUES(?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET name=excluded.name, password_hash=COALESCE(excluded.password_hash, users.password_hash)", (user_id, device_id, name, password_hash))
        return dict(db.execute("SELECT * FROM users WHERE device_id=?", (device_id,)).fetchone())

def find_user(identifier):
    with connection() as db:
        row = db.execute("SELECT * FROM users WHERE device_id=? OR user_id=? OR name=? LIMIT 1", (identifier, identifier, identifier)).fetchone()
        return dict(row) if row else None

PREFERENCE_FIELDS = ("language", "sign", "dob", "notif_enabled", "notif_frequency", "notif_time", "theme", "large_text")

def save_preferences(device_id, values):
    values = {k: v for k, v in values.items() if k in PREFERENCE_FIELDS and v is not None}
    with connection() as db:
        db.execute("INSERT INTO preferences(device_id) VALUES(?) ON CONFLICT(device_id) DO NOTHING", (device_id,))
        if values:
            db.execute(f"UPDATE preferences SET {', '.join(f'{k}=?' for k in values)}, updated_at=CURRENT_TIMESTAMP WHERE device_id=?", (*values.values(), device_id))
        return dict(db.execute("SELECT * FROM preferences WHERE device_id=?", (device_id,)).fetchone())

def get_preferences(device_id):
    with connection() as db:
        row = db.execute("SELECT * FROM preferences WHERE device_id=?", (device_id,)).fetchone()
        return dict(row) if row else {}

def delete_device_data(device_id):
    with connection() as db:
        for table in ("reading_events", "saved_horoscopes", "preferences", "feedback"):
            db.execute(f"DELETE FROM {table} WHERE device_id=?", (device_id,))
        user = db.execute("SELECT user_id FROM users WHERE device_id=?", (device_id,)).fetchone()
        if user:
            db.execute("DELETE FROM sessions WHERE user_id=?", (user["user_id"],))
        db.execute("UPDATE api_request_logs SET device_id=NULL WHERE device_id=?", (device_id,))
        return db.execute("DELETE FROM users WHERE device_id=?", (device_id,)).rowcount

def save_horoscope(data, insights=None, provider=None):
    with connection() as db:
        db.execute("""INSERT INTO daily_horoscopes(horoscope_date,sign_id,language,source,summary,special_advice,lucky_color,lucky_number,raw_json,insights_json,fetched_at,translation_provider)
        VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?) ON CONFLICT(horoscope_date,sign_id,language) DO UPDATE SET source=excluded.source,summary=excluded.summary,special_advice=excluded.special_advice,lucky_color=excluded.lucky_color,lucky_number=excluded.lucky_number,raw_json=excluded.raw_json,insights_json=excluded.insights_json,fetched_at=excluded.fetched_at,translation_provider=excluded.translation_provider""",
            (*data, json.dumps(insights, ensure_ascii=False) if insights is not None else None, provider))

def get_horoscope(horoscope_date, sign_id, language):
    with connection() as db:
        row = db.execute("SELECT * FROM daily_horoscopes WHERE horoscope_date=? AND sign_id=? AND language=?", (horoscope_date, sign_id, language)).fetchone()
        return dict(row) if row else None

def latest_horoscope_date(sign_id, language="en", on_or_before=None):
    with connection() as db:
        row = db.execute("SELECT MAX(horoscope_date) AS d FROM daily_horoscopes WHERE sign_id=? AND language=? AND horoscope_date<=?", (sign_id, language, on_or_before or "9999-12-31")).fetchone()
        return row["d"]

def horoscope_dates(sign_id, language, since, until):
    with connection() as db:
        return [r["horoscope_date"] for r in db.execute("SELECT horoscope_date FROM daily_horoscopes WHERE sign_id=? AND language=? AND horoscope_date BETWEEN ? AND ? ORDER BY horoscope_date DESC", (sign_id, language, since, until))]

def count_horoscopes(horoscope_date, language=None):
    with connection() as db:
        if language:
            return db.execute("SELECT COUNT(*) FROM daily_horoscopes WHERE horoscope_date=? AND language=?", (horoscope_date, language)).fetchone()[0]
        return db.execute("SELECT COUNT(*) FROM daily_horoscopes WHERE horoscope_date=?", (horoscope_date,)).fetchone()[0]

def save_period(period, period_key, sign_id, language, summary, raw):
    with connection() as db:
        db.execute("INSERT INTO period_horoscopes(period,period_key,sign_id,language,summary,raw_json) VALUES(?,?,?,?,?,?) ON CONFLICT(period,period_key,sign_id,language) DO UPDATE SET summary=excluded.summary, raw_json=excluded.raw_json, fetched_at=CURRENT_TIMESTAMP",
                   (period, period_key, sign_id, language, summary, json.dumps(raw, ensure_ascii=False)))

def get_period(period, period_key, sign_id, language):
    with connection() as db:
        row = db.execute("SELECT * FROM period_horoscopes WHERE period=? AND period_key=? AND sign_id=? AND language=?", (period, period_key, sign_id, language)).fetchone()
        return dict(row) if row else None

def save_sky(sky_date, location, positions, times, panchang):
    with connection() as db:
        db.execute("INSERT INTO sky_snapshots(sky_date,location,positions_json,times_json,panchang_json) VALUES(?,?,?,?,?) ON CONFLICT(sky_date,location) DO UPDATE SET positions_json=excluded.positions_json, times_json=COALESCE(excluded.times_json,sky_snapshots.times_json), panchang_json=COALESCE(excluded.panchang_json,sky_snapshots.panchang_json), fetched_at=CURRENT_TIMESTAMP",
                   (sky_date, location, json.dumps(positions), json.dumps(times) if times else None, json.dumps(panchang) if panchang else None))

def get_sky(sky_date, location):
    with connection() as db:
        row = db.execute("SELECT * FROM sky_snapshots WHERE sky_date=? AND location=?", (sky_date, location)).fetchone()
        if not row:
            return None
        return {"date": row["sky_date"], "positions": json.loads(row["positions_json"]), "times": json.loads(row["times_json"]) if row["times_json"] else None,
                "panchang": json.loads(row["panchang_json"]) if row["panchang_json"] else None, "fetched_at": row["fetched_at"]}

def get_translation(language, source_hash):
    with connection() as db:
        row = db.execute("SELECT text FROM translations WHERE language=? AND source_hash=?", (language, source_hash)).fetchone()
        return row["text"] if row else None

def save_translation(language, source_hash, text, provider):
    with connection() as db:
        db.execute("INSERT OR REPLACE INTO translations(language,source_hash,text,provider) VALUES(?,?,?,?)", (language, source_hash, text, provider))

def record_read(device_id, read_date, horoscope_date, sign_id, day, language):
    with connection() as db:
        db.execute("INSERT INTO reading_events(device_id,read_date,horoscope_date,sign_id,day,language) VALUES(?,?,?,?,?,?)", (device_id, read_date, horoscope_date, sign_id, day, language))

def read_dates(device_id):
    with connection() as db:
        return [r["read_date"] for r in db.execute("SELECT DISTINCT read_date FROM reading_events WHERE device_id=? ORDER BY read_date DESC", (device_id,))]

def count_reads(device_id):
    with connection() as db:
        return db.execute("SELECT COUNT(*) FROM (SELECT DISTINCT horoscope_date, sign_id, day FROM reading_events WHERE device_id=?)", (device_id,)).fetchone()[0]

def save_bookmark(device_id, horoscope_date, sign_id, language):
    with connection() as db:
        db.execute("INSERT INTO saved_horoscopes(device_id,horoscope_date,sign_id,language) VALUES(?,?,?,?) ON CONFLICT(device_id,horoscope_date,sign_id) DO UPDATE SET language=excluded.language", (device_id, horoscope_date, sign_id, language))

def delete_bookmark(device_id, horoscope_date, sign_id):
    with connection() as db:
        return db.execute("DELETE FROM saved_horoscopes WHERE device_id=? AND horoscope_date=? AND sign_id=?", (device_id, horoscope_date, sign_id)).rowcount

def list_bookmarks(device_id):
    with connection() as db:
        return [dict(r) for r in db.execute("SELECT * FROM saved_horoscopes WHERE device_id=? ORDER BY horoscope_date DESC, saved_at DESC", (device_id,))]

def save_feedback(device_id, rating, message):
    with connection() as db:
        return db.execute("INSERT INTO feedback(device_id,rating,message) VALUES(?,?,?)", (device_id, rating, message)).lastrowid

def start_run(trigger, target_date):
    with connection() as db:
        return db.execute("INSERT INTO ingestion_runs(trigger,target_date,status) VALUES(?,?,'running')", (trigger, target_date)).lastrowid

def finish_run(run_id, status, stored=0, translated=0, source_date=None, error=None):
    with connection() as db:
        db.execute("UPDATE ingestion_runs SET status=?, stored=?, translated=?, source_date=?, error=?, finished_at=CURRENT_TIMESTAMP WHERE id=?", (status, stored, translated, source_date, error, run_id))

def log_fetch(source, url, status_code, duration_ms, ok, error=None):
    with connection() as db:
        db.execute("INSERT INTO source_fetch_logs(source,url,status_code,duration_ms,ok,error) VALUES(?,?,?,?,?,?)", (source, url, status_code, duration_ms, int(ok), error))

def log_request(method, path, query, status_code, duration_ms, device_id, app_version):
    with connection() as db:
        db.execute("INSERT INTO api_request_logs(method,path,query,status_code,duration_ms,device_id,app_version) VALUES(?,?,?,?,?,?,?)", (method, path, query, status_code, duration_ms, device_id, app_version))

def recent(table, limit=50):
    assert table in ("ingestion_runs", "source_fetch_logs", "api_request_logs")
    with connection() as db:
        return [dict(r) for r in db.execute(f"SELECT * FROM {table} ORDER BY id DESC LIMIT ?", (limit,))]

def prune_logs(days):
    with connection() as db:
        for table in ("source_fetch_logs", "api_request_logs"):
            db.execute(f"DELETE FROM {table} WHERE created_at < datetime('now', ?)", (f"-{days} days",))
