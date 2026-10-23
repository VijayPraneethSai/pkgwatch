package dev.vijay.pkgwatch.watch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public final class Watchlist {

    private static final String[] SECTIONS = {"dependencies", "devDependencies", "optionalDependencies"};

    private Watchlist() {
    }

    public static Set<String> fromPackageJson(String json, ObjectMapper mapper) throws IOException {
        Set<String> names = new TreeSet<>();
        JsonNode root = mapper.readTree(json);
        for (String section : SECTIONS) {
            Iterator<String> fields = root.path(section).fieldNames();
            while (fields.hasNext()) {
                add(names, fields.next());
            }
        }
        return names;
    }

    public static Set<String> load(Path packageJson, Collection<String> extra, ObjectMapper mapper)
            throws IOException {
        Set<String> names = new TreeSet<>();
        if (packageJson != null && Files.isRegularFile(packageJson)) {
            names.addAll(fromPackageJson(Files.readString(packageJson), mapper));
        }
        extra.forEach(name -> add(names, name));
        return names;
    }

    private static void add(Set<String> names, String name) {
        if (name != null && !name.isBlank()) {
            names.add(name.trim());
        }
    }
}
