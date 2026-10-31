from datetime import date, timedelta
from fastapi import APIRouter, Depends, HTTPException
from app.config import today
from app.data import SIGNS
from app.schemas import (UserRegister, UserResponse, PreferencesUpdate, LoginRequest, AuthResponse, DeviceProfile, ProfileResponse,
                         Stats, BookmarkRequest, Bookmark, FeedbackRequest)
from app.database import (init_db, upsert_user, find_user, save_preferences, get_preferences, delete_device_data, read_dates,
                          count_reads, list_bookmarks, save_bookmark, delete_bookmark, get_horoscope, save_feedback)
from app.auth import hash_password, verify_password, create_token, current_user

router = APIRouter()
SIGN_IDS = {s[0] for s in SIGNS}

@router.post("/register", response_model=UserResponse)
def register(payload: UserRegister):
    init_db()
    existing = find_user(payload.device_id)
    if existing and existing.get("password_hash"):
        raise HTTPException(409, "Device is already registered")
    user = upsert_user(f"user_{payload.device_id[-8:]}", payload.device_id, payload.name, hash_password(payload.password))
    return {"user_id": user["user_id"], "device_id": user["device_id"], "name": user["name"]}

@router.post("/login", response_model=AuthResponse)
def login(payload: LoginRequest):
    user = find_user(payload.identifier)
    if not user or not verify_password(payload.password, user.get("password_hash")):
        raise HTTPException(401, "Invalid identifier or password")
    return {"access_token": create_token(user["user_id"]), "user": {"user_id": user["user_id"], "device_id": user["device_id"], "name": user["name"]}}

@router.get("/me", response_model=UserResponse)
def me(user=Depends(current_user)):
    return {"user_id": user["user_id"], "device_id": user["device_id"], "name": user["name"]}

@router.put("/{device_id}/preferences")
def update_preferences(device_id: str, payload: PreferencesUpdate):
    if not find_user(device_id): raise HTTPException(404, "User not found")
    return {"device_id": device_id, "preferences": save_preferences(device_id, payload.model_dump(exclude_none=True))}

def profile_response(device_id):
    user = find_user(device_id)
    if not user or user["device_id"] != device_id: raise HTTPException(404, "User not found")
    prefs = get_preferences(device_id)
    as_bool = lambda v: None if v is None else bool(v)
    return {"device_id": device_id, "user_id": user["user_id"], "name": user["name"], "language": prefs.get("language"), "sign": prefs.get("sign"),
            "dob": prefs.get("dob"), "notif_enabled": as_bool(prefs.get("notif_enabled")), "notif_frequency": prefs.get("notif_frequency"),
            "notif_time": prefs.get("notif_time"), "theme": prefs.get("theme"), "large_text": as_bool(prefs.get("large_text"))}

@router.post("/device", response_model=ProfileResponse)
def upsert_device(payload: DeviceProfile):
    """Password-less device profile used by the app's onboarding; creates or updates in one call."""
    if payload.sign and payload.sign not in SIGN_IDS: raise HTTPException(422, "Unknown zodiac sign")
    existing = find_user(payload.device_id)
    name = payload.name.strip() or (existing["name"] if existing and existing["device_id"] == payload.device_id else "Stargazer")
    upsert_user(existing["user_id"] if existing and existing["device_id"] == payload.device_id else f"user_{payload.device_id[-8:]}", payload.device_id, name)
    values = payload.model_dump(exclude={"device_id", "name"}, exclude_none=True)
    values = {k: int(v) if isinstance(v, bool) else v for k, v in values.items()}
    save_preferences(payload.device_id, values)
    return profile_response(payload.device_id)

@router.get("/{device_id}/profile", response_model=ProfileResponse)
def get_profile(device_id: str):
    return profile_response(device_id)

@router.get("/{device_id}/stats", response_model=Stats)
def stats(device_id: str):
    dates = {date.fromisoformat(d) for d in read_dates(device_id)}
    streak, day = 0, today()
    if day not in dates: day -= timedelta(days=1)  # a streak survives until the end of today
    while day in dates:
        streak += 1; day -= timedelta(days=1)
    return {"streak": streak, "read": count_reads(device_id), "saved": len(list_bookmarks(device_id))}

@router.get("/{device_id}/saved", response_model=list[Bookmark])
def saved(device_id: str):
    result = []
    for b in list_bookmarks(device_id):
        row = get_horoscope(b["horoscope_date"], b["sign_id"], b["language"]) or get_horoscope(b["horoscope_date"], b["sign_id"], "en")
        result.append({"date": b["horoscope_date"], "sign": b["sign_id"], "language": b["language"], "summary": row["summary"] if row else None, "saved_at": b["saved_at"]})
    return result

@router.post("/{device_id}/saved", response_model=Stats)
def add_saved(device_id: str, payload: BookmarkRequest):
    if not find_user(device_id): raise HTTPException(404, "User not found")
    if payload.sign not in SIGN_IDS: raise HTTPException(422, "Unknown zodiac sign")
    save_bookmark(device_id, payload.date, payload.sign, payload.language)
    return stats(device_id)

@router.delete("/{device_id}/saved/{horoscope_date}/{sign}", response_model=Stats)
def remove_saved(device_id: str, horoscope_date: str, sign: str):
    delete_bookmark(device_id, horoscope_date, sign)
    return stats(device_id)

@router.delete("/{device_id}")
def delete_my_data(device_id: str):
    return {"device_id": device_id, "deleted": bool(delete_device_data(device_id))}

feedback_router = APIRouter()

@feedback_router.post("/feedback")
def feedback(payload: FeedbackRequest):
    if not payload.message.strip() and payload.rating is None: raise HTTPException(422, "Provide a rating or a message")
    return {"id": save_feedback(payload.device_id, payload.rating, payload.message.strip()), "status": "received"}
