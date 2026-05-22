$(function () {
    function showAlert(message, isError) {
        const alert = $("#modelsAlert");
        alert.removeClass("d-none alert-success alert-danger")
            .addClass(isError ? "alert-danger" : "alert-success")
            .text(message);
    }

    function clearForm() {
        $("#modelId").val("");
        $("#modelName").val("");
        $("#modelTokensPerMinute").val("");
        $("#modelRequestsPerMinute").val(0);
        $("#modelRequestsPerDay").val(0);
        $("#modelApiKey").val("");
        $("#modelEnabled").prop("checked", true);
    }

    function rowActions(model) {
        const actions = $("<td></td>");
        const editBtn = $("<button class='btn btn-sm btn-outline-primary me-2'>Edit</button>");
        const deleteBtn = $("<button class='btn btn-sm btn-outline-danger'>Delete</button>");
        editBtn.on("click", function () {
            $("#modelId").val(model.id);
            $("#modelName").val(model.name);
            $("#modelTokensPerMinute").val(model.tokensPerMinute);
            $("#modelRequestsPerMinute").val(model.requestsPerMinute);
            $("#modelRequestsPerDay").val(model.requestsPerDay);
            $("#modelApiKey").val(model.apiKey);
            $("#modelEnabled").prop("checked", model.enabled);
        });
        deleteBtn.on("click", function () {
            if (!window.confirm("Delete selected AI model?")) {
                return;
            }
            $.ajax({
                url: `/api/ai-models/${model.id}`,
                method: "DELETE"
            }).done(function () {
                showAlert("AI model deleted.", false);
                loadModels();
                clearForm();
            }).fail(function (xhr) {
                const message = xhr.responseJSON?.message || "Failed deleting AI model.";
                showAlert(message, true);
            });
        });
        actions.append(editBtn).append(deleteBtn);
        return actions;
    }

    function renderTable(models) {
        const tbody = $("#modelsTableBody");
        tbody.empty();
        models.forEach(function (model) {
            const row = $("<tr></tr>");
            row.append($("<td></td>").text(model.name));
            row.append($("<td></td>").text(model.enabled ? "Yes" : "No"));
            row.append($("<td></td>").text(model.tokensPerMinute));
            row.append($("<td></td>").text(model.requestsPerMinute));
            row.append($("<td></td>").text(model.requestsPerDay));
            row.append(rowActions(model));
            tbody.append(row);
        });
    }

    function loadModels() {
        $.get("/api/ai-models")
            .done(function (response) {
                renderTable(response.data || []);
            })
            .fail(function (xhr) {
                const message = xhr.responseJSON?.message || "Failed loading AI models.";
                showAlert(message, true);
            });
    }

    $("#saveModelBtn").on("click", function () {
        const modelId = $("#modelId").val();
        const payload = {
            name: $("#modelName").val(),
            enabled: $("#modelEnabled").is(":checked"),
            tokensPerMinute: Number($("#modelTokensPerMinute").val()),
            requestsPerMinute: Number($("#modelRequestsPerMinute").val()),
            requestsPerDay: Number($("#modelRequestsPerDay").val()),
            apiKey: $("#modelApiKey").val()
        };
        const method = modelId ? "PUT" : "POST";
        const endpoint = modelId ? `/api/ai-models/${modelId}` : "/api/ai-models";
        $.ajax({
            url: endpoint,
            method: method,
            contentType: "application/json",
            data: JSON.stringify(payload)
        }).done(function (response) {
            showAlert(response.message || "AI model saved.", false);
            loadModels();
            clearForm();
        }).fail(function (xhr) {
            const message = xhr.responseJSON?.message || "Failed saving AI model.";
            showAlert(message, true);
        });
    });

    $("#resetModelBtn").on("click", function () {
        clearForm();
    });

    loadModels();
});
