$(function () {
    let editingReadOnly = false;

    function setFormReadOnlyState(isReadOnly) {
        editingReadOnly = Boolean(isReadOnly);
        $("#readOnlyNotice").toggleClass("d-none", !editingReadOnly);
        $("#modeCode").prop("disabled", editingReadOnly);
        $("#modeName").prop("disabled", editingReadOnly);
        $("#modePrompt").prop("disabled", editingReadOnly);
    }

    CodeAtlas.initCrudPage({
        tableBodySelector: "#modesTableBody",
        apiBase: "/api/admin/prompt-optimizer-modes",
        idFieldSelector: "#modeId",
        saveBtnSelector: "#saveModeBtn",
        saveLoadingText: "Saving Prompt Mode...",
        resetBtnSelector: "#resetModeBtn",
        deleteConfirmMessage: "Delete selected prompt optimizer mode?",
        messages: {
            loadFailed: "Failed loading prompt optimizer modes.",
            saveFailed: "Failed saving prompt optimizer mode.",
            deleteFailed: "Failed deleting prompt optimizer mode.",
            deleted: "Prompt optimizer mode deleted.",
            saved: "Prompt optimizer mode saved."
        },
        clearForm: function () {
            $("#modeId").val("");
            $("#modeCode").val("");
            $("#modeName").val("");
            $("#modePrompt").val("");
            $("#modeHidden").prop("checked", false);
            setFormReadOnlyState(false);
        },
        fillForm: function (mode) {
            $("#modeId").val(mode.id);
            $("#modeCode").val(mode.code);
            $("#modeName").val(mode.name);
            $("#modePrompt").val(mode.prompt);
            $("#modeHidden").prop("checked", mode.hidden);
            setFormReadOnlyState(mode.readOnly);
        },
        renderColumns: function (mode) {
            return [
                $("<td></td>").text(mode.code),
                $("<td></td>").text(mode.name),
                $("<td></td>").text(mode.hidden ? "Yes" : "No"),
                $("<td></td>").text(mode.readOnly ? "Yes" : "No")
            ];
        },
        validateSave: function () {
            if (editingReadOnly) {
                return null;
            }
            const code = ($("#modeCode").val() || "").trim();
            const name = ($("#modeName").val() || "").trim();
            const prompt = ($("#modePrompt").val() || "").trim();
            if (!code) {
                return "Mode code is required.";
            }
            if (!name) {
                return "Display name is required.";
            }
            if (!prompt) {
                return "Prompt template is required.";
            }
            return null;
        },
        buildPayload: function () {
            return {
                code: ($("#modeCode").val() || "").trim(),
                name: ($("#modeName").val() || "").trim(),
                prompt: ($("#modePrompt").val() || "").trim(),
                hidden: $("#modeHidden").is(":checked")
            };
        }
    });
});
