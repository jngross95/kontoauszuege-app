package com.example.kontoauszuege;

import com.example.kontoauszuege.service.BankAccess.BankConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try{
            log.info("Starte Kontoauszüge App ...");
            BankConnection.init();
            ensureDataDirectory();
            SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            log.error("Fehler beim Starten der Anwendung: {}", e.getMessage(), e);
            onServerReady(null);
        }
    }

    /**
     * Stellt sicher, dass das Datenverzeichnis ~/.jbanking existiert, bevor die
     * H2-Datenbank initialisiert wird (H2 legt das übergeordnete Verzeichnis nicht
     * selbst an). Ist der Ordner bereits vorhanden, passiert nichts.
     */
    private static void ensureDataDirectory() {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".jbanking");
            Files.createDirectories(dir);
            log.info("Datenverzeichnis: {}", dir.toAbsolutePath());
        } catch (Exception e) {
            log.error("Datenverzeichnis konnte nicht angelegt werden: {}", e.getMessage(), e);
        }
    }

    @EventListener
    static public void onServerReady(ServletWebServerInitializedEvent event) {
        //int port = event.getWebServer().getPort();
        int port = 8084;
        String url = "http://localhost:" + port;

        Thread browserThread = new Thread(() -> {
            try {
                log.info("Browser starten ... ");
                // Stelle sicher, dass das Verzeichnis existiert, in dem auch die H2-Datei liegt
                Path dbDir = Path.of(System.getProperty("user.home"), ".jbanking");
                Path braveUserData = dbDir.resolve("chromium-user-data");
                try {
                    Files.createDirectories(braveUserData);
                    log.info("Brave user-data dir: {}", braveUserData.toAbsolutePath());

                    // Preferences ins Default-Profil kopieren (Brave/Chrome lesen Default/Preferences)
                    Path defaultProfile = braveUserData.resolve("Default");
                    Files.createDirectories(defaultProfile);
                    copyResource("/chromium/Preferences", defaultProfile.resolve("Preferences"));
                    // Local State ins user-data-root kopieren (globale Einstellungen, z.B. P3A-Analyse-Dialog)
                    copyResource("/chromium/Local State", braveUserData.resolve("Local State"));

                } catch (Exception e) {
                    log.warn("Konnte Brave user-data dir nicht anlegen: {}", e.getMessage());
                }

                boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
                boolean opened = isWindows ? tryAppModeWin(url, braveUserData.toString()) : tryAppMode(url, braveUserData.toString());
                if (!opened) {
                    log.warn("Kein unterstützter Browser gefunden – bitte {} manuell öffnen.", url);
                }
            } catch (Exception e) {
                log.error("Browser konnte nicht geöffnet werden: {}", e.getMessage(), e);
            }
        }, "browser-opener");
        //browserThread.setDaemon(true);
        browserThread.start();
    }

    /** Versucht, Chrome/Chromium im App-Modus zu starten (kein Tab, kein Adressfeld). */
    private static boolean tryAppMode(String url, String userDataDir) {
        // Wenn der Benutzer die .desktop-Datei installiert hat, starte über gtk-launch
        // damit KDE die Anwendung dem .desktop-Eintrag und Icon zuordnen kann.
        Path userDesktop = Path.of(System.getProperty("user.home"), ".local/share/applications/kontoauszuege-app.desktop");
        Path sysDesktop = Path.of("/usr/share/applications/kontoauszuege-app.desktop");
        if (Files.exists(userDesktop) || Files.exists(sysDesktop)) {
            try {
                new ProcessBuilder("gtk-launch", "kontoauszuege-app").start();
                log.info("App über gtk-launch gestartet (kontoauszuege-app)");
                return true;
            } catch (Exception ex) {
                log.warn("gtk-launch fehlgeschlagen: {}", ex.getMessage());
                // fallthrough: probiere normalen Browser-Start
            }
        }

        String[] candidates = {"brave-browser", "google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "flatpak run com.brave.Browser"};
        for (String browser : candidates) {
            try {
                String[] command = browser.split(" ");
                List<String> commandList = new ArrayList<>(Arrays.asList(command));
                if (userDataDir != null && !userDataDir.isBlank()) {
                    commandList.add("--user-data-dir=" + userDataDir);
                }
                // Erzwinge X11/Ozone (nutzt XWayland unter Wayland), löst Icon/WM_CLASS Matching
                commandList.add("--ozone-platform=x11");
                // Setze eine eindeutige WM_CLASS, damit KDE das Fenster dem .desktop zuordnen kann
                commandList.add("--class=Kontoauszuege");
                // Autofill/Password-Dialoge unterdrücken (funktioniert für alle Chromium-basierten Browser)
                commandList.add("--disable-save-password-bubble");
                commandList.add("--disable-features=AutofillCreditCardUpload,AutofillSaveCardBubble,AutofillEnableAccountWalletStorage");
                commandList.add("--no-first-run");
                commandList.add("--app=" + url);
                new ProcessBuilder(commandList).start();
                log.info("App-Modus gestartet mit: {}", browser);
                return true;
            } catch (Exception ignored) {
                // nächsten Kandidaten versuchen
            }
        }
        return false;
    }

    /**
     * Windows-Variante: versucht zuerst Edge, dann Chrome über ihre typischen
     * Installationspfade, da diese Browser unter Windows nicht im PATH liegen.
     */
    private static boolean tryAppModeWin(String url, String userDataDir) {
        String localApp  = System.getenv("LOCALAPPDATA");
        String programFiles   = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");

        List<String> candidates = new ArrayList<>();

        // Microsoft Edge
        if (localApp != null) {
            candidates.add(localApp + "\\Microsoft\\Edge\\Application\\msedge.exe");
        }
        if (programFiles != null) {
            candidates.add(programFiles + "\\Microsoft\\Edge\\Application\\msedge.exe");
        }
        if (programFilesX86 != null) {
            candidates.add(programFilesX86 + "\\Microsoft\\Edge\\Application\\msedge.exe");
        }

        // Google Chrome
        if (localApp != null) {
            candidates.add(localApp + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        if (programFiles != null) {
            candidates.add(programFiles + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        if (programFilesX86 != null) {
            candidates.add(programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
        }

        for (String exe : candidates) {
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add(exe);
                if (userDataDir != null && !userDataDir.isBlank()) {
                    cmd.add("--user-data-dir=" + userDataDir);
                }
                cmd.add("--disable-save-password-bubble");
                cmd.add("--disable-features=AutofillCreditCardUpload,AutofillSaveCardBubble,AutofillEnableAccountWalletStorage");
                cmd.add("--no-first-run");
                cmd.add("--app=" + url);
                new ProcessBuilder(cmd).start();
                log.info("App-Modus (Windows) gestartet mit: {}", exe);
                return true;
            } catch (Exception ignored) {
                // nächsten Kandidaten versuchen
            }
        }
        return false;
    }

    private static void copyResource(String resourcePath, Path target) {
        try (InputStream in = Application.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Ressource '{}' geschrieben nach: {}", resourcePath, target.toAbsolutePath());
            } else {
                log.warn("Ressource '{}' nicht gefunden.", resourcePath);
            }
        } catch (Exception ex) {
            log.warn("Konnte '{}' nicht kopieren: {}", resourcePath, ex.getMessage());
        }
    }
}
