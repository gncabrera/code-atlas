package com.code.atlas.web.aimodel;

import com.code.atlas.web.aimodel.dto.AIModelApiKeyDto;
import com.code.atlas.web.aimodel.dto.AIModelApiKeySaveDto;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AIModelApiKeyService {

    private final AIModelApiKeyRepository aiModelApiKeyRepository;
    private final AIModelRepository aiModelRepository;

    public AIModelApiKeyService(
            AIModelApiKeyRepository aiModelApiKeyRepository,
            AIModelRepository aiModelRepository
    ) {
        this.aiModelApiKeyRepository = aiModelApiKeyRepository;
        this.aiModelRepository = aiModelRepository;
    }

    public List<AIModelApiKeyDto> getAllKeys(boolean activeOnly) {
        List<AIModelApiKey> keys = activeOnly
                ? aiModelApiKeyRepository.findByIsActiveTrueOrderByNameAsc()
                : aiModelApiKeyRepository.findAll();
        return keys.stream().map(this::toDto).toList();
    }

    public AIModelApiKeyDto getKeyById(Long id) {
        AIModelApiKey key = aiModelApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found for id: " + id));
        return toDto(key);
    }

    @Transactional
    public AIModelApiKeyDto createKey(AIModelApiKeySaveDto saveDto) {
        AIModelApiKey key = new AIModelApiKey();
        applySaveDto(key, saveDto);
        return toDto(aiModelApiKeyRepository.save(key));
    }

    @Transactional
    public AIModelApiKeyDto updateKey(Long id, AIModelApiKeySaveDto saveDto) {
        AIModelApiKey key = aiModelApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found for id: " + id));
        applySaveDto(key, saveDto);
        return toDto(aiModelApiKeyRepository.save(key));
    }

    @Transactional
    public void deleteKey(Long id) {
        AIModelApiKey key = aiModelApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found for id: " + id));
        if (aiModelRepository.countByAiModelApiKey_Id(id) > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete API key: it is still assigned to one or more AI models."
            );
        }
        aiModelApiKeyRepository.delete(key);
    }

    public AIModelApiKey getKeyEntity(Long id) {
        return aiModelApiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found for id: " + id));
    }

    private void applySaveDto(AIModelApiKey key, AIModelApiKeySaveDto saveDto) {
        key.setName(saveDto.name().trim());
        key.setApiKey(saveDto.apiKey().trim());
        key.setProvider(saveDto.provider().trim());
        key.setActive(saveDto.isActive());
    }

    private AIModelApiKeyDto toDto(AIModelApiKey key) {
        return new AIModelApiKeyDto(
                key.getId(),
                key.getName(),
                key.getApiKey(),
                key.getProvider(),
                key.isActive()
        );
    }
}
