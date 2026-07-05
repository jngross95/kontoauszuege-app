#!/usr/bin/env bash
set -euo pipefail

# download-icons.sh
# Lädt Icons/Images aus dem Internet und speichert sie im Ressourcen-Ordner
# Zielverzeichnis (für Vaadin/Spring statische Ressourcen):
DEST_DIR="src/main/resources/META-INF/resources/icons"

mkdir -p "$DEST_DIR"

# --- Datenbereich: URL|Dateiname ---
# Kommentarzeilen (beginnen mit #) und leere Zeilen werden ignoriert.
# Trage hier die gewünschten Icon-URLs und die gewünschten Dateinamen ein.
URL_LIST=$(cat <<'URLS'
#Sparkassen
https://www.sparkasse-erlangen.de/content/dam/myif/sksk-erlangen/work/bilder/icons/favicon2x.ico|sparkasse-favicon2x.ico
#VR-Bank
https://atruvia.scene7.com/is/image/atruvia/VRFavicon48x48|VRFavicon48x48.png
#Paypal
https://www.paypalobjects.com/marketing/web/logos/paypal-mark-color_new.svg|paypal-mark-color_new.svg
##
URLS
)

# Funktion: download mit curl oder wget
download() {
  local url="$1" file="$2"
  local out="$DEST_DIR/$file"

  echo "Herunterladen: $url -> $out"

  if command -v curl >/dev/null 2>&1; then
    curl -fSL --retry 3 --retry-delay 2 "$url" -o "$out" || return 1
  elif command -v wget >/dev/null 2>&1; then
    wget -qO "$out" "$url" || return 1
  else
    echo "Fehler: weder curl noch wget verfügbar" >&2
    return 2
  fi

  return 0
}

# Verarbeite die Liste
failures=()
successes=()
while IFS='|' read -r url filename; do
  # Trim leading/trailing whitespace
  url="$(echo "$url" | sed -e 's/^\s*//' -e 's/\s*$//')"
  filename="$(echo "$filename" | sed -e 's/^\s*//' -e 's/\s*$//')"
  # Skip comments / empty
  [[ -z "$url" ]] && continue
  [[ "$url" =~ ^# ]] && continue
  if [[ -z "$filename" ]]; then
    echo "Warnung: kein Dateiname für URL: $url. Überspringe." >&2
    failures+=("$url (no filename)")
    continue
  fi

  # Skip download if file already exists
  out="$DEST_DIR/$filename"
  if [ -f "$out" ]; then
    echo "Datei existiert bereits, überspringe: $out"
    successes+=("$filename (exists)")
    continue
  fi

  if download "$url" "$filename"; then
    successes+=("$filename")
  else
    echo "Fehler beim Herunterladen: $url" >&2
    failures+=("$url -> $filename")
  fi

done <<<"$URL_LIST"

# Zusammenfassung
echo
if [ ${#successes[@]} -gt 0 ]; then
  echo "Erfolgreich heruntergeladen (${#successes[@]}):"
  for f in "${successes[@]}"; do echo "  - $f"; done
else
  echo "Keine Dateien erfolgreich heruntergeladen."
fi

if [ ${#failures[@]} -gt 0 ]; then
  echo
  echo "Fehler beim Herunterladen (${#failures[@]}):"
  for f in "${failures[@]}"; do echo "  - $f"; done
  exit 1
fi

echo
echo "Fertig. Dateien liegen in: $DEST_DIR"
exit 0
