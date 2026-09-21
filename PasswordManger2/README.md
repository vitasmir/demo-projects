# Password Manager 2

Java Swing aplikace pro bezpečné ukládání přístupů po skupinách.

## Bezpečnost

- hlavní heslo se převádí na 256bitový klíč pomocí `PBKDF2WithHmacSHA512`
- pro každý trezor se generuje náhodná sůl
- celý obsah trezoru je šifrován jako jeden celek pomocí `AES/GCM/NoPadding`
- kontrola hlavního hesla používá pouze ověřovací otisk, neukládá se v otevřené podobě

## Funkce

- vytvoření nového trezoru chráněného hlavním heslem
- vlevo strom skupin
- vpravo tabulka se sloupci Server name, username a password
- maskované heslo pomocí hvězdiček
- editace jednotlivých záznamů
- kopírování server name, username i password do schránky
- uložení změn i mazání záznamů

## Spuštění

Z kořene projektu spusťte:

```bash
mvn compile exec:java
```

Trezor se ukládá do souboru `~/.password-manager2/vault.dat`.
