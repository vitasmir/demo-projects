<?php

declare(strict_types=1);

require_once __DIR__ . '/layout.php';
requireAdmin();

$config = appConfig();
$db = db();

// Load current rental hours from DB
$date = date('Y-m-d');
$hours = getEffectiveRentalHours($db, $date);
$start = $hours['start_time'];
$end = $hours['end_time'];

$settingsSaved = false;
if (isset($_POST['save_settings'])) {
    $start = $_POST['start'] ?? $start;
    $end = $_POST['end'] ?? $end;
    $adminId = currentUser()['id'];
    applyRentalHoursChange($db, $date, $start, $end, $adminId);
    $settingsSaved = true;
    // Reload hours after change
    $hours = getEffectiveRentalHours($db, $date);
    $start = $hours['start_time'];
    $end = $hours['end_time'];
}

// Boat management (dummy, as before)
$boats = $config['boats'];
$newBoat = $_POST['new_boat'] ?? '';
if ($newBoat) {
    $boats[] = ['name' => $newBoat];
}

renderPageStart('Administrace loděk', 'admin');
?>
<style>
body {
    background: #fafbfc;
}
.admin-flex {
    display: flex;
    gap: 2em;
    margin-top: 2em;
    justify-content: center;
}
.admin-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 8px #0001;
    padding: 2em 2em 1.5em 2em;
    min-width: 340px;
    flex: 1 1 0;
    max-width: 420px;
    border: 1px solid #e5e7eb;
}
.admin-card h3 {
    margin-top: 0;
    margin-bottom: 1.2em;
    font-size: 1.35em;
    font-weight: 600;
}
.admin-card label {
    font-weight: 500;
    color: #222;
    font-size: 1em;
}
.admin-card select, .admin-card input[type="text"] {
    border: 1px solid #cfd8dc;
    border-radius: 6px;
    padding: 0.45em 0.7em;
    font-size: 1.08em;
    margin-top: 0.3em;
    background: #f8fafc;
    width: 120px;
    box-sizing: border-box;
}
.admin-card input[type="text"] {
    width: 160px;
}
.primary-button {
    background: #2563eb;
    color: #fff;
    border: none;
    border-radius: 6px;
    padding: 0.55em 1.3em;
    font-size: 1.08em;
    font-weight: 500;
    cursor: pointer;
    margin-top: 0.7em;
    transition: background 0.15s;
}
.primary-button:hover {
    background: #1746a2;
}
.pill {
    display: inline-block;
    background: #e6f0ff;
    color: #2356a8;
    border-radius: 999px;
    padding: 0.3em 1.2em;
    margin: 0.2em 0.3em 0.2em 0;
    font-weight: 600;
    font-size: 1em;
    border: none;
}
.admin-card form {
    margin-bottom: 0;
}
.admin-card .form-row {
    display: flex;
    gap: 1.2em;
    align-items: flex-end;
}
@media (max-width: 900px) {
    .admin-flex {
        flex-direction: column;
        gap: 1.5em;
        align-items: center;
    }
    .admin-card {
        max-width: 98vw;
        min-width: 0;
    }
}
</style>
<div class="admin-flex">
    <div class="admin-card">
        <h3>Nastavení půjčování</h3>
        <?php if ($settingsSaved): ?>
            <div style="color: green; margin-bottom: 1em;">Nastavení bylo uloženo a platí od dneška.</div>
        <?php endif; ?>
        <form method="post" autocomplete="off">
            <div class="form-row">
                <div>
                    <label>Od<br>
                        <select name="start">
                            <?php for ($h = 6; $h <= 20; $h++): for ($m = 0; $m < 60; $m += 30): $val = sprintf('%02d:%02d', $h, $m); ?>
                                <option value="<?= $val ?>" <?= $val === $start ? 'selected' : '' ?>><?= $val ?></option>
                            <?php endfor; endfor; ?>
                        </select>
                    </label>
                </div>
                <div>
                    <label>Do<br>
                        <select name="end">
                            <?php for ($h = 7; $h <= 22; $h++): for ($m = 0; $m < 60; $m += 30): $val = sprintf('%02d:%02d', $h, $m); ?>
                                <option value="<?= $val ?>" <?= $val === $end ? 'selected' : '' ?>><?= $val ?></option>
                            <?php endfor; endfor; ?>
                        </select>
                    </label>
                </div>
            </div>
            <div style="font-size: 0.97em; color: #555; margin: 1em 0 0.5em 0;">
                Délka jedné výpůjčky je pevně nastavena na 30 minut.
            </div>
            <button type="submit" name="save_settings" class="primary-button">Uložit nastavení</button>
        </form>
    </div>
    <div class="admin-card">
        <h3>Správa loděk</h3>
        <form method="post" style="display: flex; gap: 0.5em; align-items: flex-end;">
            <label>Nová loďka
                <input type="text" name="new_boat" value="" placeholder="Lodka 3">
            </label>
            <button type="submit" class="primary-button">Přidat loďku</button>
        </form>
        <div style="margin-top: 0.7em;">
            <?php foreach ($boats as $b): ?>
                <button type="button" class="pill" tabindex="-1"><?= htmlspecialchars($b['name']); ?></button>
            <?php endforeach; ?>
        </div>
    </div>
</div>
<?php renderPageEnd(); ?>
