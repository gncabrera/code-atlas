package com.code.atlas.web.service;

import com.code.atlas.web.domain.UserPreferences;
import com.code.atlas.web.repository.UserPreferencesRepository;
import com.code.atlas.web.service.dto.UserPreferencesDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferencesService {

    private static final long GLOBAL_PREFERENCES_ID = 1L;

    private final UserPreferencesRepository repository;

    public UserPreferencesService(UserPreferencesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences() {
        return toDto(resolveGlobalPreferences());
    }

    @Transactional
    public UserPreferencesDto savePreferences(UserPreferencesDto dto) {
        UserPreferences entity = resolveGlobalPreferences();
        entity.setPromptOptimizerDefaultAiModelId(dto.promptOptimizerDefaultAiModelId());
        entity.setPromptOptimizerDefaultPromptModeId(dto.promptOptimizerDefaultPromptModeId());
        entity.setPromptOptimizerDefaultContextStrategy(dto.promptOptimizerDefaultContextStrategy());
        entity.setPromptOptimizerDefaultContextAiModelId(dto.promptOptimizerDefaultContextAiModelId());
        entity.setCommitHelperDefaultAiModelId(dto.commitHelperDefaultAiModelId());
        entity.setCodeReviewDefaultAiModelId(dto.codeReviewDefaultAiModelId());
        entity.setChangelogBuilderDefaultAiModelId(dto.changelogBuilderDefaultAiModelId());
        return toDto(repository.save(entity));
    }

    private UserPreferences resolveGlobalPreferences() {
        return repository.findById(GLOBAL_PREFERENCES_ID)
                .orElseGet(() -> repository.save(new UserPreferences()));
    }

    private UserPreferencesDto toDto(UserPreferences entity) {
        return new UserPreferencesDto(
                entity.getPromptOptimizerDefaultAiModelId(),
                entity.getPromptOptimizerDefaultPromptModeId(),
                entity.getPromptOptimizerDefaultContextStrategy(),
                entity.getPromptOptimizerDefaultContextAiModelId(),
                entity.getCommitHelperDefaultAiModelId(),
                entity.getCodeReviewDefaultAiModelId(),
                entity.getChangelogBuilderDefaultAiModelId()
        );
    }
}
