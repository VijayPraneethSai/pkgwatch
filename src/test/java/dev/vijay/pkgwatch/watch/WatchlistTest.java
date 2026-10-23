package dev.vijay.pkgwatch.watch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WatchlistTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String PACKAGE_JSON = """
            {
              "name": "app",
              "dependencies": {"express": "^4.19.0", "@babel/core": "^7.0.0"},
              "devDependencies": {"jest": "^29.0.0", "express": "^4.0.0"},
              "scripts": {"test": "jest"}
            }
            """;

    @Test
    void readsDependencySectionsOnly() throws IOException {
        Set<String> names = Watchlist.fromPackageJson(PACKAGE_JSON, mapper);

        assertEquals(Set.of("express", "@babel/core", "jest"), names);
    }

    @Test
    void emptyDocumentGivesEmptyList() throws IOException {
        assertEquals(Set.of(), Watchlist.fromPackageJson("{}", mapper));
    }

    @Test
    void invalidJsonIsRejected() {
        assertThrows(IOException.class, () -> Watchlist.fromPackageJson("{not json", mapper));
    }

    @Test
    void mergesFileWithExtraNamesAndSkipsBlanks(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("package.json");
        Files.writeString(file, PACKAGE_JSON);

        Set<String> names = Watchlist.load(file, List.of("react", " ", "express"), mapper);

        assertEquals(Set.of("express", "@babel/core", "jest", "react"), names);
    }

    @Test
    void missingFileFallsBackToExtraNames(@TempDir Path dir) throws IOException {
        Set<String> names = Watchlist.load(dir.resolve("absent.json"), List.of("react"), mapper);

        assertEquals(Set.of("react"), names);
    }
}
