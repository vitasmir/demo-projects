<?php

declare(strict_types=1);

setlocale(LC_TIME, 'cs_CZ.UTF-8');
// Robust Czech date/time formatting
function cz_date($date, $pattern = 'EEEE d. MMMM y') {
    if (class_exists('IntlDateFormatter')) {
        $fmt = new IntlDateFormatter('cs_CZ', IntlDateFormatter::FULL, IntlDateFormatter::NONE, 'Europe/Prague', IntlDateFormatter::GREGORIAN, $pattern);
        return $fmt->format(strtotime($date));
    }
    $dt = DateTimeImmutable::createFromFormat('Y-m-d', $date) ?: new DateTimeImmutable($date);
    return $dt->format('l j. F Y');
}
function cz_time($time, $pattern = 'H:mm') {
    if (class_exists('IntlDateFormatter')) {
        $fmt = new IntlDateFormatter('cs_CZ', IntlDateFormatter::NONE, IntlDateFormatter::SHORT, 'Europe/Prague', IntlDateFormatter::GREGORIAN, $pattern);
        return $fmt->format(strtotime($time));
    }
    $dt = DateTimeImmutable::createFromFormat('H:i', $time) ?: new DateTimeImmutable($time);
    return $dt->format('H:i');
}

require_once __DIR__ . '/layout.php';

requireLogin();

$config = appConfig();
$date = selectedDate();
$hours = getEffectiveRentalHours(db(), $date);
$slots = buildSlots($hours['start_time'], $hours['end_time'], (int) $config['slot_minutes']);
$boats = $config['boats'];
$user = currentUser();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $boatCode = (string) ($_POST['boat_code'] ?? '');
    $slotTime = (string) ($_POST['slot_time'] ?? '');
    $action = (string) ($_POST['action'] ?? '');
    $validBoatCodes = array_column($boats, 'code');

    if (!in_array($boatCode, $validBoatCodes, true) || !in_array($slotTime, $slots, true)) {
        flash('error', 'Neplatný výběr loďky nebo času.');
        if ($_GET['date'] !== $date) {
            redirect('index.php?date=' . urlencode($date));
        }
    }


    if ($action === 'reserve') {
        $stmt = db()->prepare(
            'SELECT id FROM reservations WHERE reservation_date = :reservation_date AND boat_code = :boat_code AND slot_time = :slot_time'
        );
        $stmt->execute([
            'reservation_date' => $date,
            'boat_code' => $boatCode,
            'slot_time' => $slotTime,
        ]);

        if ($stmt->fetch()) {
            flash('error', 'Tento slot je již obsazen.');
        } else {
            $insert = db()->prepare(
                'INSERT INTO reservations (reservation_date, boat_code, slot_time, user_id)
                 VALUES (:reservation_date, :boat_code, :slot_time, :user_id)'
            );
            $insert->execute([
                'reservation_date' => $date,
                'boat_code' => $boatCode,
                'slot_time' => $slotTime,
                'user_id' => $user['id'],
            ]);
            flash('success', 'Rezervace byla úspěšně vytvořena.');
        }
    }

    if ($action === 'cancel') {
        $stmt = db()->prepare(
            'SELECT * FROM reservations WHERE reservation_date = :reservation_date AND boat_code = :boat_code AND slot_time = :slot_time LIMIT 1'
        );
        $stmt->execute([
            'reservation_date' => $date,
            'boat_code' => $boatCode,
            'slot_time' => $slotTime,
        ]);
        $reservation = $stmt->fetch();

        if (!$reservation) {
            flash('error', 'Rezervace nebyla nalezena.');
        } elseif (!canCancelReservation($reservation, $user)) {
            flash('error', 'Tuto rezervaci nelze zrušit.');
        } else {
            $delete = db()->prepare('DELETE FROM reservations WHERE id = :id');
            $delete->execute(['id' => $reservation['id']]);
            flash('success', 'Rezervace byla zrušena.');
        }
    }

    if ($_GET['date'] !== $date) {
        redirect('index.php?date=' . urlencode($date));
    }
}

