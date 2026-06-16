<<<<<<< HEAD
# Dance School Management System

Рабочий full-stack проект школы танцев:

- `backend` — Java 21, Spring Boot, Spring Security, JPA, Flyway;
- `frontend` — React, Material UI, Axios, Vite;
- `mysql` — MySQL 8;
- корневой `docker-compose.yml` запускает весь стек.

## Запуск через Docker

```powershell
Copy-Item .env.example .env
docker compose up --build
```

После запуска:

- приложение: http://localhost:3000
- backend API: http://localhost:8080/api/v1/classes
- Swagger: http://localhost:8080/swagger-ui.html

## Локальная разработка frontend

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
```

## Текущий функционал

- стартовая адаптивная страница;
- загрузка расписания из REST API;
- фильтрация по направлению;
- регистрация и подтверждение email одноразовым токеном;
- вход с BCrypt и блокировкой после пяти неверных попыток;
- JWT access token и ротируемый refresh token в HttpOnly cookie;
- защищенный личный профиль и выход;
- страницы регистрации, входа и профиля;
- редактирование личных данных;
- административная панель пользователей, инструкторов и расписания;
- управление ролями и статусами пользователей;
- создание профилей инструкторов;
- создание, публикация и отмена занятий;
- Spring Security и CORS;
- схема MySQL через Flyway;
- демонстрационные занятия;
- Docker-сборка frontend и backend.

В локальной среде токен подтверждения email возвращается frontend и применяется
автоматически. В production следует установить `EXPOSE_VERIFICATION_TOKEN=false`
и подключить отправку ссылки через SMTP.

Для создания первого администратора задайте:

```text
BOOTSTRAP_ADMIN_ENABLED=true
ADMIN_EMAIL=admin@dsms.local
ADMIN_PASSWORD=Admin1234
```

В Docker Compose bootstrap включён для локальной разработки. Пароль необходимо
заменить перед любым публичным развертыванием.

Следующий функциональный блок: бронирование занятий, лист ожидания и учет
абонементов.
=======
# inz
>>>>>>> 5cf0b596b45f63574323f689ad83db3e4e66c9c5
