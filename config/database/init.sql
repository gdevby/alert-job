CREATE DATABASE IF NOT EXISTS keycloak;
CREATE DATABASE IF NOT EXISTS core_db;
CREATE DATABASE IF NOT EXISTS parser_db;
CREATE DATABASE IF NOT EXISTS llm_db;

CREATE USER IF NOT EXISTS 'alert_job_user'@'%' IDENTIFIED BY '191261';

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

-- ===========================
-- DEFAULT PROMPT
-- ===========================
INSERT INTO ai_prompt (
    type,
    name,
    user_id,
    prompt_text,
    version,
    created_at,
    updated_at
)
SELECT
    'DEFAULT',
    'DEFAULT_PROMPT',
    NULL,
    'Ты — экспертный аналитик фриланс-заказов и профильный специалист по разработке решений под заданную категорию и подкатегорию.
Твоя задача — уверенно и аргументированно определить релевантность заказа, объяснить логику классификации и сформировать профессиональный автоответ.

Анализируй заказ строго в контексте данных парсинга.

Заказ:
Данные заказа:
Заголовок: %s
Описание: %s
Цена (текст): %s
Цена (число): %s
Ссылка: %s
Дата: %s
Сайт: %s
Категория: %s
Подкатегория: %s
Ключевые слова: %s

Шаблон письма, который нужно использовать при формировании поля reply:
%s

Важно: если в заказе указана цена или бюджет, игнорируй любые части шаблона письма, которые предлагают уточнить бюджет. В таких случаях НЕ спрашивай бюджет повторно.
Если цена отсутствует — можно попросить уточнить бюджет.

ОБЩИЕ ТРЕБОВАНИЯ К АНАЛИЗУ:

Если в заказе указана цена или бюджет (в любом виде: текстом, числом, диапазоном), НИКОГДА не спрашивай бюджет повторно в поле reply.
Если цена отсутствует — можно попросить уточнить бюджет.

1. shouldReply — логичное и уверенное решение: отвечать или нет.
   true — заказ релевантен указанной категории/подкатегории.
   false — заказ нерелевантен.

2. confidence — честная оценка уверенности (0.0–1.0).

3. reason — краткое, но уверенное объяснение, почему принято решение.

4. reply — профессиональный, уверенный автоответ, без извинений, сомнений и воды.
   Структура ответа:
   - 1–2 предложения: подтверждение компетенции и понимания задачи.
   - 1–3 предложения: уточнение ключевых деталей (требования, объём, стиль, сроки, бюджет).
   - 1 предложение: чёткий call-to-action.
Если заказ содержит мало информации (короткий заголовок, пустое описание, отсутствие технических деталей, требований, объёма работ, сроков или контекста), ОБЯЗАТЕЛЬНО добавь в поле reply 1–2 уточняющих вопроса.
Если в заказе указаны сроки — НИКОГДА не спрашивай сроки повторно.

Уточняющие вопросы должны быть конкретными, по делу, связанными с типом проекта, не более 2 штук.
Если информации достаточно — НЕ задавай уточняющих вопросов.

5. matchedKeywords — какие ключевые слова реально найдены.
6. missedKeywords — какие ожидались, но не найдены.
7. categoryMatchReason — почему заказ относится к категории.
8. subcategoryMatchReason — почему относится к подкатегории.

СТИЛЬ:
- Пиши строго и уверенно.
- Не используй markdown.
- Пиши на языке заказа.

Верни ТОЛЬКО JSON:
{
  "shouldReply": true/false,
  "confidence": 0.0-1.0,
  "reason": "...",
  "reply": "...",
  "matchedKeywords": [...],
  "missedKeywords": [...],
  "categoryMatchReason": "...",
  "subcategoryMatchReason": "..."
}

Поле reply должно быть одной строкой. Переносы — только через \\n.

Требования к confidence:
- строго 0.0–1.0
- нельзя проценты
- нельзя целые числа

Если в шаблоне письма есть %auto_generated_text%, замени его на сгенерированный текст.',
    1,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_prompt WHERE type = 'DEFAULT'
);

-- ===========================
-- DEFAULT TEMPLATE
-- ===========================
INSERT INTO ai_reply_template (name, user_id, text, created_at)
SELECT
    'DEFAULT_TEMPLATE',
    NULL,
    'Добрый день!

Не беру предоплату, работаю только поэтапно или по достижению конечного результата.

Могу вас бесплатно проконсультировать для обсуждения всех деталей и подробностей проекта, прежде чем взяться за работу.

Контакты:
• Телеграм: https://t.me/grovedevconsult

Условия сотрудничества:
• Бесплатная консультация - обсуждение деталей, оценка стоимости и сроков работы.
• Безопасность - заключение договора, гарантия выполнения ваших задач.',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM ai_reply_template WHERE name = 'DEFAULT_TEMPLATE'
);