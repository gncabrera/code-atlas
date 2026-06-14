$(function () {

    // ─── State ───────────────────────────────────────────────────────────────

    var state = {
        sessions: [],
        activeSessionId: null,
        activeSession: null,
        activeStep: 'request',      // request | context | discovery | review | plan
        discoveryAnswers: {},        // { questionId: optionId }
        selectedSuggestions: [],     // [ title, ... ]
        pendingSelectValues: null,   // { outputType, projectId, contextModelId, planModelId } — applied after dropdowns load
    };

    var STEPS = ['request', 'context', 'discovery', 'review', 'plan'];

    var STATUS_STEP_MAP = {
        'DRAFT':            'request',
        'CONTEXT_READY':    'context',
        'DISCOVERY_READY':  'discovery',
        'ANSWERED':         'review',
        'PLAN_READY':       'plan',
    };

    // ─── Init ────────────────────────────────────────────────────────────────

    loadModels();
    loadProjects();
    loadSessions();

    // ─── Event Bindings ──────────────────────────────────────────────────────

    $('#newSessionBtn, #welcomeNewSessionBtn').on('click', function () {
        startNewSession();
    });

    $('#createSessionBtn').on('click', createSession);
    $('#continueToContextBtn').on('click', function () { navigateTo('context'); });
    $('#generateContextBtn').on('click', generateContext);
    $('#continueToDiscoveryBtn').on('click', function () { navigateTo('discovery'); });
    $('#generateDiscoveryBtn').on('click', generateDiscovery);
    $('#continueToReviewBtn').on('click', function () {
        renderReviewFromState();
        navigateTo('review');
    });
    $('#saveAnswersBtn').on('click', saveAnswers);
    $('#generatePlanBtn').on('click', generatePlan);
    $('#refinePlanBtn').on('click', refinePlan);
    $('#exportPlanBtn').on('click', exportPlan);
    $('#toggleContextPreview').on('click', function () {
        $('#contextDataPreview').toggle();
    });

    $(document).on('click', '.plan-step', function () {
        var step = $(this).data('step');
        if ($(this).hasClass('locked') || !state.activeSession) return;
        navigateTo(step);
    });

    // ─── API helpers ─────────────────────────────────────────────────────────

    function api(method, url, data, onSuccess, onError) {
        var opts = {
            method: method,
            url: url,
            success: function (res) {
                if (res.result === 'success') {
                    onSuccess(res.data);
                } else {
                    showAlert('danger', res.message || 'Request failed.');
                    if (onError) onError(res.message);
                }
            },
            error: function (xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message : 'Request failed.';
                showAlert('danger', msg);
                if (onError) onError(msg);
            }
        };
        if (data !== null) {
            opts.contentType = 'application/json';
            opts.data = JSON.stringify(data);
        }
        return $.ajax(opts);
    }

    // ─── Data Loading ─────────────────────────────────────────────────────────

    function loadModels() {
        $.ajax({
            method: 'GET',
            url: '/api/ai-models?enabledOnly=true',
            success: function (res) {
                if (res.result !== 'success') return;
                var models = res.data || [];
                var selects = ['#contextModelSelect', '#planModelSelect'];
                selects.forEach(function (sel) {
                    var $s = $(sel).empty();
                    if (!models.length) {
                        $s.append('<option value="">No models available</option>');
                        return;
                    }
                    models.forEach(function (m) {
                        $s.append($('<option>').val(m.id).text(m.name));
                    });
                });
                applyPendingSelectValues();
            }
        });
    }

    function loadProjects() {
        $.ajax({
            method: 'GET',
            url: '/api/projects',
            success: function (res) {
                if (res.result !== 'success') return;
                var projects = res.data || [];
                var $s = $('#projectSelect').empty();
                $s.append('<option value="">No project</option>');
                projects.forEach(function (p) {
                    $s.append($('<option>').val(p.id).text(p.name));
                });
                applyPendingSelectValues();
            }
        });
    }

    function applyPendingSelectValues() {
        if (!state.pendingSelectValues) return;
        var v = state.pendingSelectValues;
        var planGenerated = !!(state.activeSession && state.activeSession.result);
        if (v.outputType !== undefined)     $('#outputTypeSelect').val(String(v.outputType)).prop('disabled', planGenerated);
        if (v.projectId !== undefined)      $('#projectSelect').val(v.projectId ? String(v.projectId) : '').prop('disabled', planGenerated);
        if (v.contextModelId !== undefined) $('#contextModelSelect').val(String(v.contextModelId)).prop('disabled', planGenerated);
        if (v.planModelId !== undefined)    $('#planModelSelect').val(String(v.planModelId)).prop('disabled', planGenerated);
    }

    function loadSessions() {
        $.ajax({
            method: 'GET',
            url: '/api/plan-mode/sessions',
            success: function (res) {
                if (res.result !== 'success') return;
                state.sessions = res.data || [];
                renderSessionList();
            }
        });
    }

    // ─── Session List Rendering ───────────────────────────────────────────────

    function renderSessionList() {
        var $list = $('#sessionList').empty();
        if (!state.sessions.length) {
            $('#sessionListEmpty').show();
            return;
        }
        $('#sessionListEmpty').hide();
        state.sessions.forEach(function (s) {
            var $item = $('<div>')
                .addClass('session-item')
                .data('id', s.id)
                .toggleClass('active', s.id === state.activeSessionId);

            var $delete = $('<button>')
                .addClass('session-delete btn btn-link btn-sm p-0 border-0')
                .attr('title', 'Delete session')
                .html('<i class="bi bi-trash"></i>')
                .on('click', function (e) {
                    e.stopPropagation();
                    deleteSession(s.id);
                });

            $item.append(
                $('<div>').addClass('session-title').text(s.title).append($delete),
                $('<div>').addClass('session-meta').text(
                    s.outputTypeDisplay + ' · ' + formatStatus(s.status)
                )
            );
            $item.on('click', function () { loadSession(s.id); });
            $list.append($item);
        });
    }

    function formatStatus(status) {
        var map = {
            'DRAFT': 'Draft',
            'CONTEXT_READY': 'Context ready',
            'DISCOVERY_READY': 'Discovery ready',
            'ANSWERED': 'Answered',
            'PLAN_READY': 'Plan ready',
        };
        return map[status] || status;
    }

    // ─── Session CRUD ─────────────────────────────────────────────────────────

    function startNewSession() {
        state.activeSessionId = null;
        state.activeSession = null;
        state.discoveryAnswers = {};
        state.selectedSuggestions = [];
        clearAlert();
        resetRequestForm();
        resetContextPanel();
        resetDiscoveryPanel();
        showStepper(false);
        updateStepperState('request', null);
        showPanel('request');
    }

    function resetContextPanel() {
        $('#contextEmptyMsg').show();
        $('#contextDataPreview').val('').prop('disabled', false).hide();
        $('#toggleContextPreview').hide();
        $('#continueToDiscoveryBtn').hide();
        $('#generateContextBtn').show();
        $('#contextReadOnlyNotice').hide();
    }

    function resetDiscoveryPanel() {
        $('#discoveryEmptyMsg').show();
        $('#discoveryContent').hide();
        $('#questionsContainer').empty();
        $('#suggestionsContainer').empty();
        $('#continueToReviewBtn').hide();
        $('#saveAnswersBtn').show();
        $('#generateDiscoveryBtn').show();
        $('#discoveryReadOnlyNotice').hide();
    }

    function setDiscoveryReadOnly(readOnly) {
        $('#questionsContainer input[type="radio"]').prop('disabled', readOnly);
        $('#suggestionsContainer input[type="checkbox"]').prop('disabled', readOnly);
    }

    function resetRequestForm() {
        state.pendingSelectValues = null;
        $('#userRequestInput').val('').prop('disabled', false);
        $('#outputTypeSelect').val('CURSOR_OPTIMIZED').prop('disabled', false);
        $('#projectSelect').val('').prop('disabled', false);
        $('#contextModelSelect').prop('disabled', false);
        $('#planModelSelect').prop('disabled', false);
        $('#createSessionBtn').show();
        $('#continueToContextBtn').hide();
        $('#updateRequestBtn').hide();
        $('#requestReadOnlyNotice').hide();
    }

    function populateRequestForm(session) {
        // Store for async dropdowns (may not have options yet when this runs)
        state.pendingSelectValues = {
            outputType: session.outputType,
            projectId: session.projectId || '',
            contextModelId: session.contextModelId,
            planModelId: session.planModelId
        };

        $('#userRequestInput').val(session.userRequest).prop('disabled', true);
        $('#outputTypeSelect').val(String(session.outputType || '')).prop('disabled', true);
        $('#projectSelect').val(session.projectId ? String(session.projectId) : '').prop('disabled', true);
        $('#contextModelSelect').val(String(session.contextModelId || '')).prop('disabled', true);
        $('#planModelSelect').val(String(session.planModelId || '')).prop('disabled', true);

        // Also apply immediately in case dropdowns already loaded
        applyPendingSelectValues();

        $('#createSessionBtn').hide();
        $('#continueToContextBtn').hide();
        $('#updateRequestBtn').hide();
        $('#requestReadOnlyNotice').show();
    }

    function populateRequestFormEditable(session) {
        state.pendingSelectValues = {
            outputType: session.outputType,
            projectId: session.projectId || '',
            contextModelId: session.contextModelId,
            planModelId: session.planModelId
        };

        $('#userRequestInput').val(session.userRequest).prop('disabled', false);
        $('#outputTypeSelect').val(String(session.outputType || '')).prop('disabled', false);
        $('#projectSelect').val(session.projectId ? String(session.projectId) : '').prop('disabled', false);
        $('#contextModelSelect').val(String(session.contextModelId || '')).prop('disabled', false);
        $('#planModelSelect').val(String(session.planModelId || '')).prop('disabled', false);

        applyPendingSelectValues();

        $('#createSessionBtn').show();
        $('#continueToContextBtn').show();
        $('#updateRequestBtn').hide();
        $('#requestReadOnlyNotice').hide();
    }

    function createSession() {
        var userRequest = $('#userRequestInput').val().trim();
        var outputType = $('#outputTypeSelect').val();
        var contextModelId = parseInt($('#contextModelSelect').val(), 10);
        var planModelId = parseInt($('#planModelSelect').val(), 10);
        var projectId = parseInt($('#projectSelect').val(), 10) || null;

        if (!userRequest) { showAlert('warning', 'User request is required.'); return; }
        if (!contextModelId) { showAlert('warning', 'Context model is required.'); return; }
        if (!planModelId) { showAlert('warning', 'Plan model is required.'); return; }

        var $btn = $('#createSessionBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Creating...');

        api('POST', '/api/plan-mode/sessions', {
            userRequest: userRequest,
            outputType: outputType,
            contextModelId: contextModelId,
            planModelId: planModelId,
            projectId: projectId
        }, function (session) {
            CodeAtlas.setButtonLoading($btn, false);
            state.activeSession = session;
            state.activeSessionId = session.id;
            loadSessions();
            renderSession(session);
            navigateTo('context');
        }, function () {
            CodeAtlas.setButtonLoading($btn, false);
        });
    }

    function loadSession(id) {
        $.ajax({
            method: 'GET',
            url: '/api/plan-mode/sessions/' + id,
            success: function (res) {
                if (res.result !== 'success') return;
                var session = res.data;
                state.activeSession = session;
                state.activeSessionId = id;
                state.discoveryAnswers = session.answers || {};
                state.selectedSuggestions = session.selectedSuggestions || [];
                renderSession(session);
                var step = STATUS_STEP_MAP[session.status] || 'request';
                navigateTo(step);
                renderSessionList();
            }
        });
    }

    function deleteSession(id) {
        if (!window.confirm('Delete this session? This cannot be undone.')) return;
        api('DELETE', '/api/plan-mode/sessions/' + id, null, function () {
            if (id === state.activeSessionId) {
                state.activeSession = null;
                state.activeSessionId = null;
                showStepper(false);
                showPanel('welcome');
            }
            loadSessions();
        }, null);
    }

    // ─── Session Rendering ───────────────────────────────────────────────────

    function renderSession(session) {
        var planGenerated = !!session.result;

        showStepper(true);
        resetContextPanel();
        resetDiscoveryPanel();
        // Populate Step 1 (read-only only when plan is generated)
        if (planGenerated) {
            populateRequestForm(session);
        } else {
            populateRequestFormEditable(session);
        }

        // Populate context panel info
        $('#ctxOutputType').text(session.outputTypeDisplay);
        $('#ctxProject').text(session.projectName || 'None');
        $('#ctxContextModel').text(session.contextModelName);
        $('#ctxUserRequest').text(session.userRequest);

        if (session.contextData) {
            $('#contextEmptyMsg').hide();
            $('#contextDataPreview').val(session.contextData).show();
            $('#toggleContextPreview').show();
            if (planGenerated) {
                $('#generateContextBtn').hide();
                $('#continueToDiscoveryBtn').hide();
                $('#contextDataPreview').prop('disabled', true);
                $('#contextReadOnlyNotice').show();
            } else {
                $('#generateContextBtn').show();
                $('#continueToDiscoveryBtn').show();
                $('#contextDataPreview').prop('disabled', false);
                $('#contextReadOnlyNotice').hide();
            }
        }

        // Populate discovery
        if (session.discovery) {
            renderDiscovery(session.discovery);
            state.discoveryAnswers = session.answers || {};
            state.selectedSuggestions = session.selectedSuggestions || [];
            restoreDiscoverySelections();
            $('#discoveryEmptyMsg').hide();
            $('#discoveryContent').show();
            if (planGenerated) {
                setDiscoveryReadOnly(true);
                $('#continueToReviewBtn').hide();
                $('#saveAnswersBtn').hide();
                $('#generateDiscoveryBtn').hide();
                $('#discoveryReadOnlyNotice').show();
            } else {
                setDiscoveryReadOnly(false);
                $('#continueToReviewBtn').show();
                $('#saveAnswersBtn').show();
                $('#generateDiscoveryBtn').show();
                $('#discoveryReadOnlyNotice').hide();
            }
        }

        // Populate review
        if (session.answers) {
            renderReviewFromState();
            if (!planGenerated) {
                $('#saveAnswersBtn').show();
            }
        }

        // Populate plan
        if (session.result) {
            renderPlan(session.result);
        }

        updateStepperState(STATUS_STEP_MAP[session.status] || 'request', session.status);
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    function navigateTo(step) {
        state.activeStep = step;
        showPanel(step);
        updateStepperState(step, state.activeSession ? state.activeSession.status : null);
    }

    function showPanel(name) {
        var panels = ['welcome', 'request', 'context', 'discovery', 'review', 'plan'];
        panels.forEach(function (p) {
            $('#panel' + capitalize(p)).hide();
        });
        $('#panel' + capitalize(name)).show();
        clearAlert();
    }

    function capitalize(s) {
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    function showStepper(visible) {
        $('#planStepper').toggle(visible);
    }

    function updateStepperState(activeStep, status) {
        var reached = status ? stepIndex(STATUS_STEP_MAP[status] || 'request') : 0;
        STEPS.forEach(function (step, i) {
            var $s = $('.plan-step[data-step="' + step + '"]');
            $s.removeClass('active completed locked');
            if (step === activeStep) {
                $s.addClass('active');
            } else if (i <= reached) {
                $s.addClass('completed');
            } else {
                $s.addClass('locked');
            }
        });
    }

    function stepIndex(step) {
        return STEPS.indexOf(step);
    }

    // ─── Step 2: Context ──────────────────────────────────────────────────────

    function generateContext() {
        if (!state.activeSessionId) return;
        var $btn = $('#generateContextBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Generating context...');

        api('POST', '/api/plan-mode/sessions/' + state.activeSessionId + '/generate-context', {}, function (session) {
            CodeAtlas.setButtonLoading($btn, false);
            state.activeSession = session;
            if (session.contextData) {
                $('#contextEmptyMsg').hide();
                $('#contextDataPreview').val(session.contextData).show();
                $('#toggleContextPreview').show();
                $('#continueToDiscoveryBtn').show();
            }
            updateStepperState('context', session.status);
            loadSessions();
            showAlert('success', 'Context generated.');
        }, function () {
            CodeAtlas.setButtonLoading($btn, false);
        });
    }

    // ─── Step 3: Discovery ───────────────────────────────────────────────────

    function generateDiscovery() {
        if (!state.activeSessionId) return;
        var $btn = $('#generateDiscoveryBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Generating discovery...');

        api('POST', '/api/plan-mode/sessions/' + state.activeSessionId + '/generate-discovery', {}, function (session) {
            CodeAtlas.setButtonLoading($btn, false);
            state.activeSession = session;
            state.discoveryAnswers = {};
            state.selectedSuggestions = [];
            if (session.discovery) {
                renderDiscovery(session.discovery);
                $('#discoveryEmptyMsg').hide();
                $('#discoveryContent').show();
                $('#continueToReviewBtn').show();
            }
            updateStepperState('discovery', session.status);
            loadSessions();
            showAlert('success', 'Discovery generated.');
        }, function () {
            CodeAtlas.setButtonLoading($btn, false);
        });
    }

    function renderDiscovery(discovery) {
        renderQuestions(discovery.questions || []);
        renderSuggestions(discovery.suggestions || []);
    }

    function renderQuestions(questions) {
        var $container = $('#questionsContainer').empty();
        if (!questions.length) {
            $container.append('<p class="text-muted">No questions generated.</p>');
            return;
        }
        questions.forEach(function (q) {
            var $qDiv = $('<div>').addClass('mb-4');
            $qDiv.append($('<p>').addClass('fw-semibold mb-2').text(q.question));
            var $opts = $('<div>').addClass('d-flex flex-column gap-2');
            (q.options || []).forEach(function (opt) {
                var inputId = 'q_' + q.id + '_' + opt.id;
                var $label = $('<label>').addClass('form-check-label d-flex align-items-center gap-2 cursor-pointer').attr('for', inputId);
                var $radio = $('<input>').attr({
                    type: 'radio',
                    id: inputId,
                    name: 'q_' + q.id,
                    value: opt.id,
                    class: 'form-check-input'
                }).on('change', function () {
                    state.discoveryAnswers[q.id] = opt.id;
                });
                if (opt.isDefault && !state.discoveryAnswers[q.id]) {
                    $radio.prop('checked', true);
                    state.discoveryAnswers[q.id] = opt.id;
                }
                var $optText = $('<span>').text(opt.text);
                if (opt.isDefault) {
                    $optText.append($('<span>').addClass('badge bg-secondary ms-2 small').text('default'));
                }
                $label.append($radio, $optText);
                $opts.append($('<div>').addClass('form-check mb-1').append($label));
            });
            $qDiv.append($opts);
            $container.append($qDiv);
        });
    }

    function renderSuggestions(suggestions) {
        var $container = $('#suggestionsContainer').empty();
        if (!suggestions.length) {
            $container.append('<p class="text-muted">No suggestions generated.</p>');
            return;
        }

        var IMPACT_COLORS = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'secondary' };
        var CAT_COLORS = {
            FEATURE: 'primary', UX: 'info', ARCHITECTURE: 'dark', TESTING: 'success',
            PERFORMANCE: 'warning', SECURITY: 'danger', OBSERVABILITY: 'secondary',
            DEVELOPER_EXPERIENCE: 'info', ROLLOUT: 'primary'
        };

        suggestions.forEach(function (s, idx) {
            var checkId = 'sug_' + idx;
            var $row = $('<div>').addClass('border rounded p-3 mb-2');
            var $header = $('<div>').addClass('d-flex align-items-start gap-2');
            var $check = $('<input>').attr({ type: 'checkbox', id: checkId, class: 'form-check-input mt-1 flex-shrink-0' })
                .on('change', function () {
                    if (this.checked) {
                        if (!state.selectedSuggestions.includes(s.title)) {
                            state.selectedSuggestions.push(s.title);
                        }
                    } else {
                        state.selectedSuggestions = state.selectedSuggestions.filter(function (t) { return t !== s.title; });
                    }
                });
            var $meta = $('<div>').addClass('flex-grow-1');
            $meta.append(
                $('<label>').attr('for', checkId).addClass('fw-semibold mb-1 d-block').text(s.title),
                $('<p>').addClass('text-muted small mb-1').text(s.change),
                $('<p>').addClass('text-muted small mb-2').text(s.reason)
            );
            var $badges = $('<div>').addClass('d-flex flex-wrap gap-1');
            if (s.category) $badges.append($('<span>').addClass('badge bg-' + (CAT_COLORS[s.category] || 'secondary')).text(s.category));
            if (s.impact) $badges.append($('<span>').addClass('badge bg-' + (IMPACT_COLORS[s.impact] || 'secondary')).text('Impact: ' + s.impact));
            if (s.effort) $badges.append($('<span>').addClass('badge bg-light text-dark border').text('Effort: ' + s.effort));
            $meta.append($badges);
            $header.append($check, $meta);
            $row.append($header);
            $container.append($row);
        });
    }

    function restoreDiscoverySelections() {
        // Restore question radio selections
        Object.keys(state.discoveryAnswers).forEach(function (qId) {
            var optId = state.discoveryAnswers[qId];
            $('input[name="q_' + qId + '"][value="' + optId + '"]').prop('checked', true);
        });
        // Restore suggestion checkboxes
        if (state.selectedSuggestions && state.selectedSuggestions.length) {
            var titles = state.selectedSuggestions;
            $('#suggestionsContainer input[type="checkbox"]').each(function (idx) {
                var $input = $(this);
                var label = $input.closest('.border').find('label').first().text().trim();
                if (titles.indexOf(label) !== -1) {
                    $input.prop('checked', true);
                }
            });
        }
    }

    // ─── Step 4: Review ──────────────────────────────────────────────────────

    function renderReviewFromState() {
        if (!state.activeSession || !state.activeSession.discovery) return;
        var questions = state.activeSession.discovery.questions || [];
        var suggestions = state.activeSession.discovery.suggestions || [];

        // Answers summary
        var $answersCont = $('#reviewAnswersContainer').empty();
        if (!questions.length) {
            $answersCont.append('<div class="text-muted">No questions to answer.</div>');
        } else {
            questions.forEach(function (q) {
                var answerId = state.discoveryAnswers[q.id];
                var answerText = '(not answered)';
                if (answerId) {
                    var opt = (q.options || []).find(function (o) { return o.id === answerId; });
                    if (opt) answerText = opt.text;
                }
                $answersCont.append(
                    $('<div>').addClass('mb-2').append(
                        $('<div>').addClass('small text-muted').text(q.question),
                        $('<div>').addClass('fw-semibold small').text(answerText)
                    )
                );
            });
        }

        // Suggestions summary
        var $sugCont = $('#reviewSuggestionsContainer').empty();
        var selected = suggestions.filter(function (s) { return state.selectedSuggestions.includes(s.title); });
        if (!selected.length) {
            $sugCont.append('<div class="text-muted small">No suggestions selected.</div>');
        } else {
            selected.forEach(function (s) {
                $sugCont.append(
                    $('<div>').addClass('mb-1 small').append(
                        $('<span>').addClass('fw-semibold').text(s.title + ': '),
                        $('<span>').addClass('text-muted').text(s.change)
                    )
                );
            });
        }
    }

    function saveAnswers() {
        if (!state.activeSessionId) return;
        var $btn = $('#saveAnswersBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Saving...');

        api('POST', '/api/plan-mode/sessions/' + state.activeSessionId + '/answers', {
            answers: state.discoveryAnswers,
            selectedSuggestions: state.selectedSuggestions
        }, function (session) {
            CodeAtlas.setButtonLoading($btn, false);
            state.activeSession = session;
            loadSessions();
            navigateTo('plan');
        }, function () {
            CodeAtlas.setButtonLoading($btn, false);
        });
    }

    // ─── Step 5: Plan ────────────────────────────────────────────────────────

    function generatePlan() {
        if (!state.activeSessionId) return;
        var $btn = $('#generatePlanBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Generating plan...');

        api('POST', '/api/plan-mode/sessions/' + state.activeSessionId + '/generate-plan', {}, function (session) {
            CodeAtlas.setButtonLoading($btn, false);
            state.activeSession = session;
            if (session.result) {
                renderPlan(session.result);
            }
            updateStepperState('plan', session.status);
            loadSessions();
            showAlert('success', 'Plan generated.');
        }, function () {
            CodeAtlas.setButtonLoading($btn, false);
        });
    }

    function refinePlan() {
        var userMessage = $('#refinementInput').val().trim();
        if (!userMessage) { showAlert('warning', 'Enter a refinement message.'); return; }
        if (!state.activeSessionId) return;

        var $btn = $('#refinePlanBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Refining...');

        api('POST', '/api/plan-mode/sessions/' + state.activeSessionId + '/refine-plan', {
            userMessage: userMessage
        }, function (session) {
            CodeAtlas.setButtonLoading($btn, false);
            state.activeSession = session;
            if (session.result) {
                renderPlan(session.result);
            }
            $('#refinementInput').val('');
            showAlert('success', 'Plan refined.');
        }, function () {
            CodeAtlas.setButtonLoading($btn, false);
        });
    }

    function renderPlan(result) {
        $('#planEmptyMsg').hide();
        $('#planResultContent').show();
        $('#generatedPlanText').val(result.generatedPlan || '');
        $('#generatePlanBtn').hide();
    }

    function exportPlan() {
        var plan = $('#generatedPlanText').val();
        if (!plan) return;
        var blob = new Blob([plan], { type: 'text/markdown;charset=utf-8;' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = 'plan-' + (state.activeSessionId || 'export') + '.md';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    // ─── Alert helpers ───────────────────────────────────────────────────────

    function showAlert(type, message) {
        clearAlert();
        var $alert = $('<div>')
            .addClass('alert alert-' + type + ' alert-dismissible fade show mt-3')
            .attr('role', 'alert')
            .html(message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>');
        $('.plan-panel:visible').prepend($alert);
    }

    function clearAlert() {
        $('.plan-panel .alert').remove();
    }

});

