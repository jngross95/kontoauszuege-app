#!/usr/bin/env bash
set -euo pipefail
# Downloads pdf.js distribution from GitHub releases and extracts minimal files
# Adjust VER if you want a different version.

VER="3.10.110"
ZIP="pdfjs-${VER}-dist.zip"
URL="https://github.com/mozilla/pdf.js/releases/download/v${VER}/${ZIP}"
TMPDIR="/tmp/pdfjs_dist_${VER}"
OUT_DIR="src/main/resources/META-INF/resources/pdfjs"
LIB_DIR="$OUT_DIR/lib"

mkdir -p "$TMPDIR" "$LIB_DIR"

# First try to download build files directly from jsDelivr (fast single files)
JSDELIVR_BASE="https://cdn.jsdelivr.net/npm/pdfjs-dist@${VER}/build"
echo "Trying direct download from $JSDELIVR_BASE ..."
if command -v curl >/dev/null 2>&1; then
  curl -fL -o "$LIB_DIR/pdf.min.js" "$JSDELIVR_BASE/pdf.min.js" || true
  curl -fL -o "$LIB_DIR/pdf.worker.min.js" "$JSDELIVR_BASE/pdf.worker.min.js" || true
elif command -v wget >/dev/null 2>&1; then
  wget -O "$LIB_DIR/pdf.min.js" "$JSDELIVR_BASE/pdf.min.js" || true
  wget -O "$LIB_DIR/pdf.worker.min.js" "$JSDELIVR_BASE/pdf.worker.min.js" || true
else
  echo "Install curl or wget to download files." >&2
  exit 1
fi

if [ -s "$LIB_DIR/pdf.min.js" ] && [ -s "$LIB_DIR/pdf.worker.min.js" ]; then
  # Quick sanity check: ensure files are not HTML error pages
  if grep -Eqi "Not found|Package version not found|404|Not Found" "$LIB_DIR/pdf.min.js" || grep -Eqi "Not found|Package version not found|404|Not Found" "$LIB_DIR/pdf.worker.min.js"; then
    echo "Downloaded files appear to be error pages; removing and falling back." >&2
    rm -f "$LIB_DIR/pdf.min.js" "$LIB_DIR/pdf.worker.min.js" || true
  else
    echo "Downloaded build files from jsDelivr into $LIB_DIR"
    echo "You can now serve viewer.html which references lib/pdf.min.js and lib/pdf.worker.min.js"
    exit 0
  fi
fi

echo "Direct build download failed; falling back to GitHub release ZIP: $URL"
if command -v curl >/dev/null 2>&1; then
  curl -L -o "$TMPDIR/$ZIP" "$URL"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$TMPDIR/$ZIP" "$URL"
fi

echo "Extracting $ZIP ..."
if unzip -o "$TMPDIR/$ZIP" -d "$TMPDIR/extracted"; then
  if [ -f "$TMPDIR/extracted/build/pdf.min.js" ]; then
    cp "$TMPDIR/extracted/build/pdf.min.js" "$LIB_DIR/"
  fi
  if [ -f "$TMPDIR/extracted/build/pdf.worker.min.js" ]; then
    cp "$TMPDIR/extracted/build/pdf.worker.min.js" "$LIB_DIR/"
  fi
  # Copy web viewer assets (viewer.css, viewer.js templates) to the pdfjs folder
  if [ -d "$TMPDIR/extracted/web" ]; then
    cp -r "$TMPDIR/extracted/web"/* "$OUT_DIR/" || true
  fi
  echo "PDF.js files copied to $OUT_DIR (lib: $LIB_DIR)"
  echo "You can now serve viewer.html which references lib/pdf.min.js and lib/pdf.worker.min.js"
else
  echo "Failed to extract ZIP. The release may not exist for VER=$VER or network blocked." >&2
  exit 1
fi
