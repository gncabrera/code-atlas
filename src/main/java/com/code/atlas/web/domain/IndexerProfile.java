package com.code.atlas.web.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public enum IndexerProfile {

    SPRING_JAVA(Set.of("java", "xml", "yml", "yaml", "properties")),
    SPRING_FLYWAY(Set.of("sql")),
    THYMELEAF(Set.of("html", "js")),
    ANGULAR(Set.of("ts", "html", "scss", "css", "json"));

    private final Set<String> extensions;

    IndexerProfile(Set<String> extensions) {
        this.extensions = extensions;
    }

    public Set<String> extensions() {
        return extensions;
    }

    public boolean owns(String extension) {
        return extension != null && extensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    public static List<String> conflicts(Collection<IndexerProfile> selected) {
        List<IndexerProfile> distinct = new ArrayList<>(new LinkedHashSet<>(selected));
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < distinct.size(); i++) {
            for (int j = i + 1; j < distinct.size(); j++) {
                Set<String> overlap = new TreeSet<>(distinct.get(i).extensions());
                overlap.retainAll(distinct.get(j).extensions());
                if (!overlap.isEmpty()) {
                    messages.add(distinct.get(i) + " and " + distinct.get(j) + " share " + overlap);
                }
            }
        }
        return messages;
    }
}