$reservations = fetchReservationsByDate(db(), $date);

renderPageStart('Rezervace loděk', 'reservations');
?>
<section class="card hero" id="hero-card">
    <h1>Rezervace loděk</h1>
    <p>
        Uživatel vidí obsazenost všech loděk v kalendáři a může si rezervovat volný 30minutový slot.
    </p>
    <!-- Flatpickr CSS -->
    <!-- Flatpickr JS & Czech locale -->
    <script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
    <script src="https://cdn.jsdelivr.net/npm/flatpickr/dist/l10n/cs.js"></script>

    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
    <form class="date-controls" method="get" id="date-nav-form">
        <button class="secondary-button" type="button" id="prev-day">Předchozí den</button>
        <input class="date-input" type="text" id="date-input-visible" value="<?= h(date('d.m.Y', strtotime($date))); ?>" autocomplete="off">
        <input type="hidden" name="date" id="date-input" value="<?= h($date); ?>">
        <button class="secondary-button" type="button" id="next-day">Další den</button>
    </form>
    
    <script>
    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('date-nav-form');
        var dateInput = document.getElementById('date-input'); // hidden input (Y-m-d)
        var dateInputVisible = document.getElementById('date-input-visible'); // visible input (d.m.Y)
        var czDateLabel = document.getElementById('cz-date-label');

        function parseYmd(value) {
            var parts = value.split('-');
            if (parts.length !== 3) {
                return new Date();
            }
            return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        }

        function formatYmd(date) {
            var y = date.getFullYear();
            var m = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            return y + '-' + m + '-' + day;
        }

        function formatDmy(date) {
            var y = date.getFullYear();
            var m = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            return day + '.' + m + '.' + y;
        }

        function submitDate(date) {
            dateInput.value = formatYmd(date);
            dateInputVisible.value = formatDmy(date);
            fp.setDate(date, false);
            form.submit();
        }

        dateInputVisible.value = formatDmy(parseYmd(dateInput.value));

        // Flatpickr init
        var fp = flatpickr(dateInputVisible, {
            dateFormat: 'd.m.Y',
            locale: 'cs',
            defaultDate: parseYmd(dateInput.value),
            allowInput: true,
            time_24hr: false,
            onChange: function(selectedDates, dateStr) {
                if (selectedDates.length) {
                    var d = selectedDates[0];
                    dateInput.value = formatYmd(d);
                    form.submit();
                }
            }
        });
        // Navigation buttons
        document.getElementById('prev-day').addEventListener('click', function () {
            var d = parseYmd(dateInput.value);
            d.setDate(d.getDate() - 1);
            submitDate(d);
        });
        document.getElementById('next-day').addEventListener('click', function () {
            var d = parseYmd(dateInput.value);
            d.setDate(d.getDate() + 1);
            submitDate(d);
        });
        // Update Czech label on change
        dateInputVisible.addEventListener('change', function() {
            if (czDateLabel) {
                fetch('?cz_date_format=' + encodeURIComponent(dateInput.value))
                    .then(r => r.text())
                    .then(txt => { czDateLabel.textContent = txt; });
            }
        });
    });
    </script>

</section>

<section class="card">
    <div class="info-grid">
        <div class="info-box user-info-box">
            <span class="info-box-label">Přihlášený uživatel</span>
            <span class="info-box-field"><?= h($user['username']); ?></span>
        </div>
        <div class="info-box schedule-info-box">Čas půjčování: <strong><?= cz_time($hours['start_time']); ?> až <?= cz_time($hours['end_time']); ?></strong> · délka slotu: <strong><?= h((string) $config['slot_minutes']); ?> min</strong></div>
    </div>
</section>

