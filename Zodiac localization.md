# Zodiac localization

The database uses stable English IDs such as `aries`, `taurus`, and `pisces`. Display names are localized at the API boundary and in the Android sign picker.

Request the catalog in the selected language:

```http
GET /api/v1/zodiac-signs?language=hi
```

The same sign keeps the same ID while its display name changes:

```json
{
  "id": "aries",
  "name": "मेष",
  "date_range": "Mar 21 - Apr 19",
  "symbol": "♈"
}
```

The horoscope endpoint uses the same rule:

```http
GET /api/v1/horoscope/today?sign=aries&language=hi
```

`sign=aries` is used for lookup and storage. The response's `sign.name` is localized. Horoscope paragraph translation remains a separate ingestion concern; it must be translated and stored per language rather than labeling English text as Hindi.
