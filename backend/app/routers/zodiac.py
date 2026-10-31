from fastapi import APIRouter, Query
from app.data import SIGNS, localized_sign
from app.schemas import ZodiacSign

router = APIRouter()

@router.get("/zodiac-signs", response_model=list[ZodiacSign])
def list_signs(language: str = Query("en", min_length=2, max_length=5)):
    return [localized_sign(item, language) for item in SIGNS]
