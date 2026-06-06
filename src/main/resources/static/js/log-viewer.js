(function (global) {
    const CodeAtlas = global.CodeAtlas || {};
    const POLL_FAST_MS = 3000;
    const POLL_SLOW_MS = 15000;
    const MAX_DOM_LINES = 500;
    const AUTO_SCROLL_THRESHOLD_PX = 40;
    const TAIL_LINE_COUNT = 500;

    const HIGHLIGHT_RULES = [
        { pattern: /\b(FATAL|ERROR)\b/g, className: "text-danger fw-bold" },
        { pattern: /\bWARN\b/g, className: "text-warning" },
        { pattern: /\bINFO\b/g, className: "text-info" }
    ];

    function escapeHtml(text) {
        return String(text)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function highlightLine(text) {
        let html = escapeHtml(text);
        HIGHLIGHT_RULES.forEach(function (rule) {
            html = html.replace(rule.pattern, function (match) {
                return `<span class="${rule.className}">${match}</span>`;
            });
        });
        return html;
    }

    CodeAtlas.initLogViewer = function () {
        const $modal = $("#logViewerModal");
        if ($modal.length === 0) {
            return;
        }

        const $output = $("#logViewerOutput");
        const $filter = $("#logViewerFilter");
        const $pauseBtn = $("#logViewerPauseBtn");
        const $clearBtn = $("#logViewerClearBtn");
        const $pollStatus = $("#logViewerPollStatus");
        const $spinner = $("#logActivitySpinner");
        const $progressWrap = $("#logViewerProgressWrap");
        const $progressBar = $("#logViewerProgressBar");

        let modalOpen = false;
        let paused = false;
        let fastPollTimer = null;
        let slowPollTimer = null;
        let lastRunning = false;

        function isNearBottom() {
            const element = $output[0];
            if (!element) {
                return true;
            }
            return element.scrollHeight - element.scrollTop - element.clientHeight <= AUTO_SCROLL_THRESHOLD_PX;
        }

        function applyFilter() {
            const query = String($filter.val() || "").trim().toLowerCase();
            $output.find(".log-viewer-line").each(function () {
                const $line = $(this);
                const text = String($line.data("text") || "").toLowerCase();
                const visible = !query || text.indexOf(query) !== -1;
                $line.toggle(visible);
            });
        }

        function updateNavbarState(running) {
            lastRunning = running;
            $spinner.toggleClass("d-none", !running);
        }

        function updateProgress(progressPercent) {
            if (typeof progressPercent !== "number" || progressPercent < 0) {
                $progressWrap.addClass("d-none");
                $progressBar.css("width", "0%").text("0%");
                return;
            }
            const rounded = Math.max(0, Math.min(100, Math.round(progressPercent)));
            $progressWrap.removeClass("d-none");
            $progressBar.css("width", rounded + "%").text(rounded + "%");
        }

        function renderLines(lines) {
            const stickToBottom = isNearBottom();
            $output.empty();
            const visibleLines = (lines || []).slice(-MAX_DOM_LINES);
            visibleLines.forEach(function (line) {
                const $line = $("<div class='log-viewer-line'></div>");
                $line.attr("data-text", line);
                $line.html(highlightLine(line));
                $output.append($line);
            });
            applyFilter();
            if (stickToBottom && !paused) {
                $output.scrollTop($output[0].scrollHeight);
            }
        }

        function setPollStatus(text) {
            $pollStatus.text(text);
        }

        function fetchTail(updateConsole) {
            return CodeAtlas.apiGet("/api/logs/tail?lines=" + TAIL_LINE_COUNT)
                .done(function (response) {
                    const data = response.data || {};
                    updateNavbarState(Boolean(data.running));
                    if (modalOpen) {
                        updateProgress(data.progressPercent);
                    }
                    if (updateConsole && modalOpen && !paused) {
                        renderLines(data.lines || []);
                    }
                })
                .fail(function () {
                    if (updateConsole && modalOpen && !paused) {
                        return;
                    }
                });
        }

        function stopFastPoll() {
            if (fastPollTimer !== null) {
                clearInterval(fastPollTimer);
                fastPollTimer = null;
            }
        }

        function startFastPoll() {
            stopFastPoll();
            if (paused) {
                setPollStatus("Stream paused");
                return;
            }
            setPollStatus("Refreshing every 3s");
            fetchTail(true);
            fastPollTimer = setInterval(function () {
                fetchTail(true);
            }, POLL_FAST_MS);
        }

        function startSlowPoll() {
            if (slowPollTimer !== null) {
                return;
            }
            setPollStatus("Refreshing every 15s");
            fetchTail(false);
            slowPollTimer = setInterval(function () {
                fetchTail(false);
            }, POLL_SLOW_MS);
        }

        $modal.on("show.bs.modal", function () {
            modalOpen = true;
            if (!paused) {
                startFastPoll();
            } else {
                setPollStatus("Stream paused");
            }
            fetchTail(true);
        });

        $modal.on("hidden.bs.modal", function () {
            modalOpen = false;
            stopFastPoll();
            setPollStatus("Refreshing every 15s");
        });

        $pauseBtn.on("click", function () {
            paused = !paused;
            if (paused) {
                $pauseBtn.text("Resume Stream");
                stopFastPoll();
                setPollStatus("Stream paused");
                return;
            }
            $pauseBtn.text("Pause Stream");
            if (modalOpen) {
                startFastPoll();
            }
        });

        $clearBtn.on("click", function () {
            $output.empty();
        });

        $filter.on("keyup input", function () {
            applyFilter();
        });

        startSlowPoll();
    };

    $(function () {
        CodeAtlas.initLogViewer();
    });

    global.CodeAtlas = CodeAtlas;
})(window);
