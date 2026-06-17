# DSMS: analiza wymagań

## 1. Cel dokumentu

Dokument doprecyzowuje wyjściowe założenia techniczne dla systemu zarządzania
szkołą tańca i określa zakres pierwszej wersji produktu.

## 2. Cele produktu

System ma:

- umożliwić klientom samodzielny zapis na zajęcia;
- zautomatyzować sprzedaż i ewidencję karnetów;
- ograniczyć ręczną pracę administratorów;
- dać instruktorom narzędzia do ewidencji obecności;
- zapewnić szkole przejrzyste rozliczanie klientów, rezerwacji i płatności.

## 3. Role i uprawnienia

### Gość

- przegląda publiczny grafik;
- filtruje zajęcia;
- rejestruje się;
- loguje się do systemu;
- odzyskuje hasło.

### Klient

- zarządza profilem i zdjęciem;
- przegląda grafik i liczbę wolnych miejsc;
- rezerwuje i anuluje zajęcia;
- dołącza do listy oczekujących;
- kupuje i przegląda karnety;
- przegląda rezerwacje, obecności i płatności;
- zostawia opinie po odbytych zajęciach.

### Instruktor

- przegląda wyłącznie własne zajęcia;
- przegląda uczestników swoich zajęć;
- oznacza obecności i nieobecności;
- przegląda statystyki swoich zajęć.

### Administrator

- zarządza użytkownikami i instruktorami;
- zarządza stylami i zajęciami;
- zarządza typami karnetów;
- przegląda rezerwacje i płatności;
- tworzy wydarzenia;
- przegląda raporty i statystyki ogólne.

## 4. Zakres MVP

Do pierwszej działającej wersji wchodzą:

1. Rejestracja, potwierdzenie email, logowanie i odzyskiwanie hasła.
2. Model ról CLIENT, INSTRUCTOR i ADMIN.
3. Profil użytkownika.
4. Katalog zajęć i grafik z filtrami.
5. Rezerwacja, anulowanie i lista oczekujących.
6. Karnety limitowane i nielimitowane.
7. Płatność przez PayU z obsługą BLIK w ramach PayU.
8. Powiadomienia email.
9. Ewidencja obecności.
10. Opinie i oceny.
11. Panele klienta, instruktora i administratora.
12. Podstawowe raporty finansowe i operacyjne.
13. Swagger/OpenAPI oraz uruchamianie przez Docker Compose.

Poza zakresem MVP:

- aplikacja mobilna;
- SMS-y i powiadomienia push;
- kody promocyjne, rabaty i program lojalnościowy;
- naliczanie wynagrodzeń instruktorów;
- integracja z księgowością;
- zajęcia wideo;
- rezerwacje dla wielu oddziałów;
- automatyczne odtworzenie całej infrastruktury po awarii.

## 5. Główne scenariusze użytkownika

### UC-01. Rejestracja klienta

1. Gość podaje imię, nazwisko, email, telefon i hasło.
2. System sprawdza unikalność emaila i wymagania dotyczące hasła.
3. System tworzy nieaktywną kartę użytkownika.
4. Użytkownik otrzymuje email z linkiem potwierdzającym.
5. Po przejściu aktywnego linku konto zostaje aktywowane.

Rezultat: użytkownik może zalogować się do systemu.

### UC-02. Logowanie

1. Użytkownik podaje email i hasło.
2. System sprawdza dane logowania i stan konta.
3. Przy powodzeniu system wydaje access token i refresh token.

Rezultat: użytkownik uzyskuje dostęp zgodnie ze swoją rolą.

### UC-03. Przegląd grafiku

1. Użytkownik otwiera grafik.
2. System pokazuje przyszłe opublikowane zajęcia.
3. Użytkownik filtruje dane według daty, instruktora, stylu i poziomu.

Rezultat: dla każdych zajęć widoczne są godzina, czas trwania i wolne miejsca.

### UC-04. Rezerwacja z użyciem karnetu

1. Klient wybiera zajęcia.
2. System sprawdza dostępność zajęć i odpowiedni aktywny karnet.
3. Jeśli jest miejsce, tworzona jest potwierdzona rezerwacja.
4. Dla karnetu limitowanego rezerwowane jest jedno wejście.
5. Klient otrzymuje email.

Rezultat: miejsce zostaje przypisane do klienta.

