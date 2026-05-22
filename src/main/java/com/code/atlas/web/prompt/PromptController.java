package com.code.atlas.web.prompt;

import com.code.atlas.web.aimodel.AIModelService;
import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.api.GlobalExceptionHandler;
import com.code.atlas.web.project.ProjectService;
import com.code.atlas.web.prompt.dto.BuildPreviewRequestDto;
import com.code.atlas.web.prompt.dto.BuildPreviewResponseDto;
import com.code.atlas.web.prompt.dto.PromptPageMetadataDto;
import com.code.atlas.web.prompt.dto.SendPromptRequestDto;
import com.code.atlas.web.prompt.dto.SendPromptResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;
    private final ProjectService projectService;
    private final AIModelService aiModelService;

    public PromptController(PromptService promptService, ProjectService projectService, AIModelService aiModelService) {
        this.promptService = promptService;
        this.projectService = projectService;
        this.aiModelService = aiModelService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<?>> getPageMetadata() {
        try {
            PromptPageMetadataDto metadata = new PromptPageMetadataDto(
                    projectService.getAllProjects(),
                    aiModelService.getEnabledModels()
            );
            return ResponseEntity.ok(ApiResponse.success("Prompt metadata fetched.", metadata));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/build-preview")
    public ResponseEntity<ApiResponse<?>> buildPreview(@RequestBody BuildPreviewRequestDto requestDto) {
        try {
            BuildPreviewResponseDto responseDto = promptService.buildPreview(requestDto);
            return ResponseEntity.ok(ApiResponse.success("Prompt preview built.", responseDto));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<?>> sendToModel(@RequestBody SendPromptRequestDto requestDto) {
        try {
            SendPromptResponseDto responseDto = promptService.sendToModel(requestDto);
            return ResponseEntity.ok(ApiResponse.success("Prompt sent to AI model.", responseDto));
        } catch (Exception ex) {
            return GlobalExceptionHandler.errorResponseEntity(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
