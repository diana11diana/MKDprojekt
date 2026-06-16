# DSMS: проектирование REST API

## 1. Общие правила

- Базовый путь: `/api/v1`.
- Формат: JSON, кроме загрузки файлов и CSV.
- Идентификаторы ресурсов: числовые `id`; заказы дополнительно имеют UUID
  `publicId`.
- Даты и время: ISO 8601 UTC, например `2026-06-15T16:30:00Z`.
- Денежные суммы передаются строкой, например `"129.00"`.
- Пагинация: `page`, `size`, `sort`.
- Размер страницы по умолчанию 20, максимум 100.
- Удаление исторических ресурсов заменяется деактивацией или отменой.

## 2. Формат ответа

Одиночный ресурс возвращается напрямую:

```json
{
  "id": 42,
  "title": "Salsa Beginners",
  "startAt": "2026-06-20T16:00:00Z"
}
```

Страница:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Ошибка соответствует `application/problem+json`:

```json
{
  "type": "https://dsms.example/problems/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request contains invalid fields",
  "instance": "/api/v1/auth/register",
  "code": "VALIDATION_ERROR",
  "traceId": "c2d91f...",
  "fieldErrors": {
    "email": "Invalid email format"
  }
}
```

## 3. Статусы HTTP

| Код | Использование |
|---|---|
| 200 | успешное чтение или изменение |
| 201 | ресурс создан |
| 204 | успешная операция без тела |
| 400 | некорректные параметры или переход статуса |
| 401 | отсутствует или недействительна аутентификация |
| 403 | недостаточно прав |
| 404 | ресурс не найден |
| 409 | конфликт состояния, дубликат или занятое место |
| 422 | бизнес-правило не позволяет выполнить операцию |
| 429 | превышен лимит запросов или попыток входа |

## 4. Authentication API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| POST | `/auth/register` | Public | регистрация клиента |
| POST | `/auth/verify-email` | Public | подтверждение email по токену |
| POST | `/auth/resend-verification` | Public | повторное письмо |
| POST | `/auth/login` | Public | вход |
| POST | `/auth/refresh` | Cookie | обновление access token |
| POST | `/auth/logout` | Authenticated | отзыв текущей сессии |
| POST | `/auth/logout-all` | Authenticated | отзыв всех сессий |
| POST | `/auth/forgot-password` | Public | запрос сброса пароля |
| POST | `/auth/reset-password` | Public | установка нового пароля |
| POST | `/auth/change-password` | Authenticated | смена пароля |

Login имеет отдельный rate limit. Ответы восстановления пароля не раскрывают,
существует ли указанный email.

## 5. Profile and Users API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/me` | Authenticated | текущий профиль |
| PATCH | `/me` | Authenticated | изменение профиля |
| POST | `/me/avatar` | Authenticated | загрузка фотографии |
| DELETE | `/me/avatar` | Authenticated | удаление фотографии |
| GET | `/admin/users` | ADMIN | список и фильтрация пользователей |
| GET | `/admin/users/{id}` | ADMIN | данные пользователя |
| PATCH | `/admin/users/{id}/status` | ADMIN | блокировка или активация |
| PATCH | `/admin/users/{id}/role` | ADMIN | изменение роли |

## 6. Instructors and Catalog API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/instructors` | Public | публичный список инструкторов |
| GET | `/instructors/{id}` | Public | профиль инструктора |
| POST | `/admin/instructors` | ADMIN | создание профиля инструктора |
| PATCH | `/admin/instructors/{id}` | ADMIN | изменение профиля |
| GET | `/dance-styles` | Public | активные направления |
| POST | `/admin/dance-styles` | ADMIN | создание направления |
| PATCH | `/admin/dance-styles/{id}` | ADMIN | изменение направления |
| PATCH | `/admin/dance-styles/{id}/status` | ADMIN | активация или деактивация |

## 7. Schedule API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/classes` | Public | расписание с фильтрами |
| GET | `/classes/{id}` | Public | карточка занятия |
| POST | `/admin/classes` | ADMIN | создание занятия |
| PATCH | `/admin/classes/{id}` | ADMIN | изменение занятия |
| POST | `/admin/classes/{id}/publish` | ADMIN | публикация |
| POST | `/admin/classes/{id}/cancel` | ADMIN | отмена |
| GET | `/instructor/classes` | INSTRUCTOR | свои занятия |
| GET | `/instructor/classes/{id}/participants` | INSTRUCTOR | участники своего занятия |

Фильтры `GET /classes`:

- `from`, `to`;
- `instructorId`;
- `danceStyleId`;
- `level`;
- `availability`;
- стандартная пагинация и сортировка.

