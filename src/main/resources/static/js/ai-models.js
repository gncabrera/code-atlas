$(function () {
    const descriptionMaxLength = 500;

    function updateDescriptionCounter() {
        const length = $("#modelDescription").val().length;
        $("#modelDescriptionCounter").text(length + " / " + descriptionMaxLength);
    }

    $("#modelDescription").on("input", updateDescriptionCounter);
    updateDescriptionCounter();

    CodeAtlas.initCrudPage({
        alertSelector: "#modelsAlert",
        tableBodySelector: "#modelsTableBody",
        apiBase: "/api/ai-models",
        idFieldSelector: "#modelId",
        saveBtnSelector: "#saveModelBtn",
        saveLoadingText: "Saving AI Model...",
        resetBtnSelector: "#resetModelBtn",
        deleteConfirmMessage: "Delete selected AI model?",
        messages: {
            loadFailed: "Failed loading AI models.",
            saveFailed: "Failed saving AI model.",
            deleteFailed: "Failed deleting AI model.",
            deleted: "AI model deleted.",
            saved: "AI model saved."
        },
        clearForm: function () {
            $("#modelId").val("");
            $("#modelName").val("");
            $("#modelDescription").val("");
            $("#modelTokensPerMinute").val("");
            $("#modelRequestsPerMinute").val(0);
            $("#modelRequestsPerDay").val(0);
            $("#modelApiKey").val("");
            $("#modelEnabled").prop("checked", true);
            updateDescriptionCounter();
        },
        fillForm: function (model) {
            $("#modelId").val(model.id);
            $("#modelName").val(model.name);
            $("#modelDescription").val(model.description || "");
            $("#modelTokensPerMinute").val(model.tokensPerMinute);
            $("#modelRequestsPerMinute").val(model.requestsPerMinute);
            $("#modelRequestsPerDay").val(model.requestsPerDay);
            $("#modelApiKey").val(model.apiKey);
            $("#modelEnabled").prop("checked", model.enabled);
            updateDescriptionCounter();
        },
        buildPayload: function () {
            return {
                name: $("#modelName").val().trim(),
                description: $("#modelDescription").val().trim(),
                enabled: $("#modelEnabled").is(":checked"),
                tokensPerMinute: Number($("#modelTokensPerMinute").val()),
                requestsPerMinute: Number($("#modelRequestsPerMinute").val()),
                requestsPerDay: Number($("#modelRequestsPerDay").val()),
                apiKey: $("#modelApiKey").val().trim()
            };
        },
        validateSave: function () {
            const name = $("#modelName").val().trim();
            const description = $("#modelDescription").val();
            const apiKey = $("#modelApiKey").val().trim();
            const tokensPerMinute = Number($("#modelTokensPerMinute").val());
            const requestsPerMinute = Number($("#modelRequestsPerMinute").val());
            const requestsPerDay = Number($("#modelRequestsPerDay").val());
            if (!name) {
                return "Model name is required.";
            }
            if (description.length > descriptionMaxLength) {
                return "Description cannot exceed " + descriptionMaxLength + " characters.";
            }
            if (!Number.isFinite(tokensPerMinute) || tokensPerMinute < 1) {
                return "Tokens per minute must be at least 1.";
            }
            if (!Number.isFinite(requestsPerMinute) || requestsPerMinute < 0) {
                return "Requests per minute must be 0 or greater.";
            }
            if (!Number.isFinite(requestsPerDay) || requestsPerDay < 0) {
                return "Requests per day must be 0 or greater.";
            }
            if (!apiKey) {
                return "API key is required.";
            }
            return null;
        },
        renderColumns: function (model) {
            return [
                $("<td></td>").text(model.name),
                $("<td></td>").text(model.description || ""),
                $("<td></td>").text(model.enabled ? "Yes" : "No"),
                $("<td></td>").text(model.tokensPerMinute),
                $("<td></td>").text(model.requestsPerMinute),
                $("<td></td>").text(model.requestsPerDay)
            ];
        }
    });
});
