from datetime import date, timedelta
from fastapi.testclient import TestClient
from app.main import app
from app import astro, ingest
from app.config import today
from app.database import connection, save_horoscope, get_horoscope, recent

client = TestClient(app)
ADMIN = {"X-Admin-Key": "test-admin"}

def test_health():
    assert client.get('/health').json()['status'] == 'ok'

def test_catalogs():
    assert len(client.get('/api/v1/languages').json()) == 10
    assert len(client.get('/api/v1/zodiac-signs').json()) == 12
    hindi = client.get('/api/v1/zodiac-signs?language=hi').json()
    tamil = client.get('/api/v1/zodiac-signs?language=ta').json()
    assert hindi[0]['id'] == 'aries' and hindi[0]['name'] == 'मेष'
    assert tamil[0]['id'] == 'aries' and tamil[0]['name'] == 'மேஷம்'

def test_register_login_and_horoscope():
    response = client.post('/api/v1/users/register', json={'device_id':'emulator-123','name':'Asha','password':'secret123'})
    assert response.status_code == 200
    login = client.post('/api/v1/users/login', json={'identifier':'emulator-123','password':'secret123'})
    assert login.status_code == 200
    assert login.json()['access_token']
    result = ingest.ingest_daily(languages=['hi'])
    assert len(result['stored']) == 12
    assert result['source'] == 'freehoroscopeapi'
    assert result['status'] == 'ok' and result['translated'] == 12
    horoscope = client.get('/api/v1/horoscope/today?sign=aries&language=en')
    assert horoscope.status_code == 200
    body = horoscope.json()
    assert body['categories'][0]['key'] == 'love'
    assert body['summary'] == 'aries daily reading'
    assert set(body['energy']) == {'love', 'career', 'health', 'spiritual'}
    assert body['sky']['moon_sign'] == 'Leo' and body['sky']['tithi'] == 'Krishna Shashthi'
    assert body['attribution'] == 'Powered by FreeHoroscopeAPI'

def test_translated_reading_is_stored_per_language():
    ingest.ingest_daily(languages=['hi'])
    hindi = client.get('/api/v1/horoscope?sign=leo&language=hi').json()
    assert hindi['summary'] == '[hi] leo daily reading'
    assert hindi['sign']['name'] == 'सिंह'
    assert hindi['translated'] is True
    # Tamil was not part of the run: it is translated on first request and then stored.
    tamil = client.get('/api/v1/horoscope?sign=leo&language=ta').json()
    assert tamil['summary'] == '[ta] leo daily reading'
    assert get_horoscope(tamil['date'], 'leo', 'ta')['translation_provider'] == 'gtx'

def test_live_fetch_when_daily_job_has_not_run():
    with connection() as db:
        db.execute("DELETE FROM daily_horoscopes WHERE sign_id='virgo'")
    response = client.get('/api/v1/horoscope?sign=virgo&language=en&day=today')
    assert response.status_code == 200
    assert response.json()['summary'] == 'virgo daily reading'

def test_yesterday_and_tomorrow():
    yesterday = (today() - timedelta(days=1)).isoformat()
    row, insights = ingest.build_row(date.fromisoformat(yesterday), 'pisces', 'pisces from yesterday', {})
    save_horoscope(row, insights, 'source')
    y = client.get('/api/v1/horoscope?sign=pisces&day=yesterday').json()
    assert y['date'] == yesterday and y['summary'] == 'pisces from yesterday'
    t = client.get('/api/v1/horoscope?sign=pisces&day=tomorrow').json()
    assert t['kind'] == 'forecast' and t['date'] == (today() + timedelta(days=1)).isoformat()
    assert 'Tomorrow the Moon moves through Leo' in t['summary']
    assert client.get('/api/v1/horoscope?sign=pisces&day=yesterday&language=xx').status_code == 400

