#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${DAILY_ASTRO_ADMIN_KEY:?Set DAILY_ASTRO_ADMIN_KEY before running ingestion}"
exec python3 -m app.ingest