<section class="card reservation-card">
    <div class="reservation-table-scroll" id="reservation-table-scroll">
        <table class="reservation-table">
            <thead>
            <tr>
                <th>Lodka</th>
                <?php foreach ($slots as $slot): ?>
                    <th><?= cz_time($slot); ?></th>
                <?php endforeach; ?>
            </tr>
            </thead>
            <tbody>
            <?php foreach ($boats as $boat): ?>
                <tr>
                    <td class="boat-cell"><?= h($boat['name']); ?></td>
                    <?php foreach ($slots as $slot): ?>
                        <?php $reservation = $reservations[$boat['code']][$slot] ?? null; ?>
                        <td class="slot-cell">
                            <div class="slot-content">
                                <?php if ($reservation): ?>
                                    <span class="badge badge-danger">Obsazeno</span>
                                    <div class="reservation-owner"><?= h((string) reservationOwnerName($reservation)); ?></div>
                                    <?php if (canCancelReservation($reservation, $user)): ?>
                                        <form method="post" class="inline-form">
                                            <input type="hidden" name="boat_code" value="<?= h($boat['code']); ?>">
                                            <input type="hidden" name="slot_time" value="<?= h($slot); ?>">
                                            <input type="hidden" name="action" value="cancel">
                                            <button class="danger-button" type="submit">Zrušit rezervaci</button>
                                        </form>
                                    <?php endif; ?>
                                <?php else: ?>
                                    <form method="post" class="inline-form">
                                        <input type="hidden" name="boat_code" value="<?= h($boat['code']); ?>">
                                        <input type="hidden" name="slot_time" value="<?= h($slot); ?>">
                                        <input type="hidden" name="action" value="reserve">
                                        <button class="primary-button reserve-button" type="submit">Rezervovat</button>
                                    </form>
                                <?php endif; ?>
                            </div>
                        </td>
                    <?php endforeach; ?>
                </tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    </div>
    <div class="reservation-bottom-scroll" id="reservation-bottom-scroll" aria-hidden="true">
        <div class="reservation-bottom-scroll-inner" id="reservation-bottom-scroll-inner"></div>
    </div>
</section>
<script>
document.addEventListener('DOMContentLoaded', function () {
    const tableScroll = document.getElementById('reservation-table-scroll');
    const bottomScroll = document.getElementById('reservation-bottom-scroll');
    const bottomInner = document.getElementById('reservation-bottom-scroll-inner');
    const table = tableScroll ? tableScroll.querySelector('.reservation-table') : null;
    const firstStickyCell = table ? table.querySelector('thead th:first-child, tbody .boat-cell') : null;

    // hero remains in the document flow (no DOM move required)

    if (!tableScroll || !bottomScroll || !bottomInner || !table) {
        return;
    }

    const syncWidth = function () {
        const stickyWidth = firstStickyCell ? firstStickyCell.getBoundingClientRect().width : 176;
        document.documentElement.style.setProperty('--sticky-boat-width', stickyWidth + 'px');
        bottomInner.style.width = table.scrollWidth + 'px';
        bottomScroll.style.display = table.scrollWidth > tableScroll.clientWidth ? 'block' : 'none';
        bottomScroll.scrollLeft = tableScroll.scrollLeft;
    };

    let syncingFromTable = false;
    let syncingFromBottom = false;

    tableScroll.addEventListener('scroll', function () {
        if (syncingFromBottom) {
            syncingFromBottom = false;
            return;
        }
        syncingFromTable = true;
        bottomScroll.scrollLeft = tableScroll.scrollLeft;
    });

    bottomScroll.addEventListener('scroll', function () {
        if (syncingFromTable) {
            syncingFromTable = false;
            return;
        }
        syncingFromBottom = true;
        tableScroll.scrollLeft = bottomScroll.scrollLeft;
    });

    window.addEventListener('resize', syncWidth);
    syncWidth();
    // no hero sync required — hero stays in flow and scrolls with the page

    // Dynamically set --table-left to align scroll container with right edge
    function setTableLeftVar() {
        var tableScroll = document.getElementById('reservation-table-scroll');
        if (tableScroll) {
            var rect = tableScroll.getBoundingClientRect();
            var left = rect.left + window.scrollX;
            document.documentElement.style.setProperty('--table-left', left + 'px');
        }
    }
    setTableLeftVar();
    window.addEventListener('resize', setTableLeftVar);
});
</script>
<?php renderPageEnd(); ?>
