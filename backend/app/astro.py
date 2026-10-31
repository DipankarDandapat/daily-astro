"""Turns live planetary positions into per-sign insights.

Signs are tropical (the app's date ranges are tropical), so positions must be fetched with zodiac=tropical.
Houses are whole-sign houses counted from the reader's sun sign. Everything here is pure and deterministic:
the same sky always produces the same scores and text.
"""
from datetime import date, datetime, timedelta

SIGN_IDS = ["aries", "taurus", "gemini", "cancer", "leo", "virgo", "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces"]
ELEMENTS = ["fire", "earth", "air", "water"] * 3
MODALITIES = ["cardinal", "fixed", "mutable"] * 4
RULERS = ["mars", "venus", "mercury", "moon", "sun", "mercury", "venus", "mars", "jupiter", "saturn", "saturn", "jupiter"]
EXALTED = {"sun": "aries", "moon": "taurus", "mercury": "virgo", "venus": "pisces", "mars": "capricorn", "jupiter": "cancer", "saturn": "libra"}
HOUSE_MEANING = {1: "self and first impressions", 2: "money and values", 3: "communication and short trips", 4: "home and family",
                 5: "romance, creativity and fun", 6: "work routines and health", 7: "partnerships", 8: "intimacy and shared resources",
                 9: "travel, learning and beliefs", 10: "career and reputation", 11: "friends and long-term goals", 12: "rest and the inner world"}
HOUSE_TONE = {1: 1, 2: .5, 3: .5, 4: .5, 5: 1, 6: -1, 7: .5, 8: -1, 9: 1, 10: 1, 11: 1, 12: -1}
CATEGORY_RULES = {
    "love": {"planets": {"venus": 1.0, "moon": .6, "mars": .4}, "houses": {5: 1.5, 7: 1.5, 1: .5, 11: .5}},
    "career": {"planets": {"saturn": .8, "jupiter": .7, "sun": .6, "mercury": .6}, "houses": {10: 1.5, 2: 1, 11: 1, 6: .5}},
    "health": {"planets": {"sun": .8, "mars": .7, "moon": .6}, "houses": {1: 1, 6: .5, 8: -.5, 12: -.5}},
    "spiritual": {"planets": {"jupiter": .9, "moon": .6, "ketu": .6, "neptune": .4}, "houses": {9: 1.5, 12: 2.5, 4: .5, 8: 1}},
}
PLANET_COLOR = {"sun": "Gold", "moon": "Silver", "mars": "Red", "mercury": "Green", "jupiter": "Yellow", "venus": "Pink", "saturn": "Blue"}
PLANET_NUMBER = {"sun": 1, "moon": 2, "jupiter": 3, "mercury": 5, "venus": 6, "saturn": 8, "mars": 9}
WEEKDAY_LORD = ["moon", "mars", "mercury", "jupiter", "venus", "saturn", "sun"]  # date.weekday(): Monday=0
CHALDEAN = ["saturn", "jupiter", "mars", "sun", "venus", "mercury", "moon"]
RETRO_NOTE = {
    "mercury": "Mercury is retrograde, so re-read messages and double-check plans before you commit.",
    "venus": "Venus is retrograde: old feelings or past connections may resurface — revisit before you rush.",
    "mars": "Mars is retrograde, so pace yourself and finish what you started rather than forcing new starts.",
    "jupiter": "Jupiter is retrograde, which favours reviewing goals over expanding them.",
    "saturn": "Saturn is retrograde: a good time to fix foundations and settle old responsibilities.",
}
LINES = {
    ("love", "singles"): {"good": "Charm comes naturally today; say yes to invitations and let people see the real you.",
                          "mixed": "A steady day for self-love — know what you want before you look for it in someone else.",
                          "hard": "Old patterns may tug at you; slow down and don't chase anyone who isn't meeting you halfway."},
    ("love", "partnered"): {"good": "Closeness grows easily; plan time together and say the kind thing out loud.",
                            "mixed": "Small gestures count more than big declarations — check in and really listen.",
                            "hard": "Moods run high; pause before reacting and choose curiosity over criticism."},
    ("career", "career"): {"good": "Momentum is on your side — take the initiative and make your work visible.",
                           "mixed": "Progress comes through steady effort; tidy loose ends before starting something new.",
                           "hard": "Expect delays or extra scrutiny; keep your head down and document your work."},
    ("career", "finance"): {"good": "A favourable day to review income, ask for what you're worth, or plan a sensible purchase.",
                            "mixed": "Keep spending in line with your plan; small savings add up.",
                            "hard": "Avoid impulse buys and risky bets today; read the fine print."},
    ("health", "physical"): {"good": "Energy is strong — a great day to move your body and try a new routine.",
                             "mixed": "Balance activity with rest; stay hydrated and eat regular meals.",
                             "hard": "Your reserves may run low; prioritise sleep and avoid overdoing it."},
    ("health", "emotional"): {"good": "You feel centred; share that calm with the people around you.",
                              "mixed": "Feelings may come in waves — journaling or a short walk helps you process.",
                              "hard": "Be gentle with yourself; protect your energy and say no where you need to."},
    ("spiritual", "spiritual"): {"good": "Intuition is sharp; meditation, prayer or quiet reflection will feel especially rewarding.",
                                 "mixed": "Carve out a few quiet minutes to journal and notice what keeps coming up.",
                                 "hard": "Your mind may feel noisy; simple rituals — breathing, a walk, gratitude — bring you back."},
}
MOON_ADVICE = {1: "put yourself first and start something that is yours.", 2: "review your money and what you truly value.",
               3: "it's a good day for calls, writing and quick errands.", 4: "slow down and invest time in home and family.",
               5: "make room for play, romance and creative work.", 6: "tidy up routines, health habits and to-do lists.",
               7: "focus on one-to-one relationships and meet people halfway.", 8: "go deep — honest talks about trust and shared money.",
               9: "learn something new or plan a trip.", 10: "step up at work; your effort is noticed.",
               11: "connect with friends and work toward a shared goal.", 12: "rest, reflect and recharge before the next push."}
