from pydantic import BaseModel, Field
from typing import Literal

class UserRegister(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    name: str = Field(min_length=1, max_length=80)
    password: str = Field(min_length=8, max_length=128)

class LoginRequest(BaseModel):
    identifier: str = Field(min_length=1)
    password: str = Field(min_length=8, max_length=128)

class UserResponse(BaseModel):
    user_id: str
    device_id: str
    name: str

class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

class Language(BaseModel):
    code: str
    name: str
    native_name: str

class ZodiacSign(BaseModel):
    id: str
    name: str
    date_range: str
    symbol: str

class PreferencesUpdate(BaseModel):
    language: str | None = None
    sign: str | None = None

class DeviceProfile(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    name: str = Field(default="", max_length=80)
    language: str | None = Field(default=None, max_length=5)
    sign: str | None = None
    dob: str | None = Field(default=None, pattern=r"^\d{4}-\d{2}-\d{2}$")
    notif_enabled: bool | None = None
    notif_frequency: Literal["daily", "twice", "weekly"] | None = None
    notif_time: str | None = Field(default=None, pattern=r"^\d{2}:\d{2}$")
    theme: Literal["auto", "light", "dark"] | None = None
    large_text: bool | None = None

class ProfileResponse(BaseModel):
    device_id: str
    user_id: str
    name: str
    language: str | None = None
    sign: str | None = None
    dob: str | None = None
    notif_enabled: bool | None = None
    notif_frequency: str | None = None
    notif_time: str | None = None
    theme: str | None = None
    large_text: bool | None = None

class Stats(BaseModel):
    streak: int
    read: int
    saved: int

class Detail(BaseModel):
    label: str
    text: str

class Category(BaseModel):
    key: Literal["love", "career", "health", "spiritual", "lucky"]
    title: str
    summary: str
    score: int | None = None
    details: list[Detail] = []

class Lucky(BaseModel):
    color: str
    number: int
    time: str | None = None
    planet: str | None = None

class Sky(BaseModel):
    sun_sign: str
    moon_sign: str
    moon_phase: str | None = None
    moon_illumination: int | None = None
    retrogrades: list[str] = []
    sunrise: str | None = None
    sunset: str | None = None
    moonrise: str | None = None
    moonset: str | None = None
    rahu_kaal_start: str | None = None
    rahu_kaal_end: str | None = None
    tithi: str | None = None
    nakshatra: str | None = None
    yoga: str | None = None
    location: str | None = None

class HoroscopeResponse(BaseModel):
    date: str
    day: Literal["yesterday", "today", "tomorrow"] = "today"
    kind: Literal["daily", "forecast"] = "daily"
    sign: ZodiacSign
    language: str
    summary: str
    special_advice: str
    categories: list[Category]
    lucky_color: str
    lucky_number: int
    lucky: Lucky | None = None
    energy: dict[str, int] = {}
    affirmation: str | None = None
    sky: Sky | None = None
    translated: bool = False
    source: str | None = None
    attribution: str | None = None
    attribution_url: str | None = None
    fetched_at: str | None = None
    saved: bool = False

class HistoryItem(BaseModel):
    date: str
    sign: str
    summary: str

class PeriodResponse(BaseModel):
    period: Literal["weekly", "monthly"]
    period_key: str
    sign: ZodiacSign
    language: str
    summary: str

class BookmarkRequest(BaseModel):
    date: str = Field(pattern=r"^\d{4}-\d{2}-\d{2}$")
    sign: str
    language: str = "en"

class Bookmark(BaseModel):
    date: str
    sign: str
    language: str
    summary: str | None = None
    saved_at: str

class CompatibilityResponse(BaseModel):
    a: ZodiacSign
    b: ZodiacSign
    score: int
    summary: str
    elements: list[str]
    language: str

class FeedbackRequest(BaseModel):
    device_id: str | None = Field(default=None, max_length=128)
    rating: int | None = Field(default=None, ge=1, le=5)
    message: str = Field(default="", max_length=2000)
