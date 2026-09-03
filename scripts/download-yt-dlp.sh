#!/usr/bin/env bash
# Download the latest yt-dlp release into a target assets directory.
# Idempotent: if the binary is already present and looks complete, do nothing.

set -euo pipefail

ASSETS_DIR="${1:-$(pwd)/app/src/main/assets}"
TARGET="${ASSETS_DIR}/yt-dlp"
MIN_SIZE=1000000  # 1 MB sanity floor

mkdir -p "${ASSETS_DIR}"

if [ -f "${TARGET}" ] && [ "$(stat -c%s "${TARGET}" 2>/dev/null || stat -f%z "${TARGET}")" -ge "${MIN_SIZE}" ]; then
    echo "yt-dlp already present at ${TARGET} ($(stat -c%s "${TARGET}" 2>/dev/null || stat -f%z "${TARGET}") bytes) — skipping."
    exit 0
fi

URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
echo "Downloading yt-dlp from ${URL} ..."
TMP=$(mktemp)
if command -v curl >/dev/null 2>&1; then
    curl -fsSL --retry 3 -o "${TMP}" "${URL}"
elif command -v wget >/dev/null 2>&1; then
    wget -q -O "${TMP}" "${URL}"
else
    echo "Neither curl nor wget is available." >&2
    exit 1
fi

SIZE=$(stat -c%s "${TMP}" 2>/dev/null || stat -f%z "${TMP}")
if [ "${SIZE}" -lt "${MIN_SIZE}" ]; then
    echo "Downloaded yt-dlp is too small (${SIZE} bytes) — aborting." >&2
    exit 1
fi

mv "${TMP}" "${TARGET}"
chmod +x "${TARGET}" 2>/dev/null || true
echo "yt-dlp downloaded to ${TARGET} (${SIZE} bytes)"
