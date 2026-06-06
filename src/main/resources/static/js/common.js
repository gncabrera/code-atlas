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

    function dismissibleAlertsApi() {
        return global.CodeAtlasDismissibleAlerts || null;
    }

    CodeAtlas.initDismissibleAlerts = function () {
        const api = dismissibleAlertsApi();
        const dismissed = api ? api.readDismissedAlerts() : {};
        $("[data-alert-id]").each(function () {
            const $alert = $(this);
            const alertElement = $alert[0];
            const alertId = String($alert.data("alert-id") || "").trim();
            if (!alertId) {
                return;
            }
            const shouldPersist = api ? api.shouldPersistAlert(alertElement) : true;
            if (shouldPersist && dismissed[alertId]) {
                $alert.remove();
                return;
            }
            $alert.on("closed.bs.alert", function () {
                if (shouldPersist && api) {
                    api.writeDismissedAlert(alertId);
                }
            });
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

    const MARKED_CDN_URL = "https://cdn.jsdelivr.net/npm/marked/marked.min.js";
    let markedLoadState = "idle";
    let markedPreloadStarted = false;
    let markedConfigured = false;
    const markedLoadCallbacks = [];

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function configureMarkedParser() {
        if (markedConfigured || typeof global.marked === "undefined") {
            return;
        }
        markedConfigured = true;
        const htmlRenderer = function (token) {
            const raw = token && typeof token === "object" ? token.text : token;
            return escapeHtml(raw || "");
        };
        if (typeof global.marked.use === "function") {
            global.marked.use({
                breaks: true,
                gfm: true,
                renderer: {
                    html: htmlRenderer
                }
            });
            return;
        }
        const renderer = new global.marked.Renderer();
        renderer.html = function (html) {
            return escapeHtml(html);
        };
        global.marked.setOptions({
            breaks: true,
            gfm: true,
            renderer: renderer
        });
    }

    function ensureMarkedLibrary(callback) {
        if (typeof global.marked !== "undefined") {
            markedLoadState = "ready";
            if (callback) {
                callback(null);
            }
            return;
        }
        if (callback) {
            markedLoadCallbacks.push(callback);
        }
        if (markedLoadState === "loading") {
            return;
        }
        if (markedLoadState === "failed") {
            if (callback) {
                callback(new Error("Failed to load markdown library from CDN."));
            }
            return;
        }
        markedLoadState = "loading";
        const script = document.createElement("script");
        script.src = MARKED_CDN_URL;
        script.type = "text/javascript";
        script.onload = function () {
            configureMarkedParser();
            markedLoadState = "ready";
            const callbacks = markedLoadCallbacks.splice(0);
            callbacks.forEach(function (cb) {
                cb(null);
            });
            setMarkdownPreviewButtonsEnabled(true);
        };
        script.onerror = function () {
            markedLoadState = "failed";
            const callbacks = markedLoadCallbacks.splice(0);
            callbacks.forEach(function (cb) {
                cb(new Error("Failed to load markdown library from CDN."));
            });
            setMarkdownPreviewButtonsEnabled(true);
        };
        document.head.appendChild(script);
    }

    function setMarkdownPreviewButtonsEnabled(isEnabled) {
        $(".markdown-preview-container .btn-preview-mode").prop("disabled", !isEnabled);
    }

    function preloadMarkedLibrary() {
        if (markedPreloadStarted) {
            return;
        }
        markedPreloadStarted = true;
        setMarkdownPreviewButtonsEnabled(false);
        ensureMarkedLibrary();
    }

    function renderMarkdownPreviewPane($previewPane, rawMarkdown) {
        if (markedLoadState === "failed" || typeof global.marked === "undefined") {
            $previewPane.html(
                $('<div class="alert alert-danger p-2 m-0"></div>').text(
                    "Failed to load markdown library from CDN."
                )
            );
            return;
        }
        try {
            configureMarkedParser();
            const parsedHtml = global.marked.parse(rawMarkdown || "");
            if (!parsedHtml || !String(parsedHtml).trim()) {
                $previewPane.html('<p class="text-muted fst-italic">Nothing to preview...</p>');
                return;
            }
            $previewPane.html(parsedHtml);
        } catch (error) {
            const message = error && error.message ? error.message : "Unknown error";
            $previewPane.html(
                $('<div class="alert alert-danger p-2 m-0"></div>').text(
                    "Failed parsing markdown: " + message
                )
            );
        }
    }

    CodeAtlas.initMarkdownPreview = function (selectorOrElement) {
        const $targets = selectorOrElement instanceof $
            ? selectorOrElement
            : $(selectorOrElement);
        let initializedAny = false;

        $targets.each(function () {
            const $textarea = $(this);
            if (!$textarea.is("textarea") || $textarea.data("md-initialized") === true) {
                return;
            }
            $textarea.data("md-initialized", true);
            initializedAny = true;

            const $container = $('<div class="markdown-preview-container d-flex flex-column gap-2"></div>');
            $textarea.before($container);

            const $toolbar = $(`
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <div class="btn-group btn-group-sm" role="group" aria-label="Editor Mode">
                        <button type="button" class="btn btn-outline-primary active btn-edit-mode">Edit</button>
                        <button type="button" class="btn btn-outline-primary btn-preview-mode" disabled>Preview</button>
                    </div>
                    <small class="text-muted"><i class="bi bi-markdown me-1"></i>Markdown Editor</small>
                </div>
            `);
            const $previewPane = $('<div class="markdown-preview-pane d-none"></div>');

            $container.append($toolbar);
            $textarea.detach().appendTo($container);
            $container.append($previewPane);

            const $btnEdit = $toolbar.find(".btn-edit-mode");
            const $btnPreview = $toolbar.find(".btn-preview-mode");

            $btnEdit.on("click", function () {
                $btnPreview.removeClass("active");
                $btnEdit.addClass("active");
                $previewPane.addClass("d-none");
                $textarea.removeClass("d-none").trigger("focus");
            });

            $btnPreview.on("click", function () {
                if ($btnPreview.prop("disabled")) {
                    return;
                }
                renderMarkdownPreviewPane($previewPane, $textarea.val() || "");
                $btnEdit.removeClass("active");
                $btnPreview.addClass("active");
                $textarea.addClass("d-none");
                $previewPane.removeClass("d-none");
            });

            if (markedLoadState === "ready" || markedLoadState === "failed") {
                $btnPreview.prop("disabled", false);
            }
        });

        if (initializedAny && markedLoadState === "idle") {
            preloadMarkedLibrary();
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
        CodeAtlas.initDismissibleAlerts();
        CodeAtlas.initMarkdownPreview(".md-preview");
    });

    global.CodeAtlas = CodeAtlas;
})(window);
