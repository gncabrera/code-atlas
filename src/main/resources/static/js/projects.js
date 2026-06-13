(function () {
    const $projectId = $("#projectId");
    const $projectName = $("#projectName");
    const $exportBtn = $("#exportIndexBtn");
    const $importBtn = $("#importIndexBtn");
    const $importFileInput = $("#importIndexFileInput");
    const $clearBtn = $("#clearIndexBtn");
    const $integrityBadge = $("#indexIntegrityBadge");
    const $statsText = $("#indexStatsText");
    const $preflightPanel = $("#indexPreflightPanel");
    const $preflightStats = $("#indexPreflightStats");
    const $preflightWarnings = $("#indexPreflightWarnings");
    const $confirmImportBtn = $("#confirmImportBtn");
    const $cancelImportBtn = $("#cancelImportBtn");
    const $clearModal = $("#indexClearModal");
    const $clearConfirmInput = $("#indexClearConfirmInput");
    const $clearSubmitBtn = $("#indexClearSubmitBtn");

    let transientBadgeTimer = null;
    let activePreflightId = null;
    let clearModalInstance = null;
    let loadedProjectTypes = [];

    function populateProjectTypeMultiselect(projectTypes, selectedIds) {
        const $select = $("#projectTypeMultiselect");
        loadedProjectTypes = projectTypes || [];
        if ($select.data("multiselect")) {
            $select.multiselect("destroy");
        }
        $select.empty();
        const initialSelection = selectedIds !== undefined && selectedIds !== null
            ? selectedIds.map(String)
            : [];
        if (!projectTypes || projectTypes.length === 0) {
            $select.multiselect({
                enableFiltering: true,
                includeSelectAllOption: true,
                buttonWidth: "100%",
                nonSelectedText: "No project types available",
                numberDisplayed: 3
            });
            return;
        }
        projectTypes.forEach(function (projectType) {
            const optionId = String(projectType.id);
            $select.append(
                $("<option>", {
                    value: optionId,
                    text: projectType.name,
                    selected: initialSelection.indexOf(optionId) >= 0
                })
            );
        });
        $select.multiselect({
            enableFiltering: true,
            includeSelectAllOption: true,
            buttonWidth: "100%",
            nonSelectedText: "Select project types",
            numberDisplayed: 3
        });
    }

    function loadProjectTypes(selectedIds) {
        $.ajax({
            url: "/api/admin/project-types",
            method: "GET"
        }).done(function (response) {
            if (response.result !== "success") {
                CodeAtlas.showToast(response.message || "Failed loading project types.", "danger");
                populateProjectTypeMultiselect([], selectedIds);
                return;
            }
            populateProjectTypeMultiselect(response.data || [], selectedIds);
        }).fail(function (xhr) {
            CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed loading project types."), "danger");
            populateProjectTypeMultiselect([], selectedIds);
        });
    }

    function getSelectedProjectTypeIds() {
        return ($("#projectTypeMultiselect").val() || []).map(function (id) {
            return Number(id);
        });
    }

    function getSelectedProjectId() {
        const id = $projectId.val();
        return id ? String(id) : "";
    }

    function setIndexButtonsEnabled(enabled) {
        $exportBtn.prop("disabled", !enabled);
        $importBtn.prop("disabled", !enabled);
        $clearBtn.prop("disabled", !enabled);
    }

    function hidePreflightPanel() {
        activePreflightId = null;
        $preflightPanel.addClass("d-none");
        $preflightStats.empty();
        $preflightWarnings.empty();
    }

    function renderBaselineBadge(integrity) {
        $integrityBadge.removeClass("bg-success bg-warning bg-info bg-secondary");
        if (integrity === "FRESH") {
            $integrityBadge.addClass("bg-success").text("Fresh");
        } else if (integrity === "STALE") {
            $integrityBadge.addClass("bg-warning").text("Stale");
        } else {
            $integrityBadge.addClass("bg-secondary").text("No project selected");
        }
    }

    function showTransientBadge(label, cssClass) {
        if (transientBadgeTimer) {
            clearTimeout(transientBadgeTimer);
        }
        $integrityBadge.removeClass("bg-success bg-warning bg-info bg-secondary");
        $integrityBadge.addClass(cssClass).text(label);
        transientBadgeTimer = setTimeout(function () {
            transientBadgeTimer = null;
            refreshIndexStatus();
        }, 8000);
    }

    function renderStats(status) {
        if (!status) {
            $statsText.text("");
            return;
        }
        const parts = [
            status.fileIndexCount + " file records",
            status.metadataIndexCount + " metadata records"
        ];
        if (status.newestUpdatedAt) {
            parts.push("updated " + status.newestUpdatedAt);
        }
        $statsText.text(parts.join(" · "));
    }

    function refreshIndexStatus() {
        const projectId = getSelectedProjectId();
        if (!projectId) {
            renderBaselineBadge(null);
            renderStats(null);
            setIndexButtonsEnabled(false);
            hidePreflightPanel();
            return;
        }
        $.ajax({
            url: "/api/projects/" + projectId + "/index/status",
            method: "GET"
        }).done(function (response) {
            const status = response.data;
            if (transientBadgeTimer) {
                return;
            }
            renderBaselineBadge(status.integrity);
            renderStats(status);
            setIndexButtonsEnabled(true);
        }).fail(function (xhr) {
            CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed loading index status."), "danger");
        });
    }

    function bindIndexControls() {
        $exportBtn.on("click", function () {
            const projectId = getSelectedProjectId();
            if (!projectId) {
                CodeAtlas.showToast("Select a project first.", "danger");
                return;
            }
            const $btn = $(this);
            CodeAtlas.setButtonLoading($btn, true, "Exporting...");
            fetch("/api/projects/" + projectId + "/index/export")
                .then(function (response) {
                    if (!response.ok) {
                        return response.json().then(function (body) {
                            throw new Error(body.message || "Export failed.");
                        }).catch(function () {
                            throw new Error("Export failed.");
                        });
                    }
                    return response.blob().then(function (blob) {
                        const url = window.URL.createObjectURL(blob);
                        const link = document.createElement("a");
                        link.href = url;
                        link.download = "project-" + projectId + "-index.json";
                        document.body.appendChild(link);
                        link.click();
                        link.remove();
                        window.URL.revokeObjectURL(url);
                    });
                })
                .then(function () {
                    CodeAtlas.showToast("Project index exported.", "success");
                    showTransientBadge("Exported", "bg-info");
                })
                .catch(function (err) {
                    CodeAtlas.showToast(err.message || "Export failed.", "danger");
                })
                .finally(function () {
                    CodeAtlas.setButtonLoading($btn, false);
                });
        });

        $importBtn.on("click", function () {
            $importFileInput.val("");
            $importFileInput.trigger("click");
        });

        $importFileInput.on("change", function () {
            const file = this.files && this.files[0];
            const projectId = getSelectedProjectId();
            if (!file || !projectId) {
                return;
            }
            const fileName = file.name || "";
            if (!fileName.toLowerCase().endsWith(".json")) {
                CodeAtlas.showToast("Import file must have a .json extension.", "danger");
                return;
            }
            const formData = new FormData();
            formData.append("file", file);
            CodeAtlas.setButtonLoading($importBtn, true, "Analyzing...");
            $.ajax({
                url: "/api/projects/" + projectId + "/index/import/preflight",
                method: "POST",
                data: formData,
                processData: false,
                contentType: false
            }).done(function (response) {
                const preflight = response.data;
                activePreflightId = preflight.preflightId;
                $preflightStats.empty();
                $preflightStats.append($("<li></li>").text("Total records: " + preflight.totalRecords));
                $preflightStats.append($("<li></li>").text("Overwrite targets: " + preflight.overwriteCount));
                $preflightStats.append($("<li></li>").text("New paths: " + preflight.newPathCount));
                $preflightStats.append($("<li></li>").text("Missing on disk: " + preflight.missingOnDiskCount));
                $preflightWarnings.empty();
                (preflight.warnings || []).forEach(function (warning) {
                    $preflightWarnings.append($("<li></li>").text(warning));
                });
                $preflightPanel.removeClass("d-none");
            }).fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Import preflight failed."), "danger");
            }).always(function () {
                CodeAtlas.setButtonLoading($importBtn, false);
            });
        });

        $cancelImportBtn.on("click", function () {
            hidePreflightPanel();
        });

        $confirmImportBtn.on("click", function () {
            const projectId = getSelectedProjectId();
            if (!projectId || !activePreflightId) {
                return;
            }
            if (!window.confirm("Proceed with override? Existing records for matching paths will be replaced.")) {
                return;
            }
            const $btn = $(this);
            CodeAtlas.setButtonLoading($btn, true, "Importing...");
            $.ajax({
                url: "/api/projects/" + projectId + "/index/import/confirm",
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify({ preflightId: activePreflightId })
            }).done(function (response) {
                CodeAtlas.showToast(response.message || "Project index imported.", "success");
                hidePreflightPanel();
                showTransientBadge("Imported", "bg-info");
                refreshIndexStatus();
            }).fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Import failed."), "danger");
            }).always(function () {
                CodeAtlas.setButtonLoading($btn, false);
            });
        });

        $clearBtn.on("click", function () {
            const projectId = getSelectedProjectId();
            if (!projectId) {
                CodeAtlas.showToast("Select a project first.", "danger");
                return;
            }
            $clearConfirmInput.val("");
            if (!clearModalInstance) {
                clearModalInstance = new bootstrap.Modal($clearModal[0]);
            }
            clearModalInstance.show();
        });

        $clearSubmitBtn.on("click", function () {
            const projectId = getSelectedProjectId();
            const confirmationText = $clearConfirmInput.val().trim();
            const projectName = $projectName.val().trim();
            if (!projectId) {
                return;
            }
            if (confirmationText !== "CLEAR" && confirmationText !== projectName) {
                CodeAtlas.showToast("Type CLEAR or the exact project name to confirm.", "danger");
                return;
            }
            const $btn = $(this);
            CodeAtlas.setButtonLoading($btn, true, "Clearing...");
            $.ajax({
                url: "/api/projects/" + projectId + "/index/clear",
                method: "DELETE",
                contentType: "application/json",
                data: JSON.stringify({ confirmationText: confirmationText })
            }).done(function (response) {
                CodeAtlas.showToast(response.message || "Project index cleared.", "success");
                if (clearModalInstance) {
                    clearModalInstance.hide();
                }
                hidePreflightPanel();
                refreshIndexStatus();
            }).fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Clear index failed."), "danger");
            }).always(function () {
                CodeAtlas.setButtonLoading($btn, false);
            });
        });
    }

    const originalFillForm = function (project) {
        $("#projectId").val(project.id);
        $("#projectName").val(project.name);
        $("#projectPath").val(project.path);
        $("#projectDescription").val(project.description);
        $("#projectUseAgentsFile").prop("checked", project.useAgentsFile);
        $("#projectUseDesignFile").prop("checked", project.useDesignFile);
        populateProjectTypeMultiselect(loadedProjectTypes, project.projectTypeIds || []);
        refreshIndexStatus();
    };

    const originalClearForm = function () {
        $("#projectId").val("");
        $("#projectName").val("");
        $("#projectPath").val("");
        $("#projectDescription").val("");
        $("#projectUseAgentsFile").prop("checked", true);
        $("#projectUseDesignFile").prop("checked", true);
        populateProjectTypeMultiselect(loadedProjectTypes, []);
        $(".project-profile").prop("checked", false);
        $("#profileSpringJava, #profileSpringFlyway, #profileThymeleaf").prop("checked", true);
        hidePreflightPanel();
        refreshIndexStatus();
    };

    $(function () {
        bindIndexControls();
        loadProjectTypes([]);
        refreshIndexStatus();
    });

    CodeAtlas.initCrudPage({
        tableBodySelector: "#projectsTableBody",
        apiBase: "/api/projects",
        idFieldSelector: "#projectId",
        saveBtnSelector: "#saveProjectBtn",
        saveLoadingText: "Saving Project...",
        resetBtnSelector: "#resetProjectBtn",
        deleteConfirmMessage: "Delete selected project?",
        messages: {
            loadFailed: "Failed loading projects.",
            saveFailed: "Failed saving project.",
            deleteFailed: "Failed deleting project.",
            deleted: "Project deleted.",
            saved: "Project saved."
        },
        clearForm: originalClearForm,
        fillForm: originalFillForm,
        buildPayload: function () {
            const profiles = [];
            $(".project-profile:checked").each(function () {
                profiles.push($(this).val());
            });
            return {
                name: $("#projectName").val().trim(),
                path: $("#projectPath").val().trim(),
                description: $("#projectDescription").val().trim(),
                useAgentsFile: $("#projectUseAgentsFile").is(":checked"),
                useDesignFile: $("#projectUseDesignFile").is(":checked"),
                projectTypeIds: getSelectedProjectTypeIds()
            };
        },
        validateSave: function () {
            const name = $("#projectName").val().trim();
            const path = $("#projectPath").val().trim();
            const description = $("#projectDescription").val().trim();
            if (!name) {
                return "Name is required.";
            }
            if (!path) {
                return "Path is required.";
            }
            if (!description) {
                return "Description is required.";
            }
            if (getSelectedProjectTypeIds().length === 0) {
                return "At least one project type is required.";
            }
            return null;
        },
        renderColumns: function (project) {
            const typeNames = project.projectTypeNames && project.projectTypeNames.length
                ? project.projectTypeNames.join(", ")
                : "—";
            return [
                $("<td></td>").text(project.name),
                $("<td></td>").text(project.path),
                $("<td></td>").text(project.description),
                $("<td></td>").text(project.useAgentsFile ? "Yes" : "No"),
                $("<td></td>").text(project.useDesignFile ? "Yes" : "No"),
                $("<td></td>").text(typeNames)
            ];
        }
    });
})();
