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

class Category(BaseModel):
    key: Literal["love", "career", "health", "spiritual", "lucky"]
    title: str
    summary: str

class HoroscopeResponse(BaseModel):
    date: str
    sign: ZodiacSign
    language: str
    summary: str
    special_advice: str
    categories: list[Category]
    lucky_color: str
    lucky_number: int
    source: str | None = None
