# Password Vault

Jednoduchá Java Swing aplikace pro bezpečné ukládání uživatelských jmen a hesel pro jednotlivé webové stránky.

## Bezpečnost

- hlavní heslo se převádí na 256bitový klíč pomocí `PBKDF2WithHmacSHA256`
- pro každý trezor se generuje náhodná sůl
- celý obsah trezoru je šifrován jako jeden celek pomocí `AES/GCM/NoPadding`
- kontrola hlavního hesla používá pouze ověřovací otisk, neukládá se v otevřené podobě

## Funkce

- vytvoření nového trezoru chráněného hlavním heslem
- přehledné desktopové GUI ve Swingu
- přidání nebo aktualizace přístupu pro web
- zobrazení uloženého přístupu v editoru formuláře
- kopírování hesla do schránky bez jeho zobrazení
- výpis uložených webů v tabulce
- smazání záznamu

## Spuštění

Z kořene projektu spusťte:

```bash
mvn compile exec:java
```

Trezor se ukládá do souboru `~/.password-vault/vault.dat`.

##
spousti se to java -jar target/password-vault-1.0.0.jar

