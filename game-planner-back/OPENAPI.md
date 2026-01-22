# OpenAPI документация

Проект использует SpringDoc OpenAPI для автоматической генерации документации API из аннотаций контроллеров.

## Настройка

В проект добавлена зависимость `springdoc-openapi-starter-webmvc-ui`, которая автоматически генерирует OpenAPI спецификацию из аннотаций контроллеров.

## Доступ к документации

После запуска приложения документация доступна по следующим URL:

- **OpenAPI JSON спецификация**: http://localhost:8080/api/api-docs
- **OpenAPI YAML спецификация**: http://localhost:8080/api/api-docs.yaml
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html

**⚠️ Важно**: Доступ к OpenAPI документации и Swagger UI ограничен только для пользователей с правами администратора. Для доступа необходимо:
1. Авторизоваться в системе (получить JWT токен)
2. Иметь роль администратора (`isAdmin = true`)
3. Использовать токен при обращении к эндпоинтам (через браузер с авторизацией или с заголовком `Authorization: Bearer <token>`)

## Настройка OpenAPI

Настройки OpenAPI находятся в `application.properties`:

```properties
springdoc.api-docs.path=/api/api-docs
springdoc.swagger-ui.path=/api/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

**Безопасность**: Доступ к OpenAPI эндпоинтам ограничен ролью `ADMIN` в `SecurityConfig`. Для доступа пользователь должен быть авторизован и иметь роль администратора.

## Получение спецификации

Для получения OpenAPI спецификации в файл можно использовать любой HTTP-клиент (curl, Postman, браузер) с авторизацией администратора:

```bash
curl -H "Authorization: Bearer <admin_jwt_token>" http://localhost:8080/api/api-docs > openapi.json
```

Для более детальной настройки (название API, версия, описание) можно создать конфигурационный класс с аннотациями `@OpenAPIDefinition` и `@Operation`.
