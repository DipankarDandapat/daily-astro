from fastapi import APIRouter, Depends, HTTPException
from app.schemas import UserRegister, UserResponse, PreferencesUpdate, LoginRequest, AuthResponse
from app.database import init_db, upsert_user, find_user, save_preferences
from app.auth import hash_password, verify_password, create_token, current_user

router = APIRouter()
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
