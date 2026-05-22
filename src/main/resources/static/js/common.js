(function (global) {
    const CodeAtlas = {};
    const TOAST_CONTAINER_ID = "globalToastContainer";

    const TOAST_TYPE_CLASSES = {
        success: "bg-success text-white",
        error: "bg-danger text-white",
        danger: "bg-danger text-white",
        warning: "bg-warning text-dark",
        info: "bg-info text-white"
    };

    CodeAtlas.ensureToastContainer = function () {
        if ($("#" + TOAST_CONTAINER_ID).length === 0) {
            $("body").append(
                `<div class="toast-container position-fixed bottom-0 end-0 p-3" id="${TOAST_CONTAINER_ID}" style="z-index: 1055;"></div>`
            );
        }
    };

    CodeAtlas.showToast = function (message, type) {
        if (!message) {
            return;
        }
        CodeAtlas.ensureToastContainer();
        const normalizedType = String(type || "info").toLowerCase();
        const bgClass = TOAST_TYPE_CLASSES[normalizedType] || TOAST_TYPE_CLASSES.info;
        const isWarning = normalizedType === "warning";
        const closeBtnClass = isWarning ? "btn-close" : "btn-close btn-close-white";
        const toastId = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;

        const $toast = $(`
            <div class="toast align-items-center ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="5000" id="${toastId}">
              <div class="d-flex">
                <div class="toast-body"></div>
                <button type="button" class="${closeBtnClass} me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
              </div>
            </div>
        `);
        $toast.find(".toast-body").text(message);

        const $container = $("#" + TOAST_CONTAINER_ID);
        $container.append($toast);

        const toast = bootstrap.Toast.getOrCreateInstance($toast[0]);
        $toast.on("hidden.bs.toast", function () {
            $toast.remove();
        });
        toast.show();
    };

    CodeAtlas.apiMessage = function (xhr, fallback) {
        return xhr.responseJSON?.message || fallback;
    };

    CodeAtlas.setButtonLoading = function ($button, isLoading, loadingText) {
        const text = loadingText || "Processing...";
        if (isLoading) {
            if (!$button.data("original-html")) {
                $button.data("original-html", $button.html());
            }
            $button.prop("disabled", true);
            $button.html(
                `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>${text}`
            );
            return;
        }
        const originalHtml = $button.data("original-html");
        if (originalHtml) {
            $button.html(originalHtml);
        }
        $button.prop("disabled", false);
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
                CodeAtlas.showToast(message, isError ? "danger" : "success");
            }

            function loadList() {
                CodeAtlas.apiGet(config.apiBase)
                    .done(function (response) {
                        const items = response.data || [];
                        renderTable(items);
                        if (typeof config.onListLoaded === "function") {
                            config.onListLoaded(items);
                        }
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
                const $saveBtn = $(this);
                const validationError = config.validateSave();
                if (validationError) {
                    show(validationError, true);
                    return;
                }
                const entityId = $(config.idFieldSelector).val();
                const payload = config.buildPayload();
                const method = entityId ? "PUT" : "POST";
                const endpoint = entityId ? `${config.apiBase}/${entityId}` : config.apiBase;
                const loadingText = config.saveLoadingText || "Saving...";
                CodeAtlas.setButtonLoading($saveBtn, true, loadingText);
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
                }).always(function () {
                    CodeAtlas.setButtonLoading($saveBtn, false);
                });
            });

            $(config.resetBtnSelector).on("click", function () {
                config.clearForm();
            });

            loadList();
        });
    };

    $(function () {
        CodeAtlas.ensureToastContainer();
    });

    global.CodeAtlas = CodeAtlas;
})(window);
