from fastapi.testclient import TestClient
from app.main import app
from app import ingest

client = TestClient(app)

def test_health():
    assert client.get('/health').json()['status'] == 'ok'

def test_catalogs():
    assert len(client.get('/api/v1/languages').json()) == 10
    assert len(client.get('/api/v1/zodiac-signs').json()) == 12
    hindi = client.get('/api/v1/zodiac-signs?language=hi').json()
    tamil = client.get('/api/v1/zodiac-signs?language=ta').json()
    assert hindi[0]['id'] == 'aries' and hindi[0]['name'] == 'मेष'
    assert tamil[0]['id'] == 'aries' and tamil[0]['name'] == 'மேஷம்'

def test_register_login_and_horoscope(monkeypatch):
    response = client.post('/api/v1/users/register', json={'device_id':'emulator-123','name':'Asha','password':'secret123'})
    assert response.status_code == 200
    login = client.post('/api/v1/users/login', json={'identifier':'emulator-123','password':'secret123'})
    assert login.status_code == 200
    assert login.json()['access_token']
    monkeypatch.setattr(ingest, 'fetch_sign', lambda sign: {'description': f'{sign} reading', 'mood':'Focused', 'color':'Red', 'lucky_number':'7'})
    result = ingest.ingest_daily()
    assert len(result['stored']) == 12
    assert result['source'] == 'freehoroscopeapi'
    horoscope = client.get('/api/v1/horoscope/today?sign=aries&language=en')
    assert horoscope.status_code == 200
    assert horoscope.json()['categories'][0]['key'] == 'love'
