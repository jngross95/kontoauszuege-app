# Copilot Instruktionen — Vaadin MCP Dokumentation

Zweck: Diese Datei weist Copilot an, bei Vaadin‑bezogenen Fragen primär die konfigurierte Vaadin MCP‑Dokumentation zu verwenden.

Anweisungen:
- **Quelle:** Nutze die URL in `.vscode/mcp.json` (Schlüssel `servers.vaadin.url`) als erste Referenzquelle für Vaadin‑spezifische Antworten.
- **Priorität lokal:** Prüfe zuerst, ob ein lokaler MCP‑Server erreichbar ist (z. B. `http://127.0.0.1:<port>`). Wenn lokal erreichbar, verwende diese Instanz.
- **Fallback remote:** Falls kein lokaler MCP erreichbar ist, verwende die Remote‑URL aus `.vscode/mcp.json`.
- **Zitieren:** Wenn Inhalte aus der MCP‑Dokumentation verwendet werden, nenne die Quelle und gib die verwendete URL an.
- **Fehlerbehandlung:** Wenn die MCP‑Doku nicht erreichbar ist, informiere kurz den Nutzer und frage, ob stattdessen andere Quellen (z. B. Vaadin Website oder StackOverflow) verwendet werden dürfen.

Verhalten bei Antworten:
- Nutze MCP‑Dokumentation nur für Vaadin‑spezifische Fragen; allgemeine Fragen beantworte ich weiterhin aus meinem eingebauten Wissen.
- Halte Zitate kurz und verlinke zur Originalseite.

Pfad zur Konfiguration: `.vscode/mcp.json`

Wenn du Anpassungen möchtest (z. B. harte Priorisierung einer lokalen URL), sag Bescheid.
