$(function () {
    function showAlert(message, isError) {
        const alert = $("#projectsAlert");
        alert.removeClass("d-none alert-success alert-danger")
            .addClass(isError ? "alert-danger" : "alert-success")
            .text(message);
    }

    function clearForm() {
        $("#projectId").val("");
        $("#projectName").val("");
        $("#projectPath").val("");
        $("#projectDescription").val("");
        $("#projectUseAgentsFile").prop("checked", true);
    }

    function rowActions(project) {
        const actions = $("<td></td>");
        const editBtn = $("<button class='btn btn-sm btn-outline-primary me-2'>Edit</button>");
        const deleteBtn = $("<button class='btn btn-sm btn-outline-danger'>Delete</button>");
        editBtn.on("click", function () {
            $("#projectId").val(project.id);
            $("#projectName").val(project.name);
            $("#projectPath").val(project.path);
            $("#projectDescription").val(project.description);
            $("#projectUseAgentsFile").prop("checked", project.useAgentsFile);
        });
        deleteBtn.on("click", function () {
            if (!window.confirm("Delete selected project?")) {
                return;
            }
            $.ajax({
                url: `/api/projects/${project.id}`,
                method: "DELETE"
            }).done(function () {
                showAlert("Project deleted.", false);
                loadProjects();
                clearForm();
            }).fail(function (xhr) {
                const message = xhr.responseJSON?.message || "Failed deleting project.";
                showAlert(message, true);
            });
        });
        actions.append(editBtn).append(deleteBtn);
        return actions;
    }

    function renderTable(projects) {
        const tbody = $("#projectsTableBody");
        tbody.empty();
        projects.forEach(function (project) {
            const row = $("<tr></tr>");
            row.append($("<td></td>").text(project.name));
            row.append($("<td></td>").text(project.path));
            row.append($("<td></td>").text(project.description));
            row.append($("<td></td>").text(project.useAgentsFile ? "Yes" : "No"));
            row.append(rowActions(project));
            tbody.append(row);
        });
    }

    function loadProjects() {
        $.get("/api/projects")
            .done(function (response) {
                renderTable(response.data || []);
            })
            .fail(function (xhr) {
                const message = xhr.responseJSON?.message || "Failed loading projects.";
                showAlert(message, true);
            });
    }

    $("#saveProjectBtn").on("click", function () {
        const projectId = $("#projectId").val();
        const payload = {
            name: $("#projectName").val(),
            path: $("#projectPath").val(),
            description: $("#projectDescription").val(),
            useAgentsFile: $("#projectUseAgentsFile").is(":checked")
        };
        const method = projectId ? "PUT" : "POST";
        const endpoint = projectId ? `/api/projects/${projectId}` : "/api/projects";
        $.ajax({
            url: endpoint,
            method: method,
            contentType: "application/json",
            data: JSON.stringify(payload)
        }).done(function (response) {
            showAlert(response.message || "Project saved.", false);
            loadProjects();
            clearForm();
        }).fail(function (xhr) {
            const message = xhr.responseJSON?.message || "Failed saving project.";
            showAlert(message, true);
        });
    });

    $("#resetProjectBtn").on("click", function () {
        clearForm();
    });

    loadProjects();
});