AFFIRMATIONS = [
    "My intuition is my greatest guide, and I follow it.", "I welcome today's opportunities with an open heart.",
    "I am calm, focused and ready for what comes.", "I choose progress over perfection.",
    "I attract people who support my growth.", "My words are kind, clear and honest.",
    "I trust the timing of my life.", "I release what I cannot control.",
    "I am worthy of the love I give.", "Every small step moves me forward.",
    "I listen to my body and give it what it needs.", "I turn challenges into lessons.",
    "I am grateful for what I have and open to more.", "My energy is precious, and I spend it wisely.",
    "I speak up for what matters to me.", "I am becoming the best version of myself.",
]


def sign_index(sign_id: str) -> int:
    return SIGN_IDS.index(sign_id)


def house(planet_sign: str, reader_sign: str) -> int:
    return (sign_index(planet_sign.lower()) - sign_index(reader_sign)) % 12 + 1


def dignity(planet: str, sign: str) -> float:
    sign = sign.lower()
    if EXALTED.get(planet) == sign: return 2
    if EXALTED.get(planet) == SIGN_IDS[(sign_index(sign) + 6) % 12]: return -2
    if planet in RULERS and RULERS[sign_index(sign)] == planet: return 1.5
    if planet in RULERS and RULERS[(sign_index(sign) + 6) % 12] == planet: return -1.5
    return 0


def ordinal(n: int) -> str:
    return f"{n}{'th' if 10 <= n % 100 <= 20 else {1: 'st', 2: 'nd', 3: 'rd'}.get(n % 10, 'th')}"


def planet_contribution(category: str, planet: str, body: dict, reader_sign: str) -> float:
    rules = CATEGORY_RULES[category]
    h = house(body["sign"], reader_sign)
    value = HOUSE_TONE[h] + rules["houses"].get(h, 0) + .5 * dignity(planet, body["sign"])
    if body.get("retrograde") and planet not in ("ketu", "rahu"):
        # Retrogrades turn energy inward: a drag on outward matters, a help for reflection.
        value += .3 if category == "spiritual" else -1.2
    return rules["planets"][planet] * value


def scores(bodies: dict, reader_sign: str) -> dict[str, int]:
    result = {}
    for category, rules in CATEGORY_RULES.items():
        total = sum(planet_contribution(category, p, bodies[p], reader_sign) for p in rules["planets"] if p in bodies)
        result[category] = max(22, min(96, round(62 + 8 * total)))
    return result


def tone(value: float) -> str:
    return "good" if value > .6 else "hard" if value < -.4 else "mixed"


def describe(category: str, part: str, planet: str, bodies: dict, reader_sign: str) -> str:
    body = bodies[planet]
    h = house(body["sign"], reader_sign)
    text = f"{planet.title()} in {body['sign']} lights your {ordinal(h)} house of {HOUSE_MEANING[h]}. {LINES[(category, part)][tone(planet_contribution(category, planet, body, reader_sign))]}"
    if body.get("retrograde") and planet in RETRO_NOTE:
        text += " " + RETRO_NOTE[planet]
    return text


