import base64, hashlib, hmac, json, os, secrets, time
from fastapi import Header, HTTPException
from app.database import find_user

SECRET = os.getenv("DAILY_ASTRO_AUTH_SECRET", "change-this-secret-in-production")

def hash_password(password: str) -> str:
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 120_000)
    return base64.urlsafe_b64encode(salt + digest).decode()

def verify_password(password: str, encoded: str | None) -> bool:
    if not encoded: return False
    raw = base64.urlsafe_b64decode(encoded.encode())
    expected = hashlib.pbkdf2_hmac("sha256", password.encode(), raw[:16], 120_000)
    return hmac.compare_digest(expected, raw[16:])

def create_token(user_id: str) -> str:
    payload = {"sub": user_id, "exp": int(time.time()) + 60 * 60 * 24 * 30}
    body = base64.urlsafe_b64encode(json.dumps(payload, separators=(",", ":")).encode()).decode().rstrip("=")
    sig = hmac.new(SECRET.encode(), body.encode(), hashlib.sha256).hexdigest()
    return f"{body}.{sig}"

def current_user(authorization: str | None = Header(default=None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Bearer token required")
    try:
        body, signature = authorization[7:].split(".", 1)
        expected = hmac.new(SECRET.encode(), body.encode(), hashlib.sha256).hexdigest()
        if not hmac.compare_digest(signature, expected): raise ValueError
        payload = json.loads(base64.urlsafe_b64decode(body + "=="))
        if payload["exp"] < time.time(): raise ValueError
        user = find_user(payload["sub"])
        if not user: raise ValueError
        return user
    except (ValueError, KeyError, json.JSONDecodeError):
        raise HTTPException(401, "Invalid or expired token")
