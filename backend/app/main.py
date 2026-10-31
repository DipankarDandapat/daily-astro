import time
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from app.routers import languages, zodiac, horoscope, users, admin, sky
from app.database import log_request
from app.startup import initialize, start_background

initialize()

@asynccontextmanager
async def lifespan(_app):
    start_background()
    yield

app = FastAPI(title="Daily Astro API", version="2.0.0", docs_url="/docs", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

@app.middleware("http")
async def request_log(request: Request, call_next):
    started = time.monotonic()
    status = 500
    try:
        response = await call_next(request)
        status = response.status_code
        return response
    finally:
        if request.url.path.startswith("/api/"):
            log_request(request.method, request.url.path, request.url.query or None, status, int((time.monotonic() - started) * 1000),
                        request.headers.get("x-device-id"), request.headers.get("x-app-version"))

app.include_router(users.router, prefix="/api/v1/users", tags=["users"])
app.include_router(users.feedback_router, prefix="/api/v1", tags=["feedback"])
app.include_router(languages.router, prefix="/api/v1", tags=["languages"])
app.include_router(zodiac.router, prefix="/api/v1", tags=["zodiac"])
app.include_router(horoscope.router, prefix="/api/v1", tags=["horoscope"])
app.include_router(sky.router, prefix="/api/v1", tags=["sky"])
app.include_router(admin.router, prefix="/api/v1/admin", tags=["admin"])

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "daily-astro"}
