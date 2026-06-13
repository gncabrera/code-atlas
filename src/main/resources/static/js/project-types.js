$(function () {
    function splitCommaList(value) {
        if (!value) {
            return [];
        }
        return value.split(",").map(function (item) {
            return item.trim();
        }).filter(Boolean);
    }

    function bindAllowedFilesControls() {
        $("#addAllowedFileBtn").on("click", function () {
            addAllowedFileRow("");
        });
        $("#allowedFilesTableBody").on("click", ".remove-allowed-file-btn", function () {
            $(this).closest("tr").remove();
        });
    }

    function addAllowedFileRow(value) {
        const $row = $("<tr></tr>");
        const $input = $("<input>", {
            type: "text",
            class: "form-control form-control-sm allowed-file-input",
            placeholder: "Dockerfile"
        }).val(value || "");
        const $deleteBtn = $("<button>", {
            type: "button",
            class: "btn btn-sm btn-outline-danger remove-allowed-file-btn"
        }).text("Delete");
        $row.append($("<td></td>").append($input));
        $row.append($("<td></td>").append($deleteBtn));
        $("#allowedFilesTableBody").append($row);
    }

    function getAllowedFilesFromTable() {
        const files = [];
        $("#allowedFilesTableBody .allowed-file-input").each(function () {
            const value = ($(this).val() || "").trim();
            if (value) {
                files.push(value);
            }
        });
        return files;
    }

    function setAllowedFilesInTable(files) {
        $("#allowedFilesTableBody").empty();
        (files || []).forEach(function (fileName) {
            addAllowedFileRow(fileName);
        });
    }

    function formatAllowedFilesSummary(projectType) {
        const files = splitCommaList(projectType.allowedFiles);
        if (files.length === 0) {
            return "—";
        }
        return files.join(", ");
    }

    bindAllowedFilesControls();

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
            setAllowedFilesInTable([]);
        },
        fillForm: function (projectType) {
            $("#projectTypeId").val(projectType.id);
            $("#projectTypeName").val(projectType.name);
            $("#projectTypeExtensions").val(projectType.allowedExtensions);
            $("#projectTypeDescription").val(projectType.description || "");
            setAllowedFilesInTable(splitCommaList(projectType.allowedFiles));
        },
        renderColumns: function (projectType) {
            return [
                $("<td></td>").text(projectType.name),
                $("<td></td>").text(projectType.allowedExtensions),
                $("<td></td>").text(formatAllowedFilesSummary(projectType)),
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
            const files = getAllowedFilesFromTable();
            for (let index = 0; index < files.length; index++) {
                if (files[index].includes("/") || files[index].includes("\\")) {
                    return "Allowed files must be filenames only.";
                }
            }
            return null;
        },
        buildPayload: function () {
            const description = ($("#projectTypeDescription").val() || "").trim();
            const files = getAllowedFilesFromTable();
            return {
                name: ($("#projectTypeName").val() || "").trim(),
                allowedExtensions: ($("#projectTypeExtensions").val() || "").trim(),
                allowedFiles: files.length ? files.join(",") : null,
                description: description || null
            };
        }
    });
});
