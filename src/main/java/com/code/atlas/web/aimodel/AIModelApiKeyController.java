package com.code.atlas.web.aimodel;

import com.code.atlas.web.aimodel.dto.AIModelApiKeyDto;
import com.code.atlas.web.aimodel.dto.AIModelApiKeySaveDto;
import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/api-keys")
public class AIModelApiKeyController {

    private final AIModelApiKeyService aiModelApiKeyService;

    public AIModelApiKeyController(AIModelApiKeyService aiModelApiKeyService) {
        this.aiModelApiKeyService = aiModelApiKeyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getKeys(@RequestParam(defaultValue = "false") boolean activeOnly) {
        try {
            List<AIModelApiKeyDto> keys = aiModelApiKeyService.getAllKeys(activeOnly);
            return ResponseEntity.ok(ApiResponse.success("API keys fetched.", keys));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getKeyById(@PathVariable Long id) {
        try {
            AIModelApiKeyDto key = aiModelApiKeyService.getKeyById(id);
            return ResponseEntity.ok(ApiResponse.success("API key fetched.", key));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createKey(@RequestBody AIModelApiKeySaveDto saveDto) {
        try {
            AIModelApiKeyDto created = aiModelApiKeyService.createKey(saveDto);
            return new ResponseEntity<>(ApiResponse.success("API key created.", created), HttpStatus.CREATED);
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateKey(@PathVariable Long id, @RequestBody AIModelApiKeySaveDto saveDto) {
        try {
            AIModelApiKeyDto updated = aiModelApiKeyService.updateKey(id, saveDto);
            return ResponseEntity.ok(ApiResponse.success("API key updated.", updated));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteKey(@PathVariable Long id) {
        try {
            aiModelApiKeyService.deleteKey(id);
            return ResponseEntity.ok(ApiResponse.success("API key deleted.", null));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
