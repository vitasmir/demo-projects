UPDATE boat_rental_settings
SET day_end_time = '19:30:00',
    updated_at = now()
WHERE day_end_time < '19:30:00';
