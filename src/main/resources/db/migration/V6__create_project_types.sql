CREATE TABLE IF NOT EXISTS project_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    allowed_extensions TEXT NOT NULL,
    description TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_type_name
    ON project_type(name);

CREATE TABLE IF NOT EXISTS project_project_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    project_type_id INTEGER NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (project_type_id) REFERENCES project_type(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_project_type_project_type
    ON project_project_type(project_id, project_type_id);

INSERT INTO project_type (name, allowed_extensions, description) VALUES
    ('Java / Spring', 'java,kt,groovy,gradle', 'JVM backend'),
    ('Frontend', 'js,ts,jsx,tsx,html,css,scss', 'Web UI'),
    ('SQL', 'sql', 'Database scripts'),
    ('DevOps', 'yml,yaml,tf,sh,dockerfile', 'Infra/config'),
    ('Docs', 'md,rst,adoc', 'Documentation');
