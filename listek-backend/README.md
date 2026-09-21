# Listek Backend

Spring Boot 4.1 backend pro internetove bankovnictvi Listek. Aplikace obsahuje REST API, business logiku prevodu mezi ucty, JPA persistenci a Flyway migrace databaze.

## Technologie

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA, Bean Validation
- PostgreSQL 16
- Flyway 12
- Maven

## Spusteni databaze a aplikace

```bash
sh docker-compose-vault.sh up --build
```

Launcher načte secret `secrets/data/listek` z lokálního Vaultu a předá databázové proměnné pouze procesu Compose. Token se neukládá do Compose konfigurace. Služba `vault-seed` načte uživatelsky spravované připojení z cest `secrets/listek` a `secrets/listek-admin` pomocí Vault CLI a připraví odpovídající PostgreSQL role a databáze:

```text
DB_URL=jdbc:postgresql://postgres:5432/listek
DB_USERNAME=<uživatel databáze>
DB_PASSWORD=<heslo databáze>
```

`DB_URL` musí být JDBC URL včetně prefixu `jdbc:`. Hodnota jako
`postgresql://postgres:5432/listek` nebo `postgres://...` není platná pro
Spring Boot datasource.

Launcher je předá jako `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` a `SPRING_DATASOURCE_PASSWORD` službám backendu a adminu. Heslo není uloženo v `docker-compose.yml` ani v souboru v repozitáři.

PostgreSQL používá v tomto čistě lokálním vývojovém stacku autentizaci `trust`, aby nevznikl bootstrap cyklus PostgreSQL → Vault → PostgreSQL. Toto nastavení není vhodné pro produkci.

Flyway se spousti automaticky pri startu aplikace. Schema je v `src/main/resources/db/migration` a nove zmeny se pridavaji jako dalsi migrace, napr. `V2__add_card_table.sql`. Hibernate ma nastaveno `ddl-auto: validate`, takze strukturu databaze meni pouze Flyway.

## REST API

`GET /api/v1/accounts` vrati vsechny ucty.

`POST /api/v1/accounts` vytvori ucet:

```json
{
  "ownerName": "Jan Kral",
  "accountNumber": "123456789",
  "initialBalance": 126840.35,
  "currency": "CZK"
}
```

`GET /api/v1/accounts/{accountId}/transactions` vrati poslednich 20 pohybu uctu.

`POST /api/v1/transfers` provede prevod mezi ucty:

```json
{
  "fromAccountId": "00000000-0000-0000-0000-000000000001",
  "toAccountId": "00000000-0000-0000-0000-000000000002",
  "amount": 1250.00,
  "description": "Najem za studio"
}
```

Prevod kontroluje existenci uctu, shodnou menu, kladnou castku, dostatecny zustatek a zapisuje oba pohyby v jedne transakci.

## Kontrola

```bash
mvn test
```

Testy business logiky nevyzaduji bezici databazi. Pro automaticke overeni migraci a API proti realnemu PostgreSQL je v POMu pripravena zavislost Testcontainers.