### UC-05. Lista oczekujących

1. Klient wybiera pełne zajęcia.
2. System dodaje klienta na koniec kolejki.
3. Po anulowaniu potwierdzonej rezerwacji pierwszy uczestnik kolejki
   automatycznie otrzymuje miejsce.
4. System wysyła mu powiadomienie.

Rezultat: kolejka jest obsługiwana w porządku FIFO.

### UC-06. Anulowanie rezerwacji

1. Klient anuluje rezerwację przed wyznaczonym terminem granicznym.
2. System zwalnia miejsce i zwraca zarezerwowane wejście.
3. Jeśli lista oczekujących nie jest pusta, system przesuwa pierwszą osobę.

Rezultat: rezerwacja zostaje anulowana, a dane karnetu pozostają spójne.

### UC-07. Zakup karnetu

1. Klient wybiera dostępny typ karnetu.
2. System tworzy zamówienie i płatność ze statusem PENDING.
3. Na czas płatności zamówienie jest rezerwowane na 5 minut.
4. Klient opłaca zamówienie przez PayU/BLIK.
5. Backend weryfikuje callback systemu płatności.
6. Po udanej płatności tworzony jest karnet użytkownika.

Rezultat: płatność ma status PAID, a karnet jest aktywny.

### UC-08. Ewidencja obecności

1. Instruktor otwiera zakończone lub trwające zajęcia.
2. Instruktor oznacza każdego uczestnika jako PRESENT albo ABSENT.
3. System ostatecznie rozlicza zarezerwowane wejście tylko dla PRESENT.
4. Dla ABSENT działa polityka późnej anulacji albo nieobecności.

Rezultat: obecność jest zapisana i dostępna w statystykach.

### UC-09. Opinia

1. Klient otwiera zakończone zajęcia z oznaczeniem PRESENT.
2. Klient wystawia ocenę od 1 do 5 i opcjonalnie dodaje komentarz.
3. System dopuszcza maksymalnie jedną opinię klienta na zajęcia.

Rezultat: opinia zostaje zapisana.

## 6. Zasady biznesowe

### Konta użytkowników

- Email jest unikalny bez rozróżniania wielkości liter.
- Hasło zawiera co najmniej 8 znaków, jedną wielką literę i jedną cyfrę.
- Niepotwierdzone konto nie może się zalogować.
- Po 5 nieudanych próbach logowanie jest blokowane na 15 minut.
- Linki potwierdzenia email i resetu hasła są jednorazowe i mają ograniczony
  czas ważności.

### Zajęcia i rezerwacje

- Nie można utworzyć dwóch aktywnych rezerwacji jednego klienta na te same zajęcia.
- Nie można jednocześnie mieć aktywnej rezerwacji i wpisu na liście oczekujących.
- Nie można rezerwować zajęć rozpoczętych ani anulowanych.
- Liczba potwierdzonych rezerwacji nie może przekroczyć pojemności.
- Operacje zajęcia miejsca i przesuwania kolejki są transakcyjne.
- Klient może anulować rezerwację bez utraty wejścia najpóźniej 12 godzin
  przed rozpoczęciem.
- Przy późniejszej anulacji albo nieobecności wejście nie jest zwracane.
- Administrator może anulować zajęcia; wtedy wejścia wracają do wszystkich klientów.

### Karnety

- Karnet limitowany określa liczbę wejść i okres ważności.
- Karnet bez limitu określa wyłącznie okres ważności.
- Karnet zaczyna obowiązywać w chwili udanej płatności.
- Aby zarezerwować zajęcia, karnet musi być ważny w dniu zajęć.
- Jedne zajęcia nie mogą rozliczyć więcej niż jednego wejścia.
- Wygasłych karnetów nie można używać do nowych rezerwacji.

### Płatności

- BLIK jest metodą płatności udostępnianą przez PayU.
- Status płatności zmienia się dopiero po weryfikacji podpisanego powiadomienia PayU.
- Powtórzony callback nie może ponownie aktywować karnetu.
- Kwota i waluta callbacku muszą odpowiadać zamówieniu.
- Waluta pierwszej wersji: PLN.

### Opinie

- Opinia jest dostępna dopiero po zakończeniu zajęć.
- Autor musi mieć status PRESENT.
- Jeden klient może zostawić jedną opinię do jednych zajęć.
- Administrator może ukryć opinię, ale nie może zmienić jej treści.

