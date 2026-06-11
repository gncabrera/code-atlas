CREATE TABLE IF NOT EXISTS project_indexer_types (
    project_id INTEGER NOT NULL,
    profile TEXT NOT NULL,
    PRIMARY KEY (project_id, profile),
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX IF NOT EXISTS idx_project_indexer_types_project
    ON project_indexer_types(project_id);

INSERT INTO project_indexer_types (project_id, profile)
SELECT id, 'SPRING_JAVA' FROM projects;

INSERT INTO project_indexer_types (project_id, profile)
SELECT id, 'SPRING_FLYWAY' FROM projects;

INSERT INTO project_indexer_types (project_id, profile)
SELECT id, 'THYMELEAF' FROM projects;
