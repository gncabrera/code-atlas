$(function () {
    let projects = [];
    let enabledModels = [];
    let branches = [];
    let lastReviewData = null;
    let activeSeverityFilter = "ALL";

    const $project = $("#projectSelect");
    const $model = $("#modelSelect");
    const $branchA = $("#branchA");
    const $branchB = $("#branchB");
    const $currentChangesCheckbox = $("#currentChangesOnly");
    const $btn = $("#btnRunReview");
    const $progress = $("#reviewProgress");
    const $result = $("#reviewResult");
    const $severityFilters = $("#severityFilters");

    function populateProjects() {
        $project.empty().append($("<option>", { value: "", text: "Select project" }));
        projects.forEach(function (project) {
            $project.append($("<option>", { value: project.id, text: project.name }));
        });
    }

    function populateModels() {
        $model.empty().append($("<option>", { value: "", text: "Select AI model" }));
        enabledModels.forEach(function (model) {
            $model.append($("<option>", { value: model.id, text: model.name }));
        });
    }

    function populateBranches(branchList) {
        const options = [$("<option>", { value: "", text: "Choose branch" })];
        branchList.forEach(function (branch) {
            options.push($("<option>", { value: branch, text: branch }));
        });
        $branchA.empty().append(options).prop("disabled", false);
        $branchB.empty().append(options.map(function (option) {
            return option.clone();
        })).prop("disabled", false);
    }

    function resetBranches() {
        $branchA.html('<option value="">Select project first</option>').prop("disabled", true).val("");
        $branchB.html('<option value="">Select project first</option>').prop("disabled", true).val("");
        branches = [];
    }

    function toggleBranchDropdowns(disabled) {
        $branchA.prop("disabled", disabled);
        $branchB.prop("disabled", disabled);
    }

    function isCurrentChangesOnly() {
        return $currentChangesCheckbox.is(":checked");
    }

    function checkFormState() {
        const hasProjectAndModel = $project.val() && $model.val();
        const hasBranches = $branchA.val() && $branchB.val();
        const valid = hasProjectAndModel && (isCurrentChangesOnly() || hasBranches);
        $btn.prop("disabled", !valid);
    }

    function validateRunReview() {
        if (!$project.val()) {
            return "Select project.";
        }
        if (!$model.val()) {
            return "Select AI model.";
        }
        if (isCurrentChangesOnly()) {
            return "";
        }
        if (!$branchA.val() || !$branchB.val()) {
            return "Select both branches.";
        }
        if ($branchA.val() === $branchB.val()) {
            return "Base branch and compare branch must be different.";
        }
        return "";
    }

    function loadMetadata(projectId) {
        const url = projectId
            ? "/api/code-review/metadata?projectId=" + encodeURIComponent(projectId)
            : "/api/code-review/metadata";

        return CodeAtlas.apiGet(url)
            .done(function (response) {
                projects = response.data.projects || [];
                enabledModels = response.data.enabledModels || [];
                populateProjects();
                populateModels();
                const applyStoredPreferences = function () {
                    if (window.CodeAtlasUserPreferences) {
                        CodeAtlasUserPreferences.applyPreferenceFields([
                            { field: "codeReviewDefaultAiModelId", selectId: "modelSelect" }
                        ]);
                    }
                    if (projectId) {
                        branches = response.data.branches || [];
                        populateBranches(branches);
                    } else {
                        resetBranches();
                    }
                    if (projectId) {
                        $project.val(String(projectId));
                    }
                    if (isCurrentChangesOnly()) {
                        toggleBranchDropdowns(true);
                    }
                    checkFormState();
                };
                if (window.CodeAtlasUserPreferences) {
                    CodeAtlasUserPreferences.whenLoaded().always(applyStoredPreferences);
                } else {
                    applyStoredPreferences();
                }
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load code review metadata."), "danger");
            });
    }

    $project.on("change", function () {
        const projectId = $(this).val();
        $result.addClass("d-none");
        if (!projectId) {
            resetBranches();
            checkFormState();
            return;
        }
        loadMetadata(Number(projectId));
    });

    $model.on("change", checkFormState);
    $branchA.on("change", checkFormState);
    $branchB.on("change", checkFormState);

    $currentChangesCheckbox.on("change", function () {
        toggleBranchDropdowns($(this).is(":checked"));
        checkFormState();
    });

    $btn.on("click", function () {
        const validationError = validateRunReview();
        if (validationError) {
            CodeAtlas.showToast(validationError, "warning");
            return;
        }

        $result.addClass("d-none");
        $progress.removeClass("d-none");
        CodeAtlas.setButtonLoading($btn, true, "Reviewing...");

        const payload = {
            projectId: Number($project.val()),
            modelId: Number($model.val()),
            currentChangesOnly: isCurrentChangesOnly()
        };

        if (!payload.currentChangesOnly) {
            payload.branchA = $branchA.val();
            payload.branchB = $branchB.val();
        }

        $.ajax({
            url: "/api/code-review",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify(payload)
        })
            .done(function (response) {
                const result = String(response.result || "").toUpperCase();
                if (result === "SUCCESS") {
                    lastReviewData = response.data;
                    renderReviewResult(response.data);
                } else {
                    CodeAtlas.showToast(response.message || "Code review failed.", "danger");
                }
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Code review failed."), "danger");
            })
            .always(function () {
                $progress.addClass("d-none");
                CodeAtlas.setButtonLoading($btn, false);
            });
    });

    $severityFilters.on("click", ".severity-filter", function () {
        activeSeverityFilter = $(this).data("severity");
        $severityFilters.find(".severity-filter").removeClass("active");
        $(this).addClass("active");
        applySeverityFilter();
    });

    function renderReviewResult(data) {
        const summary = data.summary || {};
        $("#summaryScore").text(summary.score != null ? summary.score : "-");

        const risk = String(summary.risk || "").toUpperCase();
        const riskClass = risk === "HIGH" ? "danger" : risk === "MEDIUM" ? "warning" : "success";
        $("#summaryRisk").html('<span class="badge bg-' + riskClass + '">' + escapeHtml(risk || "N/A") + "</span>");

        const $concerns = $("#summaryConcerns").empty();
        const concerns = summary.mainConcerns || [];
        if (concerns.length > 0) {
            concerns.forEach(function (concern) {
                $concerns.append(
                    '<li class="list-group-item bg-transparent px-0 py-1 border-0 text-muted">' +
                    escapeHtml(concern) +
                    "</li>"
                );
            });
        } else {
            $concerns.append(
                '<li class="list-group-item bg-transparent px-0 py-1 border-0 text-success">' +
                "No major concerns identified." +
                "</li>"
            );
        }

        renderSeverityFilters(data.findings || []);
        renderFindings(data.findings || []);
        $result.removeClass("d-none");
    }

    function renderSeverityFilters(findings) {
        const severities = ["ALL"];
        findings.forEach(function (finding) {
            const severity = String(finding.severity || "").toUpperCase();
            if (severity && severities.indexOf(severity) === -1) {
                severities.push(severity);
            }
        });

        $severityFilters.empty();
        if (findings.length === 0) {
            $severityFilters.addClass("d-none");
            return;
        }

        severities.forEach(function (severity) {
            const isActive = severity === activeSeverityFilter;
            $severityFilters.append(
                '<button type="button" class="btn btn-outline-secondary severity-filter' +
                (isActive ? " active" : "") +
                '" data-severity="' + escapeHtml(severity) + '">' +
                escapeHtml(severity) +
                "</button>"
            );
        });
        if (severities.indexOf(activeSeverityFilter) === -1) {
            activeSeverityFilter = "ALL";
            $severityFilters.find('.severity-filter[data-severity="ALL"]').addClass("active");
        }
        $severityFilters.removeClass("d-none");
    }

    function renderFindings(findings) {
        const $container = $("#findingsContainer").empty();
        if (!findings.length) {
            $container.html(
                '<div class="alert alert-success mb-0">' +
                "No findings reported." +
                "</div>"
            );
            return;
        }

        findings.forEach(function (finding, index) {
            const severity = String(finding.severity || "LOW").toUpperCase();
            const badgeColor = severity === "HIGH" ? "danger" : severity === "MEDIUM" ? "warning" : "info";
            const patchHtml = finding.suggestedPatch
                ? '<div class="mt-3 finding-details">' +
                '<span class="small fw-bold text-secondary">Suggested patch</span>' +
                '<pre class="bg-dark text-light p-3 rounded mt-1 small mb-0 suggested-patch"><code>' +
                escapeHtml(finding.suggestedPatch) +
                "</code></pre></div>"
                : "";

            const $card = $(
                '<div class="card mb-3 finding-card" data-severity="' +
                escapeHtml(severity) +
                '">' +
                '<div class="card-body">' +
                '<div class="d-flex justify-content-between align-items-start gap-2">' +
                '<div class="flex-grow-1">' +
                '<span class="badge bg-secondary me-2">' + escapeHtml(finding.category || "") + "</span>" +
                '<span class="badge bg-' + badgeColor + '">' + escapeHtml(severity) + "</span>" +
                '<h5 class="mt-2 mb-1">' + escapeHtml(finding.title || "Finding") + "</h5>" +
                '<p class="text-muted small mb-0 finding-location">' +
                escapeHtml(finding.file || "N/A") +
                " : line " +
                escapeHtml(finding.line != null ? String(finding.line) : "N/A") +
                "</p></div>" +
                '<button type="button" class="btn btn-sm btn-outline-secondary finding-toggle" aria-expanded="false">' +
                "Details</button></div>" +
                '<p class="mb-2 text-secondary small mt-2 finding-summary">' +
                escapeHtml(finding.description || "") +
                "</p>" +
                '<div class="finding-details">' +
                '<div class="row g-2 bg-light p-2 rounded small">' +
                '<div class="col-md-6"><span class="fw-bold">Impact:</span> ' +
                escapeHtml(finding.impact || "") +
                "</div>" +
                '<div class="col-md-6"><span class="fw-bold">Suggestion:</span> ' +
                escapeHtml(finding.suggestion || "") +
                "</div></div>" +
                patchHtml +
                "</div></div></div>"
            );

            $card.find(".finding-toggle").on("click", function () {
                const expanded = $card.toggleClass("is-expanded").hasClass("is-expanded");
                $(this).text(expanded ? "Hide" : "Details").attr("aria-expanded", expanded ? "true" : "false");
            });

            $container.append($card);
        });

        applySeverityFilter();
    }

    function applySeverityFilter() {
        $(".finding-card").each(function () {
            const $card = $(this);
            const severity = String($card.data("severity") || "").toUpperCase();
            const visible = activeSeverityFilter === "ALL" || severity === activeSeverityFilter;
            $card.toggleClass("is-filtered-out", !visible);
        });
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    const urlParams = new URLSearchParams(window.location.search);
    const initialProjectId = urlParams.get("projectId");
    if (urlParams.get("currentChangesOnly") === "true") {
        $currentChangesCheckbox.prop("checked", true);
        toggleBranchDropdowns(true);
    }

    loadMetadata(initialProjectId ? Number(initialProjectId) : null);
});
