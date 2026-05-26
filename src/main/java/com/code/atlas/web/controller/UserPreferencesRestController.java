package com.code.atlas.web.controller;

import com.code.atlas.web.api.ApiResponse;
import com.code.atlas.web.service.UserPreferencesService;
import com.code.atlas.web.service.dto.UserPreferencesDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-preferences")
public class UserPreferencesRestController extends BaseRestController {

    private final UserPreferencesService userPreferencesService;

    public UserPreferencesRestController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getPreferences() {
        try {
            UserPreferencesDto data = userPreferencesService.getPreferences();
            return ResponseEntity.ok(ApiResponse.success("Preferences retrieved successfully.", data));
        } catch (Exception ex) {
            return handledException("GET /api/user-preferences", ex);
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<?>> updatePreferences(@RequestBody UserPreferencesDto dto) {
        try {
            UserPreferencesDto data = userPreferencesService.savePreferences(dto);
            return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully.", data));
        } catch (Exception ex) {
            return handledException("PUT /api/user-preferences", ex);
        }
    }
}
