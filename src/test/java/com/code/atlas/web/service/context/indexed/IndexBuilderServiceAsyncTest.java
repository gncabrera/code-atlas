package com.code.atlas.web.service.context.indexed;

import com.code.atlas.web.service.AIModelService;
import com.code.atlas.web.service.ProjectService;
import com.code.atlas.web.service.context.indexed.engine.context.builder.FileSummariesIndexService;
import com.code.atlas.web.service.dto.OfflineIndexJobMode;
import com.code.atlas.web.service.dto.OfflineIndexJobResponseDto;
import com.code.atlas.web.service.dto.OfflineIndexJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class IndexBuilderServiceAsyncTest {

    @Mock
    private ContextPipelineLogger pipelineLogger;

    @Mock
    private FileSummariesIndexService fileSummariesIndexService;

    @Mock
    private ProjectService projectService;

    @Mock
    private AIModelService aiModelService;

    private IndexBuilderService indexBuilderService;

    @BeforeEach
    void setUp() {
        indexBuilderService = spy(new IndexBuilderService(
                pipelineLogger,
                null,
                fileSummariesIndexService,
                projectService,
                aiModelService
        ));
        ReflectionTestUtils.setField(indexBuilderService, "self", indexBuilderService);
        doNothing().when(indexBuilderService).executeRegenerateAsync(anyLong(), anyLong(), any());
    }

    @Test
    void startRegenerateAsyncReturnsRunningJob() {
        OfflineIndexJobResponseDto job = indexBuilderService.startRegenerateAsync(1L, 2L);

        assertEquals(1L, job.projectId());
        assertEquals(OfflineIndexJobMode.FULL, job.mode());
        assertEquals(OfflineIndexJobStatus.RUNNING, job.status());
    }

    @Test
    void startRegenerateAsyncRejectsConcurrentJobForSameProject() {
        indexBuilderService.startRegenerateAsync(1L, 2L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> indexBuilderService.startRegenerateIncrementalAsync(1L, 2L)
        );

        assertEquals("Offline index job already running for this project.", ex.getMessage());
    }

    @Test
    void getJobStatusReturnsLatestRunningJob() {
        indexBuilderService.startRegenerateIncrementalAsync(1L, 2L);

        OfflineIndexJobResponseDto status = indexBuilderService.getJobStatus(1L);

        assertEquals(OfflineIndexJobMode.INCREMENTAL, status.mode());
        assertEquals(OfflineIndexJobStatus.RUNNING, status.status());
    }
}
