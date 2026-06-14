package com.code.atlas.web.service.context.indexed.engine.context.builder;

import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.ProjectProjectType;
import com.code.atlas.web.helper.IndexPathExclusions;
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

    private final ProjectProjectTypeRepository projectProjectTypeRepository;

    public MetadataFilter(ProjectProjectTypeRepository projectProjectTypeRepository) {
        this.projectProjectTypeRepository = projectProjectTypeRepository;
    }

    public MetadataFilterContext createContext(Project project) {
        List<ProjectProjectType> assignments = projectProjectTypeRepository
                .findByProjectIdOrderByProjectTypeNameAsc(project.getId());
        Set<String> allowedExtensions = new LinkedHashSet<>();
        Set<String> allowedFileNames = new LinkedHashSet<>();
        for (ProjectProjectType assignment : assignments) {
            if (assignment.getProjectType() == null) {
                continue;
            }
            addCommaSeparatedValues(allowedExtensions, assignment.getProjectType().getAllowedExtensions());
            addCommaSeparatedValues(allowedFileNames, assignment.getProjectType().getAllowedFiles());
        }
        GitIgnoreMatcher gitIgnore = GitIgnoreMatcher.fromPath(Path.of(project.getPath(), ".gitignore"));
        return new MetadataFilterContext(allowedExtensions, allowedFileNames, gitIgnore);
    }

    public boolean shouldGenerateMetadata(MetadataFilterContext context, ProjectFileIndex file) {
        if (context == null || file == null) {
            return false;
        }
        String relativePath = normalizePath(file.getFilePath());
        String extension = normalizeExtension(file.getFileExtension());
        String basename = basenameOf(relativePath);
        boolean extensionOk = !extension.isEmpty() && context.allowedExtensions().contains(extension);
        boolean fileOk = !basename.isEmpty() && context.allowedFileNames().contains(basename);
        if (!extensionOk && !fileOk) {
            return false;
        }
        if (IndexPathExclusions.containsExcludedSegment(relativePath)) {
            return false;
        }
        return !context.gitIgnore().isIgnored(relativePath);
    }

    private void addCommaSeparatedValues(Set<String> target, String rawValues) {
        if (rawValues == null || rawValues.isBlank()) {
            return;
        }
        Arrays.stream(rawValues.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .forEach(target::add);
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

    private String basenameOf(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }
        int slashIndex = relativePath.lastIndexOf('/');
        String basename = slashIndex < 0 ? relativePath : relativePath.substring(slashIndex + 1);
        return basename.trim().toLowerCase(Locale.ROOT);
    }

    public record MetadataFilterContext(
            Set<String> allowedExtensions,
            Set<String> allowedFileNames,
            GitIgnoreMatcher gitIgnore
    ) {
    }
}
