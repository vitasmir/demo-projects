<?php

declare(strict_types=1);

session_start();

$config = require __DIR__ . '/config.php';

$pdo = new PDO('sqlite:' . $config['db_path']);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
$pdo->exec('PRAGMA foreign_keys = ON');

initializeDatabase($pdo, $config);

function initializeDatabase(PDO $pdo, array $config): void
{
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL UNIQUE,
            email TEXT,
            password_hash TEXT NOT NULL,
            role TEXT NOT NULL DEFAULT "user",
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )'
    );

    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS rental_hours (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            valid_from TEXT NOT NULL,
            start_time TEXT NOT NULL,
            end_time TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by INTEGER,
            FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
        )'
    );

    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS reservations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            reservation_date TEXT NOT NULL,
            boat_code TEXT NOT NULL,
            slot_time TEXT NOT NULL,
            user_id INTEGER NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(reservation_date, boat_code, slot_time),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )'
    );

    $admin = $pdo->prepare('SELECT id FROM users WHERE username = :username LIMIT 1');
    $admin->execute(['username' => $config['admin']['username']]);
    if (!$admin->fetch()) {
        $insertAdmin = $pdo->prepare(
            'INSERT INTO users (username, email, password_hash, role) VALUES (:username, :email, :password_hash, :role)'
        );
        $insertAdmin->execute([
            'username' => $config['admin']['username'],
            'email' => $config['admin']['email'],
            'password_hash' => password_hash($config['admin']['password'], PASSWORD_DEFAULT),
            'role' => 'admin',
        ]);
    }

    $hoursCount = (int) $pdo->query('SELECT COUNT(*) FROM rental_hours')->fetchColumn();
    if ($hoursCount === 0) {
        $adminId = (int) $pdo->query("SELECT id FROM users WHERE username = 'admin' LIMIT 1")->fetchColumn();
        $insertHours = $pdo->prepare(
            'INSERT INTO rental_hours (valid_from, start_time, end_time, created_by) VALUES (:valid_from, :start_time, :end_time, :created_by)'
        );
        $insertHours->execute([
            'valid_from' => $config['default_hours']['valid_from'],
            'start_time' => $config['default_hours']['start_time'],
            'end_time' => $config['default_hours']['end_time'],
            'created_by' => $adminId ?: null,
        ]);
    }
}

function appConfig(): array
{
    global $config;

    return $config;
}

function db(): PDO
{
    global $pdo;

    return $pdo;
}

function currentUser(): ?array
{
    if (!isset($_SESSION['user'])) {
        return null;
    }

    return $_SESSION['user'];
}

function isLoggedIn(): bool
{
    return currentUser() !== null;
}

function isAdmin(): bool
{
    return isLoggedIn() && (currentUser()['role'] ?? '') === 'admin';
}

function requireLogin(): void
{
    if (!isLoggedIn()) {
        redirect('login.php');
    }
}

function requireAdmin(): void
{
    requireLogin();
    if (!isAdmin()) {
        flash('error', 'Přístup mají pouze administrátoři.');
        redirect('index.php');
    }
}

function redirect(string $path): void
{
    header('Location: ' . $path);
    exit;
}

function flash(string $type, string $message): void
{
    $_SESSION['flash'][$type][] = $message;
}

function pullFlashMessages(): array
{
    $messages = $_SESSION['flash'] ?? [];
    unset($_SESSION['flash']);

    return $messages;
}

function h(?string $value): string
{
    return htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8');
}

function findUserByUsername(PDO $pdo, string $username): ?array
{
    $stmt = $pdo->prepare('SELECT * FROM users WHERE username = :username LIMIT 1');
    $stmt->execute(['username' => $username]);
    $user = $stmt->fetch();

    return $user ?: null;
}

function loginUser(array $user): void
{
    $_SESSION['user'] = [
        'id' => (int) $user['id'],
        'username' => $user['username'],
        'email' => $user['email'],
        'role' => $user['role'],
    ];
}

function logoutUser(): void
{
    unset($_SESSION['user']);
}

