"""Machine translation of English readings into the app's languages, cached per string in the translations table."""
import hashlib, logging
from app.config import TRANSLATION_PROVIDERS
from app.database import get_translation, save_translation
from app.sources import SourceError, get_json

log = logging.getLogger("daily_astro.translate")
CHUNK_CHARS = 1800


def _key(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()


def _gtx(texts: list[str], language: str) -> list[str]:
    # Newline-joined batches keep one request per chunk; the endpoint preserves line breaks.
    out, chunk = [], []
    def flush():
        if not chunk: return
        body = get_json("https://translate.googleapis.com/translate_a/single", {"client": "gtx", "sl": "en", "tl": language, "dt": "t", "q": "\n".join(chunk)}, source="gtx", retries=1)
        lines = "".join(seg[0] for seg in body[0] if seg and seg[0]).split("\n")
        if len(lines) != len(chunk):
            raise SourceError(f"gtx returned {len(lines)} lines for {len(chunk)} inputs")
        out.extend(line.strip() for line in lines)
        chunk.clear()
    for text in texts:
        if chunk and sum(len(t) + 1 for t in chunk) + len(text) > CHUNK_CHARS:
            flush()
        chunk.append(text.replace("\n", " "))
    flush()
    return out


def _mymemory(texts: list[str], language: str) -> list[str]:
    out = []
    for text in texts:
        body = get_json("https://api.mymemory.translated.net/get", {"q": text[:500], "langpair": f"en|{language}"}, source="mymemory", retries=1)
        if body.get("responseStatus") != 200 or body.get("quotaFinished"):
            raise SourceError(f"mymemory: {body.get('responseDetails') or 'quota finished'}")
        out.append(body["responseData"]["translatedText"])
    return out


PROVIDERS = {"gtx": _gtx, "mymemory": _mymemory}


def translate_many(texts: list[str], language: str) -> tuple[list[str], str]:
    """Returns (translations, provider). provider is 'none' when nothing could be translated (English is returned)."""
    if language == "en" or not texts:
        return list(texts), "source"
    result: list[str | None] = [get_translation(language, _key(t)) if t.strip() else t for t in texts]
    missing = [i for i, value in enumerate(result) if value is None]
    if not missing:
        return result, "cache"
    for name in TRANSLATION_PROVIDERS:
        provider = PROVIDERS.get(name)
        if not provider: continue
        try:
            translated = provider([texts[i] for i in missing], language)
        except Exception as exc:
            log.warning("translation via %s to %s failed: %s", name, language, exc)
            continue
        for i, value in zip(missing, translated):
            result[i] = value
            save_translation(language, _key(texts[i]), value, name)
        return result, name
    return list(texts), "none"
