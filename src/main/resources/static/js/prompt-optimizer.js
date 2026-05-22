$(function () {
    const LOCAL_STORAGE_KEY = "prompt_optimizer_draft";
    const DRAFT_SAVE_DELAY_MS = 500;
    const DRAFT_STATUS_VISIBLE_MS = 2000;

    let projects = [];
    let enabledModels = [];
    let isRestoringDraft = false;
    let draftStatusHideTimeout = null;

    const promptPageControlSelectors = [
        "#projectSelect",
        "#promptModeSelect",
        "#shouldSendAgentsFile",
        "#userRequest",
        "#buildPreviewBtn",
        "#btn-clear-prompt",
        "#aiModelSelect",
        "#aiModelPrompt",
        "#sendToModelBtn",
        "#outputPrompt",
        "#copyOutputBtn"
    ];

    const draftFieldSelectors = [
        "#projectSelect",
        "#promptModeSelect",
        "#shouldSendAgentsFile",
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
            promptModeSelect: $("#promptModeSelect").val() || "BALANCED",
            shouldSendAgentsFile: $("#shouldSendAgentsFile").is(":checked"),
            userRequest: ($("#userRequest").val() || "").trim(),
            aiModelSelect: $("#aiModelSelect").val() || "",
            aiModelPrompt: ($("#aiModelPrompt").val() || "").trim(),
            outputPrompt: ($("#outputPrompt").val() || "").trim()
        };
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
            && data.promptModeSelect === "BALANCED"
            && data.shouldSendAgentsFile === true
            && String(data.aiModelSelect || "") === getDefaultAiModelSelectValue();
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
        $("#projectSelect").val("");
        $("#promptModeSelect").val("BALANCED");
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
        $("#btn-clear-prompt").on("click", clearDraft);
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

    function loadMetadata() {
        CodeAtlas.apiGet("/api/prompts/metadata")
            .done(function (response) {
                projects = response.data.projects || [];
                enabledModels = response.data.enabledModels || [];
                populateProjects();
                populateModels();
                loadDraftFromLocalStorage();
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
        const payload = {
            projectId: $("#projectSelect").val() || null,
            userRequest: userRequest,
            shouldSendAgentsFile: $("#shouldSendAgentsFile").is(":checked"),
            promptMode: $("#promptModeSelect").val()
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
        const payload = {
            projectId: $("#projectSelect").val() || null,
            aiModelId: model.id,
            aiModelPrompt: aiModelPrompt,
            shouldSendAgentsFile: $("#shouldSendAgentsFile").is(":checked"),
            promptMode: $("#promptModeSelect").val()
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

    $("#copyOutputBtn").on("click", function () {
        const output = $("#outputPrompt").val() || "";
        if (CodeAtlas.copyToClipboard(output)) {
            CodeAtlas.showToast("Output prompt copied.", "info");
            return;
        }
        CodeAtlas.showToast("Copy failed.", "danger");
    });

    bindDraftAutoSave();
    loadMetadata();
});
