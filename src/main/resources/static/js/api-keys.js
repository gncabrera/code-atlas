$(function () {
    const apiBase = "/api/api-keys";

    function show(message, isError) {
        CodeAtlas.showToast(message, isError ? "danger" : "success");
    }

    function loadList() {
        CodeAtlas.apiGet(apiBase)
            .done(function (response) {
                renderTable(response.data || []);
            })
            .fail(function (xhr) {
                show(CodeAtlas.apiMessage(xhr, "Failed loading API keys."), true);
            });
    }

    function renderTable(items) {
        const tbody = $("#apiKeysTableBody");
        tbody.empty();
        items.forEach(function (item) {
            const row = $("<tr></tr>");
            row.append($("<td></td>").text(item.name));
            row.append($("<td></td>").text(item.provider));
            row.append($("<td></td>").text(item.apiKey));
            row.append($("<td></td>").text(item.isActive ? "Yes" : "No"));
            row.append(buildActions(item));
            tbody.append(row);
        });
    }

    function buildActions(item) {
        const actions = $("<td></td>");
        const editBtn = $("<button class='btn btn-sm btn-outline-primary me-2'>Edit</button>");
        const deleteBtn = $("<button class='btn btn-sm btn-outline-danger'>Delete</button>");
        editBtn.on("click", function () {
            fillForm(item);
        });
        deleteBtn.on("click", function () {
            if (!window.confirm("Delete selected API key?")) {
                return;
            }
            CodeAtlas.setButtonLoading(deleteBtn, true, "Deleting...");
            $.ajax({
                url: `${apiBase}/${item.id}`,
                method: "DELETE"
            }).done(function () {
                show("API key deleted.", false);
                loadList();
                clearForm();
            }).fail(function (xhr) {
                show(CodeAtlas.apiMessage(xhr, "Failed deleting API key."), true);
            }).always(function () {
                CodeAtlas.setButtonLoading(deleteBtn, false);
            });
        });
        actions.append(editBtn).append(deleteBtn);
        return actions;
    }

    function clearForm() {
        $("#apiKeyId").val("");
        $("#apiKeyName").val("");
        $("#apiKeyProvider").val("");
        $("#apiKeyValue").val("");
        $("#apiKeyActive").prop("checked", true);
    }

    function fillForm(item) {
        $("#apiKeyId").val(item.id);
        $("#apiKeyName").val(item.name);
        $("#apiKeyProvider").val(item.provider);
        $("#apiKeyValue").val(item.apiKey);
        $("#apiKeyActive").prop("checked", item.isActive);
    }

    function validateSave() {
        const name = $("#apiKeyName").val().trim();
        const provider = $("#apiKeyProvider").val().trim();
        const apiKey = $("#apiKeyValue").val().trim();
        if (!name) {
            return "Name is required.";
        }
        if (!provider) {
            return "Provider is required.";
        }
        if (!apiKey) {
            return "API key value is required.";
        }
        return null;
    }

    function buildPayload() {
        return {
            name: $("#apiKeyName").val().trim(),
            provider: $("#apiKeyProvider").val().trim(),
            apiKey: $("#apiKeyValue").val().trim(),
            isActive: $("#apiKeyActive").is(":checked")
        };
    }

    $("#saveApiKeyBtn").on("click", function () {
        const $saveBtn = $(this);
        const validationError = validateSave();
        if (validationError) {
            show(validationError, true);
            return;
        }
        const entityId = $("#apiKeyId").val();
        const payload = buildPayload();
        const method = entityId ? "PUT" : "POST";
        const endpoint = entityId ? `${apiBase}/${entityId}` : apiBase;
        CodeAtlas.setButtonLoading($saveBtn, true, "Saving API Key...");
        $.ajax({
            url: endpoint,
            method: method,
            contentType: "application/json",
            data: JSON.stringify(payload)
        }).done(function (response) {
            show(response.message || "API key saved.", false);
            loadList();
            clearForm();
        }).fail(function (xhr) {
            show(CodeAtlas.apiMessage(xhr, "Failed saving API key."), true);
        }).always(function () {
            CodeAtlas.setButtonLoading($saveBtn, false);
        });
    });

    $("#resetApiKeyBtn").on("click", clearForm);

    loadList();
});
