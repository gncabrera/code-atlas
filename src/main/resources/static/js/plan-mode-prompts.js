$(function () {

    var currentPrompt = null;

    loadPrompts();

    $('#savePromptBtn').on('click', savePrompt);
    $('#resetPromptBtn').on('click', resetForm);

    function loadPrompts() {
        $.ajax({
            method: 'GET',
            url: '/api/admin/plan-mode-prompts',
            success: function (res) {
                if (res.result !== 'success') return;
                renderTable(res.data || []);
            }
        });
    }

    function renderTable(prompts) {
        var $body = $('#promptsTableBody').empty();
        prompts.forEach(function (p) {
            var $row = $('<tr>');
            $row.append(
                $('<td>').append($('<code>').text(p.code)),
                $('<td>').text(p.name),
                $('<td>').append(
                    $('<button>').addClass('btn btn-sm btn-outline-primary')
                        .text('Edit')
                        .on('click', function () { loadForEdit(p); })
                )
            );
            $body.append($row);
        });
    }

    function loadForEdit(prompt) {
        currentPrompt = prompt;
        $('#promptId').val(prompt.id);
        $('#promptCode').val(prompt.code);
        $('#promptName').val(prompt.name);
        $('#promptText').val(prompt.prompt);
        $('#savePromptBtn').prop('disabled', false);
        $('html, body').animate({ scrollTop: 0 }, 200);
    }

    function savePrompt() {
        if (!currentPrompt) return;
        var promptText = $('#promptText').val().trim();
        if (!promptText) {
            alert('Prompt text is required.');
            return;
        }
        var $btn = $('#savePromptBtn');
        CodeAtlas.setButtonLoading($btn, true, 'Saving...');

        $.ajax({
            method: 'PUT',
            url: '/api/admin/plan-mode-prompts/' + currentPrompt.id,
            contentType: 'application/json',
            data: JSON.stringify({ prompt: promptText }),
            success: function (res) {
                CodeAtlas.setButtonLoading($btn, false);
                if (res.result === 'success') {
                    currentPrompt = res.data;
                    showAlert('success', 'Prompt saved.');
                } else {
                    showAlert('danger', res.message || 'Save failed.');
                }
            },
            error: function (xhr) {
                CodeAtlas.setButtonLoading($btn, false);
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Save failed.';
                showAlert('danger', msg);
            }
        });
    }

    function resetForm() {
        currentPrompt = null;
        $('#promptId').val('');
        $('#promptCode').val('');
        $('#promptName').val('');
        $('#promptText').val('');
        $('#savePromptBtn').prop('disabled', true);
    }

    function showAlert(type, msg) {
        $('#alertContainer').remove();
        var $alert = $('<div id="alertContainer">').addClass('alert alert-' + type + ' alert-dismissible fade show mt-3')
            .attr('role', 'alert')
            .html(msg + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>');
        $('main').prepend($alert);
    }
});
