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
    clearForm: function () {
        $("#projectId").val("");
        $("#projectName").val("");
        $("#projectPath").val("");
        $("#projectDescription").val("");
        $("#projectUseAgentsFile").prop("checked", true);
    },
    fillForm: function (project) {
        $("#projectId").val(project.id);
        $("#projectName").val(project.name);
        $("#projectPath").val(project.path);
        $("#projectDescription").val(project.description);
        $("#projectUseAgentsFile").prop("checked", project.useAgentsFile);
    },
    buildPayload: function () {
        return {
            name: $("#projectName").val().trim(),
            path: $("#projectPath").val().trim(),
            description: $("#projectDescription").val().trim(),
            useAgentsFile: $("#projectUseAgentsFile").is(":checked")
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
        return null;
    },
    renderColumns: function (project) {
        return [
            $("<td></td>").text(project.name),
            $("<td></td>").text(project.path),
            $("<td></td>").text(project.description),
            $("<td></td>").text(project.useAgentsFile ? "Yes" : "No")
        ];
    }
});
