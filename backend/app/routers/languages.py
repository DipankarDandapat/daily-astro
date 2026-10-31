from fastapi import APIRouter
from app.data import LANGUAGES
from app.schemas import Language

router = APIRouter()

@router.get("/languages", response_model=list[Language])
def list_languages():
    return LANGUAGES