def categories(bodies: dict, reader_sign: str, energy: dict, lucky: dict, affirmation: str) -> list[dict]:
    def strongest(planets):
        return max(planets, key=lambda p: abs(HOUSE_TONE[house(bodies[p]["sign"], reader_sign)]) + abs(dignity(p, bodies[p]["sign"])))
    love = [{"label": "For Singles", "text": describe("love", "singles", "venus", bodies, reader_sign)},
            {"label": "In a Relationship", "text": describe("love", "partnered", "moon", bodies, reader_sign)}]
    career = [{"label": "Career", "text": describe("career", "career", strongest(["saturn", "sun", "mercury"]), bodies, reader_sign)},
              {"label": "Finance", "text": describe("career", "finance", "jupiter", bodies, reader_sign)}]
    health = [{"label": "Physical", "text": describe("health", "physical", "mars", bodies, reader_sign)},
              {"label": "Emotional", "text": describe("health", "emotional", "moon", bodies, reader_sign)}]
    spiritual = [{"label": "", "text": describe("spiritual", "spiritual", "jupiter", bodies, reader_sign)}]
    lucky_parts = [{"label": "Lucky Color", "text": lucky["color"]}, {"label": "Lucky Number", "text": str(lucky["number"])}]
    if lucky.get("time"):
        lucky_parts.append({"label": "Lucky Time", "text": lucky["time"]})
    lucky_parts.append({"label": "Lucky Affirmation", "text": affirmation})
    rows = [("love", "Love & Relationship", love), ("career", "Career & Finance", career), ("health", "Health & Wellness", health),
            ("spiritual", "Spiritual Growth", spiritual), ("lucky", "Lucky Insights", lucky_parts)]
    return [{"key": key, "title": title, "score": energy.get(key), "details": parts,
             "summary": " ".join(p["text"] for p in parts) if key != "lucky" else f"Lucky color: {lucky['color']} · Lucky number: {lucky['number']}"}
            for key, title, parts in rows]


def parse_time(value: str | None) -> datetime | None:
    if not value: return None
    try: return datetime.fromisoformat(value)
    except ValueError: return None


def lucky(bodies: dict, reader_sign: str, day: date, times: dict | None) -> dict:
    candidates = ["jupiter", "venus", "moon", "mercury", "sun"]
    planet = max(candidates, key=lambda p: (HOUSE_TONE[house(bodies[p]["sign"], reader_sign)] + .5 * dignity(p, bodies[p]["sign"]) - (.8 if bodies[p].get("retrograde") else 0), -candidates.index(p)))
    result = {"planet": planet, "color": PLANET_COLOR[planet], "number": PLANET_NUMBER[planet], "time": None, "time_start": None, "time_end": None}
    sun = (times or {}).get("sun", {}).get("events", {})
    sunrise, sunset = parse_time(sun.get("sunrise")), parse_time(sun.get("sunset"))
    if not sunrise or not sunset:
        return result
    rahu = (times or {}).get("rahukaalam") or {}
    rahu_start, rahu_end = parse_time(rahu.get("start")), parse_time(rahu.get("end"))
    span = (sunset - sunrise) / 12
    first = CHALDEAN.index(WEEKDAY_LORD[day.weekday()])
    for i in range(12):
        start, end = sunrise + span * i, sunrise + span * (i + 1)
        lord = CHALDEAN[(first + i) % 7]
        overlaps_rahu = rahu_start and rahu_end and start < rahu_end and end > rahu_start
        if lord == planet and not overlaps_rahu:
            clock = lambda t: t.strftime("%I:%M %p").lstrip("0")
            result.update(time=f"{clock(start)} – {clock(end)}", time_start=start.isoformat(), time_end=end.isoformat())
            break
    return result


def affirmation(reader_sign: str, day: date) -> str:
    return AFFIRMATIONS[(day.toordinal() * 7 + sign_index(reader_sign) * 3) % len(AFFIRMATIONS)]


def special_advice(bodies: dict, reader_sign: str) -> str:
    moon = bodies["moon"]
    h = house(moon["sign"], reader_sign)
    text = f"With the Moon in {moon['sign']}, your {ordinal(h)} house of {HOUSE_MEANING[h]}, {MOON_ADVICE[h]}"
    if bodies.get("mercury", {}).get("retrograde"):
        text += " " + RETRO_NOTE["mercury"]
    return text


