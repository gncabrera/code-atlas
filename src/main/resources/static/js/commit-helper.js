$(function () {
    let projects = [];
    let enabledModels = [];
    let currentBranch = "";

    const $projectSelect = $("#projectSelect");
    const $aiModelSelect = $("#aiModelSelect");
    const $commitMessageTextArea = $("#commitMessageTextArea");
    const $generateCommitBtn = $("#generateCommitBtn");
    const $copyCommitBtn = $("#copyCommitBtn");
    const $commitBtn = $("#commitBtn");
    const $commitPushBtn = $("#commitPushBtn");
    const $clearCommitBtn = $("#clearCommitBtn");
    const $currentBranchBadge = $("#currentBranchBadge");
    const $autoCommitCheckbox = $("#autoCommitCheckbox");

    function populateProjects() {
        $projectSelect.empty().append($("<option>", { value: "", text: "Select project" }));
        projects.forEach(function (project) {
            $projectSelect.append($("<option>", { value: project.id, text: project.name }));
        });
    }

    function populateModels() {
        $aiModelSelect.empty().append($("<option>", { value: "", text: "Select AI model" }));
        enabledModels.forEach(function (model) {
            $aiModelSelect.append($("<option>", { value: model.id, text: model.name }));
        });
    }

    function updateProjectDependentState() {
        const projectId = selectedProjectId();
        if (!projectId) {
            currentBranch = "";
            $currentBranchBadge.addClass("d-none").text("");
            $autoCommitCheckbox.prop("disabled", true).prop("checked", false);
            return;
        }

        $autoCommitCheckbox.prop("disabled", false);
        loadProjectBranch(projectId);
    }

    function loadProjectBranch(projectId) {
        CodeAtlas.apiGet("/api/commit-helper/metadata?projectId=" + projectId)
            .done(function (response) {
                currentBranch = response.data.currentBranch || "";
                if (currentBranch) {
                    $currentBranchBadge.removeClass("d-none").text(currentBranch);
                } else {
                    $currentBranchBadge.addClass("d-none").text("");
                }
            })
            .fail(function (xhr) {
                currentBranch = "";
                $currentBranchBadge.addClass("d-none").text("");
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load project branch."), "danger");
            });
    }

    function loadMetadata() {
        CodeAtlas.apiGet("/api/commit-helper/metadata")
            .done(function (response) {
                projects = response.data.projects || [];
                enabledModels = response.data.enabledModels || [];
                populateProjects();
                populateModels();
                const applyStoredPreferences = function () {
                    if (window.CodeAtlasUserPreferences) {
                        CodeAtlasUserPreferences.applyPreferenceFields([
                            { field: "commitHelperDefaultAiModelId", selectId: "aiModelSelect" }
                        ]);
                    }
                    updateProjectDependentState();
                };
                if (window.CodeAtlasUserPreferences) {
                    CodeAtlasUserPreferences.whenLoaded().always(applyStoredPreferences);
                } else {
                    applyStoredPreferences();
                }
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load commit helper metadata."), "danger");
            });
    }

    function selectedProjectId() {
        const projectId = $projectSelect.val();
        if (!projectId) {
            return null;
        }
        return Number(projectId);
    }

    function selectedModelId() {
        const modelId = $aiModelSelect.val();
        if (!modelId) {
            return null;
        }
        return Number(modelId);
    }

    function validateGenerateSelection() {
        if (!selectedProjectId()) {
            CodeAtlas.showToast("Select a project.", "warning");
            return false;
        }
        if (!selectedModelId()) {
            CodeAtlas.showToast("Select an AI model.", "warning");
            return false;
        }
        return true;
    }

    function validateCommitMessage() {
        const message = ($commitMessageTextArea.val() || "").trim();
        if (!message) {
            CodeAtlas.showToast("Commit message is required.", "warning");
            return null;
        }
        return message;
    }

    function validateCommitSelection() {
        if (!selectedProjectId()) {
            CodeAtlas.showToast("Select a project.", "warning");
            return false;
        }
        return true;
    }

    function executeAutocommitPush(message) {
        if (!message) {
            CodeAtlas.showToast("Commit message empty; autocommit skipped.", "warning");
            CodeAtlas.setButtonLoading($generateCommitBtn, false);
            return;
        }

        CodeAtlas.setButtonLoading($generateCommitBtn, true, "Committing...");
        $.ajax({
            url: "/api/commit-helper/push",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({
                projectId: selectedProjectId(),
                commitMessage: message
            })
        })
            .done(function (response) {
                CodeAtlas.showToast(response.message || "Changes committed and pushed.", "success");
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to commit and push changes."), "danger");
            })
            .always(function () {
                CodeAtlas.setButtonLoading($generateCommitBtn, false);
            });
    }

    $projectSelect.on("change", updateProjectDependentState);

    $autoCommitCheckbox.on("change", function () {
        if ($autoCommitCheckbox.is(":checked") && currentBranch) {
            CodeAtlas.showToast(
                "After generating will commit and push to the branch " + currentBranch,
                "warning"
            );
        }
    });

    $generateCommitBtn.on("click", function () {
        if (!validateGenerateSelection()) {
            return;
        }

        CodeAtlas.setButtonLoading($generateCommitBtn, true, "Processing...");
        $.ajax({
            url: "/api/commit-helper/generate",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({
                projectId: selectedProjectId(),
                aiModelId: selectedModelId()
            })
        })
            .done(function (response) {
                const message = (response.data || "").trim();
                $commitMessageTextArea.val(message);

                if ($autoCommitCheckbox.is(":checked")) {
                    executeAutocommitPush(message);
                    return;
                }

                CodeAtlas.showToast(response.message || "Commit message generated.", "success");
                CodeAtlas.setButtonLoading($generateCommitBtn, false);
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to generate commit message."), "danger");
                CodeAtlas.setButtonLoading($generateCommitBtn, false);
            });
    });

    $copyCommitBtn.on("click", function () {
        const message = ($commitMessageTextArea.val() || "").trim();
        if (!message) {
            CodeAtlas.showToast("Nothing to copy.", "warning");
            return;
        }
        const copied = CodeAtlas.copyToClipboard(message);
        CodeAtlas.showToast(copied ? "Commit message copied." : "Failed to copy commit message.", copied ? "success" : "danger");
    });

    $clearCommitBtn.on("click", function () {
        $commitMessageTextArea.val("");
    });

    $commitBtn.on("click", function () {
        if (!validateCommitSelection()) {
            return;
        }
        const message = validateCommitMessage();
        if (!message) {
            return;
        }
        if (!window.confirm("Commit changes with the current message?")) {
            return;
        }

        CodeAtlas.setButtonLoading($commitBtn, true, "Committing...");
        $.ajax({
            url: "/api/commit-helper/commit",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({
                projectId: selectedProjectId(),
                commitMessage: message
            })
        })
            .done(function (response) {
                CodeAtlas.showToast(response.message || "Changes committed.", "success");
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to commit changes."), "danger");
            })
            .always(function () {
                CodeAtlas.setButtonLoading($commitBtn, false);
            });
    });

    $commitPushBtn.on("click", function () {
        if (!validateCommitSelection()) {
            return;
        }
        const message = validateCommitMessage();
        if (!message) {
            return;
        }
        if (!window.confirm("Commit and push changes with the current message?")) {
            return;
        }

        CodeAtlas.setButtonLoading($commitPushBtn, true, "Committing...");
        $.ajax({
            url: "/api/commit-helper/push",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({
                projectId: selectedProjectId(),
                commitMessage: message
            })
        })
            .done(function (response) {
                CodeAtlas.showToast(response.message || "Changes committed and pushed.", "success");
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to commit and push changes."), "danger");
            })
            .always(function () {
                CodeAtlas.setButtonLoading($commitPushBtn, false);
            });
    });

    loadMetadata();
});
