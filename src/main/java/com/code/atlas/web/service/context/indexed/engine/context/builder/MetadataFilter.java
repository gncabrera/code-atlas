package com.code.atlas.web.service.context.indexed.engine.context.builder;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectProjectType;
import com.code.atlas.web.repository.ProjectProjectTypeRepository;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MetadataFilter {

    private static final Set<String> EXCLUDED_PATH_SEGMENTS = Set.of(
            "node_modules",
            "target",
            "build",
            "dist",
            "out",
            "coverage",
            ".git"
    );

    private final ProjectProjectTypeRepository projectProjectTypeRepository;

    public MetadataFilter(ProjectProjectTypeRepository projectProjectTypeRepository) {
        this.projectProjectTypeRepository = projectProjectTypeRepository;
    }

    public MetadataFilterContext createContext(Project project) {
        Set<String> allowedExtensions = loadAllowedExtensions(project.getId());
        GitIgnoreMatcher gitIgnore = GitIgnoreMatcher.fromPath(Path.of(project.getPath(), ".gitignore"));
        return new MetadataFilterContext(allowedExtensions, gitIgnore);
    }

    public boolean shouldGenerateMetadata(MetadataFilterContext context, ProjectFileIndex file) {
        if (context == null || file == null) {
            return false;
        }
        String extension = normalizeExtension(file.getFileExtension());
        if (extension.isEmpty() || !context.allowedExtensions().contains(extension)) {
            return false;
        }
        String relativePath = normalizePath(file.getFilePath());
        if (containsExcludedSegment(relativePath)) {
            return false;
        }
        return !context.gitIgnore().isIgnored(relativePath);
    }

    private Set<String> loadAllowedExtensions(Long projectId) {
        List<ProjectProjectType> assignments = projectProjectTypeRepository.findByProjectIdOrderByProjectTypeNameAsc(projectId);
        Set<String> allowedExtensions = new LinkedHashSet<>();
        for (ProjectProjectType assignment : assignments) {
            if (assignment.getProjectType() == null || assignment.getProjectType().getAllowedExtensions() == null) {
                continue;
            }
            Arrays.stream(assignment.getProjectType().getAllowedExtensions().split(","))
                    .map(String::trim)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isEmpty())
                    .forEach(allowedExtensions::add);
        }
        return allowedExtensions;
    }

    private boolean containsExcludedSegment(String relativePath) {
        for (String segment : relativePath.split("/")) {
            if (EXCLUDED_PATH_SEGMENTS.contains(segment)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        return extension.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
    }

    private String normalizePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        return relativePath.replace('\\', '/').replaceAll("^/+", "");
    }

    public record MetadataFilterContext(
            Set<String> allowedExtensions,
            GitIgnoreMatcher gitIgnore
    ) {
    }
}