function selectedDate(): string
{
    $date = $_GET['date'] ?? date('Y-m-d');
    $parsed = DateTimeImmutable::createFromFormat('Y-m-d', $date);

    return $parsed ? $parsed->format('Y-m-d') : date('Y-m-d');
}

function getEffectiveRentalHours(PDO $pdo, string $date): array
{
    $stmt = $pdo->prepare(
        'SELECT * FROM rental_hours WHERE valid_from <= :date ORDER BY valid_from DESC, id DESC LIMIT 1'
    );
    $stmt->execute(['date' => $date]);
    $row = $stmt->fetch();

    if ($row) {
        return $row;
    }

    $config = appConfig();
    return [
        'valid_from' => $config['default_hours']['valid_from'],
        'start_time' => $config['default_hours']['start_time'],
        'end_time' => $config['default_hours']['end_time'],
    ];
}

function buildSlots(string $startTime, string $endTime, int $minutes): array
{
    $slots = [];
    $start = DateTimeImmutable::createFromFormat('H:i', $startTime);
    $end = DateTimeImmutable::createFromFormat('H:i', $endTime);

    if (!$start || !$end || $start > $end) {
        return $slots;
    }

    // The last slot should start at end - slot duration
    $lastSlotStart = $end->modify('-' . $minutes . ' minutes');
    for ($cursor = $start; $cursor <= $lastSlotStart; $cursor = $cursor->modify('+' . $minutes . ' minutes')) {
        $slots[] = $cursor->format('H:i');
    }

    return $slots;
}

function fetchReservationsByDate(PDO $pdo, string $date): array
{
    $stmt = $pdo->prepare(
        'SELECT r.*, u.username FROM reservations r
         INNER JOIN users u ON u.id = r.user_id
         WHERE reservation_date = :reservation_date'
    );
    $stmt->execute(['reservation_date' => $date]);

    $data = [];
    foreach ($stmt->fetchAll() as $row) {
        $data[$row['boat_code']][$row['slot_time']] = $row;
    }

    return $data;
}

function reservationOwnerName(?array $reservation): ?string
{
    return $reservation['username'] ?? null;
}

function canCancelReservation(array $reservation, array $user): bool
{
    return (int) $reservation['user_id'] === (int) $user['id'] || ($user['role'] ?? '') === 'admin';
}

function applyRentalHoursChange(PDO $pdo, string $validFrom, string $startTime, string $endTime, int $adminId): void
{
    $stmt = $pdo->prepare(
        'INSERT INTO rental_hours (valid_from, start_time, end_time, created_by)
         VALUES (:valid_from, :start_time, :end_time, :created_by)'
    );
    $stmt->execute([
        'valid_from' => $validFrom,
        'start_time' => $startTime,
        'end_time' => $endTime,
        'created_by' => $adminId,
    ]);

    pruneReservationsOutsideHours($pdo, $validFrom);
}

function pruneReservationsOutsideHours(PDO $pdo, string $fromDate): void
{
    $stmt = $pdo->prepare(
        'SELECT id, reservation_date, slot_time FROM reservations WHERE reservation_date >= :from_date ORDER BY reservation_date ASC'
    );
    $stmt->execute(['from_date' => $fromDate]);

    $deleteStmt = $pdo->prepare('DELETE FROM reservations WHERE id = :id');

    foreach ($stmt->fetchAll() as $reservation) {
        $hours = getEffectiveRentalHours($pdo, $reservation['reservation_date']);
        if ($reservation['slot_time'] < $hours['start_time'] || $reservation['slot_time'] > $hours['end_time']) {
            $deleteStmt->execute(['id' => $reservation['id']]);
        }
    }
}

function pageTitle(string $title): string
{
    return $title . ' | ' . appConfig()['app_name'];
}

function previousDate(string $date): string
{
    return (new DateTimeImmutable($date))->modify('-1 day')->format('Y-m-d');
}

function nextDate(string $date): string
{
    return (new DateTimeImmutable($date))->modify('+1 day')->format('Y-m-d');
}
