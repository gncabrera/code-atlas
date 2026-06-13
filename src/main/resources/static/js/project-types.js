$(function () {
    CodeAtlas.initCrudPage({
        tableBodySelector: "#projectTypesTableBody",
        apiBase: "/api/admin/project-types",
        idFieldSelector: "#projectTypeId",
        saveBtnSelector: "#saveProjectTypeBtn",
        saveLoadingText: "Saving Project Type...",
        resetBtnSelector: "#resetProjectTypeBtn",
        deleteConfirmMessage: "Delete selected project type?",
        messages: {
            loadFailed: "Failed loading project types.",
            saveFailed: "Failed saving project type.",
            deleteFailed: "Failed deleting project type.",
            deleted: "Project type deleted.",
            saved: "Project type saved."
        },
        clearForm: function () {
            $("#projectTypeId").val("");
            $("#projectTypeName").val("");
            $("#projectTypeExtensions").val("");
            $("#projectTypeDescription").val("");
        },
        fillForm: function (projectType) {
            $("#projectTypeId").val(projectType.id);
            $("#projectTypeName").val(projectType.name);
            $("#projectTypeExtensions").val(projectType.allowedExtensions);
            $("#projectTypeDescription").val(projectType.description || "");
        },
        renderColumns: function (projectType) {
            return [
                $("<td></td>").text(projectType.name),
                $("<td></td>").text(projectType.allowedExtensions),
                $("<td></td>").text(projectType.description || "—")
            ];
        },
        validateSave: function () {
            const name = ($("#projectTypeName").val() || "").trim();
            const extensions = ($("#projectTypeExtensions").val() || "").trim();
            if (!name) {
                return "Name is required.";
            }
            if (!extensions) {
                return "Allowed extensions are required.";
            }
            return null;
        },
        buildPayload: function () {
            const description = ($("#projectTypeDescription").val() || "").trim();
            return {
                name: ($("#projectTypeName").val() || "").trim(),
                allowedExtensions: ($("#projectTypeExtensions").val() || "").trim(),
                description: description || null
            };
        }
    });
});
