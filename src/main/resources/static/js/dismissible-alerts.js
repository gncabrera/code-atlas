(function (global) {
    const DISMISSED_ALERTS_KEY = "codeAtlas.dismissedAlerts";
    const STYLE_ELEMENT_ID = "codeAtlas-dismissed-alerts-style";

    function readDismissedAlerts() {
        try {
            const raw = localStorage.getItem(DISMISSED_ALERTS_KEY);
            if (!raw) {
                return {};
            }
            const parsed = JSON.parse(raw);
            return parsed && typeof parsed === "object" ? parsed : {};
        } catch (error) {
            return {};
        }
    }

    function cssEscapeAlertId(alertId) {
        return String(alertId).replace(/\\/g, "\\\\").replace(/"/g, '\\"');
    }

    function applyDismissedAlertStyles() {
        const dismissed = readDismissedAlerts();
        let css = "";
        Object.keys(dismissed).forEach(function (alertId) {
            if (dismissed[alertId]) {
                css += '[data-alert-id="' + cssEscapeAlertId(alertId) + '"]{display:none!important}';
            }
        });
        let styleEl = document.getElementById(STYLE_ELEMENT_ID);
        if (!css) {
            if (styleEl) {
                styleEl.remove();
            }
            return;
        }
        if (!styleEl) {
            styleEl = document.createElement("style");
            styleEl.id = STYLE_ELEMENT_ID;
            document.head.appendChild(styleEl);
        }
        styleEl.textContent = css;
    }

    function writeDismissedAlert(alertId) {
        const dismissed = readDismissedAlerts();
        dismissed[alertId] = true;
        localStorage.setItem(DISMISSED_ALERTS_KEY, JSON.stringify(dismissed));
        applyDismissedAlertStyles();
    }

    function shouldPersistAlert(element) {
        const persistValue = element.getAttribute("data-alert-persist");
        if (persistValue === null) {
            return true;
        }
        return String(persistValue).toLowerCase() !== "false";
    }

    applyDismissedAlertStyles();

    global.CodeAtlasDismissibleAlerts = {
        readDismissedAlerts: readDismissedAlerts,
        writeDismissedAlert: writeDismissedAlert,
        applyDismissedAlertStyles: applyDismissedAlertStyles,
        shouldPersistAlert: shouldPersistAlert
    };
})(window);
