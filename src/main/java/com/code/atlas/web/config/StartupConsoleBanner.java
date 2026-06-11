package com.code.atlas.web.config;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

final class StartupConsoleBanner {

    private static final String RESET = "\u001B[0m";
    private static final String BOX_TOP_LEFT = "\u250C";
    private static final String BOX_TOP_RIGHT = "\u2510";
    private static final String BOX_BOTTOM_LEFT = "\u2514";
    private static final String BOX_BOTTOM_RIGHT = "\u2518";
    private static final String BOX_HORIZONTAL = "\u2500";
    private static final String BOX_VERTICAL = "\u2502";

    private StartupConsoleBanner() {
    }

    static String render(
            String applicationName,
            String localUrl,
            String version,
            String profile,
            String javaVersion) {
        String readyLine = "  " + applicationName + " is ready";
        String localLine = "  Local   " + styled(localUrl, AnsiStyle.BOLD, AnsiColor.BRIGHT_CYAN);
        String versionLine = "  Version " + version;
        String profileLine = "  Profile " + profile;
        String javaLine = "  Java    " + javaVersion;

        int innerWidth = maxLength(readyLine, localLine, versionLine, profileLine, javaLine);
        String horizontal = BOX_HORIZONTAL.repeat(innerWidth + 2);

        StringBuilder box = new StringBuilder();
        box.append('\n');
        box.append(styled(BOX_TOP_LEFT + horizontal + BOX_TOP_RIGHT, AnsiColor.BRIGHT_GREEN)).append('\n');
        appendBoxLine(box, readyLine, innerWidth);
        box.append(styled(BOX_VERTICAL, AnsiColor.BRIGHT_GREEN))
                .append(styled(" ".repeat(innerWidth + 2), AnsiColor.BRIGHT_GREEN))
                .append(styled(BOX_VERTICAL, AnsiColor.BRIGHT_GREEN))
                .append('\n');
        appendBoxLine(box, localLine, innerWidth);
        appendBoxLine(box, versionLine, innerWidth);
        appendBoxLine(box, profileLine, innerWidth);
        appendBoxLine(box, javaLine, innerWidth);
        box.append(styled(BOX_BOTTOM_LEFT + horizontal + BOX_BOTTOM_RIGHT, AnsiColor.BRIGHT_GREEN))
                .append('\n');
        return box.toString();
    }

    private static void appendBoxLine(StringBuilder box, String content, int innerWidth) {
        int visibleLength = visibleLength(content);
        int padding = Math.max(0, innerWidth - visibleLength);
        box.append(styled(BOX_VERTICAL, AnsiColor.BRIGHT_GREEN))
                .append(' ')
                .append(content)
                .append(" ".repeat(padding))
                .append(' ')
                .append(styled(BOX_VERTICAL, AnsiColor.BRIGHT_GREEN))
                .append('\n');
    }

    private static int maxLength(String... lines) {
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, visibleLength(line));
        }
        return max;
    }

    private static int visibleLength(String text) {
        return text.replaceAll("\u001B\\[[0-9;]*m", "").length();
    }

    private static String styled(String text, AnsiColor color) {
        if (!ansiEnabled()) {
            return text;
        }
        return AnsiOutput.toString(color) + text + RESET;
    }

    private static String styled(String text, AnsiStyle style, AnsiColor color) {
        if (!ansiEnabled()) {
            return text;
        }
        return AnsiOutput.toString(style, color) + text + RESET;
    }

    private static boolean ansiEnabled() {
        return AnsiOutput.getEnabled() != AnsiOutput.Enabled.NEVER;
    }
}
