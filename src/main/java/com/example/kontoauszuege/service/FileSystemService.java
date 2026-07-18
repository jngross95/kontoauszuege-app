package com.example.kontoauszuege.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSystemService {

    // Basisverzeichnis (konfiguriert als feste, private Variable wie gewünscht)
    private String baseDir = "/home/jngross/Dokumente/doc";

    public String getBaseDir() {
        return baseDir;
    }

    /**
     * Liefert die Dateinamen (nur Dateien, keine Verzeichnisse) im angegebenen
     * relativen Ordner-Pfad. Wenn der übergebene Pfad null oder leer ist,
     * wird das Basisverzeichnis verwendet.
     * Rückgabe: sortierte Liste der Dateinamen (ohne Pfad).
     */
    public List<String> getFileNames(String relativeFolderPath) {
        Objects.requireNonNull(baseDir, "baseDir must not be null");
        Path root = Paths.get(baseDir);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return List.of();
        }

        Path folder = (relativeFolderPath == null || relativeFolderPath.isBlank())
                ? root
                : root.resolve(relativeFolderPath.replace('/', File.separatorChar));

        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(folder)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted(String::compareToIgnoreCase)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Lesen des Ordners: " + folder, e);
        }
    }

    /**
     * Liefert alle Unterverzeichnisse des basisverzeichnisses rekursiv als
     * relative Pfade (mit '/' als Trenner), z.B. "juergen/dir1", "kg/w1".
     */
    public List<String> getSubDirectories() {
        Objects.requireNonNull(baseDir, "baseDir must not be null");
        Path root = Paths.get(baseDir);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(root))
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(s -> s.replace(File.separatorChar, '/'))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Durchsuchen des Verzeichnisses: " + baseDir, e);
        }
    }

}
