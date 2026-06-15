create database keycloak;
create database core_db;
create database parser_db;
create database llm_db;
GRANT ALL PRIVILEGES ON `keycloak`.* to alert_job_user@'%';
GRANT ALL PRIVILEGES ON `core_db`.* to alert_job_user@'%';
GRANT ALL PRIVILEGES ON `parser_db`.* to alert_job_user@'%';
GRANT ALL PRIVILEGES ON `llm_db`.* to alert_job_user@'%';