## 7. Powiadomienia

Email jest wysyłany:

- po rejestracji;
- w celu potwierdzenia emaila;
- w celu odzyskania hasła;
- po potwierdzeniu albo anulowaniu rezerwacji;
- po dodaniu do listy oczekujących;
- po automatycznym przydzieleniu miejsca;
- 24 godziny przed zajęciami;
- po udanej albo nieudanej płatności;
- po anulowaniu zajęć przez administratora;
- po zakończeniu odbytych zajęć z propozycją wystawienia opinii.

Błąd wysyłki emaila nie powinien cofać poprawnie wykonanej operacji biznesowej.
Niewysłane wiadomości muszą być zachowane do ponownej wysyłki.

## 8. Raporty MVP

Administrator ma dostęp do:

- przychodu za wybrany okres;
- liczby udanych i nieudanych płatności;
- sprzedaży według typów karnetów;
- obłożenia zajęć;
- frekwencji i nieobecności;
- popularności stylów;
- aktywnych klientów;
- statystyk instruktorów.

Eksport raportów w pierwszej wersji: CSV.

## 9. Wymagania niefunkcjonalne

### Wydajność

- 95% zwykłych zapytań API wykonuje się w czasie do 2 sekund.
- System jest projektowany na 500 równoczesnych sesji użytkowników.
- Grafik używa paginacji i indeksów bazy danych.

### Bezpieczeństwo

- Hasła są haszowane przez BCrypt.
- Access token ma krótki czas życia; refresh token może zostać odwołany.
- Uprawnienia są sprawdzane po stronie backendu.
- Wejściowe DTO przechodzą walidację serwerową.
- Dla uploadów sprawdzane są typ MIME, rozszerzenie i rozmiar pliku.
- Sekrety nie są przechowywane w Git.
- Production działa wyłącznie przez HTTPS.
- Zdarzenia logowania, zmian ról i płatności są logowane audytowo.

### Niezawodność

- Wszystkie zmiany płatności i rezerwacji są idempotentne.
- Błędy są logowane bez haseł, tokenów i sekretów płatności.
- Kopia zapasowa bazy danych wykonywana jest codziennie.
- Procedura odtwarzania jest testowana osobno przed wydaniem production.

### Zgodność

- Interfejs jest responsywny na telefonie, tablecie i komputerze.
- Obsługiwane są aktualne wersje Chrome, Edge, Firefox i Safari.
- Głównym językiem pierwszej wersji jest polski.
- Strefa czasowa szkoły jest konfigurowalna; wartość początkowa: Europe/Warsaw.

## 10. Kryteria gotowości MVP

MVP uznaje się za gotowe, jeśli:

- wszystkie obowiązkowe scenariusze są realizowane przez interfejs;
- prawa trzech ról są przetestowane;
- współbieżne rezerwacje nie prowadzą do przepełnienia zajęć;
- sandbox PayU poprawnie obsługuje płatność, błąd i powtórzony callback;
- emaile są generowane i wysyłane przez skonfigurowany SMTP;
- główne API są pokryte testami integracyjnymi;
- frontend przechodzi kluczowe scenariusze end-to-end;
- projekt uruchamia się jednym poleceniem Docker Compose;
- Swagger opisuje dostępne API;
- przygotowano instrukcje uruchomienia, konfiguracji i backupu.

## 11. Przyjęte założenia

Do czasu osobnego uzgodnienia obowiązują następujące decyzje:

- jedna szkoła i jedna strefa czasowa;
- jedna waluta PLN;
- BLIK jest integrowany przez PayU;
- zdjęcie profilowe jest przechowywane w magazynie zgodnym z S3, a lokalnie
  może być używane MinIO;
- bezpłatna anulacja wynosi 12 godzin;
- podczas zakupu rezerwowane jest nie miejsce na zajęciach, lecz zamówienie na karnet;
- wejście jest rezerwowane przy zapisie i ostatecznie rozliczane po oznaczeniu
  PRESENT albo przy późnej anulacji/nieobecności;
- wydarzenia są osobnymi jednorazowymi zdarzeniami i nie korzystają ze
  standardowego karnetu, dopóki nie zostanie uzgodnione inaczej.
