# Lístek Manager

Interní administrace banky Lístek postavená na Next.js, TypeScriptu a Tailwind CSS.

## Funkce

- operační dashboard nad živými bankovními daty
- schválení a zamítnutí půjček s poznámkou k rozhodnutí
- založení, schválení a zamítnutí kontokorentu
- přehled klientů, účtů a zůstatků
- responzivní rozhraní pro desktop i mobil

## Spuštění

Nejprve spusťte PostgreSQL a projekt `listek-admin`. Potom:

```bash
npm install
npm run dev -- --port 3001
```

Manager je dostupný na `http://localhost:3001`. URL backendu lze změnit proměnnou `ADMIN_BACKEND_URL`.
