package com.code.atlas.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

class StartupConsoleBannerTest {

    @Test
    void renderIncludesLocalUrlAndMetadata() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);

        String banner = StartupConsoleBanner.render(
                "Code Atlas",
                "http://localhost:8088",
                "0.2.0",
                "default",
                "21.0.6");

        assertThat(banner).contains("Code Atlas is ready");
        assertThat(banner).contains("http://localhost:8088");
        assertThat(banner).contains("0.2.0");
        assertThat(banner).contains("default");
        assertThat(banner).contains("21.0.6");
    }
}
