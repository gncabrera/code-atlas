(function (global) {
    const PREFERENCES_API = "/api/user-preferences";
    const OBSERVER_TIMEOUT_MS = 4000;

    const preferences = {
        promptOptimizerDefaultAiModelId: 0,
        promptOptimizerDefaultPromptModeId: 0,
        commitHelperDefaultAiModelId: 0,
        codeReviewDefaultAiModelId: 0
    };

    let preferencesLoaded = false;
    let loadPromise = null;

    const pageBindings = {
        "prompt-optimizer": [
            { field: "promptOptimizerDefaultPromptModeId", selectId: "promptModeSelect" },
            { field: "promptOptimizerDefaultAiModelId", selectId: "aiModelSelect" }
        ],
        "commit-helper": [
            { field: "commitHelperDefaultAiModelId", selectId: "aiModelSelect" }
        ],
        "code-review": [
            { field: "codeReviewDefaultAiModelId", selectId: "modelSelect" }
        ]
    };

    function currentPageKey() {
        const path = global.location.pathname || "";
        if (path.includes("prompt-optimizer")) {
            return "prompt-optimizer";
        }
        if (path.includes("commit-helper")) {
            return "commit-helper";
        }
        if (path.includes("code-review")) {
            return "code-review";
        }
        return "";
    }

    function bindingsForCurrentPage() {
        return pageBindings[currentPageKey()] || [];
    }

    function fetchPreferences() {
        if (loadPromise) {
            return loadPromise;
        }
        loadPromise = CodeAtlas.apiGet(PREFERENCES_API)
            .done(function (response) {
                if (response && response.data) {
                    Object.assign(preferences, response.data);
                }
                preferencesLoaded = true;
            })
            .fail(function () {
                preferencesLoaded = true;
            });
        return loadPromise;
    }

    function savePreferences() {
        return $.ajax({
            url: PREFERENCES_API,
            method: "PUT",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify(preferences)
        });
    }

    function hasSelectableOptions($select) {
        return $select.find("option").length > 1;
    }

    function applySelectValue($select, targetValue) {
        const numericValue = parseInt(String(targetValue || 0), 10) || 0;
        if (numericValue > 0) {
            const value = String(numericValue);
            if ($select.find('option[value="' + value + '"]').length > 0) {
                $select.val(value);
            } else if ($select.find("option").length > 0) {
                $select.prop("selectedIndex", 0);
            }
        } else if ($select.find("option").length > 0) {
            $select.prop("selectedIndex", 0);
        }
        $select.trigger("change");
    }

    function selectValueSafely(selectId, targetValue) {
        const $select = $("#" + selectId);
        if ($select.length === 0) {
            return;
        }

        const apply = function () {
            applySelectValue($select, targetValue);
            refreshDefaultIndicators();
        };

        if (hasSelectableOptions($select)) {
            apply();
            return;
        }

        const selectElement = $select[0];
        const observer = new MutationObserver(function () {
            if (hasSelectableOptions($select)) {
                apply();
                observer.disconnect();
            }
        });
        observer.observe(selectElement, { childList: true });
        global.setTimeout(function () {
            observer.disconnect();
        }, OBSERVER_TIMEOUT_MS);
    }

    function applyPreferenceFields(fieldBindings) {
        (fieldBindings || []).forEach(function (binding) {
            const targetValue = preferences[binding.field];
            if (targetValue === undefined) {
                return;
            }
            selectValueSafely(binding.selectId, targetValue);
        });
    }

    function applyForCurrentPage() {
        applyPreferenceFields(bindingsForCurrentPage());
    }

    function isCurrentSelectionDefault($select, fieldName) {
        const savedValue = parseInt(String(preferences[fieldName] || 0), 10) || 0;
        const currentValue = parseInt(String($select.val() || 0), 10) || 0;
        return savedValue > 0 && savedValue === currentValue;
    }

    function refreshDefaultIndicators() {
        bindingsForCurrentPage().forEach(function (binding) {
            const $select = $("#" + binding.selectId);
            if ($select.length === 0) {
                return;
            }
            const $label = findLabelForSelect(binding.selectId, $select);
            if ($label.length === 0) {
                return;
            }
            const $link = $label.find(".set-default-btn");
            if ($link.length === 0) {
                return;
            }
            updateLinkVisualState($link, $select, binding.field);
        });
    }

    function updateLinkVisualState($link, $select, fieldName) {
        const isDefault = isCurrentSelectionDefault($select, fieldName);
        const $icon = $link.find("i");
        if (isDefault) {
            $link.addClass("text-primary").removeClass("text-muted");
            $icon.removeClass("bi-pin-angle").addClass("bi-pin-angle-fill");
            $link.attr("title", "Current default");
        } else {
            $link.addClass("text-muted").removeClass("text-primary");
            $icon.removeClass("bi-pin-angle-fill").addClass("bi-pin-angle");
            $link.attr("title", "Set as default");
        }
    }

    function findLabelForSelect(selectId, $select) {
        const $forLabel = $('label[for="' + selectId + '"]');
        if ($forLabel.length > 0) {
            return $forLabel;
        }
        const $parentLabel = $select.closest(".form-group, .mb-3").find("label").first();
        return $parentLabel.length > 0 ? $parentLabel : $();
    }

    function injectSetDefaultLink(binding) {
        const $select = $("#" + binding.selectId);
        if ($select.length === 0) {
            return;
        }

        const $label = findLabelForSelect(binding.selectId, $select);
        if ($label.length === 0) {
            return;
        }
        if ($label.find(".set-default-btn").length > 0) {
            refreshDefaultIndicators();
            return;
        }

        const $link = $('<a href="#" class="set-default-btn text-muted ms-2 small text-decoration-none"></a>');
        $link.css("fontSize", "11px");
        $link.attr("title", "Set as default");
        $link.html('<i class="bi bi-pin-angle"></i> Set as default');

        $link.on("click", function (event) {
            event.preventDefault();
            const selectedValue = parseInt(String($select.val() || 0), 10) || 0;
            preferences[binding.field] = selectedValue;

            savePreferences()
                .done(function (response) {
                    if (String(response.result || "").toLowerCase() === "success") {
                        if (response.data) {
                            Object.assign(preferences, response.data);
                        }
                        CodeAtlas.showToast("Preference updated successfully!", "success");
                        refreshDefaultIndicators();
                        return;
                    }
                    CodeAtlas.showToast(response.message || "Failed to update preference.", "danger");
                })
                .fail(function (xhr) {
                    CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to save default choice."), "danger");
                });
        });

        $select.on("change.userPreferences", function () {
            updateLinkVisualState($link, $select, binding.field);
        });

        $label.append($link);
        updateLinkVisualState($link, $select, binding.field);
    }

    function setupSetAsDefaultLinks() {
        bindingsForCurrentPage().forEach(function (binding) {
            injectSetDefaultLink(binding);
        });
    }

    function setupSetAsDefaultLinksWhenReady() {
        bindingsForCurrentPage().forEach(function (binding) {
            const $select = $("#" + binding.selectId);
            if ($select.length === 0) {
                return;
            }
            if ($select.find("option").length > 0) {
                injectSetDefaultLink(binding);
                return;
            }
            const selectElement = $select[0];
            const observer = new MutationObserver(function () {
                if ($select.find("option").length > 0) {
                    injectSetDefaultLink(binding);
                    observer.disconnect();
                }
            });
            observer.observe(selectElement, { childList: true });
            global.setTimeout(function () {
                observer.disconnect();
            }, OBSERVER_TIMEOUT_MS);
        });
    }

    global.CodeAtlasUserPreferences = {
        getPreferences: function () {
            return Object.assign({}, preferences);
        },
        isLoaded: function () {
            return preferencesLoaded;
        },
        whenLoaded: function () {
            return fetchPreferences();
        },
        applyPreferenceFields: applyPreferenceFields,
        applyForCurrentPage: applyForCurrentPage,
        refreshDefaultIndicators: refreshDefaultIndicators
    };

    global.userPreferences = preferences;

    $(function () {
        fetchPreferences().always(function () {
            setupSetAsDefaultLinksWhenReady();
            refreshDefaultIndicators();
        });
    });
})(window);
