(function (global) {
    const CodeAtlas = {};

    CodeAtlas.showAlert = function (selector, message, isError) {
        const alert = $(selector);
        alert.removeClass("d-none alert-success alert-danger")
            .addClass(isError ? "alert-danger" : "alert-success")
            .text(message);
    };

    CodeAtlas.apiMessage = function (xhr, fallback) {
        return xhr.responseJSON?.message || fallback;
    };

    CodeAtlas.apiGet = function (url) {
        return $.ajax({
            url: url,
            method: "GET",
            dataType: "json"
        });
    };

    CodeAtlas.copyToClipboard = function (text) {
        const textarea = document.createElement("textarea");
        textarea.value = text;
        textarea.setAttribute("readonly", "");
        textarea.style.position = "absolute";
        textarea.style.left = "-9999px";
        document.body.appendChild(textarea);
        textarea.select();
        try {
            const copied = document.execCommand("copy");
            document.body.removeChild(textarea);
            return copied;
        } catch (error) {
            document.body.removeChild(textarea);
            return false;
        }
    };

    CodeAtlas.initCrudPage = function (config) {
        $(function () {
            function show(message, isError) {
                CodeAtlas.showAlert(config.alertSelector, message, isError);
            }

            function loadList() {
                CodeAtlas.apiGet(config.apiBase)
                    .done(function (response) {
                        renderTable(response.data || []);
                    })
                    .fail(function (xhr) {
                        show(CodeAtlas.apiMessage(xhr, config.messages.loadFailed), true);
                    });
            }

            function renderTable(items) {
                const tbody = $(config.tableBodySelector);
                tbody.empty();
                items.forEach(function (item) {
                    const row = $("<tr></tr>");
                    config.renderColumns(item).forEach(function (cell) {
                        row.append(cell);
                    });
                    row.append(buildActions(item));
                    tbody.append(row);
                });
            }

            function buildActions(item) {
                const actions = $("<td></td>");
                const editBtn = $("<button class='btn btn-sm btn-outline-primary me-2'>Edit</button>");
                const deleteBtn = $("<button class='btn btn-sm btn-outline-danger'>Delete</button>");
                editBtn.on("click", function () {
                    config.fillForm(item);
                });
                deleteBtn.on("click", function () {
                    if (!window.confirm(config.deleteConfirmMessage)) {
                        return;
                    }
                    $.ajax({
                        url: `${config.apiBase}/${item.id}`,
                        method: "DELETE"
                    }).done(function () {
                        show(config.messages.deleted, false);
                        loadList();
                        config.clearForm();
                    }).fail(function (xhr) {
                        show(CodeAtlas.apiMessage(xhr, config.messages.deleteFailed), true);
                    });
                });
                actions.append(editBtn).append(deleteBtn);
                return actions;
            }

            $(config.saveBtnSelector).on("click", function () {
                const validationError = config.validateSave();
                if (validationError) {
                    show(validationError, true);
                    return;
                }
                const entityId = $(config.idFieldSelector).val();
                const payload = config.buildPayload();
                const method = entityId ? "PUT" : "POST";
                const endpoint = entityId ? `${config.apiBase}/${entityId}` : config.apiBase;
                $.ajax({
                    url: endpoint,
                    method: method,
                    contentType: "application/json",
                    data: JSON.stringify(payload)
                }).done(function (response) {
                    show(response.message || config.messages.saved, false);
                    loadList();
                    config.clearForm();
                }).fail(function (xhr) {
                    show(CodeAtlas.apiMessage(xhr, config.messages.saveFailed), true);
                });
            });

            $(config.resetBtnSelector).on("click", function () {
                config.clearForm();
            });

            loadList();
        });
    };

    global.CodeAtlas = CodeAtlas;
})(window);