## 8. Booking API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| POST | `/classes/{id}/reservations` | CLIENT | забронировать или войти в очередь |
| DELETE | `/classes/{id}/reservations/me` | CLIENT | отменить свою бронь |
| DELETE | `/classes/{id}/waitlist/me` | CLIENT | выйти из очереди |
| GET | `/me/reservations` | CLIENT | история бронирований |
| GET | `/me/waitlist` | CLIENT | активные позиции в очереди |
| GET | `/admin/classes/{id}/reservations` | ADMIN | брони и очередь занятия |
| POST | `/admin/classes/{id}/reservations` | ADMIN | ручное бронирование клиента |
| DELETE | `/admin/reservations/{id}` | ADMIN | административная отмена |

Ответ `POST /classes/{id}/reservations` возвращает один из статусов:

- `CONFIRMED`;
- `WAITLISTED`.

Для повторов клиент может передавать заголовок `Idempotency-Key`.

## 9. Passes API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/pass-types` | Public | доступные типы абонементов |
| GET | `/pass-types/{id}` | Public | карточка абонемента |
| GET | `/me/passes` | CLIENT | свои абонементы |
| GET | `/me/passes/{id}/ledger` | CLIENT | история посещений абонемента |
| POST | `/admin/pass-types` | ADMIN | создание типа |
| PATCH | `/admin/pass-types/{id}` | ADMIN | изменение типа |
| PATCH | `/admin/pass-types/{id}/status` | ADMIN | активация или деактивация |
| POST | `/admin/user-passes/{id}/adjustments` | ADMIN | ручная корректировка с причиной |

## 10. Orders and Payments API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| POST | `/orders` | CLIENT | создать заказ на абонемент |
| GET | `/orders/{publicId}` | Owner/ADMIN | состояние заказа |
| POST | `/orders/{publicId}/payments` | Owner | начать оплату PayU |
| POST | `/orders/{publicId}/cancel` | Owner | отменить неоплаченный заказ |
| GET | `/me/payments` | CLIENT | история платежей |
| GET | `/admin/payments` | ADMIN | поиск платежей |
| GET | `/admin/payments/{id}` | ADMIN | данные платежа |
| POST | `/payments/payu/notifications` | PayU | callback провайдера |

Callback не использует JWT. Доступ определяется проверкой подписи PayU,
идентификатора заказа, суммы и валюты.

## 11. Attendance API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| PUT | `/instructor/classes/{id}/attendance` | Instructor owner | пакетная отметка посещаемости |
| GET | `/instructor/classes/{id}/attendance` | Instructor owner | посещаемость занятия |
| PUT | `/admin/classes/{id}/attendance` | ADMIN | административная корректировка |
| GET | `/me/attendance` | CLIENT | история посещений |

Пакетный запрос содержит список `reservationId` и `PRESENT`/`ABSENT`.
Изменение после установленного окна аудируется.

## 12. Reviews API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/classes/{id}/reviews` | Public | видимые отзывы |
| POST | `/classes/{id}/reviews` | CLIENT | создать отзыв |
| PATCH | `/reviews/{id}` | Author | изменить свой отзыв |
| DELETE | `/reviews/{id}` | Author | удалить свой отзыв |
| PATCH | `/admin/reviews/{id}/visibility` | ADMIN | скрыть или показать |

## 13. Events API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/events` | Public | опубликованные мероприятия |
| GET | `/events/{id}` | Public | карточка мероприятия |
| POST | `/admin/events` | ADMIN | создать мероприятие |
| PATCH | `/admin/events/{id}` | ADMIN | изменить мероприятие |
| POST | `/admin/events/{id}/publish` | ADMIN | опубликовать |
| POST | `/admin/events/{id}/cancel` | ADMIN | отменить |

Продажа билетов добавляется после уточнения сценария мероприятий.

## 14. Reports API

| Метод | Endpoint | Доступ | Назначение |
|---|---|---|---|
| GET | `/admin/reports/revenue` | ADMIN | выручка |
| GET | `/admin/reports/payments` | ADMIN | статусы платежей |
| GET | `/admin/reports/pass-sales` | ADMIN | продажи абонементов |
| GET | `/admin/reports/class-occupancy` | ADMIN | загрузка занятий |
| GET | `/admin/reports/attendance` | ADMIN | посещения и неявки |
| GET | `/admin/reports/dance-styles` | ADMIN | популярность направлений |
| GET | `/admin/reports/clients` | ADMIN | активность клиентов |
| GET | `/admin/reports/instructors` | ADMIN | статистика инструкторов |
| GET | `/admin/reports/{report}/export` | ADMIN | CSV выбранного отчета |

Все отчеты принимают `from` и `to`; дополнительные фильтры зависят от отчета.

## 15. Идемпотентность и конкурентность

- `POST /orders` и создание платежа поддерживают `Idempotency-Key`.
- Повторный PayU callback возвращает успешный ответ без повторного применения.
- Бронирование защищается транзакционной блокировкой занятия.
- Изменяемые административные ресурсы используют поле `version`; конфликт
  версии возвращает `409 CONFLICT`.

## 16. OpenAPI

Backend публикует:

- `/v3/api-docs`;
- `/swagger-ui.html`.

Контракт OpenAPI считается источником истины для DTO после начала backend.
Из него можно генерировать типы и API-клиент frontend.

