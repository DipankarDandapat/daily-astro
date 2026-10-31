from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import languages, zodiac, horoscope, users, admin
from app.startup import initialize

app = FastAPI(title="Daily Astro API", version="1.0.0", docs_url="/docs")
initialize()
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.include_router(users.router, prefix="/api/v1/users", tags=["users"])
app.include_router(languages.router, prefix="/api/v1", tags=["languages"])
app.include_router(zodiac.router, prefix="/api/v1", tags=["zodiac"])
app.include_router(horoscope.router, prefix="/api/v1", tags=["horoscope"])
app.include_router(admin.router, prefix="/api/v1/admin", tags=["admin"])

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "daily-astro"}
