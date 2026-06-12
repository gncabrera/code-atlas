package com.code.atlas.web.service.context.indexed.engine.context.retriever;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SymbolCenteredSnippetExtractor {

    private static final int SYMBOL_LINE_RADIUS = 20;

    private SymbolCenteredSnippetExtractor() {
    }

    public static String extract(
            List<String> lines,
            List<String> anchorSymbols,
            int maxSnippetLines,
            int maxSnippetChars
    ) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        List<LineRange> ranges = findSymbolRanges(lines, anchorSymbols);
        if (ranges.isEmpty()) {
            return extractLeadingLines(lines, maxSnippetLines, maxSnippetChars);
        }
        List<LineRange> mergedRanges = mergeRanges(ranges);
        return buildSnippet(lines, mergedRanges, maxSnippetLines, maxSnippetChars);
    }

    private static List<LineRange> findSymbolRanges(List<String> lines, List<String> anchorSymbols) {
        List<LineRange> ranges = new ArrayList<>();
        if (anchorSymbols == null || anchorSymbols.isEmpty()) {
            return ranges;
        }
        for (String symbol : anchorSymbols) {
            if (symbol == null || symbol.isBlank()) {
                continue;
            }
            String needle = symbol.trim().toLowerCase(Locale.ROOT);
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                if (lines.get(lineIndex).toLowerCase(Locale.ROOT).contains(needle)) {
                    int start = Math.max(0, lineIndex - SYMBOL_LINE_RADIUS);
                    int end = Math.min(lines.size() - 1, lineIndex + SYMBOL_LINE_RADIUS);
                    ranges.add(new LineRange(start, end));
                }
            }
        }
        return ranges;
    }

    private static List<LineRange> mergeRanges(List<LineRange> ranges) {
        List<LineRange> sorted = ranges.stream()
                .sorted(Comparator.comparingInt(LineRange::start).thenComparingInt(LineRange::end))
                .toList();
        List<LineRange> merged = new ArrayList<>();
        for (LineRange range : sorted) {
            if (merged.isEmpty()) {
                merged.add(range);
                continue;
            }
            LineRange last = merged.getLast();
            if (range.start <= last.end + 1) {
                merged.set(merged.size() - 1, new LineRange(last.start, Math.max(last.end, range.end)));
            } else {
                merged.add(range);
            }
        }
        return merged;
    }

    private static String buildSnippet(
            List<String> lines,
            List<LineRange> ranges,
            int maxSnippetLines,
            int maxSnippetChars
    ) {
        StringBuilder snippetBuilder = new StringBuilder();
        int emittedLines = 0;
        for (int rangeIndex = 0; rangeIndex < ranges.size(); rangeIndex++) {
            if (rangeIndex > 0) {
                appendSeparator(snippetBuilder, maxSnippetChars);
            }
            LineRange range = ranges.get(rangeIndex);
            for (int lineIndex = range.start; lineIndex <= range.end; lineIndex++) {
                if (emittedLines >= maxSnippetLines) {
                    appendTruncation(snippetBuilder, maxSnippetChars);
                    return snippetBuilder.toString().trim();
                }
                String line = lines.get(lineIndex);
                if (!appendLine(snippetBuilder, line, maxSnippetChars)) {
                    return snippetBuilder.toString().trim();
                }
                emittedLines++;
            }
        }
        return snippetBuilder.toString().trim();
    }

    private static String extractLeadingLines(List<String> lines, int maxSnippetLines, int maxSnippetChars) {
        StringBuilder snippetBuilder = new StringBuilder();
        int emittedLines = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (emittedLines >= maxSnippetLines) {
                appendTruncation(snippetBuilder, maxSnippetChars);
                break;
            }
            if (!appendLine(snippetBuilder, line, maxSnippetChars)) {
                break;
            }
            emittedLines++;
        }
        return snippetBuilder.toString().trim();
    }

    private static boolean appendLine(StringBuilder snippetBuilder, String line, int maxSnippetChars) {
        int additionalChars = line.length() + (snippetBuilder.isEmpty() ? 0 : 1);
        if (snippetBuilder.length() + additionalChars > maxSnippetChars) {
            appendTruncation(snippetBuilder, maxSnippetChars);
            return false;
        }
        if (!snippetBuilder.isEmpty()) {
            snippetBuilder.append('\n');
        }
        snippetBuilder.append(line);
        return true;
    }

    private static void appendSeparator(StringBuilder snippetBuilder, int maxSnippetChars) {
        String separator = "\n// ...\n";
        if (snippetBuilder.length() + separator.length() <= maxSnippetChars) {
            snippetBuilder.append(separator);
        }
    }

    private static void appendTruncation(StringBuilder snippetBuilder, int maxSnippetChars) {
        String marker = "\n// ... truncated ...";
        if (snippetBuilder.length() + marker.length() <= maxSnippetChars) {
            snippetBuilder.append(marker);
        }
    }

    private record LineRange(int start, int end) {
    }

}
