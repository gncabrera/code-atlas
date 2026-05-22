(function () {
    const $skillModal = $("#skillFormModal");
    const skillModal = $skillModal.length ? bootstrap.Modal.getOrCreateInstance($skillModal[0]) : null;

    function showSkillModal() {
        if (skillModal) {
            skillModal.show();
        }
    }

    function renderInstallCheckboxes(skills) {
        const $container = $("#skillInstallCheckboxes");
        $container.empty();
        if (!skills || skills.length === 0) {
            $container.append($("<p class='text-muted mb-0'></p>").text("No skills available."));
            return;
        }
        skills.forEach(function (skill) {
            const id = "install-skill-" + skill.id;
            const label = skill.category ? skill.name + " (" + skill.category + ")" : skill.name;
            const $check = $(
                `<div class="form-check">
                    <input class="form-check-input skill-install-checkbox" type="checkbox" value="${skill.id}" id="${id}">
                    <label class="form-check-label" for="${id}"></label>
                </div>`
            );
            $check.find("label").text(label);
            $container.append($check);
        });
    }

    function setInstallControlsEnabled(enabled) {
        $("#installProjectId").prop("disabled", !enabled);
        $(".skill-install-checkbox").prop("disabled", !enabled);
    }

    CodeAtlas.initCrudPage({
        tableBodySelector: "#skillsTableBody",
        apiBase: "/api/skills",
        idFieldSelector: "#skillId",
        saveBtnSelector: "#saveSkillBtn",
        saveLoadingText: "Saving Skill...",
        resetBtnSelector: "#resetSkillBtn",
        deleteConfirmMessage: "Delete selected skill?",
        onListLoaded: renderInstallCheckboxes,
        messages: {
            loadFailed: "Failed loading skills.",
            saveFailed: "Failed saving skill.",
            deleteFailed: "Failed deleting skill.",
            deleted: "Skill deleted.",
            saved: "Skill saved."
        },
        clearForm: function () {
            $("#skillId").val("");
            $("#skillName").val("");
            $("#skillCategory").val("");
            $("#skillTargetPath").val("");
            $("#skillDescription").val("");
            $("#skillPrompt").val("");
            $("#skillDefaultInOutputPrompt").prop("checked", false);
            if (skillModal) {
                skillModal.hide();
            }
        },
        fillForm: function (skill) {
            $("#skillId").val(skill.id);
            $("#skillName").val(skill.name);
            $("#skillCategory").val(skill.category || "");
            $("#skillTargetPath").val(skill.targetPath);
            $("#skillDescription").val(skill.description || "");
            $("#skillPrompt").val(skill.prompt);
            $("#skillDefaultInOutputPrompt").prop("checked", Boolean(skill.defaultInOutputPrompt));
            showSkillModal();
        },
        buildPayload: function () {
            return {
                name: $("#skillName").val().trim(),
                prompt: $("#skillPrompt").val(),
                targetPath: $("#skillTargetPath").val().trim(),
                description: $("#skillDescription").val().trim(),
                category: $("#skillCategory").val().trim(),
                defaultInOutputPrompt: $("#skillDefaultInOutputPrompt").is(":checked")
            };
        },
        validateSave: function () {
            const name = $("#skillName").val().trim();
            const prompt = $("#skillPrompt").val();
            const targetPath = $("#skillTargetPath").val().trim();
            if (!name) {
                return "Name is required.";
            }
            if (!prompt || !prompt.trim()) {
                return "Prompt is required.";
            }
            if (!targetPath) {
                return "Target path is required.";
            }
            return null;
        },
        renderColumns: function (skill) {
            const desc = skill.description || "";
            const descSnippet = desc.length > 80 ? desc.substring(0, 80) + "…" : desc;
            return [
                $("<td></td>").text(skill.name),
                $("<td></td>").text(skill.category || ""),
                $("<td></td>").text(skill.targetPath),
                $("<td></td>").text(descSnippet),
                $("<td></td>").text(skill.defaultInOutputPrompt ? "Yes" : "No")
            ];
        }
    });

    $(function () {
        $("#btn-create-skill").on("click", function () {
            $("#skillId").val("");
            $("#skillName").val("");
            $("#skillCategory").val("");
            $("#skillTargetPath").val("");
            $("#skillDescription").val("");
            $("#skillPrompt").val("");
            $("#skillDefaultInOutputPrompt").prop("checked", false);
            showSkillModal();
        });

        $("#btn-install-skills").on("click", function () {
            const $btn = $(this);
            const projectId = $("#installProjectId").val();
            const skillIds = $(".skill-install-checkbox:checked")
                .map(function () {
                    return Number($(this).val());
                })
                .get();

            if (!projectId) {
                CodeAtlas.showToast("Select a project.", "danger");
                return;
            }
            if (skillIds.length === 0) {
                CodeAtlas.showToast("Select at least one skill.", "danger");
                return;
            }
            if (!window.confirm("All these skills in the project will be overriden if they exist. Are you sure?")) {
                return;
            }

            CodeAtlas.setButtonLoading($btn, true, "Installing...");
            setInstallControlsEnabled(false);

            $.ajax({
                url: "/api/skills/install",
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    projectId: Number(projectId),
                    skillIds: skillIds
                })
            })
                .done(function (response) {
                    CodeAtlas.showToast(response.message || "Skills installed.", "success");
                })
                .fail(function (xhr) {
                    CodeAtlas.showToast(
                        CodeAtlas.apiMessage(xhr, "Failed installing skills."),
                        "danger"
                    );
                })
                .always(function () {
                    CodeAtlas.setButtonLoading($btn, false);
                    setInstallControlsEnabled(true);
                });
        });
    });
})();
