package com.code.atlas.web.service.context.indexed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.AIModel;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.service.ProjectIndexService;
import com.code.atlas.web.service.context.indexed.dto.ContextResult;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import com.code.atlas.web.service.context.indexed.dto.KnowledgeResult;
import com.code.atlas.web.service.context.indexed.engine.context.ContextEngine;
import com.code.atlas.web.service.context.indexed.engine.intent.IntentEngine;
import com.code.atlas.web.service.context.indexed.engine.knowledge.KnowlegeEngine;
import com.code.atlas.web.service.context.indexed.engine.prompt.PromptEngine;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexedContextServiceTest {

    @Mock
    private ProjectIndexService projectIndexService;

    @Mock
    private ContextPipelineLogger pipelineLogger;

    @Mock
    private ContextEngine contextEngine;

    @Mock
    private IntentEngine intentEngine;

    @Mock
    private KnowlegeEngine knowlegeEngine;

    @Mock
    private PromptEngine promptEngine;

    @InjectMocks
    private IndexedContextService indexedContextService;

    private Project project;
    private AIModel aiModel;
    private Intent intent;
    private Intent missingIntent;
    private ContextResult emptyContext;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);

        aiModel = new AIModel();
        aiModel.setId(5L);

        intent = new Intent("implement", List.of(), List.of(), List.of(), List.of(), List.of(), false, 0.9);
        missingIntent = new Intent("refine", List.of(), List.of(), List.of(), List.of(), List.of(), false, 0.8);
        emptyContext = new ContextResult(List.of());
    }

    @Test
    void build_returnsCachedContext_onSecondCall() {
        when(projectIndexService.isStale(project)).thenReturn(false);
        stubPipeline("assembled-context");

        String first = indexedContextService.build(project, "add feature", aiModel);
        String second = indexedContextService.build(project, "add feature", aiModel);

        assertEquals("assembled-context", first);
        assertEquals("assembled-context", second);
        verify(intentEngine, times(1)).extract(eq(project), eq("add feature"), eq(aiModel));
    }

    @Test
    void build_bypassesCache_whenStaleAtEntry() {
        when(projectIndexService.isStale(project)).thenReturn(true);
        stubPipeline("fresh-context");

        indexedContextService.build(project, "add feature", aiModel);
        indexedContextService.build(project, "add feature", aiModel);

        verify(intentEngine, times(2)).extract(eq(project), eq("add feature"), eq(aiModel));

        when(projectIndexService.isStale(project)).thenReturn(false);
        stubPipeline("fresh-context-again");
        indexedContextService.build(project, "add feature", aiModel);

        verify(intentEngine, times(3)).extract(eq(project), eq("add feature"), eq(aiModel));
    }

    @Test
    void build_regenerates_afterTtlExpiry() throws Exception {
        when(projectIndexService.isStale(project)).thenReturn(false);
        stubPipeline("cached-then-fresh");

        indexedContextService.build(project, "ttl test", aiModel);
        backdateCacheEntry(project, "ttl test", aiModel, System.currentTimeMillis() - (31L * 60L * 1000L));

        stubPipeline("regenerated-context");
        String result = indexedContextService.build(project, "ttl test", aiModel);

        assertEquals("regenerated-context", result);
        verify(intentEngine, times(2)).extract(eq(project), eq("ttl test"), eq(aiModel));
    }

    @Test
    void hashUserRequest_isDeterministic() {
        String first = IndexedContextService.hashUserRequest("same request");
        String second = IndexedContextService.hashUserRequest("same request");
        String different = IndexedContextService.hashUserRequest("other request");

        assertEquals(first, second);
        assertNotEquals(first, different);
    }

    @Test
    void buildCacheKey_usesProjectModelAndRequestHash() {
        IndexedContextService.CacheKey first = IndexedContextService.buildCacheKey(project, "request-a", aiModel);
        IndexedContextService.CacheKey second = IndexedContextService.buildCacheKey(project, "request-a", aiModel);
        IndexedContextService.CacheKey different = IndexedContextService.buildCacheKey(project, "request-b", aiModel);

        assertEquals(first, second);
        assertNotEquals(first, different);
    }

    private void stubPipeline(String assembledContext) {
        when(intentEngine.extract(any(), any(), any())).thenReturn(intent);
        when(contextEngine.retrieve(any(), any())).thenReturn(emptyContext);
        when(knowlegeEngine.detectMissingContext(any(), any(), any(), any(), any())).thenReturn(missingIntent);
        when(knowlegeEngine.summarize(any(), any(), any(), any(), any())).thenReturn("architecture");
        when(promptEngine.assemble(any(Intent.class), any(KnowledgeResult.class))).thenReturn(assembledContext);
    }

    @SuppressWarnings("unchecked")
    private void backdateCacheEntry(Project project, String userRequest, AIModel aiModel, long cachedAtMs)
            throws Exception {
        Field cacheField = IndexedContextService.class.getDeclaredField("contextCache");
        cacheField.setAccessible(true);
        ConcurrentHashMap<IndexedContextService.CacheKey, IndexedContextService.CacheEntry> cache =
                (ConcurrentHashMap<IndexedContextService.CacheKey, IndexedContextService.CacheEntry>) cacheField.get(indexedContextService);

        IndexedContextService.CacheKey key = IndexedContextService.buildCacheKey(project, userRequest, aiModel);
        IndexedContextService.CacheEntry entry = cache.get(key);
        cache.put(key, new IndexedContextService.CacheEntry(entry.context(), cachedAtMs));
    }
}
