$(function () {
    let projects = [];
    let enabledModels = [];

    const promptPageControlSelectors = [
        "#projectSelect",
        "#promptModeSelect",
        "#shouldSendAgentsFile",
        "#userRequest",
        "#buildPreviewBtn",
        "#aiModelSelect",
        "#aiModelPrompt",
        "#sendToModelBtn",
        "#outputPrompt",
        "#copyOutputBtn"
    ];

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

    function showAlert(message, isError) {
        CodeAtlas.showAlert("#globalAlert", message, isError);
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
        const tokenLimit = model ? model.tokensPerMinute : 0;
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
            })
            .fail(function (xhr) {
                showAlert(CodeAtlas.apiMessage(xhr, "Failed loading prompt page metadata."), true);
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
            showAlert("User request is required.", true);
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
                showAlert(response.message || "Preview built.", false);
            })
            .fail(function (xhr) {
                showAlert(CodeAtlas.apiMessage(xhr, "Failed building preview."), true);
            })
            .always(function () {
                CodeAtlas.setButtonLoading($buildBtn, false);
            });
    });

    $("#sendToModelBtn").on("click", function () {
        const $sendBtn = $(this);
        const model = selectedModel();
        if (!model) {
            showAlert("Select an enabled AI model.", true);
            return;
        }
        const aiModelPrompt = $("#aiModelPrompt").val();
        if (!aiModelPrompt || !aiModelPrompt.trim()) {
            showAlert("AIModelPrompt is required.", true);
            return;
        }
        const estimatedTokens = estimateTokens(aiModelPrompt);
        if (estimatedTokens > model.tokensPerMinute) {
            showAlert("Cannot send. Estimated tokens exceed tokensPerMinute.", true);
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
                showAlert(response.message || "Prompt sent.", false);
            })
            .fail(function (xhr) {
                showAlert(CodeAtlas.apiMessage(xhr, "Failed sending prompt to model."), true);
            })
            .always(function () {
                setPromptPageLocked(false, $sendBtn);
            });
    });

    $("#copyOutputBtn").on("click", function () {
        const output = $("#outputPrompt").val() || "";
        if (CodeAtlas.copyToClipboard(output)) {
            showAlert("Output prompt copied.", false);
            return;
        }
        showAlert("Copy failed.", true);
    });

    loadMetadata();
});
