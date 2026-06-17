# DSMS: projekt REST API

## 1. Zasady ogólne

- Bazowa ścieżka: `/api/v1`.
- Format: JSON, z wyjątkiem uploadu plików i CSV.
- Identyfikatory zasobów: numeryczne `id`; zamówienia dodatkowo mają UUID
  `publicId`.
- Daty i czas: ISO 8601 UTC, na przykład `2026-06-15T16:30:00Z`.
- Kwoty pieniężne są przesyłane jako string, na przykład `"129.00"`.
- Paginacja: `page`, `size`, `sort`.
- Domyślny rozmiar strony to 20, maksimum 100.
- Usuwanie zasobów historycznych jest zastępowane dezaktywacją albo anulowaniem.

## 2. Format odpowiedzi

Pojedynczy zasób jest zwracany bezpośrednio:

```json
{
  "id": 42,
  "title": "Salsa Beginners",
  "startAt": "2026-06-20T16:00:00Z"
}
```

Strona:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Błąd jest zgodny z `application/problem+json`:

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

## 3. Statusy HTTP

| Kod | Zastosowanie |
|---|---|
| 200 | poprawny odczyt albo modyfikacja |
| 201 | zasób utworzony |
| 204 | poprawna operacja bez treści |
| 400 | niepoprawne parametry albo niedozwolone przejście statusu |
| 401 | brak autoryzacji albo nieważne uwierzytelnienie |
| 403 | niewystarczające uprawnienia |
| 404 | zasób nie został znaleziony |
| 409 | konflikt stanu, duplikat albo zajęte miejsce |
| 422 | zasada biznesowa nie pozwala wykonać operacji |
| 429 | przekroczony limit żądań albo prób logowania |

## 4. Authentication API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| POST | `/auth/register` | Public | rejestracja klienta |
| POST | `/auth/verify-email` | Public | potwierdzenie email przez token |
| POST | `/auth/resend-verification` | Public | ponowna wysyłka wiadomości |
| POST | `/auth/login` | Public | logowanie |
| POST | `/auth/refresh` | Cookie | odświeżenie access tokena |
| POST | `/auth/logout` | Authenticated | odwołanie bieżącej sesji |
| POST | `/auth/logout-all` | Authenticated | odwołanie wszystkich sesji |
| POST | `/auth/forgot-password` | Public | żądanie resetu hasła |
| POST | `/auth/reset-password` | Public | ustawienie nowego hasła |
| POST | `/auth/change-password` | Authenticated | zmiana hasła |

Logowanie ma osobny rate limit. Odpowiedzi resetu hasła nie ujawniają, czy
podany email istnieje.

## 5. Profile and Users API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/me` | Authenticated | aktualny profil |
| PATCH | `/me` | Authenticated | zmiana profilu |
| POST | `/me/avatar` | Authenticated | upload zdjęcia |
| DELETE | `/me/avatar` | Authenticated | usunięcie zdjęcia |
| GET | `/admin/users` | ADMIN | lista i filtrowanie użytkowników |
| GET | `/admin/users/{id}` | ADMIN | dane użytkownika |
| PATCH | `/admin/users/{id}/status` | ADMIN | blokada albo aktywacja |
| PATCH | `/admin/users/{id}/role` | ADMIN | zmiana roli |

## 6. Instructors and Catalog API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/instructors` | Public | publiczna lista instruktorów |
| GET | `/instructors/{id}` | Public | profil instruktora |
| POST | `/admin/instructors` | ADMIN | utworzenie profilu instruktora |
| PATCH | `/admin/instructors/{id}` | ADMIN | zmiana profilu |
| GET | `/dance-styles` | Public | aktywne style |
| POST | `/admin/dance-styles` | ADMIN | utworzenie stylu |
| PATCH | `/admin/dance-styles/{id}` | ADMIN | zmiana stylu |
| PATCH | `/admin/dance-styles/{id}/status` | ADMIN | aktywacja albo dezaktywacja |

## 7. Schedule API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/classes` | Public | grafik z filtrami |
| GET | `/classes/{id}` | Public | karta zajęć |
| POST | `/admin/classes` | ADMIN | utworzenie zajęć |
| PATCH | `/admin/classes/{id}` | ADMIN | zmiana zajęć |
| POST | `/admin/classes/{id}/publish` | ADMIN | publikacja |
| POST | `/admin/classes/{id}/cancel` | ADMIN | anulowanie |
| GET | `/instructor/classes` | INSTRUCTOR | własne zajęcia |
| GET | `/instructor/classes/{id}/participants` | INSTRUCTOR | uczestnicy własnych zajęć |

Filtry `GET /classes`:

- `from`, `to`;
- `instructorId`;
- `danceStyleId`;
- `level`;
- `availability`;
- standardowa paginacja i sortowanie.

## 8. Booking API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| POST | `/classes/{id}/reservations` | CLIENT | rezerwacja albo dołączenie do kolejki |
| DELETE | `/classes/{id}/reservations/me` | CLIENT | anulowanie własnej rezerwacji |
| DELETE | `/classes/{id}/waitlist/me` | CLIENT | opuszczenie kolejki |
| GET | `/me/reservations` | CLIENT | historia rezerwacji |
| GET | `/me/waitlist` | CLIENT | aktywne pozycje w kolejce |
| GET | `/admin/classes/{id}/reservations` | ADMIN | rezerwacje i kolejka zajęć |
| POST | `/admin/classes/{id}/reservations` | ADMIN | ręczna rezerwacja dla klienta |
| DELETE | `/admin/reservations/{id}` | ADMIN | administracyjne anulowanie |

