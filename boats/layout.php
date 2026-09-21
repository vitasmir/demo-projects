<?php

declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

function renderPageStart(string $title, string $active = ''): void
{
    $user = currentUser();
    $messages = pullFlashMessages();
    $pageClass = 'page-' . ($active !== '' ? preg_replace('/[^a-z0-9_-]+/i', '-', $active) : 'default');
    ?>
<!DOCTYPE html>
<html lang="cs">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= h(pageTitle($title)); ?></title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body class="<?= h($pageClass); ?>">
<div class="app-shell">
    <aside class="sidebar">
        <div class="brand">Půjčovna loděk</div>
        <?php if ($user): ?>
            <a href="profile.php" class="primary-button logout-button">
                Profil (<?= h($user['username']); ?>)
            </a>
            <a class="ghost-button logout-button" href="logout.php">Odhlásit se</a>
            <nav class="nav-stack">
                <a class="nav-link <?= $active === 'reservations' ? 'is-active' : ''; ?>" href="index.php">Rezervace loděk</a>
                <?php if (isAdmin()): ?>
                    <a class="nav-link <?= $active === 'admin' ? 'is-active' : ''; ?>" href="admin.php">Administrace loděk</a>
                <?php endif; ?>
            </nav>
        <?php else: ?>
            <nav class="nav-stack compact">
                <a class="nav-link <?= $active === 'login' ? 'is-active' : ''; ?>" href="login.php">Přihlášení</a>
                <a class="nav-link <?= $active === 'register' ? 'is-active' : ''; ?>" href="register.php">Registrace</a>
            </nav>
        <?php endif; ?>
    </aside>
    <main class="content <?= h($pageClass); ?>-content">
        <?php foreach ($messages as $type => $items): ?>
            <?php foreach ($items as $message): ?>
                <div class="flash flash-<?= h($type); ?>"><?= h($message); ?></div>
            <?php endforeach; ?>
        <?php endforeach; ?>
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
