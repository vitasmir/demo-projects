<?php

declare(strict_types=1);

require_once __DIR__ . '/layout.php';
requireLogin();

$user = currentUser();
$db = db();

$success = false;
$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? $user['username']);
    $email = trim($_POST['email'] ?? $user['email']);
    $password = $_POST['password'] ?? '';
    $password2 = $_POST['password2'] ?? '';

    if ($username === '' || $email === '') {
        $error = 'Uživatelské jméno a e-mail nesmí být prázdné.';
    } elseif ($password !== '' && $password !== $password2) {
        $error = 'Hesla se neshodují.';
    } else {
        // Check if username is taken by someone else
        $stmt = $db->prepare('SELECT id FROM users WHERE username = :username AND id != :id');
        $stmt->execute(['username' => $username, 'id' => $user['id']]);
        if ($stmt->fetch()) {
            $error = 'Toto uživatelské jméno je již obsazeno.';
        } else {
            $params = [
                'username' => $username,
                'email' => $email,
                'id' => $user['id'],
            ];
            $sql = 'UPDATE users SET username = :username, email = :email';
            if ($password !== '') {
                $sql .= ', password_hash = :password_hash';
                $params['password_hash'] = password_hash($password, PASSWORD_DEFAULT);
            }
            $sql .= ' WHERE id = :id';
            $stmt = $db->prepare($sql);
            $stmt->execute($params);
            // Update session
            $_SESSION['user']['username'] = $username;
            $_SESSION['user']['email'] = $email;
            $success = true;
        }
    }
}

renderPageStart('Profil uživatele');
?>
<section class="card profile-card" style="max-width: 420px; margin: 32px auto 0 auto;">
    <h2 style="margin-bottom: 18px;">Profil uživatele</h2>
    <?php if ($success): ?>
        <div class="flash flash-success">Změny byly uloženy.</div>
    <?php elseif ($error): ?>
        <div class="flash flash-error"><?= h($error) ?></div>
    <?php endif; ?>
    <form method="post" autocomplete="off" class="profile-form">
        <div class="form-group">
            <label for="username">Uživatelské jméno</label>
            <input type="text" id="username" name="username" value="<?= h($user['username']) ?>" required class="input-text">
        </div>
        <div class="form-group">
            <label for="email">E-mail</label>
            <input type="email" id="email" name="email" value="<?= h($user['email']) ?>" required class="input-text">
        </div>
        <div class="form-group">
            <label for="password">Nové heslo</label>
            <input type="password" id="password" name="password" autocomplete="new-password" class="input-text">
        </div>
        <div class="form-group">
            <label for="password2">Potvrzení nového hesla</label>
            <input type="password" id="password2" name="password2" autocomplete="new-password" class="input-text">
        </div>
        <button type="submit" class="primary-button" style="width: 100%; margin-top: 10px;">Uložit změny</button>
    </form>
    <a href="index.php" class="ghost-button profile-back" style="margin-top: 18px; display: flex; align-items: center; justify-content: center; height: 44px; width: 100%;">Zpět na rezervace</a>
</section>
<style>
.profile-form .form-group {
    margin-bottom: 16px;
}
.profile-form label {
    display: block;
    font-weight: 600;
    margin-bottom: 6px;
    color: #1c2740;
}
.profile-form .input-text {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #d9e1ea;
    border-radius: 6px;
    font-size: 16px;
    background: #f5f7fb;
    color: #1c2740;
    transition: border 0.2s;
}
.profile-form .input-text:focus {
    border-color: #156ff7;
    outline: none;
    background: #fff;
}
.profile-back {
    width: 100%;
    max-width: none;
    margin-left: 0;
    margin-right: 0;
}
</style>
<?php renderPageEnd(); ?>
