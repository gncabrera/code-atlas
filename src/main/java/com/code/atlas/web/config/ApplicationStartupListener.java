package com.code.atlas.web.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildProperties;

    public ApplicationStartupListener(
            Environment environment,
            ObjectProvider<BuildProperties> buildProperties) {
        this.environment = environment;
        this.buildProperties = buildProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String applicationName = environment.getProperty("spring.application.name", "code-atlas");
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String contextPath = normalizeContextPath(environment.getProperty("server.servlet.context-path", ""));
        String localUrl = "http://localhost:" + port + contextPath;
        BuildProperties properties = buildProperties.getIfAvailable();
        String version = properties != null ? properties.getVersion() : "development";
        String profile = String.join(
                ", ",
                environment.getActiveProfiles().length > 0
                        ? environment.getActiveProfiles()
                        : new String[] {environment.getProperty("spring.profiles.active", "default")});
        String javaVersion = System.getProperty("java.version", "unknown");

        System.out.print(StartupConsoleBanner.render(
                capitalize(applicationName),
                localUrl,
                version,
                profile,
                javaVersion));
    }

    private static String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    private static String capitalize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String[] parts = value.split("-");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return formatted.isEmpty() ? value : formatted.toString();
    }
}
