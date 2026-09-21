# listek-admin

Spring Boot 4.1 backend pro interní administraci banky Lístek. Používá stejnou PostgreSQL databázi jako `listek-backend` a vlastní Flyway historii `flyway_schema_history_admin`.

## API

- `GET /api/admin/dashboard`
- `GET /api/admin/accounts`
- `GET /api/admin/loans`
- `PATCH /api/admin/loans/{id}/decision`
- `GET /api/admin/overdrafts`
- `POST /api/admin/overdrafts`
- `PATCH /api/admin/overdrafts/{id}/decision`

## Spuštění

```bash
mvn spring-boot:run
```

Backend běží na `http://localhost:8090`. Výchozí databáze je `jdbc:postgresql://localhost:5433/listek`; připojení lze změnit pomocí `DB_URL`, `DB_USERNAME` a `DB_PASSWORD`.
