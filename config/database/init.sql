CREATE DATABASE IF NOT EXISTS keycloak;
CREATE DATABASE IF NOT EXISTS core_db;
CREATE DATABASE IF NOT EXISTS parser_db;
CREATE DATABASE IF NOT EXISTS llm_db;

CREATE USER IF NOT EXISTS 'alert_job_user'@'%' IDENTIFIED BY 'password_replace';

GRANT ALL PRIVILEGES ON `keycloak`.* to alert_job_user@'%';
GRANT ALL PRIVILEGES ON `core_db`.* to alert_job_user@'%';
GRANT ALL PRIVILEGES ON `parser_db`.* to alert_job_user@'%';
GRANT ALL PRIVILEGES ON `llm_db`.* to alert_job_user@'%';

USE llm_db;

-- ===========================
-- llm_user
-- ===========================
CREATE TABLE IF NOT EXISTS llm_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uuid VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- ai_prompt
-- ===========================
CREATE TABLE IF NOT EXISTS ai_prompt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(100) NOT NULL,
    name VARCHAR(512),
    user_id BIGINT,
    prompt_text LONGTEXT NOT NULL,
    version INT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_prompt_user
        FOREIGN KEY (user_id) REFERENCES llm_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- ai_reply_template
-- ===========================
CREATE TABLE IF NOT EXISTS ai_reply_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(512),
    user_id BIGINT,
    text LONGTEXT,
    CONSTRAINT fk_ai_reply_template_user
        FOREIGN KEY (user_id) REFERENCES llm_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- DEFAULT_PROMPT и DEFAULT_TEMPLATE создаются при старте llm-alert-job
-- (LlmDefaultDataInitializer, prompts/*.txt)