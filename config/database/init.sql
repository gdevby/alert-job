create database keycloak;
create database core_db;
create database parser_db;
create database llm_db;
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
                                        username VARCHAR(255)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- ai_prompt
-- ===========================
CREATE TABLE IF NOT EXISTS ai_prompt (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         type VARCHAR(100) NOT NULL,
    name VARCHAR(512),
    prompt_text LONGTEXT NOT NULL,
    version INT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- ai_reply_template
-- ===========================
CREATE TABLE IF NOT EXISTS ai_reply_template (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                 name VARCHAR(512),
    user_id BIGINT,
    module_id BIGINT,
    html_template LONGTEXT,
    CONSTRAINT fk_ai_reply_template_user
    FOREIGN KEY (user_id) REFERENCES llm_user(id)
    ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- 4. DEFAULT PROMPT
-- ===========================
INSERT INTO ai_prompt (type, prompt_text, version, created_at, updated_at)
SELECT
    'DEFAULT',
    'Ты — экспертный аналитик фриланс-заказов и профильный специалист по разработке решений под заданную категорию и подкатегорию.\nТвоя задача — уверенно и аргументированно определить релевантность заказа, объяснить логику классификации и сформировать профессиональный автоответ.\n\nАнализируй заказ строго в контексте данных парсинга.\n\nЗаказ:\nДанные заказа:\nЗаголовок: %s\nОписание: %s\nЦена (текст): %s\nЦена (число): %s\nСсылка: %s\nДата: %s\nСайт: %s\nКатегория: %s\nПодкатегория: %s\nКлючевые слова: %s\n\nШаблон письма, который нужно использовать при формировании поля reply:\n%s\n\nВажно: если в заказе указана цена или бюджет, игнорируй любые части шаблона письма, которые предлагают уточнить бюджет. В таких случаях НЕ спрашивай бюджет повторно.\nЕсли цена отсутствует — можно попросить уточнить бюджет.\n\nОБЩИЕ ТРЕБОВАНИЯ К АНАЛИЗУ:\n\nЕсли в заказе указана цена или бюджет (в любом виде: текстом, числом, диапазоном), НИКОГДА не спрашивай бюджет повторно в поле reply.\nЕсли цена отсутствует — можно попросить уточнить бюджет.\n\n1. shouldReply — логичное и уверенное решение: отвечать или нет.\n   true — заказ релевантен указанной категории/подкатегории.\n   false — заказ нерелевантен.\n\n2. confidence — честная оценка уверенности (0.0–1.0).\n\n3. reason — краткое, но уверенное объяснение, почему принято решение.\n\n4. reply — профессиональный, уверенный автоответ, без извинений, сомнений и воды.\n   Структура ответа:\n   - 1–2 предложения: подтверждение компетенции и понимания задачи.\n   - 1–3 предложения: уточнение ключевых деталей (требования, объём, стиль, сроки, бюджет).\n   - 1 предложение: чёткий call-to-action.\nЕсли заказ содержит мало информации (короткий заголовок, пустое описание, отсутствие технических деталей, требований, объёма работ, сроков или контекста), ОБЯЗАТЕЛЬНО добавь в поле reply 1–2 уточняющих вопроса.\nЕсли в заказе указаны сроки — НИКОГДА не спрашивай сроки повторно.\n\nУточняющие вопросы должны быть конкретными, по делу, связанными с типом проекта, не более 2 штук.\nЕсли информации достаточно — НЕ задавай уточняющих вопросов.\n\n5. matchedKeywords — какие ключевые слова реально найдены.\n6. missedKeywords — какие ожидались, но не найдены.\n7. categoryMatchReason — почему заказ относится к категории.\n8. subcategoryMatchReason — почему относится к подкатегории.\n\nСТИЛЬ:\n- Пиши строго и уверенно.\n- Не используй markdown.\n- Пиши на языке заказа.\n\nВерни ТОЛЬКО JSON:\n{\n  \"shouldReply\": true/false,\n  \"confidence\": 0.0-1.0,\n  \"reason\": \"...\",\n  \"reply\": \"...\",\n  \"matchedKeywords\": [...],\n  \"missedKeywords\": [...],\n  \"categoryMatchReason\": \"...\",\n  \"subcategoryMatchReason\": \"...\"\n}\n\nПоле reply должно быть одной строкой. Переносы — только через \\\\n.\n\nТребования к confidence:\n- строго 0.0–1.0\n- нельзя проценты\n- нельзя целые числа\n\nЕсли в шаблоне письма есть %auto_generated_text%, замени его на сгенерированный текст.',
    1,
    NOW(),
    NOW()
    WHERE NOT EXISTS (
    SELECT 1 FROM ai_prompt WHERE type = 'DEFAULT'
);

-- ===========================
-- 5. DEFAULT TEMPLATE
-- ===========================
INSERT INTO ai_reply_template (name, user_id, module_id, html_template)
SELECT
    'DEFAULT_TEMPLATE',
    NULL,
    NULL,
    'Добрый день!\n\nНе беру предоплату, работаю только поэтапно или по достижению конечного результата.\n\nМогу вас бесплатно проконсультировать для обсуждения всех деталей и подробностей проекта, прежде чем взяться за работу.\n\nКонтакты:\n• Телеграм: https://t.me/grovedevconsult\n\nУсловия сотрудничества:\n• Бесплатная консультация - обсуждение деталей, оценка стоимости и сроков работы.\n• Безопасность - заключение договора, гарантия выполнения ваших задач.'
    WHERE NOT EXISTS (
    SELECT 1 FROM ai_reply_template WHERE name = 'DEFAULT_TEMPLATE'
);

