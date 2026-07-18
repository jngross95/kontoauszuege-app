package com.example.kontoauszuege.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemServiceTest {

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
                    .filter(x -> x.getFileName().toString().contains("fsstest-test"))
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        }
    }
}
