package com.example.kontoauszuege.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemService fs;

    private void setBaseDir(FileSystemService fs, Path base) throws Exception {
        Field f = FileSystemService.class.getDeclaredField("baseDir");
        f.setAccessible(true);
        f.set(fs, base.toString());
    }

    @BeforeEach
    void beforeEach() throws Exception {
        fs = new FileSystemService();
        setBaseDir(fs, tempDir);
    }

    @Test
    void copyFile_success() throws Exception {

        // prepare source file (outside baseDir)
        Path source = Files.createTempFile("srcfile", ".txt");
        String content = "hello-world";
        Files.writeString(source, content);

        // prepare target directory inside baseDir
        Path targetDir = tempDir.resolve("kunde/2026");
        Files.createDirectories(targetDir);
        String destRel = "kunde/2026/beleg.pdf";

        fs.copyFile(source.toString(), destRel);

        Path dest = targetDir.resolve("beleg.pdf");
        assertTrue(Files.exists(dest), "Zieldatei sollte existieren");
        assertEquals(content, Files.readString(dest));
    }

    @Test
    void copyFile_parentMissing_throws() throws Exception {

        Path source = Files.createTempFile("srcfile", ".txt");
        Files.writeString(source, "x");

        String destRel = "no/such/dir/file.pdf";

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> fs.copyFile(source.toString(), destRel));
        assertTrue(ex.getMessage().contains("Zielverzeichnis existiert nicht"));
    }

    @Test
    void copyFile_destExists_throws() throws Exception {

        Path source = Files.createTempFile("srcfile", ".txt");
        Files.writeString(source, "data");

        Path targetDir = tempDir.resolve("kunde/2026");
        Files.createDirectories(targetDir);
        Path dest = targetDir.resolve("beleg.pdf");
        Files.writeString(dest, "already");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> fs.copyFile(source.toString(), "kunde/2026/beleg.pdf"));
        assertTrue(ex.getMessage().contains("Zieldatei existiert bereits"));
        assertTrue(ex.getMessage().contains(dest.toAbsolutePath().toString()));
    }

    @Test
    void copyFile_sourceMissing_throws() throws Exception {
        String missing = tempDir.resolve("does-not-exist.txt").toString();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fs.copyFile(missing, "any/file.pdf"));
        assertTrue(ex.getMessage().contains("Quelldatei existiert nicht"));
    }

    @Test
    void getSubDirectories_returnsRelativePaths() throws Exception {
        // create temp directory structure
        Path root = Files.createTempDirectory("fsstest-test");
        try {
            Path d1 = root.resolve("juergen").resolve("dir1");
            Files.createDirectories(d1);

            Path d2 = root.resolve("kg").resolve("w1");
            Files.createDirectories(d2);

            FileSystemService svc = new FileSystemService();

            // set private baseDir via reflection
            Field f = FileSystemService.class.getDeclaredField("baseDir");
            f.setAccessible(true);
            f.set(svc, root.toString());

            assertEquals(root.toString(), svc.getBaseDir());

            List<String> subs = svc.getSubDirectories();
            // should contain juergen/dir1 and kg/w1 (slash normalized)
            assertTrue(subs.contains("juergen/dir1"), "missing juergen/dir1");
            assertTrue(subs.contains("kg/w1"), "missing kg/w1");
        } finally {
            // cleanup
            Files.walk(root)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        }
    }
}