Odpowiedź `POST /classes/{id}/reservations` zwraca jeden ze statusów:

- `CONFIRMED`;
- `WAITLISTED`.

Przy powtórzeniach klient może przekazać nagłówek `Idempotency-Key`.

## 9. Passes API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/pass-types` | Public | dostępne typy karnetów |
| GET | `/pass-types/{id}` | Public | karta karnetu |
| GET | `/me/passes` | CLIENT | własne karnety |
| GET | `/me/passes/{id}/ledger` | CLIENT | historia wejść dla karnetu |
| POST | `/admin/pass-types` | ADMIN | utworzenie typu |
| PATCH | `/admin/pass-types/{id}` | ADMIN | zmiana typu |
| PATCH | `/admin/pass-types/{id}/status` | ADMIN | aktywacja albo dezaktywacja |
| POST | `/admin/user-passes/{id}/adjustments` | ADMIN | ręczna korekta z uzasadnieniem |

## 10. Orders and Payments API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| POST | `/orders` | CLIENT | utworzenie zamówienia na karnet |
| GET | `/orders/{publicId}` | Owner/ADMIN | stan zamówienia |
| POST | `/orders/{publicId}/payments` | Owner | rozpoczęcie płatności PayU |
| POST | `/orders/{publicId}/cancel` | Owner | anulowanie nieopłaconego zamówienia |
| GET | `/me/payments` | CLIENT | historia płatności |
| GET | `/admin/payments` | ADMIN | wyszukiwanie płatności |
| GET | `/admin/payments/{id}` | ADMIN | dane płatności |
| POST | `/payments/payu/notifications` | PayU | callback dostawcy |

Callback nie używa JWT. Dostęp jest określany przez weryfikację podpisu PayU,
identyfikatora zamówienia, kwoty i waluty.

## 11. Attendance API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| PUT | `/instructor/classes/{id}/attendance` | Instructor owner | pakietowe oznaczanie obecności |
| GET | `/instructor/classes/{id}/attendance` | Instructor owner | obecność na zajęciach |
| PUT | `/admin/classes/{id}/attendance` | ADMIN | korekta administracyjna |
| GET | `/me/attendance` | CLIENT | historia obecności |

Żądanie pakietowe zawiera listę `reservationId` oraz `PRESENT`/`ABSENT`.
Zmiana po ustalonym oknie jest rejestrowana audytowo.

## 12. Reviews API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/classes/{id}/reviews` | Public | widoczne opinie |
| POST | `/classes/{id}/reviews` | CLIENT | utworzenie opinii |
| PATCH | `/reviews/{id}` | Author | zmiana własnej opinii |
| DELETE | `/reviews/{id}` | Author | usunięcie własnej opinii |
| PATCH | `/admin/reviews/{id}/visibility` | ADMIN | ukrycie albo pokazanie |

## 13. Events API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/events` | Public | opublikowane wydarzenia |
| GET | `/events/{id}` | Public | karta wydarzenia |
| POST | `/admin/events` | ADMIN | utworzenie wydarzenia |
| PATCH | `/admin/events/{id}` | ADMIN | zmiana wydarzenia |
| POST | `/admin/events/{id}/publish` | ADMIN | publikacja |
| POST | `/admin/events/{id}/cancel` | ADMIN | anulowanie |

Sprzedaż biletów zostanie dodana po doprecyzowaniu scenariusza wydarzeń.

## 14. Reports API

| Metoda | Endpoint | Dostęp | Przeznaczenie |
|---|---|---|---|
| GET | `/admin/reports/revenue` | ADMIN | przychód |
| GET | `/admin/reports/payments` | ADMIN | statusy płatności |
| GET | `/admin/reports/pass-sales` | ADMIN | sprzedaż karnetów |
| GET | `/admin/reports/class-occupancy` | ADMIN | obłożenie zajęć |
| GET | `/admin/reports/attendance` | ADMIN | obecności i nieobecności |
| GET | `/admin/reports/dance-styles` | ADMIN | popularność stylów |
| GET | `/admin/reports/clients` | ADMIN | aktywność klientów |
| GET | `/admin/reports/instructors` | ADMIN | statystyki instruktorów |
| GET | `/admin/reports/{report}/export` | ADMIN | CSV wybranego raportu |

Wszystkie raporty przyjmują `from` i `to`; dodatkowe filtry zależą od raportu.

## 15. Idempotentność i współbieżność

- `POST /orders` oraz tworzenie płatności wspierają `Idempotency-Key`.
- Powtórzony callback PayU zwraca poprawną odpowiedź bez ponownego zastosowania.
- Rezerwacja jest chroniona przez transakcyjną blokadę zajęć.
- Zmienne zasoby administracyjne używają pola `version`; konflikt wersji
  zwraca `409 CONFLICT`.

## 16. OpenAPI

Backend publikuje:

- `/v3/api-docs`;
- `/swagger-ui.html`.

Kontrakt OpenAPI jest traktowany jako źródło prawdy dla DTO po rozpoczęciu
backendu. Na jego podstawie można generować typy i klienta API dla frontendu.
