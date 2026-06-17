# Dance School Management System

Roboczy projekt full-stack dla szkoły tańca:

- `backend` - Java 21, Spring Boot, Spring Security, JPA, Flyway;
- `frontend` - React, Material UI, Axios, Vite;
- `mysql` - MySQL 8;
- główny `docker-compose.yml` uruchamia cały stos.

## Uruchomienie przez Docker

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Po uruchomieniu:

- aplikacja: http://localhost:3000
- backend API: http://localhost:8080/api/v1/classes
- Swagger: http://localhost:8080/swagger-ui.html

## Lokalny development frontendu

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
```

## Aktualna funkcjonalność

- responsywna strona startowa;
- wczytywanie grafiku z REST API;
- filtrowanie według stylu;
- rejestracja i potwierdzanie adresu email jednorazowym tokenem;
- logowanie z użyciem BCrypt i blokadą po pięciu błędnych próbach;
- JWT access token oraz rotowany refresh token w ciasteczku `HttpOnly`;
- chroniony profil użytkownika i wylogowanie;
- strony rejestracji, logowania i profilu;
- edycja danych osobowych;
- panel administracyjny użytkowników, instruktorów i grafiku;
- zarządzanie rolami i statusami użytkowników;
- tworzenie profili instruktorów;
- tworzenie, publikacja i anulowanie zajęć;
- Spring Security i CORS;
- schemat MySQL zarządzany przez Flyway;
- przykładowe zajęcia demonstracyjne;
- dockerowy build frontendu i backendu.

W środowisku lokalnym token potwierdzenia email wraca do frontendu i jest
stosowany automatycznie. W środowisku production należy ustawić
`EXPOSE_VERIFICATION_TOKEN=false` i podłączyć wysyłkę linków przez SMTP.

Aby utworzyć pierwszego administratora, ustaw:

```text
BOOTSTRAP_ADMIN_ENABLED=true
ADMIN_EMAIL=admin@dsms.local
ADMIN_PASSWORD=Admin1234
```

W Docker Compose bootstrap jest włączony dla lokalnego developmentu. Hasło
należy zmienić przed jakimkolwiek publicznym wdrożeniem.

Kolejny blok funkcjonalny: rezerwacje zajęć, lista oczekujących i rozliczanie
karnetów.
