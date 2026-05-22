$(function () {
    const TRUNCATE_LENGTH = 80;
    const API_URL = "/api/prompt-history";

    let allItems = [];
    let filteredItems = [];
    let currentPage = 1;
    let pageSize = 25;

    const $alert = $("#promptHistoryAlert");
    const $tbody = $("#promptHistoryTableBody");
    const $search = $("#promptHistorySearch");
    const $pageSize = $("#promptHistoryPageSize");
    const $prevBtn = $("#promptHistoryPrevBtn");
    const $nextBtn = $("#promptHistoryNextBtn");
    const $pageLabel = $("#promptHistoryPageLabel");
    const $paginationInfo = $("#promptHistoryPaginationInfo");
    const detailModal = new bootstrap.Modal(document.getElementById("promptHistoryDetailModal"));

    function showAlert(message, isError) {
        CodeAtlas.showAlert("#promptHistoryAlert", message, isError);
    }

    function truncateText(text) {
        if (!text) {
            return "";
        }
        const normalized = String(text);
        if (normalized.length <= TRUNCATE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, TRUNCATE_LENGTH) + "…";
    }

    function formatDate(isoString) {
        if (!isoString) {
            return "—";
        }
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) {
            return isoString;
        }
        return date.toLocaleString();
    }

    function projectLabel(item) {
        if (item.projectName) {
            return item.projectName;
        }
        if (item.projectId != null) {
            return "Project #" + item.projectId;
        }
        return "—";
    }

    function matchesSearch(item, query) {
        if (!query) {
            return true;
        }
        const haystack = [
            item.projectName,
            item.aiModelName,
            item.requestPrompt,
            item.responsePrompt,
            item.mode,
            item.status,
            item.errorMessage
        ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();
        return haystack.includes(query);
    }

    function applyFilter() {
        const query = $search.val().trim().toLowerCase();
        filteredItems = allItems.filter(function (item) {
            return matchesSearch(item, query);
        });
        currentPage = 1;
        renderTable();
    }

    function totalPages() {
        if (filteredItems.length === 0) {
            return 0;
        }
        return Math.ceil(filteredItems.length / pageSize);
    }

    function pageItems() {
        const start = (currentPage - 1) * pageSize;
        return filteredItems.slice(start, start + pageSize);
    }

    function updatePaginationControls() {
        const pages = totalPages();
        $prevBtn.prop("disabled", currentPage <= 1);
        $nextBtn.prop("disabled", pages === 0 || currentPage >= pages);
        if (pages === 0) {
            $pageLabel.text("No matching records");
            $paginationInfo.text(filteredItems.length === 0 && allItems.length === 0
                ? "0 records"
                : "0 of " + filteredItems.length + " matching");
            return;
        }
        $pageLabel.text("Page " + currentPage + " of " + pages);
        const start = (currentPage - 1) * pageSize + 1;
        const end = Math.min(currentPage * pageSize, filteredItems.length);
        $paginationInfo.text(
            "Showing " + start + "–" + end + " of " + filteredItems.length +
            (filteredItems.length !== allItems.length ? " (filtered from " + allItems.length + ")" : "")
        );
    }

    function buildTextCell(text, item, fieldLabel) {
        const display = truncateText(text);
        const $cell = $("<td></td>");
        if (!text) {
            $cell.text("—");
            return $cell;
        }
        const $span = $("<span class='prompt-history-cell-text d-inline-block'></span>").text(display);
        const $btn = $("<button type='button' class='btn btn-link btn-sm p-0 ms-1'>View</button>");
        $btn.on("click", function () {
            openDetailModal(item, fieldLabel);
        });
        $cell.append($span).append($btn);
        return $cell;
    }

    function statusBadgeClass(status) {
        if (!status) {
            return "bg-secondary";
        }
        const normalized = status.toLowerCase();
        if (normalized === "success" || normalized === "completed") {
            return "bg-success";
        }
        if (normalized === "error" || normalized === "failed") {
            return "bg-danger";
        }
        return "bg-secondary";
    }

    function openDetailModal(item, focusField) {
        const title = "Record #" + item.id + (focusField ? " — " + focusField : "");
        $("#promptHistoryDetailModalLabel").text(title);
        $("#promptHistoryDetailMeta").html(
            "<dt class='col-sm-3'>Date</dt><dd class='col-sm-9'>" + formatDate(item.createdAt) + "</dd>" +
            "<dt class='col-sm-3'>Project</dt><dd class='col-sm-9'>" + escapeHtml(projectLabel(item)) + "</dd>" +
            "<dt class='col-sm-3'>AI Model</dt><dd class='col-sm-9'>" + escapeHtml(item.aiModelName || "—") + "</dd>" +
            "<dt class='col-sm-3'>Mode</dt><dd class='col-sm-9'>" + escapeHtml(item.mode || "—") + "</dd>" +
            "<dt class='col-sm-3'>Status</dt><dd class='col-sm-9'>" + escapeHtml(item.status || "—") + "</dd>" +
            "<dt class='col-sm-3'>Tokens</dt><dd class='col-sm-9'>" + item.estimatedTokens + "</dd>" +
            "<dt class='col-sm-3'>Agents file</dt><dd class='col-sm-9'>" +
            (item.shouldSendAgentsFile ? "Yes" : "No") + "</dd>"
        );
        $("#promptHistoryDetailRequest").text(item.requestPrompt || "");
        $("#promptHistoryDetailResponse").text(item.responsePrompt || "(no response)");
        if (item.errorMessage) {
            $("#promptHistoryDetailErrorSection").removeClass("d-none");
            $("#promptHistoryDetailError").text(item.errorMessage);
        } else {
            $("#promptHistoryDetailErrorSection").addClass("d-none");
            $("#promptHistoryDetailError").text("");
        }
        detailModal.show();
    }

    function escapeHtml(value) {
        return $("<span></span>").text(value).html();
    }

    function renderTable() {
        $tbody.empty();
        const items = pageItems();
        if (items.length === 0) {
            const message = allItems.length === 0
                ? "No prompt history records yet."
                : "No records match your search.";
            $tbody.append(
                $("<tr></tr>").append(
                    $("<td colspan='11' class='text-center text-muted py-4'></td>").text(message)
                )
            );
            updatePaginationControls();
            return;
        }

        items.forEach(function (item) {
            const $row = $("<tr></tr>");
            $row.append($("<td class='prompt-history-id'></td>").text("#" + item.id));
            $row.append($("<td class='text-nowrap'></td>").text(formatDate(item.createdAt)));
            $row.append($("<td></td>").text(projectLabel(item)));
            $row.append($("<td></td>").text(item.aiModelName || "—"));
            $row.append($("<td></td>").text(item.mode || "—"));
            const $status = $("<td></td>");
            $status.append(
                $("<span class='badge'></span>")
                    .addClass(statusBadgeClass(item.status))
                    .text(item.status || "—")
            );
            $row.append($status);
            $row.append($("<td></td>").text(item.shouldSendAgentsFile ? "Yes" : "No"));
            $row.append($("<td></td>").text(item.estimatedTokens));
            $row.append(buildTextCell(item.requestPrompt, item, "Request"));
            $row.append(buildTextCell(item.responsePrompt, item, "Response"));
            $row.append(buildTextCell(item.errorMessage, item, "Error"));
            $tbody.append($row);
        });
        updatePaginationControls();
    }

    function loadHistory() {
        $tbody.html(
            "<tr><td colspan='11' class='text-center py-4'>" +
            "<span class='spinner-border spinner-border-sm me-2'></span>Loading prompt history...</td></tr>"
        );
        CodeAtlas.apiGet(API_URL)
            .done(function (response) {
                allItems = response.data || [];
                applyFilter();
            })
            .fail(function (xhr) {
                allItems = [];
                filteredItems = [];
                $tbody.empty();
                showAlert(CodeAtlas.apiMessage(xhr, "Failed loading prompt history."), true);
                updatePaginationControls();
            });
    }

    $search.on("input", function () {
        applyFilter();
    });

    $pageSize.on("change", function () {
        pageSize = parseInt($pageSize.val(), 10) || 25;
        currentPage = 1;
        renderTable();
    });

    $prevBtn.on("click", function () {
        if (currentPage > 1) {
            currentPage -= 1;
            renderTable();
        }
    });

    $nextBtn.on("click", function () {
        if (currentPage < totalPages()) {
            currentPage += 1;
            renderTable();
        }
    });

    loadHistory();
});
