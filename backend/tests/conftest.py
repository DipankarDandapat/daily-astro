import os, tempfile

# Must run before app modules are imported: isolate the database and keep the scheduler off.
_tmp = tempfile.mkdtemp(prefix="daily-astro-test-")
os.environ["DAILY_ASTRO_DB"] = os.path.join(_tmp, "test.db")
os.environ["DAILY_ASTRO_LOG_DIR"] = os.path.join(_tmp, "logs")
os.environ["DAILY_ASTRO_SCHEDULER"] = "0"
os.environ["DAILY_ASTRO_ADMIN_KEY"] = "test-admin"

import pytest
from app import ingest, sources, translate

SIGNS = ["Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"]


def fake_positions(datetime_iso, zodiac="tropical"):
    def body(sign, retro=False, kind="planet"):
        return {"sign": sign, "degrees_in_sign": 10.0, "retrograde": retro, "type": kind}
    return {"datetime": datetime_iso, "bodies": {
        "sun": body("Scorpio", kind="luminary"), "moon": body("Leo", kind="luminary"), "mercury": body("Scorpio", True),
        "venus": body("Libra", True), "mars": body("Leo"), "jupiter": body("Leo"), "saturn": body("Aries", True),
        "neptune": body("Aries", True), "rahu": body("Aquarius", True, "lunar_node"), "ketu": body("Leo", True, "lunar_node")}}


def fake_times(lat, lng, day):
    return {"date": day, "sun": {"events": {"sunrise": f"{day}T06:30:00+05:30", "sunset": f"{day}T17:30:00+05:30"}},
            "moon": {"phase_description": "Waning Gibbous", "details": {"illumination_pct": 67.4}, "moonrise": f"{day}T22:28:00+05:30"},
            "rahukaalam": {"start": f"{day}T09:15:00+05:30", "end": f"{day}T10:37:00+05:30"}}


def fake_panchang(lat, lng, day):
    return {"tithi": {"at_start": {"name": "Shashthi", "paksha": "Krishna"}}, "nakshatra": {"at_start": {"name": "Ardra"}}, "yoga": {"at_start": {"name": "Siddha"}}}


@pytest.fixture(autouse=True)
def offline_sources(monkeypatch):
    monkeypatch.setattr(sources, "planetary_positions", fake_positions)
    monkeypatch.setattr(sources, "sun_moon_times", fake_times)
    monkeypatch.setattr(sources, "panchang", fake_panchang)
    monkeypatch.setattr(sources, "horoscope_text", lambda sign, period="daily": {"date": ingest.today().isoformat(), "period": period, "sign": sign.title(), "horoscope": f"{sign} {period} reading"})
    monkeypatch.setattr(translate, "PROVIDERS", {"gtx": lambda texts, language: [f"[{language}] {t}" for t in texts]})
