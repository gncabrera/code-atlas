$(function () {
    let selectedProjectId = null;

    const $projectSelect = $("#projectSelect");
    const $aiModelSelect = $("#aiModelSelect");
    const $exportFormatSelect = $("#exportFormatSelect");
    const $branchSelect = $("#branchSelect");
    const $journalCard = $("#journalCard");
    const $commitTableBody = $("#commitTableBody");
    const $selectAllCommits = $("#selectAllCommits");
    const $generateChangelogBtn = $("#generateChangelogBtn");
    const $resultsCard = $("#resultsCard");
    const $resultTextarea = $("#changelogResultTextarea");
    const $semverBadge = $("#semverBadge");
    const $copyChangelogBtn = $("#copyChangelogBtn");

    function initSelectors() {
        CodeAtlas.apiGet("/api/projects")
            .done(function (res) {
                const projects = res.data || [];
                projects.forEach(function (p) {
                    $projectSelect.append(new Option(p.name, p.id));
                });
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load projects."), "danger");
            });

        CodeAtlas.apiGet("/api/ai-models")
            .done(function (res) {
                const models = res.data || [];
                models.forEach(function (m) {
                    if (m.enabled) {
                        $aiModelSelect.append(new Option(m.name, m.id));
                    }
                });
                const applyStoredPreferences = function () {
                    if (window.CodeAtlasUserPreferences) {
                        CodeAtlasUserPreferences.applyPreferenceFields([
                            { field: "changelogBuilderDefaultAiModelId", selectId: "aiModelSelect" }
                        ]);
                    }
                };
                if (window.CodeAtlasUserPreferences) {
                    CodeAtlasUserPreferences.whenLoaded().always(applyStoredPreferences);
                } else {
                    applyStoredPreferences();
                }
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load AI models."), "danger");
            });
    }

    function resetJournalState() {
        $journalCard.addClass("d-none");
        $resultsCard.addClass("d-none");
        $branchSelect.empty().append(new Option("Select branch...", ""));
        $commitTableBody.empty().append(
            '<tr><td colspan="4" class="text-center text-muted">Select a branch to retrieve commits.</td></tr>'
        );
        $selectAllCommits.prop("checked", false);
        $generateChangelogBtn.prop("disabled", true);
        $semverBadge.addClass("d-none").text("");
        $resultTextarea.val("");
    }

    function toggleSubmitButton() {
        const selectedCount = $(".commit-checkbox:checked").length;
        $generateChangelogBtn.prop("disabled", selectedCount === 0);
    }

    function escapeHtml(text) {
        return String(text || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function renderSemverBadge(semverBump, semverReason) {
        if (!semverBump) {
            $semverBadge.addClass("d-none").text("");
            return;
        }
        const reason = semverReason ? " — " + semverReason : "";
        $semverBadge
            .removeClass("d-none")
            .html("<strong>Recommended:</strong> " + escapeHtml(semverBump) + escapeHtml(reason));
    }

    $projectSelect.on("change", function () {
        selectedProjectId = $(this).val();
        resetJournalState();

        if (!selectedProjectId) {
            return;
        }

        CodeAtlas.apiGet("/api/changelog-builder/projects/" + selectedProjectId + "/branches")
            .done(function (res) {
                const branches = res.data || [];
                branches.forEach(function (b) {
                    $branchSelect.append(new Option(b, b));
                });
                $journalCard.removeClass("d-none");
            })
            .fail(function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load branches."), "danger");
            });
    });

    $branchSelect.on("change", function () {
        const branchName = $(this).val();
        $commitTableBody.empty().append(
            '<tr><td colspan="4" class="text-center"><span class="spinner-border spinner-border-sm"></span> Loading commits...</td></tr>'
        );
        $selectAllCommits.prop("checked", false);
        $generateChangelogBtn.prop("disabled", true);
        $resultsCard.addClass("d-none");

        if (!branchName) {
            $commitTableBody.empty().append(
                '<tr><td colspan="4" class="text-center text-muted">Select a branch to retrieve commits.</td></tr>'
            );
            return;
        }

        CodeAtlas.apiGet(
            "/api/changelog-builder/projects/" + selectedProjectId + "/commits?branch=" + encodeURIComponent(branchName)
        )
            .done(function (res) {
                $commitTableBody.empty();
                const commits = res.data || [];
                if (commits.length === 0) {
                    $commitTableBody.append(
                        '<tr><td colspan="4" class="text-center text-muted">No commits found.</td></tr>'
                    );
                    return;
                }
                commits.forEach(function (commit) {
                    const shortHash = escapeHtml(commit.hash.substring(0, 8));
                    const subject = escapeHtml(commit.subject);
                    const author = escapeHtml(commit.author);
                    const dateFormatted = escapeHtml(commit.dateFormatted);
                    const hash = escapeHtml(commit.hash);
                    $commitTableBody.append(
                        "<tr>" +
                            '<td><input class="form-check-input commit-checkbox" type="checkbox" value="' + hash + '"></td>' +
                            "<td><strong>" + subject + "</strong><br><small class=\"text-muted\">" + shortHash + "</small></td>" +
                            "<td>" + author + "</td>" +
                            "<td>" + dateFormatted + "</td>" +
                        "</tr>"
                    );
                });
            })
            .fail(function (xhr) {
                $commitTableBody.empty().append(
                    '<tr><td colspan="4" class="text-center text-danger">Failed to load commits.</td></tr>'
                );
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Failed to load commits."), "danger");
            });
    });

    $selectAllCommits.on("change", function () {
        const isChecked = $(this).is(":checked");
        $(".commit-checkbox").prop("checked", isChecked);
        toggleSubmitButton();
    });

    $(document).on("change", ".commit-checkbox", function () {
        toggleSubmitButton();
    });

    $generateChangelogBtn.on("click", function () {
        const modelId = $aiModelSelect.val();
        if (!modelId) {
            CodeAtlas.showToast("Please select an AI model.", "warning");
            return;
        }

        const selectedHashes = [];
        $(".commit-checkbox:checked").each(function () {
            selectedHashes.push($(this).val());
        });

        const payload = {
            projectId: parseInt(selectedProjectId, 10),
            modelId: parseInt(modelId, 10),
            commitHashes: selectedHashes,
            exportFormat: $exportFormatSelect.val()
        };

        CodeAtlas.setButtonLoading($generateChangelogBtn, true, "Generating...");
        $resultsCard.addClass("d-none");

        $.ajax({
            url: "/api/changelog-builder/generate",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(payload),
            success: function (res) {
                const data = res.data || {};
                $resultTextarea.val(data.changelog || "");
                renderSemverBadge(data.semverBump, data.semverReason);
                $resultsCard.removeClass("d-none");
                CodeAtlas.showToast("Changelog generated!", "success");
            },
            error: function (xhr) {
                CodeAtlas.showToast(CodeAtlas.apiMessage(xhr, "Changelog generation failed."), "danger");
            },
            complete: function () {
                CodeAtlas.setButtonLoading($generateChangelogBtn, false);
            }
        });
    });

    $copyChangelogBtn.on("click", function () {
        const changelog = $resultTextarea.val();
        if (!changelog) {
            CodeAtlas.showToast("Nothing to copy.", "warning");
            return;
        }
        CodeAtlas.setButtonLoading($copyChangelogBtn, true, "Copying...");
        const copied = CodeAtlas.copyToClipboard(changelog);
        CodeAtlas.setButtonLoading($copyChangelogBtn, false);
        CodeAtlas.showToast(
            copied ? "Changelog copied." : "Failed to copy changelog.",
            copied ? "success" : "danger"
        );
    });

    initSelectors();
});
