<?php

declare(strict_types=1);

return [
    'app_name' => 'Rezervace loděk',
    'db_path' => __DIR__ . '/storage.sqlite',
    'slot_minutes' => 30,
    'boats' => [
        ['code' => 'boat_1', 'name' => 'Loďka 1'],
        ['code' => 'boat_2', 'name' => 'Loďka 2'],
    ],
    'default_hours' => [
        'valid_from' => '2026-01-01',
        'start_time' => '08:00',
        'end_time' => '19:30',
    ],
    'admin' => [
        'username' => 'admin',
        'password' => 'admin',
        'email' => 'admin@example.com',
    ],
];