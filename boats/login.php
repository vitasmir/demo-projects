<?php

declare(strict_types=1);

require_once __DIR__ . '/layout.php';

if (isLoggedIn()) {
    redirect('index.php');
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = (string) ($_POST['password'] ?? '');

    if ($username === '' || $password === '') {
        flash('error', 'Vyplňte uživatelské jméno i heslo.');
    } else {
        $user = findUserByUsername(db(), $username);
        if (!$user || !password_verify($password, $user['password_hash'])) {
            flash('error', 'Neplatné uživatelské jméno nebo heslo.');
        } else {
            loginUser($user);
            flash('success', 'Přihlášení proběhlo úspěšně.');
            redirect('index.php');
        }
    }
}

renderPageStart('Přihlášení', 'login');
?>
<div class="card auth-wrap">
    <h1 class="page-title">Přihlášení</h1>
    <p class="subtitle">Přihlaste se svým uživatelským jménem a heslem.</p>
    <form method="post" class="form-grid" autocomplete="off">
        <div class="form-field">
            <label for="username">Uživatelské jméno</label>
            <input id="username" name="username" type="text" placeholder="napr. uzivatel" required>
        </div>
        <div class="form-field">
            <label for="password">Heslo</label>
            <input id="password" name="password" type="password" required>
        </div>
        <div class="form-actions">
            <button class="nav-link" type="submit">Přihlásit se</button>
        </div>
    </form>
    <div class="link-row">Nemáte účet? <a href="register.php">Zaregistrujte se</a>.</div>
    <div class="auth-note">Admin přihlášení: <strong>admin / admin</strong></div>
</div>
<?php renderPageEnd(); ?>
