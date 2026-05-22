$(function () {
    let projects = [];
    let enabledModels = [];

    function estimateTokens(content) {
        const size = content ? content.length : 0;
        return Math.ceil(size / 4);
    }

    function showAlert(message, isError) {
        const alert = $("#globalAlert");
        alert.removeClass("d-none alert-success alert-danger")
            .addClass(isError ? "alert-danger" : "alert-success")
            .text(message);
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
        $.get("/api/prompts/metadata")
            .done(function (response) {
                projects = response.data.projects || [];
                enabledModels = response.data.enabledModels || [];
                populateProjects();
                populateModels();
            })
            .fail(function (xhr) {
                const message = xhr.responseJSON?.message || "Failed loading prompt page metadata.";
                showAlert(message, true);
            });
    }

    $("#projectSelect").on("change", function () {
        updateAgentsCheckboxVisibility();
    });

    $("#aiModelSelect, #aiModelPrompt").on("change keyup", function () {
        updateTokenInfo();
    });

    $("#buildPreviewBtn").on("click", function () {
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
                const message = xhr.responseJSON?.message || "Failed building preview.";
                showAlert(message, true);
            });
    });

    $("#sendToModelBtn").on("click", function () {
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
                const message = xhr.responseJSON?.message || "Failed sending prompt to model.";
                showAlert(message, true);
            });
    });

    $("#copyOutputBtn").on("click", function () {
        const output = $("#outputPrompt").val() || "";
        navigator.clipboard.writeText(output)
            .then(function () {
                showAlert("Output prompt copied.", false);
            })
            .catch(function () {
                showAlert("Copy failed.", true);
            });
    });

    loadMetadata();
});
