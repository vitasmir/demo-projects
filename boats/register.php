<?php

declare(strict_types=1);

require_once __DIR__ . '/layout.php';

if (isLoggedIn()) {
    redirect('index.php');
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $email = trim($_POST['email'] ?? '');
    $password = (string) ($_POST['password'] ?? '');
    $passwordConfirmation = (string) ($_POST['password_confirmation'] ?? '');

    if ($username === '' || $password === '' || $passwordConfirmation === '') {
        flash('error', 'Vyplňte povinná pole.');
    } elseif (!preg_match('/^[a-zA-Z0-9._-]{3,40}$/', $username)) {
        flash('error', 'Uživatelské jméno musí mít 3 až 40 znaků a může obsahovat pouze písmena, čísla, tečku, pomlčku a podtržítko.');
    } elseif ($email !== '' && !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        flash('error', 'Email nemá platný formát.');
    } elseif ($password !== $passwordConfirmation) {
        flash('error', 'Hesla se neshodují.');
    } elseif (strlen($password) < 4) {
        flash('error', 'Heslo musí mít alespoň 4 znaky.');
    } elseif (findUserByUsername(db(), $username)) {
        flash('error', 'Toto uživatelské jméno již existuje.');
    } else {
        $stmt = db()->prepare(
            'INSERT INTO users (username, email, password_hash, role) VALUES (:username, :email, :password_hash, :role)'
        );
        $stmt->execute([
            'username' => $username,
            'email' => $email !== '' ? $email : null,
            'password_hash' => password_hash($password, PASSWORD_DEFAULT),
            'role' => 'user',
        ]);

        $user = findUserByUsername(db(), $username);
        if ($user) {
            loginUser($user);
        }

        flash('success', 'Registrace proběhla úspěšně.');
        redirect('index.php');
    }
}

renderPageStart('Registrace', 'register');
?>
<div class="card auth-wrap">
    <h1 class="page-title">Registrace</h1>
    <p class="subtitle">Vytvořte si účet pro rezervaci loděk.</p>
    <form method="post" class="form-grid" autocomplete="off">
        <div class="form-field">
            <label for="username">Uživatelské jméno</label>
            <input id="username" name="username" type="text" required>
        </div>
        <div class="form-field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" placeholder="napr. uzivatel@example.com">
        </div>
        <div class="form-field">
            <label for="password">Heslo</label>
            <input id="password" name="password" type="password" required>
        </div>
        <div class="form-field">
            <label for="password_confirmation">Potvrzení hesla</label>
            <input id="password_confirmation" name="password_confirmation" type="password" required>
        </div>
        <div class="form-actions">
            <button class="nav-link" type="submit">Zaregistrovat</button>
        </div>
    </form>
    <div class="link-row">Máte účet? <a href="login.php">Přihlaste se</a>.</div>
</div>
<?php renderPageEnd(); ?>
