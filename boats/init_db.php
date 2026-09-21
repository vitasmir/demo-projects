<?php

declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

echo "SQLite databáze byla inicializována v souboru: " . appConfig()['db_path'] . PHP_EOL;
echo "Admin účet: admin / admin" . PHP_EOL;
