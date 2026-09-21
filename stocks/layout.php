<?php

declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

function renderPageStart(string $title): void
{
    ?>
<!DOCTYPE html>
<html lang="cs">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= h(pageTitle($title)); ?></title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
<div class="app-shell">
    <header class="topbar">
        <div>
            <div class="eyebrow">PHP přepis Angular modulu</div>
            <h1>Akcie</h1>
            <p>Seznam akcií je lokální a historický graf se načítá přímo z Yahoo Finance.</p>
        </div>
        <div class="topbar-meta">
            <span class="pill">Bez Java backendu</span>
            <span class="pill">Zdroj dat: Yahoo Finance</span>
        </div>
    </header>
    <main class="page-content">
<?php
}

function renderPageEnd(): void
{
    ?>
    </main>
</div>
</body>
</html>
<?php
}
