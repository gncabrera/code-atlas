$(function () {
    const LOCAL_STORAGE_KEY = "prompt_optimizer_draft";
    const DRAFT_SAVE_DELAY_MS = 500;
    const DRAFT_STATUS_VISIBLE_MS = 2000;

    let projects = [];
    let enabledModels = [];
    let promptModes = [];
    let skillsById = {};
    let loadedSkills = [];
    let pendingDraftSkillIds = null;
    let isRestoringDraft = false;
    let draftStatusHideTimeout = null;

    const promptPageControlSelectors = [
        "#projectSelect",
        "#promptModeSelect",
        "#shouldSendAgentsFile",
        "#shouldSendDesignFile",
        "#userRequest",
        "#buildPreviewBtn",
        "#btn-clear-prompt",
        "#aiModelSelect",
        "#aiModelPrompt",
        "#sendToModelBtn",
        "#outputPrompt",
        "#copyOutputBtn",
        "#skillMultiselect"
    ];

    const draftFieldSelectors = [
        "#projectSelect",
        "#promptModeSelect",
        "#shouldSendAgentsFile",
        "#shouldSendDesignFile",
        "#userRequest",
        "#aiModelSelect",
        "#aiModelPrompt",
        "#outputPrompt"
    ];

    function debounce(func, delay) {
        let timeout;
        return function (...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    }

    function setPromptPageLocked(isLocked, $activeButton, loadingText) {
        promptPageControlSelectors.forEach(function (selector) {
            $(selector).prop("disabled", isLocked);
        });
        if ($activeButton) {
            CodeAtlas.setButtonLoading($activeButton, isLocked, loadingText);
        }
    }

    function estimateTokens(content) {
        const size = content ? content.length : 0;
        return Math.ceil(size / 4);
    }

    function selectedProject() {
        const id = $("#projectSelect").val();
        if (!id) {
            return null;
        }
        return projects.find(project => String(project.id) === String(id)) || null;
    }

    function selectedModel() {
        const id = $("#aiModelSelect").val();
        return enabledModels.find(model => String(model.id) === String(id)) || null;
    }

    function collectDraftData() {
        return {
            projectSelect: $("#projectSelect").val() || "",
            promptModeSelect: $("#promptModeSelect").val() || getDefaultPromptModeId(),
            shouldSendAgentsFile: $("#shouldSendAgentsFile").is(":checked"),
            userRequest: ($("#userRequest").val() || "").trim(),
            aiModelSelect: $("#aiModelSelect").val() || "",
            aiModelPrompt: ($("#aiModelPrompt").val() || "").trim(),
            outputPrompt: ($("#outputPrompt").val() || "").trim(),
            skillMultiselect: getSelectedSkillIds()
        };
    }

    function getSelectedSkillIds() {
        return ($("#skillMultiselect").val() || []).map(String);
    }

    function getDefaultSkillIds(skills) {
        return (skills || [])
            .filter(function (skill) {
                return Boolean(skill.defaultInOutputPrompt);
            })
            .map(function (skill) {
                return String(skill.id);
            });
    }

    function skillIdsEqual(left, right) {
        const a = (left || []).map(String).sort();
        const b = (right || []).map(String).sort();
        if (a.length !== b.length) {
            return false;
        }
        return a.every(function (value, index) {
            return value === b[index];
        });
    }

    function getDefaultAiModelSelectValue() {
        const $modelSelect = $("#aiModelSelect");
        if ($modelSelect.find("option").length === 0) {
            return "";
        }
        return String($modelSelect.find("option").first().val() || "");
    }

    function isDraftAtDefaults(data) {
        return !data.userRequest
            && !data.aiModelPrompt
            && !data.outputPrompt
            && !data.projectSelect
            && String(data.promptModeSelect || "") === getDefaultPromptModeId()
            && data.shouldSendAgentsFile === true
            && String(data.aiModelSelect || "") === getDefaultAiModelSelectValue()
            && skillIdsEqual(data.skillMultiselect, getDefaultSkillIds(loadedSkills));
    }

    function hideDraftStatus() {
        if (draftStatusHideTimeout) {
            clearTimeout(draftStatusHideTimeout);
            draftStatusHideTimeout = null;
        }
        $("#draft-status-container").removeClass("opacity-100").addClass("opacity-0");
    }

    function showDraftStatus(message) {
        $("#draft-status-text").text(message);
        $("#draft-status-container").removeClass("opacity-0").addClass("opacity-100");
        if (draftStatusHideTimeout) {
            clearTimeout(draftStatusHideTimeout);
        }
        draftStatusHideTimeout = setTimeout(function () {
            hideDraftStatus();
            draftStatusHideTimeout = null;
        }, DRAFT_STATUS_VISIBLE_MS);
    }

    function saveDraftToLocalStorage() {
        if (isRestoringDraft) {
            return;
        }
        const data = collectDraftData();
        try {
            if (isDraftAtDefaults(data)) {
                localStorage.removeItem(LOCAL_STORAGE_KEY);
                hideDraftStatus();
                return;
            }
            localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
            showDraftStatus("Draft Saved");
        } catch (error) {
            hideDraftStatus();
        }
    }

    const debouncedSaveDraft = debounce(saveDraftToLocalStorage, DRAFT_SAVE_DELAY_MS);

    function applyDraftField(fieldKey, value) {
        if (fieldKey === "shouldSendAgentsFile") {
            $("#shouldSendAgentsFile").prop("checked", Boolean(value));
            return true;
        }
        if (fieldKey === "skillMultiselect") {
            applySkillMultiselectSelection(Array.isArray(value) ? value.map(String) : []);
            return true;
        }
        const $field = $("#" + fieldKey);
        if (!$field.length) {
            return false;
        }
        if (fieldKey === "aiModelSelect") {
            if (value && $field.find('option[value="' + value + '"]').length === 0) {
                return false;
            }
            $field.val(value || "");
            return true;
        }
        if (fieldKey === "promptModeSelect") {
            if (!/^\d+$/.test(String(value || ""))) {
                return false;
            }
            if ($field.find('option[value="' + value + '"]').length === 0) {
                return false;
            }
            $field.val(String(value));
            return true;
        }
        $field.val(value ?? "");
        return true;
    }

    function loadDraftFromLocalStorage() {
        let rawDraft;
        try {
            rawDraft = localStorage.getItem(LOCAL_STORAGE_KEY);
        } catch (error) {
            return false;
        }
        if (!rawDraft) {
            return false;
        }
        let data;
        try {
            data = JSON.parse(rawDraft);
        } catch (error) {
            try {
                localStorage.removeItem(LOCAL_STORAGE_KEY);
            } catch (removeError) {
                // ignore storage errors
            }
            return false;
        }
        if (!data || typeof data !== "object") {
            return false;
        }
        pendingDraftSkillIds = Array.isArray(data.skillMultiselect)
            ? data.skillMultiselect.map(String)
            : null;
        isRestoringDraft = true;
        let restored = false;
        const hadContent = !isDraftAtDefaults(data);
        Object.keys(data).forEach(function (fieldKey) {
            applyDraftField(fieldKey, data[fieldKey]);
        });
        restored = hadContent;
        isRestoringDraft = false;
        updateAgentsCheckboxVisibility();
        updateTokenInfo();
        if (restored) {
            showDraftStatus("Draft Restored");
        }
        return restored;
    }

    function resetPromptPageToDefaults() {
        $("#shouldSendAgentsFile").prop("checked", true);
        $("#userRequest").val("");
        $("#aiModelPrompt").val("");
        $("#outputPrompt").val("");
        const $modelSelect = $("#aiModelSelect");
        if ($modelSelect.find("option").length > 0) {
            $modelSelect.prop("selectedIndex", 0);
        } else {
            $modelSelect.val("");
        }
        applySkillMultiselectSelection(getDefaultSkillIds(loadedSkills));
        updateAgentsCheckboxVisibility();
        updateTokenInfo();
    }

    function clearDraft() {
        resetPromptPageToDefaults();
        try {
            localStorage.removeItem(LOCAL_STORAGE_KEY);
        } catch (error) {
            // ignore storage errors
        }
        hideDraftStatus();
    }

    function bindDraftAutoSave() {
        $("#userRequest, #aiModelPrompt, #outputPrompt").on("input", debouncedSaveDraft);
        $("#projectSelect, #promptModeSelect, #aiModelSelect").on("change", debouncedSaveDraft);
        $("#shouldSendAgentsFile").on("change", debouncedSaveDraft);
        $("#skillMultiselect").on("change", debouncedSaveDraft);
        $("#btn-clear-prompt").on("click", clearDraft);
    }

    function applySkillMultiselectSelection(selectedIds) {
        const $select = $("#skillMultiselect");
        if (!$select.length || !$select.find("option").length) {
            return;
        }
        $select.val(selectedIds || []);
        if ($select.data("multiselect")) {
            $select.multiselect("refresh");
        }
    }

    function updateAgentsCheckboxVisibility() {
        const project = selectedProject();
        if (project && !project.useAgentsFile) {
            $("#agentsCheckboxWrapper").addClass("d-none");
            $("#shouldSendAgentsFile").prop("checked", false);
            return;
        }
        $("#agentsCheckboxWrapper").removeClass("d-none");
    }

    function updateTokenInfo() {
        const model = selectedModel();
        const tokens = estimateTokens($("#aiModelPrompt").val());
        const tokenLimit = model
            ? (model.tokensPerMinute === 0 ? "unlimited" : model.tokensPerMinute)
            : 0;
        $("#estimatedTokens").text(tokens);
        $("#tokenLimit").text(tokenLimit);
    }

    function populateProjects() {
        const select = $("#projectSelect");
        select.empty().append($("<option>", {value: "", text: "No project"}));
        projects.forEach(project => {
            select.append($("<option>", {value: project.id, text: project.name}));
        });
        updateAgentsCheckboxVisibility();
    }

    function populateModels() {
        const select = $("#aiModelSelect");
        select.empty();
        enabledModels.forEach(model => {
            select.append($("<option>", {value: model.id, text: model.name}));
        });
        updateTokenInfo();
    }

    function getDefaultPromptModeId() {
        const balanced = promptModes.find(function (mode) {
            return mode.code === "BALANCED";
        });
        if (balanced) {
            return String(balanced.id);
        }
        return promptModes.length > 0 ? String(promptModes[0].id) : "";
    }

    function populatePromptModes() {
        const select = $("#promptModeSelect");
        select.empty();
        if (!promptModes.length) {
            select.append($("<option>", {value: "", text: "No modes available"}));
            return;
        }
        promptModes.forEach(function (mode) {
            select.append($("<option>", {value: mode.id, text: mode.name}));
        });
        select.val(getDefaultPromptModeId());
    }

    function loadMetadata() {
        CodeAtlas.apiGet("/api/prompts/metadata")
            .done(function (response) {
                projects = response.data.projects || [];
                enabledModels = response.data.enabledModels || [];
                promptModes = response.data.promptModes || [];
                populateProjects();
                populateModels();
                populatePromptModes();
                loadDraftFromLocalStorage();
                loadSkills();
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(
                    CodeAtlas.apiMessage(xhr, "Failed loading prompt page metadata."),
                    "danger"
                );
            });
    }

    $("#projectSelect").on("change", function () {
        updateAgentsCheckboxVisibility();
    });

    $("#aiModelSelect, #aiModelPrompt").on("change keyup", function () {
        updateTokenInfo();
    });

    $("#buildPreviewBtn").on("click", function () {
        const $buildBtn = $(this);
        const userRequest = $("#userRequest").val();
        if (!userRequest || !userRequest.trim()) {
            CodeAtlas.showToast("User request is required.", "danger");
            return;
        }
        const promptModeId = $("#promptModeSelect").val();
        if (!promptModeId) {
            CodeAtlas.showToast("Select a prompt mode.", "danger");
            return;
        }
        const payload = {
            projectId: $("#projectSelect").val() || null,
            userRequest: userRequest,
            shouldSendAgentsFile: $("#shouldSendAgentsFile").is(":checked"),
            promptModeId: Number(promptModeId)
        };
        CodeAtlas.setButtonLoading($buildBtn, true, "Building Preview...");
        $.ajax({
            url: "/api/prompts/build-preview",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(payload)
        })
            .done(function (response) {
                $("#aiModelPrompt").val(response.data.aiModelPrompt);
                updateTokenInfo();
                debouncedSaveDraft();
                CodeAtlas.showToast(response.message || "Preview built.", "success");
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed building preview."), "danger");
            })
            .always(function () {
                CodeAtlas.setButtonLoading($buildBtn, false);
            });
    });

    $("#sendToModelBtn").on("click", function () {
        const $sendBtn = $(this);
        const model = selectedModel();
        if (!model) {
            CodeAtlas.showToast("Select an enabled AI model.", "danger");
            return;
        }
        const aiModelPrompt = $("#aiModelPrompt").val();
        if (!aiModelPrompt || !aiModelPrompt.trim()) {
            CodeAtlas.showToast("AIModelPrompt is required.", "danger");
            return;
        }
        const estimatedTokens = estimateTokens(aiModelPrompt);
        if (model.tokensPerMinute > 0 && estimatedTokens > model.tokensPerMinute) {
            CodeAtlas.showToast("Cannot send. Estimated tokens exceed tokensPerMinute.", "danger");
            return;
        }
        const confirmed = window.confirm("Send prompt exactly as written to selected AI model?");
        if (!confirmed) {
            return;
        }
        const promptModeId = $("#promptModeSelect").val();
        const payload = {
            projectId: $("#projectSelect").val() || null,
            aiModelId: model.id,
            aiModelPrompt: aiModelPrompt,
            shouldSendAgentsFile: $("#shouldSendAgentsFile").is(":checked"),
            promptModeId: promptModeId ? Number(promptModeId) : null
        };
        setPromptPageLocked(true, $sendBtn, "Sending...");
        $.ajax({
            url: "/api/prompts/send",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify(payload)
        })
            .done(function (response) {
                $("#outputPrompt").val(response.data.outputPrompt);
                debouncedSaveDraft();
                CodeAtlas.showToast(response.message || "Prompt sent.", "success");
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed sending prompt to model."), "danger");
            })
            .always(function () {
                setPromptPageLocked(false, $sendBtn);
            });
    });

    function uncategorizedCategory(category) {
        const normalized = (category || "").trim();
        return normalized || "(Uncategorized)";
    }

    function populateSkillMultiselect(skills, selectedIds) {
        const $select = $("#skillMultiselect");
        loadedSkills = skills || [];
        skillsById = {};
        if ($select.data("multiselect")) {
            $select.multiselect("destroy");
        }
        $select.empty();
        const initialSelection = selectedIds !== undefined && selectedIds !== null
            ? selectedIds.map(String)
            : getDefaultSkillIds(loadedSkills);
        if (!skills || skills.length === 0) {
            $select.multiselect({
                enableFiltering: true,
                includeSelectAllOption: true,
                buttonWidth: "20%",
                nonSelectedText: "No skills available",
                onChange: debouncedSaveDraft
            });
            return;
        }
        skills.forEach(function (skill) {
            skillsById[String(skill.id)] = skill;
        });
        const grouped = {};
        skills.forEach(function (skill) {
            const category = uncategorizedCategory(skill.category);
            if (!grouped[category]) {
                grouped[category] = [];
            }
            grouped[category].push(skill);
        });
        Object.keys(grouped).sort(function (a, b) {
            return a.localeCompare(b);
        }).forEach(function (category) {
            const $optgroup = $("<optgroup>").attr("label", category);
            grouped[category].sort(function (a, b) {
                return a.name.localeCompare(b.name);
            }).forEach(function (skill) {
                const optionId = String(skill.id);
                $optgroup.append(
                    $("<option>", {
                        value: optionId,
                        text: skill.name,
                        selected: initialSelection.indexOf(optionId) >= 0
                    })
                );
            });
            $select.append($optgroup);
        });
        $select.multiselect({
            enableFiltering: true,
            includeSelectAllOption: true,
            buttonWidth: "20%",
            nonSelectedText: "Select skills to append",
            onChange: debouncedSaveDraft
        });
    }

    function loadSkills() {
        CodeAtlas.apiGet("/api/skills")
            .done(function (response) {
                if (response.result !== "success") {
                    CodeAtlas.showToast(
                        response.message || "Failed loading skills.",
                        "danger"
                    );
                    populateSkillMultiselect([]);
                    return;
                }
                const skills = response.data || [];
                const selection = pendingDraftSkillIds !== null
                    ? pendingDraftSkillIds
                    : getDefaultSkillIds(skills);
                populateSkillMultiselect(skills, selection);
                pendingDraftSkillIds = null;
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(
                    CodeAtlas.apiMessage(xhr, "Failed loading skills."),
                    "danger"
                );
                populateSkillMultiselect([]);
            });
    }

    function buildCopyPayload() {
        let output = $("#outputPrompt").val() || "";
        const selectedIds = $("#skillMultiselect").val() || [];
        if (selectedIds.length === 0) {
            return output;
        }
        const promptBlocks = selectedIds
            .map(function (id) {
                const skill = skillsById[String(id)];
                return skill && skill.prompt ? skill.prompt.trim() : "";
            })
            .filter(function (prompt) {
                return prompt.length > 0;
            });
        if (promptBlocks.length === 0) {
            return output;
        }
        return output + "\n\n--- Skills ---\n\n" + promptBlocks.join("\n\n");
    }

    $("#copyOutputBtn").on("click", function () {
        const $copyBtn = $(this);
        const output = buildCopyPayload();
        if (!output.trim()) {
            CodeAtlas.showToast("Nothing to copy.", "danger");
            return;
        }
        CodeAtlas.setButtonLoading($copyBtn, true, "Copying...");
        if (CodeAtlas.copyToClipboard(output)) {
            CodeAtlas.showToast("Output prompt copied.", "info");
        } else {
            CodeAtlas.showToast("Copy failed.", "danger");
        }
        CodeAtlas.setButtonLoading($copyBtn, false);
    });

    bindDraftAutoSave();
    loadMetadata();
});