def sky_summary(positions: dict, times: dict | None, panchang: dict | None) -> dict:
    bodies = positions["bodies"]
    moon = (times or {}).get("moon") or {}
    retro = [name for name, b in bodies.items() if b.get("retrograde") and b.get("type") not in ("lunar_node",)]
    sun = (times or {}).get("sun", {}).get("events", {})
    pan = panchang or {}
    def at_start(limb):
        value = (pan.get(limb) or {}).get("at_start") or {}
        return value.get("name") and (f"{value['paksha']} {value['name']}" if value.get("paksha") else value["name"])
    return {
        "sun_sign": bodies["sun"]["sign"], "moon_sign": bodies["moon"]["sign"],
        "moon_phase": moon.get("phase_description"), "moon_illumination": round((moon.get("details") or {}).get("illumination_pct", 0)) if moon.get("details") else None,
        "retrogrades": retro, "sunrise": sun.get("sunrise"), "sunset": sun.get("sunset"),
        "moonrise": moon.get("moonrise"), "moonset": moon.get("moonset"),
        "rahu_kaal_start": ((times or {}).get("rahukaalam") or {}).get("start"), "rahu_kaal_end": ((times or {}).get("rahukaalam") or {}).get("end"),
        "tithi": at_start("tithi"), "nakshatra": at_start("nakshatra"), "yoga": at_start("yoga"),
    }


def insights(positions: dict, times: dict | None, panchang: dict | None, reader_sign: str, day: date) -> dict:
    bodies = positions["bodies"]
    energy = scores(bodies, reader_sign)
    luck = lucky(bodies, reader_sign, day, times)
    affirm = affirmation(reader_sign, day)
    return {"energy": energy, "lucky": luck, "affirmation": affirm, "special_advice": special_advice(bodies, reader_sign),
            "categories": categories(bodies, reader_sign, energy, luck, affirm), "sky": sky_summary(positions, times, panchang),
            "positions_at": positions.get("datetime")}


def forecast_summary(positions: dict, reader_sign: str, energy: dict) -> str:
    bodies = positions["bodies"]
    moon = bodies["moon"]
    h = house(moon["sign"], reader_sign)
    best = max(energy, key=energy.get)
    worst = min(energy, key=energy.get)
    names = {"love": "Love", "career": "Career", "health": "Health", "spiritual": "Spiritual growth"}
    text = (f"Tomorrow the Moon moves through {moon['sign']}, your {ordinal(h)} house of {HOUSE_MEANING[h]}, so {MOON_ADVICE[h]} "
            f"{names[best]} looks strongest ({energy[best]}%), while {names[worst].lower()} may need extra care ({energy[worst]}%).")
    retro = [p.title() for p in ("mercury", "venus", "mars") if bodies.get(p, {}).get("retrograde")]
    if retro:
        text += f" {' and '.join(retro)} {'is' if len(retro) == 1 else 'are'} still retrograde."
    return text


def noon(day: date, tz) -> str:
    return datetime(day.year, day.month, day.day, 12, tzinfo=tz).isoformat()


def week_key(day: date) -> str:
    monday = day - timedelta(days=day.weekday())
    return monday.isoformat()


def compatibility(a: str, b: str) -> dict:
    ia, ib = sign_index(a), sign_index(b)
    distance = min((ia - ib) % 12, (ib - ia) % 12)
    same_element = ELEMENTS[ia] == ELEMENTS[ib]
    friendly = {("fire", "air"), ("air", "fire"), ("earth", "water"), ("water", "earth")}
    base = {0: 78, 1: 55, 2: 82, 3: 50, 4: 92, 5: 45, 6: 74}[distance]
    if (ELEMENTS[ia], ELEMENTS[ib]) in friendly and distance != 2:
        base += 4
    aspect = {0: "share the same sign — instant understanding, but you may mirror each other's blind spots",
              1: "are neighbours on the zodiac — different instincts that need patience to align",
              2: "form a sextile — easy friendship and lively conversation",
              3: "form a square — real chemistry with some friction; growth comes from compromise",
              4: "form a trine — a natural, flowing match that feels comfortable from day one",
              5: "form a quincunx — very different needs, so clear communication is essential",
              6: "are opposites — a magnetic attraction where each fills the other's gaps"}[distance]
    element_note = (f"Both are {ELEMENTS[ia]} signs, so you speak the same emotional language." if same_element
                    else f"{a.title()} brings {ELEMENTS[ia]} energy and {b.title()} brings {ELEMENTS[ib]} energy.")
    return {"score": min(base, 97), "aspect": distance, "summary": f"{a.title()} and {b.title()} {aspect}. {element_note}",
            "elements": [ELEMENTS[ia], ELEMENTS[ib]], "modalities": [MODALITIES[ia], MODALITIES[ib]]}
