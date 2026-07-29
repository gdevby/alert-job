# AGENTS.md — руководство для Cursor Agent

## Что это за проект

**alert-job** — микросервисная платформа для мониторинга заказов на фриланс-биржах (freelance.ru, fl.ru, weblancer, kwork и др.) с фильтрацией и уведомлениями. Продакшен: [aj.gdev.by](https://aj.gdev.by).

## Быстрый старт для агента

1. Прочитать `.cursor/rules/project-overview.mdc` — архитектура и модули
2. Для Java-задач — `.cursor/rules/java-backend.mdc`
3. Для парсеров — `.cursor/rules/parser-module.mdc`
4. Для UI — `.cursor/rules/frontend.mdc`
5. Для Docker/prod — `.cursor/rules/docker-deploy.mdc`

## Репозиторий

```
alert-job/                    # этот репозиторий (backend + front + infra)
├── alert-job-config/         # Config Server
├── alert-job-eureka/         # Eureka
├── alert-job-gateway/        # Gateway + OAuth2
├── parser-alert-job/         # Парсеры бирж
├── core-alert-job/           # Ядро (фильтры, пользователи, заказы)
├── notification-alert-job/     # Уведомления
├── llm-alert-job/              # AI-модуль
├── common-alert-job/           # Shared library
├── front/                      # React SPA
├── config/                     # nginx, prometheus, database init
├── keycloak/                   # Keycloak build scripts
├── docker-compose.yml          # dev
└── docker-compose-prod.yml     # prod
```

Внешний репозиторий конфигурации: `alert-job-config-repo` (клонируется отдельно).

## Типичные задачи

| Задача | Где смотреть |
|--------|-------------|
| Новая биржа | `parser-alert-job/.../service/order/`, `SiteName`, `AbsctractSiteParser` |
| API endpoint | `core-alert-job/.../controller/` |
| Фильтры пользователя | `core-alert-job/.../service/`, `UserFilterController` |
| Уведомления | `notification-alert-job/` |
| AI-автоответы | `llm-alert-job/`, `front/src/modules/auto-replies/` |
| Маршрутизация API | `alert-job-gateway/` |
| UI-страница | `front/src/pages/` |

## Сборка и проверка

```bash
# Backend
mvn clean install

# Frontend
cd front && npm i && npm run build

# Docker (dev)
docker compose up -d
```

## Ограничения

- Java 17, не обновлять версии Spring/Maven без запроса
- Не коммитить `.env`, секреты, `target/`, `node_modules/`, `dist/`
- Не исправлять исторические опечатки в именах классов
- Ответы агенту — на языке запроса пользователя (RU/EN)
- Коммиты и PR — только по явному запросу

## Полезные ссылки в репозитории

- `README_RU.md` — общее описание
- `README_FOR_DEVELOPERS_LINUX_RU.md` — настройка dev-окружения
- `env_sample.properties` — шаблон prod-переменных
