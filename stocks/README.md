# PHP aplikace Akcie

PHP přepis modulu `myapp-client/src/app/stocks`.

## Co aplikace dělá

- načítá seznam akcií z lokálního souboru `catalog.json`
- načítá historická data přímo z Yahoo Finance
- vykresluje SVG graf, statistiky a tooltip nad body

## Bez backendu

Tato verze nepoužívá `myapp-jpa-server`.

Seznam akcií je uložen lokálně v `catalog.json` a cenová historie se stahuje server-side přímo z Yahoo Finance, takže není potřeba Spring Boot backend ani proxy.

## Spuštění

Například přes vestavěný PHP server:

```bash
php -S localhost:8081
```

Pak otevři:

- `http://localhost:8081/index.php`