def test_astro_scoring_is_house_based():
    positions = {"bodies": {"venus": {"sign": "Leo", "retrograde": False}, "moon": {"sign": "Leo"}, "mars": {"sign": "Aries"},
                            "sun": {"sign": "Aries"}, "saturn": {"sign": "Libra"}, "jupiter": {"sign": "Cancer"}, "mercury": {"sign": "Virgo"}, "ketu": {"sign": "Leo"}}}
    # Venus in Leo is the 5th house (romance) for Aries but the 12th for Virgo.
    assert astro.house("Leo", "aries") == 5 and astro.house("Leo", "virgo") == 12
    assert astro.scores(positions["bodies"], "aries")["love"] > astro.scores(positions["bodies"], "virgo")["love"]
    assert astro.dignity("jupiter", "Cancer") == 2 and astro.dignity("saturn", "Aries") == -2

def test_lucky_hour_avoids_rahu_kaal():
    from tests.conftest import fake_positions, fake_times
    day = date(2026, 10, 31)  # Saturday: first hora is Saturn's
    luck = astro.lucky(fake_positions("x")["bodies"], "leo", day, fake_times(0, 0, day.isoformat()))
    assert luck["planet"] in astro.PLANET_COLOR and luck["time"]
    assert not (luck["time_start"] < f"{day}T10:37:00+05:30" and luck["time_end"] > f"{day}T09:15:00+05:30")

def test_device_profile_stats_saved_and_delete():
    device = {"X-Device-Id": "pixel-7"}
    profile = client.post('/api/v1/users/device', json={'device_id': 'pixel-7', 'name': 'Ravi', 'language': 'hi', 'sign': 'leo', 'theme': 'dark', 'notif_enabled': True, 'notif_time': '07:30'})
    assert profile.status_code == 200 and profile.json()['sign'] == 'leo' and profile.json()['notif_enabled'] is True
    updated = client.post('/api/v1/users/device', json={'device_id': 'pixel-7', 'dob': '1995-08-10'}).json()
    assert updated['name'] == 'Ravi' and updated['dob'] == '1995-08-10' and updated['theme'] == 'dark'
    reading = client.get('/api/v1/horoscope?sign=leo&language=en', headers=device).json()
    stats = client.get('/api/v1/users/pixel-7/stats').json()
    assert stats == {'streak': 1, 'read': 1, 'saved': 0}
    assert client.post('/api/v1/users/pixel-7/saved', json={'date': reading['date'], 'sign': 'leo'}).json()['saved'] == 1
    assert client.get('/api/v1/horoscope?sign=leo', headers=device).json()['saved'] is True
    assert client.get('/api/v1/users/pixel-7/saved').json()[0]['summary'] == 'leo daily reading'
    assert client.delete(f"/api/v1/users/pixel-7/saved/{reading['date']}/leo").json()['saved'] == 0
    assert client.post('/api/v1/feedback', json={'device_id': 'pixel-7', 'rating': 5, 'message': 'Lovely'}).json()['status'] == 'received'
    assert client.delete('/api/v1/users/pixel-7').json()['deleted'] is True
    assert client.get('/api/v1/users/pixel-7/profile').status_code == 404

def test_compatibility_and_sky():
    match = client.get('/api/v1/compatibility?a=aries&b=leo').json()
    assert match['score'] >= 90 and 'trine' in match['summary']
    assert client.get('/api/v1/sky').json()['moon_phase'] == 'Waning Gibbous'

def test_period_reading():
    weekly = client.get('/api/v1/horoscope/period?sign=leo&period=weekly&language=hi').json()
    assert weekly['summary'] == '[hi] leo weekly reading'

def test_history_lists_stored_days():
    ingest.ingest_daily(languages=[])
    items = client.get('/api/v1/horoscope/history?sign=aries&days=7').json()
    assert items and items[0]['date'] == today().isoformat()

def test_logs_and_admin():
    assert client.post('/api/v1/admin/ingest/daily?languages=').status_code == 401
    run = client.post('/api/v1/admin/ingest/daily?languages=', headers=ADMIN)
    assert run.status_code == 200 and run.json()['status'] == 'ok'
    runs = client.get('/api/v1/admin/ingest/runs', headers=ADMIN).json()['runs']
    assert runs[0]['trigger'] == 'admin' and runs[0]['stored'] == 12
    client.get('/api/v1/zodiac-signs', headers={'X-Device-Id': 'logger'})
    assert any(r['device_id'] == 'logger' for r in recent('api_request_logs'))
