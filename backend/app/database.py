import os, sqlite3
from pathlib import Path
from contextlib import contextmanager

DB_PATH = Path(os.getenv("DAILY_ASTRO_DB", Path(__file__).resolve().parents[1] / "daily_astro.db"))

@contextmanager
def connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()

def init_db():
    with connection() as db:
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
        """)

def upsert_user(user_id, device_id, name, password_hash=None):
    with connection() as db:
        db.execute("INSERT INTO users(user_id,device_id,name,password_hash) VALUES(?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET name=excluded.name, password_hash=COALESCE(excluded.password_hash, users.password_hash)", (user_id, device_id, name, password_hash))
        return dict(db.execute("SELECT * FROM users WHERE device_id=?", (device_id,)).fetchone())

def find_user(identifier):
    with connection() as db:
        row = db.execute("SELECT * FROM users WHERE device_id=? OR user_id=? OR name=? LIMIT 1", (identifier, identifier, identifier)).fetchone()
        return dict(row) if row else None

def save_preferences(device_id, values):
    with connection() as db:
        db.execute("INSERT INTO preferences(device_id,language,sign) VALUES(?,?,?) ON CONFLICT(device_id) DO UPDATE SET language=COALESCE(excluded.language,preferences.language), sign=COALESCE(excluded.sign,preferences.sign)", (device_id, values.get("language"), values.get("sign")))
        row = db.execute("SELECT * FROM preferences WHERE device_id=?", (device_id,)).fetchone()
        return dict(row)

def save_horoscope(data):
    with connection() as db:
        db.execute("""INSERT INTO daily_horoscopes(horoscope_date,sign_id,language,source,summary,special_advice,lucky_color,lucky_number,raw_json)
        VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(horoscope_date,sign_id,language) DO UPDATE SET source=excluded.source,summary=excluded.summary,special_advice=excluded.special_advice,lucky_color=excluded.lucky_color,lucky_number=excluded.lucky_number,raw_json=excluded.raw_json""", data)

def get_horoscope(horoscope_date, sign_id, language):
    with connection() as db:
        row = db.execute("SELECT * FROM daily_horoscopes WHERE horoscope_date=? AND sign_id=? AND language=?", (horoscope_date, sign_id, language)).fetchone()
        return dict(row) if row else None
