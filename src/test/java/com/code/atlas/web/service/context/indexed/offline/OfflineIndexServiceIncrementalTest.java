package com.code.atlas.web.service.context.indexed.offline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.code.atlas.web.domain.FileSummaryIndexEntry;
import com.code.atlas.web.domain.ProjectFileIndex;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OfflineIndexServiceIncrementalTest {

    @Test
    void selectFilesNeedingSummary_includesNewAndChangedFilesOnly() {
        ProjectFileIndex unchanged = fileIndex("src/A.java", "hash-a");
        ProjectFileIndex changed = fileIndex("src/B.java", "hash-b-new");
        ProjectFileIndex added = fileIndex("src/C.java", "hash-c");

        FileSummaryIndexEntry summaryA = summary("src/A.java", "hash-a");
        FileSummaryIndexEntry summaryB = summary("src/B.java", "hash-b-old");

        List<ProjectFileIndex> selected = OfflineIndexService.selectFilesNeedingSummary(
                List.of(unchanged, changed, added),
                Map.of(
                        summaryA.getFilePath(), summaryA,
                        summaryB.getFilePath(), summaryB
                )
        );

        assertEquals(2, selected.size());
        assertTrue(selected.stream().anyMatch(file -> "src/B.java".equals(file.getFilePath())));
        assertTrue(selected.stream().anyMatch(file -> "src/C.java".equals(file.getFilePath())));
    }

    private static ProjectFileIndex fileIndex(String path, String hash) {
        ProjectFileIndex entry = new ProjectFileIndex();
        entry.setFilePath(path);
        entry.setContentHash(hash);
        return entry;
    }

    private static FileSummaryIndexEntry summary(String path, String hash) {
        FileSummaryIndexEntry entry = new FileSummaryIndexEntry();
        entry.setFilePath(path);
        entry.setContentHash(hash);
        return entry;
    }
}
