# DSMS: otwarte decyzje

Te kwestie nie blokują rozpoczęcia projektowania architektury. Do momentu
wdrożenia odpowiedniego modułu obowiązują założenia przyjęte w analizie wymagań.

| ID | Pytanie | Aktualne założenie |
|---|---|---|
| D-01 | Ile oddziałów ma obsługiwać system? | Jeden oddział |
| D-02 | W jakim języku ma działać interfejs? | Tylko polski |
| D-03 | Jakiej waluty używamy? | PLN  |
| D-04 | Jaki jest czas bezpłatnej anulacji rezerwacji? | 12 godzin |
| D-05 | Czy wejście przepada przy nieobecności? | Tak |
| D-06 | Czy klient może opłacić pojedyncze zajęcia bez karnetu? | Nie |
| D-07 | Czy administrator może ręcznie korygować liczbę pozostałych wejść? | Tak, z audytem |
| D-08 | Jakie ograniczenia ma karnet bez limitu? | Tylko okres ważności |
| D-09 | Czy opinie wymagają moderacji przed publikacją? | Nie, dostępne jest ukrywanie |
| D-10 | Czym są wydarzenia? | Jednorazowe płatne wydarzenia |
| D-11 | Czy zwroty pieniędzy mają być dostępne w interfejsie? | W MVP tylko obsługa ręczna |
| D-12 | Gdzie będzie hostowany system production? | Decyzja jeszcze nie została podjęta |
| D-13 | Jaki dostawca SMTP zostanie użyty? | Decyzja jeszcze nie została podjęta |
| D-14 | Czy potrzebny jest branding, czy wystarczy Material UI? | Material UI z podstawowym motywem |
| D-15 | Czy potrzebny jest import istniejących klientów? | Poza zakresem MVP |

## Decyzje potrzebne przed integracjami

Przed integracją PayU będą potrzebne:

- konto merchant i dane sandbox;
- lista dozwolonych metod płatności;
- zasady zwrotów;
- publiczne URL-e dla callbacków po wdrożeniu.

Przed wydaniem production będą potrzebne:

- nazwa domeny;
- hosting i sposób wdrożenia;
- dostawca SMTP;
- magazyn plików zgodny z S3;
- polityka przechowywania danych osobowych i logów;
- dane firmowe oraz treści regulaminów użytkownika.
