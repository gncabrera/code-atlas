CREATE TABLE IF NOT EXISTS project_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    allowed_extensions TEXT NOT NULL,
    allowed_files TEXT,
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

INSERT INTO project_type (name, allowed_extensions, description, allowed_files) VALUES

-- JVM
('Java / Spring', 'java,kt,groovy,gradle', 'JVM backend', 'pom.xml,build.gradle,build.gradle.kts,settings.gradle'),
('Android', 'java,kt,xml,gradle', 'Android application', 'AndroidManifest.xml,build.gradle'),
('Scala', 'scala,sbt', 'Scala application', 'build.sbt'),
('Clojure', 'clj,cljs,edn', 'Clojure application', 'project.clj,deps.edn'),

-- .NET
('.NET', 'cs,csproj,sln,razor', '.NET application', 'Program.cs,appsettings.json'),
('Blazor', 'cs,razor', 'Blazor frontend', '_Imports.razor'),

-- JavaScript / TypeScript
('Frontend', 'js,ts,jsx,tsx,html,css,scss', 'Web frontend', 'package.json,tsconfig.json'),
('React', 'js,jsx,ts,tsx,css,scss', 'React application', 'package.json,vite.config.ts'),
('Angular', 'ts,html,scss,css', 'Angular application', 'angular.json,tsconfig.json'),
('Vue', 'js,ts,vue', 'Vue application', 'vite.config.ts,nuxt.config.ts'),
('Node.js', 'js,ts,mjs,cjs', 'Node backend', 'package.json'),
('Next.js', 'js,jsx,ts,tsx', 'Next.js application', 'next.config.js,next.config.mjs'),
('Nuxt', 'js,ts,vue', 'Nuxt application', 'nuxt.config.ts'),

-- Python
('Python', 'py', 'Python application', 'requirements.txt,pyproject.toml,Pipfile'),
('Django', 'py,html', 'Django application', 'manage.py'),
('Flask', 'py,html', 'Flask application', 'app.py'),
('FastAPI', 'py', 'FastAPI application', 'main.py'),

-- Go
('Go', 'go', 'Go application', 'go.mod'),

-- Rust
('Rust', 'rs', 'Rust application', 'Cargo.toml'),

-- PHP
('PHP', 'php,phtml', 'PHP application', 'composer.json'),
('Laravel', 'php,blade.php', 'Laravel application', 'artisan'),
('Symfony', 'php,yaml,xml', 'Symfony application', 'bin/console'),

-- Ruby
('Ruby', 'rb', 'Ruby application', 'Gemfile'),
('Ruby on Rails', 'rb,erb', 'Rails application', 'config.ru'),

-- C / C++
('C', 'c,h', 'C application', 'Makefile,CMakeLists.txt'),
('C++', 'cpp,hpp,cc,h,cxx', 'C++ application', 'CMakeLists.txt'),

-- Swift
('Swift', 'swift', 'Swift application', 'Package.swift'),

-- Mobile
('Flutter', 'dart', 'Flutter application', 'pubspec.yaml'),
('React Native', 'js,jsx,ts,tsx', 'React Native application', 'app.json'),

-- Databases
('SQL', 'sql', 'Database scripts', 'schema.sql,data.sql'),
('Oracle', 'sql,pks,pkb', 'Oracle database objects', ''),
('Liquibase', 'xml,yaml,sql', 'Liquibase migrations', 'db.changelog.xml'),
('Flyway', 'sql', 'Flyway migrations', ''),

-- DevOps
('DevOps', 'yml,yaml,tf,sh,dockerfile', 'Infrastructure', 'docker-compose.yml'),
('Docker', 'dockerfile,sh,yml,yaml', 'Containerization', 'Dockerfile,docker-compose.yml'),
('Kubernetes', 'yml,yaml', 'Kubernetes manifests', 'kustomization.yaml'),
('Terraform', 'tf,hcl', 'Terraform infrastructure', 'terraform.tfvars'),
('Ansible', 'yml,yaml', 'Ansible automation', 'ansible.cfg'),
('GitHub Actions', 'yml,yaml', 'GitHub CI/CD', ''),
('Jenkins', 'groovy', 'Jenkins pipelines', 'Jenkinsfile'),

-- Cloud
('AWS', 'tf,yaml,yml,json', 'AWS infrastructure', ''),
('Azure', 'tf,bicep,json', 'Azure infrastructure', ''),
('GCP', 'tf,yaml,yml', 'Google Cloud infrastructure', ''),

-- Configuration
('Configuration', 'properties,yml,yaml,json,xml,conf,ini,toml', 'Configuration files', 'application.properties'),
('Build Files', 'xml,gradle,sbt,toml', 'Build and dependency descriptors', 'pom.xml,build.gradle,Cargo.toml'),

-- Documentation
('Docs', 'md,rst,adoc,txt', 'Documentation', 'README.md,CONTRIBUTING.md'),
('Architecture Docs', 'md,drawio,puml,plantuml', 'Architecture documentation', ''),
('API Specs', 'yaml,yml,json', 'API contracts', 'openapi.yaml,swagger.yaml'),

-- Testing
('Tests', 'java,kt,js,ts,py,cs,go,rb,php', 'Automated tests', ''),
('Postman', 'json', 'Postman collections', ''),

-- Data
('Data Files', 'json,csv,xml,yaml,yml', 'Data files', ''),
('ETL', 'sql,py,scala', 'Data processing pipelines', ''),

-- AI / ML
('Machine Learning', 'py,ipynb', 'Machine learning project', 'requirements.txt'),
('Jupyter', 'ipynb', 'Notebook project', ''),

-- Misc
('Shell Scripts', 'sh,bat,ps1', 'Automation scripts', ''),
('Maven', 'xml', 'Maven build files', 'pom.xml'),
('Gradle', 'gradle,kts', 'Gradle build files', 'build.gradle'),
('Git', 'gitignore,gitattributes', 'Git configuration', '.gitignore'),
('Monorepo', 'json,yaml,yml', 'Monorepo configuration', 'nx.json,turbo.json,pnpm-workspace.yaml');
