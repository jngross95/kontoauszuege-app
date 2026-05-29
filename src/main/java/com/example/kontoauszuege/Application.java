package com.example.kontoauszuege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try{
            log.info("Starte Kontoauszüge App ...");
            SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            log.error("Fehler beim Starten der Anwendung: {}", e.getMessage(), e);
            onServerReady(null);
        }
    }

    @EventListener
    static public void onServerReady(ServletWebServerInitializedEvent event) {
        //int port = event.getWebServer().getPort();
        int port = 8080;
        String url = "http://localhost:" + port;

        Thread browserThread = new Thread(() -> {
            try {
                log.info("Browser starten ... ");

                if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    // Fallback für Linux ohne GUI-Desktop (z.B. xdg-open)
                    new ProcessBuilder("xdg-open", url).start();
                }
            } catch (Exception e) {
                log.error("Browser konnte nicht geöffnet werden: {}", e.getMessage(), e);
            }
        }, "browser-opener");
        //browserThread.setDaemon(true);
        browserThread.start();
    }
}
