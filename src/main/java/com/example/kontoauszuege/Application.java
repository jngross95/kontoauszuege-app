package com.example.kontoauszuege;

import com.example.kontoauszuege.service.BankAccess.BankConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;

import java.nio.file.Files;
import java.nio.file.Path;
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
                boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
                boolean opened = isWindows ? tryAppModeWin(url) : tryAppMode(url);
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
    private static boolean tryAppMode(String url) {
        String[] candidates = {"google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "brave-browser", "flatpak run com.brave.Browser"};
        for (String browser : candidates) {
            try {
                String[] command = browser.split(" ");
                List<String> commandList = new ArrayList<>(Arrays.asList(command));
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
    private static boolean tryAppModeWin(String url) {
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
                new ProcessBuilder(exe, "--app=" + url).start();
                log.info("App-Modus (Windows) gestartet mit: {}", exe);
                return true;
            } catch (Exception ignored) {
                // nächsten Kandidaten versuchen
            }
        }
        return false;
    }
}
